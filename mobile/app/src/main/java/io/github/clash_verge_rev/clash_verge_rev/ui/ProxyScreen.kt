package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.clash_verge_rev.clash_verge_rev.data.*
import io.github.clash_verge_rev.clash_verge_rev.ui.theme.AppDimensions
import kotlinx.coroutines.launch
import java.io.File

/**
 * 代理页面 - 对应桌面端的proxies.tsx
 * 
 * 功能：
 * 1. 显示代理模式切换（Rule/Global/Direct）
 * 2. 显示代理组列表
 * 3. 显示每个代理组的代理列表
 * 4. 支持代理切换
 * 5. 支持延迟测试
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val proxyRepository = remember { ProxyRepository.getInstance(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    
    // 监听当前配置路径
    val currentConfigPath by settingsManager.currentConfigPath
    val clashMode by settingsManager.clashMode
    
    // 从SettingsManager读取初始模式
    val initialMode = when (clashMode) {
        "global" -> ProxyMode.GLOBAL
        "direct" -> ProxyMode.DIRECT
        else -> ProxyMode.RULE
    }
    
    // 代理数据状态 - 使用 remember(currentConfigPath) 确保配置路径相同时状态持久
    var proxiesState by remember(currentConfigPath) { 
        mutableStateOf(ProxiesState(mode = initialMode, isLoading = currentConfigPath.isNotEmpty())) 
    }
    
    // 从缓存恢复选中的组索引
    var selectedGroupIndex by remember(currentConfigPath) { 
        mutableStateOf(
            if (currentConfigPath.isNotEmpty()) {
                proxyRepository.getSelectedGroupIndex(currentConfigPath)
            } else {
                0
            }
        )
    }
    
    var testingNodes by remember { mutableStateOf(setOf<String>()) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    
    // Provider对话框状态
    var showProviderDialog by remember { mutableStateOf(false) }
    
    // 链式代理模式状态（从SharedPreferences恢复）
    var isChainMode by remember {
        mutableStateOf(
            context.getSharedPreferences("proxy_state", android.content.Context.MODE_PRIVATE)
                .getBoolean("chain_mode_enabled", false)
        )
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 监听Clash模式变化，同步更新UI状态
    LaunchedEffect(clashMode) {
        val newMode = when (clashMode) {
            "global" -> ProxyMode.GLOBAL
            "direct" -> ProxyMode.DIRECT
            else -> ProxyMode.RULE
        }
        if (proxiesState.mode != newMode) {
            proxiesState = proxiesState.copy(mode = newMode)
        }
    }
    
    // 监听配置路径变化，重新加载代理数据（会使用缓存，速度很快）
    LaunchedEffect(currentConfigPath) {
        if (currentConfigPath.isEmpty()) {
            proxiesState = proxiesState.copy(
                groups = emptyList(),
                error = "请先在配置页面选择订阅",
                isLoading = false
            )
            return@LaunchedEffect
        }
        
        val configFile = File(currentConfigPath)
        if (configFile.exists()) {
            // 不显示加载状态，因为有缓存会很快
            // proxiesState = proxiesState.copy(isLoading = true)
            val newState = proxyRepository.loadProxiesFromConfig(configFile)
            proxiesState = newState.copy(mode = proxiesState.mode)
            
            // 验证并调整选中的组索引
            if (selectedGroupIndex >= newState.groups.size) {
                selectedGroupIndex = 0
                if (currentConfigPath.isNotEmpty()) {
                    proxyRepository.saveSelectedGroupIndex(currentConfigPath, 0)
                }
            }
        } else {
            proxiesState = proxiesState.copy(
                groups = emptyList(),
                error = "配置文件不存在，请重新选择",
                isLoading = false
            )
        }
    }
    
    // 显示Snackbar消息
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            snackbarMessage = null
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // 代理模式切换栏 + 操作按钮
            if (!proxiesState.isLoading && proxiesState.error == null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 顶部操作图标栏
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 定位到当前代理
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (proxiesState.groups.isNotEmpty() && selectedGroupIndex < proxiesState.groups.size) {
                                            val currentGroup = proxiesState.groups[selectedGroupIndex]
                                            snackbarMessage = "当前选中: ${currentGroup.now}"
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.MyLocation,
                                    contentDescription = "定位",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // 延迟测试
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        snackbarMessage = "开始测试代理延迟..."
                                        // TODO: 实现延迟测试功能
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.NetworkCheck,
                                    contentDescription = "延迟测试",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // 排序（对应桌面端Sort系列）
                            var sortType by remember { mutableStateOf(0) }
                            IconButton(
                                onClick = {
                                    sortType = (sortType + 1) % 3
                                    snackbarMessage = when(sortType) {
                                        0 -> "默认排序"
                                        1 -> "按延迟排序"
                                        2 -> "按名称排序"
                                        else -> "默认排序"
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    when(sortType) {
                                        1 -> Icons.Rounded.AccessTime
                                        2 -> Icons.Rounded.SortByAlpha
                                        else -> Icons.Rounded.Sort
                                    },
                                    contentDescription = "排序",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // 延迟测试URL（对应桌面端WifiTethering）
                            var showTestUrl by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = {
                                    showTestUrl = !showTestUrl
                                    snackbarMessage = if(showTestUrl) "自定义测试URL" else "默认测试URL"
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    if(showTestUrl) Icons.Rounded.WifiTethering else Icons.Rounded.PortableWifiOff,
                                    contentDescription = "测试URL",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // 显示详情切换（对应桌面端Visibility）
                            var showDetail by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = {
                                    showDetail = !showDetail
                                    snackbarMessage = if(showDetail) "显示详细信息" else "显示基本信息"
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    if(showDetail) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = "显示详情",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // 过滤/搜索
                            var showFilter by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = {
                                    showFilter = !showFilter
                                    snackbarMessage = if(showFilter) "显示过滤器" else "隐藏过滤器"
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    if(showFilter) Icons.Rounded.FilterAlt else Icons.Rounded.FilterAltOff,
                                    contentDescription = "过滤",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                        }
                    }
                    
                    // Provider和Chain Proxy按钮行（对应桌面端header区域）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Provider按钮（对应桌面端ProviderButton）
                        OutlinedButton(
                            onClick = { showProviderDialog = true },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("提供者", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Chain Proxy按钮（对应桌面端Chain Proxy按钮）
                        FilterChip(
                            selected = isChainMode,
                            onClick = {
                                isChainMode = !isChainMode
                                // 保存到SharedPreferences
                                context.getSharedPreferences("proxy_state", android.content.Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("chain_mode_enabled", isChainMode)
                                    .apply()
                                
                                snackbarMessage = if (isChainMode) "已开启链式代理" else "已关闭链式代理"
                            },
                            label = {
                                Text(
                                    "链式代理",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                    
                    // 代理模式切换栏
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
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
                                    text = if (isChainMode) "链式代理模式" else "代理模式",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (proxiesState.mode) {
                                        ProxyMode.RULE -> "规则模式"
                                        ProxyMode.GLOBAL -> "全局模式"
                                        ProxyMode.DIRECT -> "直连模式"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // 模式切换按钮
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProxyMode.values().forEach { mode ->
                                    FilterChip(
                                        selected = proxiesState.mode == mode,
                                        onClick = {
                                            scope.launch {
                                                if (proxyRepository.switchMode(mode)) {
                                                    proxiesState = proxiesState.copy(mode = mode)
                                                    // 同步更新SettingsManager
                                                    settingsManager.setClashMode(mode.name.lowercase())
                                                    
                                                    // 如果VPN正在运行，通知重新加载配置
                                                    val intent = android.content.Intent(context, io.github.clash_verge_rev.clash_verge_rev.service.ClashVpnService::class.java)
                                                    intent.action = io.github.clash_verge_rev.clash_verge_rev.service.ClashVpnService.ACTION_RESTART
                                                    try {
                                                        context.startService(intent)
                                                    } catch (e: Exception) {
                                                        android.util.Log.w("ProxyScreen", "Failed to restart VPN service", e)
                                                    }
                                                    
                                                    snackbarMessage = "已切换到${
                                                        when (mode) {
                                                            ProxyMode.RULE -> "规则模式"
                                                            ProxyMode.GLOBAL -> "全局模式"
                                                            ProxyMode.DIRECT -> "直连模式"
                                                        }
                                                    }"
                                                } else {
                                                    snackbarMessage = "切换模式失败"
                                                }
                                            }
                                        },
                                        label = {
                                            Text(
                                                when (mode) {
                                                    ProxyMode.RULE -> "规则"
                                                    ProxyMode.GLOBAL -> "全局"
                                                    ProxyMode.DIRECT -> "直连"
                                                }
                                            )
                                        },
                                        leadingIcon = if (proxiesState.mode == mode) {
                                            {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                proxiesState.isLoading -> {
                    // 加载中
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在加载代理数据...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                proxiesState.error != null -> {
                    // 错误状态
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = proxiesState.error ?: "未知错误",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FilledTonalButton(
                            onClick = {
                                // 重新加载
                                scope.launch {
                                    val configDir = File(context.filesDir, "configs")
                                    if (configDir.exists()) {
                                        val configFiles = configDir.listFiles { file ->
                                            file.extension == "yaml" || file.extension == "yml"
                                        }?.sortedByDescending { it.lastModified() }
                                        
                                        if (!configFiles.isNullOrEmpty()) {
                                            proxiesState = proxiesState.copy(isLoading = true, error = null)
                                            proxiesState = proxyRepository.loadProxiesFromConfig(configFiles[0])
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("重新加载")
                        }
                    }
                }
                
                proxiesState.groups.isEmpty() -> {
                    // 无代理组
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无代理组",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请在配置页面导入订阅",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                else -> {
                    // 显示代理组和代理列表
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 代理组标签栏
                        ScrollableTabRow(
                            selectedTabIndex = selectedGroupIndex,
                            modifier = Modifier.fillMaxWidth(),
                            edgePadding = 16.dp
                        ) {
                            proxiesState.groups.forEachIndexed { index, group ->
                                Tab(
                                    selected = selectedGroupIndex == index,
                                    onClick = { 
                                        selectedGroupIndex = index
                                        // 保存选中的组索引
                                        if (currentConfigPath.isNotEmpty()) {
                                            proxyRepository.saveSelectedGroupIndex(currentConfigPath, index)
                                        }
                                    },
                                    text = {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = group.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (selectedGroupIndex == index) 
                                                    FontWeight.Bold 
                                                else 
                                                    FontWeight.Normal
                                            )
                                            Text(
                                                text = "${group.proxies.size} 代理",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        
                        HorizontalDivider()
                        
                        // 代理列表
                        val currentGroup = proxiesState.groups.getOrNull(selectedGroupIndex)
                        if (currentGroup != null) {
                            ProxyGroupContent(
                                group = currentGroup,
                                testingNodes = testingNodes,
                                onNodeSelect = { node ->
                                    scope.launch {
                                        if (proxyRepository.switchProxy(currentGroup.name, node.name, currentConfigPath)) {
                                            // 更新当前选中的代理
                                            val updatedGroups = proxiesState.groups.toMutableList()
                                            updatedGroups[selectedGroupIndex] = currentGroup.copy(now = node.name)
                                            proxiesState = proxiesState.copy(groups = updatedGroups)
                                            snackbarMessage = "已切换到 ${node.name}"
                                        } else {
                                            snackbarMessage = "切换失败"
                                        }
                                    }
                                },
                                onNodeTest = { node ->
                                    scope.launch {
                                        testingNodes = testingNodes + node.name
                                        val result = proxyRepository.testProxyDelay(node.name)
                                        testingNodes = testingNodes - node.name
                                        
                                        when (result) {
                                            is TestStatus.Success -> {
                                                // 更新延迟
                                                val updatedProxies = currentGroup.proxies.map { proxy ->
                                                    if (proxy.name == node.name) {
                                                        proxy.copy(delay = result.delay)
                                                    } else {
                                                        proxy
                                                    }
                                                }
                                                val updatedGroups = proxiesState.groups.toMutableList()
                                                updatedGroups[selectedGroupIndex] = currentGroup.copy(proxies = updatedProxies)
                                                proxiesState = proxiesState.copy(groups = updatedGroups)
                                                
                                                snackbarMessage = "${node.name}: ${result.delay}ms"
                                            }
                                            is TestStatus.Failed -> {
                                                snackbarMessage = "${node.name}: 测试失败"
                                            }
                                            else -> {}
                                        }
                                    }
                                },
                                onTestAll = {
                                    scope.launch {
                                        testingNodes = currentGroup.proxies.map { it.name }.toSet()
                                        val results = proxyRepository.testGroupDelay(currentGroup)
                                        testingNodes = emptySet()
                                        
                                        // 更新所有延迟
                                        val updatedProxies = currentGroup.proxies.map { proxy ->
                                            val result = results[proxy.name]
                                            if (result is TestStatus.Success) {
                                                proxy.copy(delay = result.delay)
                                            } else {
                                                proxy
                                            }
                                        }
                                        val updatedGroups = proxiesState.groups.toMutableList()
                                        updatedGroups[selectedGroupIndex] = currentGroup.copy(proxies = updatedProxies)
                                        proxiesState = proxiesState.copy(groups = updatedGroups)
                                        
                                        snackbarMessage = "测试完成"
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    } // Scaffold content结束
    
    // Provider对话框（对应桌面端ProviderButton的Dialog）
    if (showProviderDialog) {
        ProviderDialog(
            providers = emptyMap(), // TODO: 从配置文件解析providers
            onDismiss = { showProviderDialog = false },
            onUpdateProvider = { providerName ->
                scope.launch {
                    snackbarMessage = "开始更新Provider: $providerName"
                    // TODO: 实现provider更新逻辑
                }
            },
            onUpdateAllProviders = {
                scope.launch {
                    snackbarMessage = "开始更新所有Providers"
                    // TODO: 实现全部更新逻辑
                }
            }
        )
    }
} // ProxyScreen函数结束

@Composable
fun ProxyGroupContent(
    group: ProxyGroup,
    testingNodes: Set<String>,
    onNodeSelect: (ProxyNode) -> Unit,
    onNodeTest: (ProxyNode) -> Unit,
    onTestAll: () -> Unit
) {
    val context = LocalContext.current
    val proxyRepository = remember { ProxyRepository.getInstance(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentConfigPath by settingsManager.currentConfigPath
    
    // 创建 LazyListState 用于保存和恢复滚动位置（类似桌面端localStorage）
    val listState = rememberLazyListState()
    
    // 恢复滚动位置
    LaunchedEffect(group.name, currentConfigPath) {
        if (currentConfigPath.isNotEmpty()) {
            // 从持久化存储恢复滚动位置
            val savedPosition = proxyRepository.getScrollPosition(currentConfigPath, group.name.hashCode())
            if (savedPosition > 0 && savedPosition < group.proxies.size) {
                listState.scrollToItem(savedPosition)
            }
        }
    }
    
    // 监听滚动，保存位置
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (currentConfigPath.isNotEmpty()) {
            proxyRepository.saveScrollPosition(
                currentConfigPath,
                group.name.hashCode(),
                listState.firstVisibleItemIndex
            )
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 代理组信息和操作栏
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                        Text(
                                    text = group.type.uppercase(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (group.now.isNotEmpty()) {
                        Text(
                                text = "当前: ${group.now}",
                                style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        }
                    }
                    
                    // 测速所有代理按钮
                    FilledTonalButton(
                        onClick = onTestAll,
                        enabled = testingNodes.isEmpty()
                    ) {
                        if (testingNodes.isNotEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Speed, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("测速全部")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                    Text(
                    text = "共 ${group.proxies.size} 个代理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            }
        }
        
        // 代理列表（使用 listState 保存滚动位置）
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(group.proxies) { proxy ->
                ProxyNodeItem(
                    proxy = proxy,
                    isSelected = proxy.name == group.now,
                    isTesting = testingNodes.contains(proxy.name),
                    onSelect = { onNodeSelect(proxy) },
                    onTest = { onNodeTest(proxy) }
                )
            }
        }
    }
}

/**
 * 类型标签组件（参考桌面端TypeBox）
 */
@Composable
fun TypeBox(
    text: String,
    isSelected: Boolean
) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(
            1.dp,
            if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.36f)
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f)
        ),
        color = Color.Transparent
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/**
 * 延迟标签组件（参考桌面端延迟颜色）
 */
@Composable
fun DelayChip(delay: Int) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = when {
            delay < 0 -> MaterialTheme.colorScheme.errorContainer
            delay == 0 -> MaterialTheme.colorScheme.secondaryContainer
            delay < 200 -> Color(0xFF4CAF50).copy(alpha = 0.2f)  // 绿色
            delay < 500 -> Color(0xFFFFC107).copy(alpha = 0.2f)  // 黄色
            else -> Color(0xFFF44336).copy(alpha = 0.2f)  // 红色
        }
    ) {
        Text(
            text = when {
                delay < 0 -> "超时"
                delay == 0 -> "直连"
                else -> "${delay}ms"
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = when {
                delay < 0 -> MaterialTheme.colorScheme.onErrorContainer
                delay == 0 -> MaterialTheme.colorScheme.onSecondaryContainer
                delay < 200 -> Color(0xFF1B5E20)  // 深绿色
                delay < 500 -> Color(0xFFF57F17)  // 深黄色
                else -> Color(0xFFB71C1C)  // 深红色
            },
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProxyNodeItem(
    proxy: ProxyNode,
    isSelected: Boolean,
    isTesting: Boolean,
    onSelect: () -> Unit,
    onTest: () -> Unit
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
                .clickable(onClick = onSelect)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择指示器
            Icon(
                imageVector = if (isSelected) 
                    Icons.Filled.CheckCircle 
                else 
                    Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 代理信息
            Column(modifier = Modifier.weight(1f)) {
                // 第一行：代理名称
                Text(
                    text = proxy.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 第二行：类型、特性标签、延迟
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Provider标签
                    proxy.provider?.let { provider ->
                        TypeBox(
                            text = provider,
                            isSelected = isSelected
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    // 类型标签
                    TypeBox(
                        text = proxy.type.uppercase(),
                        isSelected = isSelected
                    )
                    
                    // 特性标签
                    if (proxy.udp) {
                        Spacer(modifier = Modifier.width(4.dp))
                        TypeBox(text = "UDP", isSelected = isSelected)
                    }
                    if (proxy.xudp) {
                        Spacer(modifier = Modifier.width(4.dp))
                        TypeBox(text = "XUDP", isSelected = isSelected)
                    }
                    if (proxy.tfo) {
                        Spacer(modifier = Modifier.width(4.dp))
                        TypeBox(text = "TFO", isSelected = isSelected)
                    }
                    if (proxy.mptcp) {
                        Spacer(modifier = Modifier.width(4.dp))
                        TypeBox(text = "MPTCP", isSelected = isSelected)
                    }
                    if (proxy.smux) {
                        Spacer(modifier = Modifier.width(4.dp))
                        TypeBox(text = "SMUX", isSelected = isSelected)
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 延迟标签
                    proxy.delay?.let { delay ->
                        DelayChip(delay = delay)
                    }
                }
                
                // 第三行：服务器信息
                if (!proxy.server.isNullOrEmpty() && !proxy.now.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "→ ${proxy.now}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            
            // 测速按钮
            if (proxy.type.lowercase() !in listOf("direct", "reject")) {
                IconButton(
                    onClick = onTest,
                    enabled = !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = "测速",
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
