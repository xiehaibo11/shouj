package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.clash_verge_rev.clash_verge_rev.core.ClashCore
import io.github.clash_verge_rev.clash_verge_rev.data.TrafficData
import kotlinx.coroutines.delay

/**
 * 流量统计卡片
 */
@Composable
fun TrafficCard(
    modifier: Modifier = Modifier
) {
    var trafficData by remember { mutableStateOf(TrafficData()) }
    var previousUpload by remember { mutableStateOf(0L) }
    var previousDownload by remember { mutableStateOf(0L) }
    var uploadSpeed by remember { mutableStateOf("0 B/s") }
    var downloadSpeed by remember { mutableStateOf("0 B/s") }
    
    // 定期更新流量统计
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val total = ClashCore.queryTraffic()
                trafficData = TrafficData(total = total)
                
                // 计算速率 (假设upload和download存在)
                val currentUpload = trafficData.upload
                val currentDownload = trafficData.download
                
                if (previousUpload > 0) {
                    val uploadDiff = currentUpload - previousUpload
                    uploadSpeed = formatSpeed(uploadDiff)
                }
                if (previousDownload > 0) {
                    val downloadDiff = currentDownload - previousDownload
                    downloadSpeed = formatSpeed(downloadDiff)
                }
                
                previousUpload = currentUpload
                previousDownload = currentDownload
            } catch (e: Exception) {
                // 忽略错误
            }
            delay(1000) // 每秒更新一次
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "流量统计",
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    Icons.Default.DataUsage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 实时速率
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpeedItem(
                    icon = Icons.Default.Upload,
                    label = "上传速率",
                    value = uploadSpeed,
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                SpeedItem(
                    icon = Icons.Default.Download,
                    label = "下载速率",
                    value = downloadSpeed,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // 累计流量
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrafficItem(
                    label = "上传总计",
                    value = trafficData.uploadFormatted
                )
                
                TrafficItem(
                    label = "下载总计",
                    value = trafficData.downloadFormatted
                )
                
                TrafficItem(
                    label = "总计",
                    value = trafficData.totalFormatted
                )
            }
        }
    }
}

private fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
        bytesPerSecond < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSecond / 1024.0)
        bytesPerSecond < 1024 * 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024))
        else -> String.format("%.1f GB/s", bytesPerSecond / (1024.0 * 1024 * 1024))
    }
}

@Composable
fun SpeedItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TrafficItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}



