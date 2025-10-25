# 手机端闪退问题排查指南

## 🔍 常见闪退原因

### 1. **View 初始化失败**
**症状**: 应用启动后立即闪退
**原因**: 
- `findViewById` 找不到对应的 View ID
- 布局文件加载失败
- Material Design 组件缺失

**解决方案**: ✅ 已修复
- 添加了 try-catch 异常处理
- 显示详细错误信息
- 安全的初始化流程

### 2. **权限问题**
**症状**: 点击连接按钮后闪退
**原因**:
- VPN 权限未授予
- 通知权限缺失（Android 13+）

**解决方案**:
```xml
<!-- AndroidManifest.xml 已包含 -->
<uses-permission android:name="android.permission.BIND_VPN_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### 3. **依赖缺失**
**症状**: 找不到类或方法
**原因**:
- Material Design 库未正确导入
- Kotlin 标准库版本不匹配

**解决方案**: ✅ 已配置
```gradle
implementation "com.google.android.material:material:1.11.0"
implementation "org.jetbrains.kotlin:kotlin-stdlib:1.9.0"
implementation "androidx.appcompat:appcompat:1.6.1"
```

### 4. **ProGuard 混淆问题**
**症状**: Release 版本闪退，Debug 版本正常
**原因**: 代码混淆导致反射失败

**解决方案**: ✅ 已禁用
```gradle
buildTypes {
    release {
        minifyEnabled false  // 暂时禁用混淆
    }
}
```

## 📱 如何获取崩溃日志

### 方法 1: 使用 ADB (推荐)
```bash
# 连接手机后执行
adb logcat | grep -E "AndroidRuntime|FATAL|Exception"

# 或者保存到文件
adb logcat > crash.log
```

### 方法 2: 使用 Android Studio
1. 打开 Android Studio
2. 连接手机
3. 打开 Logcat 窗口
4. 筛选 "Error" 级别日志
5. 重现闪退，查看错误信息

### 方法 3: 手机开发者选项
1. 设置 → 开发者选项
2. 启用 "USB 调试"
3. 启用 "显示所有 ANR"
4. 查看系统日志

## 🛠️ 修复步骤

### Step 1: 检查应用是否正确安装
```bash
adb shell pm list packages | grep clash
# 应该看到: package:io.github.clashverge.mobile
```

### Step 2: 清除应用数据
```bash
adb shell pm clear io.github.clashverge.mobile
```

### Step 3: 重新安装
```bash
adb install -r app-debug.apk
```

### Step 4: 查看实时日志
```bash
# 启动应用前执行
adb logcat -c  # 清空日志
adb logcat | grep "io.github.clashverge"
```

## 🐛 已修复的问题

### ✅ 1. 添加异常处理
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
        setContentView(R.layout.activity_main)
        notificationService = VpnNotificationService(this)
        initViews()
        setupListeners()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        finish()
    }
}
```

### ✅ 2. View 初始化安全检查
```kotlin
private fun initViews() {
    try {
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusDesc = findViewById(R.id.tvStatusDesc)
        btnToggleVPN = findViewById(R.id.btnToggleVPN)
        tvUpload = findViewById(R.id.tvUpload)
        tvDownload = findViewById(R.id.tvDownload)
        tvCurrentProxy = findViewById(R.id.tvCurrentProxy)
    } catch (e: Exception) {
        e.printStackTrace()
        throw RuntimeException("View 初始化失败: ${e.message}", e)
    }
}
```

### ✅ 3. 布局文件完整性
- 所有 View ID 都已正确定义
- Material Design 组件正确使用
- 布局层级合理

## 📋 检查清单

在报告闪退问题前，请确认：

- [ ] 手机 Android 版本 >= 7.0 (API 24)
- [ ] 已授予 VPN 权限
- [ ] 已授予通知权限（Android 13+）
- [ ] 存储空间充足
- [ ] 已清除旧版本数据
- [ ] 使用最新构建的 APK

## 🔧 调试模式

### 启用详细日志
在 `MainActivity.kt` 中添加：
```kotlin
companion object {
    private const val TAG = "ClashVerge"
    private const val DEBUG = true
}

private fun log(message: String) {
    if (DEBUG) {
        android.util.Log.d(TAG, message)
    }
}
```

### 使用日志
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    log("onCreate started")
    
    try {
        setContentView(R.layout.activity_main)
        log("Layout inflated")
        
        notificationService = VpnNotificationService(this)
        log("Notification service initialized")
        
        initViews()
        log("Views initialized")
        
        setupListeners()
        log("Listeners setup")
    } catch (e: Exception) {
        log("Error in onCreate: ${e.message}")
        e.printStackTrace()
        Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        finish()
    }
}
```

## 📞 获取帮助

如果问题仍然存在，请提供：

1. **设备信息**
   - 手机型号
   - Android 版本
   - 系统 UI (MIUI/ColorOS/OneUI 等)

2. **崩溃日志**
   ```bash
   adb logcat -d > crash.log
   ```

3. **复现步骤**
   - 详细描述操作步骤
   - 是否每次都闪退
   - 特定操作后闪退

4. **APK 信息**
   - 构建时间
   - 版本号
   - Debug 还是 Release

## 🎯 快速测试

### 测试 1: 基础启动
```bash
adb shell am start -n io.github.clashverge.mobile/.MainActivity
```

### 测试 2: 检查 Activity 是否注册
```bash
adb shell dumpsys package io.github.clashverge.mobile | grep Activity
```

### 测试 3: 检查权限
```bash
adb shell dumpsys package io.github.clashverge.mobile | grep permission
```

## 📝 常见错误信息

### 错误 1: `android.content.res.Resources$NotFoundException`
**原因**: 资源文件缺失或 ID 错误
**解决**: 检查 `R.layout.activity_main` 和所有 `R.id.*`

### 错误 2: `java.lang.ClassNotFoundException`
**原因**: 类找不到，可能是混淆或依赖问题
**解决**: 检查 ProGuard 规则，确保依赖完整

### 错误 3: `android.view.InflateException`
**原因**: 布局文件解析失败
**解决**: 检查 XML 语法，确保所有自定义 View 存在

### 错误 4: `java.lang.NullPointerException`
**原因**: 空指针异常
**解决**: 检查 `findViewById` 是否返回 null

## 🚀 性能优化建议

1. **延迟初始化非关键组件**
2. **使用 ViewBinding 替代 findViewById**
3. **异步加载重资源**
4. **添加启动画面（Splash Screen）**

## 📚 参考资料

- [Android 调试指南](https://developer.android.com/studio/debug)
- [崩溃日志分析](https://developer.android.com/studio/debug/bug-report)
- [Material Design 组件](https://material.io/develop/android)

