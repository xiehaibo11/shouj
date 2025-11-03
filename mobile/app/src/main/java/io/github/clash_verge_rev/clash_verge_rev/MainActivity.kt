package io.github.clash_verge_rev.clash_verge_rev

import android.app.Activity
import android.content.*
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.clash_verge_rev.clash_verge_rev.service.ClashVpnService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * Clash Verge Rev MainActivity
 * 原生 Android 主 Activity
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val VPN_REQUEST_CODE = 100
        const val ACTION_VPN_STATUS = "io.github.clash_verge_rev.VPN_STATUS"
        const val EXTRA_CONNECTED = "connected"
    }

    // VPN状态
    private val vpnStatusState = mutableStateOf(false)
    
    // VPN状态广播接收器
    private val vpnStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_VPN_STATUS) {
                val isConnected = intent.getBooleanExtra(EXTRA_CONNECTED, false)
                vpnStatusState.value = isConnected
            }
        }
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // VPN 权限已授予，启动服务
            startVpnService()
        } else {
            // 用户拒绝了 VPN 权限
            onVpnPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 注册VPN状态广播接收器
        val filter = IntentFilter(ACTION_VPN_STATUS)
        registerReceiver(vpnStatusReceiver, filter, Context.RECEIVER_EXPORTED)
        
        // 设置 Compose UI
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        vpnStatusState = vpnStatusState,
                        onStartVpn = { requestVpnPermission() },
                        onStopVpn = { stopVpnService() }
                    )
                }
            }
        }
        
        // 处理 Deep Link
        handleDeepLink(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(vpnStatusReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleDeepLink(it) }
    }

    /**
     * 处理 Deep Link（订阅导入等）
     */
    private fun handleDeepLink(intent: Intent) {
        val data = intent.data ?: return
        val scheme = data.scheme ?: return
        
        when (scheme) {
            "clash", "clash-verge" -> {
                // 处理 clash:// 或 clash-verge:// 链接
                val url = data.toString()
                handleSubscriptionImport(url)
            }
            "http", "https" -> {
                // 处理配置文件链接
                if (data.path?.endsWith(".yaml") == true || data.path?.endsWith(".yml") == true) {
                    val url = data.toString()
                    handleConfigImport(url)
                }
            }
        }
    }
    
    /**
     * 处理订阅导入
     */
    private fun handleSubscriptionImport(url: String) {
        // 显示导入对话框
        runOnUiThread {
            // TODO: 实现订阅导入UI
        }
    }
    
    /**
     * 处理配置导入
     */
    private fun handleConfigImport(url: String) {
        // 下载并保存配置
        runOnUiThread {
            // TODO: 实现配置导入UI
        }
    }

    /**
     * 请求 VPN 权限
     */
    fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            // 已经有权限，直接启动
            startVpnService()
        }
    }

    /**
     * 启动 VPN 服务
     */
    private fun startVpnService() {
        // ✅ 首先检查VPN权限
        val intent = VpnService.prepare(this)
        if (intent != null) {
            // 需要请求权限
            android.util.Log.i("MainActivity", "Requesting VPN permission...")
            vpnPermissionLauncher.launch(intent)
            return
        }
        
        // 已有权限，继续启动
        val settingsManager = io.github.clash_verge_rev.clash_verge_rev.data.SettingsManager.getInstance(this)
        var currentConfigPath = settingsManager.currentConfigPath.value
        
        // 如果没有选中配置，自动选择第一个可用配置
        if (currentConfigPath.isEmpty()) {
            lifecycleScope.launch {
                val profileStorage = io.github.clash_verge_rev.clash_verge_rev.data.ProfileStorage.getInstance(this@MainActivity)
                val profiles = profileStorage.getAllProfiles()
                if (profiles.isNotEmpty()) {
                    val profile = profiles[0]
                    currentConfigPath = File(filesDir, "configs/${profile.uid}.yaml").absolutePath
                    settingsManager.setCurrentConfigPath(currentConfigPath)
                    android.util.Log.i("MainActivity", "Auto-selected first config: $currentConfigPath")
                    android.widget.Toast.makeText(this@MainActivity, "已自动选择配置: ${profile.name}", android.widget.Toast.LENGTH_SHORT).show()
                    
                    // 继续启动VPN
                    startVpnServiceWithConfig(currentConfigPath)
                } else {
                    android.widget.Toast.makeText(this@MainActivity, "请先在配置页面导入订阅", android.widget.Toast.LENGTH_LONG).show()
                }
            }
            return
        }
        
        startVpnServiceWithConfig(currentConfigPath)
    }
    
    private fun startVpnServiceWithConfig(configPath: String) {
        val configFile = java.io.File(configPath)
        if (!configFile.exists()) {
            android.util.Log.e("MainActivity", "Config file not found: $configPath")
            android.widget.Toast.makeText(this, "配置文件不存在，请重新选择", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        android.util.Log.i("MainActivity", "Starting VPN with config: $configPath")
        val intent = Intent(this, ClashVpnService::class.java)
        intent.action = ClashVpnService.ACTION_START
        intent.putExtra("config_path", configPath)
        startService(intent)
        android.util.Log.i("MainActivity", "VPN service start command sent")
    }

    /**
     * 停止 VPN 服务
     */
    fun stopVpnService() {
        val intent = Intent(this, ClashVpnService::class.java)
        intent.action = ClashVpnService.ACTION_STOP
        startService(intent)
    }

    /**
     * VPN 权限被拒绝时的处理
     */
    private fun onVpnPermissionDenied() {
        runOnUiThread {
            // 通过Compose状态显示Snackbar
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vpnStatusState: MutableState<Boolean>,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit
) {
    val isVpnRunning by vpnStatusState
    var currentTab by remember { mutableStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val tabs = listOf(
        TabItem("首页", Icons.Rounded.Home),
        TabItem("代理", Icons.Rounded.Wifi),
        TabItem("订阅", Icons.Rounded.Dns),
        TabItem("连接", Icons.Rounded.Language),
        TabItem("规则", Icons.Rounded.TrendingUp),
        TabItem("日志", Icons.Rounded.Subject),
        TabItem("测试", Icons.Rounded.Lock),
        TabItem("设置", Icons.Rounded.Settings)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Clash Verge Rev")
                        if (isVpnRunning) {
                            Text(
                                "运行中",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "更多")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("设置") },
                            onClick = {
                                showMenu = false
                                showSettings = true
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("关于") },
                            onClick = {
                                showMenu = false
                                showAbout = true
                            },
                            leadingIcon = { Icon(Icons.Default.Info, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = currentTab == index,
                        onClick = { currentTab = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                0 -> io.github.clash_verge_rev.clash_verge_rev.ui.HomeScreen(
                    isVpnRunning = isVpnRunning,
                    onStartVpn = onStartVpn,
                    onStopVpn = onStopVpn
                )
                1 -> io.github.clash_verge_rev.clash_verge_rev.ui.ProxyScreen()
                2 -> io.github.clash_verge_rev.clash_verge_rev.ui.ConfigScreenNew()
                3 -> ConnectionsScreen(isVpnRunning = isVpnRunning)
                4 -> io.github.clash_verge_rev.clash_verge_rev.ui.RulesScreen()  // ✅ 规则页面
                5 -> io.github.clash_verge_rev.clash_verge_rev.ui.LogScreen()
                6 -> io.github.clash_verge_rev.clash_verge_rev.ui.TestScreen()  // ✅ 测试页面
                7 -> io.github.clash_verge_rev.clash_verge_rev.ui.SettingsScreen()  // ✅ 设置页面
            }
        }
    }
    
    // 设置对话框（全屏）
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("关闭")
                }
            },
            title = { Text("设置") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                ) {
                    io.github.clash_verge_rev.clash_verge_rev.ui.SettingsScreen()
                }
            }
        )
    }
    
    // 关于对话框
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("关闭")
                }
            },
            icon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("关于 Clash Verge Rev") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Clash Verge Rev",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "版本 2.4.3",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "基于 Mihomo 核心的代理工具",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "核心版本: Mihomo 1.18.1",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "© 2024 Clash Verge Rev",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

data class TabItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun HomeTab(
    isVpnRunning: Boolean,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 状态指示器 - 大圆形按钮
        Card(
            modifier = Modifier
                .size(200.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = if (isVpnRunning) 
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            onClick = {
                if (isVpnRunning) onStopVpn() else onStartVpn()
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isVpnRunning) 
                            Icons.Default.CheckCircle 
                        else 
                            Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (isVpnRunning)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (isVpnRunning) "已连接" else "未连接",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isVpnRunning)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (isVpnRunning) {
                        Text(
                            text = "点击断开",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "点击连接",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 当前代理信息
        if (isVpnRunning) {
            Card(
                modifier = Modifier.fillMaxWidth()
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
                            text = "当前代理",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "DIRECT",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "查看",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 流量统计
        if (isVpnRunning) {
            io.github.clash_verge_rev.clash_verge_rev.ui.TrafficCard()
        } else {
            // 空状态提示
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "点击上方按钮开始连接",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "连接后可查看实时流量统计",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))

        // 快捷操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("规则功能开发中")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Rule, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("规则")
            }
            
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("延迟测试功能开发中")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Speed, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("测速")
            }
        }
    }
}

@Composable
fun ConfigTab(snackbarHostState: SnackbarHostState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configDir = File(context.filesDir, "config")
    val coroutineScope = rememberCoroutineScope()
    
    io.github.clash_verge_rev.clash_verge_rev.ui.ConfigScreen(
        configDir = configDir,
        onLoadConfig = { file ->
            // 加载配置
            coroutineScope.launch {
                try {
                    val result = io.github.clash_verge_rev.clash_verge_rev.core.ClashCore.loadConfig(file)
                    if (result == 0) {
                        snackbarHostState.showSnackbar("配置加载成功")
                    } else {
                        snackbarHostState.showSnackbar("配置加载失败: $result")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("配置加载错误: ${e.message}")
                }
            }
        }
    )
}

/**
 * 连接管理页面 - 完全复刻桌面端Connections页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(isVpnRunning: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val connectionManager = remember { io.github.clash_verge_rev.clash_verge_rev.data.ConnectionManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    // 从ConnectionManager获取状态
    val connectionsState by connectionManager.connectionsState.collectAsState()
    val isPaused by connectionManager.isPaused.collectAsState()
    
    // UI状态
    var searchQuery by remember { mutableStateOf("") }
    var sortType by remember { mutableStateOf(io.github.clash_verge_rev.clash_verge_rev.data.ConnectionSortType.DEFAULT) }
    var selectedConnection by remember { mutableStateOf<io.github.clash_verge_rev.clash_verge_rev.data.Connection?>(null) }
    
    // 启动/停止自动更新
    LaunchedEffect(isVpnRunning) {
        if (isVpnRunning) {
            connectionManager.startUpdating()
        } else {
            connectionManager.stopUpdating()
        }
    }
    
    // 过滤和排序连接
    val filteredConnections = remember(connectionsState, searchQuery, sortType) {
        var connections = connectionsState.connections
        
        // 搜索过滤
        if (searchQuery.isNotEmpty()) {
            connections = connections.filter { conn ->
                conn.metadata.host.contains(searchQuery, ignoreCase = true) ||
                conn.metadata.destinationIP.contains(searchQuery, ignoreCase = true) ||
                conn.metadata.process.contains(searchQuery, ignoreCase = true)
            }
        }
        
        // 排序
        connections = when (sortType) {
            io.github.clash_verge_rev.clash_verge_rev.data.ConnectionSortType.DEFAULT -> connections.sortedByDescending {
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(it.start)?.time ?: 0
                } catch (e: Exception) {
                    0
                }
            }
            io.github.clash_verge_rev.clash_verge_rev.data.ConnectionSortType.UPLOAD -> connections.sortedByDescending { it.curUpload }
            io.github.clash_verge_rev.clash_verge_rev.data.ConnectionSortType.DOWNLOAD -> connections.sortedByDescending { it.curDownload }
        }
        
        connections
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 流量统计卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "已下载",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        io.github.clash_verge_rev.clash_verge_rev.ui.formatBytes(connectionsState.downloadTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "已上传",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        io.github.clash_verge_rev.clash_verge_rev.ui.formatBytes(connectionsState.uploadTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "活动连接",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${filteredConnections.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
        
        // 搜索和控制栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索主机、IP或进程") },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true
            )
            
            // 暂停/继续按钮
            IconButton(onClick = { connectionManager.togglePause() }) {
                Icon(
                    if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = if (isPaused) "继续" else "暂停"
                )
            }
            
            // 排序按钮
            IconButton(onClick = {
                sortType = when (sortType) {
                    io.github.clash_verge_rev.clash_verge_rev.data.ConnectionSortType.DEFAULT -> 
                        io.github.clash_verge_rev.clash_verge_rev.data.ConnectionSortType.UPLOAD
                    io.github.clash_verge_rev.clash_verge_rev.data.ConnectionSortType.UPLOAD -> 
                        io.github.clash_verge_rev.clash_verge_rev.data.ConnectionSortType.DOWNLOAD
                    io.github.clash_verge_rev.clash_verge_rev.data.ConnectionSortType.DOWNLOAD -> 
                        io.github.clash_verge_rev.clash_verge_rev.data.ConnectionSortType.DEFAULT
                }
            }) {
                Icon(
                    when (sortType) {
                        io.github.clash_verge_rev.clash_verge_rev.data.ConnectionSortType.DEFAULT -> Icons.Rounded.Schedule
                        io.github.clash_verge_rev.clash_verge_rev.data.ConnectionSortType.UPLOAD -> Icons.Rounded.Upload
                        io.github.clash_verge_rev.clash_verge_rev.data.ConnectionSortType.DOWNLOAD -> Icons.Rounded.Download
                    },
                    contentDescription = "排序方式"
                )
            }
            
            // 关闭所有连接按钮
            IconButton(
                onClick = {
                    scope.launch {
                        connectionManager.closeAllConnections()
                    }
                },
                enabled = connectionsState.connections.isNotEmpty()
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "关闭所有连接")
            }
        }
        
        // 连接列表
        if (filteredConnections.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (searchQuery.isEmpty()) "暂无连接" else "未找到匹配的连接",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredConnections.size) { index ->
                    val connection = filteredConnections[index]
                    ConnectionItemCard(
                        connection = connection,
                        onClick = { selectedConnection = connection },
                        onClose = {
                            scope.launch {
                                connectionManager.closeConnection(connection.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionItemCard(
    connection: io.github.clash_verge_rev.clash_verge_rev.data.Connection,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 主机或目标IP
                Text(
                    connection.metadata.host.ifEmpty { connection.metadata.destinationIP },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    maxLines = 1
                )
                
                Spacer(Modifier.height(4.dp))
                
                // 标签行
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 网络类型
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                connection.metadata.network.uppercase(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                    
                    // 连接类型
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                connection.metadata.type,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
            
            // 关闭按钮
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "关闭连接",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 占位页面
 */
@Composable
fun PlaceholderScreen(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "$title 功能",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "正在开发中...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


