#![allow(non_snake_case)]
#![recursion_limit = "512"]

mod cmd;
pub mod config;
mod core;
mod enhance;
mod feat;
mod module;
mod process;
pub mod utils;
#[cfg(target_os = "linux")]
use crate::utils::linux;
#[cfg(target_os = "macos")]
use crate::utils::window_manager::WindowManager;
use crate::{
    core::{EventDrivenProxyManager, handle, hotkey},
    process::AsyncHandler,
    utils::{resolve, server},
};
use config::Config;
use once_cell::sync::OnceCell;
use tauri::{AppHandle, Manager};
#[cfg(target_os = "macos")]
use tauri_plugin_autostart::MacosLauncher;
use tauri_plugin_deep_link::DeepLinkExt;
use utils::logging::Type;

pub static APP_HANDLE: OnceCell<AppHandle> = OnceCell::new();

/// Application initialization helper functions
mod app_init {
    use anyhow::Result;

    use super::*;

    /// Initialize singleton monitoring for other instances
    pub fn init_singleton_check() -> Result<()> {
        tauri::async_runtime::block_on(async move {
            logging!(info, Type::Setup, "开始检查单例实例...");
            server::check_singleton().await?;
            Ok(())
        })
    }

    /// Setup plugins for the Tauri builder
    pub fn setup_plugins(builder: tauri::Builder<tauri::Wry>) -> tauri::Builder<tauri::Wry> {
        #[allow(unused_mut)]
        let mut builder = builder
            .plugin(tauri_plugin_notification::init())
            .plugin(tauri_plugin_updater::Builder::new().build())
            .plugin(tauri_plugin_clipboard_manager::init())
            .plugin(tauri_plugin_process::init())
            .plugin(tauri_plugin_global_shortcut::Builder::new().build())
            .plugin(tauri_plugin_fs::init())
            .plugin(tauri_plugin_dialog::init())
            .plugin(tauri_plugin_shell::init())
            .plugin(tauri_plugin_deep_link::init())
            .plugin(tauri_plugin_http::init())
            .plugin(
                tauri_plugin_mihomo::Builder::new()
                    .protocol(tauri_plugin_mihomo::models::Protocol::LocalSocket)
                    .socket_path(crate::config::IClashTemp::guard_external_controller_ipc())
                    .build(),
            );

        // Devtools plugin only in debug mode with feature tauri-dev
        // to avoid duplicated registering of logger since the devtools plugin also registers a logger
        #[cfg(all(debug_assertions, not(feature = "tokio-trace"), feature = "tauri-dev"))]
        {
            builder = builder.plugin(tauri_plugin_devtools::init());
        }
        builder
    }

    /// Setup deep link handling
    pub fn setup_deep_links(app: &tauri::App) -> Result<(), Box<dyn std::error::Error>> {
        #[cfg(any(target_os = "linux", all(debug_assertions, windows)))]
        {
            logging!(info, Type::Setup, "注册深层链接...");
            app.deep_link().register_all()?;
        }

        app.deep_link().on_open_url(|event| {
            let url = event.urls().first().map(|u| u.to_string());
            if let Some(url) = url {
                let _ = AsyncHandler::spawn(|| async {
                    if let Err(e) = resolve::resolve_scheme(url).await {
                        logging!(error, Type::Setup, "Failed to resolve scheme: {}", e);
                    }
                });
            }
        });

        Ok(())
    }

    /// Setup autostart plugin
    pub fn setup_autostart(app: &tauri::App) -> Result<(), Box<dyn std::error::Error>> {
        #[cfg(target_os = "macos")]
        let mut auto_start_plugin_builder = tauri_plugin_autostart::Builder::new();
        #[cfg(not(target_os = "macos"))]
        let auto_start_plugin_builder = tauri_plugin_autostart::Builder::new();

        #[cfg(target_os = "macos")]
        {
            auto_start_plugin_builder = auto_start_plugin_builder
                .macos_launcher(MacosLauncher::LaunchAgent)
                .app_name(app.config().identifier.clone());
        }
        app.handle().plugin(auto_start_plugin_builder.build())?;
        Ok(())
    }

    /// Setup window state management
    pub fn setup_window_state(app: &tauri::App) -> Result<(), Box<dyn std::error::Error>> {
        logging!(info, Type::Setup, "初始化窗口状态管理...");
        let window_state_plugin = tauri_plugin_window_state::Builder::new()
            .with_filename("window_state.json")
            .with_state_flags(tauri_plugin_window_state::StateFlags::default())
            .build();
        app.handle().plugin(window_state_plugin)?;
        Ok(())
    }

    pub fn generate_handlers()
    -> impl Fn(tauri::ipc::Invoke<tauri::Wry>) -> bool + Send + Sync + 'static {
        tauri::generate_handler![
            cmd::get_sys_proxy,
            cmd::get_auto_proxy,
            cmd::open_app_dir,
            cmd::open_logs_dir,
            cmd::open_web_url,
            cmd::open_core_dir,
            cmd::get_portable_flag,
            cmd::get_network_interfaces,
            cmd::get_system_hostname,
            cmd::restart_app,
            cmd::start_core,
            cmd::stop_core,
            cmd::restart_core,
            cmd::notify_ui_ready,
            cmd::update_ui_stage,
            cmd::get_running_mode,
            cmd::get_app_uptime,
            cmd::get_auto_launch_status,
            cmd::is_admin,
            cmd::entry_lightweight_mode,
            cmd::exit_lightweight_mode,
            cmd::install_service,
            cmd::uninstall_service,
            cmd::reinstall_service,
            cmd::repair_service,
            cmd::is_service_available,
            cmd::get_clash_info,
            cmd::patch_clash_config,
            cmd::patch_clash_mode,
            cmd::change_clash_core,
            cmd::get_runtime_config,
            cmd::get_runtime_yaml,
            cmd::get_runtime_exists,
            cmd::get_runtime_logs,
            cmd::get_runtime_proxy_chain_config,
            cmd::update_proxy_chain_config_in_runtime,
            cmd::invoke_uwp_tool,
            cmd::copy_clash_env,
            cmd::sync_tray_proxy_selection,
            cmd::save_dns_config,
            cmd::apply_dns_config,
            cmd::check_dns_config_exists,
            cmd::get_dns_config_content,
            cmd::validate_dns_config,
            cmd::get_clash_logs,
            cmd::get_verge_config,
            cmd::patch_verge_config,
            cmd::test_delay,
            cmd::get_app_dir,
            cmd::copy_icon_file,
            cmd::download_icon_cache,
            cmd::open_devtools,
            cmd::exit_app,
            cmd::get_network_interfaces_info,
            cmd::get_profiles,
            cmd::enhance_profiles,
            cmd::patch_profiles_config,
            cmd::view_profile,
            cmd::patch_profile,
            cmd::create_profile,
            cmd::import_profile,
            cmd::reorder_profile,
            cmd::update_profile,
            cmd::delete_profile,
            cmd::read_profile_file,
            cmd::save_profile_file,
            cmd::get_next_update_time,
            cmd::script_validate_notice,
            cmd::validate_script_file,
            cmd::create_local_backup,
            cmd::list_local_backup,
            cmd::delete_local_backup,
            cmd::restore_local_backup,
            cmd::export_local_backup,
            cmd::create_webdav_backup,
            cmd::save_webdav_config,
            cmd::list_webdav_backup,
            cmd::delete_webdav_backup,
            cmd::restore_webdav_backup,
            cmd::export_diagnostic_info,
            cmd::get_system_info,
            cmd::get_unlock_items,
            cmd::check_media_unlock,
        ]
    }
}

pub fn run() {
    if app_init::init_singleton_check().is_err() {
        return;
    }

    let _ = utils::dirs::init_portable_flag();

    #[cfg(target_os = "linux")]
    linux::configure_environment();

    let builder = app_init::setup_plugins(tauri::Builder::default())
        .setup(|app| {
            logging!(info, Type::Setup, "开始应用初始化...");

            #[allow(clippy::expect_used)]
            APP_HANDLE
                .set(app.app_handle().clone())
                .expect("failed to set global app handle");

            if let Err(e) = app_init::setup_autostart(app) {
                logging!(error, Type::Setup, "Failed to setup autostart: {}", e);
            }

            if let Err(e) = app_init::setup_deep_links(app) {
                logging!(error, Type::Setup, "Failed to setup deep links: {}", e);
            }

            if let Err(e) = app_init::setup_window_state(app) {
                logging!(error, Type::Setup, "Failed to setup window state: {}", e);
            }

            resolve::resolve_setup_handle();
            resolve::resolve_setup_async();
            resolve::resolve_setup_sync();

            logging!(info, Type::Setup, "初始化已启动");
            Ok(())
        })
        .invoke_handler(app_init::generate_handlers());

    mod event_handlers {
        use crate::core::handle;
        use super::*;

        pub fn handle_ready_resumed(_app_handle: &AppHandle) {
            if handle::Handle::global().is_exiting() {
                logging!(debug, Type::System, "应用正在退出，跳过处理");
                return;
            }

            logging!(info, Type::System, "应用就绪");
            handle::Handle::global().init();

            #[cfg(target_os = "macos")]
            if let Some(window) = _app_handle.get_webview_window("main") {
                let _ = window.set_title("Clash Verge");
            }
        }

        #[cfg(target_os = "macos")]
        pub async fn handle_reopen(has_visible_windows: bool) {
            handle::Handle::global().init();

            if !has_visible_windows {
                handle::Handle::global().set_activation_policy_regular();
                let _ = WindowManager::show_main_window().await;
            }
        }

        pub fn handle_window_close(api: &tauri::WindowEvent) {
            #[cfg(target_os = "macos")]
            handle::Handle::global().set_activation_policy_accessory();

            if core::handle::Handle::global().is_exiting() {
                return;
            }

            if let tauri::WindowEvent::CloseRequested { api, .. } = api {
                api.prevent_close();
                if let Some(window) = core::handle::Handle::get_window() {
                    let _ = window.hide();
                }
            }
        }

        pub fn handle_window_focus(focused: bool) {
            let _ = AsyncHandler::spawn(move || async move {
                let is_enable_global_hotkey = Config::verge()
                    .await
                    .latest_ref()
                    .enable_global_hotkey
                    .unwrap_or(true);

                if focused {
                    #[cfg(target_os = "macos")]
                    {
                        use crate::core::hotkey::SystemHotkey;
                        let _ = hotkey::Hotkey::global().register_system_hotkey(SystemHotkey::CmdQ).await;
                        let _ = hotkey::Hotkey::global().register_system_hotkey(SystemHotkey::CmdW).await;
                    }

                    if !is_enable_global_hotkey {
                        let _ = hotkey::Hotkey::global().init().await;
                    }
                    return;
                }

                #[cfg(target_os = "macos")]
                {
                    use crate::core::hotkey::SystemHotkey;
                    let _ = hotkey::Hotkey::global().unregister_system_hotkey(SystemHotkey::CmdQ);
                    let _ = hotkey::Hotkey::global().unregister_system_hotkey(SystemHotkey::CmdW);
                }

                if !is_enable_global_hotkey {
                    let _ = hotkey::Hotkey::global().reset();
                }
            });
        }

        pub fn handle_window_destroyed() {
            let _ = AsyncHandler::spawn(|| async {
                let _ = handle::Handle::mihomo()
                    .await
                    .clear_all_ws_connections()
                    .await;
            });

            #[cfg(target_os = "macos")]
            {
                use crate::core::hotkey::SystemHotkey;
                let _ = hotkey::Hotkey::global().unregister_system_hotkey(SystemHotkey::CmdQ);
                let _ = hotkey::Hotkey::global().unregister_system_hotkey(SystemHotkey::CmdW);
            }
        }
    }

    #[cfg(feature = "clippy")]
    let context = tauri::test::mock_context(tauri::test::noop_assets());
    #[cfg(feature = "clippy")]
    let app = builder.build(context).unwrap_or_else(|e| {
        logging!(error, Type::Setup, "Failed to build Tauri application: {}", e);
        std::process::exit(1);
    });

    #[cfg(not(feature = "clippy"))]
    let app = builder.build(tauri::generate_context!()).unwrap_or_else(|e| {
        logging!(error, Type::Setup, "Failed to build Tauri application: {}", e);
        std::process::exit(1);
    });

    app.run(|app_handle, e| {
        match e {
            tauri::RunEvent::Ready | tauri::RunEvent::Resumed => {
                if core::handle::Handle::global().is_exiting() {
                    return;
                }
                event_handlers::handle_ready_resumed(app_handle);
            }
            #[cfg(target_os = "macos")]
            tauri::RunEvent::Reopen { has_visible_windows, .. } => {
                if core::handle::Handle::global().is_exiting() {
                    return;
                }
                let _ = AsyncHandler::spawn(move || async move {
                    event_handlers::handle_reopen(has_visible_windows).await;
                });
            }
            tauri::RunEvent::ExitRequested { api, code, .. } => {
                tauri::async_runtime::block_on(async {
                    let _ = handle::Handle::mihomo().await.clear_all_ws_connections().await;
                });

                if core::handle::Handle::global().is_exiting() {
                    return;
                }

                if code.is_none() {
                    api.prevent_exit();
                }
            }
            tauri::RunEvent::Exit => {
                let handle = core::handle::Handle::global();
                if !handle.is_exiting() {
                    handle.set_is_exiting();
                    EventDrivenProxyManager::global().notify_app_stopping();
                    feat::clean();
                }
            }
            tauri::RunEvent::WindowEvent { label, event, .. } if label == "main" => {
                match event {
                    tauri::WindowEvent::CloseRequested { .. } => {
                        event_handlers::handle_window_close(&event);
                    }
                    tauri::WindowEvent::Focused(focused) => {
                        event_handlers::handle_window_focus(focused);
                    }
                    tauri::WindowEvent::Destroyed => {
                        event_handlers::handle_window_destroyed();
                    }
                    _ => {}
                }
            }
            _ => {}
        }
    });
}
