# ⚡ Clash Verge Mobile 快速开始

> 5分钟内运行您的第一个移动端构建

## 📋 前置条件检查

在开始之前，确保您已安装：

| 工具 | 版本要求 | 检查命令 |
|------|---------|----------|
| Node.js | 18+ | `node --version` |
| pnpm | 8+ | `pnpm --version` |
| Rust | 1.75+ | `rustc --version` |

### Android开发
- [ ] Android Studio 2023.1+
- [ ] JDK 17+
- [ ] Android SDK API 34
- [ ] Android NDK 25+

### iOS开发 (仅macOS)
- [ ] Xcode 14+
- [ ] CocoaPods 1.12+

## 🚀 5分钟快速启动

### Step 1: 克隆并安装依赖

```bash
# 进入mobile目录
cd clash-verge-rev/mobile

# 安装依赖
pnpm install
```

### Step 2: 选择平台并运行

#### Android

```bash
# 启动开发服务器
pnpm android

# 或使用Tauri CLI
cargo tauri android dev
```

第一次运行会自动：
1. 下载Android NDK
2. 编译Rust代码
3. 构建APK
4. 安装到设备/模拟器

#### iOS (仅macOS)

```bash
# 安装CocoaPods依赖
cd ios && pod install && cd ..

# 启动开发服务器
pnpm ios

# 或使用Tauri CLI
cargo tauri ios dev
```

### Step 3: 查看效果

应用启动后，您将看到：
- 🏠 主页：流量统计和连接按钮
- 🌐 代理页：代理节点列表
- 📁 配置页：订阅管理
- ⚙️ 设置页：应用配置

## 📱 在真机上运行

### Android

1. **启用USB调试**
   ```
   设置 > 关于手机 > 连续点击版本号7次
   设置 > 开发者选项 > 开启USB调试
   ```

2. **连接设备**
   ```bash
   # 检查设备连接
   adb devices
   
   # 如果显示设备，直接运行
   pnpm android
   ```

### iOS

1. **连接iPhone到Mac**

2. **信任设备**
   - 在iPhone上点击"信任此电脑"

3. **运行**
   ```bash
   # 列出可用设备
   xcrun xctrace list devices
   
   # 在设备上运行
   pnpm ios --device "Your iPhone"
   ```

## 🔨 常用命令

### 开发模式

```bash
# Android开发模式
pnpm android

# iOS开发模式
pnpm ios

# 仅启动Metro bundler
pnpm start
```

### 构建发布版

```bash
# Android Release APK
pnpm build:android

# iOS Release (需要在Xcode中操作)
open ios/ClashVerge.xcworkspace
# Product > Archive
```

### 清理缓存

```bash
# 清理所有缓存
pnpm clean

# 仅清理Metro缓存
pnpm start -- --reset-cache
```

### 调试

```bash
# 打开React DevTools
pnpm devtools

# 查看日志
# Android
adb logcat | grep ReactNative

# iOS
xcrun simctl spawn booted log stream --predicate 'process == "ClashVerge"'
```

## 🐛 常见问题解决

### Android

**问题1: Metro端口被占用**
```bash
# 杀掉占用8081端口的进程
# Windows
netstat -ano | findstr :8081
taskkill /PID <PID> /F

# macOS/Linux
lsof -ti:8081 | xargs kill -9
```

**问题2: Gradle下载缓慢**
```bash
# 使用国内镜像
# 编辑 android/build.gradle
repositories {
    maven { url 'https://maven.aliyun.com/repository/google' }
    maven { url 'https://maven.aliyun.com/repository/central' }
}
```

**问题3: 签名错误**
```bash
# 生成debug签名
cd android/app
keytool -genkey -v -keystore debug.keystore \
    -storepass android -alias androiddebugkey \
    -keypass android -keyalg RSA -keysize 2048 -validity 10000
```

### iOS

**问题1: CocoaPods安装失败**
```bash
# 更新CocoaPods
sudo gem install cocoapods

# 清理并重新安装
cd ios
pod deintegrate
rm Podfile.lock
pod install --repo-update
```

**问题2: 开发者证书问题**
```
在Xcode中:
1. 打开 ios/ClashVerge.xcworkspace
2. 选择项目 > Signing & Capabilities
3. 选择您的开发团队
4. 勾选 "Automatically manage signing"
```

**问题3: Simulator无法启动**
```bash
# 列出所有模拟器
xcrun simctl list devices

# 删除不可用的模拟器
xcrun simctl delete unavailable

# 重启Simulator
killall Simulator
```

## 📚 下一步

现在您已经成功运行了应用，可以：

1. 📖 阅读 [开发指南](./DEVELOPMENT.md) 了解详细开发流程
2. 🏗️ 查看 [架构设计](./ARCHITECTURE.md) 了解项目结构
3. 🚢 参考 [部署指南](./DEPLOYMENT.md) 发布应用
4. 🎨 查看 [UI组件](./src/components/) 了解可用组件

## 💡 开发技巧

### 1. 热重载

修改代码后，应用会自动刷新：
- React组件：立即刷新
- 样式更改：立即应用
- 原生代码：需要重新构建

### 2. 开发菜单

在应用中摇动设备，或按：
- Android: `Ctrl+M` (Windows/Linux) 或 `Cmd+M` (macOS)
- iOS: `Cmd+D`

可以访问：
- Reload - 重新加载
- Debug - 打开Chrome DevTools
- Enable Fast Refresh - 快速刷新
- Show Inspector - 元素检查器

### 3. Chrome DevTools

```bash
# 在浏览器中打开
chrome://inspect

# 选择您的应用进行调试
```

### 4. 性能监控

```typescript
// 在组件中添加性能标记
import { performance } from 'perf_hooks';

performance.mark('proxy-list-start');
// ... 渲染逻辑
performance.mark('proxy-list-end');
performance.measure('proxy-list', 'proxy-list-start', 'proxy-list-end');
```

## 🎯 开发工作流推荐

```bash
# 1. 启动开发服务器（终端1）
pnpm start

# 2. 运行Android/iOS（终端2）
pnpm android
# 或
pnpm ios

# 3. 查看日志（终端3）
# Android
adb logcat | grep ReactNative

# iOS
xcrun simctl spawn booted log stream

# 4. 代码编辑器
# 推荐使用VS Code + React Native Tools扩展
```

## 🌟 推荐扩展

### VS Code扩展
- React Native Tools
- ESLint
- Prettier
- TypeScript Hero
- GitLens

### 调试工具
- [Reactotron](https://github.com/infinitered/reactotron) - React Native调试器
- [Flipper](https://fbflipper.com/) - 移动端调试平台

## 🤝 获取帮助

如遇问题：

1. 📖 查看文档：`./DEVELOPMENT.md`
2. 🔍 搜索Issues：[GitHub Issues](https://github.com/clash-verge-rev/clash-verge-rev/issues)
3. 💬 社区讨论：[Telegram](https://t.me/clash_verge_rev)
4. 🐛 报告Bug：提交Issue并包含日志

## ✅ 检查清单

在提交代码前：

- [ ] 代码通过ESLint检查：`pnpm lint`
- [ ] 格式化代码：`pnpm format`
- [ ] 测试通过：`pnpm test`
- [ ] Android构建成功：`pnpm build:android`
- [ ] iOS构建成功 (macOS)：在Xcode中Archive
- [ ] 更新相关文档

## 🎉 完成！

恭喜！您已经成功搭建了Clash Verge Mobile开发环境。

开始您的开发之旅吧！ 🚀

