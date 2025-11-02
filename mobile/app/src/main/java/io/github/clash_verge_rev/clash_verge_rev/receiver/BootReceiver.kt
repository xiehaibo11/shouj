package io.github.clash_verge_rev.clash_verge_rev.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 开机自启 BroadcastReceiver
 * 监听系统启动事件，自动启动 VPN 服务
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "Device boot completed")
                // TODO: 检查用户设置是否启用开机自启
                // val autoStart = PreferenceManager.getDefaultSharedPreferences(context)
                //     .getBoolean("auto_start", false)
                // if (autoStart) {
                //     startVpnService(context)
                // }
            }
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.i(TAG, "Quick boot power on")
                // 某些设备的快速启动
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.i(TAG, "Package replaced (app updated)")
                // 应用更新后
            }
        }
    }

    /**
     * 启动 VPN 服务
     */
    private fun startVpnService(context: Context) {
        try {
            // TODO: 实现自动启动逻辑
            // val intent = Intent(context, ClashVpnService::class.java)
            // intent.action = ClashVpnService.ACTION_START
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //     context.startForegroundService(intent)
            // } else {
            //     context.startService(intent)
            // }
            Log.i(TAG, "VPN service start requested")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN service", e)
        }
    }
}
