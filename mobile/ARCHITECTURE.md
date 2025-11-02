# Clash Verge Rev Android - 架构文档

## 📐 系统架构

Clash Verge Rev Android 采用三层架构设计：

```
┌─────────────────────────────────────────────────────┐
│              Android Application Layer              │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │   MainActivity │  │  VpnService  │  │    UI     │ │
│  │   (Compose)   │  │   Manager    │  │ Components│ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
│                        ↕                             │
│         ┌──────────────────────────────┐            │
│         │     ClashCore (Kotlin)       │            │
│         │   JNI Interface Wrapper      │            │
│         └──────────────────────────────┘            │
└─────────────────────────────────────────────────────┘
                        ↕ JNI Calls
┌─────────────────────────────────────────────────────┐
│              JNI Bridge Layer (C/C++)               │
│  ┌──────────────────────────────────────────────┐  │
│  │         native-lib.cpp                       │  │
│  │  - Java ↔ C type conversions                │  │
│  │  - Memory management                         │  │
│  │  - Error handling                            │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                        ↕ CGO Calls
┌─────────────────────────────────────────────────────┐
│          Go Core Layer (Mihomo/Clash Meta)          │
│  ┌──────────────────────────────────────────────┐  │
│  │         main.go (Core Logic)                 │  │
│  │  - TUN device management                     │  │
│  │  - Config parsing (YAML)                     │  │
│  │  - Proxy routing engine                      │  │
│  │  - Traffic statistics                        │  │
│  │  - DNS resolution                            │  │
│  └──────────────────────────────────────────────┘  │
│           ↕ (Future: Mihomo Integration)            │
│  ┌──────────────────────────────────────────────┐  │
│  │    github.com/metacubex/mihomo              │  │
│  │  - Full Clash Meta implementation            │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

## 🔄 数据流

### 1. VPN 启动流程

```
User Action (UI)
    ↓
MainActivity.requestVpnPermission()
    ↓
VpnService.prepare() → Request Permission
    ↓
ClashVpnService.startVpn()
    ├─→ ClashCore.init(context)
    │       ↓ JNI
    │   native-lib.cpp::nativeInit()
    │       ↓ CGO
    │   main.go::coreInit()
    │       ↓
    │   初始化工作目录、日志、配置
    │
    ├─→ ClashVpnService.loadClashConfig()
    │       ↓ JNI
    │   native-lib.cpp::nativeLoadConfig()
    │       ↓ CGO
    │   main.go::loadConfig()
    │       ↓
    │   解析 YAML配置、设置代理规则
    │
    └─→ VpnService.Builder.establish()
            ↓
        ParcelFileDescriptor (VPN fd)
            ↓
        ClashCore.startTun(fd, mtu)
            ↓ JNI
        native-lib.cpp::nativeStartTun()
            ↓ CGO
        main.go::startTun()
            ↓
        将 fd 传递给 Mihomo 核心
        开始处理网络数据包
```

### 2. 数据包处理流程

```
Android System
    ↓
VPN Interface (TUN)
    ↓
File Descriptor (fd) → Go Runtime
    ↓
Mihomo Core (Go)
    ├─→ 解析 IP 数据包
    ├─→ 应用规则匹配
    ├─→ 选择代理节点
    ├─→ 加密/转发
    └─→ 返回响应
```

## 📦 核心组件

### 1. Kotlin/Android 层

#### ClashCore.kt
- **职责**: JNI 接口封装
- **主要方法**:
  - `init(context)`: 初始化核心
  - `startTun(fd, mtu)`: 启动 TUN 设备
  - `stopTun()`: 停止 TUN 设备
  - `loadConfig(path)`: 加载配置文件
  - `queryTraffic()`: 查询流量统计
  - `getVersion()`: 获取核心版本

#### ClashVpnService.kt
- **职责**: Android VPN 服务管理
- **功能**:
  - VPN 接口建立
  - 前台服务通知
  - 生命周期管理
  - 配置加载
  - 状态广播

### 2. JNI 桥接层

#### native-lib.cpp
- **职责**: Java ↔ C/C++ 类型转换
- **关键点**:
  - JNI 函数命名规范
  - 内存管理 (GetStringUTFChars/ReleaseStringUTFChars)
  - 错误检查和异常处理
  - 日志记录

#### bridge.h
- **职责**: C 函数声明
- **导出函数**:
  ```c
  void coreInit(const char*, const char*);
  int startTun(int fd, int mtu);
  void stopTun();
  int loadConfig(const char*);
  long long queryTraffic();
  char* getVersion();
  void freeString(char*);
  ```

### 3. Go 核心层

#### main.go
- **职责**: 核心逻辑实现
- **导出函数** (使用 `//export`):
  - `coreInit`: 初始化核心环境
  - `startTun`: 启动 TUN 设备处理
  - `stopTun`: 停止 TUN 设备
  - `loadConfig`: 解析和加载 YAML 配置
  - `queryTraffic`: 返回流量统计
  - `getVersion`: 返回版本信息
  - `freeString`: 释放 C 字符串内存

## 🔧 构建系统

### Gradle 配置

```kotlin
// mobile/app/build.gradle.kts
externalNativeBuild {
    cmake {
        path = file("src/main/cpp/CMakeLists.txt")
    }
}
```

### CMake 配置

```cmake
# mobile/app/src/main/cpp/CMakeLists.txt
# 链接 Go 编译的 libclash.so
add_library(clash-core SHARED IMPORTED)
set_target_properties(clash-core PROPERTIES 
    IMPORTED_LOCATION ${GO_OUTPUT_DIR}/libclash.so)

# 链接到 JNI 库
target_link_libraries(clash-jni clash-core ${log-lib})
```

### Go 编译流程

```bash
# 设置环境变量
export GOOS=android
export GOARCH=arm64  # 或 arm, amd64, 386
export CGO_ENABLED=1
export CC=aarch64-linux-android21-clang

# 编译为共享库
go build -buildmode=c-shared \
    -ldflags="-s -w" \
    -o libclash.so main.go
```

## 🔐 内存管理

### C 字符串内存
- **分配**: Go 使用 `C.CString()` 分配
- **释放**: C/C++ 必须调用 `freeString()` 释放
- **示例**:
  ```cpp
  char* version = getVersion();
  jstring result = env->NewStringUTF(version);
  freeString(version);  // 必须释放！
  ```

### JNI 字符串
- **获取**: `GetStringUTFChars()`
- **释放**: `ReleaseStringUTFChars()`
- **示例**:
  ```cpp
  const char* str = env->GetStringUTFChars(jstr, nullptr);
  // ... 使用 str ...
  env->ReleaseStringUTFChars(jstr, str);  // 必须释放！
  ```

## ⚠️ 错误处理

### 分层错误处理

1. **Go 层**:
   ```go
   defer func() {
       if r := recover(); r != nil {
           C.LOGE(C.CString(fmt.Sprintf("Panic: %v", r)))
       }
   }()
   ```

2. **JNI 层**:
   ```cpp
   if (result != 0) {
       LOGE("Operation failed: %d", result);
   }
   ```

3. **Kotlin 层**:
   ```kotlin
   @Synchronized
   fun operation() {
       try {
           nativeOperation()
       } catch (e: Exception) {
           Log.e(TAG, "Failed", e)
           throw RuntimeException("Operation failed", e)
       }
   }
   ```

## 📝 配置文件格式

### config.yaml

```yaml
# Clash 配置示例
mixed-port: 7897          # HTTP + SOCKS5 混合端口
allow-lan: false          # 是否允许局域网连接
mode: rule                # 模式: rule/global/direct
log-level: info           # 日志级别

# DNS 配置
dns:
  enable: true
  listen: 0.0.0.0:1053
  enhanced-mode: fake-ip
  nameserver:
    - 8.8.8.8
    - 1.1.1.1

# 代理节点
proxies:
  - name: "节点1"
    type: ss              # shadowsocks
    server: server.com
    port: 443
    cipher: aes-256-gcm
    password: password

# 代理组
proxy-groups:
  - name: "PROXY"
    type: select
    proxies:
      - 节点1
      - DIRECT

# 规则
rules:
  - DOMAIN-SUFFIX,google.com,PROXY
  - GEOIP,CN,DIRECT
  - MATCH,PROXY
```

## 🚀 性能优化

### 1. 内存优化
- 使用 `@Synchronized` 避免并发问题
- 及时释放 C/Go 分配的内存
- 定期调用 `forceGc()` 清理 Go 内存

### 2. 线程管理
- Go 协程处理 TUN 数据包
- JNI 调用在主线程/工作线程
- VPN 服务使用 Kotlin 协程

### 3. 日志优化
- 使用 Android NDK 日志 (`__android_log_print`)
- 分级日志 (DEBUG/INFO/ERROR)
- 生产环境可关闭 DEBUG 日志

## 🐛 调试技巧

### 查看日志

```bash
# 查看所有日志
adb logcat | grep -E "ClashCore|ClashVpnService"

# 查看 Go 层日志
adb logcat | grep "ClashCore-Go"

# 查看 JNI 层日志
adb logcat | grep "ClashCore-JNI"
```

### 常见问题

1. **库加载失败**:
   - 检查 `.so` 文件是否存在于 `jniLibs/`
   - 确认架构匹配 (arm64-v8a, armeabi-v7a)
   
2. **JNI 方法未找到**:
   - 检查 JNI 函数命名 (包名下划线转义)
   - 确认 native 方法声明匹配

3. **VPN 无法启动**:
   - 检查 VPN 权限
   - 查看核心初始化日志
   - 验证配置文件格式

## 📚 参考资料

- [Clash Meta (Mihomo)](https://github.com/MetaCubeX/mihomo)
- [ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid)
- [Android VpnService API](https://developer.android.com/reference/android/net/VpnService)
- [JNI Tips](https://developer.android.com/training/articles/perf-jni)
- [Go Mobile](https://pkg.go.dev/golang.org/x/mobile)




