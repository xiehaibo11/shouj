# Clash Verge Mobile 架构设计

## 📐 整体架构

```
┌─────────────────────────────────────────────────────┐
│                  Mobile Frontend                     │
│              (React Native + TypeScript)             │
├─────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐│
│  │    Screens   │  │  Components  │  │   Hooks    ││
│  │  (HomeScreen)│  │  (ProxyList) │  │(useProxies)││
│  └──────────────┘  └──────────────┘  └────────────┘│
├─────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐│
│  │   Services   │  │    Utils     │  │   Themes   ││
│  │    (API)     │  │  (Formatters)│  │  (Styles)  ││
│  └──────────────┘  └──────────────┘  └────────────┘│
└─────────────────────────────────────────────────────┘
                         ↕ Tauri IPC
┌─────────────────────────────────────────────────────┐
│                  Backend (Rust)                      │
│                  [复用桌面版]                         │
├─────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐│
│  │ Core Manager │  │Config Manager│  │Profile Mgr ││
│  └──────────────┘  └──────────────┘  └────────────┘│
├─────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────┐  │
│  │          Mihomo Core (Go Binary)              │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                         ↕
┌─────────────────────────────────────────────────────┐
│                  Platform Layer                      │
├──────────────────────┬──────────────────────────────┤
│   Android Native     │      iOS Native              │
│  ┌────────────────┐  │  ┌────────────────────────┐ │
│  │ VPN Service    │  │  │ Network Extension      │ │
│  │ (ClashVpnSvc)  │  │  │ (PacketTunnelProvider) │ │
│  └────────────────┘  │  └────────────────────────┘ │
└──────────────────────┴──────────────────────────────┘
```

## 🧩 核心模块

### 1. 前端层 (React Native)

#### 导航系统
- **React Navigation**: 页面路由和导航栈管理
- **屏幕**: Home, Proxies, Profiles, Connections, Rules, Logs, Settings
- **深层链接**: 支持 `clash://` 和 `clash-verge://` URL Scheme

#### 状态管理
- **SWR**: 数据获取和缓存
- **Zustand**: 全局状态管理（VPN状态、设置等）
- **React Context**: 应用级数据共享

#### UI组件库
- **React Native Paper**: Material Design 3 组件
- **React Native Vector Icons**: 图标库
- **自定义组件**: ProxyCard, ConnectionItem, LogItem等

### 2. 服务层 (Services)

#### API服务 (`api.ts`)
```typescript
// Clash API调用
getAxios() → Axios实例
getProxies() → 代理列表
selectProxy() → 切换代理
getConnections() → 连接列表
getRules() → 规则列表
getIpInfo() → IP信息
```

#### 初始化服务 (`initialize.ts`)
```typescript
initializeApp() → 应用启动初始化
loadSettings() → 加载本地设置
setupEventListeners() → 事件监听
```

#### VPN服务 (`vpn.ts`)
```typescript
requestVpnPermission() → 请求VPN权限
startVpn() → 启动VPN
stopVpn() → 停止VPN
getVpnStatus() → VPN状态
```

### 3. Hooks层

| Hook | 功能 | 主要方法 |
|------|------|----------|
| `useProxies` | 代理管理 | `select()`, `mutate()` |
| `useProfiles` | 配置管理 | `selectProfile()`, `createProfile()` |
| `useConnections` | 连接监控 | `connections`, `mutate()` |
| `useRules` | 规则管理 | `rules`, `mutate()` |
| `useLogs` | 日志显示 | `logs`, `clearLogs()` |
| `useSettings` | 设置管理 | `updateSetting()`, `resetSettings()` |

### 4. 后端层 (Rust)

#### 核心模块 (复用桌面版)
```
src-tauri/src/
├── core/          # 核心逻辑
│   ├── manager/   # Mihomo进程管理
│   ├── handle.rs  # 应用句柄
│   ├── hotkey.rs  # 快捷键（移动端禁用）
│   └── service.rs # 服务管理
├── config/        # 配置管理
│   ├── clash.rs   # Clash配置
│   ├── profiles.rs# 配置文件
│   └── verge.rs   # Verge配置
├── cmd/           # Tauri命令
│   ├── app.rs     # 应用命令
│   ├── clash.rs   # Clash命令
│   └── profile.rs # 配置命令
└── enhance/       # 增强功能
    ├── merge.rs   # 配置合并
    └── script.rs  # 脚本执行
```

#### Tauri命令
```rust
// 移动端需要的核心命令
#[tauri::command]
pub async fn get_clash_info() -> Result<ClashInfo>
#[tauri::command]
pub async fn patch_clash_config(payload: Mapping) -> Result<()>
#[tauri::command]
pub async fn get_proxies() -> Result<ProxiesMapping>
#[tauri::command]
pub async fn select_proxy(group: String, name: String) -> Result<()>
#[tauri::command]
pub async fn get_profiles() -> Result<ProfilesConfig>
#[tauri::command]
pub async fn import_profile(url: String) -> Result<()>
```

### 5. 平台层 (Native)

#### Android VPN服务
```kotlin
// ClashVpnService.kt
class ClashVpnService : VpnService() {
    fun startVPN() {
        // 1. 建立VPN接口
        val builder = Builder()
            .setSession("Clash Verge")
            .addAddress("172.19.0.1", 30)
            .addRoute("0.0.0.0", 0)
        
        // 2. 启动Mihomo进程
        startProxyCore()
        
        // 3. 转发网络流量
        setupTrafficRouting()
    }
}
```

#### iOS Network Extension
```swift
// PacketTunnelProvider.swift
class PacketTunnelProvider: NEPacketTunnelProvider {
    override func startTunnel() {
        // 1. 配置网络设置
        let settings = NEPacketTunnelNetworkSettings()
        
        // 2. 启动Mihomo进程
        startProxyCore()
        
        // 3. 处理数据包
        handlePackets()
    }
}
```

## 🔄 数据流

### 代理切换流程
```
用户点击代理
    ↓
ProxyScreen.onSelect()
    ↓
useProxies.select(proxyName)
    ↓
api.selectProxy(group, proxyName)
    ↓
Tauri IPC: select_proxy
    ↓
Rust Backend: handle_select_proxy()
    ↓
Mihomo API: /proxies/{group}
    ↓
Clash Core更新代理
    ↓
SWR mutate() 刷新UI
```

### VPN启动流程
```
用户点击连接
    ↓
HomeScreen.onConnect()
    ↓
vpn.requestVpnPermission()
    ↓ (授权成功)
vpn.startVpn()
    ↓
Platform Native: startVpnService()
    ↓
建立VPN隧道
    ↓
Tauri IPC: start_clash_core
    ↓
Rust Backend: CoreManager.run()
    ↓
启动Mihomo进程
    ↓
开始转发流量
```

## 🔐 安全设计

### 1. 数据存储
- **敏感配置**: 使用平台加密存储（Keychain/KeyStore）
- **缓存数据**: 使用应用沙盒
- **日志文件**: 定期清理，限制大小

### 2. 权限管理
```typescript
// 运行时权限检查
const permissions = {
    android: ['BIND_VPN_SERVICE', 'FOREGROUND_SERVICE'],
    ios: ['NetworkExtension']
};
```

### 3. 网络安全
- TLS证书验证
- 防止DNS泄露
- WebRTC泄露防护

## 📱 移动端特有设计

### 1. 电池优化
- **后台任务**: 使用Workmanager（Android）/ Background Tasks（iOS）
- **网络轮询**: 根据电池状态调整频率
- **唤醒锁**: 仅在必要时持有

### 2. 流量统计
```typescript
interface TrafficStats {
    upload: number;
    download: number;
    connections: number;
}
```

### 3. 快捷操作
- **Android**: Quick Settings Tile
- **iOS**: Widget / Shortcuts

### 4. 通知
```typescript
// 连接状态通知
showNotification({
    title: 'Clash Verge',
    body: 'VPN已连接',
    ongoing: true,
    actions: ['断开', '切换节点']
});
```

## 🧪 测试策略

### 单元测试
- 工具函数测试
- Hook逻辑测试
- 状态管理测试

### 集成测试
- API调用测试
- VPN启动流程测试
- 配置导入导出测试

### E2E测试
- 完整用户流程测试
- 跨平台兼容性测试

## 🎨 UI/UX设计原则

1. **Material Design 3**: 遵循最新设计规范
2. **响应式布局**: 适配不同屏幕尺寸
3. **手势操作**: 下拉刷新、侧滑删除
4. **加载状态**: Skeleton加载、下拉刷新
5. **错误处理**: 友好的错误提示

## 📊 性能优化

### 1. 渲染优化
- 使用 `React.memo` 避免不必要的重渲染
- FlatList虚拟化列表
- 图片懒加载

### 2. 内存管理
- 及时清理事件监听器
- 控制日志缓存大小
- WebSocket连接复用

### 3. 启动优化
- 延迟加载非关键模块
- 预加载关键数据
- 使用Hermes引擎

## 🔌 扩展性

### 插件系统（未来规划）
- 自定义规则引擎
- 第三方主题
- 脚本扩展

### 多核心支持
- Clash Meta (Mihomo)
- Clash Premium
- Sing-box

## 📝 技术债务

1. ⚠️ VPN进程通信需要优化
2. ⚠️ iOS Network Extension性能调优
3. ⚠️ 日志系统需要重构
4. ⚠️ 配置迁移工具待完善

## 🚀 未来规划

- [ ] 支持Wireguard协议
- [ ] 订阅转换服务
- [ ] 智能分流规则
- [ ] 多设备同步
- [ ] WebDAV备份

