# Clash Verge Mobile 开发指南

## 🚀 快速开始

### 环境要求

#### 通用要求
- Node.js 18+
- pnpm 8+
- Rust 1.75+
- Tauri CLI 2.0+

#### Android 开发
- Android Studio 2023.1+
- Android SDK API 34
- Android NDK 25+
- JDK 17+

#### iOS 开发 (仅 macOS)
- Xcode 14+
- CocoaPods 1.12+
- iOS 13.0+ SDK

### 安装步骤

1. **克隆项目**
```bash
cd clash-verge-rev
cd mobile
```

2. **安装依赖**
```bash
pnpm install
```

3. **安装Rust工具链**
```bash
# 安装Tauri CLI
cargo install tauri-cli --version "^2.0.0"

# 添加目标平台
rustup target add aarch64-linux-android armv7-linux-androideabi
rustup target add aarch64-apple-ios
```

4. **Android额外配置**
```bash
# 安装Android NDK
# 在Android Studio中安装SDK和NDK

# 设置环境变量
export ANDROID_HOME=$HOME/Android/Sdk
export NDK_HOME=$ANDROID_HOME/ndk/25.1.8937393
```

5. **iOS额外配置** (仅macOS)
```bash
cd ios
pod install
```

## 🔨 开发

### 启动开发服务器

#### 使用React Native CLI
```bash
# Android
pnpm android

# iOS
pnpm ios
```

#### 使用Tauri
```bash
# Android
cargo tauri android dev

# iOS
cargo tauri ios dev
```

### 调试

#### 启用开发菜单
- Android: 摇动设备或按 `Cmd+M` (macOS) / `Ctrl+M` (Windows/Linux)
- iOS: 摇动设备或按 `Cmd+D`

#### Chrome DevTools
```bash
# 在浏览器中打开
chrome://inspect
```

#### 日志查看
```bash
# Android
adb logcat | grep ReactNative

# iOS
xcrun simctl spawn booted log stream --predicate 'process == "ClashVerge"'
```

## 📦 构建

### Android

#### Debug版本
```bash
pnpm build:android
# 输出: android/app/build/outputs/apk/debug/app-debug.apk
```

#### Release版本
```bash
# 1. 生成签名密钥
keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000

# 2. 配置gradle.properties
echo "MYAPP_RELEASE_STORE_FILE=my-release-key.keystore" >> android/gradle.properties
echo "MYAPP_RELEASE_KEY_ALIAS=my-key-alias" >> android/gradle.properties
echo "MYAPP_RELEASE_STORE_PASSWORD=***" >> android/gradle.properties
echo "MYAPP_RELEASE_KEY_PASSWORD=***" >> android/gradle.properties

# 3. 构建
cd android
./gradlew assembleRelease

# 输出: android/app/build/outputs/apk/release/app-release.apk
```

### iOS

#### Debug版本
```bash
pnpm ios
```

#### Release版本
```bash
# 1. 在Xcode中打开项目
open ios/ClashVerge.xcworkspace

# 2. 选择 Product > Archive
# 3. 在Organizer中选择 Distribute App
# 4. 选择发布方式 (App Store / Ad Hoc / Enterprise)
```

## 🧪 测试

### 单元测试
```bash
pnpm test
```

### E2E测试
```bash
# Android
pnpm test:e2e:android

# iOS
pnpm test:e2e:ios
```

## 📱 在真机上运行

### Android

1. **启用开发者选项**
   - 设置 > 关于手机 > 连续点击版本号7次

2. **启用USB调试**
   - 设置 > 开发者选项 > USB调试

3. **连接设备并运行**
```bash
adb devices
pnpm android
```

### iOS

1. **配置开发者账号**
   - Xcode > Preferences > Accounts > 添加Apple ID

2. **选择开发团队**
   - 在Xcode中选择项目 > Signing & Capabilities > Team

3. **运行**
```bash
# 列出可用设备
xcrun xctrace list devices

# 在指定设备上运行
pnpm ios --device "Your Device Name"
```

## 🐛 常见问题

### Android

**Q: Metro bundler端口被占用**
```bash
# 杀掉占用8081端口的进程
lsof -ti:8081 | xargs kill -9
```

**Q: Gradle下载慢**
```bash
# 使用阿里云镜像
# 在 android/build.gradle 中添加:
repositories {
    maven { url 'https://maven.aliyun.com/repository/google' }
    maven { url 'https://maven.aliyun.com/repository/central' }
}
```

**Q: VPN权限被拒绝**
- 确保在AndroidManifest.xml中声明了BIND_VPN_SERVICE权限
- 检查用户是否授予了VPN权限

### iOS

**Q: CocoaPods安装失败**
```bash
cd ios
pod deintegrate
pod install --repo-update
```

**Q: 代码签名错误**
- 检查开发者证书是否有效
- 确认Bundle Identifier唯一
- 重新选择开发团队

**Q: Network Extension不工作**
- 确保在Capabilities中启用了Network Extensions
- 检查Entitlements.plist配置

## 🔧 性能优化

### Bundle大小优化

#### Android
```bash
# 启用ProGuard
# 在 android/app/build.gradle 中
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
    }
}

# 分架构打包
splits {
    abi {
        enable true
        reset()
        include 'arm64-v8a', 'armeabi-v7a'
    }
}
```

#### iOS
```bash
# 启用Bitcode
# 在Xcode中: Build Settings > Enable Bitcode = Yes

# 移除未使用的架构
# Build Settings > Excluded Architectures
```

### 启动速度优化

1. **使用Hermes引擎** (已默认启用)
2. **启用内联require**
3. **移除console.log** (生产环境)
4. **使用RAM Bundle**

### 运行时性能

1. **使用FlatList代替ScrollView**
2. **避免匿名函数**
3. **使用React.memo**
4. **优化图片加载**

## 📚 相关资源

- [React Native文档](https://reactnative.dev/)
- [Tauri文档](https://tauri.app/)
- [Material Design](https://m3.material.io/)
- [Android开发者文档](https://developer.android.com/)
- [iOS开发者文档](https://developer.apple.com/)

## 🤝 贡献

欢迎提交Issue和Pull Request！

