package io.github.clashverge.mobile.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.clashverge.mobile.MainActivity
import io.github.clashverge.mobile.R
import java.io.IOException

class ClashVpnService : VpnService() {
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    
    companion object {
        private const val TAG = "ClashVpnService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "clash_vpn_channel"
        
        const val ACTION_START = "io.github.clashverge.ACTION_START"
        const val ACTION_STOP = "io.github.clashverge.ACTION_STOP"
        
        const val EXTRA_SERVER_ADDRESS = "server_address"
        const val EXTRA_SERVER_PORT = "server_port"
        const val EXTRA_DNS = "dns"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "VPN Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val serverAddress = intent.getStringExtra(EXTRA_SERVER_ADDRESS) ?: "127.0.0.1"
                val serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 7897)
                val dns = intent.getStringArrayExtra(EXTRA_DNS) ?: arrayOf("8.8.8.8", "8.8.4.4")
                
                startVPN(serverAddress, serverPort, dns)
            }
            ACTION_STOP -> {
                stopVPN()
            }
        }
        
        return START_STICKY
    }
    
    private fun startVPN(serverAddress: String, serverPort: Int, dns: Array<String>) {
        if (isRunning) {
            Log.w(TAG, "VPN is already running")
            return
        }
        
        try {
            // 建立VPN连接
            val builder = Builder()
                .setSession("Clash Verge")
                .addAddress("172.19.0.1", 30)
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)
                .setBlocking(false)
            
            // 添加DNS服务器
            dns.forEach { dnsServer ->
                builder.addDnsServer(dnsServer)
            }
            
            // 排除本地网络
            builder.addDisallowedApplication(packageName)
            
            vpnInterface = builder.establish()
            
            if (vpnInterface != null) {
                isRunning = true
                
                // 启动前台通知
                startForeground(NOTIFICATION_ID, createNotification())
                
                Log.i(TAG, "VPN started successfully")
                
                // TODO: 启动代理核心进程
                startProxyCore(serverAddress, serverPort)
            } else {
                Log.e(TAG, "Failed to establish VPN")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN", e)
            stopVPN()
        }
    }
    
    private fun stopVPN() {
        if (!isRunning) {
            Log.w(TAG, "VPN is not running")
            return
        }
        
        try {
            vpnInterface?.close()
            vpnInterface = null
            isRunning = false
            
            // TODO: 停止代理核心进程
            stopProxyCore()
            
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            
            Log.i(TAG, "VPN stopped successfully")
        } catch (e: IOException) {
            Log.e(TAG, "Error stopping VPN", e)
        }
    }
    
    private fun startProxyCore(serverAddress: String, serverPort: Int) {
        // TODO: 启动Mihomo核心进程
        // 这里需要通过JNI或ProcessBuilder启动native二进制文件
        Log.d(TAG, "Starting proxy core at $serverAddress:$serverPort")
    }
    
    private fun stopProxyCore() {
        // TODO: 停止Mihomo核心进程
        Log.d(TAG, "Stopping proxy core")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Clash VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Clash VPN Service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Clash Verge")
            .setContentText("VPN已连接")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopVPN()
        Log.d(TAG, "VPN Service destroyed")
    }
}

