# DNS 修复测试指南

## ⚠️ 当前状态

### 已完成
- ✅ Kotlin 层：DNS 配置 `listen: 127.0.0.1:1053`
- ✅ Kotlin 层：DoH nameserver（https://1.1.1.1/dns-query）
- ✅ Go 代码：DNSHijack 修改为 `any:1053`

### 未完成
- ❌ Go 核心重新编译：`libclash.so` 仍使用旧的 `any:53` 配置

**结果**：Kotlin 层 DNS 配置已优化，但 Go 层 DNS 劫持可能仍有问题。

---

## 📝 测试步骤

### 1️⃣ 重新导入订阅
1. 打开 Clash Verge Rev
2. 删除现有配置（如果有）
3. 重新导入订阅 URL
4. **观察日志**：应该看到 "Adding DNS configuration for TUN mode (Android optimized)"

### 2️⃣ 开启 TUN 模式
1. 点击 TUN 模式开关
2. 授权 VPN 权限（如果需要）
3. **观察通知栏**：应该显示 VPN 已连接

### 3️⃣ 测试网页访问
1. 打开浏览器
2. 访问 `https://www.google.com`
3. **预期结果**：
   - ✅ 成功：页面加载 → DNS 修复完全生效
   - ⚠️ 部分成功：页面加载但很慢 → Kotlin 层生效，Go 层需要重编译
   - ❌ 失败：ERR_NAME_NOT_RESOLVED → 需要完整重编译 Go 核心

---

## 🔍 日志监控

### 实时监控命令

```bash
adb logcat -s "ClashCore-JNI:*" "ClashVpnService:*" "ProfileManager:*" | grep -E "DNS|TUN|1053|fake-ip"
```

### 预期日志（成功情况）

```
✅ ProfileManager: Adding DNS configuration for TUN mode (Android optimized)
✅ ClashVpnService: Loading configuration (Kotlin mode)...
✅ ClashCore-JNI: Starting Mihomo TUN: fd=XX, mtu=9000
✅ ClashCore-JNI: Mihomo TUN started successfully
✅ ClashVpnService: HTTP API available
✅ ClashVpnService: Mihomo version: Clash.Meta vX.X.X

# DNS 查询日志（如果完全成功）
✅ [DNS] Mihomo DNS listening at 127.0.0.1:1053
✅ [DNS] query www.google.com -> 198.18.x.x (fake-ip)
✅ [TUN] new connection TCP 198.18.x.x:443
```

### 问题日志（需要重编译）

```
⚠️ [DNS] failed to hijack DNS on port 53 (permission denied)
⚠️ [DNS] DNS queries not intercepted
```

---

## 🔧 如果测试失败：完整重编译 Go 核心

### 方法 1：Linux/macOS 编译

```bash
# 1. 进入 Go 源码目录
cd mobile/app/src/main/golang

# 2. 安装依赖
go mod download

# 3. 设置环境变量
export ANDROID_NDK_HOME="/path/to/ndk/25.2.9519653"

# 4. 编译 x86_64（模拟器）
export GOOS=android
export GOARCH=amd64
export CGO_ENABLED=1
export CC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android21-clang"

go build -buildmode=c-shared \
  -ldflags="-s -w -extldflags=-Wl,-soname,libclash.so" \
  -tags="with_gvisor" \
  -trimpath \
  -o ../jniLibs/x86_64/libclash.so \
  .

# 5. 删除不需要的头文件
rm -f ../jniLibs/x86_64/libclash.h

# 6. 重新编译 APK
cd ../../../../
./gradlew assembleDebug

# 7. 安装测试
adb install -r app/build/outputs/apk/debug/app-x86_64-debug.apk
```

### 方法 2：Windows + WSL

```powershell
# 在 WSL 中执行上述 Linux 命令
wsl
cd /mnt/c/Users/Administrator/Desktop/clash-verge-rev/mobile
# 然后执行方法 1 的命令
```

### 方法 3：使用编译脚本

```bash
cd mobile/scripts
./build-go.sh
# 选择 3 (x86_64 - Emulators)
```

---

## 📊 测试结果对照表

| 现象 | 原因 | 下一步 |
|------|------|--------|
| ✅ 网页正常访问 | DNS 完全修复 | 无需操作，修复成功！ |
| ⚠️ 网页加载很慢 | Kotlin 层生效，Go 层部分问题 | 建议重编译 Go 核心 |
| ❌ ERR_NAME_NOT_RESOLVED | DNS 劫持未生效 | **必须重编译 Go 核心** |
| ❌ ERR_PROXY_CONNECTION_FAILED | 代理节点问题 | 检查节点配置 |

---

## 🎯 完整修复的标志

当完全修复后，配置文件应该包含：

```yaml
dns:
  enable: true
  listen: 127.0.0.1:1053  # ✅ Kotlin 层已修复
  enhanced-mode: fake-ip
  fake-ip-range: 198.18.0.1/16
  nameserver:
    - https://1.1.1.1/dns-query
    - https://8.8.8.8/dns-query
  fallback:
    - https://dns.alidns.com/dns-query
    - https://doh.pub/dns-query
```

Go 核心日志应该显示：

```
[TUN] DNSHijack: any:1053  # ✅ Go 层需要重编译
```

---

## ✅ 验证清单

- [ ] 删除旧配置
- [ ] 重新导入订阅
- [ ] 开启 TUN 模式
- [ ] 查看 VPN 已连接
- [ ] 访问 www.google.com
- [ ] 检查日志中的 DNS 相关信息
- [ ] 如失败：重新编译 Go 核心
- [ ] 再次测试

---

**测试时间**: 待测试  
**预期结果**: ⏳ 部分生效（Kotlin 层）或 完全生效  
**需要完整修复**: 🔴 是（需要重编译 Go 核心）

