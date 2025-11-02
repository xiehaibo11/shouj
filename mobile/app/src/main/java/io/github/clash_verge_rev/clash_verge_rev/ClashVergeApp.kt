package io.github.clash_verge_rev.clash_verge_rev

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Clash Verge Rev Application
 * 应用入口类
 */
class ClashVergeApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_VPN = "vpn_service_channel"
        const val NOTIFICATION_CHANNEL_UPDATE = "update_channel"
        const val NOTIFICATION_ID_VPN = 1001
        
        lateinit var instance: ClashVergeApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化通知渠道
        createNotificationChannels()
        
        // 初始化配置目录
        initDirectories()
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // VPN 服务通知渠道
            val vpnChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_VPN,
                getString(R.string.notification_channel_vpn),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_vpn_desc)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(vpnChannel)
            
            // 更新通知渠道
            val updateChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_UPDATE,
                getString(R.string.notification_channel_update),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_update_desc)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(updateChannel)
        }
    }

    /**
     * 初始化应用所需目录
     */
    private fun initDirectories() {
        val directories = listOf(
            filesDir.resolve("profiles"),
            filesDir.resolve("config"),
            filesDir.resolve("cache"),
            filesDir.resolve("logs"),
            filesDir.resolve("mihomo")
        )
        
        directories.forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }
}

