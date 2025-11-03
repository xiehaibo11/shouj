package io.github.clash_verge_rev.clash_verge_rev.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.clash_verge_rev.clash_verge_rev.data.SettingsManager
import io.github.clash_verge_rev.clash_verge_rev.data.ProfileStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat

/**
 * 首页 - 完整集成桌面端功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isVpnRunning: Boolean,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val scrollState = rememberScrollState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // 卡片显示设置（从SharedPreferences加载）- 与桌面端完全一致
    val prefs = remember { context.getSharedPreferences("home_settings", android.content.Context.MODE_PRIVATE) }
    var showProfileCard by remember { mutableStateOf(prefs.getBoolean("show_profile", true)) }
    var showProxyCard by remember { mutableStateOf(prefs.getBoolean("show_proxy", true)) }
    var showNetworkCard by remember { mutableStateOf(prefs.getBoolean("show_network", true)) }
    var showModeCard by remember { mutableStateOf(prefs.getBoolean("show_mode", true)) }
    var showConnectionsCard by remember { mutableStateOf(prefs.getBoolean("show_connections", true)) }
    var showTrafficCard by remember { mutableStateOf(prefs.getBoolean("show_traffic", true)) }
    var showIpCard by remember { mutableStateOf(prefs.getBoolean("show_ip", true)) }
    var showClashInfoCard by remember { mutableStateOf(prefs.getBoolean("show_clashinfo", true)) }
    var showSystemInfoCard by remember { mutableStateOf(prefs.getBoolean("show_systeminfo", true)) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Language,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).padding(end = 8.dp)
                        )
                        Text("连接")
                    }
                },
                actions = {
                    // 帮助文档按钮
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://clash-verge-rev.github.io/index.html"))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "帮助")
                    }
                    
                    // 首页设置按钮
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "首页设置")
                    }
                }
            )
        }
    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
                .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            // 关键卡片（与桌面端一致）
            if (showProfileCard) {
        ProfileCard()
            }
        
            if (showProxyCard) {
        CurrentProxyCard(isVpnRunning)
            }
        
            if (showNetworkCard) {
        NetworkSettingsCard(
            tunMode = isVpnRunning,
            systemProxy = settingsManager.systemProxy.value,
            onTunModeChange = { enabled ->
                android.util.Log.i("HomeScreen", "TUN switch clicked: $enabled")
                if (enabled) {
                    android.util.Log.i("HomeScreen", "Calling onStartVpn()")
                    onStartVpn()
                } else {
                    android.util.Log.i("HomeScreen", "Calling onStopVpn()")
                    onStopVpn()
                }
            },
            onSystemProxyChange = { settingsManager.setSystemProxy(it) }
        )
            }
        
         if (showModeCard) {
        ProxyModeCard()
         }
         
         // 次要卡片（与桌面端一致）
         if (showConnectionsCard) {
         ConnectionsCard(isVpnRunning)
         }
         
         if (showTrafficCard) {
        TrafficStatsCard(isVpnRunning)
         }
        
            if (showIpCard) {
        IpInfoCard(isVpnRunning)
            }
            
            if (showClashInfoCard) {
                ClashInfoCard(isVpnRunning)
            }
            
            if (showSystemInfoCard) {
                SystemInfoCard()
            }
        }
    }
    
    // 首页设置对话框（与桌面端一致）
    if (showSettingsDialog) {
        HomeSettingsDialog(
            showProfileCard = showProfileCard,
            showProxyCard = showProxyCard,
            showNetworkCard = showNetworkCard,
            showModeCard = showModeCard,
            showConnectionsCard = showConnectionsCard,
            showTrafficCard = showTrafficCard,
            showIpCard = showIpCard,
            showClashInfoCard = showClashInfoCard,
            showSystemInfoCard = showSystemInfoCard,
            onDismiss = { showSettingsDialog = false },
            onSave = { settings ->
                // 保存到SharedPreferences
                prefs.edit().apply {
                    putBoolean("show_profile", settings["profile"] ?: true)
                    putBoolean("show_proxy", settings["proxy"] ?: true)
                    putBoolean("show_network", settings["network"] ?: true)
                    putBoolean("show_mode", settings["mode"] ?: true)
                    putBoolean("show_connections", settings["connections"] ?: true)
                    putBoolean("show_traffic", settings["traffic"] ?: true)
                    putBoolean("show_ip", settings["ip"] ?: true)
                    putBoolean("show_clashinfo", settings["clashinfo"] ?: true)
                    putBoolean("show_systeminfo", settings["systeminfo"] ?: true)
                    apply()
                }
                
                // 更新状态
                showProfileCard = settings["profile"] ?: true
                showProxyCard = settings["proxy"] ?: true
                showNetworkCard = settings["network"] ?: true
                showModeCard = settings["mode"] ?: true
                showConnectionsCard = settings["connections"] ?: true
                showTrafficCard = settings["traffic"] ?: true
                showIpCard = settings["ip"] ?: true
                showClashInfoCard = settings["clashinfo"] ?: true
                showSystemInfoCard = settings["systeminfo"] ?: true
                
                showSettingsDialog = false
            }
        )
    }
}

/**
 * 首页设置对话框（与桌面端一致）
 */
@Composable
fun HomeSettingsDialog(
    showProfileCard: Boolean,
    showProxyCard: Boolean,
    showNetworkCard: Boolean,
    showModeCard: Boolean,
    showConnectionsCard: Boolean,
    showTrafficCard: Boolean,
    showIpCard: Boolean,
    showClashInfoCard: Boolean,
    showSystemInfoCard: Boolean,
    onDismiss: () -> Unit,
    onSave: (Map<String, Boolean>) -> Unit
) {
    var profile by remember { mutableStateOf(showProfileCard) }
    var proxy by remember { mutableStateOf(showProxyCard) }
    var network by remember { mutableStateOf(showNetworkCard) }
    var mode by remember { mutableStateOf(showModeCard) }
    var connections by remember { mutableStateOf(showConnectionsCard) }
    var traffic by remember { mutableStateOf(showTrafficCard) }
    var ip by remember { mutableStateOf(showIpCard) }
    var clashinfo by remember { mutableStateOf(showClashInfoCard) }
    var systeminfo by remember { mutableStateOf(showSystemInfoCard) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("首页设置") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "选择要显示的卡片",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("配置文件卡片")
                    Switch(checked = profile, onCheckedChange = { profile = it })
                }
                
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Text("当前代理卡片")
                    Switch(checked = proxy, onCheckedChange = { proxy = it })
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("网络设置卡片")
                    Switch(checked = network, onCheckedChange = { network = it })
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("代理模式卡片")
                    Switch(checked = mode, onCheckedChange = { mode = it })
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("连接管理卡片")
                    Switch(checked = connections, onCheckedChange = { connections = it })
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("流量统计卡片")
                    Switch(checked = traffic, onCheckedChange = { traffic = it })
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("IP信息卡片")
                    Switch(checked = ip, onCheckedChange = { ip = it })
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Clash信息卡片")
                    Switch(checked = clashinfo, onCheckedChange = { clashinfo = it })
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("系统信息卡片")
                    Switch(checked = systeminfo, onCheckedChange = { systeminfo = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(mapOf(
                    "profile" to profile,
                    "proxy" to proxy,
                    "network" to network,
                    "mode" to mode,
                    "connections" to connections,
                    "traffic" to traffic,
                    "ip" to ip,
                    "clashinfo" to clashinfo,
                    "systeminfo" to systeminfo
                ))
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 配置文件卡片 - 显示真实数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCard() {
    val context = LocalContext.current
    val profileStorage = remember { ProfileStorage.getInstance(context) }
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(if (expanded) 180f else 0f)
    
    // 获取当前配置文件信息
    val currentProfile = remember { mutableStateOf<ProfileStorage.ProfileMetadata?>(null) }
    val proxyCount = remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // 获取所有配置文件
            val profiles = profileStorage.getAllProfiles()
            // 查找选中的配置（selected是List<String>，不为空表示被选中）
            currentProfile.value = profiles.find { it.selected.isNotEmpty() } ?: profiles.firstOrNull()
            
            // 统计代理节点数
            if (currentProfile.value != null) {
                val configFile = File(context.filesDir, "configs/${currentProfile.value!!.uid}.yaml")
                if (configFile.exists()) {
                    try {
                        val content = configFile.readText()
                        // 简单统计proxies数量
                        proxyCount.value = content.lines().count { it.trim().startsWith("- name:") }
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileCard", "Failed to count proxies", e)
                    }
                }
            }
        }
    }
    
    InfoCard(
        icon = Icons.Rounded.Dns,
        title = "配置文件",
        iconColor = MaterialTheme.colorScheme.primary
    ) {
                    Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                        Text(
                        currentProfile.value?.name ?: "未配置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "当前配置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentProfile.value?.url?.isNotEmpty() == true) {
                        ProfileInfoRow("订阅地址", currentProfile.value?.url?.let { 
                            Uri.parse(it).host ?: "未知"
                        } ?: "本地配置")
                    }
                    
                    ProfileInfoRow("更新时间", currentProfile.value?.updatedAt?.let {
                        if (it == 0L) "从未更新" else {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            sdf.format(java.util.Date(it))
                        }
                    } ?: "从未更新")
                    
                    ProfileInfoRow("节点数量", proxyCount.value.toString())
                    
                    if (currentProfile.value != null) {
                        val upload = currentProfile.value?.trafficUpload ?: 0L
                        val download = currentProfile.value?.trafficDownload ?: 0L
                        val total = currentProfile.value?.trafficTotal ?: 0L
                        
                        if (total > 0) {
                            ProfileInfoRow("已用流量", 
                                "${formatBytes(upload + download)} / ${formatBytes(total)}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 当前代理卡片 - 完整复刻桌面端功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentProxyCard(isVpnRunning: Boolean) {
    val context = LocalContext.current
    val proxyRepository = remember { io.github.clash_verge_rev.clash_verge_rev.data.ProxyRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    // 状态管理
    var proxiesState by remember { mutableStateOf<io.github.clash_verge_rev.clash_verge_rev.data.ProxiesState?>(null) }
    var selectedGroupIndex by remember { mutableStateOf(0) }
    var selectedProxyIndex by remember { mutableStateOf(0) }
    var sortType by remember { mutableStateOf(0) } // 0=默认, 1=延迟, 2=名称
    var isTesting by remember { mutableStateOf(false) }
    var expandedGroup by remember { mutableStateOf(false) }
    var expandedProxy by remember { mutableStateOf(false) }
    var currentConfigFile by remember { mutableStateOf<File?>(null) }
    
    // 持久化存储
    val prefs = remember { 
        context.getSharedPreferences("current_proxy_card", android.content.Context.MODE_PRIVATE) 
    }
    
    // 加载配置文件和数据 + 自动刷新
    LaunchedEffect(isVpnRunning) {
        if (!isVpnRunning) return@LaunchedEffect
        
        // 首次加载
        withContext(Dispatchers.IO) {
            try {
                // 获取当前配置文件
                val profileStorage = ProfileStorage.getInstance(context)
                val profiles = profileStorage.getAllProfiles()
                val currentProfile = profiles.find { it.selected.isNotEmpty() } ?: profiles.firstOrNull()
                val configFile = currentProfile?.let { File(context.filesDir, "configs/${it.uid}.yaml") }

                configFile?.let { file ->
                    if (file.exists()) {
                        currentConfigFile = file
                        val state = proxyRepository.loadProxiesFromConfig(file)
                        
                        withContext(Dispatchers.Main) {
                            proxiesState = state
                            
                            // 恢复保存的选择
                            val savedGroupIndex = prefs.getInt("selected_group_${file.name}", 0)
                            selectedGroupIndex = if (savedGroupIndex in 0 until state.groups.size) {
                                savedGroupIndex
                            } else {
                                0
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CurrentProxyCard", "Failed to load proxies", e)
            }
        }
        
        // 每5秒自动刷新
        while (true) {
            delay(5000)
            
            if (!isVpnRunning) break
            
            withContext(Dispatchers.IO) {
                try {
                    currentConfigFile?.let { file ->
                        if (file.exists()) {
                            val state = proxyRepository.loadProxiesFromConfig(file)
                            withContext(Dispatchers.Main) {
                                proxiesState = state
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CurrentProxyCard", "Failed to refresh proxies", e)
                }
            }
        }
    }
    
    // 获取信号强度图标和颜色
    val getSignalIcon: @Composable (Int) -> Pair<ImageVector, androidx.compose.ui.graphics.Color> = { delay ->
        when {
            delay < 0 -> Icons.Filled.SignalCellularAlt to MaterialTheme.colorScheme.onSurfaceVariant
            delay >= 10000 -> Icons.Filled.WifiOff to MaterialTheme.colorScheme.error
            delay >= 500 -> Icons.Filled.SignalCellularAlt1Bar to MaterialTheme.colorScheme.error
            delay >= 300 -> Icons.Filled.SignalCellularAlt2Bar to MaterialTheme.colorScheme.tertiary
            delay >= 200 -> Icons.Filled.SignalCellularAlt to MaterialTheme.colorScheme.primary
            else -> Icons.Filled.SignalCellularAlt to MaterialTheme.colorScheme.secondary
        }
    }
    
    // 获取延迟颜色
    val getDelayColor: @Composable (Int) -> androidx.compose.ui.graphics.Color = { delay ->
        when {
            delay < 0 -> MaterialTheme.colorScheme.onSurfaceVariant
            delay >= 10000 -> MaterialTheme.colorScheme.error
            delay >= 500 -> MaterialTheme.colorScheme.error
            delay >= 300 -> MaterialTheme.colorScheme.tertiary
            delay >= 200 -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondary
        }
    }
    
    // 格式化延迟
    fun formatDelay(delay: Int): String {
        return when {
            delay < 0 -> "未测试"
            delay >= 10000 -> "超时"
            else -> "${delay}ms"
        }
    }
    
    // 当前选中的组和节点
    val currentGroup = proxiesState?.groups?.getOrNull(selectedGroupIndex)
    val currentProxy = currentGroup?.proxies?.getOrNull(selectedProxyIndex)
    val currentDelay = currentProxy?.delay ?: -1
    val (signalIcon, signalColor) = getSignalIcon(currentDelay)
    
    // 排序节点列表
    val sortedProxies = remember(currentGroup, sortType) {
        currentGroup?.proxies?.let { proxies ->
            when (sortType) {
                1 -> proxies.sortedBy { proxy -> 
                    val delay = proxy.delay ?: -1
                    if (delay < 0) Int.MAX_VALUE else delay 
                }
                2 -> proxies.sortedBy { it.name }
                else -> proxies
            }
        } ?: emptyList()
    }
    
    InfoCard(
        icon = signalIcon,
        title = "当前代理",
        iconColor = signalColor
    ) {
        if (!isVpnRunning) {
            // 未连接状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "代理未启动",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (proxiesState == null || currentGroup == null) {
            // 加载中
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 顶部操作按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                     // 延迟测试按钮
                     IconButton(
                         onClick = {
                             scope.launch {
                                 isTesting = true
                                 try {
                                     // ✅ 使用 ProxyRepository 测试延迟（不依赖JNI）
                                     currentGroup?.let { group ->
                                         val results = proxyRepository.testGroupDelay(group)
                                         android.util.Log.i("CurrentProxyCard", "Tested ${results.size} proxies")
                                         
                                         // 更新状态
                                         withContext(Dispatchers.Main) {
                                             currentConfigFile?.let { file ->
                                                 val state = proxyRepository.loadProxiesFromConfig(file)
                                                 proxiesState = state
                                             }
                                         }
                                     }
                                 } catch (e: Exception) {
                                     android.util.Log.e("CurrentProxyCard", "Delay test failed", e)
                                 } finally {
                                     withContext(Dispatchers.Main) {
                                         isTesting = false
                                     }
                                 }
                             }
                         },
                         enabled = !isTesting && currentGroup != null
                     ) {
                         if (isTesting) {
                             CircularProgressIndicator(modifier = Modifier.size(20.dp))
                         } else {
                             Icon(
                                 Icons.Rounded.NetworkCheck,
                                 contentDescription = "延迟测试",
                                 modifier = Modifier.size(20.dp)
                             )
                         }
                     }
                    
                    // 排序按钮
                    IconButton(onClick = {
                        sortType = (sortType + 1) % 3
                    }) {
                        Icon(
                            when (sortType) {
                                1 -> Icons.Rounded.AccessTime
                                2 -> Icons.Rounded.SortByAlpha
                                else -> Icons.Rounded.Sort
                            },
                            contentDescription = when (sortType) {
                                1 -> "按延迟排序"
                                2 -> "按名称排序"
                                else -> "默认排序"
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // 跳转代理页按钮
                    OutlinedButton(
                        onClick = { /* TODO: 跳转到代理页面 */ },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("代理", style = MaterialTheme.typography.bodySmall)
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // 当前节点信息卡片
                currentProxy?.let { proxy ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                            Column(modifier = Modifier.weight(1f)) {
                Text(
                                    proxy.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                
                                Spacer(Modifier.height(4.dp))
                                
                                // 节点类型和特性标签
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                Text(
                                        proxy.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    if (proxy.udp) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("UDP", style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.height(20.dp)
                                        )
                                    }
                                    if (proxy.tfo) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("TFO", style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.height(20.dp)
                                        )
                                    }
                                    if (proxy.xudp) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("XUDP", style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.height(20.dp)
                                        )
                                    }
                                }
                            }
                            
                            // 延迟显示
                            AssistChip(
                                onClick = {},
                                label = { 
                                    Text(
                                        formatDelay(currentDelay),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = getDelayColor(currentDelay).copy(alpha = 0.2f),
                                    labelColor = getDelayColor(currentDelay)
                                )
                            )
                        }
                    }
                }
                
                // 代理组选择器
                ExposedDropdownMenuBox(
                    expanded = expandedGroup,
                    onExpandedChange = { expandedGroup = !expandedGroup }
                ) {
                    OutlinedTextField(
                        value = currentGroup?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("代理组") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGroup) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedGroup,
                        onDismissRequest = { expandedGroup = false }
                    ) {
                        proxiesState?.groups?.forEachIndexed { index, group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    selectedGroupIndex = index
                                    selectedProxyIndex = 0
                                    expandedGroup = false
                                    currentConfigFile?.let {
                                        prefs.edit().putInt("selected_group_${it.name}", index).apply()
                                    }
                                }
                            )
                        }
                    }
                }
                
                // 代理节点选择器
                ExposedDropdownMenuBox(
                    expanded = expandedProxy,
                    onExpandedChange = { expandedProxy = !expandedProxy }
                ) {
                    OutlinedTextField(
                        value = currentProxy?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("代理节点") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProxy) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedProxy,
                        onDismissRequest = { expandedProxy = false },
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        sortedProxies.forEachIndexed { _, proxy ->
                            val proxyDelay = proxy.delay ?: -1
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            proxy.name,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        
                                        AssistChip(
                                            onClick = {},
                                            label = { 
                                                Text(
                                                    formatDelay(proxyDelay),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = getDelayColor(proxyDelay).copy(alpha = 0.2f),
                                                labelColor = getDelayColor(proxyDelay)
                                            ),
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    val originalIndex = currentGroup?.proxies?.indexOf(proxy) ?: 0
                                    selectedProxyIndex = originalIndex
                                    expandedProxy = false
                                    
                                    // 调用API切换节点（使用ProxyRepository）
                                    scope.launch {
                                        try {
                                            val groupName = currentGroup?.name ?: return@launch
                                            val proxyRepo = io.github.clash_verge_rev.clash_verge_rev.data.ProxyRepository.getInstance(context)
                                            val success = proxyRepo.switchProxy(
                                                groupName,
                                                proxy.name,
                                                currentConfigFile?.absolutePath
                                            )
                                            
                                            if (success) {
                                                android.util.Log.d("CurrentProxyCard", "✓ Switched to ${proxy.name} in group $groupName")
                                            } else {
                                                android.util.Log.e("CurrentProxyCard", "✗ Failed to switch proxy")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("CurrentProxyCard", "✗ Proxy switch error", e)
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
}

/**
 * 网络设置卡片
 */
@Composable
fun NetworkSettingsCard(
    tunMode: Boolean,
    systemProxy: Boolean,
    onTunModeChange: (Boolean) -> Unit,
    onSystemProxyChange: (Boolean) -> Unit
) {
    InfoCard(
        icon = Icons.Default.Dns,
        title = "网络设置",
        iconColor = MaterialTheme.colorScheme.tertiary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "TUN 模式",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (tunMode) "虚拟网卡已启用" else "虚拟网卡已禁用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = tunMode,
                    onCheckedChange = onTunModeChange
                )
            }
            
            Divider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "系统代理",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (systemProxy) "已接管系统网络" else "未接管系统网络",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = systemProxy,
                    onCheckedChange = onSystemProxyChange
                )
            }
        }
    }
}

/**
 * 代理模式卡片
 */
@Composable
fun ProxyModeCard() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val clashMode by settingsManager.clashMode
    
    val selectedMode = when (clashMode) {
        "global" -> "全局模式"
        "direct" -> "直连模式"
        else -> "规则模式"
    }
    
    val modes = listOf("规则模式", "全局模式", "直连模式")
    
    InfoCard(
        icon = Icons.Default.Router,
        title = "代理模式",
        iconColor = MaterialTheme.colorScheme.error
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = {
                        val modeValue = when (mode) {
                            "全局模式" -> "global"
                            "直连模式" -> "direct"
                            else -> "rule"
                        }
                        
                        // 保存模式设置
                        settingsManager.setClashMode(modeValue)
                        
                        // ✅ 调用 Mihomo API 更新模式
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                val patch = mapOf("mode" to modeValue)
                                io.github.clash_verge_rev.clash_verge_rev.core.ClashCore.updateConfig(patch)
                                android.util.Log.i("HomeScreen", "✅ Mode updated to: $modeValue")
                            } catch (e: Exception) {
                                android.util.Log.e("HomeScreen", "Failed to update mode", e)
                            }
                        }
                    },
                    label = { Text(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            when (selectedMode) {
                "规则模式" -> "根据规则自动选择代理"
                "全局模式" -> "所有流量通过代理"
                "直连模式" -> "所有流量直接连接"
                else -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 流量统计卡片 - 实时从ConnectionManager获取数据
 */
@Composable
fun TrafficStatsCard(isVpnRunning: Boolean) {
    val trafficStatsManager = remember { io.github.clash_verge_rev.clash_verge_rev.core.TrafficStatsManager.getInstance() }
    
    // ✅ 从新的 TrafficStatsManager 获取实时流量数据
    val trafficStats by trafficStatsManager.statsFlow.collectAsState()
    
    InfoCard(
        icon = Icons.Default.Speed,
        title = "流量统计",
        iconColor = MaterialTheme.colorScheme.secondary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ✅ 实时速度（使用新的 TrafficStatsManager）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TrafficItem(
                    icon = Icons.Default.ArrowUpward,
                    label = "上传",
                    value = trafficStats.formatUploadSpeed(),
                    color = MaterialTheme.colorScheme.error
                )
                TrafficItem(
                    icon = Icons.Default.ArrowDownward,
                    label = "下载",
                    value = trafficStats.formatDownloadSpeed(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Divider()
            
            // ✅ 总流量（使用新的 TrafficStatsManager）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "总上传",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        trafficStats.formatTotalUpload(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "总下载",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        trafficStats.formatTotalDownload(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun TrafficItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * IP信息卡片
 */
@Composable
fun IpInfoCard(isVpnRunning: Boolean) {
    var ipAddress by remember { mutableStateOf("获取中...") }
    var country by remember { mutableStateOf("--") }
    var isp by remember { mutableStateOf("--") }
    
    LaunchedEffect(isVpnRunning) {
        // TODO: 实际应该调用IP查询API
        delay(1000)
        if (isVpnRunning) {
            ipAddress = "203.0.113.42"
            country = "🇺🇸 美国"
            isp = "Example ISP"
        } else {
            ipAddress = "未连接"
            country = "--"
            isp = "--"
        }
    }
    
    InfoCard(
        icon = Icons.Default.Language,
        title = "IP 信息",
        iconColor = MaterialTheme.colorScheme.tertiary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileInfoRow("IP 地址", ipAddress)
            ProfileInfoRow("国家/地区", country)
            ProfileInfoRow("运营商", isp)
        }
    }
}

/**
 * 通用信息卡片组件
 */
@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    iconColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

/**
 * Clash信息卡片
 */
@Composable
fun ClashInfoCard(isVpnRunning: Boolean) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    
    // 运行时间统计
    var uptime by remember { mutableStateOf(0L) }
    var rulesCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(isVpnRunning) {
        val startTime = System.currentTimeMillis()
        if (isVpnRunning) {
            // 统计规则数量
            withContext(Dispatchers.IO) {
                try {
                    val profileStorage = ProfileStorage.getInstance(context)
                    val profiles = profileStorage.getAllProfiles()
                    val currentProfile = profiles.find { it.selected.isNotEmpty() } ?: profiles.firstOrNull()
                    
                    currentProfile?.let { profile ->
                        val configFile = File(context.filesDir, "configs/${profile.uid}.yaml")
                        if (configFile.exists()) {
                            val content = configFile.readText()
                            // 统计rules数量
                            rulesCount = content.lines().count { line ->
                                val trimmed = line.trim()
                                trimmed.startsWith("- DOMAIN") || 
                                trimmed.startsWith("- IP-CIDR") ||
                                trimmed.startsWith("- GEOIP") ||
                                trimmed.startsWith("- MATCH")
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ClashInfoCard", "Failed to count rules", e)
                }
            }
            
            // 更新运行时间
            while (true) {
                uptime = System.currentTimeMillis() - startTime
                delay(1000)
            }
        } else {
            uptime = 0
        }
    }
    
    InfoCard(
        icon = Icons.Outlined.DeveloperBoard,
        title = "Clash 信息",
        iconColor = MaterialTheme.colorScheme.tertiary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileInfoRow("核心版本", "Mihomo v1.18.0")
            Divider()
            ProfileInfoRow("混合端口", settingsManager.mixedPort.value.toString())
            Divider()
            ProfileInfoRow("运行时间", formatUptime(uptime))
            Divider()
            ProfileInfoRow("规则数量", rulesCount.toString())
        }
    }
}

/**
 * 系统信息卡片
 */
@Composable
fun SystemInfoCard() {
    val context = LocalContext.current
    
    // 获取应用版本
    val appVersion = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "未知"
    } catch (e: Exception) {
        "未知"
    }
    
    // 获取系统信息
    val osInfo = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
    
    InfoCard(
        icon = Icons.Outlined.Info,
        title = "系统信息",
        iconColor = MaterialTheme.colorScheme.error
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileInfoRow("应用版本", appVersion)
            Divider()
            ProfileInfoRow("系统版本", osInfo)
            Divider()
            ProfileInfoRow("设备型号", deviceModel)
            Divider()
            ProfileInfoRow("ABI架构", Build.SUPPORTED_ABIS.firstOrNull() ?: "未知")
        }
    }
}

/**
 * 连接管理卡片 - 完全复刻桌面端Connections页面
 */
@Composable
fun ConnectionsCard(isVpnRunning: Boolean) {
    val context = LocalContext.current
    val connectionTracker = remember { io.github.clash_verge_rev.clash_verge_rev.core.ConnectionTracker.getInstance() }
    val trafficStatsManager = remember { io.github.clash_verge_rev.clash_verge_rev.core.TrafficStatsManager.getInstance() }
    val scope = rememberCoroutineScope()
    
    // ✅ 从新的 ConnectionTracker 获取连接
    val connections by connectionTracker.connectionsFlow.collectAsState()
    
    // ✅ 从新的 TrafficStatsManager 获取流量统计
    val trafficStats by trafficStatsManager.statsFlow.collectAsState()
    
    // 暂停状态（保留用于UI控制）
    var isPaused by remember { mutableStateOf(false) }
    
    InfoCard(
        icon = Icons.Rounded.Language,
        title = "连接管理",
        iconColor = MaterialTheme.colorScheme.tertiary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ✅ 流量统计（使用新的 TrafficStatsManager）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "已下载",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        trafficStats.formatTotalDownload(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "已上传",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        trafficStats.formatTotalUpload(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Divider()
            
            // ✅ 连接数和控制按钮（使用新的 ConnectionTracker）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "活动连接",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${connections.size} 个",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // ✅ 清除所有连接按钮
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                connectionTracker.clearAll()
                            }
                        },
                        modifier = Modifier.height(36.dp),
                        enabled = connections.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("清除全部", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            // 查看详情按钮
            if (connections.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, io.github.clash_verge_rev.clash_verge_rev.ui.ConnectionsActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("查看连接详情")
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 格式化运行时间
 */
fun formatUptime(uptimeMs: Long): String {
    val hours = uptimeMs / 3600000
    val minutes = (uptimeMs % 3600000) / 60000
    val seconds = (uptimeMs % 60000) / 1000
    return String.format("%d:%02d:%02d", hours, minutes, seconds)
}

/**
 * 格式化字节数
 */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    
    val df = DecimalFormat("#,##0.##")
    return df.format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

