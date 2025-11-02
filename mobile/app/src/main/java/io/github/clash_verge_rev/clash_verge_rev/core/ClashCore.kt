package io.github.clash_verge_rev.clash_verge_rev.core

import android.content.Context
import android.util.Log
import java.io.File

object ClashCore {
    private const val TAG = "ClashCore"
    private var nativeLibrariesLoaded = false
    
    init {
        try {
            System.loadLibrary("clash")
            System.loadLibrary("clash-jni")
            nativeLibrariesLoaded = true
            Log.i(TAG, "Native libraries loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native libraries", e)
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
            Log.i(TAG, "Initializing core: $homeDir, $versionName")
            nativeInit(homeDir, versionName)
            Log.i(TAG, "Core initialized successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method not found", e)
            nativeLibrariesLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize core", e)
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
        if (fd <= 0) throw IllegalArgumentException("Invalid fd: $fd")
        if (mtu <= 0) throw IllegalArgumentException("Invalid mtu: $mtu")
        if (!isInitialized()) throw IllegalStateException("Core not initialized")
        Log.i(TAG, "Starting TUN: fd=$fd, mtu=$mtu")
        return try {
            nativeStartTun(fd, mtu)
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
        return nativeGetVersion()
    }
    
    fun isInitialized(): Boolean {
        if (!nativeLibrariesLoaded) return false
        return try {
            nativeGetVersion().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    private external fun nativeInit(homeDir: String, versionName: String)
    private external fun nativeReset()
    private external fun nativeForceGc()
    private external fun nativeStartTun(fd: Int, mtu: Int): Int
    private external fun nativeStopTun()
    private external fun nativeLoadConfig(configPath: String): Int
    private external fun nativeQueryTraffic(): Long
    private external fun nativeGetVersion(): String
    
    private fun getAppVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
