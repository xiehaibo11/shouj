use super::{CoreManager, RunningMode};
use crate::{
    core::{
        logger::ClashLogger,
        service::{SERVICE_MANAGER, ServiceStatus},
    },
    logging,
    utils::logging::Type,
};
use anyhow::Result;

impl CoreManager {
    pub async fn start_core(&self) -> Result<()> {
        self.prepare_startup().await?;

        match self.get_running_mode() {
            RunningMode::Service => self.start_core_by_service().await,
            RunningMode::NotRunning | RunningMode::Sidecar => self.start_core_by_sidecar().await,
        }
    }

    pub async fn stop_core(&self) -> Result<()> {
        ClashLogger::global().clear_logs();

        match self.get_running_mode() {
            RunningMode::Service => self.stop_core_by_service().await,
            RunningMode::Sidecar => self.stop_core_by_sidecar(),
            RunningMode::NotRunning => Ok(()),
        }
    }

    pub async fn restart_core(&self) -> Result<()> {
        logging!(info, Type::Core, "Restarting core");
        self.stop_core().await?;

        if SERVICE_MANAGER.lock().await.init().await.is_ok() {
            let _ = SERVICE_MANAGER.lock().await.refresh().await;
        }

        self.start_core().await
    }

    pub async fn change_core(&self, clash_core: Option<String>) -> Result<(), String> {
        use crate::config::{Config, ConfigType, IVerge};

        let core = clash_core
            .as_ref()
            .ok_or_else(|| "Clash core cannot be None".to_string())?;

        if !IVerge::VALID_CLASH_CORES.contains(&core.as_str()) {
            return Err(format!("Invalid clash core: {}", core));
        }

        Config::verge().await.draft_mut().clash_core = clash_core;
        Config::verge().await.apply();

        let verge_data = Config::verge().await.latest_ref().clone();
        verge_data.save_file().await.map_err(|e| e.to_string())?;

        let run_path = Config::generate_file(ConfigType::Run)
            .await
            .map_err(|e| e.to_string())?;

        self.apply_config(run_path).await.map_err(|e| e.to_string())
    }

    async fn prepare_startup(&self) -> Result<()> {
        #[cfg(target_os = "windows")]
        self.wait_for_service_if_needed().await;

        let mode = match SERVICE_MANAGER.lock().await.current() {
            ServiceStatus::Ready => RunningMode::Service,
            _ => RunningMode::Sidecar,
        };

        self.set_running_mode(mode);
        Ok(())
    }

    #[cfg(target_os = "windows")]
    async fn wait_for_service_if_needed(&self) {
        use crate::{config::Config, constants::timing};
        use backoff::{Error as BackoffError, ExponentialBackoff};

        let needs_service = Config::verge()
            .await
            .latest_ref()
            .enable_tun_mode
            .unwrap_or(false);

        if !needs_service {
            return;
        }

        let backoff = ExponentialBackoff {
            initial_interval: timing::SERVICE_WAIT_INTERVAL,
            max_interval: timing::SERVICE_WAIT_INTERVAL,
            max_elapsed_time: Some(timing::SERVICE_WAIT_MAX),
            multiplier: 1.0,
            randomization_factor: 0.0,
            ..Default::default()
        };

        let operation = || async {
            let mut manager = SERVICE_MANAGER.lock().await;

            if matches!(manager.current(), ServiceStatus::Ready) {
                return Ok(());
            }

            manager.init().await.map_err(BackoffError::transient)?;
            let _ = manager.refresh().await;

            if matches!(manager.current(), ServiceStatus::Ready) {
                Ok(())
            } else {
                Err(BackoffError::transient(anyhow::anyhow!(
                    "Service not ready"
                )))
            }
        };

        let _ = backoff::future::retry(backoff, operation).await;
    }
}
