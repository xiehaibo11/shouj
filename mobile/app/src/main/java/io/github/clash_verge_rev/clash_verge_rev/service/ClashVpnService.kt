package io.github.clash_verge_rev.clash_verge_rev.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import io.github.clash_verge_rev.clash_verge_rev.ClashVergeApp
import io.github.clash_verge_rev.clash_verge_rev.MainActivity
import io.github.clash_verge_rev.clash_verge_rev.R
import kotlinx.coroutines.*
import java.io.File
import java.io.FileDescriptor
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * Clash VPN Service
 * 基于 Android VpnService API 的透明代理服务
 */
class ClashVpnService : VpnService() {

    companion object {
        const val ACTION_START = "io.github.clash_verge_rev.START_VPN"
        const val ACTION_STOP = "io.github.clash_verge_rev.STOP_VPN"
        const val ACTION_RESTART = "io.github.clash_verge_rev.RESTART_VPN"
        
        private const val VPN_MTU = 1500
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ROUTE = "0.0.0.0"
        private const val VPN_DNS = "8.8.8.8"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
            ACTION_RESTART -> {
                stopVpn()
                startVpn()
            }
        }
        return START_STICKY
    }

    /**
     * 启动 VPN
     */
    private fun startVpn() {
        if (isRunning) return
        
        try {
            // 创建前台通知
            startForeground(ClashVergeApp.NOTIFICATION_ID_VPN, createNotification())
            
            // 建立 VPN 接口
            vpnInterface = establishVpnInterface()
            
            if (vpnInterface != null) {
                isRunning = true
                
                // 启动数据包处理协程
                serviceScope.launch {
                    processPackets()
                }
                
                // 通知前端 VPN 已启动
                broadcastVpnStatus(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    /**
     * 停止 VPN
     */
    private fun stopVpn() {
        isRunning = false
        
        try {
            vpnInterface?.close()
            vpnInterface = null
            
            serviceScope.cancel()
            
            // 通知前端 VPN 已停止
            broadcastVpnStatus(false)
            
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 建立 VPN 接口
     */
    private fun establishVpnInterface(): ParcelFileDescriptor? {
        val builder = Builder()
        
        // 配置 VPN 参数
        builder.setMtu(VPN_MTU)
        builder.addAddress(VPN_ADDRESS, 24)
        builder.addRoute(VPN_ROUTE, 0)
        builder.addDnsServer(VPN_DNS)
        
        // 设置会话名称
        builder.setSession(getString(R.string.app_name))
        
        // 允许的应用（默认全局代理）
        // builder.addAllowedApplication("package.name")
        
        // 排除的应用
        // builder.addDisallowedApplication("package.name")
        
        // 配置 HTTP 代理（如果支持）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val proxyInfo = android.net.ProxyInfo.buildDirectProxy(
                "127.0.0.1",
                7897  // Mihomo mixed port
            )
            builder.setHttpProxy(proxyInfo)
        }
        
        return builder.establish()
    }

    /**
     * 处理数据包
     */
    private suspend fun processPackets() {
        val fd = vpnInterface?.fileDescriptor ?: return
        val inputStream = ParcelFileDescriptor.AutoCloseInputStream(vpnInterface)
        val outputStream = ParcelFileDescriptor.AutoCloseOutputStream(vpnInterface)
        
        val buffer = ByteBuffer.allocate(32767)
        
        while (isRunning && !serviceScope.isActive.not()) {
            try {
                // 读取数据包
                val length = inputStream.read(buffer.array())
                if (length > 0) {
                    buffer.limit(length)
                    
                    // 处理数据包
                    // TODO: 将数据包转发给 Mihomo 核心处理
                    processPacket(buffer)
                    
                    buffer.clear()
                }
                
                // 避免 CPU 占用过高
                delay(1)
            } catch (e: Exception) {
                if (isRunning) {
                    e.printStackTrace()
                }
                break
            }
        }
    }

    /**
     * 处理单个数据包
     */
    private fun processPacket(packet: ByteBuffer) {
        // TODO: 实现数据包处理逻辑
        // 1. 解析 IP 数据包
        // 2. 根据规则决定是否代理
        // 3. 转发到 Mihomo 核心
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, ClashVergeApp.NOTIFICATION_CHANNEL_VPN)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * 广播 VPN 状态变化
     */
    private fun broadcastVpnStatus(isConnected: Boolean) {
        val intent = Intent("io.github.clash_verge_rev.VPN_STATUS")
        intent.putExtra("connected", isConnected)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }
}

