package io.github.clash_verge_rev.clash_verge_rev

import android.app.Activity
import android.content.*
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.clash_verge_rev.clash_verge_rev.service.ClashVpnService
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
        val settingsManager = io.github.clash_verge_rev.clash_verge_rev.data.SettingsManager.getInstance(this)
        val currentConfigPath = settingsManager.currentConfigPath.value
        
        if (currentConfigPath.isEmpty()) {
            android.widget.Toast.makeText(this, "请先在配置页面选择订阅", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        val configFile = java.io.File(currentConfigPath)
        if (!configFile.exists()) {
            android.widget.Toast.makeText(this, "配置文件不存在，请重新选择", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(this, ClashVpnService::class.java)
        intent.action = ClashVpnService.ACTION_START
        intent.putExtra("config_path", currentConfigPath)
        startService(intent)
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
        TabItem("主页", Icons.Default.Home),
        TabItem("节点", Icons.Default.Public),
        TabItem("配置", Icons.Default.Settings),
        TabItem("日志", Icons.Default.Article)
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
                3 -> io.github.clash_verge_rev.clash_verge_rev.ui.LogScreen()
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

        // 当前节点信息
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
                            text = "当前节点",
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


