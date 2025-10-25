package io.github.clashverge.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import io.github.clashverge.mobile.MainActivity
import io.github.clashverge.mobile.R

class VpnNotificationService(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "clash_vpn_channel"
        private const val CHANNEL_NAME = "Clash VPN"
        const val NOTIFICATION_ID = 1
        
        const val ACTION_TOGGLE_VPN = "io.github.clashverge.ACTION_TOGGLE_VPN"
        const val ACTION_DISCONNECT = "io.github.clashverge.ACTION_DISCONNECT"
    }
    
    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Clash VPN 服务通知"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun buildNotification(
        isConnected: Boolean,
        uploadSpeed: String = "0 B/s",
        downloadSpeed: String = "0 B/s"
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val toggleIntent = Intent(ACTION_TOGGLE_VPN).apply {
            setPackage(context.packageName)
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            toggleIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val disconnectIntent = Intent(ACTION_DISCONNECT).apply {
            setPackage(context.packageName)
        }
        val disconnectPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isConnected) "Clash VPN 已连接" else "Clash VPN 未连接")
            .setContentText(
                if (isConnected) "↑ $uploadSpeed  ↓ $downloadSpeed" 
                else "点击启动 VPN"
            )
            .setContentIntent(pendingIntent)
            .setOngoing(isConnected)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
        
        if (isConnected) {
            builder.addAction(
                R.drawable.ic_notification,
                "断开",
                disconnectPendingIntent
            )
        } else {
            builder.addAction(
                R.drawable.ic_notification,
                "连接",
                togglePendingIntent
            )
        }
        
        return builder.build()
    }
    
    fun updateNotification(
        isConnected: Boolean,
        uploadSpeed: String = "0 B/s",
        downloadSpeed: String = "0 B/s"
    ) {
        val notification = buildNotification(isConnected, uploadSpeed, downloadSpeed)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

