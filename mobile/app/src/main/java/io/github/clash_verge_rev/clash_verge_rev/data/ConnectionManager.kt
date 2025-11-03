package io.github.clash_verge_rev.clash_verge_rev.data

import android.content.Context
import android.util.Log
import io.github.clash_verge_rev.clash_verge_rev.core.ClashCore
import io.github.clash_verge_rev.clash_verge_rev.core.ConnectionTracker
import io.github.clash_verge_rev.clash_verge_rev.core.TrafficStatsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * 连接管理器 - 从 Mihomo 核心获取真实连接数据
 * ✅ 集成 Mihomo nativeGetConnections() API
 */
class ConnectionManager private constructor(context: Context) {
    
    companion object {
        private const val TAG = "ConnectionManager"
        
        @Volatile
        private var instance: ConnectionManager? = null
        
        fun getInstance(context: Context): ConnectionManager {
            return instance ?: synchronized(this) {
                instance ?: ConnectionManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // ✅ 使用新的 ConnectionTracker 和 TrafficStatsManager
    private val connectionTracker = ConnectionTracker.getInstance()
    private val trafficStatsManager = TrafficStatsManager.getInstance()
    
    // 状态流
    private val _connectionsState = MutableStateFlow(ConnectionsResponse())
    val connectionsState: StateFlow<ConnectionsResponse> = _connectionsState.asStateFlow()
    
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()
    
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()
    
    // 冻结的数据（用于暂停时）
    private var frozenData: ConnectionsResponse? = null
    
    private var updateJob: Job? = null
    
    /**
     * 开始自动更新连接数据
     * ✅ 从 Mihomo 核心获取真实连接数据
     */
    fun startUpdating() {
        if (updateJob?.isActive == true) {
            Log.d(TAG, "Update job already running")
            return
        }
        
        Log.i(TAG, "✅ Starting connection updates (Mihomo API)")
        _isUpdating.value = true
        
        updateJob = scope.launch {
            while (isActive) {
                if (!_isPaused.value) {
                    try {
                        // ✅ 从 Mihomo 核心获取连接数据
                        refreshConnections()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to refresh connections", e)
                    }
                }
                
                // 每秒更新一次
                delay(1000)
            }
        }
    }
    
    /**
     * 从 Mihomo 核心刷新连接数据
     */
    private suspend fun refreshConnections() {
        withContext(Dispatchers.IO) {
            try {
                // ✅ 调用 Mihomo nativeGetConnections()
                val json = ClashCore.getConnections()
                
                if (json != null) {
                    val response = parseConnectionsResponse(json)
                    _connectionsState.value = response
                } else {
                    Log.w(TAG, "getConnections() returned null")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get connections from Mihomo", e)
            }
        }
    }
    
    /**
     * 解析 Mihomo 返回的连接 JSON
     */
    private fun parseConnectionsResponse(json: String): ConnectionsResponse {
        try {
            val jsonObj = JSONObject(json)
            
            // 解析流量统计
            val downloadTotal = jsonObj.optLong("downloadTotal", 0)
            val uploadTotal = jsonObj.optLong("uploadTotal", 0)
            
            // 解析连接列表
            val connectionsArray = jsonObj.optJSONArray("connections")
            val connections = mutableListOf<Connection>()
            
            if (connectionsArray != null) {
                for (i in 0 until connectionsArray.length()) {
                    try {
                        val connObj = connectionsArray.getJSONObject(i)
                        val connection = parseConnection(connObj)
                        connections.add(connection)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse connection at index $i", e)
                    }
                }
            }
            
            return ConnectionsResponse(
                downloadTotal = downloadTotal,
                uploadTotal = uploadTotal,
                connections = connections
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse connections JSON", e)
            return ConnectionsResponse()
        }
    }
    
    /**
     * 解析单个连接对象
     */
    private fun parseConnection(jsonObj: JSONObject): Connection {
        // 解析 metadata
        val metadataObj = jsonObj.optJSONObject("metadata")
        val metadata = if (metadataObj != null) {
            val sourcePort = metadataObj.optString("sourcePort", "0")
            val destPort = metadataObj.optString("destPort", "0")
            val destIP = metadataObj.optString("destIP", "")
            
            ConnectionMetadata(
                network = metadataObj.optString("network", "tcp"),
                type = metadataObj.optString("type", "HTTP"),
                sourceIP = metadataObj.optString("sourceIP", ""),
                destinationIP = destIP,
                sourcePort = sourcePort,
                destinationPort = destPort,
                host = metadataObj.optString("host", ""),
                dnsMode = metadataObj.optString("dnsMode", "normal"),
                uid = metadataObj.optInt("uid", 0),
                process = metadataObj.optString("processPath", ""),
                processPath = metadataObj.optString("processPath", ""),
                specialProxy = metadataObj.optString("specialProxy", ""),
                specialRules = metadataObj.optString("specialRules", ""),
                remoteDestination = "$destIP:$destPort",
                sniffHost = metadataObj.optString("sniffHost", "")
            )
        } else {
            ConnectionMetadata(
                network = "tcp",
                type = "HTTP",
                sourceIP = "",
                destinationIP = "",
                sourcePort = "0",
                destinationPort = "0",
                host = "",
                dnsMode = "normal",
                uid = 0,
                process = "",
                processPath = "",
                specialProxy = "",
                specialRules = "",
                remoteDestination = "",
                sniffHost = ""
            )
        }
        
        // 解析 chains
        val chainsArray = jsonObj.optJSONArray("chains")
        val chains = mutableListOf<String>()
        if (chainsArray != null) {
            for (i in 0 until chainsArray.length()) {
                chains.add(chainsArray.optString(i, ""))
            }
        }
        
        // 获取总量和实时速度
        val uploadTotal = jsonObj.optLong("uploadTotal", 0)
        val downloadTotal = jsonObj.optLong("downloadTotal", 0)
        val curUpload = jsonObj.optLong("upload", 0)
        val curDownload = jsonObj.optLong("download", 0)
        
        return Connection(
            id = jsonObj.optString("id", ""),
            metadata = metadata,
            upload = uploadTotal,
            download = downloadTotal,
            start = jsonObj.optString("start", ""),
            chains = chains,
            rule = jsonObj.optString("rule", ""),
            rulePayload = jsonObj.optString("rulePayload", ""),
            curUpload = curUpload,
            curDownload = curDownload
        )
    }
    
    /**
     * 停止自动更新
     */
    fun stopUpdating() {
        Log.i(TAG, "Stopping connection updates")
        updateJob?.cancel()
        updateJob = null
        _isUpdating.value = false
        _connectionsState.value = ConnectionsResponse()
    }
    
    /**
     * 暂停/继续更新
     */
    fun togglePause() {
        val newPausedState = !_isPaused.value
        _isPaused.value = newPausedState
        
        if (newPausedState) {
            // 暂停时保存当前数据
            frozenData = _connectionsState.value
            Log.d(TAG, "Connections paused")
        } else {
            // 继续时清除冻结数据
            frozenData = null
            Log.d(TAG, "Connections resumed")
        }
    }
    /**
     * ✅ 关闭指定连接（调用 Mihomo API）
     */
    suspend fun closeConnection(id: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // ✅ 调用 Mihomo closeConnection()
                val result = ClashCore.closeConnection(id)
                
                if (result) {
                    Log.i(TAG, "✅ Connection closed: $id")
                    // 立即刷新连接列表
                    refreshConnections()
                }
                result
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close connection", e)
                false
            }
        }
    }
    
    /**
     * ✅ 关闭所有连接（调用 Mihomo API）
     */
    suspend fun closeAllConnections(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // ✅ 调用 Mihomo closeAllConnections()
                val result = ClashCore.closeAllConnections()
                
                if (result) {
                    Log.i(TAG, "✅ All connections closed")
                    // 立即刷新连接列表
                    refreshConnections()
                }
                result
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close all connections", e)
                false
            }
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        stopUpdating()
        scope.cancel()
    }
}

