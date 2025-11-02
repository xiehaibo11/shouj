package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.clash_verge_rev.clash_verge_rev.ui.theme.AppDimensions
import io.github.clash_verge_rev.clash_verge_rev.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * 订阅配置卡片
 * 显示订阅名称、流量、节点数、更新时间等信息
 * 类似桌面端的ProfileItem
 */
@Composable
fun ProfileCard(
    uid: String,
    name: String,
    url: String,
    nodeCount: Int,
    trafficTotal: Long,
    trafficUsed: Long,
    expireTime: Long,
    updatedAt: Long,
    isSelected: Boolean,
    isUpdating: Boolean,
    onSelect: () -> Unit,
    onUpdate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) AppDimensions.Card.elevation * 2 else AppDimensions.Card.elevation
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(AppDimensions.Padding.cardMedium)
        ) {
            // 顶部：名称和操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // 节点数量
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = AppDimensions.Spacing.extraSmall)
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimensions.IconSize.small),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(AppDimensions.Spacing.extraSmall))
                        Text(
                            text = "$nodeCount 节点",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 操作按钮
                Row {
                    // 更新按钮
                    IconButton(
                        onClick = onUpdate,
                        enabled = !isUpdating
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(AppDimensions.IconSize.medium),
                                strokeWidth = AppDimensions.ProgressBar.strokeWidth
                            )
                        } else {
                            Icon(Icons.Default.Refresh, "更新")
                        }
                    }
                    
                    // 更多菜单
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "更多")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("编辑") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("删除") },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error,
                                    leadingIconColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }
            }
            
            // 流量信息
            if (trafficTotal > 0) {
                Spacer(modifier = Modifier.height(AppDimensions.Spacing.medium))
                
                Column {
                    // 流量进度条
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = FormatUtils.formatBytes(trafficUsed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = FormatUtils.formatBytes(trafficTotal),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(AppDimensions.Spacing.extraSmall))
                    
                    LinearProgressIndicator(
                        progress = {
                            if (trafficTotal > 0) {
                                (trafficUsed.toFloat() / trafficTotal.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppDimensions.ProgressBar.heightMedium),
                        color = when {
                            trafficTotal == 0L -> MaterialTheme.colorScheme.secondary
                            trafficUsed.toFloat() / trafficTotal.toFloat() > 0.9f -> MaterialTheme.colorScheme.error
                            trafficUsed.toFloat() / trafficTotal.toFloat() > 0.7f -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    
                    // 剩余流量
                    val remaining = trafficTotal - trafficUsed
                    if (remaining > 0) {
                        Text(
                            text = "剩余: ${formatBytes(remaining)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = AppDimensions.Spacing.extraSmall)
                        )
                    }
                }
            }
            
            // 底部：更新时间和过期时间
            Spacer(modifier = Modifier.height(AppDimensions.Spacing.medium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 更新时间
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Update,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.IconSize.tiny),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.Spacing.extraSmall))
                    Text(
                        text = formatTime(updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 过期时间
                if (expireTime > 0) {
                    val isExpired = expireTime * 1000 < System.currentTimeMillis()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isExpired) Icons.Default.Warning else Icons.Default.Event,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimensions.IconSize.tiny),
                            tint = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(AppDimensions.Spacing.extraSmall))
                        Text(
                            text = if (isExpired) "已过期" else formatDate(expireTime * 1000),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 展开/收起 URL
            if (url.isNotEmpty()) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.padding(top = AppDimensions.Spacing.extraSmall)
                ) {
                    Text(
                        text = if (expanded) "隐藏URL" else "显示URL",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.IconSize.small)
                    )
                }
                
                if (expanded) {
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = AppDimensions.Spacing.extraSmall)
                    )
                }
            }
        }
    }
}

/**
 * 格式化时间为相对时间
 */
private fun formatTime(timestamp: Long): String {
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

