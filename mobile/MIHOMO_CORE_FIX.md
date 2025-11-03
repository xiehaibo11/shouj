# Mihomo核心启动问题修复方案

## 问题诊断

### 当前问题：
1. ❌ libclash.so加载成功，但JNI函数无法调用
2. ❌ nativeInit等函数调用失败 → Mihomo核心未初始化
3. ❌ API服务器（127.0.0.1:9090）未启动
4. ❌ 配置未加载 → 只显示DIRECT节点
5. ❌ 代理切换失败

### 根本原因：
**Go编译的libclash.so与C++ JNI桥接库之间的函数导出/链接问题。**

---

## 解决方案：绕过JNI，直接启动Mihomo

参考桌面端（Tauri）的实现，Mihomo应该作为独立进程启动，而不是通过JNI调用。

### 桌面端启动流程（参考）：

```rust
// 1. 准备配置文件
let config_path = "clash.yaml";

// 2. 启动Mihomo进程
Command::new("mihomo")
    .arg("-d").arg(home_dir)
    .arg("-f").arg(config_path)
    .spawn()

// 3. 等待API服务器启动

// 4. 通过HTTP API控制
// PUT http://127.0.0.1:9090/proxies/{group}
// Body: {"name": "proxy_name"}
```

### Android实现方案：

#### 方案A：使用Go的HTTP服务器模式（推荐）

1. **修改mihomo_core.go，添加独立启动函数：**

```go
//export nativeStartMihomoServer
func nativeStartMihomoServer(configPath *C.char, homeDir *C.char) C.int {
    path := C.GoString(configPath)
    home := C.GoString(homeDir)
    
    // 初始化Mihomo
    if mihomoCore == nil {
        if err := initMihomoCore(home); err != nil {
            return -1
        }
    }
    
    // 加载配置
    if err := mihomoCore.reloadConfig(path, true); err != nil {
        return -2
    }
    
    // Mihomo内部会自动启动API服务器（基于config中的external-controller）
    // 无需额外操作
    
    return 0
}
```

2. **在JNI层简单封装：**

```cpp
extern "C" JNIEXPORT jint JNICALL
Java_..._ClashCore_nativeStartMihomoServer(
        JNIEnv* env, jobject, jstring configPath, jstring homeDir) {
    const char* configPathStr = env->GetStringUTFChars(configPath, nullptr);
    const char* homeDirStr = env->GetStringUTFChars(homeDir, nullptr);
    
    int result = nativeStartMihomoServer(configPathStr, homeDirStr);
    
    env->ReleaseStringUTFChars(configPath, configPathStr);
    env->ReleaseStringUTFChars(homeDir, homeDirStr);
    return result;
}
```

3. **Kotlin层调用：**

```kotlin
object ClashCore {
    init {
        System.loadLibrary("clash")  // 只需加载Go核心
    }
    
    fun startMihomo(configPath: String, homeDir: String): Boolean {
        return nativeStartMihomoServer(configPath, homeDir) == 0
    }
    
    private external fun nativeStartMihomoServer(configPath: String, homeDir: String): Int
}
```

#### 方案B：最小化修改（临时方案）

如果无法重新编译Go代码，可以：

1. **检查libclash.so是否已经暴露了初始化函数**
2. **使用`dlopen`/`dlsym`动态加载符号**
3. **绕过JNI，直接调用C函数**

```kotlin
object MihomoBridge {
    init {
        System.loadLibrary("clash")
    }
    
    external fun directInit(homeDir: String, version: String): Int
    external fun directLoadConfig(configPath: String): Int
    
    // 直接映射到Go导出的函数
}
```

---

## 立即可行的临时方案

### 使用现有的Go代码，修复调用方式：

当前libclash.so已经编译并包含了所有函数，问题是调用方式。

**检查点：**
1. ✅ libclash.so已加载
2. ❌ libclash-jni.so加载失败（因为依赖符号找不到）
3. 🔍 需要确认：libclash.so导出了哪些符号？

**验证命令（在开发机上）：**
```bash
nm -D app/src/main/jniLibs/x86_64/libclash.so | grep native
```

**如果符号都在，问题是libclash-jni.so的链接配置错误。**

---

## 建议的修复步骤

### 立即执行（不需要重新编译Go）：

1. **修改CMakeLists.txt，正确链接libclash.so**
2. **确保libclash-jni.so能找到libclash.so的符号**
3. **或者：完全移除libclash-jni.so，直接在Kotlin中调用**

### 中期方案（需要修改Go代码）：

1. **简化Go导出函数**
2. **提供一个`start`函数，内部完成所有初始化**
3. **从Kotlin直接调用，不需要复杂的JNI桥接**

---

## 当前紧急修复

由于时间紧迫，采用**方案C：完全绕过native调用，纯HTTP API模式**：

1. **假设配置文件已准备好**（包含external-controller）
2. **手动启动一个后台线程，模拟Mihomo的功能**
3. **或者：使用Android自带的LocalServerSocket实现简单的代理**

这是最后的fallback方案，但至少能让应用跑起来。


