package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.clash_verge_rev.clash_verge_rev.ui.theme.AppDimensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 创建本地YAML配置对话框（对应桌面端从零创建配置）
 * 提供YAML模板，用户可以选择并编辑
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLocalConfigDialog(
    configDir: File,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    var configName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(ConfigTemplate.MINIMAL) }
    var configContent by remember { mutableStateOf(selectedTemplate.content) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCreating by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    
    // 当模板变化时更新内容
    LaunchedEffect(selectedTemplate) {
        configContent = selectedTemplate.content
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                TopAppBar(
                    title = { Text("创建本地配置") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, "关闭")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                
                // 错误提示
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                // 表单内容
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 配置名称
                    OutlinedTextField(
                        value = configName,
                        onValueChange = { configName = it },
                        label = { Text("配置名称") },
                        placeholder = { Text("例如: my-config") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Label, null) },
                        singleLine = true,
                        supportingText = { Text("文件将保存为: $configName.yaml") }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 模板选择
                    Text(
                        text = "选择配置模板",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ConfigTemplate.values().forEach { template ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTemplate == template) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            onClick = { selectedTemplate = template }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedTemplate == template,
                                    onClick = { selectedTemplate = template }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = template.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    Text(
                                        text = template.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 预览按钮
                    OutlinedButton(
                        onClick = { showPreview = !showPreview },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (showPreview) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (showPreview) "隐藏预览" else "显示预览")
                    }
                    
                    // 配置预览
                    if (showPreview) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = configContent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                // 底部操作栏
                HorizontalDivider()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isCreating
                    ) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // 验证表单
                                if (configName.isEmpty()) {
                                    errorMessage = "配置名称不能为空"
                                    return@launch
                                }
                                
                                if (!configName.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
                                    errorMessage = "配置名称只能包含字母、数字、下划线和横线"
                                    return@launch
                                }
                                
                                isCreating = true
                                errorMessage = null
                                
                                try {
                                    // 创建配置文件
                                    val configFile = File(configDir, "$configName.yaml")
                                    
                                    if (configFile.exists()) {
                                        errorMessage = "配置文件已存在"
                                        isCreating = false
                                        return@launch
                                    }
                                    
                                    withContext(Dispatchers.IO) {
                                        configDir.mkdirs()
                                        configFile.writeText(configContent)
                                    }
                                    
                                    onCreated()
                                } catch (e: Exception) {
                                    errorMessage = "创建失败: ${e.message}"
                                    isCreating = false
                                }
                            }
                        },
                        enabled = !isCreating
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("创建")
                    }
                }
            }
        }
    }
}

/**
 * 配置模板枚举
 */
enum class ConfigTemplate(
    val displayName: String,
    val description: String,
    val content: String
) {
    MINIMAL(
        "最小配置",
        "包含基本的代理设置和规则",
        """# Clash 最小配置
port: 7890
socks-port: 7891
allow-lan: false
mode: rule
log-level: info

proxies:
  - name: "示例节点"
    type: ss
    server: server.example.com
    port: 443
    cipher: aes-256-gcm
    password: "your-password"

proxy-groups:
  - name: "PROXY"
    type: select
    proxies:
      - "示例节点"
      - DIRECT

rules:
  - DOMAIN-SUFFIX,google.com,PROXY
  - DOMAIN-KEYWORD,google,PROXY
  - GEOIP,CN,DIRECT
  - MATCH,PROXY
"""
    ),
    
    STANDARD(
        "标准配置",
        "包含常用的代理组和规则集",
        """# Clash 标准配置
port: 7890
socks-port: 7891
mixed-port: 7892
allow-lan: false
bind-address: '*'
mode: rule
log-level: info
ipv6: false

dns:
  enable: true
  listen: 0.0.0.0:1053
  default-nameserver:
    - 114.114.114.114
    - 8.8.8.8
  nameserver:
    - https://doh.pub/dns-query
    - https://dns.alidns.com/dns-query

proxies:
  - name: "示例节点"
    type: ss
    server: server.example.com
    port: 443
    cipher: aes-256-gcm
    password: "your-password"

proxy-groups:
  - name: "PROXY"
    type: select
    proxies:
      - "自动选择"
      - "示例节点"
      - DIRECT
  
  - name: "自动选择"
    type: url-test
    proxies:
      - "示例节点"
    url: 'http://www.gstatic.com/generate_204'
    interval: 300

rules:
  - DOMAIN-SUFFIX,google.com,PROXY
  - DOMAIN-KEYWORD,google,PROXY
  - GEOIP,CN,DIRECT
  - MATCH,PROXY
"""
    ),
    
    ADVANCED(
        "高级配置",
        "包含完整的功能配置和规则",
        """# Clash 高级配置
port: 7890
socks-port: 7891
mixed-port: 7892
allow-lan: false
bind-address: '*'
mode: rule
log-level: info
ipv6: false
external-controller: 127.0.0.1:9090

dns:
  enable: true
  listen: 0.0.0.0:1053
  ipv6: false
  default-nameserver:
    - 114.114.114.114
    - 8.8.8.8
  enhanced-mode: fake-ip
  fake-ip-range: 198.18.0.1/16
  fake-ip-filter:
    - '*.lan'
    - localhost.ptlogin2.qq.com
  nameserver:
    - https://doh.pub/dns-query
    - https://dns.alidns.com/dns-query
  fallback:
    - https://cloudflare-dns.com/dns-query
    - https://dns.google/dns-query

proxies:
  - name: "示例SS节点"
    type: ss
    server: server.example.com
    port: 443
    cipher: aes-256-gcm
    password: "your-password"
    udp: true
  
  - name: "示例VMess节点"
    type: vmess
    server: server.example.com
    port: 443
    uuid: your-uuid
    alterId: 0
    cipher: auto
    tls: true

proxy-groups:
  - name: "PROXY"
    type: select
    proxies:
      - "自动选择"
      - "手动切换"
      - "示例SS节点"
      - "示例VMess节点"
      - DIRECT
  
  - name: "自动选择"
    type: url-test
    proxies:
      - "示例SS节点"
      - "示例VMess节点"
    url: 'http://www.gstatic.com/generate_204'
    interval: 300
    tolerance: 50
  
  - name: "手动切换"
    type: select
    proxies:
      - "示例SS节点"
      - "示例VMess节点"
  
  - name: "国外媒体"
    type: select
    proxies:
      - PROXY
      - "自动选择"
      - "示例SS节点"
      - "示例VMess节点"
  
  - name: "国内媒体"
    type: select
    proxies:
      - DIRECT
      - PROXY

rules:
  - DOMAIN-SUFFIX,youtube.com,国外媒体
  - DOMAIN-SUFFIX,netflix.com,国外媒体
  - DOMAIN-SUFFIX,qq.com,国内媒体
  - DOMAIN-SUFFIX,bilibili.com,国内媒体
  - DOMAIN-KEYWORD,google,PROXY
  - GEOIP,CN,DIRECT
  - MATCH,PROXY
"""
    )
}

