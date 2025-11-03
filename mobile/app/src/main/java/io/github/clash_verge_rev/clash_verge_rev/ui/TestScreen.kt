package io.github.clash_verge_rev.clash_verge_rev.ui

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 测试页面 - 流媒体解锁测试
 * 对应桌面端的 test.tsx
 * 
 * 功能：
 * 1. 测试 Netflix、Disney+、YouTube Premium 等流媒体解锁状态
 * 2. 测试 ChatGPT、Claude、Gemini 等 AI 服务可用性
 * 3. 测试 TikTok、Spotify 等其他服务
 * 4. 支持一键测试所有服务
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 测试服务列表
    var services by remember { mutableStateOf(getInitialServices()) }
    var isTestingAll by remember { mutableStateOf(false) }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                // 标题卡片
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
                                text = "服务测试",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "测试流媒体解锁和服务可用性",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        
                        Icon(
                            Icons.Rounded.PlayCircleFilled,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        )
                    }
                }
                
                // 测试全部按钮
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            isTestingAll = true
                            services.forEachIndexed { index, service ->
                                if (!service.isTesting) {
                                    services = services.toMutableList().apply {
                                        this[index] = service.copy(isTesting = true)
                                    }
                                    
                                    val result = testService(service)
                                    
                                    services = services.toMutableList().apply {
                                        this[index] = result
                                    }
                                }
                            }
                            isTestingAll = false
                            snackbarHostState.showSnackbar("所有测试已完成")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = !isTestingAll
                ) {
                        if (isTestingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isTestingAll) "测试中..." else "测试全部")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 流媒体服务
            item {
                Text(
                    "📺 流媒体服务",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(services.filter { it.category == ServiceCategory.STREAMING }) { service ->
                ServiceTestCard(
                    service = service,
                    onTest = {
                        scope.launch {
                            val index = services.indexOf(service)
                            services = services.toMutableList().apply {
                                this[index] = service.copy(isTesting = true)
                            }
                            
                            val result = testService(service)
                            
                            services = services.toMutableList().apply {
                                this[index] = result
                            }
                        }
                    }
                )
            }
            
            // AI 服务
            item {
                Text(
                    "🤖 AI 服务",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(services.filter { it.category == ServiceCategory.AI }) { service ->
                ServiceTestCard(
                    service = service,
                    onTest = {
                        scope.launch {
                            val index = services.indexOf(service)
                            services = services.toMutableList().apply {
                                this[index] = service.copy(isTesting = true)
                            }
                            
                            val result = testService(service)
                            
                            services = services.toMutableList().apply {
                                this[index] = result
                            }
                        }
                    }
                )
            }
            
            // 其他服务
            item {
                Text(
                    "🎵 其他服务",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(services.filter { it.category == ServiceCategory.OTHER }) { service ->
                ServiceTestCard(
                    service = service,
                    onTest = {
                        scope.launch {
                            val index = services.indexOf(service)
                            services = services.toMutableList().apply {
                                this[index] = service.copy(isTesting = true)
                            }
                            
                            val result = testService(service)
                            
                            services = services.toMutableList().apply {
                                this[index] = result
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * 服务分类
 */
enum class ServiceCategory {
    STREAMING,  // 流媒体
    AI,         // AI服务
    OTHER       // 其他
}

/**
 * 测试状态
 */
enum class TestResult {
    IDLE,       // 未测试
    SUCCESS,    // 可用
    FAILED,     // 不可用
    PARTIAL     // 部分可用
}

/**
 * 测试服务数据
 */
data class TestService(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val category: ServiceCategory,
    val testUrl: String,
    val isTesting: Boolean = false,
    val result: TestResult = TestResult.IDLE,
    val message: String = "",
    val region: String = ""  // 解锁的地区
)

/**
 * 获取初始服务列表
 */
private fun getInitialServices(): List<TestService> {
    return listOf(
        // 流媒体服务
        TestService(
            id = "netflix",
            name = "Netflix",
            icon = Icons.Rounded.Movie,
            category = ServiceCategory.STREAMING,
            testUrl = "https://www.netflix.com"
        ),
        TestService(
            id = "disneyplus",
            name = "Disney+",
            icon = Icons.Rounded.TheaterComedy,
            category = ServiceCategory.STREAMING,
            testUrl = "https://www.disneyplus.com"
        ),
        TestService(
            id = "youtube",
            name = "YouTube Premium",
            icon = Icons.Rounded.VideoLibrary,
            category = ServiceCategory.STREAMING,
            testUrl = "https://www.youtube.com/premium"
        ),
        TestService(
            id = "primevideo",
            name = "Prime Video",
            icon = Icons.Rounded.PlayCircle,
            category = ServiceCategory.STREAMING,
            testUrl = "https://www.primevideo.com"
        ),
        TestService(
            id = "bilibili",
            name = "哔哩哔哩",
            icon = Icons.Rounded.Videocam,
            category = ServiceCategory.STREAMING,
            testUrl = "https://www.bilibili.com"
        ),
        
        // AI 服务
        TestService(
            id = "chatgpt",
            name = "ChatGPT",
            icon = Icons.Rounded.Psychology,
            category = ServiceCategory.AI,
            testUrl = "https://chat.openai.com"
        ),
        TestService(
            id = "claude",
            name = "Claude",
            icon = Icons.Rounded.AutoAwesome,
            category = ServiceCategory.AI,
            testUrl = "https://claude.ai"
        ),
        TestService(
            id = "gemini",
            name = "Gemini",
            icon = Icons.Rounded.Stars,
            category = ServiceCategory.AI,
            testUrl = "https://gemini.google.com"
        ),
        
        // 其他服务
        TestService(
            id = "tiktok",
            name = "TikTok",
            icon = Icons.Rounded.MusicNote,
            category = ServiceCategory.OTHER,
            testUrl = "https://www.tiktok.com"
        ),
        TestService(
            id = "spotify",
            name = "Spotify",
            icon = Icons.Rounded.AudioFile,
            category = ServiceCategory.OTHER,
            testUrl = "https://www.spotify.com"
        )
    )
}

/**
 * 服务测试卡片
 */
@Composable
fun ServiceTestCard(
    service: TestService,
    onTest: () -> Unit
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
            // 服务图标
            Icon(
                service.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 服务信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 测试结果
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (service.result) {
                        TestResult.IDLE -> {
                            Text(
                                text = "未测试",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TestResult.SUCCESS -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (service.region.isNotEmpty()) "可用 (${service.region})" else "可用",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        TestResult.FAILED -> {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFF44336)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "不可用",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF44336)
                            )
                        }
                        TestResult.PARTIAL -> {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "部分可用",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
                
                // 消息
                if (service.message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = service.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 测试按钮
            IconButton(
                onClick = onTest,
                enabled = !service.isTesting
            ) {
                if (service.isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = "测试",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 测试服务（实际测试逻辑）
 */
private suspend fun testService(service: TestService): TestService = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        
        val request = Request.Builder()
            .url(service.testUrl)
            .get()
            .build()
        
        client.newCall(request).execute().use { response ->
            val result = if (response.isSuccessful) {
                // 根据响应头判断地区
                val region = detectRegion(response.headers.toString())
                TestResult.SUCCESS
            } else {
                TestResult.FAILED
            }
            
            service.copy(
                isTesting = false,
                result = result,
                message = if (result == TestResult.SUCCESS) "连接成功" else "连接失败",
                region = if (result == TestResult.SUCCESS) detectRegion(response.headers.toString()) else ""
            )
        }
    } catch (e: Exception) {
        service.copy(
            isTesting = false,
            result = TestResult.FAILED,
            message = e.message ?: "测试失败"
        )
    }
}

/**
 * 检测地区
 */
private fun detectRegion(headers: String): String {
    // 简化实现：从响应头尝试检测地区
    return when {
        headers.contains("country", ignoreCase = true) -> {
            val regex = """country[:\s]+([A-Z]{2})""".toRegex(RegexOption.IGNORE_CASE)
            regex.find(headers)?.groupValues?.getOrNull(1) ?: ""
        }
        else -> ""
    }
}

