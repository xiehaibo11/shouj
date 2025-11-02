package io.github.clash_verge_rev.clash_verge_rev.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.clash_verge_rev.clash_verge_rev.ClashVergeApp
import io.github.clash_verge_rev.clash_verge_rev.MainActivity
import io.github.clash_verge_rev.clash_verge_rev.R
import io.github.clash_verge_rev.clash_verge_rev.core.ClashCore
import kotlinx.coroutines.*
import java.io.File
import java.io.FileDescriptor
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * Clash VPN Service
 * 基于 Android VpnService API 的透明代理服务
 * 集成 Mihomo 核心实现完整的代理功能
 */
class ClashVpnService : VpnService() {

    companion object {
        private const val TAG = "ClashVpnService"
        
        const val ACTION_START = "io.github.clash_verge_rev.START_VPN"
        const val ACTION_STOP = "io.github.clash_verge_rev.STOP_VPN"
        const val ACTION_RESTART = "io.github.clash_verge_rev.RESTART_VPN"
        
        private const val VPN_MTU = 9000
        private const val VPN_ADDRESS = "172.19.0.1"
        private const val VPN_ROUTE = "0.0.0.0"
        private const val VPN_DNS = "8.8.8.8"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private var coreInitialized = false

    private var configPath: String? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                configPath = intent.getStringExtra("config_path")
                startVpn()
            }
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
        if (isRunning) {
            Log.w(TAG, "VPN is already running")
            return
        }
        
        try {
            Log.i(TAG, "Starting VPN service...")
            
            // 初始化 Clash 核心
            if (!coreInitialized) {
                Log.i(TAG, "Initializing Clash Core...")
                ClashCore.init(this)
                
                // 加载默认配置或上次使用的配置
                loadClashConfig()
                
                coreInitialized = true
            }
            
            // 创建前台通知
            startForeground(ClashVergeApp.NOTIFICATION_ID_VPN, createNotification())
            
            // 建立 VPN 接口
            vpnInterface = establishVpnInterface()
            
            if (vpnInterface != null) {
                // 获取 VPN 文件描述符
                val fd = vpnInterface!!.fd
                
                // 将 VPN fd 传递给 Clash 核心
                val result = ClashCore.startTun(fd, VPN_MTU)
                
                if (result == 0) {
                    isRunning = true
                    Log.i(TAG, "VPN started successfully")
                    
                    // 通知前端 VPN 已启动
                    broadcastVpnStatus(true)
                } else {
                    Log.e(TAG, "Failed to start TUN in core: $result")
                    vpnInterface?.close()
                    vpnInterface = null
                    stopSelf()
                }
            } else {
                Log.e(TAG, "Failed to establish VPN interface")
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN", e)
            stopSelf()
        }
    }
    
    /**
     * 加载 Clash 配置
     */
    private fun loadClashConfig() {
        try {
            // 如果有传入的配置路径，使用传入的路径
            val configFile = if (!configPath.isNullOrEmpty()) {
                File(configPath!!)
            } else {
                // 否则尝试从SettingsManager读取
                val settingsManager = io.github.clash_verge_rev.clash_verge_rev.data.SettingsManager.getInstance(this)
                val currentConfigPath = settingsManager.currentConfigPath.value
                if (currentConfigPath.isNotEmpty()) {
                    File(currentConfigPath)
                } else {
                    // 最后尝试默认路径
                    val configDir = File(filesDir, "config")
                    File(configDir, "config.yaml")
                }
            }
            
            if (configFile.exists()) {
                Log.i(TAG, "Loading config from: ${configFile.absolutePath}")
                val result = ClashCore.loadConfig(configFile)
                
                if (result == 0) {
                    Log.i(TAG, "Config loaded successfully")
                } else {
                    Log.w(TAG, "Failed to load config: $result, using default")
                    createDefaultConfig(configFile)
                    ClashCore.loadConfig(configFile)
                }
            } else {
                Log.i(TAG, "Config file not found, creating default config")
                createDefaultConfig(configFile)
                ClashCore.loadConfig(configFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading config", e)
        }
    }
    
    /**
     * 创建默认配置
     */
    private fun createDefaultConfig(configFile: File) {
        val defaultConfig = """
            # Clash Verge Rev 默认配置
            mixed-port: 7897
            allow-lan: false
            mode: rule
            log-level: info
            ipv6: true
            
            dns:
              enable: true
              listen: 0.0.0.0:1053
              enhanced-mode: fake-ip
              nameserver:
                - 8.8.8.8
                - 1.1.1.1
            
            proxies:
              - name: "DIRECT"
                type: direct
            
            proxy-groups:
              - name: "PROXY"
                type: select
                proxies:
                  - DIRECT
            
            rules:
              - MATCH,PROXY
        """.trimIndent()
        
        configFile.parentFile?.mkdirs()
        configFile.writeText(defaultConfig)
        Log.i(TAG, "Default config created")
    }

    /**
     * 停止 VPN
     */
    private fun stopVpn() {
        Log.i(TAG, "Stopping VPN service...")
        isRunning = false
        
        try {
            // 停止 Clash 核心 TUN
            if (coreInitialized) {
                ClashCore.stopTun()
            }
            
            // 关闭 VPN 接口
            vpnInterface?.close()
            vpnInterface = null
            
            // 取消协程
            serviceScope.cancel()
            
            // 通知前端 VPN 已停止
            broadcastVpnStatus(false)
            
            // 停止前台服务
            stopForeground(true)
            stopSelf()
            
            Log.i(TAG, "VPN stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN", e)
        }
    }

    /**
     * 建立 VPN 接口
     */
    private fun establishVpnInterface(): ParcelFileDescriptor? {
        val builder = Builder()
        
        // 配置 VPN 参数
        builder.setMtu(VPN_MTU)
        builder.addAddress(VPN_ADDRESS, 30)
        builder.addRoute(VPN_ROUTE, 0)
        
        // 配置 DNS 服务器
        builder.addDnsServer("8.8.8.8")
        builder.addDnsServer("1.1.1.1")
        
        // 设置会话名称
        builder.setSession(getString(R.string.app_name))
        
        // 允许的应用（默认全局代理）
        // 可以通过配置文件指定
        // builder.addAllowedApplication("package.name")
        
        // 排除的应用（比如排除自己避免循环）
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to disallow self", e)
        }
        
        // 配置 HTTP 代理（Android 10+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val proxyInfo = android.net.ProxyInfo.buildDirectProxy(
                    "127.0.0.1",
                    7897  // Mihomo mixed port
                )
                builder.setHttpProxy(proxyInfo)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set HTTP proxy", e)
            }
        }
        
        return builder.establish()
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

