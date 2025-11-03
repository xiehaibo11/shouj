# TUN 模式配置验证报告

## ✅ 检查结果总结

所有关键配置均已正确设置，符合 Mihomo Android TUN 模式最佳实践。

---

## 📋 逐项检查

### 1️⃣ 权限配置（AndroidManifest.xml）

**检查项**：App 是否声明 `BIND_VPN_SERVICE` 权限

**状态**：✅ **已正确配置**

```xml
<!-- 第 14 行 -->
<uses-permission android:name="android.permission.BIND_VPN_SERVICE" />

<!-- 第 98-109 行：VPN Service 配置 -->
<service
    android:name=".service.ClashVpnService"
    android:exported="false"
    android:foregroundServiceType="specialUse"
    android:permission="android.permission.BIND_VPN_SERVICE">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
</service>
```

---

### 2️⃣ 路由配置（ClashVpnService.kt）

**检查项**：TUN 已建立但流量无法转发 → 路由是否添加

**状态**：✅ **已正确配置**

```kotlin
// ClashVpnService.kt:354
builder.addRoute("0.0.0.0", 0)  // ✅ 默认路由，所有流量进入 TUN
```

**配置详情**：
- `VPN_ADDRESS = "172.19.0.1"` - TUN 设备 IP
- `VPN_ROUTE = "0.0.0.0"` - 默认路由
- `VPN_MTU = 9000` - 最大传输单元

---

### 3️⃣ DNS 劫持配置（mihomo_core.go）

**检查项**：DNS 无效 → DNS 劫持是否启用

**状态**：✅ **已正确配置**

```go
// mihomo_core.go:588
tunConfig := &config.Tun{
    Enable:    true,
    Stack:     config.TunGvisor,
    DNSHijack: []string{"any:53"},  // ✅ 劫持所有 DNS 请求
    // ...
}
```

**说明**：
- `any:53` 会劫持所有 DNS 查询（UDP/TCP 53 端口）
- 配合配置文件中的 `enhanced-mode: fake-ip` 实现 DNS 分流

---

### 4️⃣ gVisor 支持（build-go.sh）

**检查项**：报错 `tun: device not found` → 是否链接 gVisor 支持

**状态**：✅ **已正确配置**

```bash
# build-go.sh:88
go build -buildmode=c-shared \
    -ldflags="-s -w -extldflags=-Wl,-soname,libclash.so" \
    -tags="with_gvisor" \   # ✅ 启用 gVisor 用户态网络栈
    -trimpath \
    -o "$OUTPUT_DIR/libclash.so"
```

**重要性**：
- gVisor 是 Google 开发的用户态网络栈
- Android TUN 模式必须使用 gVisor（无需 kernel TUN）
- 不带此标签编译会导致 `tun: device not found` 错误

---

### 5️⃣ FD 传递（ClashVpnService.kt → main.go）

**检查项**：日志无 TUN 输出 → fd 是否成功传递

**状态**：✅ **已正确配置**

**流程**：
1. **Android 层**（ClashVpnService.kt:102-114）
   ```kotlin
   vpnInterface = establishVpnInterface()  // 创建 VPN 接口
   val fd = vpnInterface!!.fd              // 获取文件描述符
   ClashCore.startTun(fd, VPN_MTU)         // 传递给 Go 层
   ```

2. **JNI 桥接**（main.go:181-214）
   ```go
   //export nativeStartTun
   func nativeStartTun(fd, mtu int) int {
       return globalCore.startTunWithFd(fd, mtu)
   }
   ```

3. **Mihomo 核心**（mihomo_core.go:573-605）
   ```go
   tunConfig := &config.Tun{
       FileDescriptor: fd,  // ✅ 使用 Android VPN 的 fd
       MTU:           uint32(mtu),
       // ...
   }
   listener.ReCreateTun(tunConfig, tunnel.Instance())
   ```

4. **FD 所有权管理**（ClashVpnService.kt:119-120）
   ```kotlin
   vpnInterface!!.detachFd()    // ✅ 转移所有权给 Go 层
   vpnInterface = null          // 避免重复关闭
   ```

---

## 🎯 修复历史

### 已修复的问题

| 问题 | 原因 | 修复方式 | 提交 |
|------|------|----------|------|
| ERR_PROXY_CONNECTION_FAILED | VPN 接口错误设置 HTTP 代理 | 移除 `setHttpProxy()` 调用 | 最新 |
| fdsan 错误崩溃 | 文件描述符重复关闭 | 使用 `detachFd()` 转移所有权 | 最新 |
| TUN 启动后立即停止 | 验证逻辑要求 proxy port 必须监听 | 将 proxy port 检查改为可选 | 最新 |
| DNS 配置缺失 | 订阅配置未包含 DNS | 自动注入完整 DNS 配置 | 最新 |
| 无有效代理 | TUN 启动时选中 DIRECT | 自动选择第一个非 DIRECT 代理 | 最新 |

---

## 🧪 测试验证

### 预期行为

当 TUN 模式正常工作时，应该看到：

1. **VPN 连接建立**
   ```
   ✅ VPN interface established (Mihomo Go mode)
   - VPN fd: 76 (ownership transferred to Go)
   - MTU: 9000
   - API Server: http://127.0.0.1:9090
   - TUN Handler: Mihomo Go Core ✅
   ```

2. **核心验证通过**
   ```
   ✅ HTTP API available
   ⚠️ Mixed proxy port (7897) not responding (OK in TUN mode)
   ✅ Mihomo version: Clash.Meta vX.X.X
   ```

3. **VPN 状态**（通过 `adb shell dumpsys connectivity`）
   ```
   NetworkAgentInfo{network{147} ni{VPN CONNECTED
     InterfaceName: tun0
     Routes: [ 0.0.0.0/0 -> 0.0.0.0 tun0 ]
     ✅ 无 HttpProxy 配置
   ```

4. **流量日志**（当访问网站时）
   ```
   [TUN] new connection TCP 10.0.2.15:54321 -> 142.250.185.78:443
   [TUN] DNS request: www.google.com A
   [Rule] www.google.com => PROXY (via 节点名称)
   ```

---

## 🔍 故障排查流程

如果 TUN 模式仍然无法工作，按以下顺序检查：

### Step 1: 检查日志中的错误

```bash
adb logcat -s ClashVpnService:I ClashCore:I AndroidRuntime:E libc:F
```

### Step 2: 验证 VPN 状态

```bash
adb shell dumpsys connectivity | grep -A 20 "VPN"
```

关键检查点：
- `ni{VPN CONNECTED}` - VPN 已连接
- `InterfaceName: tun0` - TUN 设备名称
- `Routes: [ 0.0.0.0/0 -> 0.0.0.0 tun0 ]` - 默认路由已添加
- **不应该有** `HttpProxy: [127.0.0.1] 7897`

### Step 3: 检查 libclash.so 是否加载

```bash
adb shell "ps -A | grep clash"
adb shell "cat /proc/$(pgrep -f clash_verge_rev)/maps | grep libclash"
```

### Step 4: 手动测试连接

```bash
# 测试 Mihomo API
adb shell "curl -s http://127.0.0.1:9090/version"

# 测试外网连接（通过 TUN）
adb shell "ping -c 4 8.8.8.8"
```

---

## 📚 参考文档

- [Mihomo TUN 模式文档](https://wiki.metacubex.one/config/inbound/tun/)
- [Android VpnService API](https://developer.android.com/reference/android/net/VpnService)
- [gVisor 网络栈](https://gvisor.dev/docs/user_guide/networking/)
- 项目文档：
  - `mobile/TUN_MODE_QUICK_START.md`
  - `mobile/PROXY_ERROR_FIX.md`
  - `mobile/MIHOMO_INTEGRATION_COMPLETE.md`

---

## ✅ 结论

**所有 TUN 模式关键配置均已正确设置，符合生产环境要求。**

当前配置已包含：
- ✅ 完整的 Android VPN 权限
- ✅ 正确的路由配置（0.0.0.0/0）
- ✅ DNS 劫持启用（any:53）
- ✅ gVisor 编译标签
- ✅ 文件描述符正确传递和所有权管理
- ✅ 无 HTTP 代理配置（透明代理）

如果测试时仍然遇到问题，请提供：
1. 完整的 logcat 日志（带时间戳）
2. `dumpsys connectivity` 输出
3. 配置文件内容（脱敏后）
4. 具体的错误现象（无法访问、部分访问、DNS 失败等）

---

**生成时间**: 2025-11-04
**验证状态**: ✅ 通过

