package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 配置管理界面
 */
@Composable
fun ConfigScreen(
    configDir: File,
    onLoadConfig: (File) -> Unit = {}
) {
    var configFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedConfig by remember { mutableStateOf<File?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<File?>(null) }
    
    LaunchedEffect(configDir) {
        // 加载配置文件列表
        if (configDir.exists() && configDir.isDirectory) {
            configFiles = configDir.listFiles { file ->
                file.extension == "yaml" || file.extension == "yml"
            }?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部信息卡
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "配置管理",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedConfig != null) 
                                "当前: ${selectedConfig?.name}" 
                            else 
                                "未选择配置",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedConfig != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    FilledTonalButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "共 ${configFiles.size} 个配置文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 配置文件列表
        if (configFiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无配置文件",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击上方按钮添加配置",
                        style = MaterialTheme.typography.bodySmall,
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
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(configFiles.size) { index ->
                    ConfigFileItem(
                        file = configFiles[index],
                        isSelected = selectedConfig == configFiles[index],
                        onSelect = {
                            selectedConfig = it
                            onLoadConfig(it)
                        },
                        onDelete = { showDeleteDialog = it }
                    )
                }
            }
        }
    }
    
    // 添加配置对话框
    if (showAddDialog) {
        AddConfigDialog(
            configDir = configDir,
            onDismiss = { showAddDialog = false },
            onAdded = {
                showAddDialog = false
                // 刷新列表
                configFiles = configDir.listFiles { file ->
                    file.extension == "yaml" || file.extension == "yml"
                }?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
            }
        )
    }
    
    // 删除确认对话框
    showDeleteDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("删除配置") },
            text = { Text("确定要删除 ${file.name} 吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        file.delete()
                        configFiles = configFiles.filter { it != file }
                        if (selectedConfig == file) {
                            selectedConfig = null
                        }
                        showDeleteDialog = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ConfigFileItem(
    file: File,
    isSelected: Boolean = false,
    onSelect: (File) -> Unit,
    onDelete: (File) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelected) 
                            Icons.Default.CheckCircle 
                        else 
                            Icons.Default.Description,
                        contentDescription = null,
                        tint = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${file.length() / 1024} KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                            .format(Date(file.lastModified())),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 操作按钮
            Row {
                IconButton(onClick = { onSelect(file) }) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "加载",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = { onDelete(file) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AddConfigDialog(
    configDir: File,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profileManager = remember { io.github.clash_verge_rev.clash_verge_rev.data.ProfileManager.getInstance(context) }
    
    var configUrl by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = if (isLoading) { {} } else onDismiss,
        icon = { Icon(Icons.Default.Add, null) },
        title = { Text("添加配置") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 错误消息
                errorMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // 成功消息
                successMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("URL订阅") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("本地文件") }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when (selectedTab) {
                    0 -> {
                        Text(
                            text = "订阅地址",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = configUrl,
                            onValueChange = { configUrl = it },
                            label = { Text("订阅URL") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Link, null) },
                            placeholder = { Text("https://example.com/subscribe?token=...") },
                            singleLine = false,
                            maxLines = 3,
                            enabled = !isLoading
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "• 配置名称将自动从订阅中获取\n• 节点信息将自动下载并加载",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    1 -> {
                        Text(
                            text = "选择本地配置文件",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "支持 .yaml 和 .yml 格式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FilledTonalButton(
                            onClick = {
                                errorMessage = "文件选择器功能开发中"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.FolderOpen, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("选择文件")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedTab == 0 && configUrl.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        successMessage = null
                        
                        coroutineScope.launch {
                            try {
                                // 使用ProfileManager下载订阅
                                val result = profileManager.importSubscription(configUrl)
                                
                                result.onSuccess { profileInfo ->
                                    successMessage = "成功导入: ${profileInfo.name}\n" +
                                        "节点数量: ${profileInfo.nodeCount}\n" +
                                        "供应商: ${profileInfo.providers.joinToString(", ").ifEmpty { "无" }}"
                                    
                                    isLoading = false
                                    
                                    // 延迟关闭对话框，让用户看到成功消息
                                    kotlinx.coroutines.delay(1500)
                                    onAdded()
                                    onDismiss()
                                }.onFailure { error ->
                                    errorMessage = "导入失败: ${error.message}"
                                    isLoading = false
                                }
                            } catch (e: Exception) {
                                errorMessage = "导入失败: ${e.message}"
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading && when (selectedTab) {
                    0 -> configUrl.isNotBlank()
                    else -> false
                }
            ) {
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("下载中...")
                    }
                } else {
                    Text("导入")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("取消")
            }
        }
    )
}



