package io.github.clash_verge_rev.clash_verge_rev.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import io.github.clash_verge_rev.clash_verge_rev.data.SettingsManager
import kotlinx.coroutines.launch

/**
 * 设置界面 - 对应桌面端所有设置功能
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ============ 系统设置 ============
        item {
            SettingsSection(title = "系统设置")
        }
        
        // TUN模式（虚拟网卡模式）
        item {
            val tunMode by settingsManager.tunMode
            var showTunSettings by remember { mutableStateOf(false) }
            
            SettingsSwitchItem(
                icon = Icons.Default.Security,
                title = "TUN模式",
                subtitle = if (tunMode) "虚拟网卡已启用" else "虚拟网卡已禁用",
                checked = tunMode,
                onCheckedChange = { settingsManager.setTunMode(it) },
                action = {
                    IconButton(onClick = { showTunSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "配置")
                    }
                }
            )
            
            TunSettingsDialog(
                show = showTunSettings,
                onDismiss = { showTunSettings = false }
            )
        }
        
        // 系统代理
        item {
            val systemProxy by settingsManager.systemProxy
            SettingsSwitchItem(
                icon = Icons.Default.Public,
                title = "系统代理",
                subtitle = if (systemProxy) "已接管系统网络" else "未接管系统网络",
                checked = systemProxy,
                onCheckedChange = { settingsManager.setSystemProxy(it) }
            )
        }
        
        // 开机自启
        item {
            val autoStart by settingsManager.autoStart
            SettingsSwitchItem(
                icon = Icons.Default.PowerSettingsNew,
                title = "开机自启",
                subtitle = if (autoStart) "开机时自动启动应用" else "需手动启动应用",
                checked = autoStart,
                onCheckedChange = { settingsManager.setAutoStart(it) }
            )
        }
        
        // 静默启动
        item {
            val silentStart by settingsManager.silentStart
            SettingsSwitchItem(
                icon = Icons.Default.VisibilityOff,
                title = "静默启动",
                subtitle = if (silentStart) "启动时最小化到托盘" else "启动时显示主窗口",
                checked = silentStart,
                onCheckedChange = { settingsManager.setSilentStart(it) }
            )
        }
        
        // ============ Clash 设置 ============
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSection(title = "Clash 设置")
        }
        
        // 局域网连接
        item {
            val allowLan by settingsManager.allowLan
            SettingsSwitchItem(
                icon = Icons.Default.Wifi,
                title = "局域网连接",
                subtitle = if (allowLan) "允许局域网设备连接" else "仅本机可访问",
                checked = allowLan,
                onCheckedChange = { settingsManager.setAllowLan(it) }
            )
        }
        
        // DNS覆写
        item {
            val dnsOverwrite by settingsManager.dnsOverwrite
            SettingsSwitchItem(
                icon = Icons.Default.Dns,
                title = "DNS 覆写",
                subtitle = if (dnsOverwrite) "使用自定义DNS配置" else "使用系统DNS",
                checked = dnsOverwrite,
                onCheckedChange = { settingsManager.setDnsOverwrite(it) }
            )
        }
        
        // IPv6支持
        item {
            val ipv6 by settingsManager.ipv6
            SettingsSwitchItem(
                icon = Icons.Default.Language,
                title = "IPv6 支持",
                subtitle = if (ipv6) "已启用IPv6网络" else "仅使用IPv4",
                checked = ipv6,
                onCheckedChange = { settingsManager.setIpv6(it) }
            )
        }
        
        // 统一延迟
        item {
            val unifiedDelay by settingsManager.unifiedDelay
            SettingsSwitchItem(
                icon = Icons.Default.Speed,
                title = "统一延迟",
                subtitle = if (unifiedDelay) "使用TCP延迟测速" else "使用ICMP延迟测速",
                checked = unifiedDelay,
                onCheckedChange = { settingsManager.setUnifiedDelay(it) }
            )
        }
        
        // 日志等级
        item {
            val logLevels = listOf("Debug", "Info", "Warn", "Error", "Silent")
            val selectedLogLevel by settingsManager.logLevel
            var showDialog by remember { mutableStateOf(false) }
            
            SettingsItem(
                icon = Icons.Default.BugReport,
                title = "日志等级",
                subtitle = selectedLogLevel,
                onClick = { showDialog = true }
            )
            
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("选择日志等级") },
                    text = {
                        Column {
                            logLevels.forEach { level ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            settingsManager.setLogLevel(level)
                                            showDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedLogLevel == level,
                                        onClick = {
                                            settingsManager.setLogLevel(level)
                                            showDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(level)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
        
        // ============ 端口设置 ============
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSection(title = "端口设置")
        }
        
        // 混合端口
        item {
            val mixedPort by settingsManager.mixedPort
            var showDialog by remember { mutableStateOf(false) }
            
            SettingsItem(
                icon = Icons.Default.Settings,
                title = "混合端口",
                subtitle = mixedPort,
                onClick = { showDialog = true }
            )
            
            if (showDialog) {
                var tempPort by remember { mutableStateOf(mixedPort) }
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("设置混合端口") },
                    text = {
                        OutlinedTextField(
                            value = tempPort,
                            onValueChange = { tempPort = it },
                            label = { Text("端口号") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                settingsManager.setMixedPort(tempPort)
                                showDialog = false
                            }
                        ) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
        
        // 外部控制
        item {
            val externalController by settingsManager.externalController
            var showDialog by remember { mutableStateOf(false) }
            
            SettingsItem(
                icon = Icons.Default.Api,
                title = "外部控制",
                subtitle = externalController,
                onClick = { showDialog = true }
            )
            
            if (showDialog) {
                var tempController by remember { mutableStateOf(externalController) }
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("外部控制地址") },
                    text = {
                        OutlinedTextField(
                            value = tempController,
                            onValueChange = { tempController = it },
                            label = { Text("地址:端口") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                settingsManager.setExternalController(tempController)
                                showDialog = false
                            }
                        ) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
        
        // 网页界面
        item {
            SettingsItem(
                icon = Icons.Default.Web,
                title = "网页界面",
                subtitle = "Clash Dashboard",
                onClick = { /* TODO: 打开网页界面 */ }
            )
        }
        
        // ============ Clash 内核 ============
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSection(title = "Clash 内核")
        }
        
        // 核心版本
        item {
            SettingsItem(
                icon = Icons.Default.Code,
                title = "核心版本",
                subtitle = "Mihomo 1.18.1",
                onClick = { /* TODO: 显示核心详情 */ }
            )
        }
        
        // 更新GeoData
        item {
            SettingsItem(
                icon = Icons.Default.Update,
                title = "更新 GeoData",
                subtitle = "更新地理位置数据库",
                onClick = { /* TODO: 更新GeoData */ }
            )
        }
        
        // ============ 外观设置 ============
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSection(title = "外观设置")
        }
        
        // 主题模式
        item {
            val themes = listOf("跟随系统", "浅色", "深色")
            val selectedTheme by settingsManager.theme
            var showDialog by remember { mutableStateOf(false) }
            
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "主题模式",
                subtitle = selectedTheme,
                onClick = { showDialog = true }
            )
            
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("选择主题") },
                    text = {
                        Column {
                            themes.forEach { theme ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            settingsManager.setTheme(theme)
                                            showDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedTheme == theme,
                                        onClick = {
                                            settingsManager.setTheme(theme)
                                            showDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(theme)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
        
        // ============ 关于 ============
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSection(title = "关于")
        }
        
        // 版本信息
        item {
            var showVersionDialog by remember { mutableStateOf(false) }
            val appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
            } catch (e: Exception) {
                "未知"
            }
            
            SettingsItem(
                icon = Icons.Default.Info,
                title = "版本信息",
                subtitle = "Clash Verge Rev Mobile $appVersion",
                onClick = { showVersionDialog = true }
            )
            
            if (showVersionDialog) {
                AlertDialog(
                    onDismissRequest = { showVersionDialog = false },
                    icon = { Icon(Icons.Default.Info, null) },
                    title = { Text("版本信息") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            VersionInfoRow("应用名称", "Clash Verge Rev Mobile")
                            VersionInfoRow("应用版本", appVersion)
                            VersionInfoRow("Clash内核", io.github.clash_verge_rev.clash_verge_rev.core.ClashCore.getVersion())
                            VersionInfoRow("包名", context.packageName)
                            
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            
                            Text(
                                "基于 Clash Verge Rev 桌面版开发\n" +
                                "使用 Jetpack Compose 构建",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showVersionDialog = false }) {
                            Text("关闭")
                        }
                    }
                )
            }
        }
        
        // Clash核心版本
        item {
            SettingsItem(
                icon = Icons.Default.Memory,
                title = "Clash 内核",
                subtitle = io.github.clash_verge_rev.clash_verge_rev.core.ClashCore.getVersion().ifEmpty { "未初始化" },
                onClick = { }
            )
        }
        
        // GitHub仓库
        item {
            val githubUrl = "https://github.com/clash-verge-rev/clash-verge-rev"
            
            SettingsItem(
                icon = Icons.Default.Code,
                title = "GitHub 仓库",
                subtitle = "查看源代码和贡献",
                onClick = {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse(githubUrl)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Failed to open GitHub", e)
                    }
                }
            )
        }
        
        // 检查更新
        item {
            var isCheckingUpdate by remember { mutableStateOf(false) }
            var showUpdateDialog by remember { mutableStateOf(false) }
            var updateMessage by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()
            
            SettingsItem(
                icon = Icons.Default.SystemUpdate,
                title = "检查更新",
                subtitle = if (isCheckingUpdate) "检查中..." else "点击检查应用更新",
                onClick = {
                    isCheckingUpdate = true
                    // TODO: 实际的更新检查逻辑
                    scope.launch {
                        kotlinx.coroutines.delay(1000)
                        isCheckingUpdate = false
                        updateMessage = "当前已是最新版本"
                        showUpdateDialog = true
                    }
                }
            )
            
            if (showUpdateDialog) {
                AlertDialog(
                    onDismissRequest = { showUpdateDialog = false },
                    icon = { Icon(Icons.Default.CheckCircle, null) },
                    title = { Text("更新检查") },
                    text = { Text(updateMessage) },
                    confirmButton = {
                        TextButton(onClick = { showUpdateDialog = false }) {
                            Text("确定")
                        }
                    }
                )
            }
        }
        
        // 开源许可
        item {
            var showLicenseDialog by remember { mutableStateOf(false) }
            
            SettingsItem(
                icon = Icons.Default.Description,
                title = "开源许可",
                subtitle = "GPL-3.0 License",
                onClick = { showLicenseDialog = true }
            )
            
            if (showLicenseDialog) {
                AlertDialog(
                    onDismissRequest = { showLicenseDialog = false },
                    icon = { Icon(Icons.Default.Description, null) },
                    title = { Text("开源许可") },
                    text = {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "本项目采用 GPL-3.0 许可证",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "这意味着您可以自由地使用、修改和分发本软件，" +
                                "但必须保持相同的许可证，并公开源代码。",
                                style = MaterialTheme.typography.bodySmall
                            )
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Text(
                                "本软件基于以下开源项目：",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("• Clash Meta Core", style = MaterialTheme.typography.bodySmall)
                            Text("• Jetpack Compose", style = MaterialTheme.typography.bodySmall)
                            Text("• Material Design 3", style = MaterialTheme.typography.bodySmall)
                            Text("• Kotlin Coroutines", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLicenseDialog = false }) {
                            Text("关闭")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun VersionInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    action: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            action?.invoke()
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
