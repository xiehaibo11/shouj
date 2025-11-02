package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.delay
import java.text.DecimalFormat

/**
 * 首页 - 对应桌面端完整功能
 */
@Composable
fun HomeScreen(
    isVpnRunning: Boolean,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 配置文件卡片
        ProfileCard()
        
        // 2. 当前代理卡片
        CurrentProxyCard(isVpnRunning)
        
        // 3. 网络设置卡片（TUN/系统代理）
        NetworkSettingsCard(
            tunMode = settingsManager.tunMode.value,
            systemProxy = settingsManager.systemProxy.value,
            onTunModeChange = { settingsManager.setTunMode(it) },
            onSystemProxyChange = { settingsManager.setSystemProxy(it) }
        )
        
        // 4. 代理模式卡片
        ProxyModeCard()
        
        // 5. 连接控制卡片
        ConnectionCard(
            isVpnRunning = isVpnRunning,
            onStartVpn = onStartVpn,
            onStopVpn = onStopVpn
        )
        
        // 6. 流量统计卡片
        TrafficStatsCard(isVpnRunning)
        
        // 7. IP信息卡片
        IpInfoCard(isVpnRunning)
    }
}

/**
 * 配置文件卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCard() {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(if (expanded) 180f else 0f)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            "配置文件",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "当前配置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    ProfileInfoRow("订阅地址", "未配置")
                    ProfileInfoRow("更新时间", "从未更新")
                    ProfileInfoRow("节点数量", "0")
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* TODO: 更新配置 */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("更新")
                        }
                        OutlinedButton(
                            onClick = { /* TODO: 编辑配置 */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("编辑")
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
 * 当前代理卡片
 */
@Composable
fun CurrentProxyCard(isVpnRunning: Boolean) {
    InfoCard(
        icon = Icons.Default.Public,
        title = "当前代理",
        iconColor = MaterialTheme.colorScheme.secondary
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isVpnRunning) "DIRECT" else "未连接",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isVpnRunning) "直连模式" else "代理未启动",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isVpnRunning) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
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
                        settingsManager.setClashMode(modeValue)
                        
                        // 如果VPN正在运行，通知重新加载配置
                        val intent = android.content.Intent(context, io.github.clash_verge_rev.clash_verge_rev.service.ClashVpnService::class.java)
                        intent.action = io.github.clash_verge_rev.clash_verge_rev.service.ClashVpnService.ACTION_RESTART
                        try {
                            context.startService(intent)
                        } catch (e: Exception) {
                            android.util.Log.w("HomeScreen", "Failed to restart VPN service", e)
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
 * 连接控制卡片
 */
@Composable
fun ConnectionCard(
    isVpnRunning: Boolean,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isVpnRunning)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (isVpnRunning) Icons.Default.CheckCircle else Icons.Default.PowerSettingsNew,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (isVpnRunning)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = if (isVpnRunning) "已连接" else "未连接",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = if (isVpnRunning) onStopVpn else onStartVpn,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVpnRunning)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (isVpnRunning) "断开连接" else "启动代理",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * 流量统计卡片
 */
@Composable
fun TrafficStatsCard(isVpnRunning: Boolean) {
    var uploadSpeed by remember { mutableStateOf(0L) }
    var downloadSpeed by remember { mutableStateOf(0L) }
    var totalUpload by remember { mutableStateOf(0L) }
    var totalDownload by remember { mutableStateOf(0L) }
    
    // 模拟流量统计（实际应该从ClashCore获取）
    LaunchedEffect(isVpnRunning) {
        if (isVpnRunning) {
            while (true) {
                uploadSpeed = (100..500).random() * 1024L
                downloadSpeed = (500..2000).random() * 1024L
                totalUpload += uploadSpeed
                totalDownload += downloadSpeed
                delay(1000)
            }
        } else {
            uploadSpeed = 0
            downloadSpeed = 0
        }
    }
    
    InfoCard(
        icon = Icons.Default.Speed,
        title = "流量统计",
        iconColor = MaterialTheme.colorScheme.secondary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 实时速度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TrafficItem(
                    icon = Icons.Default.ArrowUpward,
                    label = "上传",
                    value = formatBytes(uploadSpeed) + "/s",
                    color = MaterialTheme.colorScheme.error
                )
                TrafficItem(
                    icon = Icons.Default.ArrowDownward,
                    label = "下载",
                    value = formatBytes(downloadSpeed) + "/s",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Divider()
            
            // 总流量
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
                        formatBytes(totalUpload),
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
                        formatBytes(totalDownload),
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
 * 格式化字节数
 */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    
    val df = DecimalFormat("#,##0.##")
    return df.format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

