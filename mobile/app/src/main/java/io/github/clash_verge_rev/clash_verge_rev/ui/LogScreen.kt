package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.clash_verge_rev.clash_verge_rev.data.SettingsManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(
    val level: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val color: Color
        get() = when (level) {
            "ERROR" -> Color(0xFFE57373)
            "WARN" -> Color(0xFFFFB74D)
            "INFO" -> Color(0xFF64B5F6)
            "DEBUG" -> Color(0xFF81C784)
            else -> Color.Gray
        }
}

@Composable
fun LogScreen() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentConfigPath by settingsManager.currentConfigPath
    
    var logs by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    var autoScroll by remember { mutableStateOf(true) }
    var selectedLevel by remember { mutableStateOf("ALL") }
    var showFilterMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    // 监听配置路径变化
    LaunchedEffect(currentConfigPath) {
        if (currentConfigPath.isEmpty()) {
            // 没有配置时清空日志
            logs = emptyList()
        } else {
            // 有配置时显示初始日志
            logs = listOf(
                LogEntry("INFO", "配置已加载: ${currentConfigPath.substringAfterLast("/")}"),
                LogEntry("INFO", "Clash核心已初始化"),
            )
        }
    }
    
    // 只在有配置时生成模拟日志（TODO: 替换为实际日志读取）
    LaunchedEffect(currentConfigPath, logs.size) {
        if (currentConfigPath.isEmpty()) return@LaunchedEffect
        
        while (true) {
            delay(5000)
            val newLog = LogEntry(
                level = listOf("INFO", "DEBUG").random(),
                message = when ((0..3).random()) {
                    0 -> "连接建立: ${listOf("香港节点", "美国节点", "日本节点", "DIRECT").random()}"
                    1 -> "处理请求: ${listOf("www.google.com", "www.youtube.com", "github.com").random()}"
                    2 -> "DNS查询: ${listOf("8.8.8.8", "1.1.1.1").random()}"
                    else -> "流量统计更新"
                }
            )
            logs = (logs + newLog).takeLast(100)
            
            if (autoScroll && logs.isNotEmpty()) {
                listState.animateScrollToItem(logs.size - 1)
            }
        }
    }
    
    // 筛选日志
    val filteredLogs = if (selectedLevel == "ALL") {
        logs
    } else {
        logs.filter { it.level == selectedLevel }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部控制栏
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
                            text = "日志",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${filteredLogs.size} / ${logs.size} 条",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 筛选按钮
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, "筛选")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            listOf("ALL", "ERROR", "WARN", "INFO", "DEBUG").forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level) },
                                    onClick = {
                                        selectedLevel = level
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        if (selectedLevel == level) {
                                            Icon(Icons.Default.Check, null)
                                        }
                                    }
                                )
                            }
                        }
                        
                        // 清空按钮
                        IconButton(onClick = { logs = emptyList() }) {
                            Icon(Icons.Default.Delete, "清空")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 自动滚动开关
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "自动滚动",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = autoScroll,
                        onCheckedChange = { autoScroll = it }
                    )
                }
            }
        }
        
        // 日志列表
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (currentConfigPath.isEmpty()) Icons.Default.Warning else Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (currentConfigPath.isEmpty()) 
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                        else 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (currentConfigPath.isEmpty()) 
                            "请先导入配置" 
                        else 
                            "暂无日志",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (currentConfigPath.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "导入订阅配置后将显示运行日志",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredLogs) { log ->
                    LogItem(log)
                }
            }
        }
    }
}

@Composable
fun LogItem(log: LogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp, 40.dp)
                    .background(log.color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row {
                    Text(
                        text = log.level,
                        style = MaterialTheme.typography.labelSmall,
                        color = log.color
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            .format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


