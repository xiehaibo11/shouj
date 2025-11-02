package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.clash_verge_rev.clash_verge_rev.data.ProfileManager
import io.github.clash_verge_rev.clash_verge_rev.data.ProfileStorage
import io.github.clash_verge_rev.clash_verge_rev.data.SettingsManager
import io.github.clash_verge_rev.clash_verge_rev.ui.theme.AppDimensions
import io.github.clash_verge_rev.clash_verge_rev.utils.FormatUtils
import kotlinx.coroutines.launch
import java.io.File

/**
 * 配置管理界面（订阅管理）
 * 类似桌面端的profiles.tsx
 */
@Composable
fun ConfigScreenNew() {
    val context = LocalContext.current
    val profileManager = remember { ProfileManager.getInstance(context) }
    val profileStorage = remember { ProfileStorage.getInstance(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var profiles by remember { mutableStateOf<List<Pair<File, ProfileStorage.ProfileMetadata?>>>(emptyList()) }
    val currentConfigPath by settingsManager.currentConfigPath
    var showAddDialog by remember { mutableStateOf(false) }
    var updatingProfiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 加载订阅列表
    fun loadProfiles() {
        coroutineScope.launch {
            try {
                profiles = profileManager.getAllProfilesWithMetadata()
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "加载订阅列表失败: ${e.message}"
            }
        }
    }
    
    // 初始化加载
    LaunchedEffect(Unit) {
        loadProfiles()
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部工具栏
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "订阅管理",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${profiles.size} 个配置",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 全部更新按钮
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                profiles.forEach { (file, metadata) ->
                                    if (metadata != null && metadata.type == ProfileStorage.ProfileType.REMOTE) {
                                        val uid = file.nameWithoutExtension
                                        updatingProfiles = updatingProfiles + uid
                                        
                                        profileManager.updateSubscription(file, metadata.url)
                                            .onSuccess {
                                                loadProfiles()
                                            }
                                            .onFailure { e ->
                                                errorMessage = "更新失败: ${e.message}"
                                            }
                                        
                                        updatingProfiles = updatingProfiles - uid
                                    }
                                }
                            }
                        },
                        enabled = updatingProfiles.isEmpty()
                    ) {
                        Icon(Icons.Default.Refresh, "全部更新")
                    }
                    
                    // 添加订阅按钮
                    Button(
                        onClick = { showAddDialog = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加订阅")
                    }
                }
            }
        }
        
        // 错误提示
        errorMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // 订阅列表
        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无订阅配置",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Text("添加订阅")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(profiles) { (file, metadata) ->
                    val uid = file.nameWithoutExtension
                    val isSelected = file.absolutePath == currentConfigPath
                    val isUpdating = updatingProfiles.contains(uid)
                    
                    ProfileCard(
                        uid = uid,
                        name = metadata?.name ?: file.nameWithoutExtension,
                        url = metadata?.url ?: "",
                        nodeCount = metadata?.nodeCount ?: 0,
                        trafficTotal = metadata?.trafficTotal ?: 0,
                        trafficUsed = metadata?.trafficUsed ?: 0,
                        expireTime = metadata?.expireTime ?: 0,
                        updatedAt = metadata?.updatedAt ?: file.lastModified(),
                        isSelected = isSelected,
                        isUpdating = isUpdating,
                        onSelect = {
                            // 设置为当前配置
                            settingsManager.setCurrentConfigPath(file.absolutePath)
                        },
                        onUpdate = {
                            if (metadata != null && metadata.type == ProfileStorage.ProfileType.REMOTE) {
                                coroutineScope.launch {
                                    updatingProfiles = updatingProfiles + uid
                                    
                                    profileManager.updateSubscription(file, metadata.url)
                                        .onSuccess {
                                            loadProfiles()
                                        }
                                        .onFailure { e ->
                                            errorMessage = "更新失败: ${e.message}"
                                        }
                                    
                                    updatingProfiles = updatingProfiles - uid
                                }
                            }
                        },
                        onEdit = {
                            // TODO: 实现编辑功能
                        },
                        onDelete = {
                            coroutineScope.launch {
                                android.util.Log.i("ConfigScreenNew", "Deleting profile: $uid, file: ${file.absolutePath}")
                                
                                // 先删除元数据
                                profileStorage.deleteProfile(uid)
                                android.util.Log.i("ConfigScreenNew", "Metadata deleted for: $uid")
                                
                                // 再删除文件
                                if (profileManager.deleteProfile(file)) {
                                    android.util.Log.i("ConfigScreenNew", "File deleted: ${file.absolutePath}")
                                    
                                    // 如果删除的是当前配置，清空当前配置路径
                                    if (file.absolutePath == currentConfigPath) {
                                        settingsManager.setCurrentConfigPath("")
                                    }
                                    loadProfiles()
                                } else {
                                    android.util.Log.e("ConfigScreenNew", "Failed to delete file: ${file.absolutePath}")
                                    errorMessage = "删除失败"
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    
    // 添加订阅对话框
    if (showAddDialog) {
        AddConfigDialog(
            onDismiss = { showAddDialog = false },
            onAdded = {
                showAddDialog = false
                loadProfiles()
            }
        )
    }
}

/**
 * 添加订阅对话框
 */
@Composable
private fun AddConfigDialog(
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val context = LocalContext.current
    val profileManager = remember { ProfileManager.getInstance(context) }
    
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
                // Error message display
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
                
                // Success message display
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
                                // 先检查URL是否已存在
                                android.util.Log.i("ConfigScreenNew", "Checking if subscription exists: $configUrl")
                                val (exists, existingName) = profileManager.isSubscriptionExists(configUrl)
                                android.util.Log.i("ConfigScreenNew", "Subscription check result: exists=$exists, name=$existingName")
                                
                                if (exists) {
                                    errorMessage = "订阅已存在：$existingName\n请勿重复导入"
                                    isLoading = false
                                    return@launch
                                }
                                
                                // Use ProfileManager to download subscription
                                val result = profileManager.importSubscription(configUrl)
                                
                                result.onSuccess { profileInfo ->
                                    successMessage = "成功导入: ${profileInfo.name}\n" +
                                        "节点数量: ${profileInfo.nodeCount}\n" +
                                        "流量: ${if (profileInfo.trafficTotal > 0) FormatUtils.formatBytes(profileInfo.trafficTotal) else "无限制"}"
                                    
                                    isLoading = false
                                    
                                    // Delay closing the dialog to let the user see the success message
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

