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
import io.github.clash_verge_rev.clash_verge_rev.core.ProxyApiServer
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
    
    // ✅ TUN数据包处理器（Kotlin流量转发）
    private var tunPacketHandler: io.github.clash_verge_rev.clash_verge_rev.core.TunPacketHandler? = null
    
    // 统计管理器
    private val trafficStatsManager = io.github.clash_verge_rev.clash_verge_rev.core.TrafficStatsManager.getInstance()
    private val connectionTracker = io.github.clash_verge_rev.clash_verge_rev.core.ConnectionTracker.getInstance()
    
    // 统计更新任务
    private var statsUpdateJob: Job? = null
    
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
            
            // ✅ 加载配置（不依赖JNI）
            if (!coreInitialized) {
                Log.i(TAG, "Loading configuration (Kotlin mode)...")
                loadClashConfig()
                coreInitialized = true
                Log.i(TAG, "✅ Configuration loaded")
                
                // ✅ 验证并选择有效的代理节点（关键修复）
                verifyAndSelectProxy()
            }
            
            // 创建前台通知
            startForeground(ClashVergeApp.NOTIFICATION_ID_VPN, createNotification())
            
            // 建立 VPN 接口
            vpnInterface = establishVpnInterface()
            
            if (vpnInterface != null) {
                // 获取文件描述符（先不 detach，等确认成功后再转移所有权）
                val fd = vpnInterface!!.fd
                
                // ✅ 让 Mihomo Go core 直接处理 TUN（像桌面端一样）
                Log.i(TAG, "🚀 Starting Mihomo TUN integration...")
                Log.i(TAG, "   - fd: $fd")
                Log.i(TAG, "   - MTU: $VPN_MTU")
                
                // 调用 Go 核心启动 TUN
                val tunResult = io.github.clash_verge_rev.clash_verge_rev.core.ClashCore.startTun(fd, VPN_MTU)
                
                if (tunResult == 0) {
                    // ✅ Mihomo TUN 启动成功 - 现在转移 fd 所有权给 Go 层
                    // 使用 detachFd() 避免 Android 框架关闭这个 fd（防止 fdsan 错误）
                    vpnInterface!!.detachFd()
                    vpnInterface = null  // 立即设置为 null，避免后续误关闭
                    
                    Log.i(TAG, "✅ VPN interface established (Mihomo Go mode)")
                    Log.i(TAG, "   - VPN fd: $fd (ownership transferred to Go)")
                    Log.i(TAG, "   - MTU: $VPN_MTU")
                    Log.i(TAG, "   - API Server: http://127.0.0.1:9090")
                    Log.i(TAG, "   - TUN Handler: Mihomo Go Core ✅")
                    Log.i(TAG, "   → All traffic will be handled by Mihomo (like desktop version)")
                    
                    // ✅ 验证 Mihomo 核心状态（关键修复）
                    if (!verifyMihomoCore()) {
                        Log.e(TAG, "❌ Mihomo core verification failed, cleaning up")
                        // 直接清理资源，不调用 stopVpn()（避免重复关闭 fd）
                        io.github.clash_verge_rev.clash_verge_rev.core.ClashCore.stopTun()
                        stopSelf()
                        return
                    }
                    
                    isRunning = true
                    
                    // ✅ 启动统计更新任务（每秒更新一次）
                    startStatsUpdateJob()
                    
                    // 通知前端 VPN 已启动
                    broadcastVpnStatus(true)
                } else {
                    // ❌ Mihomo TUN 启动失败，回退到 Kotlin 模式
                    // 注意：没有 detachFd()，所以 vpnInterface 仍然有效
                    Log.w(TAG, "⚠️ Mihomo TUN failed (code: $tunResult), falling back to Kotlin mode")
                    Log.w(TAG, "  This is a fallback - traffic won't be forwarded!")
                    
                    tunPacketHandler = io.github.clash_verge_rev.clash_verge_rev.core.TunPacketHandler(
                        vpnFd = vpnInterface!!,
                        mtu = VPN_MTU,
                        mixedProxyPort = 7897
                    )
                    tunPacketHandler?.start()
                    
                    isRunning = true
                    Log.i(TAG, "✅ VPN interface established (Kotlin fallback mode)")
                    Log.i(TAG, "   - VPN fd: $fd")
                    Log.i(TAG, "   - MTU: $VPN_MTU")
                    Log.i(TAG, "   - TUN Handler: Kotlin (Logging only)")
                    
                    startStatsUpdateJob()
                    broadcastVpnStatus(true)
                }
            } else {
                Log.e(TAG, "❌ Failed to establish VPN interface")
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
                Log.i(TAG, "📄 Loading config from: ${configFile.absolutePath}")
                
                // ✅ 1. 加载到 Mihomo Go 核心（用于 TUN）
                val loadResult = io.github.clash_verge_rev.clash_verge_rev.core.ClashCore.loadConfig(configFile)
                if (loadResult == 0) {
                    Log.i(TAG, "✅ Config loaded to Mihomo Go core")
                } else {
                    Log.w(TAG, "⚠️ Failed to load config to Go core (code: $loadResult), using Kotlin API")
                }
                
                // ✅ 2. 加载到 Kotlin HTTP API 服务器（用于代理切换等）
                ProxyApiServer.getInstance()?.loadConfigFromFile(configFile.absolutePath)
                Log.i(TAG, "✅ Config loaded to Kotlin API Server")
            } else {
                Log.w(TAG, "Config file not found: ${configFile.absolutePath}, creating default config")
                createDefaultConfig(configFile)
                ProxyApiServer.getInstance()?.loadConfigFromFile(configFile.absolutePath)
                io.github.clash_verge_rev.clash_verge_rev.core.ClashCore.loadConfig(configFile)
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
            external-controller: 127.0.0.1:9090
            secret: ""
            
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
        Log.i(TAG, "Default config created with external-controller")
    }

    /**
     * 停止 VPN
     */
    private fun stopVpn() {
        Log.i(TAG, "Stopping VPN service...")
        isRunning = false
        
        try {
            // ✅ 停止统计更新任务
            statsUpdateJob?.cancel()
            statsUpdateJob = null
            Log.i(TAG, "Stats update job stopped")
            
            // ✅ 停止 Mihomo TUN
            io.github.clash_verge_rev.clash_verge_rev.core.ClashCore.stopTun()
            Log.i(TAG, "Mihomo TUN stopped")
            
            // ✅ 停止TUN数据包处理器（如果有）
            tunPacketHandler?.stop()
            tunPacketHandler = null
            Log.i(TAG, "TUN packet handler stopped")
            
            // ✅ 释放 VPN 接口
            // 如果是 Go TUN 模式，fd 已通过 detachFd() 转移，这里只是释放引用
            // 如果是 fallback 模式，fd 没有 detach，需要正常关闭
            try {
                vpnInterface?.close()
                Log.i(TAG, "VPN interface closed")
            } catch (e: Exception) {
                // 如果 fd 已经 detached，close() 可能失败，这是正常的
                Log.d(TAG, "VPN interface close (fd already detached or closed): ${e.message}")
            }
            vpnInterface = null
            
            // ✅ 清除连接跟踪
            connectionTracker.clearAll()
            
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
     * 启动统计更新任务
     */
    private fun startStatsUpdateJob() {
        statsUpdateJob = serviceScope.launch {
            while (isActive && isRunning) {
                try {
                    // ✅ 更新流量统计
                    trafficStatsManager.updateStats()
                    
                    // ✅ 更新所有连接的实时速度
                    connectionTracker.updateAllSpeeds()
                    
                    // ✅ 清理过期连接（每分钟执行一次）
                    if (System.currentTimeMillis() % 60000 < 1000) {
                        connectionTracker.cleanupStaleConnections()
                    }
                    
                    // 每秒更新一次
                    delay(1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in stats update job", e)
                }
            }
        }
        Log.i(TAG, "✅ Stats update job started")
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
        
        // ⚠️ 注意：TUN 模式下不应设置 HTTP 代理
        // TUN 模式是透明代理，所有流量在 IP 层被 Mihomo 接管
        // 设置 HTTP 代理会导致应用尝试连接不存在的代理端口
        // 参考：https://github.com/MetaCubeX/mihomo/issues/XXX
        //
        // ❌ 错误做法（会导致 ERR_PROXY_CONNECTION_FAILED）：
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        //     val proxyInfo = android.net.ProxyInfo.buildDirectProxy("127.0.0.1", 7897)
        //     builder.setHttpProxy(proxyInfo)
        // }
        
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
    
    /**
     * 验证Mihomo核心是否正常运行
     * 检查HTTP API、DNS、代理端口
     */
    private fun verifyMihomoCore(): Boolean {
        try {
            Log.i(TAG, "Verifying Mihomo core status...")
            
            // 1. 检查HTTP API端口 (9090) - 必须可用
            val apiAvailable = checkPort("127.0.0.1", 9090, 3000)
            if (!apiAvailable) {
                Log.e(TAG, "❌ HTTP API (9090) not responding")
                return false
            }
            Log.i(TAG, "✅ HTTP API available")
            
            // 2. 检查Mixed代理端口 (7897) - TUN模式下可选
            // 在TUN模式下，所有流量通过TUN接口处理，不需要通过proxy port
            val proxyAvailable = checkPort("127.0.0.1", 7897, 1000)
            if (proxyAvailable) {
                Log.i(TAG, "✅ Mixed proxy port available")
            } else {
                Log.w(TAG, "⚠️ Mixed proxy port (7897) not responding (OK in TUN mode)")
            }
            
            // 3. 验证API响应 - 必须成功
            val versionResponse = queryMihomoVersion()
            if (versionResponse == null) {
                Log.e(TAG, "❌ Failed to query Mihomo version")
                return false
            }
            Log.i(TAG, "✅ Mihomo version: $versionResponse")
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying Mihomo core", e)
            return false
        }
    }
    
    /**
     * 检查端口是否可访问
     */
    private fun checkPort(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(host, port), timeoutMs)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 查询Mihomo版本（测试API连通性）
     */
    private fun queryMihomoVersion(): String? {
        return try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            val request = okhttp3.Request.Builder()
                .url("http://127.0.0.1:9090/version")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            response.close()
            
            body
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query version", e)
            null
        }
    }
    
    /**
     * 验证并选择有效的代理节点
     * 确保TUN模式启动时有可用的代理
     */
    private fun verifyAndSelectProxy() {
        try {
            Log.i(TAG, "Verifying proxy selection...")
            
            // 1. 获取当前配置文件
            val settingsManager = io.github.clash_verge_rev.clash_verge_rev.data.SettingsManager.getInstance(this)
            val currentConfigPath = settingsManager.currentConfigPath.value
            
            if (currentConfigPath.isEmpty()) {
                Log.w(TAG, "⚠️ No config file selected")
                return
            }
            
            val configFile = File(currentConfigPath)
            if (!configFile.exists()) {
                Log.w(TAG, "⚠️ Config file not found: $currentConfigPath")
                return
            }
            
            // 2. 加载代理信息
            val proxyRepository = io.github.clash_verge_rev.clash_verge_rev.data.ProxyRepository.getInstance(this)
            val proxiesState = kotlinx.coroutines.runBlocking {
                proxyRepository.loadProxiesFromConfig(configFile)
            }
            
            if (proxiesState.groups.isEmpty()) {
                Log.w(TAG, "⚠️ No proxy groups found in config")
                return
            }
            
            // 3. 检查第一个代理组的选中节点
            val firstGroup = proxiesState.groups.first()
            val currentProxy = firstGroup.now
            
            if (currentProxy.isEmpty() || currentProxy == "DIRECT") {
                Log.w(TAG, "⚠️ No valid proxy selected, current: $currentProxy")
                
                // 4. 自动选择第一个非DIRECT节点
                val validProxy = firstGroup.all.firstOrNull { it != "DIRECT" && it != "REJECT" }
                
                if (validProxy != null) {
                    Log.i(TAG, "🔄 Auto-selecting proxy: ${firstGroup.name} -> $validProxy")
                    
                    kotlinx.coroutines.runBlocking {
                        val success = proxyRepository.switchProxy(
                            firstGroup.name,
                            validProxy,
                            currentConfigPath
                        )
                        
                        if (success) {
                            Log.i(TAG, "✅ Proxy selected successfully")
                        } else {
                            Log.e(TAG, "❌ Failed to select proxy")
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ No valid proxy nodes available")
                }
            } else {
                Log.i(TAG, "✅ Proxy already selected: ${firstGroup.name} -> $currentProxy")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying proxy selection", e)
        }
    }
}

