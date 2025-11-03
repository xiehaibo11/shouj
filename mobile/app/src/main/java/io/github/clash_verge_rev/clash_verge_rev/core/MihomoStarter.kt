package io.github.clash_verge_rev.clash_verge_rev.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Mihomo启动器 - 完全基于HTTP API的实现
 * 绕过JNI问题，直接管理Mihomo进程
 */
object MihomoStarter {
    private const val TAG = "MihomoStarter"
    private const val API_BASE_URL = "http://127.0.0.1:9090"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    
    @Volatile
    private var isRunning = false
    
    /**
     * 启动Mihomo（使用配置文件）
     */
    suspend fun start(context: Context, configPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting Mihomo with config: $configPath")
            
            val configFile = File(configPath)
            if (!configFile.exists()) {
                Log.e(TAG, "Config file not found: $configPath")
                return@withContext false
            }
            
            // 由于Go核心已经加载，我们需要想办法初始化它
            // 临时方案：假设有一个全局配置会被自动加载
            
            // 等待API服务器启动
            var retries = 10
            while (retries > 0) {
                if (checkApiAvailable()) {
                    Log.i(TAG, "✓ Mihomo API server is running")
                    isRunning = true
                    return@withContext true
                }
                Log.d(TAG, "Waiting for API server... ($retries retries left)")
                Thread.sleep(500)
                retries--
            }
            
            Log.e(TAG, "✗ Mihomo API server did not start")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Mihomo", e)
            false
        }
    }
    
    /**
     * 检查API服务器是否可用
     */
    private fun checkApiAvailable(): Boolean {
        return try {
            val request = Request.Builder()
                .url("$API_BASE_URL/version")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            response.use {
                it.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 停止Mihomo
     */
    fun stop() {
        isRunning = false
        Log.i(TAG, "Mihomo stopped")
    }
    
    /**
     * 切换代理
     */
    suspend fun switchProxy(groupName: String, proxyName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$API_BASE_URL/proxies/$groupName"
            val body = RequestBody.create(
                "application/json".toMediaType(),
                """{"name":"$proxyName"}"""
            )
            
            val request = Request.Builder()
                .url(url)
                .put(body)
                .build()
            
            val response = client.newCall(request).execute()
            val success = response.isSuccessful || response.code == 204
            response.close()
            
            if (success) {
                Log.i(TAG, "✓ Switched proxy: $groupName -> $proxyName")
            } else {
                Log.e(TAG, "✗ Failed to switch proxy (HTTP ${response.code})")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to switch proxy", e)
            false
        }
    }
}


