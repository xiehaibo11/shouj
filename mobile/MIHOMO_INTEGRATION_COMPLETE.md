# Mihomo 核心完整集成指南

## 📋 概述

本文档说明了 Clash Verge Rev Android 完整 Mihomo 核心的集成实现，提供**真正的代理功能**，包括节点选择、规则匹配、延迟测试等。

## ✨ 已实现的功能

### 核心功能

✅ **完整的 Mihomo 核心集成**
- 直接嵌入 Mihomo 1.18.10 核心
- 支持所有 Mihomo 特性（代理协议、规则引擎、DNS 等）
- 通过 JNI 直接调用，无 IPC 开销

✅ **代理节点管理**
- `getProxies()` - 获取所有代理节点和代理组
- `selectProxy()` - 切换代理节点
- `testProxyDelay()` - 测试节点延迟
- 支持所有代理组类型：select、urltest、fallback、loadbalance

✅ **规则匹配引擎**
- `getRules()` - 获取所有规则
- 支持 DOMAIN、DOMAIN-SUFFIX、DOMAIN-KEYWORD、IP-CIDR 等
- 完整的规则匹配和代理分流

✅ **连接管理**
- `getConnections()` - 获取活动连接列表
- `closeConnection()` - 关闭指定连接
- `closeAllConnections()` - 关闭所有连接
- 实时流量统计

✅ **配置管理**
- `reloadConfig()` - 重载配置文件
- `updateConfig()` - 部分更新配置
- 支持完整的 Mihomo 配置格式

✅ **TUN 模式**
- 使用 Android VPN Service + Mihomo TUN
- 支持 gvisor/system/mixed stack
- DNS 劫持、自动路由

✅ **日志系统**
- `getLogs()` - 获取实时日志
- 同时输出到 logcat 和文件
- 5 级日志等级

## 🔧 编译步骤

### 前置要求

1. **Android SDK & NDK**
   - Android SDK: API 24+ (Android 7.0+)
   - Android NDK: r21 或更高版本
   - 设置环境变量：`ANDROID_NDK_HOME` 或 `NDK_HOME`

2. **Go 环境**
   - Go 1.21 或更高版本
   - 确保已添加到 PATH

3. **构建工具**
   - Linux/macOS: bash
   - Windows: PowerShell 或 CMD

### 步骤 1: 编译 Mihomo 核心

#### Linux / macOS

```bash
cd mobile
chmod +x scripts/build-mihomo.sh
./scripts/build-mihomo.sh
```

#### Windows

```cmd
cd mobile
scripts\build-mihomo.bat
```

构建脚本会提示选择要编译的架构：
- **1** - ARM64 (arm64-v8a) - 推荐，适用于 2015 年后的大多数设备
- **2** - ARMv7 (armeabi-v7a) - 适用于老设备
- **3** - x86_64 - 适用于模拟器
- **4** - 所有架构

编译完成后，生成的 `libclash.so` 文件将位于：
```
mobile/app/src/main/jniLibs/
├── arm64-v8a/libclash.so
├── armeabi-v7a/libclash.so
└── x86_64/libclash.so
```

### 步骤 2: 构建 Android 应用

```bash
cd mobile
./gradlew assembleDebug
```

生成的 APK 位于：
```
mobile/app/build/outputs/apk/debug/app-debug.apk
```

### 步骤 3: 安装应用

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📱 使用指南

### 1. 准备 Mihomo 配置文件

创建一个标准的 Mihomo 配置文件 `config.yaml`，包含：

```yaml
# 混合端口（HTTP + SOCKS5）
mixed-port: 7897

# 外部控制器（API 端口）
external-controller: 127.0.0.1:9090

# 运行模式：rule（规则）/ global（全局）/ direct（直连）
mode: rule

# 日志级别
log-level: info

# IPv6 支持
ipv6: true

# 代理节点
proxies:
  - name: "香港-01"
    type: ss
    server: example.com
    port: 8388
    cipher: aes-256-gcm
    password: your-password
  
  - name: "日本-01"
    type: vmess
    server: example.jp
    port: 443
    uuid: your-uuid
    alterId: 0
    cipher: auto
    tls: true

# 代理组
proxy-groups:
  - name: "PROXY"
    type: select
    proxies:
      - "香港-01"
      - "日本-01"
      - "DIRECT"
  
  - name: "Auto"
    type: url-test
    proxies:
      - "香港-01"
      - "日本-01"
    url: "http://www.gstatic.com/generate_204"
    interval: 300

# 规则
rules:
  - DOMAIN-SUFFIX,google.com,PROXY
  - DOMAIN-SUFFIX,youtube.com,PROXY
  - DOMAIN-SUFFIX,github.com,PROXY
  - DOMAIN-KEYWORD,google,PROXY
  - GEOIP,CN,DIRECT
  - MATCH,PROXY
```

### 2. 导入配置到应用

1. 将配置文件复制到手机：
   ```bash
   adb push config.yaml /sdcard/Download/
   ```

2. 在应用中导入配置：
   - 打开应用
   - 点击"配置"页面
   - 点击"导入配置"
   - 选择 `/sdcard/Download/config.yaml`

### 3. 启动代理

1. 点击主界面的"启动"按钮
2. 授予 VPN 权限（首次使用）
3. 等待连接成功（状态显示"已连接"）

### 4. 选择代理节点

1. 进入"代理"页面
2. 查看所有代理组和节点
3. 点击代理组（如"PROXY"）
4. 选择要使用的节点（如"香港-01"）
5. 应用会自动切换节点

### 5. 测试延迟

在"代理"页面：
- 点击单个节点旁的"测速"图标
- 或点击右上角的"全部测速"

### 6. 查看连接

1. 进入"连接"页面
2. 查看实时活动连接
3. 查看流量统计
4. 可以关闭单个连接或全部连接

### 7. 查看日志

1. 进入"日志"页面
2. 查看实时日志输出
3. 可以过滤日志级别

## 🎯 API 使用（Kotlin）

### 初始化核心

```kotlin
import io.github.clash_verge_rev.clash_verge_rev.core.ClashCore

// 应用启动时初始化
ClashCore.init(
    homeDir = context.filesDir.absolutePath,
    versionName = BuildConfig.VERSION_NAME
)
```

### 加载配置

```kotlin
val configPath = File(context.filesDir, "config.yaml").absolutePath
val result = ClashCore.loadConfig(configPath)

if (result == 0) {
    Log.i(TAG, "✅ Config loaded successfully")
} else {
    Log.e(TAG, "❌ Failed to load config: $result")
}
```

### 获取代理列表

```kotlin
import io.github.clash_verge_rev.clash_verge_rev.data.ProxyRepository

val proxyRepo = ProxyRepository.getInstance(context)
val configFile = File(context.filesDir, "config.yaml")

viewModelScope.launch {
    val proxiesState = proxyRepo.loadProxiesFromConfig(configFile)
    
    proxiesState.groups.forEach { group ->
        Log.i(TAG, "Group: ${group.name}, Type: ${group.type}")
        group.proxies.forEach { proxy ->
            Log.i(TAG, "  - ${proxy.name} (${proxy.type})")
        }
    }
}
```

### 切换代理节点

```kotlin
viewModelScope.launch {
    val success = proxyRepo.switchProxy(
        groupName = "PROXY",
        proxyName = "香港-01"
    )
    
    if (success) {
        Toast.makeText(context, "✅ 切换成功", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "❌ 切换失败", Toast.LENGTH_SHORT).show()
    }
}
```

### 测试延迟

```kotlin
viewModelScope.launch {
    val testStatus = proxyRepo.testProxyDelay(
        proxyName = "香港-01",
        testUrl = "https://www.gstatic.com/generate_204",
        timeout = 5000
    )
    
    when (testStatus) {
        is TestStatus.Success -> {
            Log.i(TAG, "延迟: ${testStatus.delay}ms")
        }
        is TestStatus.Failed -> {
            Log.e(TAG, "测试失败: ${testStatus.reason}")
        }
    }
}
```

### 启动 TUN 模式

```kotlin
import io.github.clash_verge_rev.clash_verge_rev.service.ClashVpnService

// 在 VpnService 中
class ClashVpnService : VpnService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. 加载配置
        val configPath = File(filesDir, "config.yaml").absolutePath
        ClashCore.loadConfig(configPath)
        
        // 2. 创建 VPN 接口
        val builder = Builder()
            .setSession("Clash Verge Rev")
            .setMtu(9000)
            .addAddress("172.19.0.1", 30)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
        
        val vpnInterface = builder.establish() ?: return START_NOT_STICKY
        val fd = vpnInterface.fd
        
        // 3. 启动 TUN
        val result = ClashCore.startTun(fd, 9000)
        
        if (result == 0) {
            Log.i(TAG, "✅ TUN started")
        } else {
            Log.e(TAG, "❌ TUN start failed: $result")
        }
        
        return START_STICKY
    }
}
```

## 🔍 故障排除

### 问题 1: 编译失败 - NDK 未找到

**错误信息**:
```
Error: ANDROID_NDK_HOME or NDK_HOME not set
```

**解决方案**:
```bash
# Linux/macOS
export ANDROID_NDK_HOME=/path/to/android-ndk
export NDK_HOME=$ANDROID_NDK_HOME

# Windows
set ANDROID_NDK_HOME=C:\path\to\android-ndk
set NDK_HOME=%ANDROID_NDK_HOME%
```

### 问题 2: Go 依赖下载失败

**错误信息**:
```
go: downloading github.com/metacubex/mihomo@v1.18.10
timeout
```

**解决方案**:
```bash
# 使用 Go 代理
export GOPROXY=https://goproxy.cn,direct

# 或使用其他镜像
export GOPROXY=https://goproxy.io,direct
```

### 问题 3: 代理切换失败

**错误信息**:
```
Failed to connect to API (port 9090)
```

**解决方案**:
1. 确认配置文件中有 `external-controller: 127.0.0.1:9090`
2. 确认配置已成功加载
3. 检查 logcat 日志：`adb logcat | grep ClashCore`

### 问题 4: 代理不生效

**症状**: 启动 TUN 后仍然无法访问被墙网站

**解决方案**:
1. 检查配置文件中的代理服务器是否正确
2. 确认代理组选择了正确的节点
3. 检查规则是否正确匹配目标域名
4. 查看连接页面，确认流量走了代理

### 问题 5: 应用闪退

**解决方案**:
1. 查看崩溃日志：`adb logcat | grep AndroidRuntime`
2. 确认 `libclash.so` 架构与设备匹配
3. 清除应用数据后重试
4. 重新编译 Go 核心

## 📊 性能优化

### 1. 减小 APK 体积

默认情况下，编译所有架构会使 APK 体积较大。可以针对特定设备编译：

```bash
# 仅编译 ARM64（最常见）
./scripts/build-mihomo.sh
# 选择 1
```

### 2. 启用混淆和压缩

在 `build.gradle.kts` 中：

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### 3. 优化内存使用

在配置文件中：

```yaml
# 限制连接数
max-download-size: 1GB

# 启用 TCP 快速打开
tfo: true

# 启用 TCP 多路径
mptcp: true
```

## 🎓 技术细节

### 架构设计

```
┌─────────────────────────────────────┐
│      Kotlin 应用层 (Compose UI)      │
│   ProxyRepository, ConnectionMgr    │
└─────────────────┬───────────────────┘
                  │ JNI Calls
┌─────────────────▼───────────────────┐
│       Go 核心层 (mihomo_core.go)     │
│  - getProxies()                     │
│  - selectProxy()                    │
│  - reloadConfig()                   │
│  - testProxyDelay()                 │
│  - getConnections()                 │
└─────────────────┬───────────────────┘
                  │ Direct Calls
┌─────────────────▼───────────────────┐
│    Mihomo 核心 (github.com/...)     │
│  - tunnel.Instance()                │
│  - adapter.Proxies()                │
│  - config.Parse()                   │
│  - listener.ReCreateTun()           │
└─────────────────────────────────────┘
```

### 与桌面端对比

| 功能 | 桌面端 (Rust/Tauri) | 移动端 (Kotlin/Go) |
|------|--------------------|--------------------|
| Mihomo 集成 | 独立进程 + IPC | 嵌入式 + JNI |
| API 调用 | HTTP/WebSocket | 直接函数调用 |
| TUN 模式 | 系统 TUN | VPN Service + TUN |
| 配置管理 | 文件系统 | 文件系统 + SharedPreferences |
| 性能 | 中等（IPC 开销） | 高（无 IPC） |

### 代理选择流程

```
1. 用户点击节点
   ↓
2. Kotlin: switchProxy("PROXY", "香港-01")
   ↓
3. JNI: nativeSelectProxy(groupName, proxyName)
   ↓
4. Go: mihomoCore.selectProxy(...)
   ↓
5. Mihomo: selector.Set(proxyName)
   ↓
6. 保存选择到 SharedPreferences
   ↓
7. 更新缓存
   ↓
8. 返回成功
```

## 📝 常见问题

**Q: 为什么要嵌入 Mihomo 而不是使用独立进程？**

A: Android 对后台进程有严格限制，嵌入式方案更稳定且性能更好。

**Q: 支持哪些代理协议？**

A: 支持 Mihomo 的所有协议：Shadowsocks、VMess、Trojan、VLESS、Hysteria、TUIC 等。

**Q: 可以同时运行多个配置吗？**

A: 不可以，同一时间只能加载一个配置文件。

**Q: 如何更新 Mihomo 核心版本？**

A: 修改 `go.mod` 中的版本号，然后重新编译。

**Q: 是否支持订阅链接？**

A: 是的，在"配置"页面可以添加订阅链接，自动下载和更新配置。

## 🎉 总结

现在您的 Clash Verge Rev Android 应用已经集成了**完整的 Mihomo 核心**，提供真正的代理功能！

主要特性：
- ✅ 完整的代理协议支持
- ✅ 规则引擎和分流
- ✅ 节点选择和测速
- ✅ TUN 模式透明代理
- ✅ 实时连接管理
- ✅ 高性能零开销

祝您使用愉快！🚀

---

**贡献者**: AI Assistant  
**创建时间**: 2025-11-03  
**版本**: v2.0.0  
**许可证**: GPL-3.0

