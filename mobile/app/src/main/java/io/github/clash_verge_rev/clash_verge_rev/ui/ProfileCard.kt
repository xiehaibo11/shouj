package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.clash_verge_rev.clash_verge_rev.ui.theme.AppDimensions
import io.github.clash_verge_rev.clash_verge_rev.utils.FormatUtils
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * 订阅配置卡片（完全参考桌面端ProfileItem）
 * 显示订阅名称、流量、节点数、更新时间等信息
 * 支持长按菜单、拖拽指示器等桌面端功能
 */
@OptIn(ExperimentalFoundationApi::class)
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
    configFile: File? = null,  // 配置文件引用（用于编辑功能）
    onSelect: () -> Unit,
    onUpdate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditFile: ((File) -> Unit)? = null,  // 编辑文件回调
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    // 解析URL来源（类似桌面端parseUrl）
    val urlSource = remember(url) {
        try {
            if (url.isNotEmpty()) {
                URL(url).host
            } else ""
        } catch (e: Exception) {
            ""
        }
    }
    
    Box {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize()
                .combinedClickable(
                    onClick = { onSelect() },
                    onLongClick = { showMenu = true }
                ),
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
                    .padding(AppDimensions.Padding.cardMedium)
            ) {
                // 顶部：拖拽手柄 + 名称 + 更新按钮（对应桌面端布局）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 拖拽指示器（类似桌面端DragIndicatorRounded）
                    Icon(
                        Icons.Rounded.DragIndicator,
                        contentDescription = "拖拽",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // 订阅名称
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 更新按钮（右上角，对应桌面端）
                    if (url.isNotEmpty()) {
                        IconButton(
                            onClick = { onUpdate() },
                            enabled = !isUpdating,
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = "更新",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    // 更多菜单按钮
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "更多",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // 第二行：URL来源或节点数（对应桌面端第二行）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // URL来源（类似桌面端显示from）
                    if (urlSource.isNotEmpty()) {
                        Text(
                            text = urlSource,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Cloud,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$nodeCount 节点",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 更新时间（类似桌面端的fromNow显示）
                    if (updatedAt > 0) {
                        Text(
                            text = formatTimeAgo(updatedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 第三行：流量或过期信息（对应桌面端第三行）
                if (trafficTotal > 0 || expireTime > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (trafficTotal > 0) {
                            Text(
                                text = "${FormatUtils.formatBytes(trafficUsed)} / ${FormatUtils.formatBytes(trafficTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (expireTime > 0) {
                            val isExpired = expireTime * 1000 < System.currentTimeMillis()
                            Text(
                                text = if (isExpired) "已过期" else formatDate(expireTime * 1000),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // 流量进度条（对应桌面端的LinearProgress）
                if (trafficTotal > 0) {
                    LinearProgressIndicator(
                        progress = {
                            if (trafficTotal > 0) {
                                (trafficUsed.toFloat() / trafficTotal.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 24.dp, end = 0.dp)
                            .height(3.dp),
                        color = when {
                            trafficTotal == 0L -> MaterialTheme.colorScheme.secondary
                            trafficUsed.toFloat() / trafficTotal.toFloat() > 0.9f -> MaterialTheme.colorScheme.error
                            trafficUsed.toFloat() / trafficTotal.toFloat() > 0.7f -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }
        
        // 长按菜单（对应桌面端的ContextMenu/右键菜单）
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            // 选中
            DropdownMenuItem(
                text = { Text("选中") },
                onClick = {
                    showMenu = false
                    onSelect()
                },
                leadingIcon = { Icon(Icons.Rounded.CheckCircle, null) }
            )
            
            HorizontalDivider()
            
            // 编辑信息
            DropdownMenuItem(
                text = { Text("编辑信息") },
                onClick = {
                    showMenu = false
                    onEdit()
                },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) }
            )
            
            // 编辑文件（对应桌面端Edit File）
            DropdownMenuItem(
                text = { Text("编辑文件") },
                onClick = {
                    showMenu = false
                    if (configFile != null && onEditFile != null) {
                        onEditFile(configFile)
                    }
                },
                leadingIcon = { Icon(Icons.Rounded.Description, null) },
                enabled = configFile != null && onEditFile != null
            )
            
            // 编辑规则（对应桌面端Edit Rules）
            DropdownMenuItem(
                text = { Text("编辑规则") },
                onClick = {
                    showMenu = false
                    // TODO: 打开规则编辑器
                },
                leadingIcon = { Icon(Icons.Rounded.Rule, null) }
            )
            
            // 编辑代理（对应桌面端Edit Proxies）
            DropdownMenuItem(
                text = { Text("编辑代理") },
                onClick = {
                    showMenu = false
                    // TODO: 打开代理编辑器
                },
                leadingIcon = { Icon(Icons.Rounded.Storage, null) }
            )
            
            // 编辑组（对应桌面端Edit Groups）
            DropdownMenuItem(
                text = { Text("编辑代理组") },
                onClick = {
                    showMenu = false
                    // TODO: 打开代理组编辑器
                },
                leadingIcon = { Icon(Icons.Rounded.Workspaces, null) }
            )
            
            HorizontalDivider()
            
            // 扩展配置（对应桌面端Extend Config/Merge）
            DropdownMenuItem(
                text = { Text("扩展配置") },
                onClick = {
                    showMenu = false
                    // TODO: 打开配置扩展编辑器
                },
                leadingIcon = { Icon(Icons.Rounded.Extension, null) }
            )
            
            // 扩展脚本（对应桌面端Extend Script）
            DropdownMenuItem(
                text = { Text("扩展脚本") },
                onClick = {
                    showMenu = false
                    // TODO: 打开脚本编辑器
                },
                leadingIcon = { Icon(Icons.Rounded.Code, null) }
            )
            
            HorizontalDivider()
            
            // 打开文件（对应桌面端Open File）
            DropdownMenuItem(
                text = { Text("打开文件") },
                onClick = {
                    showMenu = false
                    // TODO: 打开系统文件查看器
                },
                leadingIcon = { Icon(Icons.Rounded.FolderOpen, null) }
            )
            
            // 更新
            if (url.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("更新") },
                    onClick = {
                        showMenu = false
                        onUpdate()
                    },
                    leadingIcon = { Icon(Icons.Rounded.Refresh, null) }
                )
                
                // 通过代理更新（对应桌面端Update via proxy）
                DropdownMenuItem(
                    text = { Text("通过代理更新") },
                    onClick = {
                        showMenu = false
                        // TODO: 使用代理更新
                        onUpdate()
                    },
                    leadingIcon = { Icon(Icons.Rounded.CloudSync, null) }
                )
            }
            
            HorizontalDivider()
            
            // 删除
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}


/**
 * 格式化时间为相对时间（类似桌面端的fromNow）
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
 * 格式化时间为相对时间
 */
private fun formatTime(timestamp: Long): String = formatTimeAgo(timestamp)

/**
 * 格式化日期
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

