# 🔍 GitHub Actions 构建状态检查指南

## 📊 查看构建状态的方法

### 方法 1: 通过浏览器查看（推荐）

访问以下链接查看构建状态：

**主 Actions 页面：**
```
https://github.com/xiehaibo11/shouj/actions
```

**mobile-app 分支的 workflow 运行：**
```
https://github.com/xiehaibo11/shouj/actions/workflows/android-build.yml
```

### 方法 2: 使用 GitHub CLI

如果安装了 GitHub CLI (`gh`)，可以运行：

```bash
# 查看最近的 workflow 运行
gh run list --repo xiehaibo11/shouj --branch mobile-app

# 查看特定 workflow 的运行状态
gh run list --repo xiehaibo11/shouj --workflow=android-build.yml

# 查看最新运行的详细日志
gh run view --repo xiehaibo11/shouj --log
```

### 方法 3: 检查 workflow 文件

当前 workflow 配置：
- **文件位置**: `.github/workflows/android-build.yml`
- **触发条件**: 
  - Push 到 `main` 或 `dev` 分支
  - Pull Request 到 `main` 分支
  - 手动触发 (workflow_dispatch)

**注意**: 由于 workflow 配置的触发分支是 `main` 和 `dev`，而您推送到的是 `mobile-app` 分支，workflow **不会自动触发**！

## ⚠️ 重要发现

### 当前问题
您的 `android-build.yml` workflow 配置为：
```yaml
on:
  push:
    branches: [ main, dev ]
  pull_request:
    branches: [ main ]
```

但您推送到的是 `mobile-app` 分支，所以 **workflow 不会自动运行**！

### 解决方案

#### 选项 1: 修改 workflow 触发分支（推荐）
将 `mobile-app` 添加到触发分支列表：

```yaml
on:
  push:
    branches: [ main, dev, mobile-app ]  # ← 添加 mobile-app
  pull_request:
    branches: [ main ]
  workflow_dispatch:  # 保留手动触发
```

#### 选项 2: 合并到 main 分支
```bash
git checkout main
git merge mobile-app
git push shouj main
```

#### 选项 3: 创建 Pull Request
在 GitHub 上创建从 `mobile-app` 到 `main` 的 PR，这会触发 workflow。

#### 选项 4: 手动触发 workflow
1. 访问: https://github.com/xiehaibo11/shouj/actions/workflows/android-build.yml
2. 点击右上角的 "Run workflow" 按钮
3. 选择 `mobile-app` 分支
4. 点击 "Run workflow"

## 🔧 推荐的修复步骤

### 立即修复 workflow 配置

运行以下命令更新 workflow：

```bash
# 1. 修改 workflow 文件（已经为您准备好了修改）
# 2. 提交并推送
git add .github/workflows/android-build.yml
git commit -m "ci: Add mobile-app branch to workflow triggers"
git push shouj mobile-app
```

修改后，未来推送到 `mobile-app` 分支时会自动触发构建。

## 📝 最新提交状态

```
f78bc706 - fix: Use bash for gradlew script to fix 'Bad substitution' error
29968b42 - fix: Add complete Gradle wrapper files for Android build
```

这两个提交已经推送到 `shouj/mobile-app` 分支。

## ✅ 验证构建修复

一旦 workflow 运行，检查以下内容：

1. **gradlew 权限**: `chmod +x gradlew` 应该成功
2. **bash 执行**: 不应再出现 "Bad substitution" 错误
3. **gradle wrapper**: 应该能找到 `gradle-wrapper.jar`
4. **构建成功**: 应该生成 `app-release.apk`

## 🎯 预期结果

修复后的构建应该：
- ✅ 成功执行 `./gradlew assembleRelease`
- ✅ 生成 APK 文件
- ✅ 上传 artifact 到 GitHub Actions
- ✅ 整个构建过程无错误

## 📞 如果构建仍然失败

查看构建日志中的错误信息，常见问题：
- Gradle 依赖下载失败 → 网络问题
- SDK 版本不匹配 → 检查 `build.gradle` 配置
- 内存不足 → 增加 Gradle JVM 内存
- 权限问题 → 确保 `gradlew` 有执行权限

