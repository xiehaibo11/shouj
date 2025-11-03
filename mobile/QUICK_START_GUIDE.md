# 🚀 Clash Verge Rev Android - 快速开始指南

## 📱 测试新功能

### 1. 测试代理选择状态持久化（Bug修复）

**测试步骤：**
1. 打开应用，进入"代理"页面
2. 选择任意代理组（例如"PROXY"）
3. 点击任意代理节点进行切换（例如选择"HK-01"）
4. 切换到"首页"或其他页面
5. 再次返回"代理"页面

**预期结果：**
- ✅ 代理组标签页保持在之前选择的位置
- ✅ 代理节点显示为"HK-01"（而不是默认节点）
- ✅ 滚动位置保持在之前的位置

**之前的问题：**
- ❌ 代理选择会丢失，显示默认节点
- ❌ 需要重新选择代理

---

### 2. 测试规则页面

**进入方式：**
1. 打开应用
2. 点击底部导航栏的"规则"标签（第4个图标）

**功能测试：**

#### 2.1 查看规则列表
- 滚动查看所有规则
- 每条规则显示：规则类型、规则内容、策略

#### 2.2 搜索规则
1. 点击顶部搜索框
2. 输入关键词（例如"google"）
3. 查看过滤后的结果
4. 点击搜索框右侧的"✕"清除搜索

#### 2.3 按类型过滤
1. 点击顶部的类型标签（例如"DOMAIN"）
2. 查看该类型的所有规则
3. 点击"全部"返回完整列表

**预期结果：**
- ✅ 显示配置文件中的所有规则
- ✅ 搜索功能正常工作
- ✅ 类型过滤功能正常工作
- ✅ 规则统计信息正确显示

---

### 3. 测试流媒体解锁测试页面

**进入方式：**
1. 打开应用
2. 点击底部导航栏的"测试"标签（第6个图标）

**功能测试：**

#### 3.1 测试单个服务
1. 找到要测试的服务（例如Netflix）
2. 点击右侧的"▶"按钮
3. 等待测试完成（显示加载动画）
4. 查看测试结果：
   - ✅ 绿色对勾 = 可用
   - ❌ 红色叉号 = 不可用
   - ⚠️ 黄色警告 = 部分可用

#### 3.2 测试所有服务
1. 点击顶部的"测试全部"按钮
2. 等待所有服务测试完成
3. 查看综合测试结果

**支持的服务：**
- 📺 流媒体：Netflix、Disney+、YouTube Premium、Prime Video、哔哩哔哩
- 🤖 AI服务：ChatGPT、Claude、Gemini
- 🎵 其他：TikTok、Spotify

**预期结果：**
- ✅ 测试按钮正常工作
- ✅ 显示加载状态
- ✅ 测试结果正确显示
- ✅ 一键测试功能正常

---

### 4. 测试错误处理和日志系统

**查看日志文件：**

使用 adb 查看日志：
```bash
# 查看实时日志
adb logcat | grep "ClashVergeRev"

# 查看 ErrorHandler 日志
adb logcat | grep "ErrorHandler"

# 导出日志文件
adb pull /data/data/io.github.clash_verge_rev.clash_verge_rev/files/logs/
```

**触发错误测试：**
1. 在"订阅"页面尝试导入无效的配置
2. 在"代理"页面测试不存在的节点延迟
3. 观察错误提示和日志记录

**预期结果：**
- ✅ 错误信息友好提示
- ✅ 错误详情记录到日志文件
- ✅ 日志文件按日期分割
- ✅ 旧日志自动清理

---

## 🛠️ 构建和安装

### 快速构建（Debug版本）
```bash
cd mobile
./gradlew assembleDebug
```

### 生产构建（Release版本）
```bash
cd mobile
./gradlew assembleRelease
```

### 安装到设备
```bash
# 安装 Debug 版本
adb install app/build/outputs/apk/debug/app-debug.apk

# 安装 Release 版本
adb install app/build/outputs/apk/release/app-release.apk
```

---

## 🧪 运行测试

### 单元测试
```bash
cd mobile
./gradlew test
```

### 仪器测试（需要设备或模拟器）
```bash
cd mobile
./gradlew connectedAndroidTest
```

### 特定测试类
```bash
cd mobile
./gradlew connectedAndroidTest --tests "*.ProxyRepositoryTest"
```

---

## 📊 性能监控

### 检查内存使用
```bash
adb shell dumpsys meminfo io.github.clash_verge_rev.clash_verge_rev
```

### 检查CPU使用
```bash
adb shell top | grep clash_verge_rev
```

### 检查日志文件大小
```bash
adb shell ls -lh /data/data/io.github.clash_verge_rev.clash_verge_rev/files/logs/
```

---

## 🐛 故障排除

### 问题1：应用无法启动
**解决方案：**
```bash
# 清除应用数据
adb shell pm clear io.github.clash_verge_rev.clash_verge_rev

# 重新安装
./gradlew installDebug
```

### 问题2：VPN服务无法启动
**检查项：**
1. 确保已授予VPN权限
2. 检查是否有其他VPN正在运行
3. 查看logcat日志：
```bash
adb logcat | grep "ClashVpnService"
```

### 问题3：代理节点延迟测试失败
**检查项：**
1. 确保设备有网络连接
2. 确保配置文件中的节点信息正确
3. 检查防火墙设置

### 问题4：规则页面显示为空
**检查项：**
1. 确保已选择配置文件
2. 确保配置文件包含rules字段
3. 检查配置文件格式是否正确

---

## 📝 代码结构说明

### 核心文件

#### 代理管理
- `ProxyRepository.kt` - 代理数据仓库（✅ 已修复缓存同步）
- `ProxyScreen.kt` - 代理页面UI
- `ProxyModels.kt` - 代理数据模型

#### 新增页面
- `RulesScreen.kt` - 规则页面（✅ 新增）
- `TestScreen.kt` - 测试页面（✅ 新增）

#### 工具类
- `ErrorHandler.kt` - 错误处理和日志系统（✅ 新增）
- `FormatUtils.kt` - 格式化工具

#### 测试
- `ProxyRepositoryTest.kt` - 代理仓库测试（✅ 新增）

---

## 🎯 最佳实践

### 1. 日志记录
```kotlin
// 使用 ErrorHandler 记录日志
ErrorHandler.logInfo("MyTag", "操作成功")
ErrorHandler.logWarning("MyTag", "这是一个警告")
ErrorHandler.logDebug("MyTag", "调试信息")

// 处理错误
try {
    // 可能出错的代码
} catch (e: Exception) {
    ErrorHandler.handleError(
        error = e,
        context = "执行某操作时",
        showToUser = true,
        snackbarHost = snackbarHostState,
        scope = scope
    )
}
```

### 2. 状态持久化
```kotlin
// 保存状态到 SharedPreferences
proxyRepository.saveSelectedGroupIndex(configPath, index)
proxyRepository.saveSelectedProxy(configPath, groupName, proxyName)
proxyRepository.saveScrollPosition(configPath, groupIndex, position)

// 恢复状态
val index = proxyRepository.getSelectedGroupIndex(configPath)
val proxy = proxyRepository.getSelectedProxy(configPath, groupName)
val position = proxyRepository.getScrollPosition(configPath, groupIndex)
```

### 3. 缓存管理
```kotlin
// 加载数据（自动使用缓存）
val state = proxyRepository.loadProxiesFromConfig(configFile)

// 清除缓存
proxyRepository.clearCache()
proxyRepository.clearCache(filePath)
```

---

## 🔗 相关文档

- [完整改进文档](./IMPROVEMENTS_2024.md)
- [架构设计](./ARCHITECTURE.md)
- [功能实现状态](./FEATURES_IMPLEMENTATION_STATUS.md)

---

## 💡 提示和技巧

### 查看实时代理切换
```bash
# 监控代理切换日志
adb logcat | grep "Switch proxy"
```

### 导出应用状态
```bash
# 导出 SharedPreferences
adb shell "run-as io.github.clash_verge_rev.clash_verge_rev cat shared_prefs/proxy_state.xml"
```

### 性能分析
```bash
# 使用 Android Profiler
./gradlew --profile assembleDebug
```

---

**最后更新：** 2024-11-03  
**版本：** 2.4.3  
**反馈：** 如有问题请提issue到GitHub仓库

