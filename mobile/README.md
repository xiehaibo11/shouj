# Clash Verge Rev - Android 移动端

纯原生 Android 客户端，使用 **Kotlin + NDK**，与 ClashMetaForAndroid 同路。

## 📋 系统要求

- **Android 版本**: Android 7.0 (API 24) 或更高
- **架构支持**: ARM64, ARMv7, x86_64, x86
- **存储空间**: 至少 100MB

## 🚀 构建说明

### 本地开发构建

#### 前置要求

1. **Java JDK 17**
2. **Android SDK** 和 **NDK** (API 24+)
3. **Gradle 8.5+** (已包含 Gradle Wrapper)

#### 生产构建

```bash
# 进入 mobile 目录
cd mobile

# 构建所有架构（通用版）
./gradlew assembleRelease

# 构建特定架构
./gradlew assembleRelease -Pandroid.injected.build.abi=arm64-v8a      # ARM64 (推荐)
./gradlew assembleRelease -Pandroid.injected.build.abi=armeabi-v7a    # ARMv7 (老设备)
./gradlew assembleRelease -Pandroid.injected.build.abi=x86_64         # x86 64位 (模拟器)

# 或使用 npm scripts
npm run gradle:build                # 通用版
npm run gradle:build:aarch64        # ARM64
npm run gradle:build:armv7          # ARMv7
npm run gradle:build:x86_64         # x86_64
```

构建产物位置：
```
mobile/app/build/outputs/apk/release/
```

### 📦 GitHub Actions 自动构建

项目已配置 GitHub Actions 自动构建，支持以下触发方式：

#### 1. 手动触发构建

在 GitHub 仓库页面：
```
Actions → Android Build → Run workflow
```

#### 2. 推送代码自动构建

当推送到 `main` 分支且包含以下文件变更时自动触发：
- `mobile/**`
- `src/**`
- `src-tauri/**`

构建完成后，APK 将作为 **Artifacts** 上传，可在 Actions 页面下载。

#### 3. 创建正式发布

创建以 `android-v` 开头的 tag 触发正式发布：

```bash
# 创建 tag
git tag android-v1.0.0

# 推送 tag
git push origin android-v1.0.0
```

这将自动：
- 构建所有架构的 APK
- 创建 GitHub Release
- 上传 APK 到 Release 页面

## 📱 APK 架构选择指南

| 架构 | 适用设备 | 说明 |
|------|---------|------|
| **ARM64** (aarch64) | 2015年后的大多数设备 | ✅ **推荐**，性能最佳 |
| **ARMv7** (armv7) | 2015年前的老设备 | 32位架构，兼容性好 |
| **x86_64** | Android 模拟器 | 适用于开发测试 |
| **Universal** | 所有设备 | 包含所有架构，体积最大 |

## 🔧 配置说明

### 应用配置

主要配置文件：
- `mobile/app/build.gradle.kts` - Gradle 构建配置
- `mobile/app/src/main/AndroidManifest.xml` - 应用清单

### 版本管理

版本号在根目录 `package.json` 中统一管理：
```json
{
  "version": "2.4.3"
}
```

### 签名配置

生产构建使用 debug 签名（第 36 行）。正式发布需要配置 release 签名：

```kotlin
// mobile/app/build.gradle.kts
signingConfigs {
    create("release") {
        storeFile = file("your-keystore.jks")
        storePassword = "your-store-password"
        keyAlias = "your-key-alias"
        keyPassword = "your-key-password"
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ...
    }
}
```

## 🐛 故障排除

### 构建失败

1. **依赖问题**
   ```bash
   # 清理缓存
   cd mobile
   ./gradlew clean
   
   # 重新安装依赖
   pnpm install
   ```

2. **Android SDK/NDK 未找到**
   - 确保已安装 Android SDK 和 NDK
   - 设置环境变量 `ANDROID_HOME` 和 `ANDROID_NDK_HOME`
   - 在 `local.properties` 中配置 SDK 路径

### 安装失败

1. **"无法安装应用"**
   - 启用"允许安装未知应用"权限
   - 如有旧版本，先卸载

2. **"应用未安装"**
   - 检查设备存储空间
   - 确认架构匹配（查看 设置 → 关于手机）

## 📚 相关资源

- [Tauri 文档](https://tauri.app/)
- [Android 开发文档](https://developer.android.com/)
- [Rust Android 文档](https://mozilla.github.io/firefox-browser-architecture/experiments/2017-09-21-rust-on-android.html)

## 📄 许可证

GPL-3.0 License - 详见根目录 LICENSE 文件
