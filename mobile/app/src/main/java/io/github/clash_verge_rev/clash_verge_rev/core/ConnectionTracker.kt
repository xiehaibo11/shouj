package io.github.clash_verge_rev.clash_verge_rev.core

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 连接跟踪器 - 记录和管理活动连接
 */
class ConnectionTracker private constructor() {
    companion object {
        private const val TAG = "ConnectionTracker"
        
        @Volatile
        private var instance: ConnectionTracker? = null
        
        fun getInstance(): ConnectionTracker {
            return instance ?: synchronized(this) {
                instance ?: ConnectionTracker().also { instance = it }
            }
        }
    }
    
    // 连接ID生成器
    private val connectionIdGenerator = AtomicInteger(0)
    
    // 活动连接表 <ConnectionKey, Connection>
    private val activeConnections = ConcurrentHashMap<String, Connection>()
    
    // 连接列表流
    private val _connectionsFlow = MutableStateFlow<List<Connection>>(emptyList())
    val connectionsFlow: StateFlow<List<Connection>> = _connectionsFlow.asStateFlow()
    
    /**
     * 添加新连接（增强版）
     */
    fun addConnection(
        protocol: String,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int,
        proxy: String? = null,
        host: String? = null,
        type: String? = null,
        chains: List<String>? = null,
        rule: String? = null,
        rulePayload: String? = null
    ): Connection {
        val key = "$protocol:$srcIp:$srcPort:$dstIp:$dstPort"
        
        return activeConnections.getOrPut(key) {
            val conn = Connection(
                id = connectionIdGenerator.incrementAndGet(),
                protocol = protocol,
                srcIp = srcIp,
                srcPort = srcPort,
                dstIp = dstIp,
                dstPort = dstPort,
                proxy = proxy ?: "DIRECT",
                startTime = System.currentTimeMillis(),
                host = host ?: dstIp,
                network = protocol.lowercase(),
                type = type ?: inferConnectionType(dstPort),
                chains = chains ?: listOf(proxy ?: "DIRECT"),
                rule = rule ?: "DIRECT",
                rulePayload = rulePayload ?: ""
            )
            Log.d(TAG, "➕ New connection: $conn")
            updateFlow()
            conn
        }
    }
    
    /**
     * 根据端口推断连接类型
     */
    private fun inferConnectionType(port: Int): String {
        return when (port) {
            80 -> "HTTP"
            443 -> "HTTPS"
            853 -> "DNS-over-TLS"
            22 -> "SSH"
            21 -> "FTP"
            25 -> "SMTP"
            110 -> "POP3"
            143 -> "IMAP"
            3389 -> "RDP"
            5900 -> "VNC"
            else -> "Unknown"
        }
    }
    
    /**
     * 更新连接流量
     */
    fun updateTraffic(
        protocol: String,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int,
        uploadBytes: Long = 0,
        downloadBytes: Long = 0
    ) {
        val key = "$protocol:$srcIp:$srcPort:$dstIp:$dstPort"
        activeConnections[key]?.let { conn ->
            conn.uploadBytes += uploadBytes
            conn.downloadBytes += downloadBytes
            conn.lastActiveTime = System.currentTimeMillis()
        }
    }
    
    /**
     * 关闭连接
     */
    fun closeConnection(
        protocol: String,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int
    ) {
        val key = "$protocol:$srcIp:$srcPort:$dstIp:$dstPort"
        activeConnections.remove(key)?.let { conn ->
            Log.d(TAG, "➖ Closed connection: $conn")
            updateFlow()
        }
    }
    
    /**
     * ✅ 更新所有连接的实时速度（每秒调用一次）
     */
    fun updateAllSpeeds() {
        activeConnections.values.forEach { conn ->
            conn.updateSpeed()
        }
        updateFlow()
    }
    
    /**
     * 清理超时连接（5分钟无活动）
     */
    fun cleanupStaleConnections() {
        val now = System.currentTimeMillis()
        val timeout = 5 * 60 * 1000 // 5分钟
        
        val stale = activeConnections.filter { (_, conn) ->
            now - conn.lastActiveTime > timeout
        }
        
        stale.forEach { (key, _) ->
            activeConnections.remove(key)
        }
        
        if (stale.isNotEmpty()) {
            Log.d(TAG, "🧹 Cleaned up ${stale.size} stale connections")
            updateFlow()
        }
    }
    
    /**
     * 获取活动连接数
     */
    fun getActiveConnectionCount(): Int {
        return activeConnections.size
    }
    
    /**
     * 获取所有活动连接
     */
    fun getAllConnections(): List<Connection> {
        return activeConnections.values.toList()
    }
    
    /**
     * 清除所有连接
     */
    fun clearAll() {
        activeConnections.clear()
        updateFlow()
        Log.i(TAG, "🧹 All connections cleared")
    }
    
    /**
     * 更新连接流
     */
    private fun updateFlow() {
        _connectionsFlow.value = getAllConnections()
    }
    
    /**
     * 连接数据类 - 增强版，对应桌面端完整结构
     */
    data class Connection(
        val id: Int,
        val protocol: String,          // TCP/UDP/ICMP
        val srcIp: String,
        val srcPort: Int,
        val dstIp: String,
        val dstPort: Int,
        val proxy: String,             // 代理名称或DIRECT
        val startTime: Long,
        var lastActiveTime: Long = startTime,
        var uploadBytes: Long = 0,
        var downloadBytes: Long = 0,
        
        // ✅ 新增字段 - 对应桌面端
        var host: String? = null,               // 目标域名
        var network: String = protocol.lowercase(), // tcp/udp
        var type: String = "",                  // HTTP/HTTPS/SOCKS5等
        var chains: List<String> = emptyList(), // 代理链
        var rule: String = "",                  // 匹配的规则
        var rulePayload: String = "",           // 规则载荷
        var process: String? = null,            // 进程名
        var processPath: String? = null,        // 进程路径
        
        // ✅ 实时速度（每秒更新）
        var curUploadSpeed: Long = 0,           // 当前上传速度 (bytes/s)
        var curDownloadSpeed: Long = 0,         // 当前下载速度 (bytes/s)
        
        // 用于速度计算的内部字段
        internal var lastUploadBytes: Long = 0,
        internal var lastDownloadBytes: Long = 0,
        internal var lastSpeedUpdateTime: Long = startTime
    ) {
        /**
         * 更新实时速度（由 ConnectionTracker 定时调用）
         */
        fun updateSpeed() {
            val now = System.currentTimeMillis()
            val timeDelta = (now - lastSpeedUpdateTime) / 1000.0 // 秒
            
            if (timeDelta > 0) {
                val uploadDelta = uploadBytes - lastUploadBytes
                val downloadDelta = downloadBytes - lastDownloadBytes
                
                curUploadSpeed = (uploadDelta / timeDelta).toLong()
                curDownloadSpeed = (downloadDelta / timeDelta).toLong()
                
                lastUploadBytes = uploadBytes
                lastDownloadBytes = downloadBytes
                lastSpeedUpdateTime = now
            }
        }
        
        override fun toString(): String {
            return "$protocol $srcIp:$srcPort -> $dstIp:$dstPort via $proxy (↑${formatBytes(uploadBytes)} ↓${formatBytes(downloadBytes)})"
        }
        
        private fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                else -> "${bytes / (1024 * 1024)} MB"
            }
        }
    }
}

