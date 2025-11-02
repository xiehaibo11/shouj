# 按键开启功能测试指南 ✅

## 📋 已实现的功能

### ✅ 数据持久化（SharedPreferences）

所有设置现在都会自动保存并在应用重启后恢复！

```kotlin
SettingsManager.getInstance(context)
├── 系统设置 (4项)
│   ├── TUN模式
│   ├── 系统代理
│   ├── 开机自启
│   └── 静默启动
├── Clash设置 (5项)
│   ├── 局域网连接
│   ├── DNS覆写
│   ├── IPv6支持
│   ├── 统一延迟
│   └── 日志等级
├── 端口设置 (2项)
│   ├── 混合端口
│   └── 外部控制
└── 外观设置 (1项)
    └── 主题模式
```

---

## 🧪 测试步骤

### 第1步：编译安装

```bash
cd mobile
./gradlew assembleDebug --no-configuration-cache
adb -s 127.0.0.1:7555 install -r "app\build\outputs\apk\debug\app-x86_64-debug.apk"
adb -s 127.0.0.1:7555 shell am start -n io.github.clash_verge_rev.clash_verge_rev.debug/io.github.clash_verge_rev.clash_verge_rev.MainActivity
```

### 第2步：测试设置开关

#### A. 打开设置页面
1. 点击右上角 `⋮` 菜单
2. 选择"设置"
3. 设置对话框弹出

#### B. 测试每个开关
```
✅ 系统设置
  [ ] TUN模式 - 切换开关，检查副标题变化
  [ ] 系统代理 - 切换开关，检查副标题变化
  [ ] 开机自启 - 切换开关，检查副标题变化
  [ ] 静默启动 - 切换开关，检查副标题变化

✅ Clash设置
  [ ] 局域网连接 - 切换开关，检查副标题变化
  [ ] DNS覆写 - 切换开关，检查副标题变化
  [ ] IPv6支持 - 切换开关，检查副标题变化
  [ ] 统一延迟 - 切换开关，检查副标题变化
  [ ] 日志等级 - 点击选择：Debug/Info/Warn/Error/Silent
```

#### C. 测试端口编辑
```
[ ] 混合端口 - 点击 → 输入 "8080" → 确定 → 检查显示
[ ] 外部控制 - 点击 → 输入 "0.0.0.0:9090" → 确定 → 检查显示
```

#### D. 测试主题选择
```
[ ] 主题模式 - 点击 → 选择"深色" → 检查显示
```

### 第3步：测试数据持久化 🔑

**这是最重要的测试！**

1. **修改一些设置**：
   ```
   - TUN模式：开启
   - 局域网连接：开启
   - 日志等级：Debug
   - 混合端口：8080
   - 主题：深色
   ```

2. **关闭设置对话框**

3. **完全关闭应用**：
   ```bash
   adb -s 127.0.0.1:7555 shell am force-stop io.github.clash_verge_rev.clash_verge_rev.debug
   ```

4. **重新打开应用**：
   ```bash
   adb -s 127.0.0.1:7555 shell am start -n io.github.clash_verge_rev.clash_verge_rev.debug/io.github.clash_verge_rev.clash_verge_rev.MainActivity
   ```

5. **再次打开设置**：
   - ✅ 所有修改的设置应该保持原样！
   - ✅ TUN模式仍然是"开启"
   - ✅ 日志等级仍然是"Debug"
   - ✅ 混合端口仍然是"8080"

### 第4步：查看日志验证

```bash
adb -s 127.0.0.1:7555 logcat -d | Select-String -Pattern "SettingsManager"
```

应该看到类似输出：
```
I/SettingsManager: TUN模式: true
I/SettingsManager: 局域网连接: true
I/SettingsManager: 日志等级: Debug
I/SettingsManager: 混合端口: 8080
```

---

## 🎯 测试主页连接按钮

### 当前状态
- ✅ 按钮UI正常显示
- ✅ 点击会触发VPN权限请求
- ⚠️ Native库未完全初始化（会显示友好错误）

### 测试步骤

1. **点击主页大圆按钮**
2. **观察行为**：
   - Android会弹出VPN权限对话框
   - 点击"确定"授权
   - 如果成功：状态变为"已连接"
   - 如果失败：显示友好错误提示（而不是崩溃）

3. **查看日志**：
```bash
adb -s 127.0.0.1:7555 logcat -d -s ClashCore:* | Select-Object -Last 20
```

### 预期结果

#### 场景A：Native库加载成功
```
I/ClashCore: Native libraries loaded successfully
I/ClashCore: Initializing core: /data/user/0/.../files, 2.4.3-debug
I/ClashCore: Core initialized successfully
```

#### 场景B：Native库加载失败（当前状态）
```
E/ClashCore: Native method not found
W/ClashCore: Native libraries not loaded, skipping initialization
```
- **不会崩溃** ✅
- 显示友好错误提示
- 应用继续正常运行

---

## 📊 功能状态矩阵

| 功能 | UI | 数据持久化 | 后端逻辑 | 状态 |
|------|----|-----------| ---------|------|
| **设置页面** |
| 系统设置开关 (4项) | ✅ | ✅ | ⏳ | 70% |
| Clash设置开关 (4项) | ✅ | ✅ | ⏳ | 70% |
| 日志等级选择 | ✅ | ✅ | ⏳ | 70% |
| 端口编辑 (2项) | ✅ | ✅ | ⏳ | 70% |
| 主题选择 | ✅ | ✅ | ⏳ | 70% |
| **主页按钮** |
| VPN连接按钮 | ✅ | N/A | ⏳ | 50% |
| 权限请求 | ✅ | N/A | ✅ | 100% |
| 状态显示 | ✅ | N/A | ✅ | 100% |

---

## ✅ 已实现的改进

### 1. **防崩溃机制**
```kotlin
// ClashCore.kt 第35-40行
catch (e: UnsatisfiedLinkError) {
    Log.e(TAG, "Native method not found", e)
    nativeLibrariesLoaded = false // 标记为未加载
    // 不抛出异常，应用继续运行
}
```

### 2. **数据持久化**
```kotlin
// SettingsManager.kt
- 使用SharedPreferences自动保存
- 使用Compose State自动更新UI
- 重启后自动恢复设置
```

### 3. **友好的用户体验**
- ✅ 所有开关都有清晰的状态说明
- ✅ 编辑对话框提供取消选项
- ✅ 设置实时保存，无需确认按钮
- ✅ 错误不会导致崩溃

---

## 🐛 已知问题与解决方案

### 问题1：点击"运行代理"崩溃
**状态**: ✅ 已修复
**解决方案**: 
- 添加了UnsatisfiedLinkError捕获
- 应用不再崩溃，显示友好提示

### 问题2：设置不会保存
**状态**: ✅ 已修复
**解决方案**: 
- 实现了SettingsManager
- 所有设置自动持久化

### 问题3：Native库未初始化
**状态**: ⏳ 进行中
**当前**: Native函数签名不匹配
**需要**: 
1. 检查Go代码的export函数
2. 验证JNI桥接层
3. 重新编译native库

---

## 🔄 下一步开发任务

### 立即可做（不依赖Native）
1. ✅ 数据持久化 - **已完成**
2. ⏳ Toast提示消息
3. ⏳ 输入验证（端口号范围等）
4. ⏳ 错误提示优化

### 需要Native支持
1. ⏳ 应用Clash配置到核心
2. ⏳ VPN连接实际功能
3. ⏳ 流量统计显示
4. ⏳ 代理测速功能

---

## 💡 测试技巧

### 快速验证持久化
```bash
# 设置一些值
adb -s 127.0.0.1:7555 shell am start -n io.github.clash_verge_rev.clash_verge_rev.debug/.MainActivity

# 查看SharedPreferences文件
adb -s 127.0.0.1:7555 shell "cat /data/data/io.github.clash_verge_rev.clash_verge_rev.debug/shared_prefs/clash_verge_settings.xml"
```

### 查看实时日志
```bash
adb -s 127.0.0.1:7555 logcat -c
adb -s 127.0.0.1:7555 logcat -s SettingsManager:* ClashCore:*
```

### 重置所有设置
```bash
adb -s 127.0.0.1:7555 shell pm clear io.github.clash_verge_rev.clash_verge_rev.debug
```

---

## 📋 测试检查清单

### ✅ 基础功能测试
- [ ] 打开设置页面无崩溃
- [ ] 所有18个设置项正确显示
- [ ] 9个开关可以切换
- [ ] 3个对话框可以弹出
- [ ] 副标题随状态更新

### ✅ 数据持久化测试
- [ ] 修改设置 → 关闭对话框 → 重新打开 → 设置保持
- [ ] 修改设置 → 关闭应用 → 重新打开 → 设置保持
- [ ] 修改设置 → 查看日志确认保存
- [ ] 修改设置 → 查看SharedPreferences文件

### ✅ 主页按钮测试
- [ ] 点击连接按钮不崩溃
- [ ] VPN权限对话框正常弹出
- [ ] 拒绝权限后应用继续运行
- [ ] 授权后尝试启动VPN服务

---

## 🎉 成功标志

当您看到以下情况时，说明功能正常：

1. **设置页面**：
   - ✅ 所有开关能切换且保存
   - ✅ 对话框能打开和关闭
   - ✅ 关闭应用后设置仍然保留
   - ✅ 日志中有"SettingsManager"相关输出

2. **主页按钮**：
   - ✅ 点击不崩溃
   - ✅ 显示VPN权限对话框
   - ✅ 即使连接失败，应用继续运行

3. **整体表现**：
   - ✅ 无unhandled exceptions
   - ✅ UI响应流畅
   - ✅ 状态显示正确

---

**测试完成后，请告诉我结果！** 📊

如果遇到问题，提供以下信息：
1. 具体操作步骤
2. 日志输出 (`logcat`)
3. 屏幕截图（如果可能）

