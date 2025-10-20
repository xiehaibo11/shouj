use crate::AsyncHandler;
use crate::core::logger::ClashLogger;
use crate::core::validate::CoreConfigValidator;
use crate::process::CommandChildGuard;
use crate::utils::init::sidecar_writer;
use crate::utils::logging::{SharedWriter, write_sidecar_log};
use crate::{
    config::*,
    core::{
        handle,
        service::{self, SERVICE_MANAGER, ServiceStatus},
    },
    logging, logging_error, singleton_lazy,
    utils::{
        dirs,
        help::{self},
        logging::Type,
    },
};
use anyhow::{Result, anyhow};
#[cfg(target_os = "windows")]
use backoff::backoff::Backoff;
#[cfg(target_os = "windows")]
use backoff::{Error as BackoffError, ExponentialBackoff};
use compact_str::CompactString;
use flexi_logger::DeferredNow;
use log::Level;
use parking_lot::Mutex;
use std::collections::VecDeque;
use std::time::Instant;
use std::{error::Error, fmt, path::PathBuf, sync::Arc, time::Duration};
use tauri_plugin_mihomo::Error as MihomoError;
use tauri_plugin_shell::ShellExt;
use tokio::sync::Semaphore;
use tokio::time::sleep;

// TODO:
// - 重构，提升模式切换速度
// - 内核启动添加启动 IPC 启动参数, `-ext-ctl-unix` / `-ext-ctl-pipe`, 运行时配置需要删除相关配置项

#[derive(Debug)]
pub struct CoreManager {
    running: Arc<Mutex<RunningMode>>,
    child_sidecar: Arc<Mutex<Option<CommandChildGuard>>>,
    update_semaphore: Arc<Semaphore>,
    last_update: Arc<Mutex<Option<Instant>>>,
}

#[derive(Debug, Clone, Copy, serde::Serialize, PartialEq, Eq)]
pub enum RunningMode {
    Service,
    Sidecar,
    NotRunning,
}

impl fmt::Display for RunningMode {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            RunningMode::Service => write!(f, "Service"),
            RunningMode::Sidecar => write!(f, "Sidecar"),
            RunningMode::NotRunning => write!(f, "NotRunning"),
        }
    }
}

use crate::config::IVerge;

const CONNECTION_ERROR_PATTERNS: &[&str] = &[
    "Failed to create connection",
    "The system cannot find the file specified",
    "operation timed out",
    "connection refused",
];

impl CoreManager {
    pub async fn use_default_config(&self, msg_type: &str, msg_content: &str) -> Result<()> {
        let runtime_path = dirs::app_home_dir()?.join(RUNTIME_CONFIG);
        let clash_config = Config::clash().await.latest_ref().0.clone();

        *Config::runtime().await.draft_mut() = Box::new(IRuntime {
            config: Some(clash_config.clone()),
            exists_keys: vec![],
            chain_logs: Default::default(),
        });
        help::save_yaml(&runtime_path, &clash_config, Some("# Clash Verge Runtime")).await?;
        handle::Handle::notice_message(msg_type, msg_content);
        Ok(())
    }

    /// 更新proxies等配置
    pub async fn update_config(&self) -> Result<(bool, String)> {
        if handle::Handle::global().is_exiting() {
            logging!(info, Type::Config, "应用正在退出，跳过验证");
            return Ok((true, String::new()));
        }

        let now = Instant::now();
        {
            let mut last = self.last_update.lock();
            if let Some(last_time) = *last {
                if now.duration_since(last_time) < Duration::from_millis(500) {
                    logging!(debug, Type::Config, "防抖：跳过重复的配置更新请求");
                    return Ok((true, String::new()));
                }
            }
            *last = Some(now);
        }

        let permit = match self.update_semaphore.try_acquire() {
            Ok(p) => p,
            Err(_) => {
                logging!(debug, Type::Config, "配置更新已在进行中，跳过");
                return Ok((true, String::new()));
            }
        };

        let result = async {
            logging!(info, Type::Config, "生成新的配置内容");
            Config::generate().await?;

            match CoreConfigValidator::global().validate_config().await {
                Ok((true, _)) => {
                    logging!(info, Type::Config, "配置验证通过, 生成运行时配置");
                    let run_path = Config::generate_file(ConfigType::Run).await?;
                    self.put_configs_force(run_path).await?;
                    Ok((true, String::new()))
                }
                Ok((false, error_msg)) => {
                    logging!(warn, Type::Config, "配置验证失败: {}", error_msg);
                    Config::runtime().await.discard();
                    Ok((false, error_msg))
                }
                Err(e) => {
                    logging!(warn, Type::Config, "验证过程发生错误: {}", e);
                    Config::runtime().await.discard();
                    Err(e)
                }
            }
        }
        .await;

        drop(permit);
        result
    }
    pub async fn put_configs_force(&self, path_buf: PathBuf) -> Result<()> {
        let run_path_str = dirs::path_to_str(&path_buf).map_err(|e| {
            let msg = e.to_string();
            logging_error!(Type::Core, "{}", msg);
            anyhow!(msg)
        })?;

        match self.reload_config_once(run_path_str).await {
            Ok(_) => {
                Config::runtime().await.apply();
                logging!(info, Type::Core, "Configuration updated successfully");
                Ok(())
            }
            Err(err) => {
                let should_retry = Self::should_restart_on_reload_error(&err);
                let err_msg = err.to_string();

                if should_retry && !handle::Handle::global().is_exiting() {
                    logging!(
                        warn,
                        Type::Core,
                        "Reload config failed ({}), restarting core and retrying",
                        err_msg
                    );
                    if let Err(restart_err) = self.restart_core().await {
                        Config::runtime().await.discard();
                        logging_error!(
                            Type::Core,
                            "Failed to restart core after reload error: {}",
                            restart_err
                        );
                        return Err(restart_err);
                    }
                    sleep(Duration::from_millis(300)).await;

                    match self.reload_config_once(run_path_str).await {
                        Ok(_) => {
                            Config::runtime().await.apply();
                            logging!(
                                info,
                                Type::Core,
                                "Configuration updated successfully after restarting core"
                            );
                            return Ok(());
                        }
                        Err(retry_err) => {
                            let retry_msg = retry_err.to_string();
                            Config::runtime().await.discard();
                            logging_error!(
                                Type::Core,
                                "Failed to update configuration after restart: {}",
                                retry_msg
                            );
                            return Err(anyhow!(retry_msg));
                        }
                    }
                }

                Config::runtime().await.discard();
                logging_error!(Type::Core, "Failed to update configuration: {}", err_msg);
                Err(anyhow!(err_msg))
            }
        }
    }

    async fn reload_config_once(&self, config_path: &str) -> std::result::Result<(), MihomoError> {
        handle::Handle::mihomo()
            .await
            .reload_config(true, config_path)
            .await
    }

    fn should_restart_on_reload_error(err: &MihomoError) -> bool {
        fn is_connection_io_error(kind: std::io::ErrorKind) -> bool {
            matches!(
                kind,
                std::io::ErrorKind::ConnectionAborted
                    | std::io::ErrorKind::ConnectionRefused
                    | std::io::ErrorKind::ConnectionReset
                    | std::io::ErrorKind::NotFound
            )
        }

        fn contains_error_pattern(text: &str) -> bool {
            CONNECTION_ERROR_PATTERNS.iter().any(|p| text.contains(p))
        }

        match err {
            MihomoError::ConnectionFailed | MihomoError::ConnectionLost => true,
            MihomoError::Io(io_err) => is_connection_io_error(io_err.kind()),
            MihomoError::Reqwest(req_err) => {
                if req_err.is_connect() || req_err.is_timeout() {
                    return true;
                }
                if let Some(source) = req_err.source() {
                    if let Some(io_err) = source.downcast_ref::<std::io::Error>() {
                        if is_connection_io_error(io_err.kind()) {
                            return true;
                        }
                    } else if contains_error_pattern(&source.to_string()) {
                        return true;
                    }
                }
                contains_error_pattern(&req_err.to_string())
            }
            MihomoError::FailedResponse(msg) => contains_error_pattern(msg),
            _ => false,
        }
    }
}

impl CoreManager {
    async fn cleanup_orphaned_mihomo_processes(&self) -> Result<()> {
        logging!(info, Type::Core, "开始清理多余的 mihomo 进程");

        let current_pid = self
            .child_sidecar
            .lock()
            .as_ref()
            .and_then(|child| child.pid());
        let target_processes = ["verge-mihomo", "verge-mihomo-alpha"];

        let process_futures = target_processes.iter().map(|&target| {
            let process_name = if cfg!(windows) {
                format!("{target}.exe")
            } else {
                target.to_string()
            };
            self.find_processes_by_name(process_name, target)
        });

        let process_results = futures::future::join_all(process_futures).await;

        let pids_to_kill: Vec<_> = process_results
            .into_iter()
            .filter_map(|result| result.ok())
            .flat_map(|(pids, process_name)| {
                pids.into_iter()
                    .filter(|&pid| Some(pid) != current_pid)
                    .map(move |pid| (pid, process_name.clone()))
            })
            .collect();

        if pids_to_kill.is_empty() {
            logging!(debug, Type::Core, "未发现多余的 mihomo 进程");
            return Ok(());
        }

        let kill_futures = pids_to_kill
            .iter()
            .map(|(pid, name)| self.kill_process_with_verification(*pid, name.clone()));

        let killed_count = futures::future::join_all(kill_futures)
            .await
            .into_iter()
            .filter(|&success| success)
            .count();

        if killed_count > 0 {
            logging!(
                info,
                Type::Core,
                "清理完成，共终止了 {} 个多余的 mihomo 进程",
                killed_count
            );
        }

        Ok(())
    }

    /// 根据进程名查找进程PID列
    async fn find_processes_by_name(
        &self,
        process_name: String,
        _target: &str,
    ) -> Result<(Vec<u32>, String)> {
        #[cfg(windows)]
        {
            use std::mem;
            use winapi::um::handleapi::CloseHandle;
            use winapi::um::tlhelp32::{
                CreateToolhelp32Snapshot, PROCESSENTRY32W, Process32FirstW, Process32NextW,
                TH32CS_SNAPPROCESS,
            };
            use winapi::um::winnt::HANDLE;

            let process_name_clone = process_name.clone();
            let pids = AsyncHandler::spawn_blocking(move || -> Result<Vec<u32>> {
                let mut pids = Vec::with_capacity(8);

                unsafe {
                    let snapshot: HANDLE = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
                    if snapshot == winapi::um::handleapi::INVALID_HANDLE_VALUE {
                        return Err(anyhow::anyhow!("Failed to create process snapshot"));
                    }

                    let mut pe32: PROCESSENTRY32W = mem::zeroed();
                    pe32.dwSize = mem::size_of::<PROCESSENTRY32W>() as u32;

                    if Process32FirstW(snapshot, &mut pe32) != 0 {
                        loop {
                            let end_pos = pe32
                                .szExeFile
                                .iter()
                                .position(|&x| x == 0)
                                .unwrap_or(pe32.szExeFile.len());

                            if end_pos > 0 {
                                let exe_file = String::from_utf16_lossy(&pe32.szExeFile[..end_pos]);
                                if exe_file.eq_ignore_ascii_case(&process_name_clone) {
                                    pids.push(pe32.th32ProcessID);
                                }
                            }

                            if Process32NextW(snapshot, &mut pe32) == 0 {
                                break;
                            }
                        }
                    }

                    CloseHandle(snapshot);
                }

                Ok(pids)
            })
            .await??;

            Ok((pids, process_name))
        }

        #[cfg(not(windows))]
        {
            let output = if cfg!(target_os = "macos") {
                tokio::process::Command::new("pgrep")
                    .arg(&process_name)
                    .output()
                    .await?
            } else {
                tokio::process::Command::new("pidof")
                    .arg(&process_name)
                    .output()
                    .await?
            };

            if !output.status.success() {
                return Ok((Vec::new(), process_name));
            }

            let stdout = String::from_utf8_lossy(&output.stdout);
            let pids: Vec<u32> = stdout
                .split_whitespace()
                .filter_map(|s| s.parse().ok())
                .collect();

            Ok((pids, process_name))
        }
    }

    async fn kill_process_with_verification(&self, pid: u32, process_name: String) -> bool {
        logging!(
            info,
            Type::Core,
            "尝试终止进程: {} (PID: {})",
            process_name,
            pid
        );

        #[cfg(windows)]
        let success = {
            use winapi::um::handleapi::CloseHandle;
            use winapi::um::processthreadsapi::{OpenProcess, TerminateProcess};
            use winapi::um::winnt::{HANDLE, PROCESS_TERMINATE};

            AsyncHandler::spawn_blocking(move || unsafe {
                let handle: HANDLE = OpenProcess(PROCESS_TERMINATE, 0, pid);
                if handle.is_null() {
                    return false;
                }
                let result = TerminateProcess(handle, 1) != 0;
                CloseHandle(handle);
                result
            })
            .await
            .unwrap_or(false)
        };

        #[cfg(not(windows))]
        let success = tokio::process::Command::new("kill")
            .args(["-9", &pid.to_string()])
            .output()
            .await
            .map(|output| output.status.success())
            .unwrap_or(false);

        if !success {
            logging!(
                warn,
                Type::Core,
                "无法终止进程: {} (PID: {})",
                process_name,
                pid
            );
            return false;
        }

        tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;

        if self.is_process_running(pid).await.unwrap_or(false) {
            logging!(
                warn,
                Type::Core,
                "进程 {} (PID: {}) 终止命令成功但进程仍在运行",
                process_name,
                pid
            );
            false
        } else {
            logging!(
                info,
                Type::Core,
                "成功终止进程: {} (PID: {})",
                process_name,
                pid
            );
            true
        }
    }

    async fn is_process_running(&self, pid: u32) -> Result<bool> {
        #[cfg(windows)]
        {
            use winapi::shared::minwindef::DWORD;
            use winapi::um::handleapi::CloseHandle;
            use winapi::um::processthreadsapi::{GetExitCodeProcess, OpenProcess};
            use winapi::um::winnt::{HANDLE, PROCESS_QUERY_INFORMATION};

            AsyncHandler::spawn_blocking(move || unsafe {
                let handle: HANDLE = OpenProcess(PROCESS_QUERY_INFORMATION, 0, pid);
                if handle.is_null() {
                    return Ok(false);
                }
                let mut exit_code: DWORD = 0;
                let result = GetExitCodeProcess(handle, &mut exit_code);
                CloseHandle(handle);
                Ok(result != 0 && exit_code == 259)
            })
            .await?
        }

        #[cfg(not(windows))]
        {
            let output = tokio::process::Command::new("ps")
                .args(["-p", &pid.to_string()])
                .output()
                .await?;

            Ok(output.status.success() && !output.stdout.is_empty())
        }
    }

    async fn start_core_by_sidecar(&self) -> Result<()> {
        logging!(info, Type::Core, "Running core by sidecar");

        let config_file = &Config::generate_file(ConfigType::Run).await?;
        let app_handle = handle::Handle::app_handle();
        let clash_core = Config::verge().await.latest_ref().get_valid_clash_core();
        let config_dir = dirs::app_home_dir()?;

        let (mut rx, child) = app_handle
            .shell()
            .sidecar(&clash_core)?
            .args([
                "-d",
                dirs::path_to_str(&config_dir)?,
                "-f",
                dirs::path_to_str(config_file)?,
            ])
            .spawn()?;

        let pid = child.pid();
        logging!(trace, Type::Core, "Started core by sidecar pid: {}", pid);
        *self.child_sidecar.lock() = Some(CommandChildGuard::new(child));
        self.set_running_mode(RunningMode::Sidecar);

        let shared_writer: SharedWriter =
            Arc::new(tokio::sync::Mutex::new(sidecar_writer().await?));

        AsyncHandler::spawn(|| async move {
            while let Some(event) = rx.recv().await {
                match event {
                    tauri_plugin_shell::process::CommandEvent::Stdout(line)
                    | tauri_plugin_shell::process::CommandEvent::Stderr(line) => {
                        let mut now = DeferredNow::default();
                        let message = CompactString::from(String::from_utf8_lossy(&line).as_ref());
                        let w = shared_writer.lock().await;
                        write_sidecar_log(w, &mut now, Level::Error, &message);
                        ClashLogger::global().append_log(message);
                    }
                    tauri_plugin_shell::process::CommandEvent::Terminated(term) => {
                        let mut now = DeferredNow::default();
                        let message = if let Some(code) = term.code {
                            CompactString::from(format!("Process terminated with code: {}", code))
                        } else if let Some(signal) = term.signal {
                            CompactString::from(format!("Process terminated by signal: {}", signal))
                        } else {
                            CompactString::from("Process terminated")
                        };
                        let w = shared_writer.lock().await;
                        write_sidecar_log(w, &mut now, Level::Info, &message);
                        ClashLogger::global().clear_logs();
                        break;
                    }
                    _ => {}
                }
            }
        });

        Ok(())
    }
    fn stop_core_by_sidecar(&self) -> Result<()> {
        logging!(info, Type::Core, "Stopping core by sidecar");

        if let Some(child) = self.child_sidecar.lock().take() {
            let pid = child.pid();
            drop(child);
            logging!(trace, Type::Core, "Stopped core by sidecar pid: {:?}", pid);
        }
        self.set_running_mode(RunningMode::NotRunning);
        Ok(())
    }
}

impl CoreManager {
    async fn start_core_by_service(&self) -> Result<()> {
        logging!(info, Type::Core, "Running core by service");
        let config_file = &Config::generate_file(ConfigType::Run).await?;
        service::run_core_by_service(config_file).await?;
        self.set_running_mode(RunningMode::Service);
        Ok(())
    }

    async fn stop_core_by_service(&self) -> Result<()> {
        logging!(info, Type::Core, "Stopping core by service");
        service::stop_core_by_service().await?;
        self.set_running_mode(RunningMode::NotRunning);
        Ok(())
    }
}

impl Default for CoreManager {
    fn default() -> Self {
        CoreManager {
            running: Arc::new(Mutex::new(RunningMode::NotRunning)),
            child_sidecar: Arc::new(Mutex::new(None)),
            update_semaphore: Arc::new(Semaphore::new(1)),
            last_update: Arc::new(Mutex::new(None)),
        }
    }
}

impl CoreManager {
    pub async fn init(&self) -> Result<()> {
        logging!(info, Type::Core, "开始核心初始化");

        if let Err(e) = self.cleanup_orphaned_mihomo_processes().await {
            logging!(warn, Type::Core, "清理遗留 mihomo 进程失败: {}", e);
        }

        self.start_core().await?;
        logging!(info, Type::Core, "核心初始化完成");
        Ok(())
    }

    pub fn set_running_mode(&self, mode: RunningMode) {
        let mut guard = self.running.lock();
        *guard = mode;
    }

    pub fn get_running_mode(&self) -> RunningMode {
        *self.running.lock()
    }

    #[cfg(target_os = "windows")]
    async fn wait_for_service_ready_if_tun_enabled(&self) {
        let require_service = Config::verge()
            .await
            .latest_ref()
            .enable_tun_mode
            .unwrap_or(false);

        if !require_service {
            return;
        }

        let max_wait = Duration::from_millis(3000);
        let mut backoff_strategy = ExponentialBackoff {
            initial_interval: Duration::from_millis(200),
            max_interval: Duration::from_millis(200),
            max_elapsed_time: Some(max_wait),
            multiplier: 1.0,
            randomization_factor: 0.0,
            ..Default::default()
        };
        backoff_strategy.reset();

        let mut attempts = 0usize;

        let operation = || {
            attempts += 1;
            let attempt = attempts;

            async move {
                let mut manager = SERVICE_MANAGER.lock().await;

                if matches!(manager.current(), ServiceStatus::Ready) {
                    if attempt > 1 {
                        logging!(
                            info,
                            Type::Core,
                            "Service became ready for TUN after {} attempt(s)",
                            attempt
                        );
                    }
                    return Ok(());
                }

                if attempt == 1 {
                    logging!(
                        info,
                        Type::Core,
                        "TUN mode enabled but service not ready; waiting for service availability"
                    );
                }

                match manager.init().await {
                    Ok(_) => {
                        logging_error!(Type::Core, manager.refresh().await);
                    }
                    Err(err) => {
                        logging!(
                            debug,
                            Type::Core,
                            "Service connection attempt {} failed while waiting for TUN: {}",
                            attempt,
                            err
                        );
                        return Err(BackoffError::transient(err));
                    }
                }

                if matches!(manager.current(), ServiceStatus::Ready) {
                    logging!(
                        info,
                        Type::Core,
                        "Service became ready for TUN after {} attempt(s)",
                        attempt
                    );
                    return Ok(());
                }

                logging!(
                    debug,
                    Type::Core,
                    "Service not ready after attempt {}; retrying with backoff",
                    attempt
                );

                Err(BackoffError::transient(anyhow!("Service not ready yet")))
            }
        };

        let wait_started = Instant::now();

        if let Err(err) = backoff::future::retry(backoff_strategy, operation).await {
            let waited_ms = wait_started.elapsed().as_millis();
            logging!(
                warn,
                Type::Core,
                "Service still not ready after waiting approximately {} ms ({} attempt(s)); falling back to sidecar mode: {}",
                waited_ms,
                attempts,
                err
            );
        }
    }

    // TODO: 是否需要在非windows平台上进行检测
    #[allow(clippy::unused_async)]
    #[cfg(not(target_os = "windows"))]
    async fn wait_for_service_ready_if_tun_enabled(&self) {}

    pub async fn prestart_core(&self) -> Result<()> {
        self.wait_for_service_ready_if_tun_enabled().await;

        match SERVICE_MANAGER.lock().await.current() {
            ServiceStatus::Ready => {
                self.set_running_mode(RunningMode::Service);
            }
            _ => {
                self.set_running_mode(RunningMode::Sidecar);
            }
        }
        Ok(())
    }

    /// 启动核心
    pub async fn start_core(&self) -> Result<()> {
        self.prestart_core().await?;

        match self.get_running_mode() {
            RunningMode::Service => {
                logging_error!(Type::Core, self.start_core_by_service().await);
            }
            RunningMode::NotRunning | RunningMode::Sidecar => {
                logging_error!(Type::Core, self.start_core_by_sidecar().await);
            }
        };

        Ok(())
    }

    pub async fn get_clash_logs(&self) -> Result<VecDeque<CompactString>> {
        logging!(info, Type::Core, "get clash logs");
        let logs = match self.get_running_mode() {
            RunningMode::Service => service::get_clash_logs_by_service().await?,
            RunningMode::Sidecar => ClashLogger::global().get_logs().clone(),
            _ => VecDeque::new(),
        };
        Ok(logs)
    }

    /// 停止核心运行
    pub async fn stop_core(&self) -> Result<()> {
        ClashLogger::global().clear_logs();
        match self.get_running_mode() {
            RunningMode::Service => self.stop_core_by_service().await,
            RunningMode::Sidecar => self.stop_core_by_sidecar(),
            RunningMode::NotRunning => Ok(()),
        }
    }

    /// 重启内核
    pub async fn restart_core(&self) -> Result<()> {
        logging!(info, Type::Core, "Restarting core");
        self.stop_core().await?;
        if SERVICE_MANAGER.lock().await.init().await.is_ok() {
            logging_error!(Type::Setup, SERVICE_MANAGER.lock().await.refresh().await);
        }
        self.start_core().await?;
        Ok(())
    }

    /// 切换核心
    pub async fn change_core(&self, clash_core: Option<String>) -> Result<(), String> {
        if clash_core.is_none() {
            let error_message = "Clash core should not be Null";
            logging!(error, Type::Core, "{}", error_message);
            return Err(error_message.into());
        }
        let core = clash_core.as_ref().ok_or_else(|| {
            let msg = "Clash core should not be None";
            logging!(error, Type::Core, "{}", msg);
            msg.to_string()
        })?;
        if !IVerge::VALID_CLASH_CORES.contains(&core.as_str()) {
            let error_message = format!("Clash core invalid name: {core}");
            logging!(error, Type::Core, "{}", error_message);
            return Err(error_message);
        }

        Config::verge().await.draft_mut().clash_core = clash_core.clone();
        Config::verge().await.apply();

        // 分离数据获取和异步调用避免Send问题
        let verge_data = Config::verge().await.latest_ref().clone();
        logging_error!(Type::Core, verge_data.save_file().await);

        let run_path = Config::generate_file(ConfigType::Run).await.map_err(|e| {
            let msg = e.to_string();
            logging_error!(Type::Core, "{}", msg);
            msg
        })?;

        self.put_configs_force(run_path)
            .await
            .map_err(|e| e.to_string())?;

        Ok(())
    }
}

// Use simplified singleton_lazy macro
singleton_lazy!(CoreManager, CORE_MANAGER, CoreManager::default);
