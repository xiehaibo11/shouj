package io.github.clash_verge_rev.clash_verge_rev.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.clash_verge_rev.clash_verge_rev.service.ClashVpnService

/**
 * Boot Receiver
 * 开机自启接收器
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val PREF_AUTO_START = "auto_start_on_boot"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Device boot completed")
            
            // 检查是否启用开机自启
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean(PREF_AUTO_START, false)
            
            if (autoStart) {
                Log.d(TAG, "Auto start enabled, starting VPN service")
                
                // 启动 VPN 服务
                val serviceIntent = Intent(context, ClashVpnService::class.java)
                serviceIntent.action = ClashVpnService.ACTION_START
                context.startService(serviceIntent)
            }
        }
    }
}

