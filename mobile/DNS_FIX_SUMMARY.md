# DNS 劫持修复总结

## 🎯 问题诊断

**症状**：ERR_NAME_NOT_RESOLVED - DNS 解析失败

**根本原因**：
1. ❌ Mihomo 配置监听 `0.0.0.0:53` 端口
2. ❌ DNSHijack 设置为 `any:53`
3. ⚠️ **Android 非 root 应用无法绑定 53 端口**
4. 结果：DNS 请求未被 Mihomo 接管，fake-IP 模式失效

---

## ✅ 修复方案

按照 Mihomo Android 最佳实践，修改为：

### 1️⃣ Go 层 DNS 劫持端口（mihomo_core.go:588）

```go
// ❌ 修复前
DNSHijack: []string{"any:53"},

// ✅ 修复后
DNSHijack: []string{"any:1053"},  // Android 非 root 无法绑定 53
```

### 2️⃣ DNS 监听配置（ProfileManager.kt）

#### importSubscription 方法（第 82-106 行）

```yaml
# ❌ 修复前
dns:
  enable: true
  listen: 0.0.0.0:53
  enhanced-mode: fake-ip
  nameserver:
    - 8.8.8.8
    - 1.1.1.1
  fallback:
    - tls://8.8.4.4:853

# ✅ 修复后
dns:
  enable: true
  listen: 127.0.0.1:1053  # ← 关键修改
  enhanced-mode: fake-ip
  fake-ip-range: 198.18.0.1/16
  nameserver:
    - https://1.1.1.1/dns-query  # DoH (DNS over HTTPS)
    - https://8.8.8.8/dns-query
  fallback:
    - https://dns.alidns.com/dns-query  # 国内 DoH
    - https://doh.pub/dns-query
  fallback-filter:
    geoip: true
    ipcidr:
      - 240.0.0.0/4
```

#### updateSubscription 方法（第 566-590 行）

同样的修改。

---

## 🔄 配置协同

修复后的完整流程：

```
1. Android VPN 路由所有流量到 tun0
   ↓
2. TUN 接口（fd）传递给 Mihomo Go 核心
   ↓
3. gVisor 用户态网络栈处理数据包
   ↓
4. DNS 劫持：any:1053 拦截所有 DNS 查询（53 → 1053）
   ↓
5. Mihomo 监听 127.0.0.1:1053
   ↓
6. fake-IP 模式：github.com → 198.18.x.x
   ↓
7. 规则引擎匹配
   ↓
8. 代理节点转发
```

---

## 🆚 对比表

| 配置项 | 修复前 | 修复后 | 说明 |
|--------|--------|--------|------|
| **DNSHijack** | `any:53` | `any:1053` | ✅ Android 兼容 |
| **DNS Listen** | `0.0.0.0:53` | `127.0.0.1:1053` | ✅ 避免权限问题 |
| **Nameserver** | 明文 DNS | DoH (HTTPS) | ✅ 加密、防污染 |
| **Fallback** | TLS DNS | 国内 DoH | ✅ 兼容性更好 |
| **fake-IP** | 198.18.0.1/16 | 198.18.0.1/16 | ✅ 保持不变 |

---

## 📊 DoH (DNS over HTTPS) 优势

修复后使用的 DoH 服务器：

| DoH 服务器 | 提供商 | 用途 | 特点 |
|-----------|--------|------|------|
| `https://1.1.1.1/dns-query` | Cloudflare | 主用 | ✅ 快速、隐私友好 |
| `https://8.8.8.8/dns-query` | Google | 主用 | ✅ 稳定、权威 |
| `https://dns.alidns.com/dns-query` | 阿里云 | Fallback | ✅ 国内访问快 |
| `https://doh.pub/dns-query` | DNSPod | Fallback | ✅ 国内备用 |

**优势**：
- ✅ 加密传输，防止 DNS 劫持和污染
- ✅ HTTPS 协议，伪装成普通网页流量
- ✅ 避免 53 端口被 ISP 拦截
- ✅ 支持 EDNS Client Subnet（ECS）
- ✅ 不需要特殊权限

---

## 🧪 验证方法

### 编译后测试

```bash
# 1. 重新编译 Go 核心（必须！DNSHijack 改变需要重新编译）
cd mobile/app/src/main/golang
GOOS=android GOARCH=arm64 CGO_ENABLED=1 \
  go build -buildmode=c-shared -tags="with_gvisor" \
  -o ../jniLibs/arm64-v8a/libclash.so

# 2. 编译 APK
cd ../../..
./gradlew assembleDebug

# 3. 安装测试
adb install -r app/build/outputs/apk/debug/app-x86_64-debug.apk
```

### 日志验证

启动 TUN 模式后，查看日志：

```bash
adb logcat -s ClashCore-JNI:* ClashVpnService:* | grep -E "DNS|TUN|fake-ip"
```

**预期输出**：

```
✅ [DNS] Mihomo DNS server listening at 127.0.0.1:1053
✅ [TUN] DNSHijack enabled: any:1053
✅ [DNS] query github.com -> 198.18.x.x (fake-ip)
✅ [TUN] new connection TCP 198.18.x.x:443 -> 20.205.243.166:443
✅ [Rule] github.com => PROXY (via 节点名称)
```

### 浏览器测试

1. 开启 TUN 模式
2. 打开浏览器
3. 访问 `https://www.google.com`
4. **预期**：✅ 页面正常加载

### 命令行测试

```bash
# 在模拟器/设备中执行
adb shell

# 测试 DNS 解析
nslookup github.com 127.0.0.1 -port=1053

# 测试网络连接
curl -v https://www.google.com
```

---

## ⚠️ 常见问题

### Q1: 修改后还是无法解析 DNS？

**A**: 确保重新编译了 Go 核心（`libclash.so`），因为 `DNSHijack` 是编译时配置。

```bash
# 验证方法
adb shell "cat /proc/$(pgrep -f clash_verge_rev)/maps | grep libclash"
# 检查 libclash.so 的时间戳是否是最新的
```

### Q2: DoH 查询太慢？

**A**: 可以调整 nameserver 顺序，或使用国内 DoH 作为主服务器：

```yaml
nameserver:
  - https://dns.alidns.com/dns-query  # 国内优先
  - https://1.1.1.1/dns-query
```

### Q3: 部分应用还是无法访问？

**A**: 检查规则配置，确保有 fallback 规则：

```yaml
rules:
  - DOMAIN-SUFFIX,cn,DIRECT
  - GEOIP,CN,DIRECT
  - MATCH,PROXY  # ← 必须有默认规则
```

---

## 📚 参考资料

### Mihomo 文档
- [TUN 模式配置](https://wiki.metacubex.one/config/inbound/tun/)
- [DNS 配置](https://wiki.metacubex.one/config/dns/)
- [fake-IP 模式](https://wiki.metacubex.one/config/dns/fake-ip/)

### Android 限制
- [VpnService API](https://developer.android.com/reference/android/net/VpnService)
- Android 非 root 应用无法绑定 1024 以下端口（包括 53）
- 需要使用 DNS 劫持 + 端口重定向

### DoH 标准
- [RFC 8484 - DNS Queries over HTTPS](https://datatracker.ietf.org/doc/html/rfc8484)
- [DoH Providers](https://github.com/curl/curl/wiki/DNS-over-HTTPS)

---

## ✅ 修复清单

- [x] Go 层 DNSHijack: any:53 → any:1053
- [x] DNS listen: 0.0.0.0:53 → 127.0.0.1:1053
- [x] Nameserver: 明文 DNS → DoH
- [x] Fallback: TLS DNS → 国内 DoH
- [x] 添加 fallback-filter 配置
- [ ] 重新编译 Go 核心
- [ ] 重新编译 APK
- [ ] 安装测试
- [ ] 验证 DNS 解析正常
- [ ] 验证网页访问正常

---

**修复时间**: 2025-11-04  
**预期效果**: ✅ 完全解决 ERR_NAME_NOT_RESOLVED 问题  
**测试状态**: ⏳ 待重新编译测试

