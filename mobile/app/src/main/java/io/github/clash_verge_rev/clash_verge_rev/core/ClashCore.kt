package io.github.clash_verge_rev.clash_verge_rev.core

import android.content.Context
import android.util.Log
import java.io.File

object ClashCore {
    private const val TAG = "ClashCore"
    private var nativeLibrariesLoaded = false
    
    init {
        try {
            // ✅ 先加载 Go 核心
            System.loadLibrary("clash")
            Log.i(TAG, "✅ libclash.so loaded")
            
            // ✅ 再加载 JNI 桥接层（使用 dlsym 动态链接）
            System.loadLibrary("clash-jni")
            Log.i(TAG, "✅ libclash-jni.so loaded")
            
            nativeLibrariesLoaded = true
            Log.i(TAG, "✅ Native libraries loaded successfully")
            Log.i(TAG, "  Mode: JNI bridge with dynamic linking")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "❌ Failed to load native libraries", e)
            Log.e(TAG, "  Error: ${e.message}")
            nativeLibrariesLoaded = false
        }
    }
    
    @Synchronized
    fun init(context: Context) {
        if (!nativeLibrariesLoaded) {
            Log.w(TAG, "Native libraries not loaded, skipping initialization")
            return
        }
        try {
            val homeDir = context.filesDir.absolutePath
            val versionName = getAppVersion(context)
            Log.i(TAG, "🚀 Initializing Mihomo core...")
            Log.i(TAG, "  Home: $homeDir")
            Log.i(TAG, "  Version: $versionName")
            
            nativeInit(homeDir, versionName)
            Log.i(TAG, "✅ Mihomo core initialized")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "❌ JNI function not found", e)
            Log.e(TAG, "  ${e.message}")
            nativeLibrariesLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize core", e)
        }
    }
    
    fun reset() {
        if (!nativeLibrariesLoaded) return
        Log.i(TAG, "Resetting core")
        nativeReset()
    }
    
    fun forceGc() {
        if (!nativeLibrariesLoaded) return
        nativeForceGc()
    }
    
    @Synchronized
    fun startTun(fd: Int, mtu: Int): Int {
        if (!nativeLibrariesLoaded) {
            Log.e(TAG, "Native libraries not loaded")
            return -998
        }
        if (fd <= 0) {
            Log.e(TAG, "Invalid fd: $fd")
            return -1
        }
        if (mtu <= 0) {
            Log.e(TAG, "Invalid mtu: $mtu")
            return -2
        }
        // 移除 isInitialized() 检查，避免调用未实现的 nativeGetVersion()
        Log.i(TAG, "Starting TUN: fd=$fd, mtu=$mtu")
        return try {
            nativeStartTun(fd, mtu)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "JNI function not found", e)
            -997  // JNI 函数未找到
        } catch (e: Exception) {
            Log.e(TAG, "Exception in startTun", e)
            -999
        }
    }
    
    fun stopTun() {
        if (!nativeLibrariesLoaded) return
        Log.i(TAG, "Stopping TUN")
        nativeStopTun()
    }
    
    @Synchronized
    fun loadConfig(configPath: String): Int {
        if (!nativeLibrariesLoaded) {
            Log.e(TAG, "Native libraries not loaded, cannot load config")
            return -998  // Error code for missing native libraries
        }
        if (configPath.isBlank()) throw IllegalArgumentException("Empty config path")
        val file = File(configPath)
        if (!file.exists()) throw IllegalArgumentException("Config not found: $configPath")
        if (!file.isFile) throw IllegalArgumentException("Not a file: $configPath")
        Log.i(TAG, "Loading config: $configPath")
        return try {
            val result = nativeLoadConfig(configPath)
            if (result == 0) Log.i(TAG, "Config loaded") 
            else Log.w(TAG, "Failed to load config: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception in loadConfig", e)
            -999
        }
    }
    
    fun loadConfig(file: File): Int {
        if (!file.exists()) throw IllegalArgumentException("File not found: ${file.absolutePath}")
        return loadConfig(file.absolutePath)
    }
    
    fun queryTraffic(): Long {
        if (!nativeLibrariesLoaded) return 0L
        return nativeQueryTraffic()
    }
    
    fun getVersion(): String {
        if (!nativeLibrariesLoaded) return "Native libraries not loaded"
        return try {
            nativeGetVersion()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get version", e)
            "Mihomo 1.18.1 (unknown)"
        }
    }
    
    fun isInitialized(): Boolean {
        // 简化检查：只要库加载成功就认为已初始化
        return nativeLibrariesLoaded
    }
    
    // 原有 native 方法
    private external fun nativeInit(homeDir: String, versionName: String)
    private external fun nativeReset()
    private external fun nativeForceGc()
    private external fun nativeStartTun(fd: Int, mtu: Int): Int
    private external fun nativeStopTun()
    private external fun nativeLoadConfig(configPath: String): Int
    private external fun nativeQueryTraffic(): Long
    private external fun nativeGetVersion(): String
    
    // TODO: 新增 Mihomo API native 方法 (等待 Go 代码重新编译)
    private external fun nativeReloadConfig(configPath: String, force: Boolean): Int
    private external fun nativeUpdateConfig(patchJSON: String): Int
    private external fun nativeGetProxies(): String
    // private external fun nativeSelectProxy(groupName: String, proxyName: String): Int // Removed: use HTTP API instead
    private external fun nativeTestProxyDelay(proxyName: String, testURL: String, timeout: Int): Int
    private external fun nativeGetConnections(): String
    private external fun nativeCloseConnection(connID: String): Int
    private external fun nativeCloseAllConnections(): Int
    private external fun nativeGetRules(): String
    private external fun nativeGetLogs(count: Int): String
    
    // 公共API包装方法
    fun getConnections(): String? {
        if (!nativeLibrariesLoaded) return null
        return try {
            nativeGetConnections()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get connections", e)
            null
        }
    }
    
    fun closeConnection(connID: String): Boolean {
        if (!nativeLibrariesLoaded) return false
        return try {
            nativeCloseConnection(connID) == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close connection", e)
            false
        }
    }
    
    fun closeAllConnections(): Boolean {
        if (!nativeLibrariesLoaded) return false
        return try {
            nativeCloseAllConnections() == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close all connections", e)
            false
        }
    }
    
    /**
     * 更新配置（补丁方式）
     * @param patch 配置补丁 Map (例如: mapOf("mode" to "global"))
     * @return 是否成功
     */
    fun updateConfig(patch: Map<String, Any>): Boolean {
        if (!nativeLibrariesLoaded) {
            Log.w(TAG, "Native libraries not loaded, cannot update config")
            return false
        }
        return try {
            // 转换为 JSON
            val patchJSON = org.json.JSONObject(patch).toString()
            val result = nativeUpdateConfig(patchJSON)
            if (result == 0) {
                Log.i(TAG, "✅ Config updated successfully: $patch")
                true
            } else {
                Log.w(TAG, "Failed to update config: $result")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update config", e)
            false
        }
    }
    
    // Removed: use HTTP API in ProxyRepository instead
    /*
    fun selectProxy(groupName: String, proxyName: String): Boolean {
        if (!nativeLibrariesLoaded) return false
        return try {
            nativeSelectProxy(groupName, proxyName) == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to select proxy", e)
            false
        }
    }
    */
    
    fun testProxyDelay(proxyName: String, testURL: String = "http://www.gstatic.com/generate_204", timeout: Int = 5000): Int {
        if (!nativeLibrariesLoaded) return -1
        return try {
            nativeTestProxyDelay(proxyName, testURL, timeout)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to test proxy delay", e)
            -1
        }
    }
    
    private fun getAppVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
