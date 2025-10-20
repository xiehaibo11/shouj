use std::time::Duration;

use super::resolve;
use crate::{
    config::{Config, DEFAULT_PAC, IVerge},
    logging, logging_error,
    module::lightweight,
    process::AsyncHandler,
    utils::{logging::Type, window_manager::WindowManager},
};
use anyhow::{Result, bail};
use once_cell::sync::OnceCell;
use parking_lot::Mutex;
use port_scanner::local_port_available;
use reqwest::ClientBuilder;
use tokio::sync::oneshot;
use warp::Filter;

#[derive(serde::Deserialize, Debug)]
struct QueryParam {
    param: String,
}

// 关闭 embedded server 的信号发送端
static SHUTDOWN_SENDER: OnceCell<Mutex<Option<oneshot::Sender<()>>>> = OnceCell::new();

/// check whether there is already exists
pub async fn check_singleton() -> Result<()> {
    let port = IVerge::get_singleton_port();
    if !local_port_available(port) {
        let client = ClientBuilder::new()
            .timeout(Duration::from_millis(500))
            .build()?;
        let argvs: Vec<String> = std::env::args().collect();
        if argvs.len() > 1 {
            #[cfg(not(target_os = "macos"))]
            {
                let param = argvs[1].as_str();
                if param.starts_with("clash:") {
                    client
                        .get(format!(
                            "http://127.0.0.1:{port}/commands/scheme?param={param}"
                        ))
                        .send()
                        .await?;
                }
            }
        } else {
            client
                .get(format!("http://127.0.0.1:{port}/commands/visible"))
                .send()
                .await?;
        }
        log::error!("failed to setup singleton listen server");
        bail!("app exists");
    }
    Ok(())
}

/// The embed server only be used to implement singleton process
/// maybe it can be used as pac server later
pub fn embed_server() {
    let (shutdown_tx, shutdown_rx) = oneshot::channel();
    #[allow(clippy::expect_used)]
    SHUTDOWN_SENDER
        .set(Mutex::new(Some(shutdown_tx)))
        .expect("failed to set shutdown signal for embedded server");
    let port = IVerge::get_singleton_port();

    let _ = AsyncHandler::spawn(move || async move {
        let visible = warp::path!("commands" / "visible").and_then(|| async {
            logging!(info, Type::Window, "检测到从单例模式恢复应用窗口");
            if !lightweight::exit_lightweight_mode().await {
                WindowManager::show_main_window().await;
            } else {
                logging!(error, Type::Window, "轻量模式退出失败，无法恢复应用窗口");
            };
            Ok::<_, warp::Rejection>(warp::reply::with_status::<String>(
                "ok".into(),
                warp::http::StatusCode::OK,
            ))
        });

        let verge_config = Config::verge().await;
        let clash_config = Config::clash().await;

        let pac_content = verge_config
            .latest_ref()
            .pac_file_content
            .clone()
            .unwrap_or(DEFAULT_PAC.into());

        let pac_port = verge_config
            .latest_ref()
            .verge_mixed_port
            .unwrap_or(clash_config.latest_ref().get_mixed_port());

        let pac = warp::path!("commands" / "pac").map(move || {
            let processed_content = pac_content.replace("%mixed-port%", &format!("{pac_port}"));
            warp::http::Response::builder()
                .header("Content-Type", "application/x-ns-proxy-autoconfig")
                .body(processed_content)
                .unwrap_or_default()
        });

        // Use map instead of and_then to avoid Send issues
        let scheme = warp::path!("commands" / "scheme")
            .and(warp::query::<QueryParam>())
            .map(|query: QueryParam| {
                let param = query.param.clone();
                let _ = tokio::task::spawn_local(async move {
                    logging_error!(Type::Setup, resolve::resolve_scheme(param).await);
                });
                warp::reply::with_status::<String>("ok".into(), warp::http::StatusCode::OK)
            });

        let commands = visible.or(scheme).or(pac);
        warp::serve(commands)
            .bind(([127, 0, 0, 1], port))
            .await
            .graceful(async {
                shutdown_rx.await.ok();
            })
            .run()
            .await;
    });
}

pub fn shutdown_embedded_server() {
    log::info!("shutting down embedded server");
    if let Some(sender) = SHUTDOWN_SENDER.get()
        && let Some(sender) = sender.lock().take()
    {
        sender.send(()).ok();
    }
}
