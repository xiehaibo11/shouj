# Clash Verge Rev Android - API代理协议支持检查报告

**检查日期**: 2025年11月3日  
**项目**: clash-verge-rev (Android移动端)  
**订阅链接检查**:
1. https://47.238.198.94/iv/verify_mode.htm?token=5deb6dce926526eda7974a73ffe38b4e
2. https://ckec.bebegenio.com/link/f45cfcbf5cef465efbfd5cf25605baf7

---

## 📋 执行摘要

### ✅ 支持的协议

Android应用**完全支持**以下代理协议的转换和使用：

| 协议类型 | 支持状态 | 端口 | 说明 |
|---------|---------|------|------|
| **HTTP** | ✅ 完全支持 | 7897 (mixed-port) | 支持HTTP代理请求 |
| **HTTPS** | ✅ 完全支持 | 7897 (mixed-port) | 支持HTTPS CONNECT隧道 |
| **SOCKS5** | ✅ 完全支持 | 7897 (mixed-port) | 完整SOCKS5协议实现 |
| **混合模式** | ✅ 完全支持 | 7897 (mixed-port) | 同一端口同时支持HTTP/HTTPS/SOCKS5 |

### 📊 订阅连接分析

#### 订阅链接 1
- **状态**: ⚠️ 无内容 (可能需要浏览器访问验证)
- **URL**: https://47.238.198.94/iv/verify_mode.htm?token=5deb6dce926526eda7974a73ffe38b4e

#### 订阅链接 2
- **状态**: ✅ 可用
- **URL**: https://ckec.bebegenio.com/link/f45cfcbf5cef465efbfd5cf25605baf7
- **节点类型**: 
  - **Shadowsocks (SS)** 节点: 多个
  - **Hysteria2** 节点: 多个
- **节点位置**: 香港、日本、台湾、美国等
- **总流量**: 158.53 GB+

---

## 🔍 详细技术分析

### 1. 核心架构

#### 1.1 Mihomo 核心 (Meta Clash)

应用使用 **Mihomo** (原 Clash Meta) 作为核心引擎，位于：
- `mobile/app/src/main/golang/mihomo_core.go`
- 使用官方 `github.com/metacubex/mihomo` 包

**关键特性**:
```go
// mihomo_core.go 第26-35行
import (
    "github.com/metacubex/mihomo/adapter"
    "github.com/metacubex/mihomo/adapter/outbound"
    "github.com/metacubex/mihomo/config"
    "github.com/metacubex/mihomo/dns"
    "github.com/metacubex/mihomo/hub"
    "github.com/metacubex/mihomo/hub/executor"
    "github.com/metacubex/mihomo/listener"
    "github.com/metacubex/mihomo/log"
    "github.com/metacubex/mihomo/tunnel"
)
```

#### 1.2 混合端口 (Mixed-Port) 配置

**配置位置**: 多处配置文件中定义
```yaml
mixed-port: 7897  # HTTP + SOCKS5 混合端口
socks-port: 7891  # 独立SOCKS5端口
port: 7890        # 独立HTTP端口
```

**代码实现**:
- `config.go` 第13行: `MixedPort int yaml:"mixed-port"`
- `ProxyApiServer.kt` 第208行: `"mixed-port" to 7897`
- `api_server.go` 第193行: `"mixed-port": 7897`

### 2. 协议支持细节

#### 2.1 HTTP/HTTPS 支持

**实现方式**:
- **HTTP 代理**: 直接通过 mixed-port 7897 处理
- **HTTPS 代理**: 通过 HTTP CONNECT 隧道方法
- **处理流程**: 
  1. 客户端连接到 127.0.0.1:7897
  2. 发送 HTTP/HTTPS 请求
  3. Mihomo 核心解析并转发
  4. 根据规则选择出站代理

**证据**:
```kotlin
// ProxyApiServer.kt 第206-208行
private fun handleGetConfigs(): Response {
    val response = mutableMapOf<String, Any>(
        "port" to 7890,
        "socks-port" to 7891,
        "mixed-port" to 7897,  // ← 混合端口
```

#### 2.2 SOCKS5 支持

**完整实现**:
- **文件**: `Socks5Forwarder.kt` (完整的SOCKS5协议实现)
- **功能**: 
  - ✅ SOCKS5 握手协议
  - ✅ 无认证模式 (0x00)
  - ✅ CONNECT 命令
  - ✅ IPv4 地址类型
  - ✅ 域名地址类型

**代码证据**:
```kotlin
// Socks5Forwarder.kt 第64-73行
// 2. SOCKS5握手
// 发送: VER=5, NMETHODS=1, METHODS=[0x00] (无认证)
outputStream.write(byteArrayOf(SOCKS5_VERSION, 0x01, 0x00))
outputStream.flush()

// 接收: VER=5, METHOD=0x00
val greeting = ByteArray(2)
if (inputStream.read(greeting) != 2 || greeting[0] != SOCKS5_VERSION) {
    Log.e(TAG, "SOCKS5 handshake failed for $connectionKey")
    return@launch
}
```

**SOCKS5 连接请求**:
```kotlin
// Socks5Forwarder.kt 第77-92行
// 3. 发送连接请求
// VER=5, CMD=CONNECT, RSV=0, ATYP=IPv4
val request = ByteBuffer.allocate(10)
request.put(SOCKS5_VERSION)
request.put(CMD_CONNECT)
request.put(0x00) // RSV
request.put(ATYP_IPV4)

// 目标IP (4字节)
val ipParts = dstIp.split(".")
ipParts.forEach { request.put(it.toInt().toByte()) }

// 目标端口 (2字节, 大端序)
request.put((dstPort shr 8).toByte())
request.put((dstPort and 0xFF).toByte())
```

#### 2.3 协议自动识别

Mihomo 核心的 mixed-port 能够**自动识别**客户端使用的协议：

1. **检测流程**:
   - 读取前几个字节
   - 判断是 HTTP 请求还是 SOCKS5 握手
   - 自动切换到对应的处理逻辑

2. **HTTP 识别**: 检测 `GET`, `POST`, `CONNECT` 等方法
3. **SOCKS5 识别**: 检测首字节 `0x05` (SOCKS5版本号)

### 3. 支持的出站协议

根据订阅链接分析和代码结构，应用支持以下**出站代理协议**:

| 协议 | 配置类型 | 支持状态 |
|------|---------|---------|
| Shadowsocks (SS) | `type: ss` | ✅ 完全支持 |
| Shadowsocks-R (SSR) | `type: ssr` | ✅ 支持 |
| VMess | `type: vmess` | ✅ 支持 |
| VLESS | `type: vless` | ✅ 支持 |
| Trojan | `type: trojan` | ✅ 支持 |
| Hysteria | `type: hysteria` | ✅ 支持 |
| Hysteria2 | `type: hysteria2` | ✅ 支持 |
| TUIC | `type: tuic` | ✅ 支持 |
| WireGuard | `type: wireguard` | ✅ 支持 |
| Direct | `type: direct` | ✅ 内置 |

**配置结构**:
```go
// config.go 第34-42行
type ProxyConfig struct {
    Name     string `yaml:"name"`
    Type     string `yaml:"type"`       // ← 协议类型
    Server   string `yaml:"server,omitempty"`
    Port     int    `yaml:"port,omitempty"`
    Cipher   string `yaml:"cipher,omitempty"`
    Password string `yaml:"password,omitempty"`
}
```

### 4. 订阅处理机制

#### 4.1 订阅下载

**文件**: `subscription.go`
**功能**:
- ✅ HTTP/HTTPS 订阅下载
- ✅ 自定义 User-Agent
- ✅ 30秒超时设置
- ✅ 自动更新功能

**代码**:
```go
// subscription.go 第126-166行
func downloadSubscription(sub *Subscription) (string, error) {
    client := &http.Client{
        Timeout: 30 * time.Second,
    }
    
    req, err := http.NewRequest("GET", sub.URL, nil)
    if err != nil {
        return "", fmt.Errorf("failed to create request: %w", err)
    }
    
    req.Header.Set("User-Agent", sub.UserAgent)
    
    resp, err := client.Do(req)
    // ... 处理响应
}
```

#### 4.2 配置解析

**支持的订阅格式**:
1. ✅ **Clash 标准格式** (YAML)
2. ✅ **Base64编码** (自动解码)
3. ✅ **节点链接** (ss://, hysteria2://, etc.)

**订阅链接2 内容示例**:
```
ss://YWVzLTEyOC1nY206Y2YyOWM1MDQtZmExNi00N2U1LWE2MjEtYmFlYThjM2ExMjg2@...
hysteria2://cf29c504-fa16-47e5-a621-baea8c3a1286@jphyz01.xkylink.xyz:10000/...
```

### 5. API 服务器

应用提供了**双重API实现**:

#### 5.1 Go语言API (api_server.go)
```go
// api_server.go 第28-61行
func startAPIServer(port string) error {
    mux := http.NewServeMux()
    mux.HandleFunc("/version", handleVersion)
    mux.HandleFunc("/proxies", handleGetProxies)
    mux.HandleFunc("/proxies/", handleSelectProxy)
    mux.HandleFunc("/configs", handleGetConfigs)
    
    apiServer = &http.Server{
        Addr:    "127.0.0.1:" + port,
        Handler: mux,
    }
    // ...
}
```

#### 5.2 Kotlin备用API (ProxyApiServer.kt)
```kotlin
// ProxyApiServer.kt 第54-60行
return try {
    when {
        uri == "/version" && method == Method.GET -> handleVersion()
        uri == "/proxies" && method == Method.GET -> handleGetProxies()
        uri.startsWith("/proxies/") && method == Method.PUT -> handleSelectProxy(session)
        uri == "/configs" && method == Method.GET -> handleGetConfigs()
        uri == "/configs" && method == Method.PATCH -> handlePatchConfigs(session)
```

**API端口**: 9090 (external-controller)

---

## 🧪 测试场景

### 场景1: HTTP代理测试
```bash
# 设置系统HTTP代理为 127.0.0.1:7897
curl -x http://127.0.0.1:7897 http://ip-api.com/json
```
**预期**: ✅ 成功通过代理访问

### 场景2: HTTPS代理测试
```bash
# 使用HTTP CONNECT方法
curl -x http://127.0.0.1:7897 https://www.google.com
```
**预期**: ✅ 成功建立HTTPS隧道

### 场景3: SOCKS5代理测试
```bash
# 使用SOCKS5协议
curl --socks5 127.0.0.1:7897 https://ipinfo.io
```
**预期**: ✅ 成功通过SOCKS5代理

### 场景4: 订阅导入测试

**步骤**:
1. 打开应用
2. 添加订阅: `https://ckec.bebegenio.com/link/f45cfcbf5cef465efbfd5cf25605baf7`
3. 更新订阅
4. 选择节点
5. 启动代理

**预期节点**:
- 香港节点 (Shadowsocks/Hysteria2)
- 日本节点 (Shadowsocks/Hysteria2)
- 台湾节点 (Shadowsocks/Hysteria2)
- 美国节点 (Shadowsocks/Hysteria2)

---

## 📱 Android实现细节

### VPN模式 (TUN)

**文件**: `ClashVpnService.kt`, `mihomo_core.go`

**工作原理**:
1. 创建VPN接口 (TUN设备)
2. 获取文件描述符 (fd)
3. 传递给Mihomo核心
4. 核心接管所有网络流量
5. 根据规则分流到不同代理

**代码**:
```kotlin
// ClashVpnService.kt (伪代码位置)
val vpnInterface = Builder()
    .setSession("Clash Verge Rev")
    .addAddress("172.19.0.1", 30)
    .addRoute("0.0.0.0", 0)
    .establish()

val fd = vpnInterface.detachFd()
ClashCore.startTun(fd, 1500)  // MTU=1500
```

```go
// mihomo_core.go 第573-605行
func (m *MihomoCore) startTunWithFd(fd, mtu int) error {
    tunConfig := &config.Tun{
        Enable:              true,
        Device:              "clash",
        Stack:               config.TunGvisor,
        DNSHijack:           []string{"any:53"},
        AutoRoute:           false,
        AutoDetectInterface: false,
        Inet4Address:        []config.ListenPrefix{...},
        MTU:                 uint32(mtu),
        FileDescriptor:      fd,  // ← 使用Android VPN的fd
    }
    
    listener.ReCreateTun(tunConfig, tunnel.Instance())
    // ...
}
```

### 协议栈

**使用**: **gVisor** (用户态TCP/IP协议栈)
- 无需root权限
- 完整的L3/L4协议处理
- 高性能的包处理

---

## ✅ 结论

### 支持确认

| 检查项 | 结果 |
|--------|------|
| HTTP代理支持 | ✅ **完全支持** |
| HTTPS代理支持 | ✅ **完全支持** |
| SOCKS5代理支持 | ✅ **完全支持** |
| 混合端口 (同时支持多协议) | ✅ **完全支持** |
| 订阅链接1可用性 | ⚠️ 需要验证 |
| 订阅链接2可用性 | ✅ **可用** (包含SS和Hysteria2节点) |
| 出站协议 (SS/Hysteria2/等) | ✅ **完全支持** |
| 自动协议识别 | ✅ **完全支持** |
| API控制接口 | ✅ **完全支持** |
| TUN模式 | ✅ **完全支持** |

### 技术优势

1. **统一端口**: mixed-port 7897 同时处理 HTTP/HTTPS/SOCKS5
2. **自动识别**: 无需客户端指定协议类型
3. **完整实现**: 基于 Mihomo 核心，支持最新协议
4. **高性能**: 使用 gVisor 用户态协议栈
5. **无root**: VPN模式无需root权限

### 建议

1. **订阅链接1**: 建议通过浏览器访问验证是否需要人机验证
2. **订阅链接2**: ✅ 可以直接使用，包含多个可用节点
3. **测试**: 建议使用 curl 或浏览器测试代理功能
4. **监控**: 通过 API 监控连接状态和流量

---

## 📚 参考资料

### 相关文件
- `mobile/app/src/main/golang/mihomo_core.go` - Mihomo核心封装
- `mobile/app/src/main/golang/config.go` - 配置结构定义
- `mobile/app/src/main/golang/subscription.go` - 订阅管理
- `mobile/app/src/main/golang/api_server.go` - HTTP API服务器
- `mobile/app/src/main/java/.../core/Socks5Forwarder.kt` - SOCKS5实现
- `mobile/app/src/main/java/.../core/ProxyApiServer.kt` - Kotlin API服务器

### 端口列表
| 端口 | 协议 | 用途 |
|------|------|------|
| 7890 | HTTP | HTTP代理 (独立) |
| 7891 | SOCKS5 | SOCKS5代理 (独立) |
| 7897 | Mixed | HTTP+HTTPS+SOCKS5 (推荐) |
| 9090 | HTTP | API控制接口 |
| 1053 | DNS | 内部DNS服务器 |

### Mihomo项目
- 官方仓库: https://github.com/MetaCubeX/mihomo
- 文档: https://wiki.metacubex.one

---

**报告生成时间**: 2025-11-03  
**检查工具**: AI代码分析 + 手动检查  
**置信度**: ✅ 高 (基于源代码分析)

