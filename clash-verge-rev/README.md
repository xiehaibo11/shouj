# Clash Verge Rev Mobile

移动端版本（Android & iOS）

## 技术栈

- **前端框架**: React Native 0.73+
- **UI库**: React Native Paper (Material Design)
- **状态管理**: Zustand + SWR
- **导航**: React Navigation 6
- **后端**: Tauri Mobile + Rust
- **核心**: Clash Meta (mihomo)

## 目录结构

```
mobile/
├── android/              # Android 原生代码
├── ios/                  # iOS 原生代码
├── src/                  # 共享源代码
│   ├── components/       # UI组件
│   ├── screens/          # 页面
│   ├── hooks/            # 自定义Hooks
│   ├── services/         # API服务
│   ├── utils/            # 工具函数
│   ├── navigation/       # 导航配置
│   └── theme/            # 主题配置
├── src-tauri/            # Tauri后端（复用桌面版）
└── package.json

```

## 主要功能

### ✅ 已实现
- [ ] 配置文件管理（订阅、本地）
- [ ] 代理节点选择和测速
- [ ] 系统VPN集成
- [ ] 实时流量监控
- [ ] 连接管理
- [ ] 规则管理
- [ ] 日志查看
- [ ] 主题切换（深色/浅色）
- [ ] 多语言支持

### 🚧 移动端特有功能
- [ ] VPN服务（VpnService）
- [ ] 省电模式
- [ ] 快速磁贴（Quick Settings）
- [ ] 通知栏控制
- [ ] 小部件（Widget）
- [ ] 生物识别解锁
- [ ] 流量统计（按应用）

## 开发指南

### 前置要求

1. Node.js 18+
2. Rust 1.75+
3. Android Studio（Android开发）
4. Xcode 14+（iOS开发，仅macOS）
5. React Native CLI

### 安装依赖

```bash
# 安装Node依赖
npm install

# 安装Tauri CLI
cargo install tauri-cli --version "^2.0.0"

# Android
cd android && ./gradlew clean

# iOS
cd ios && pod install
```

### 运行开发服务器

```bash
# Android
npm run android

# iOS
npm run ios

# 或使用Tauri
cargo tauri android dev
cargo tauri ios dev
```

### 构建发布版本

```bash
# Android APK
npm run build:android

# iOS IPA
npm run build:ios
```

## 与桌面版的差异

### UI适配
- 使用底部标签导航（Bottom Tab Navigation）
- 移除侧边栏，改用抽屉导航
- 适配小屏幕，简化操作流程
- 添加手势操作（滑动、长按）

### 功能调整
- 系统代理 → VPN模式
- 托盘图标 → 通知栏
- 窗口管理 → Activity管理
- 热键 → 快捷操作

### 权限需求

**Android:**
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `FOREGROUND_SERVICE`
- `BIND_VPN_SERVICE`
- `WAKE_LOCK`

**iOS:**
- Network Extensions
- VPN Configuration
- Background Modes

## 构建流程

1. 前端打包 → Vite构建React应用
2. Rust编译 → 交叉编译到目标平台
3. 打包核心 → 嵌入mihomo二进制
4. 原生构建 → Gradle/Xcode构建
5. 签名发布 → APK/IPA签名

## 性能优化

- 使用Hermes引擎（Android）
- 启用新架构（New Architecture）
- 懒加载和代码分割
- 图片优化和缓存
- 网络请求合并

## 测试

```bash
# 单元测试
npm test

# E2E测试
npm run test:e2e

# 性能测试
npm run test:perf
```

## 发布渠道

- **Android**: Google Play Store / 酷安 / GitHub Releases
- **iOS**: App Store / TestFlight

## 注意事项

### Android
- 最低支持 Android 7.0 (API 24)
- 推荐 Android 10+ (API 29) 获得最佳体验
- 需要申请VPN权限

### iOS
- 最低支持 iOS 13.0
- 推荐 iOS 15+ 获得最佳体验
- 需要申请Network Extension权限
- 需要付费开发者账号

## 常见问题

### 1. VPN连接失败
- 检查VPN权限是否授予
- 确认系统版本支持
- 查看日志排查问题

### 2. 后台被杀死
- 添加到电池优化白名单
- 启用前台服务
- 使用WorkManager保活

### 3. 无法导入订阅
- 检查网络连接
- 确认订阅链接有效
- 查看错误日志

## 贡献指南

欢迎提交Issue和Pull Request！

## 许可证

GPL-3.0 License

