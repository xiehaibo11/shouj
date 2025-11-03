package io.github.clash_verge_rev.clash_verge_rev.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * 全局流量统计管理器
 */
class TrafficStatsManager private constructor() {
    companion object {
        @Volatile
        private var instance: TrafficStatsManager? = null
        
        fun getInstance(): TrafficStatsManager {
            return instance ?: synchronized(this) {
                instance ?: TrafficStatsManager().also { instance = it }
            }
        }
    }
    
    // 总流量统计（原子操作）
    private val totalUploadBytes = AtomicLong(0)
    private val totalDownloadBytes = AtomicLong(0)
    private val totalUploadSpeed = AtomicLong(0)    // 字节/秒
    private val totalDownloadSpeed = AtomicLong(0)  // 字节/秒
    
    // 上次更新时间
    private var lastUpdateTime = System.currentTimeMillis()
    private var lastUploadBytes = 0L
    private var lastDownloadBytes = 0L
    
    // 流量统计流
    private val _statsFlow = MutableStateFlow(TrafficStats())
    val statsFlow: StateFlow<TrafficStats> = _statsFlow.asStateFlow()
    
    /**
     * 记录上传流量
     */
    fun recordUpload(bytes: Long) {
        totalUploadBytes.addAndGet(bytes)
        updateStats()
    }
    
    /**
     * 记录下载流量
     */
    fun recordDownload(bytes: Long) {
        totalDownloadBytes.addAndGet(bytes)
        updateStats()
    }
    
    /**
     * 更新统计数据（计算速度）
     */
    fun updateStats() {
        val now = System.currentTimeMillis()
        val timeDelta = (now - lastUpdateTime) / 1000.0 // 秒
        
        if (timeDelta >= 1.0) {
            val currentUpload = totalUploadBytes.get()
            val currentDownload = totalDownloadBytes.get()
            
            val uploadDelta = currentUpload - lastUploadBytes
            val downloadDelta = currentDownload - lastDownloadBytes
            
            totalUploadSpeed.set((uploadDelta / timeDelta).toLong())
            totalDownloadSpeed.set((downloadDelta / timeDelta).toLong())
            
            lastUploadBytes = currentUpload
            lastDownloadBytes = currentDownload
            lastUpdateTime = now
            
            // 更新流
            _statsFlow.value = TrafficStats(
                totalUpload = currentUpload,
                totalDownload = currentDownload,
                uploadSpeed = totalUploadSpeed.get(),
                downloadSpeed = totalDownloadSpeed.get()
            )
        }
    }
    
    /**
     * 获取当前统计
     */
    fun getStats(): TrafficStats {
        return TrafficStats(
            totalUpload = totalUploadBytes.get(),
            totalDownload = totalDownloadBytes.get(),
            uploadSpeed = totalUploadSpeed.get(),
            downloadSpeed = totalDownloadSpeed.get()
        )
    }
    
    /**
     * 重置统计
     */
    fun reset() {
        totalUploadBytes.set(0)
        totalDownloadBytes.set(0)
        totalUploadSpeed.set(0)
        totalDownloadSpeed.set(0)
        lastUploadBytes = 0
        lastDownloadBytes = 0
        lastUpdateTime = System.currentTimeMillis()
        _statsFlow.value = TrafficStats()
    }
    
    /**
     * 流量统计数据类
     */
    data class TrafficStats(
        val totalUpload: Long = 0,      // 总上传字节
        val totalDownload: Long = 0,    // 总下载字节
        val uploadSpeed: Long = 0,      // 上传速度(字节/秒)
        val downloadSpeed: Long = 0     // 下载速度(字节/秒)
    ) {
        fun formatTotalUpload(): String = formatBytes(totalUpload)
        fun formatTotalDownload(): String = formatBytes(totalDownload)
        fun formatUploadSpeed(): String = "${formatBytes(uploadSpeed)}/s"
        fun formatDownloadSpeed(): String = "${formatBytes(downloadSpeed)}/s"
        
        private fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
                else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
            }
        }
    }
}


