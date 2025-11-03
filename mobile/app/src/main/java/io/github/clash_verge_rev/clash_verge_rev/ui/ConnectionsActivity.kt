package io.github.clash_verge_rev.clash_verge_rev.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.github.clash_verge_rev.clash_verge_rev.data.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * 连接详情页面 - 完全复刻桌面端 Connections 页面
 */
class ConnectionsActivity : ComponentActivity() {
    
    private lateinit var connectionManager: ConnectionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        connectionManager = ConnectionManager.getInstance(this)
        connectionManager.startUpdating()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConnectionsScreen(
                        onBack = { finish() }
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        connectionManager.stopUpdating()
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ConnectionsScreen(onBack: () -> Unit) {
        val connectionsState by connectionManager.connectionsState.collectAsState()
        val isPaused by connectionManager.isPaused.collectAsState()
        
        // UI状态
        var searchQuery by remember { mutableStateOf("") }
        var sortType by remember { mutableStateOf(ConnectionSortType.DEFAULT) }
        var selectedConnection by remember { mutableStateOf<Connection?>(null) }
        var showSnackbar by remember { mutableStateOf(false) }
        var snackbarMessage by remember { mutableStateOf("") }
        
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
                ConnectionSortType.DEFAULT -> connections.sortedByDescending {
                    try {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(it.start)?.time ?: 0
                    } catch (e: Exception) {
                        0
                    }
                }
                ConnectionSortType.UPLOAD -> connections.sortedByDescending { it.curUpload }
                ConnectionSortType.DOWNLOAD -> connections.sortedByDescending { it.curDownload }
            }
            
            connections
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("连接") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        // 暂停/继续按钮
                        IconButton(onClick = { connectionManager.togglePause() }) {
                            Icon(
                                if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = if (isPaused) "继续" else "暂停"
                            )
                        }
                        
                        // 关闭所有连接按钮
                        IconButton(
                            onClick = {
                                lifecycleScope.launch {
                                    val success = connectionManager.closeAllConnections()
                                    snackbarMessage = if (success) "已关闭所有连接" else "关闭失败"
                                    showSnackbar = true
                                }
                            },
                            enabled = connectionsState.connections.isNotEmpty()
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭所有连接")
                        }
                    }
                )
            },
            snackbarHost = {
                if (showSnackbar) {
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        action = {
                            TextButton(onClick = { showSnackbar = false }) {
                                Text("确定")
                            }
                        }
                    ) {
                        Text(snackbarMessage)
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
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
                        TrafficInfo("已下载", connectionsState.downloadTotal, MaterialTheme.colorScheme.primary)
                        TrafficInfo("已上传", connectionsState.uploadTotal, MaterialTheme.colorScheme.secondary)
                        ConnectionCount(filteredConnections.size)
                    }
                }
                
                // 搜索和排序栏
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
                    
                    // 排序按钮
                    IconButton(onClick = {
                        sortType = when (sortType) {
                            ConnectionSortType.DEFAULT -> ConnectionSortType.UPLOAD
                            ConnectionSortType.UPLOAD -> ConnectionSortType.DOWNLOAD
                            ConnectionSortType.DOWNLOAD -> ConnectionSortType.DEFAULT
                        }
                    }) {
                        Icon(
                            when (sortType) {
                                ConnectionSortType.DEFAULT -> Icons.Rounded.Schedule
                                ConnectionSortType.UPLOAD -> Icons.Rounded.Upload
                                ConnectionSortType.DOWNLOAD -> Icons.Rounded.Download
                            },
                            contentDescription = "排序方式"
                        )
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredConnections, key = { it.id }) { connection ->
                            ConnectionItem(
                                connection = connection,
                                onClick = { selectedConnection = connection },
                                onClose = {
                                    lifecycleScope.launch {
                                        val success = connectionManager.closeConnection(connection.id)
                                        snackbarMessage = if (success) "连接已关闭" else "关闭失败"
                                        showSnackbar = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // 连接详情对话框
        if (selectedConnection != null) {
            ConnectionDetailDialog(
                connection = selectedConnection!!,
                onDismiss = { selectedConnection = null },
                onClose = {
                    lifecycleScope.launch {
                        val success = connectionManager.closeConnection(selectedConnection!!.id)
                        snackbarMessage = if (success) "连接已关闭" else "关闭失败"
                        showSnackbar = true
                        selectedConnection = null
                    }
                }
            )
        }
    }
    
    @Composable
    fun TrafficInfo(label: String, bytes: Long, color: androidx.compose.ui.graphics.Color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatBytes(bytes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
    
    @Composable
    fun ConnectionCount(count: Int) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "活动连接",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
    
    @Composable
    fun ConnectionItem(
        connection: Connection,
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
                        fontWeight = FontWeight.Medium,
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
                        
                        // 进程名称
                        if (connection.metadata.process.isNotEmpty()) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        connection.metadata.process,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        
                        // 流量显示（如果有显著流量）
                        if (connection.curUpload > 100 || connection.curDownload > 100) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "${formatBytes(connection.curUpload)}/s ↑ ${formatBytes(connection.curDownload)}/s ↓",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }
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
    
    @Composable
    fun ConnectionDetailDialog(
        connection: Connection,
        onDismiss: () -> Unit,
        onClose: () -> Unit
    ) {
        // 计算时间显示（移到Composable外避免try-catch问题）
        val timeDisplay = remember(connection.start) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = sdf.parse(connection.start)
                if (date != null) {
                    val now = Date()
                    val diff = now.time - date.time
                    val seconds = diff / 1000
                    when {
                        seconds < 60 -> "${seconds}秒前"
                        seconds < 3600 -> "${seconds / 60}分钟前"
                        seconds < 86400 -> "${seconds / 3600}小时前"
                        else -> "${seconds / 86400}天前"
                    }
                } else {
                    connection.start
                }
            } catch (e: Exception) {
                connection.start
            }
        }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("连接详情") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow("主机", connection.metadata.host.ifEmpty { connection.metadata.remoteDestination })
                    DetailRow("目标", "${connection.metadata.destinationIP}:${connection.metadata.destinationPort}")
                    DetailRow("来源", "${connection.metadata.sourceIP}:${connection.metadata.sourcePort}")
                    DetailRow("类型", "${connection.metadata.type} (${connection.metadata.network})")
                    DetailRow("规则", connection.rule + if (connection.rulePayload.isNotEmpty()) " (${connection.rulePayload})" else "")
                    DetailRow("链路", connection.chains.reversed().joinToString(" / "))
                    DetailRow("进程", connection.metadata.process + if (connection.metadata.processPath.isNotEmpty()) " (${connection.metadata.processPath})" else "")
                    DetailRow("已下载", formatBytes(connection.download))
                    DetailRow("已上传", formatBytes(connection.upload))
                    DetailRow("下载速度", "${formatBytes(connection.curDownload)}/s")
                    DetailRow("上传速度", "${formatBytes(connection.curUpload)}/s")
                    DetailRow("时间", timeDisplay)
                }
            },
            confirmButton = {
                Button(onClick = onClose) {
                    Text("关闭连接")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }
    
    @Composable
    fun DetailRow(label: String, value: String) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value.ifEmpty { "无" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    
    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        
        val df = DecimalFormat("#,##0.##")
        return df.format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
}

