package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.clash_verge_rev.clash_verge_rev.data.GeoDataManager
import kotlinx.coroutines.launch

/**
 * TUN模式设置对话框
 * 
 * 类似桌面端的TunViewer，提供：
 * 1. TUN参数配置
 * 2. GeoData下载管理
 * 3. 状态检查
 */
@Composable
fun TunSettingsDialog(
    show: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val geoDataManager = remember { GeoDataManager(context) }
    val tunConfigManager = remember { io.github.clash_verge_rev.clash_verge_rev.data.TunConfigManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // TUN配置状态（从TunConfigManager加载）
    val stackState by tunConfigManager.stack
    var stack by remember { mutableStateOf(stackState) }
    
    val deviceState by tunConfigManager.device
    var device by remember { mutableStateOf(deviceState) }
    
    val autoRouteState by tunConfigManager.autoRoute
    var autoRoute by remember { mutableStateOf(autoRouteState) }
    
    val strictRouteState by tunConfigManager.strictRoute
    var strictRoute by remember { mutableStateOf(strictRouteState) }
    
    val autoDetectInterfaceState by tunConfigManager.autoDetectInterface
    var autoDetectInterface by remember { mutableStateOf(autoDetectInterfaceState) }
    
    val dnsHijackState by tunConfigManager.dnsHijack
    var dnsHijack by remember { mutableStateOf(dnsHijackState) }
    
    val mtuState by tunConfigManager.mtu
    var mtu by remember { mutableStateOf(mtuState.toString()) }
    
    // 当对话框显示时，重新加载配置
    LaunchedEffect(show) {
        if (show) {
            stack = tunConfigManager.stack.value
            device = tunConfigManager.device.value
            autoRoute = tunConfigManager.autoRoute.value
            strictRoute = tunConfigManager.strictRoute.value
            autoDetectInterface = tunConfigManager.autoDetectInterface.value
            dnsHijack = tunConfigManager.dnsHijack.value
            mtu = tunConfigManager.mtu.value.toString()
        }
    }
    
    // GeoData状态
    var geoDataAvailable by remember { mutableStateOf(geoDataManager.isGeoDataAvailable()) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadStatus by remember { mutableStateOf("") }
    var showGeoDataInfo by remember { mutableStateOf(false) }
    
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(Icons.Default.Security, contentDescription = null)
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TUN 模式设置")
                    TextButton(onClick = {
                        tunConfigManager.resetToDefault()
                        // 更新UI状态
                        stack = "gvisor"
                        device = "Mihomo"
                        autoRoute = true
                        strictRoute = false
                        autoDetectInterface = true
                        dnsHijack = "any:53"
                        mtu = "1500"
                    }) {
                        Text("重置")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // GeoData状态卡片
                    GeoDataStatusCard(
                        available = geoDataAvailable,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        downloadStatus = downloadStatus,
                        onDownload = {
                            scope.launch {
                                isDownloading = true
                                downloadStatus = "准备下载..."
                                downloadProgress = 0
                                
                                geoDataManager.downloadGeoData(
                                    useCDN = false,
                                    onProgress = { status, progress ->
                                        downloadStatus = status
                                        downloadProgress = progress
                                    }
                                ).onSuccess {
                                    downloadStatus = "下载完成"
                                    geoDataAvailable = true
                                }.onFailure { e ->
                                    downloadStatus = "下载失败: ${e.message}"
                                }
                                
                                isDownloading = false
                            }
                        },
                        onDownloadCDN = {
                            scope.launch {
                                isDownloading = true
                                downloadStatus = "准备下载（使用CDN）..."
                                downloadProgress = 0
                                
                                geoDataManager.downloadGeoData(
                                    useCDN = true,
                                    onProgress = { status, progress ->
                                        downloadStatus = status
                                        downloadProgress = progress
                                    }
                                ).onSuccess {
                                    downloadStatus = "下载完成"
                                    geoDataAvailable = true
                                }.onFailure { e ->
                                    downloadStatus = "下载失败: ${e.message}"
                                }
                                
                                isDownloading = false
                            }
                        },
                        onShowInfo = { showGeoDataInfo = true },
                        onDelete = {
                            if (geoDataManager.deleteGeoData()) {
                                geoDataAvailable = false
                            }
                        }
                    )
                    
                    HorizontalDivider()
                    
                    // Stack模式
                    Text(
                        "网络栈模式",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("gvisor", "system", "mixed").forEach { mode ->
                            FilterChip(
                                selected = stack == mode,
                                onClick = { stack = mode },
                                label = { Text(mode.uppercase()) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // 设备名称
                    OutlinedTextField(
                        value = device,
                        onValueChange = { device = it },
                        label = { Text("设备名称") },
                        placeholder = { Text("Mihomo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // 开关选项
                    TunSwitchItem(
                        title = "自动路由",
                        subtitle = "自动配置系统路由表",
                        checked = autoRoute,
                        onCheckedChange = { autoRoute = it }
                    )
                    
                    TunSwitchItem(
                        title = "严格路由",
                        subtitle = "强制所有流量通过TUN",
                        checked = strictRoute,
                        onCheckedChange = { strictRoute = it }
                    )
                    
                    TunSwitchItem(
                        title = "自动检测接口",
                        subtitle = "自动选择网络接口",
                        checked = autoDetectInterface,
                        onCheckedChange = { autoDetectInterface = it }
                    )
                    
                    // DNS劫持
                    OutlinedTextField(
                        value = dnsHijack,
                        onValueChange = { dnsHijack = it },
                        label = { Text("DNS 劫持") },
                        placeholder = { Text("any:53") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // MTU
                    OutlinedTextField(
                        value = mtu,
                        onValueChange = { mtu = it },
                        label = { Text("MTU") },
                        placeholder = { Text("1500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 保存TUN配置
                        tunConfigManager.setStack(stack)
                        tunConfigManager.setDevice(device)
                        tunConfigManager.setAutoRoute(autoRoute)
                        tunConfigManager.setStrictRoute(strictRoute)
                        tunConfigManager.setAutoDetectInterface(autoDetectInterface)
                        tunConfigManager.setDnsHijack(dnsHijack)
                        tunConfigManager.setMtu(mtu.toIntOrNull() ?: 1500)
                        onDismiss()
                    }
                ) {
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
    
    // GeoData信息对话框
    if (showGeoDataInfo) {
        GeoDataInfoDialog(
            geoDataManager = geoDataManager,
            onDismiss = { showGeoDataInfo = false }
        )
    }
}

@Composable
fun GeoDataStatusCard(
    available: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    downloadStatus: String,
    onDownload: () -> Unit,
    onDownloadCDN: () -> Unit,
    onShowInfo: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (available)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (available) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (available)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    Text(
                        "GeoData 数据库",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (available) {
                    IconButton(onClick = onShowInfo) {
                        Icon(Icons.Default.Info, contentDescription = "详情")
                    }
                }
            }
            
            Text(
                if (available) "规则数据已就绪" else "需要下载规则数据才能使用TUN模式",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (isDownloading) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        downloadStatus,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!available) {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("下载")
                        }
                        OutlinedButton(
                            onClick = onDownloadCDN,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Speed, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CDN")
                        }
                    } else {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("更新")
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("删除")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TunSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun GeoDataInfoDialog(
    geoDataManager: GeoDataManager,
    onDismiss: () -> Unit
) {
    val info = remember { geoDataManager.getGeoDataInfo() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Info, contentDescription = null)
        },
        title = {
            Text("GeoData 详细信息")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GeoFileInfo(
                    name = "GeoIP",
                    exists = info["geoip_exists"] as Boolean,
                    size = info["geoip_size"] as String,
                    path = info["geoip_path"] as String
                )
                
                HorizontalDivider()
                
                GeoFileInfo(
                    name = "GeoSite",
                    exists = info["geosite_exists"] as Boolean,
                    size = info["geosite_size"] as String,
                    path = info["geosite_path"] as String
                )
                
                HorizontalDivider()
                
                GeoFileInfo(
                    name = "Country.mmdb",
                    exists = info["mmdb_exists"] as Boolean,
                    size = info["mmdb_size"] as String,
                    path = info["mmdb_path"] as String
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun GeoFileInfo(
    name: String,
    exists: Boolean,
    size: String,
    path: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (exists) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (exists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            "大小: $size",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "路径: $path",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

