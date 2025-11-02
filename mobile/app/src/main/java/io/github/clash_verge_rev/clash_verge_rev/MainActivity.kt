package io.github.clash_verge_rev.clash_verge_rev

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import io.github.clash_verge_rev.clash_verge_rev.service.ClashVpnService

/**
 * Clash Verge Rev MainActivity
 * Tauri WebView 主 Activity
 */
class MainActivity : TauriActivity() {

    companion object {
        private const val VPN_REQUEST_CODE = 100
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // VPN 权限已授予，启动服务
            startVpnService()
        } else {
            // 用户拒绝了 VPN 权限
            onVpnPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 处理 Deep Link
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleDeepLink(it) }
    }

    /**
     * 处理 Deep Link（订阅导入等）
     */
    private fun handleDeepLink(intent: Intent) {
        val data = intent.data ?: return
        val scheme = data.scheme ?: return
        
        when (scheme) {
            "clash", "clash-verge" -> {
                // 处理 clash:// 或 clash-verge:// 链接
                val url = data.toString()
                // TODO: 通知 WebView 处理订阅导入
                runOnUiThread {
                    evaluateJavascript(
                        "window.dispatchEvent(new CustomEvent('deeplink', { detail: '$url' }))"
                    )
                }
            }
            "http", "https" -> {
                // 处理配置文件链接
                if (data.path?.endsWith(".yaml") == true || data.path?.endsWith(".yml") == true) {
                    val url = data.toString()
                    runOnUiThread {
                        evaluateJavascript(
                            "window.dispatchEvent(new CustomEvent('import-profile', { detail: '$url' }))"
                        )
                    }
                }
            }
        }
    }

    /**
     * 请求 VPN 权限
     */
    fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            // 已经有权限，直接启动
            startVpnService()
        }
    }

    /**
     * 启动 VPN 服务
     */
    private fun startVpnService() {
        val intent = Intent(this, ClashVpnService::class.java)
        intent.action = ClashVpnService.ACTION_START
        startService(intent)
    }

    /**
     * 停止 VPN 服务
     */
    fun stopVpnService() {
        val intent = Intent(this, ClashVpnService::class.java)
        intent.action = ClashVpnService.ACTION_STOP
        startService(intent)
    }

    /**
     * VPN 权限被拒绝时的处理
     */
    private fun onVpnPermissionDenied() {
        runOnUiThread {
            evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('vpn-permission-denied'))"
            )
        }
    }

    /**
     * 执行 JavaScript 代码
     */
    private fun evaluateJavascript(script: String) {
        // TODO: 通过 Tauri 的 API 执行 JavaScript
    }
}

