package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File

/**
 * 规则页面 - 对应桌面端的 rules.tsx
 * 
 * 功能：
 * 1. 显示当前加载的所有规则
 * 2. 支持按类型过滤
 * 3. 支持搜索规则
 * 4. 显示规则统计信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { io.github.clash_verge_rev.clash_verge_rev.data.SettingsManager.getInstance(context) }
    
    // UI状态
    var rules by remember { mutableStateOf<List<RuleItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("全部") }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 当前配置路径
    val currentConfigPath by settingsManager.currentConfigPath
    
    // 加载规则
    LaunchedEffect(currentConfigPath) {
        if (currentConfigPath.isEmpty()) {
            error = "请先在配置页面选择订阅"
            isLoading = false
            return@LaunchedEffect
        }
        
        isLoading = true
        error = null
        
        try {
            val configFile = File(currentConfigPath)
            if (configFile.exists()) {
                rules = loadRulesFromConfig(configFile)
            } else {
                error = "配置文件不存在"
            }
        } catch (e: Exception) {
            error = "加载规则失败: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    // 规则类型统计
    val ruleTypes = remember(rules) {
        rules.groupBy { it.type }.mapValues { it.value.size }
    }
    
    // 过滤规则
    val filteredRules = remember(rules, searchQuery, selectedType) {
        var filtered = rules
        
        // 按类型过滤
        if (selectedType != "全部") {
            filtered = filtered.filter { it.type == selectedType }
        }
        
        // 搜索过滤
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { rule ->
                rule.payload.contains(searchQuery, ignoreCase = true) ||
                rule.policy.contains(searchQuery, ignoreCase = true)
            }
        }
        
        filtered
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                // 统计卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                text = "规则统计",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${rules.size} 条规则",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Icon(
                            Icons.Rounded.Rule,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        )
                    }
                }
                
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("搜索规则...") },
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
                
                // 类型过滤器
                ScrollableTabRow(
                    selectedTabIndex = if (selectedType == "全部") 0 else ruleTypes.keys.indexOf(selectedType) + 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    edgePadding = 16.dp
                ) {
                    Tab(
                        selected = selectedType == "全部",
                        onClick = { selectedType = "全部" },
                        text = {
                            Text(
                                "全部 (${rules.size})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                    
                    ruleTypes.forEach { (type, count) ->
                        Tab(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            text = {
                                Text(
                                    "$type ($count)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
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
                isLoading -> {
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
                        Text("正在加载规则...")
                    }
                }
                
                error != null -> {
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
                            text = error ?: "未知错误",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                filteredRules.isEmpty() -> {
                    // 空状态
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "暂无规则" else "未找到匹配的规则",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                else -> {
                    // 规则列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredRules) { rule ->
                            RuleItemCard(rule = rule)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 规则项
 */
data class RuleItem(
    val type: String,        // DOMAIN, DOMAIN-SUFFIX, DOMAIN-KEYWORD, IP-CIDR等
    val payload: String,     // 规则内容
    val policy: String,      // 策略：DIRECT, REJECT, Proxy等
    val size: Int = 0        // 对于规则集，表示包含的规则数量
)

/**
 * 规则卡片组件
 */
@Composable
fun RuleItemCard(rule: RuleItem) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 规则类型标签
            Surface(
                shape = MaterialTheme.shapes.small,
                color = getRuleTypeColor(rule.type),
                modifier = Modifier.widthIn(min = 100.dp)
            ) {
                Text(
                    text = rule.type,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 规则内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.payload,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
                
                if (rule.size > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${rule.size} 条规则",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 策略标签
            Surface(
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, getPolicyColor(rule.policy)),
                color = Color.Transparent
            ) {
                Text(
                    text = rule.policy,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = getPolicyColor(rule.policy),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 从配置文件加载规则
 */
private fun loadRulesFromConfig(configFile: File): List<RuleItem> {
    try {
        val yaml = org.yaml.snakeyaml.Yaml()
        val config = configFile.inputStream().use { input ->
            yaml.load<Map<String, Any>>(input) as? Map<String, Any>
        } ?: return emptyList()
        
        val rulesList = mutableListOf<RuleItem>()
        
        // 解析普通规则
        val rules = config["rules"] as? List<String> ?: emptyList()
        rules.forEach { ruleStr ->
            try {
                val parts = ruleStr.split(",").map { it.trim() }
                if (parts.size >= 2) {
                    rulesList.add(
                        RuleItem(
                            type = parts[0],
                            payload = parts[1],
                            policy = parts.getOrNull(2) ?: "DIRECT"
                        )
                    )
                }
            } catch (e: Exception) {
                // 忽略解析失败的规则
            }
        }
        
        // 解析规则提供者（rule-providers）
        val ruleProviders = config["rule-providers"] as? Map<String, Any> ?: emptyMap()
        ruleProviders.forEach { (name, providerData) ->
            if (providerData is Map<*, *>) {
                val type = providerData["type"] as? String ?: "http"
                val behavior = providerData["behavior"] as? String ?: "domain"
                rulesList.add(
                    RuleItem(
                        type = "RULE-SET",
                        payload = name,
                        policy = behavior.uppercase(),
                        size = 0 // 实际数量需要从提供者文件读取
                    )
                )
            }
        }
        
        return rulesList
    } catch (e: Exception) {
        return emptyList()
    }
}

/**
 * 获取规则类型对应的颜色
 */
private fun getRuleTypeColor(type: String): Color {
    return when (type.uppercase()) {
        "DOMAIN" -> Color(0xFF4CAF50)
        "DOMAIN-SUFFIX" -> Color(0xFF2196F3)
        "DOMAIN-KEYWORD" -> Color(0xFF00BCD4)
        "IP-CIDR", "IP-CIDR6" -> Color(0xFFFF9800)
        "GEOIP" -> Color(0xFFFF5722)
        "RULE-SET" -> Color(0xFF9C27B0)
        "MATCH" -> Color(0xFF607D8B)
        else -> Color(0xFF757575)
    }
}

/**
 * 获取策略对应的颜色
 */
private fun getPolicyColor(policy: String): Color {
    return when (policy.uppercase()) {
        "DIRECT" -> Color(0xFF4CAF50)
        "REJECT" -> Color(0xFFE91E63)
        "PROXY" -> Color(0xFF2196F3)
        else -> Color(0xFF9C27B0)
    }
}

