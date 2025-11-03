package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import io.github.clash_verge_rev.clash_verge_rev.ui.theme.AppDimensions
import java.io.File

/**
 * Profile增强配置卡片（对应桌面端ProfileMore）
 * 用于Merge配置和Script配置
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileMoreCard(
    id: ProfileMoreType,
    configFile: File,
    onEditFile: (File, ConfigLanguage) -> Unit,
    onOpenFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val title = when (id) {
        ProfileMoreType.MERGE -> "全局 Merge"
        ProfileMoreType.SCRIPT -> "全局 Script"
    }
    
    val chipLabel = when (id) {
        ProfileMoreType.MERGE -> "Merge"
        ProfileMoreType.SCRIPT -> "Script"
    }
    
    val chipColor = when (id) {
        ProfileMoreType.MERGE -> MaterialTheme.colorScheme.primary
        ProfileMoreType.SCRIPT -> MaterialTheme.colorScheme.tertiary
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { 
                    onEditFile(
                        configFile,
                        if (id == ProfileMoreType.MERGE) ConfigLanguage.YAML else ConfigLanguage.JAVASCRIPT
                    )
                },
                onLongClick = { showMenu = true }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = AppDimensions.Card.elevation
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.Padding.cardMedium)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                AssistChip(
                    onClick = {},
                    label = { 
                        Text(
                            chipLabel,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = chipColor.copy(alpha = 0.2f),
                        labelColor = chipColor
                    ),
                    modifier = Modifier.height(24.dp),
                    border = BorderStroke(1.dp, chipColor.copy(alpha = 0.5f))
                )
            }
            
            // 描述
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (id) {
                    ProfileMoreType.MERGE -> "用于合并或覆盖配置选项"
                    ProfileMoreType.SCRIPT -> "用于自定义JavaScript脚本处理"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Script控制台按钮（仅Script配置）
            if (id == ProfileMoreType.SCRIPT) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            // TODO: 打开Script控制台
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Terminal,
                            contentDescription = "Script控制台",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Script控制台",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // 长按菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            // 编辑文件
            DropdownMenuItem(
                text = { Text("编辑文件") },
                onClick = {
                    showMenu = false
                    onEditFile(
                        configFile,
                        if (id == ProfileMoreType.MERGE) ConfigLanguage.YAML else ConfigLanguage.JAVASCRIPT
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) }
            )
            
            // 打开文件
            DropdownMenuItem(
                text = { Text("打开文件") },
                onClick = {
                    showMenu = false
                    onOpenFile(configFile)
                },
                leadingIcon = { Icon(Icons.Rounded.FolderOpen, null) }
            )
        }
    }
}

/**
 * ProfileMore类型枚举
 */
enum class ProfileMoreType {
    MERGE,
    SCRIPT
}

