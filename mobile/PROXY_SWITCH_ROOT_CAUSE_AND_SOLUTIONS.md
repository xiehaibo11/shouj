# 代理切换失败：根本原因分析与解决方案

## 🔍 **问题诊断**

### 当前错误
```
Failed to connect to /127.0.0.1:9090
ECONNREFUSED (Connection refused)
```

### 根本原因链

#### 1. **Mihomo HTTP API服务器未启动**
- 应用期望通过HTTP API（127.0.0.1:9090）切换代理
- 但API服务器从未启动

#### 2. **Go核心组件未初始化**
在 `mobile/app/src/main/golang/mihomo_core.go:95-100`：
```go
// 初始化 Mihomo 核心组件
// 实际集成时取消注释以下代码
/*
	if err := core.initMihomoComponents(); err != nil {
		cancel()
		return fmt.Errorf("failed to init mihomo components: %w", err)
	}
*/
```

**这些关键组件被注释掉了：**
- ❌ Hub (包含HTTP API服务器)
- ❌ Tunnel (代理隧道)
- ❌ Config 加载器
- ❌ 所有核心功能

#### 3. **JNI桥接库加载失败**
```
java.lang.UnsatisfiedLinkError: no error!
```

**问题原因：**
- libclash.so（Go编译）- ✅ 成功加载
- libclash-jni.so（C++桥接）- ❌ 加载失败

**为什么失败：**
1. libclash-jni.so期望从libclash.so中解析符号
2. 但运行时动态链接器找不到这些符号
3. 虽然我们在CMake中添加了`-Wl,--unresolved-symbols=ignore-all`，但Android的链接器仍然严格检查

---

## 💡 **三个解决方案**

### ⭐ **方案A：完整集成Mihomo核心（推荐，但需要重新编译）**

#### 步骤：

1. **取消注释Go代码中的Mihomo初始化**

`mobile/app/src/main/golang/mihomo_core.go`:
```go
// 95行，取消注释
if err := core.initMihomoComponents(); err != nil {
    cancel()
    return fmt.Errorf("failed to init mihomo components: %w", err)
}
```

2. **实现`initMihomoComponents()`**

```go
func (m *MihomoCore) initMihomoComponents() error {
    // 设置 Mihomo 工作目录
    constant.SetHomeDir(m.homeDir)
    constant.SetConfig(filepath.Join(m.homeDir, "config.yaml"))

    // 初始化 Hub (包含 API 服务器)
    hub.Parse(hub.WithController("127.0.0.1:9090"), hub.WithSecret(""))

    // 初始化 Tunnel
    tunnel.Instance()

    return nil
}
```

3. **重新编译Go代码**

需要Android NDK和Go mobile工具：
```bash
cd mobile/app/src/main/golang
gomobile bind -target=android .
```

4. **替换jniLibs中的libclash.so**

**优点：**
- ✅ 完整的Mihomo功能
- ✅ HTTP API和JNI双模式支持
- ✅ 与桌面端一致

**缺点：**
- ❌ 需要Go mobile工具链（当前环境可能没有）
- ❌ 编译复杂度高

---

### ⚡ **方案B：直接使用已有的JNI函数（最快）**

Go代码已经暴露了`nativeSelectProxy`函数（main.go:337-357），我们只需要：

#### 步骤：

1. **修复libclash-jni.so的链接问题**

在`native-lib.cpp`中添加调用声明：
```cpp
// 直接调用Go导出的函数
extern "C" {
    int nativeSelectProxy(const char* groupName, const char* proxyName);
}

// JNI包装
JNIEXPORT jint JNICALL
Java_..._ClashCore_nativeSelectProxy(
        JNIEnv* env, jobject, jstring group, jstring proxy) {
    const char* groupStr = env->GetStringUTFChars(group, nullptr);
    const char* proxyStr = env->GetStringUTFChars(proxy, nullptr);
    
    int result = nativeSelectProxy(groupStr, proxyStr);
    
    env->ReleaseStringUTFChars(group, groupStr);
    env->ReleaseStringUTFChars(proxy, proxyStr);
    return result;
}
```

2. **在ClashVpnService中初始化核心**

```kotlin
private fun startVpn() {
    // 1. 初始化Go核心
    ClashCore.init(this)
    
    // 2. 加载配置（这会初始化基本的代理功能）
    ClashCore.loadConfig(configPath)
    
    // 现在可以通过nativeSelectProxy切换代理了！
}
```

**优点：**
- ✅ 不需要HTTP API
- ✅ 直接函数调用，性能更好
- ✅ 不需要重新编译Go代码

**缺点：**
- ❌ 仍然需要解决libclash-jni.so的加载问题
- ❌ 当前libclash-jni.so无法加载

---

### 🚀 **方案C：运行Mihomo可执行文件（类似桌面端）**

将libclash.so改为可执行文件，然后作为子进程运行。

#### 步骤：

1. **重新编译Go为可执行文件**

```bash
GOOS=android GOARCH=amd64 go build -buildmode=pie -o mihomo-android main.go
```

2. **打包到assets**

```
mobile/app/src/main/assets/
└── mihomo-android-x86_64
└── mihomo-android-arm64
└── mihomo-android-armv7
```

3. **Android启动进程**

```kotlin
private fun startMihomo(): Process {
    val mihomoPath = extractMihomoExecutable()
    
    val process = ProcessBuilder()
        .command(mihomoPath, "-d", homeDir, "-f", configPath)
        .start()
    
    // HTTP API现在会自动启动在127.0.0.1:9090
    return process
}
```

**优点：**
- ✅ 完全模仿桌面端架构
- ✅ HTTP API自动可用
- ✅ 进程隔离，更安全

**缺点：**
- ❌ 需要重新编译Go
- ❌ 需要处理进程生命周期管理
- ❌ 内存占用可能更高（独立进程）

---

## 🎯 **当前最佳方案**

### **临时解决方案（现在就能工作）：**

1. **跳过JNI，直接在配置文件中硬编码默认节点**
2. **或者提供UI让用户手动编辑配置文件选择节点**
3. **等待Go代码正确集成后再支持动态切换**

### **长期解决方案：**

**推荐：方案A（完整集成）+ 方案B（JNI优化）**

1. 首先解决Go编译环境
2. 取消注释并实现Mihomo核心初始化
3. 确保HTTP API和JNI都可用
4. 优先使用JNI（更快），fallback到HTTP API

---

## 📋 **立即可执行的诊断命令**

```bash
# 1. 检查libclash.so导出的符号
llvm-readelf -Ws app/src/main/jniLibs/x86_64/libclash.so | grep native

# 2. 检查libclash-jni.so的依赖
llvm-readelf -d app/build/.../libclash-jni.so | grep NEEDED

# 3. 测试Go函数是否可用（如果能加载）
adb shell am instrument -w -e class io.github...Test#testNativeSelectProxy ...
```

---

## 🛠️ **需要的工具和环境**

### 重新编译Go代码需要：
- Go 1.21+
- gomobile工具: `go install golang.org/x/mobile/cmd/gomobile@latest`
- Android NDK 25.2.9519653
- 环境变量：
  ```bash
  export ANDROID_NDK_HOME=/path/to/ndk
  export PATH=$PATH:$GOPATH/bin
  ```

### 编译命令：
```bash
cd mobile/app/src/main/golang
gomobile bind -target=android -o ../jniLibs/libclash.aar .
```

---

## ✅ **验证清单**

完成后验证：

- [ ] libclash.so加载成功
- [ ] libclash-jni.so加载成功（如果使用JNI）
- [ ] ClashCore.init()调用成功
- [ ] ClashCore.loadConfig()成功加载配置
- [ ] 能够看到代理节点列表
- [ ] 点击节点后切换成功（no "connection refused"）
- [ ] 能够访问互联网

---

## 📞 **需要帮助？**

如果选择方案A或C，需要我帮助：
1. 设置Go编译环境
2. 修改Go代码实现initMihomoComponents
3. 重新编译libclash.so

**当前推荐：** 由于环境限制，建议先用**方案C（运行可执行文件）**，这样可以快速验证Mihomo核心功能，然后再优化为JNI方式。


