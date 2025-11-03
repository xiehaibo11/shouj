# 模拟器测试指南

## 📱 当前状态

✅ **应用已安装**: Clash Verge Rev Android (x86_64)  
✅ **测试配置已推送**: `/sdcard/Download/clash-config.yaml`  
✅ **Mihomo 核心已集成**: libclash.so (34MB)

---

## 🧪 测试步骤

### 1. 启动应用

在模拟器中找到并点击 **Clash Verge Rev** 图标启动应用。

### 2. 导入配置文件

1. 在应用中进入"配置"或"Profiles"页面
2. 点击"导入配置"或"Import"按钮
3. 选择文件管理器中的 `/sdcard/Download/clash-config.yaml`
4. 等待配置加载完成

### 3. 查看代理节点

1. 进入"代理"或"Proxies"页面
2. 应该能看到以下代理组：
   - **PROXY** (select 类型)
   - **Auto** (url-test 类型)
   - **Fallback** (fallback 类型)
3. 每个代理组中应该包含 **DIRECT** 节点

### 4. 测试核心功能

#### A. 获取代理列表（测试 getProxies）
- 查看代理页面是否正确显示所有代理组
- 检查每个代理组的类型和节点列表

#### B. 切换代理节点（测试 selectProxy）
1. 点击 **PROXY** 代理组
2. 选择 **DIRECT** 节点
3. 观察是否切换成功（有提示或状态变化）

#### C. 测试延迟（测试 testProxyDelay）
1. 点击节点旁边的"测速"图标
2. 观察是否显示延迟时间（DIRECT 应该显示很低的延迟，如 1-10ms）

#### D. 查看规则（测试 getRules）
1. 进入"规则"或"Rules"页面
2. 应该能看到配置文件中定义的规则：
   - DOMAIN-SUFFIX,google.com,PROXY
   - DOMAIN-SUFFIX,github.com,PROXY
   - GEOIP,CN,DIRECT
   - 等等

#### E. 启动 TUN 模式（测试完整代理功能）
1. 返回主页面
2. 点击"启动"或"Start"按钮
3. 授予 VPN 权限（首次会弹出系统对话框）
4. 等待连接成功（状态显示"已连接"或"Connected"）
5. 检查通知栏是否显示 VPN 图标

#### F. 查看连接（测试 getConnections）
1. 进入"连接"或"Connections"页面
2. 打开浏览器访问一些网站（如 baidu.com, google.com）
3. 返回应用，应该能看到实时连接列表
4. 观察流量统计（上传/下载）

#### G. 查看日志（测试 getLogs）
1. 进入"日志"或"Logs"页面
2. 应该能看到 Mihomo 核心的实时日志输出
3. 观察日志内容是否包含：
   - 配置加载信息
   - 代理切换信息
   - 连接建立信息

---

## 🔍 日志监控

在 PowerShell 中运行以下命令查看实时日志：

\`\`\`powershell
# 查看 Mihomo 核心日志
adb logcat -s ClashCore-Go:I ClashCore:I MihomoAPI:I

# 查看完整应用日志
adb logcat -s ClashVergeRev:D ClashCore-Go:I ClashCore:I

# 查看崩溃日志
adb logcat -s AndroidRuntime:E
\`\`\`

---

## ✅ 验证清单

测试完成后，验证以下功能：

- [ ] 应用成功启动，无崩溃
- [ ] 配置文件成功导入
- [ ] 代理列表正确显示（PROXY、Auto、Fallback）
- [ ] 代理节点显示正确（DIRECT）
- [ ] 可以切换代理节点
- [ ] 延迟测试功能正常
- [ ] 规则列表正确显示
- [ ] TUN 模式可以启动（VPN 权限）
- [ ] 连接列表可以显示
- [ ] 流量统计正确
- [ ] 日志正常输出
- [ ] 可以停止 TUN 模式

---

## 🐛 常见问题

### 问题 1: 应用启动后闪退

**解决方案**:
\`\`\`powershell
# 查看崩溃日志
adb logcat -d | Select-String "FATAL"

# 清除应用数据重试
adb shell pm clear io.github.clash_verge_rev.clash_verge_rev
\`\`\`

### 问题 2: 配置加载失败

**可能原因**:
- 配置文件格式错误
- libclash.so 加载失败
- external-controller 未配置

**解决方案**:
\`\`\`powershell
# 查看核心日志
adb logcat -s ClashCore-Go:I

# 检查配置文件
adb shell cat /sdcard/Download/clash-config.yaml
\`\`\`

### 问题 3: 代理列表为空

**可能原因**:
- Mihomo 核心未初始化
- 配置未成功加载
- getProxies() 调用失败

**解决方案**:
\`\`\`powershell
# 查看 Mihomo 日志
adb logcat -s ClashCore-Go:I | Select-String "getProxies"

# 重启应用
adb shell am force-stop io.github.clash_verge_rev.clash_verge_rev
adb shell monkey -p io.github.clash_verge_rev.clash_verge_rev 1
\`\`\`

### 问题 4: TUN 模式启动失败

**可能原因**:
- VPN 权限未授予
- TUN 设备创建失败
- Mihomo TUN 配置错误

**解决方案**:
\`\`\`powershell
# 查看 TUN 相关日志
adb logcat | Select-String "TUN"

# 检查 VPN 状态
adb shell dumpsys connectivity | Select-String "VPN"
\`\`\`

---

## 📊 预期结果

成功集成后，应该看到：

### 日志输出示例

\`\`\`
I/ClashCore-Go: Initializing Mihomo core...
I/ClashCore-Go: Mihomo components initialized
I/ClashCore-Go: ✅ Mihomo HTTP API will be available after config is loaded
I/ClashCore-Go: Loading config from: /data/user/0/.../files/config.yaml
I/ClashCore-Go: Config reloaded successfully
I/ClashCore-Go: ✅ Mihomo config reloaded, API server started
I/ClashCore-Go: Proxy DIRECT delay: 5ms
I/ClashCore-Go: ✓ Successfully selected proxy: PROXY -> DIRECT
\`\`\`

### UI 显示

- **代理页面**: 显示 PROXY、Auto、Fallback 三个代理组
- **节点列表**: 每个组显示 DIRECT 节点，延迟 < 10ms
- **规则页面**: 显示约 8 条规则
- **连接页面**: 启动 TUN 后显示活动连接
- **日志页面**: 实时显示 INFO 级别日志

---

## 🎯 下一步测试（添加真实代理）

如果基础功能测试通过，可以编辑配置文件添加真实代理节点：

\`\`\`yaml
proxies:
  - name: "香港-01"
    type: ss
    server: your-server.com
    port: 8388
    cipher: aes-256-gcm
    password: your-password
  
proxy-groups:
  - name: "PROXY"
    type: select
    proxies:
      - "香港-01"
      - DIRECT
\`\`\`

然后重新导入配置进行完整的代理测试。

---

**测试完成后请截图或记录日志输出！** 📸

