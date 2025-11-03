# Clash Verge Rev Android - Mihomo 核心集成指南

📅 **创建时间**: 2025-11-02  
✨ **版本**: v1.0.0

---

## 📋 目录

1. [概述](#概述)
2. [架构设计](#架构设计)
3. [集成步骤](#集成步骤)
4. [API 使用指南](#api-使用指南)
5. [完整示例](#完整示例)
6. [注意事项](#注意事项)
7. [故障排除](#故障排除)

---

## 🎯 概述

### 设计理念

**桌面端 vs 移动端**:

| 方面 | 桌面端 (Tauri) | 移动端 (Android) |
|------|---------------|------------------|
| **架构** | Rust Backend + 独立 Mihomo 进程 | Kotlin + Go (CGO 嵌入 Mihomo) |
| **通信** | IPC/Unix Socket | JNI 直接调用 |
| **进程** | 多进程 | 单进程 + 多线程 |
| **资源** | 较高 | 较低（移动优化） |

### 关键特性

✅ **直接嵌入**: Mihomo 核心直接编译到 Go 共享库中  
✅ **零延迟**: JNI 调用，无 IPC 开销  
✅ **完整 API**: 支持桌面端的所有核心功能  
✅ **向后兼容**: 保留原有 API，渐进式升级  

---

## 🏗️ 架构设计

### 三层架构

```
┌─────────────────────────────────────────────────────────┐
│              Kotlin 应用层                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ ProxyRepo    │  │ ConnectionMgr│  │  UI 组件     │  │
│  │ New          │  │              │  │  (Compose)   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                        ↓                                 │
│  ┌────────────────────────────────────────────────────┐ │
│  │         ClashCoreExtended API                      │ │
│  │  - getProxies()     - selectProxy()                │ │
│  │  - testDelay()      - getConnections()             │ │
│  │  - reloadConfig()   - closeConnections()           │ │
│  └────────────────────────────────────────────────────┘ │
│                        ↓                                 │
│  ┌────────────────────────────────────────────────────┐ │
│  │         ClashCore (JNI 接口)                       │ │
│  │  - nativeGetProxies()                              │ │
│  │  - nativeSelectProxy()                             │ │
│  │  - ...                                             │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                        ↕ JNI Calls
┌─────────────────────────────────────────────────────────┐
│              JNI 桥接层 (C/C++)                          │
│  native-lib.cpp                                          │
│  - Java/Kotlin ↔ C 类型转换                            │
│  - 参数验证                                              │
│  - 错误处理                                              │
└─────────────────────────────────────────────────────────┘
                        ↕ CGO Calls
┌─────────────────────────────────────────────────────────┐
│              Go 核心层                                    │
│  ┌────────────────────────────────────────────────────┐ │
│  │         mihomo_core.go (MihomoCore)                │ │
│  │  - reloadConfig()    - getProxies()                │ │
│  │  - selectProxy()     - testProxyDelay()            │ │
│  │  - getConnections()  - closeConnection()           │ │
│  │  - startTunWithFd()  - stopTun()                   │ │
│  └────────────────────────────────────────────────────┘ │
│                        ↓                                 │
│  ┌────────────────────────────────────────────────────┐ │
│  │         Mihomo 库 (github.com/metacubex/mihomo)    │ │
│  │  - tunnel.Instance()                               │ │
│  │  - adapter.Proxies()                               │ │
│  │  - dns.Resolver                                    │ │
│  │  - config.Parse()                                  │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## 🔧 集成步骤

### 步骤 1: 添加 Mihomo 依赖

在 `mobile/app/src/main/golang/go.mod` 中添加：

```go
module clash-verge-mobile

go 1.21

require (
    github.com/metacubex/mihomo v1.18.1
    gopkg.in/yaml.v3 v3.0.1
)
```

### 步骤 2: 取消注释代码

在 `mihomo_core.go` 中取消注释 Mihomo 集成代码（标记为 `// TODO: 实际集成时取消注释`）

### 步骤 3: 编译 Go 共享库

```bash
cd mobile/app/src/main/golang

# ARM64 (推荐)
CGO_ENABLED=1 \
GOOS=android \
GOARCH=arm64 \
CC=$NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android21-clang \
go build -buildmode=c-shared -o ../jniLibs/arm64-v8a/libclash.so

# ARMv7
CGO_ENABLED=1 \
GOOS=android \
GOARCH=arm \
GOARM=7 \
CC=$NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi21-clang \
go build -buildmode=c-shared -o ../jniLibs/armeabi-v7a/libclash.so

# x86_64 (模拟器)
CGO_ENABLED=1 \
GOOS=android \
GOARCH=amd64 \
CC=$NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android21-clang \
go build -buildmode=c-shared -o ../jniLibs/x86_64/libclash.so
```

### 步骤 4: 构建 Android 应用

```bash
cd mobile
./gradlew assembleDebug
```

---

## 📚 API 使用指南

### 1. 配置管理

#### 重载配置

```kotlin
// 方式 1: 使用 ClashCore (兼容旧版)
val result = ClashCore.loadConfig("/path/to/config.yaml")

// 方式 2: 使用 ClashCoreExtended (推荐)
val result = ClashCoreExtended.reloadConfig("/path/to/config.yaml", force = false)
if (result == 0) {
    Log.i(TAG, "Config reloaded successfully")
} else {
    Log.e(TAG, "Failed to reload config: $result")
}
```

#### 部分更新配置

```kotlin
// 修改模式
ClashCoreExtended.changeMode("global") // rule/global/direct

// 自定义更新
val patch = mapOf(
    "mode" to "rule",
    "log-level" to "info",
    "ipv6" to true
)
ClashCoreExtended.updateConfig(patch)
```

### 2. 代理管理

#### 获取代理列表

```kotlin
// 在 ViewModel 中
class ProxyViewModel : ViewModel() {
    private val proxyRepo = ProxyRepositoryNew.getInstance()
    
    val proxies = proxyRepo.proxies.asStateFlow()
    val loading = proxyRepo.loading.asStateFlow()
    val error = proxyRepo.error.asStateFlow()
    
    fun refreshProxies() {
        viewModelScope.launch {
            proxyRepo.refreshProxies()
        }
    }
}

// 在 Composable 中
@Composable
fun ProxyScreen(viewModel: ProxyViewModel = viewModel()) {
    val proxies by viewModel.proxies.collectAsState()
    val loading by viewModel.loading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.refreshProxies()
    }
    
    LazyColumn {
        items(proxies) { proxy ->
            ProxyItem(proxy)
        }
    }
}
```

#### 选择代理节点

```kotlin
viewModelScope.launch {
    val success = proxyRepo.selectProxy(
        groupName = "PROXY",
        proxyName = "香港-01"
    )
    
    if (success) {
        Toast.makeText(context, "切换成功", Toast.LENGTH_SHORT).show()
    }
}
```

#### 测试代理延迟

```kotlin
// 单个代理
viewModelScope.launch {
    val delay = proxyRepo.testProxyDelay(
        proxyName = "香港-01",
        testURL = "https://www.google.com/generate_204",
        timeout = 5000
    )
    
    if (delay >= 0) {
        Log.i(TAG, "Delay: ${delay}ms")
    } else {
        Log.w(TAG, "Test failed")
    }
}

// 批量测试
viewModelScope.launch {
    proxyRepo.testAllProxies(
        testURL = "https://www.google.com/generate_204",
        timeout = 5000
    ) { proxyName, delay ->
        Log.i(TAG, "$proxyName: ${delay}ms")
    }
}
```

### 3. 连接管理

#### 获取连接列表

```kotlin
class ConnectionViewModel : ViewModel() {
    private val connMgr = ConnectionManager.getInstance()
    
    val connections = connMgr.connections.asStateFlow()
    val uploadTotal = connMgr.uploadTotal.asStateFlow()
    val downloadTotal = connMgr.downloadTotal.asStateFlow()
    val activeCount = connMgr.activeConnectionCount.asStateFlow()
    
    fun refreshConnections() {
        viewModelScope.launch {
            connMgr.refreshConnections()
        }
    }
    
    fun closeConnection(id: String) {
        viewModelScope.launch {
            connMgr.closeConnection(id)
        }
    }
    
    fun closeAllConnections() {
        viewModelScope.launch {
            connMgr.closeAllConnections()
        }
    }
}
```

#### UI 显示

```kotlin
@Composable
fun ConnectionsScreen(viewModel: ConnectionViewModel = viewModel()) {
    val connections by viewModel.connections.collectAsState()
    val uploadTotal by viewModel.uploadTotal.collectAsState()
    val downloadTotal by viewModel.downloadTotal.collectAsState()
    
    // 定时刷新
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshConnections()
            delay(1000)
        }
    }
    
    Column {
        // 流量统计
        TrafficStats(uploadTotal, downloadTotal)
        
        // 连接列表
        LazyColumn {
            items(connections) { conn ->
                ConnectionItem(
                    connection = conn,
                    onClose = { viewModel.closeConnection(conn.id) }
                )
            }
        }
    }
}
```

### 4. 规则管理

```kotlin
// 获取规则列表
val rulesJSON = ClashCoreExtended.getRules()
val rulesObj = ClashCoreExtended.getRulesAsJson()

// 解析规则
val rules = rulesObj.getJSONArray("rules")
for (i in 0 until rules.length()) {
    val rule = rules.getJSONObject(i)
    val type = rule.getString("type")
    val payload = rule.getString("payload")
    val proxy = rule.getString("proxy")
    
    Log.i(TAG, "Rule: $type, $payload -> $proxy")
}
```

### 5. 日志管理

```kotlin
// 获取日志
val logsJSON = ClashCoreExtended.getLogs(count = 100)
val logsObj = ClashCoreExtended.getLogsAsJson(count = 100)

// 显示日志
@Composable
fun LogScreen() {
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            val logsObj = ClashCoreExtended.getLogsAsJson()
            val logsArray = logsObj.getJSONArray("logs")
            val logList = mutableListOf<String>()
            
            for (i in 0 until logsArray.length()) {
                val log = logsArray.getJSONObject(i)
                logList.add(log.getString("payload"))
            }
            
            logs = logList
            delay(1000)
        }
    }
    
    LazyColumn {
        items(logs) { log ->
            Text(log, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

---

## 🔍 完整示例

### ProxyScreen 完整实现

```kotlin
@Composable
fun ProxyScreenComplete() {
    val viewModel: ProxyViewModel = viewModel()
    val proxies by viewModel.proxies.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedProxy by viewModel.selectedProxy.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 初始加载
    LaunchedEffect(Unit) {
        viewModel.refreshProxies()
    }
    
    // 显示错误
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("代理节点") },
                actions = {
                    IconButton(onClick = { viewModel.refreshProxies() }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                    IconButton(onClick = { viewModel.testAllProxies() }) {
                        Icon(Icons.Default.Speed, "测速")
                    }
                }
            )
        }
    ) { padding ->
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(proxies) { proxy ->
                    ProxyItem(
                        proxy = proxy,
                        selected = proxy.name == selectedProxy,
                        onSelect = {
                            scope.launch {
                                viewModel.selectProxy("PROXY", proxy.name)
                            }
                        },
                        onTest = {
                            scope.launch {
                                viewModel.testProxyDelay(proxy.name)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProxyItem(
    proxy: ProxyNodeInfo,
    selected: Boolean,
    onSelect: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = proxy.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = proxy.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 延迟显示
                if (proxy.alive) {
                    Text(
                        text = "${proxy.delay}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            proxy.delay < 100 -> Color.Green
                            proxy.delay < 300 -> Color.Yellow
                            else -> Color.Red
                        }
                    )
                } else {
                    Text(
                        text = "超时",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 测速按钮
                IconButton(onClick = onTest) {
                    Icon(Icons.Default.Speed, "测速")
                }
            }
        }
    }
}
```

---

## ⚠️ 注意事项

### 1. 线程安全

- **Go 层**: 所有导出函数使用 `defer recover()` 防止 panic
- **JNI 层**: 使用 `synchronized` 保证线程安全
- **Kotlin 层**: 使用 `StateFlow` 管理状态，协程处理异步操作

### 2. 内存管理

```kotlin
// ✅ 正确: JNI 字符串自动管理
val result = ClashCore.nativeGetProxies()

// ❌ 错误: 不要手动管理 JNI 返回的字符串
// native 层已经处理了内存释放
```

### 3. 错误处理

```kotlin
// 始终检查返回值
val result = ClashCoreExtended.reloadConfig(path)
when (result) {
    0 -> Log.i(TAG, "Success")
    -1 -> Log.e(TAG, "File not found")
    -2 -> Log.e(TAG, "Parse error")
    else -> Log.e(TAG, "Unknown error: $result")
}

// 检查 JSON 响应中的错误
val proxiesJSON = ClashCoreExtended.getProxies()
if (ClashCoreExtended.hasError(proxiesJSON)) {
    val error = ClashCoreExtended.getError(proxiesJSON)
    Log.e(TAG, "Error: $error")
}
```

### 4. 性能优化

```kotlin
// ✅ 使用批量测试
proxyRepo.testAllProxies()

// ❌ 避免逐个测试
proxies.forEach { proxy ->
    proxyRepo.testProxyDelay(proxy.name) // 效率低
}

// ✅ 使用定时刷新
LaunchedEffect(Unit) {
    while (true) {
        delay(1000) // 1秒刷新一次
        refreshConnections()
    }
}

// ❌ 避免过于频繁的刷新
while (true) {
    refreshConnections()
    delay(100) // 过于频繁
}
```

---

## 🔧 故障排除

### 问题 1: JNI 方法未找到

**错误信息**:
```
java.lang.UnsatisfiedLinkError: No implementation found for ...
```

**解决方案**:
1. 检查 `libclash.so` 是否正确编译和放置
2. 检查 JNI 函数命名是否正确
3. 清理并重新构建: `./gradlew clean build`

### 问题 2: Go panic

**错误信息**:
```
panic: runtime error: invalid memory address
```

**解决方案**:
1. 确保 `nativeInit()` 已调用
2. 检查传递给 Go 的参数是否有效
3. 查看 logcat 日志定位具体错误位置

### 问题 3: 配置加载失败

**错误信息**:
```
Failed to reload config: -2
```

**解决方案**:
1. 检查配置文件格式是否正确
2. 确保文件路径有读取权限
3. 验证 YAML 语法: `yamllint config.yaml`

### 问题 4: 代理延迟测试超时

**解决方案**:
1. 增加超时时间: `testProxyDelay(proxy, url, timeout = 10000)`
2. 检查网络连接
3. 尝试不同的测试 URL

---

## 📖 参考资料

### 桌面端对照

| 功能 | 桌面端 (Rust) | 移动端 (Kotlin) |
|------|--------------|----------------|
| 重载配置 | `handle::Handle::mihomo().reload_config()` | `ClashCoreExtended.reloadConfig()` |
| 获取代理 | `handle::Handle::mihomo().get_proxies()` | `ClashCoreExtended.getProxies()` |
| 选择代理 | `handle::Handle::mihomo().select_node_for_group()` | `ClashCoreExtended.selectProxy()` |
| 获取连接 | `handle::Handle::mihomo().get_connections()` | `ClashCoreExtended.getConnections()` |
| 关闭连接 | `handle::Handle::mihomo().close_connection()` | `ClashCoreExtended.closeConnection()` |

### 相关文档

- [Mihomo 文档](https://github.com/MetaCubeX/mihomo)
- [Android JNI 指南](https://developer.android.com/training/articles/perf-jni)
- [Go Mobile](https://pkg.go.dev/golang.org/x/mobile)
- [Clash 配置说明](https://clash.wiki/)

---

## 🎉 总结

### 已实现的功能

✅ Go 层 Mihomo 核心完整封装  
✅ JNI 桥接层完整实现  
✅ Kotlin API 层设计完成  
✅ ProxyRepository 和 ConnectionManager  
✅ 向后兼容旧 API  

### 待实现的功能 (需要 Mihomo 依赖)

⚠️ 实际的代理转发逻辑  
⚠️ DNS 解析器集成  
⚠️ 规则引擎完整实现  
⚠️ Provider 订阅管理  

### 下一步

1. 添加 Mihomo 依赖到 `go.mod`
2. 取消注释 `mihomo_core.go` 中的集成代码
3. 编译测试所有架构的共享库
4. 在实际设备上测试完整功能
5. 优化性能和内存使用

---

**祝你集成顺利！** 🚀

如有问题，请参考故障排除章节或查看相关文档。

