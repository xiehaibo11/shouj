package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.clash_verge_rev.clash_verge_rev.utils.FormatUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 代理提供者管理对话框（对应桌面端ProviderButton）
 * 
 * 功能：
 * - 显示所有代理提供者列表
 * - 显示节点数量、更新时间
 * - 显示流量使用和过期时间
 * - 单个/全部更新提供者
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDialog(
    providers: Map<String, ProviderInfo>,
    onDismiss: () -> Unit,
    onUpdateProvider: (String) -> Unit,
    onUpdateAllProviders: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var updatingProviders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isUpdatingAll by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                TopAppBar(
                    title = { Text("代理提供者") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, "关闭")
                        }
                    },
                    actions = {
                        // 全部更新按钮
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isUpdatingAll = true
                                    onUpdateAllProviders()
                                    isUpdatingAll = false
                                }
                            },
                            enabled = !isUpdatingAll && updatingProviders.isEmpty()
                        ) {
                            if (isUpdatingAll) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("全部更新")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                
                // 提供者列表
                if (providers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                "没有代理提供者",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(providers.entries.sortedBy { it.key }) { (name, provider) ->
                            val isUpdating = updatingProviders.contains(name)
                            
                            ProviderCard(
                                name = name,
                                provider = provider,
                                isUpdating = isUpdating,
                                onUpdate = {
                                    coroutineScope.launch {
                                        updatingProviders = updatingProviders + name
                                        onUpdateProvider(name)
                                        updatingProviders = updatingProviders - name
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单个提供者卡片
 */
@Composable
private fun ProviderCard(
    name: String,
    provider: ProviderInfo,
    isUpdating: Boolean,
    onUpdate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 标题行：名称 + 节点数 + 类型
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    // 节点数量标签
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = "${provider.proxyCount}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    // 类型标签
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = provider.vehicleType,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                
                // 更新时间
                Text(
                    text = "更新时间: ${formatTimeAgo(provider.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 流量和过期信息（如果有）
                if (provider.subscriptionInfo != null) {
                    val sub = provider.subscriptionInfo
                    
                    // 流量信息
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${FormatUtils.formatBytes(sub.upload + sub.download)} / ${FormatUtils.formatBytes(sub.total)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // 过期时间
                        if (sub.expire > 0) {
                            val isExpired = sub.expire * 1000 < System.currentTimeMillis()
                            Text(
                                text = if (isExpired) "已过期" else formatDate(sub.expire * 1000),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 流量进度条
                    if (sub.total > 0) {
                        LinearProgressIndicator(
                            progress = {
                                ((sub.upload + sub.download).toFloat() / sub.total.toFloat()).coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = when {
                                (sub.upload + sub.download).toFloat() / sub.total.toFloat() > 0.9f -> MaterialTheme.colorScheme.error
                                (sub.upload + sub.download).toFloat() / sub.total.toFloat() > 0.7f -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
            
            // 右侧更新按钮
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onUpdate,
                enabled = !isUpdating,
                modifier = Modifier.size(40.dp)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "更新",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 代理提供者信息数据类
 */
data class ProviderInfo(
    val proxyCount: Int,
    val vehicleType: String,
    val updatedAt: Long,
    val subscriptionInfo: SubscriptionInfo? = null
)

/**
 * 订阅信息数据类
 */
data class SubscriptionInfo(
    val upload: Long,
    val download: Long,
    val total: Long,
    val expire: Long
)

/**
 * 格式化时间为相对时间
 */
private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
        else -> {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * 格式化日期
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

