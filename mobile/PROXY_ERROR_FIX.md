# 代理连接失败问题修复

## 错误信息
```
net::ERR_PROXY_CONNECTION_FAILED
```

## 原因分析

这个错误说明：
✅ **应用已成功设置系统代理**（好消息！）
❌ **但代理服务没有正常工作**

可能的原因：
1. Mihomo 核心没有正确启动监听端口
2. 配置文件加载失败
3. 端口被占用
4. TUN 模式和系统代理冲突

## 快速修复步骤

### 方法 1: 临时禁用系统代理

如果您在 Windows 主机上遇到这个错误：

1. **Windows 设置** → **网络和 Internet** → **代理**
2. 关闭 "使用代理服务器"
3. 或者在浏览器中禁用代理

### 方法 2: 检查模拟器中的应用状态

在模拟器中：
1. 打开 Clash Verge Rev 应用
2. 检查连接状态
3. 如果显示"已连接"，尝试停止并重新启动

### 方法 3: 查看应用日志

在 PowerShell 中运行（我已经为您打开了日志窗口）：
```powershell
adb logcat -s ClashCore-Go:I ClashCore:I
```

查找以下关键信息：
- "Config loaded successfully" - 配置是否加载
- "API server started" - API 服务器是否启动
- "mixed-port" - 混合端口是否监听
- 任何错误信息

### 方法 4: 检查端口监听

```powershell
# 在模拟器中检查端口
adb shell netstat -tuln | Select-String "7897|9090"
```

应该看到：
- 7897 - mixed-port (HTTP/SOCKS5 代理)
- 9090 - external-controller (API)

如果没有看到这些端口，说明 Mihomo 核心没有启动。

## 诊断命令

### 1. 检查应用是否运行
```powershell
adb shell ps | Select-String "clash_verge_rev"
```

### 2. 查看核心初始化日志
```powershell
adb logcat -d | Select-String "Mihomo|ClashCore"
```

### 3. 检查配置文件是否加载
```powershell
adb shell run-as io.github.clash_verge_rev.clash_verge_rev ls files/
```

### 4. 测试 API 端口
```powershell
adb shell curl http://127.0.0.1:9090/version
```

## 常见问题和解决方案

### 问题 1: 配置文件未导入

**症状**: 应用启动但没有代理节点

**解决方案**:
1. 确认配置文件在 `/sdcard/Download/clash-config.yaml`
2. 在应用中手动导入配置
3. 查看是否有错误提示

### 问题 2: Mihomo 核心未启动

**症状**: 日志中没有 "Mihomo core initialized"

**解决方案**:
```powershell
# 重启应用
adb shell am force-stop io.github.clash_verge_rev.clash_verge_rev
adb shell am start -n io.github.clash_verge_rev.clash_verge_rev/.MainActivity
```

### 问题 3: 端口冲突

**症状**: 日志显示 "address already in use"

**解决方案**:
```powershell
# 查找占用端口的进程
adb shell netstat -tuln | Select-String "7897"

# 如果有其他进程占用，修改配置文件中的端口
# mixed-port: 7898
```

### 问题 4: 权限问题

**症状**: 日志显示 "permission denied"

**解决方案**:
```powershell
# 授予存储权限
adb shell pm grant io.github.clash_verge_rev.clash_verge_rev android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant io.github.clash_verge_rev.clash_verge_rev android.permission.WRITE_EXTERNAL_STORAGE
```

## 推荐操作流程

### A. 如果这是首次运行

1. **不要急于启动代理**
2. 先在应用中导入配置文件
3. 查看代理列表是否显示
4. 测试节点延迟
5. 确认一切正常后再启动

### B. 如果应用已在运行

1. 在模拟器中打开应用
2. 点击"停止"按钮
3. 查看主机的系统代理设置，确保已禁用
4. 检查应用日志
5. 重新导入配置
6. 再次尝试启动

### C. 完整重置

如果以上方法都不行：

```powershell
# 1. 停止应用
adb shell am force-stop io.github.clash_verge_rev.clash_verge_rev

# 2. 清除应用数据
adb shell pm clear io.github.clash_verge_rev.clash_verge_rev

# 3. 重新推送配置
adb push test-config.yaml /sdcard/Download/clash-config.yaml

# 4. 重新启动应用
adb shell am start -n io.github.clash_verge_rev.clash_verge_rev/.MainActivity
```

## 预期的正常日志

成功启动后应该看到：

```
I/ClashCore-Go: Initializing Mihomo core...
I/ClashCore-Go: Mihomo components initialized
I/ClashCore-Go: ✅ Mihomo HTTP API will be available after config is loaded
I/ClashCore-Go: Loading config from: .../config.yaml
I/ClashCore-Go: Config reloaded successfully
I/ClashCore-Go: ✅ Mihomo config reloaded, API server started
I/MihomoAPI: Starting API server on 127.0.0.1:9090
I/ClashCore-Go: ✅ Simple SOCKS5 proxy started on 127.0.0.1:7897
```

如果看到这些日志，说明核心已正常工作！

## 下一步

请执行以下命令并告诉我输出结果：

```powershell
# 1. 查看应用进程
adb shell ps | Select-String "clash"

# 2. 查看最近的日志
adb logcat -d -s ClashCore-Go:I | Select-String -Pattern "Mihomo|Config|API" -Context 0,2

# 3. 检查端口
adb shell netstat -tuln | Select-String "7897|9090"
```

我会根据输出帮你进一步诊断！

