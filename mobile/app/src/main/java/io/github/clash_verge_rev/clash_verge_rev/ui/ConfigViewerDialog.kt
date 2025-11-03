package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.clash_verge_rev.clash_verge_rev.ui.theme.AppDimensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 配置查看器对话框（对应桌面端ConfigViewer）
 * 支持查看运行时配置、配置文件、Merge、Script等
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigViewerDialog(
    title: String,
    configFile: File? = null,
    configContent: String? = null,
    readOnly: Boolean = true,
    language: ConfigLanguage = ConfigLanguage.YAML,
    onDismiss: () -> Unit,
    onSave: ((String) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    
    var content by remember { mutableStateOf(configContent ?: "") }
    var isLoading by remember { mutableStateOf(configContent == null && configFile != null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    
    // 加载文件内容
    LaunchedEffect(configFile) {
        if (configFile != null && configContent == null) {
            try {
                isLoading = true
                content = withContext(Dispatchers.IO) {
                    configFile.readText()
                }
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "加载失败: ${e.message}"
                content = "# Error loading file\n# ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
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
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(title)
                            if (readOnly) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("只读", style = MaterialTheme.typography.labelSmall) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(language.displayName, style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, "关闭")
                        }
                    },
                    actions = {
                        if (!readOnly) {
                            // 格式化按钮（YAML/JS）
                            IconButton(
                                onClick = {
                                    // TODO: 实现格式化功能
                                },
                                enabled = !isLoading && !isSaving
                            ) {
                                Icon(Icons.Rounded.FormatAlignLeft, "格式化")
                            }
                        }
                        
                        // 复制按钮
                        IconButton(
                            onClick = {
                                // TODO: 实现复制到剪贴板
                            },
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Rounded.ContentCopy, "复制")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                
                // 错误提示
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                // 内容区域
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("加载中...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        // 代码编辑器（使用OutlinedTextField模拟）
                        if (readOnly) {
                            // 只读模式：使用Text + 滚动
                            Text(
                                text = content,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState())
                                    .horizontalScroll(rememberScrollState()),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            // 编辑模式：使用OutlinedTextField
                            OutlinedTextField(
                                value = content,
                                onValueChange = { content = it },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
                
                // 底部操作栏
                if (!readOnly) {
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isSaving
                        ) {
                            Text("取消")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        isSaving = true
                                        onSave?.invoke(content)
                                        onDismiss()
                                    } catch (e: Exception) {
                                        errorMessage = "保存失败: ${e.message}"
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = !isSaving && !isLoading
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 配置语言类型
 */
enum class ConfigLanguage(val displayName: String) {
    YAML("YAML"),
    JAVASCRIPT("JavaScript"),
    JSON("JSON")
}

