package io.github.clash_verge_rev.clash_verge_rev.utils

/**
 * 格式化工具类
 */
object FormatUtils {
    
    /**
     * 格式化字节数为可读格式
     */
    fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        
        return String.format("%.2f %s", value, units[unitIndex])
    }
}

