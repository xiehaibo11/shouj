package io.github.clash_verge_rev.clash_verge_rev.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 流量统计数据类
 */
data class TrafficData(
    val upload: Long = 0,
    val download: Long = 0,
    val total: Long = 0
) {
    /**
     * 格式化流量显示
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
    
    val uploadFormatted: String get() = formatBytes(upload)
    val downloadFormatted: String get() = formatBytes(download)
    val totalFormatted: String get() = formatBytes(total)
}

/**
 * 流量统计管理器
 */
object TrafficStatsManager {
    private val _trafficData = MutableStateFlow(TrafficData())
    val trafficData: StateFlow<TrafficData> = _trafficData.asStateFlow()
    
    /**
     * 更新流量数据
     */
    fun updateTraffic(total: Long) {
        _trafficData.value = TrafficData(
            upload = 0,  // TODO: 从核心获取详细统计
            download = 0,
            total = total
        )
    }
    
    /**
     * 重置流量统计
     */
    fun reset() {
        _trafficData.value = TrafficData()
    }
}



