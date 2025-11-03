package io.github.clash_verge_rev.clash_verge_rev.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.clash_verge_rev.clash_verge_rev.data.ProfileManager
import io.github.clash_verge_rev.clash_verge_rev.data.ProfileStorage
import io.github.clash_verge_rev.clash_verge_rev.data.SettingsManager
import io.github.clash_verge_rev.clash_verge_rev.ui.theme.AppDimensions
import io.github.clash_verge_rev.clash_verge_rev.utils.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 配置管理界面（订阅管理）
 * 完全参考桌面端的profiles.tsx实现
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreenNew() {
    val context = LocalContext.current
    val profileManager = remember { ProfileManager.getInstance(context) }
    val profileStorage = remember { ProfileStorage.getInstance(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var profiles by remember { mutableStateOf<List<Pair<File, ProfileStorage.ProfileMetadata?>>>(emptyList()) }
    val currentConfigPath by settingsManager.currentConfigPath
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCreateLocalDialog by remember { mutableStateOf(false) }
    var showNewMenu by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<Pair<File, ProfileStorage.ProfileMetadata?>?>(null) }
    var showConfigViewer by remember { mutableStateOf(false) }
    var viewerConfig by remember { mutableStateOf<Pair<String, File>?>(null) } // (title, file)
    var updatingProfiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEnhancing by remember { mutableStateOf(false) }
    
    // 批量操作模式（对应桌面端的batchMode）
    var batchMode by remember { mutableStateOf(false) }
    var selectedProfiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // URL输入（对应桌面端的url state）
    var urlInput by remember { mutableStateOf("") }
    
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
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                Column {
                        Text("订阅管理")
                    Text(
                        text = "${profiles.size} 个配置",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                actions = {
                    if (!batchMode) {
                        // 普通模式图标（对应桌面端icons）
                        // 批量操作切换
                        IconButton(
                            onClick = { batchMode = true }
                        ) {
                            Icon(
                                Icons.Outlined.CheckBoxOutlineBlank,
                                contentDescription = "批量操作",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        // 全部更新
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                    var updatedCount = 0
                                profiles.forEach { (file, metadata) ->
                                    if (metadata != null && metadata.type == ProfileStorage.ProfileType.REMOTE) {
                                        val uid = file.nameWithoutExtension
                                        updatingProfiles = updatingProfiles + uid
                                        
                                        profileManager.updateSubscription(file, metadata.url)
                                            .onSuccess {
                                                    updatedCount++
                                                }
                                            
                                            updatingProfiles = updatingProfiles - uid
                                        }
                                    }
                                                loadProfiles()
                                    snackbarHostState.showSnackbar("更新完成: $updatedCount 个配置")
                                }
                            },
                            enabled = updatingProfiles.isEmpty()
                        ) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = "更新所有",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        // 查看运行配置（对应桌面端ConfigViewer）
                        IconButton(
                            onClick = {
                                val configFile = if (currentConfigPath.isNotEmpty()) {
                                    File(currentConfigPath)
                                } else {
                                    profiles.firstOrNull()?.first
                                }
                                
                                if (configFile != null && configFile.exists()) {
                                    viewerConfig = Pair("运行配置", configFile)
                                    showConfigViewer = true
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("未找到配置文件")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Outlined.TextSnippet,
                                contentDescription = "运行配置",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        // 重新激活（对应桌面端enhanceProfiles）
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isEnhancing = true
                                    try {
                                        // 重新加载当前配置
                                        val configFile = if (currentConfigPath.isNotEmpty()) {
                                            File(currentConfigPath)
                                        } else {
                                            profiles.firstOrNull()?.first
                                        }
                                        
                                        if (configFile != null && configFile.exists()) {
                                            settingsManager.setCurrentConfigPath(configFile.absolutePath)
                                            loadProfiles()
                                            snackbarHostState.showSnackbar("配置已重新激活")
                                        } else {
                                            snackbarHostState.showSnackbar("未找到配置文件")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("重新激活失败: ${e.message}")
                                    } finally {
                                        isEnhancing = false
                                    }
                                }
                            },
                            enabled = !isEnhancing
                        ) {
                            if (isEnhancing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.LocalFireDepartment,
                                    contentDescription = "重新激活",
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        // 批量模式图标
                        // 全选/取消全选
                        IconButton(
                            onClick = {
                                if (selectedProfiles.size == profiles.size) {
                                    selectedProfiles = emptySet()
                                } else {
                                    selectedProfiles = profiles.map { it.first.nameWithoutExtension }.toSet()
                                }
                            }
                        ) {
                            Icon(
                                if (selectedProfiles.size == profiles.size) 
                                    Icons.Rounded.CheckBox 
                                else if (selectedProfiles.isNotEmpty())
                                    Icons.Rounded.IndeterminateCheckBox
                                else
                                    Icons.Outlined.CheckBoxOutlineBlank,
                                contentDescription = "全选",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        // 删除选中
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    selectedProfiles.forEach { uid ->
                                        profiles.find { it.first.nameWithoutExtension == uid }?.let { (file, _) ->
                                            profileStorage.deleteProfile(uid)
                                            file.delete()
                                        }
                                    }
                                    selectedProfiles = emptySet()
                                    loadProfiles()
                                    snackbarHostState.showSnackbar("已删除 ${selectedProfiles.size} 个配置")
                                }
                            },
                            enabled = selectedProfiles.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "删除",
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        // 完成按钮
                        TextButton(
                            onClick = { 
                                batchMode = false
                                selectedProfiles = emptySet()
                            }
                        ) {
                            Text("完成")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // URL输入栏和New按钮（对应桌面端的BaseStyledTextField + New button）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // URL输入框
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    placeholder = { Text("订阅 URL") },
                    trailingIcon = {
                        if (urlInput.isEmpty()) {
                            // 粘贴按钮（对应桌面端ContentPasteRounded）
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.primaryClip?.getItemAt(0)?.text?.let { text ->
                                        urlInput = text.toString()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.ContentPaste,
                                    contentDescription = "粘贴",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Row {
                                // 导入按钮
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            profileManager.importSubscription(urlInput)
                                                .onSuccess {
                                                    urlInput = ""
                                                    loadProfiles()
                                                    snackbarHostState.showSnackbar("订阅导入成功")
                                                }
                                                .onFailure { e ->
                                                    snackbarHostState.showSnackbar("导入失败: ${e.message}")
                                                }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = "导入",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                // 清除按钮（对应桌面端ClearRounded）
                                IconButton(
                                    onClick = { urlInput = "" }
                                ) {
                                    Icon(
                                        Icons.Rounded.Clear,
                                        contentDescription = "清除",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (urlInput.isNotEmpty()) {
                                coroutineScope.launch {
                                    profileManager.importSubscription(urlInput)
                                        .onSuccess {
                                            urlInput = ""
                                            loadProfiles()
                                            snackbarHostState.showSnackbar("订阅导入成功")
                                        }
                                        .onFailure { e ->
                                            snackbarHostState.showSnackbar("导入失败: ${e.message}")
                                        }
                                }
                            }
                        }
                    ),
                    singleLine = true
                )
            }
                
                // New按钮（对应桌面端的New按钮）- 提供下拉菜单选择
                Box {
                    Button(
                        onClick = { showNewMenu = true },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新建")
                        Icon(
                            Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showNewMenu,
                        onDismissRequest = { showNewMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("新建订阅") },
                            onClick = {
                                showNewMenu = false
                                editingProfile = null
                                showEditDialog = true
                            },
                            leadingIcon = { Icon(Icons.Rounded.CloudDownload, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("新建本地配置") },
                            onClick = {
                                showNewMenu = false
                                showCreateLocalDialog = true
                            },
                            leadingIcon = { Icon(Icons.Rounded.Description, null) }
                        )
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
                // Merge配置卡片（对应桌面端ProfileMore）
                item {
                    val mergeFile = File(context.filesDir, "merge.yaml")
                    ProfileMoreCard(
                        id = ProfileMoreType.MERGE,
                        configFile = mergeFile,
                        onEditFile = { file, language ->
                            viewerConfig = Pair("全局 Merge 配置", file)
                            showConfigViewer = true
                        },
                        onOpenFile = { file ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("文件位置: ${file.absolutePath}")
                            }
                        }
                    )
                }
                
                // Script配置卡片（对应桌面端ProfileMore）
                item {
                    val scriptFile = File(context.filesDir, "script.js")
                    ProfileMoreCard(
                        id = ProfileMoreType.SCRIPT,
                        configFile = scriptFile,
                        onEditFile = { file, language ->
                            viewerConfig = Pair("全局 Script 配置", file)
                            showConfigViewer = true
                        },
                        onOpenFile = { file ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("文件位置: ${file.absolutePath}")
                            }
                        }
                    )
                }
                
                // 分隔线
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                
                items(profiles) { (file, metadata) ->
                    val uid = file.nameWithoutExtension
                    val isSelected = file.absolutePath == currentConfigPath
                    val isUpdating = updatingProfiles.contains(uid)
                    val isChecked = selectedProfiles.contains(uid)
                    
                    // 批量选择时显示复选框
                    if (batchMode) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) {
                                        selectedProfiles = selectedProfiles - uid
                                    } else {
                                        selectedProfiles = selectedProfiles + uid
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isChecked) Icons.Rounded.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = metadata?.name ?: file.nameWithoutExtension,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
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
                            configFile = file,
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
                            editingProfile = Pair(file, metadata)
                            showEditDialog = true
                        },
                        onEditFile = { configFile: File ->
                            viewerConfig = Pair("编辑配置 - ${configFile.name}", configFile)
                            showConfigViewer = true
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
        }
    }
    
    // 添加订阅对话框
    if (showAddDialog) {
        AddConfigDialogNew(
            configDir = File(context.filesDir, "configs"),
            onDismiss = { showAddDialog = false },
            onAdded = {
                showAddDialog = false
                loadProfiles()
            }
        )
    }
    
    // 编辑订阅对话框（对应桌面端ProfileViewer）
    if (showEditDialog && editingProfile != null) {
        EditProfileDialog(
            metadata = editingProfile?.second,
            onDismiss = {
                showEditDialog = false
                editingProfile = null
            },
            onSave = { updatedMetadata ->
                coroutineScope.launch {
                    val (file, oldMetadata) = editingProfile!!
                    val uid = file.nameWithoutExtension
                    
                    try {
                        // 更新元数据
                        val finalMetadata = updatedMetadata.copy(
                            uid = uid,
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        profileStorage.saveProfile(finalMetadata)
                        
                        // 如果URL变化了，重新下载
                        if (finalMetadata.type == ProfileStorage.ProfileType.REMOTE &&
                            finalMetadata.url != oldMetadata?.url) {
                            updatingProfiles = updatingProfiles + uid
                            
                            profileManager.updateSubscription(file, finalMetadata.url)
                                .onSuccess {
                                    loadProfiles()
                                    snackbarHostState.showSnackbar("订阅更新成功")
                                }
                                .onFailure { e ->
                                    snackbarHostState.showSnackbar("更新失败: ${e.message}")
                                }
                            
                            updatingProfiles = updatingProfiles - uid
                        } else {
                            loadProfiles()
                            snackbarHostState.showSnackbar("订阅信息已保存")
                        }
                        
                        showEditDialog = false
                        editingProfile = null
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("保存失败: ${e.message}")
                    }
                }
            }
        )
    }
    
    // 配置查看器对话框（对应桌面端ConfigViewer/EditorViewer）
    if (showConfigViewer && viewerConfig != null) {
        val (title, configFile) = viewerConfig!!
        val isRuntimeConfig = title.contains("运行配置")
        val isMergeOrScript = title.contains("Merge") || title.contains("Script")
        
        // 确定语言类型
        val language = when {
            title.contains("Script") -> ConfigLanguage.JAVASCRIPT
            else -> ConfigLanguage.YAML
        }
        
        ConfigViewerDialog(
            title = title,
            configFile = configFile,
            readOnly = isRuntimeConfig,  // 运行配置只读，编辑配置可写
            language = language,
            onDismiss = {
                showConfigViewer = false
                viewerConfig = null
            },
            onSave = if (!isRuntimeConfig) { content ->
                coroutineScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            // 确保父目录存在
                            configFile.parentFile?.mkdirs()
                            configFile.writeText(content)
                        }
                        
                        // Merge/Script配置不需要刷新profiles列表
                        if (!isMergeOrScript) {
                            loadProfiles()
                        }
                        snackbarHostState.showSnackbar("配置已保存")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("保存失败: ${e.message}")
                    }
                }
            } else null
        )
    }
    
    // 创建本地配置对话框
    if (showCreateLocalDialog) {
        CreateLocalConfigDialog(
            configDir = File(context.filesDir, "configs"),
            onDismiss = {
                showCreateLocalDialog = false
            },
            onCreated = {
                showCreateLocalDialog = false
                loadProfiles()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("本地配置创建成功")
                }
            }
        )
    }
}

/**
 * 添加订阅对话框（ConfigScreenNew专用）
 */
@Composable
private fun AddConfigDialogNew(
    configDir: File,
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

