package io.github.clash_verge_rev.clash_verge_rev

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File

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
        
        // ✅ 初始化错误处理器（必须最先初始化）
        io.github.clash_verge_rev.clash_verge_rev.utils.ErrorHandler.initialize(this)
        
        // 测试库加载
        testNativeLibraries()
        
        // ✅ 初始化 Mihomo 核心（必须在使用前初始化）
        android.util.Log.i("ClashVergeApp", "🚀 Initializing Mihomo Go core...")
        io.github.clash_verge_rev.clash_verge_rev.core.ClashCore.init(this)
        
        // 初始化通知渠道
        createNotificationChannels()
        
        // 初始化配置目录
        initDirectories()
        
        // ✅ 启动HTTP API服务器（应用启动时就启动，不等VPN）
        startApiServer()
        
        io.github.clash_verge_rev.clash_verge_rev.utils.ErrorHandler.logInfo(
            "ClashVergeApp", 
            "Application initialized successfully"
        )
    }
    
    /**
     * 启动HTTP API服务器
     * 关键：在应用启动时就启动，而不是等待VPN启动
     */
    private fun startApiServer() {
        try {
            io.github.clash_verge_rev.clash_verge_rev.core.ProxyApiServer.start(9090)
            android.util.Log.i("ClashVergeApp", "✅ HTTP API Server started on port 9090")
            
            // 如果已有配置文件，自动加载
            val settingsManager = io.github.clash_verge_rev.clash_verge_rev.data.SettingsManager.getInstance(this)
            val currentConfigPath = settingsManager.currentConfigPath.value
            if (currentConfigPath.isNotEmpty() && File(currentConfigPath).exists()) {
                io.github.clash_verge_rev.clash_verge_rev.core.ProxyApiServer.getInstance()?.loadConfigFromFile(currentConfigPath)
                android.util.Log.i("ClashVergeApp", "✅ Auto-loaded config: $currentConfigPath")
            }
        } catch (e: Exception) {
            android.util.Log.e("ClashVergeApp", "❌ Failed to start API server", e)
        }
    }
    
    private fun testNativeLibraries() {
        android.util.Log.i("ClashVergeApp", "========== Testing Native Library ==========")
        
        try {
            android.util.Log.i("ClashVergeApp", "Loading libclash.so (Mihomo Go Core)...")
            System.loadLibrary("clash")
            android.util.Log.i("ClashVergeApp", "✅ libclash.so loaded successfully")
            android.util.Log.i("ClashVergeApp", "✅ Direct Go mode enabled (no JNI bridge)")
            android.util.Log.i("ClashVergeApp", "  → TUN traffic will be handled by Mihomo core")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("ClashVergeApp", "❌ Failed to load libclash.so", e)
            android.util.Log.e("ClashVergeApp", "  Error: ${e.message}")
            android.util.Log.e("ClashVergeApp", "  → App will use Kotlin fallback mode")
        }
        android.util.Log.i("ClashVergeApp", "==========================================")
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
            filesDir.resolve("configs"),
            filesDir.resolve("cache"),
            filesDir.resolve("logs"),
            filesDir.resolve("mihomo")
        )
        
        directories.forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
        
        // 创建默认配置文件
        createDefaultConfigFiles()
    }
    
    private fun createDefaultConfigFiles() {
        try {
            // 创建默认 merge.yaml
            val mergeFile = File(filesDir, "merge.yaml")
            if (!mergeFile.exists()) {
                mergeFile.writeText("""# Clash Verge Rev - Merge Configuration
# 全局配置合并文件，用于覆盖订阅配置

# 示例：设置全局DNS
# dns:
#   enable: true
#   nameserver:
#     - 223.5.5.5
#     - 119.29.29.29

# 示例：添加自定义规则
# rules:
#   - DOMAIN-SUFFIX,google.com,PROXY
#   - DOMAIN-SUFFIX,github.com,PROXY
""")
                android.util.Log.i("ClashVergeApp", "✅ Created default merge.yaml")
            }
            
            // 创建默认 script.js
            val scriptFile = File(filesDir, "script.js")
            if (!scriptFile.exists()) {
                scriptFile.writeText("""// Clash Verge Rev - JavaScript Configuration Script
// 使用JavaScript动态修改配置

/**
 * 主函数：处理配置
 * @param {Object} config - 原始配置对象
 * @returns {Object} 修改后的配置对象
 */
function main(config) {
  // 在这里添加您的自定义逻辑
  
  // 示例：添加自定义规则
  // config.rules = config.rules || [];
  // config.rules.unshift('DOMAIN-SUFFIX,example.com,DIRECT');
  
  return config;
}
""")
                android.util.Log.i("ClashVergeApp", "✅ Created default script.js")
            }
        } catch (e: Exception) {
            android.util.Log.e("ClashVergeApp", "Failed to create default config files", e)
        }
    }
}

