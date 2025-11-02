# Clash Verge Rev - Android Mobile

基于 Tauri Mobile 的 Android 移动端应用

## 环境要求

### Android 开发环境

1. **Android Studio** (推荐最新稳定版)
2. **Android SDK** (API Level 33+)
3. **Android NDK** (r25c+)
4. **Java JDK** 17+

### 环境变量配置

```bash
# Windows (PowerShell)
$env:ANDROID_HOME = "C:\Users\YourUsername\AppData\Local\Android\Sdk"
$env:ANDROID_NDK_HOME = "$env:ANDROID_HOME\ndk\25.2.9519653"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

# macOS/Linux
export ANDROID_HOME=$HOME/Library/Android/sdk
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/25.2.9519653
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

### Rust 工具链

```bash
# 安装 Android 目标
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
rustup target add i686-linux-android
rustup target add x86_64-linux-android
```

## 构建步骤

### 1. 安装依赖

```bash
# 返回项目根目录
cd ..

# 安装前端依赖
pnpm install

# 预构建（下载 Mihomo 核心等）
pnpm run prebuild
```

### 2. 开发模式

```bash
# 在 Android 模拟器或设备上运行
pnpm tauri android dev

# 或者使用 cargo
cd src-tauri
cargo tauri android dev
```

### 3. 构建发布版

```bash
# 构建 ARM64 (推荐，主流设备)
pnpm tauri android build --target aarch64

# 构建 ARMv7 (旧设备兼容)
pnpm tauri android build --target armv7

# 构建 x86_64 (模拟器)
pnpm tauri android build --target x86_64

# 构建所有架构
pnpm tauri android build --target universal
```

### 4. 签名 APK

```bash
# 生成密钥库（首次）
keytool -genkey -v -keystore clash-verge-rev.keystore \
  -alias clash-verge-rev -keyalg RSA -keysize 2048 -validity 10000

# 签名 APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore clash-verge-rev.keystore \
  app-release-unsigned.apk clash-verge-rev

# 对齐 APK
zipalign -v 4 app-release-unsigned.apk clash-verge-rev-release.apk
```

## 项目结构

```
mobile/
├── app/                          # Android 应用主模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/            # Java/Kotlin 源代码
│   │   │   ├── res/             # 资源文件
│   │   │   └── AndroidManifest.xml
│   │   └── androidTest/         # Android 测试
│   ├── build.gradle.kts         # 应用级构建配置
│   └── proguard-rules.pro       # 混淆规则
├── build.gradle.kts             # 项目级构建配置
├── gradle.properties            # Gradle 配置
├── settings.gradle.kts          # Gradle 设置
└── README.md                    # 本文件
```

## 功能特性

### 已实现功能
- ✅ 核心代理功能（基于 Mihomo）
- ✅ 配置文件管理
- ✅ 系统 VPN 模式
- ✅ 节点延迟测试
- ✅ 规则管理
- ✅ 暗色/亮色主题
- ✅ 多语言支持

### 移动端特有功能
- ✅ VPN 服务（VpnService API）
- ✅ 通知栏快捷控制
- ✅ 省电模式优化
- ✅ 流量统计
- ✅ 自动启动

## 权限说明

应用需要以下权限：

- `INTERNET` - 网络访问
- `ACCESS_NETWORK_STATE` - 网络状态检测
- `FOREGROUND_SERVICE` - 前台服务（保持连接）
- `RECEIVE_BOOT_COMPLETED` - 开机自启
- `VIBRATE` - 通知振动
- `BIND_VPN_SERVICE` - VPN 服务绑定

## 调试技巧

### 查看日志

```bash
# Android Logcat
adb logcat | grep "ClashVerge"

# Tauri 日志
adb logcat | grep "RustCore"
```

### 安装到设备

```bash
# 查看连接的设备
adb devices

# 安装 APK
adb install path/to/app-release.apk

# 卸载应用
adb uninstall io.github.clash_verge_rev.clash_verge_rev
```

## 常见问题

### 1. Android SDK 未找到

确保设置了 `ANDROID_HOME` 环境变量并指向正确的 SDK 路径。

### 2. NDK 版本不兼容

使用 Android Studio SDK Manager 安装推荐的 NDK 版本 (25.2.9519653)。

### 3. 构建失败

```bash
# 清理缓存
cd src-tauri
cargo clean
./gradlew clean  # Android 项目清理
```

### 4. 设备无法连接

```bash
# 重启 ADB
adb kill-server
adb start-server
```

## 发布检查清单

- [ ] 更新版本号 (`tauri.conf.json`)
- [ ] 测试所有架构 (ARM64, ARMv7)
- [ ] 签名 APK
- [ ] 测试升级流程
- [ ] 检查权限声明
- [ ] 更新更新日志

## 参考链接

- [Tauri Mobile 文档](https://tauri.app/develop/mobile/)
- [Android 开发文档](https://developer.android.com/)
- [Mihomo 文档](https://wiki.metacubex.one/)

