# Clash Verge Mobile 部署指南

## 📦 应用商店发布

### Google Play Store (Android)

#### 1. 准备工作

**创建开发者账号**
- 访问 [Google Play Console](https://play.google.com/console)
- 支付 $25 一次性注册费
- 完成身份验证

**准备应用资源**
- 应用图标 (512x512 PNG)
- 功能图片 (1024x500 PNG)
- 应用截图 (至少2张，最多8张)
- 隐私政策URL
- 应用描述 (简短和详细)

#### 2. 构建Release版本

```bash
cd mobile/android
./gradlew bundleRelease
```

输出文件: `android/app/build/outputs/bundle/release/app-release.aab`

#### 3. 签名配置

在 `android/gradle.properties` 中配置:
```properties
MYAPP_RELEASE_STORE_FILE=my-release-key.keystore
MYAPP_RELEASE_KEY_ALIAS=my-key-alias
MYAPP_RELEASE_STORE_PASSWORD=your-store-password
MYAPP_RELEASE_KEY_PASSWORD=your-key-password
```

#### 4. 上传到Play Store

1. 登录 [Google Play Console](https://play.google.com/console)
2. 创建新应用
3. 填写应用信息
4. 上传 AAB 文件
5. 设置定价和分发国家/地区
6. 提交审核

#### 5. 审核要点

- **VPN功能声明**: 必须明确说明VPN功能
- **隐私政策**: 必须提供有效的隐私政策链接
- **权限说明**: 解释每个权限的用途
- **合规性**: 遵守当地法律法规

### Apple App Store (iOS)

#### 1. 准备工作

**开发者账号**
- 个人: $99/年
- 企业: $299/年
- 访问 [Apple Developer](https://developer.apple.com/)

**证书和描述文件**
```bash
# 在Xcode中自动管理
# 或使用Fastlane自动化
```

#### 2. 构建Release版本

```bash
# 在Xcode中
1. Product > Scheme > Edit Scheme
2. 选择 Release 配置
3. Product > Archive
4. 在Organizer中导出IPA
```

#### 3. App Store Connect配置

1. 登录 [App Store Connect](https://appstoreconnect.apple.com/)
2. 创建新App
3. 填写应用信息
4. 上传构建版本
5. 提交审核

#### 4. Network Extension权限

**Capabilities配置**
```xml
<!-- Entitlements.plist -->
<key>com.apple.developer.networking.networkextension</key>
<array>
    <string>packet-tunnel-provider</string>
</array>
```

**审核说明**
- 提供详细的功能说明
- 解释Network Extension的用途
- 提供测试账号

#### 5. 审核注意事项

- **功能完整性**: 确保所有功能正常工作
- **崩溃率**: 提交前充分测试
- **隐私说明**: 详细说明数据收集和使用
- **年龄分级**: 正确设置应用分级

## 🌐 自托管分发

### Android - APK直接分发

#### 1. 构建签名APK

```bash
cd mobile/android
./gradlew assembleRelease
```

#### 2. 优化APK

```bash
# 使用zipalign优化
zipalign -v -p 4 app-release-unsigned.apk app-release-aligned.apk

# 使用apksigner签名
apksigner sign --ks my-release-key.keystore \
    --out app-release.apk app-release-aligned.apk
```

#### 3. 分发方式

**GitHub Releases**
```yaml
# .github/workflows/release.yml
name: Build and Release
on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build APK
        run: |
          cd mobile/android
          ./gradlew assembleRelease
      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: mobile/android/app/build/outputs/apk/release/*.apk
```

**自建服务器**
```nginx
# nginx配置
location /download {
    alias /var/www/apps;
    autoindex on;
    add_header Content-Disposition "attachment";
}
```

### iOS - TestFlight / IPA分发

#### 1. TestFlight内测

```bash
# 使用Fastlane自动化
fastlane beta
```

配置 `fastlane/Fastfile`:
```ruby
lane :beta do
  build_app(scheme: "ClashVerge")
  upload_to_testflight
end
```

#### 2. 企业分发 (需要企业账号)

**生成IPA**
```bash
xcodebuild archive -workspace ClashVerge.xcworkspace \
    -scheme ClashVerge \
    -archivePath build/ClashVerge.xcarchive

xcodebuild -exportArchive \
    -archivePath build/ClashVerge.xcarchive \
    -exportPath build \
    -exportOptionsPlist ExportOptions.plist
```

**创建manifest.plist**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN">
<plist version="1.0">
<dict>
    <key>items</key>
    <array>
        <dict>
            <key>assets</key>
            <array>
                <dict>
                    <key>kind</key>
                    <string>software-package</string>
                    <key>url</key>
                    <string>https://example.com/app.ipa</string>
                </dict>
            </array>
            <key>metadata</key>
            <dict>
                <key>bundle-identifier</key>
                <string>io.github.clashverge.mobile</string>
                <key>kind</key>
                <string>software</string>
                <key>title</key>
                <string>Clash Verge</string>
            </dict>
        </dict>
    </array>
</dict>
</plist>
```

**安装链接**
```html
<a href="itms-services://?action=download-manifest&url=https://example.com/manifest.plist">
    安装Clash Verge
</a>
```

## 🔄 持续集成/部署

### GitHub Actions示例

```yaml
name: Mobile CI/CD

on:
  push:
    branches: [ main, dev ]
  pull_request:
    branches: [ main ]

jobs:
  android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'
          
      - name: Install dependencies
        run: |
          cd mobile
          npm install -g pnpm
          pnpm install
          
      - name: Build Android
        run: |
          cd mobile/android
          ./gradlew assembleRelease
          
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-release
          path: mobile/android/app/build/outputs/apk/release/

  ios:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          
      - name: Install dependencies
        run: |
          cd mobile
          npm install -g pnpm
          pnpm install
          cd ios
          pod install
          
      - name: Build iOS
        run: |
          cd mobile/ios
          xcodebuild -workspace ClashVerge.xcworkspace \
            -scheme ClashVerge \
            -configuration Release \
            -archivePath build/ClashVerge.xcarchive \
            archive
```

## 📊 监控和分析

### 崩溃报告

**Android - Firebase Crashlytics**
```bash
npm install @react-native-firebase/app @react-native-firebase/crashlytics
```

**iOS - Firebase Crashlytics**
```bash
pod 'Firebase/Crashlytics'
```

### 应用分析

**Google Analytics**
```bash
npm install @react-native-firebase/analytics
```

**自定义事件追踪**
```typescript
import analytics from '@react-native-firebase/analytics';

await analytics().logEvent('vpn_connected', {
  server: serverName,
  protocol: 'shadowsocks',
});
```

## 🔒 安全建议

1. **代码混淆**: 启用ProGuard (Android) 和 Strip (iOS)
2. **根检测**: 检测越狱/Root设备
3. **证书固定**: 防止中间人攻击
4. **加密存储**: 敏感数据加密存储
5. **定期更新**: 及时修复安全漏洞

## 📝 版本管理

### 语义化版本

```
主版本号.次版本号.修订号

1.0.0 - 初始发布
1.1.0 - 新增功能
1.1.1 - Bug修复
2.0.0 - 重大更新
```

### 更新策略

- **强制更新**: 关键安全修复
- **推荐更新**: 重要功能更新
- **可选更新**: 小功能和优化

## 🎯 发布检查清单

- [ ] 所有功能测试通过
- [ ] 性能测试通过
- [ ] 崩溃率 < 0.1%
- [ ] 隐私政策已更新
- [ ] 更新日志已准备
- [ ] 应用截图已更新
- [ ] 版本号已递增
- [ ] 签名密钥安全存储
- [ ] 备份发布版本

