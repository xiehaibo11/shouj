# Clash Verge Rev Android - 完整使用和测试指南

## 🚀 快速开始

### 1. 安装应用
```powershell
cd C:\Users\Administrator\Desktop\clash-verge-rev\mobile
.\gradlew.bat assembleDebug installDebug
```

### 2. 首次使用步骤

#### 步骤1：导入订阅
1. 打开应用
2. 点击底部导航 "配置"
3. 点击 "+ 新建配置"
4. 选择 "导入订阅"
5. 输入订阅URL：`https://47.238.198.94/iv/verify_mode.htm?token=5deb6dce926526eda7974a73ffe38b4e`
6. 点击 "添加"

#### 步骤2：启动VPN服务
1. 返回 "首页"
2. 找到 "TUN模式" 开关
3. 点击开关启动VPN
4. ⚠️ **Android会弹出VPN权限请求** - 点击"确定"
5. 等待状态变为 "虚拟网卡已启用"

#### 步骤3：切换代理模式
在首页，点击模式按钮切换：
- **Rule** - 规则模式（默认）
- **Global** - 全局代理
- **Direct** - 直连模式

#### 步骤4：选择代理节点
1. 点击底部导航 "代理"
2. 展开代理组（如 "自动选择"）
3. 点击任意节点切换

## 📋 功能自检清单

### ✅ 基础功能

| 功能 | 检查方法 | 预期结果 |
|-----|---------|---------|
| 应用启动 | 打开应用 | 显示首页 |
| 配置导入 | 配置页面导入订阅 | 显示配置列表 |
| 配置更新 | 点击配置的刷新按钮 | 更新成功提示 |
| VPN启动 | 首页点击TUN开关 | 显示"已启用" |
| 模式切换 | 点击Rule/Global/Direct按钮 | 按钮状态改变 |
| 节点切换 | 代理页面点击节点 | 节点被选中 |
| 延迟测试 | 代理页面点击测速图标 | 显示延迟数值 |
| 流量统计 | 首页查看流量卡片 | 显示上传/下载速度 |

### ⚙️ 高级功能

| 功能 | 位置 | 说明 |
|-----|-----|------|
| 连接管理 | 连接页面 | 查看实时连接 |
| 规则查看 | 规则页面 | 查看路由规则 |
| 日志查看 | 日志页面 | 查看运行日志 |
| TUN配置 | 设置页面 | TUN模式详细设置 |
| Merge配置 | 配置页面 | 全局配置覆盖 |
| Script配置 | 配置页面 | JavaScript配置处理 |

## 🔧 故障排查

### 问题1：VPN启动失败

**症状**：点击TUN开关后无反应或报错

**解决方案**：
1. 检查日志：
```powershell
adb logcat -d | Select-String "ClashVpnService|VPN" | Select-Object -Last 50
```

2. 查看错误信息：
- "Config file not found" → 先导入订阅
- "Permission denied" → 重新授予VPN权限
- "Native library error" → 重新安装应用

3. 重启应用：
```powershell
adb shell am force-stop io.github.clash_verge_rev.clash_verge_rev.debug
adb shell am start -n io.github.clash_verge_rev.clash_verge_rev.debug/io.github.clash_verge_rev.clash_verge_rev.MainActivity
```

### 问题2：节点切换失败

**症状**：点击节点无反应或提示失败

**可能原因**：
1. HTTP API服务器未启动（VPN未启动）
2. 配置文件未正确加载
3. 网络连接问题

**解决方案**：
1. 确保VPN已启动
2. 检查API服务器日志：
```powershell
adb logcat -d | Select-String "ProxyApiServer|API Request" | Select-Object -Last 30
```

3. 查看详细错误：
```powershell
adb logcat -d | Select-String "ProxyRepository.*switch" | Select-Object -Last 20
```

### 问题3：模式切换无效

**症状**：点击模式按钮后模式未改变

**解决方案**：
1. 检查API调用：
```powershell
adb logcat -d | Select-String "switchMode|PATCH.*configs" | Select-Object -Last 10
```

2. 查看配置文件是否更新：
```powershell
adb shell "run-as io.github.clash_verge_rev.clash_verge_rev.debug cat /data/user/0/io.github.clash_verge_rev.clash_verge_rev.debug/files/configs/*.yaml | grep mode"
```

## 🧪 完整功能测试脚本

### 自动化测试
```powershell
cd C:\Users\Administrator\Desktop\clash-verge-rev\mobile
powershell -ExecutionPolicy Bypass -File .\scripts\test-all-functions.ps1
```

### 手动测试流程

#### 测试1：VPN服务
```powershell
# 1. 启动VPN
# 在应用中点击TUN开关

# 2. 等待3秒，检查日志
adb logcat -d | Select-String "ClashVpnService.*Started|API Server started" | Select-Object -Last 5
```

预期输出：
```
ClashVpnService: Starting VPN service...
ClashVpnService: ✅ VPN started successfully
ProxyApiServer: ✅ API Server started on port 9090
```

#### 测试2：节点切换
```powershell
# 1. 在代理页面选择节点
# 2. 查看日志
adb logcat -d | Select-String "switchProxy|API Request.*proxies" | Select-Object -Last 10
```

预期输出：
```
ProxyRepository: ⚙️ Switching proxy: 节点名称
ProxyApiServer: API Request: PUT /proxies/自动选择
ProxyRepository: ✅ Proxy switched successfully
```

#### 测试3：模式切换
```powershell
# 1. 在首页切换模式
# 2. 查看日志
adb logcat -d | Select-String "switchMode|Mode.*switched" | Select-Object -Last 10
```

预期输出：
```
ProxyRepository: ⚙️ Switching mode to: global
ProxyApiServer: API Request: PATCH /configs
ProxyApiServer: Mode updated to global
ProxyRepository: ✅ Mode switched successfully
```

## 📊 性能监控

### 查看实时日志
```powershell
adb logcat -s ProxyApiServer:I ProxyRepository:I ClashVpnService:I
```

### 查看流量统计
```powershell
adb logcat -d | Select-String "Traffic|upload|download" | Select-Object -Last 20
```

### 查看连接信息
```powershell
adb logcat -d | Select-String "Connection|connect" | Select-Object -Last 30
```

## 🎯 已知限制

### Android平台特性
1. **不需要系统代理** - Android使用TUN模式，无需设置系统代理
2. **VPN权限必需** - 首次启动需授予VPN权限
3. **后台限制** - 某些手机可能限制后台运行

### 当前实现状态

#### ✅ 已实现
- TUN模式VPN服务
- HTTP API服务器（Kotlin实现）
- 代理节点切换
- 代理模式切换（Rule/Global/Direct）
- 流量统计
- 延迟测试
- 订阅管理
- 配置编辑
- 规则查看
- 连接查看
- 日志查看

#### ❌ 未实现（可选）
- 代理链模式
- 搜索和过滤功能
- 连接关闭功能
- DNS配置UI
- 语言切换

## 💡 最佳实践

### 1. 订阅管理
- 定期更新订阅（24小时）
- 保留备用配置
- 使用Merge配置自定义规则

### 2. 节点选择
- 使用延迟测试选择最快节点
- 规则模式下自动选择
- 全局模式用于特定需求

### 3. 性能优化
- 及时清理连接
- 定期查看日志排查问题
- 避免频繁切换节点

## 🆘 获取帮助

### 日志收集
```powershell
# 完整日志
adb logcat -d > clash_verge_full.log

# 核心日志
adb logcat -d | Select-String "clash_verge|Mihomo|Proxy|VPN" > clash_verge_core.log
```

### 重置应用
```powershell
# 清除数据
adb shell pm clear io.github.clash_verge_rev.clash_verge_rev.debug

# 重新安装
cd C:\Users\Administrator\Desktop\clash-verge-rev\mobile
.\gradlew.bat assembleDebug installDebug
```


