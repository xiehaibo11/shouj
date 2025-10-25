# 🎉 Android 构建问题修复总结

## 📅 修复时间
2025-10-25

## 🔍 问题诊断

### 原始错误
```
./gradlew: 8: Bad substitution
./gradlew: 10: exec: /gradle/wrapper/gradle-wrapper.jar: not found
```

### 根本原因
1. **gradlew shebang 错误**: 使用 `#!/bin/sh` 而不是 `#!/usr/bin/env bash`
2. **shell 不兼容**: dash (sh) 不支持 bash 特有的参数替换语法
3. **连锁反应**: 语法错误导致路径变量为空，形成错误的绝对路径
4. **workflow 配置**: 未将 `mobile-app` 分支添加到触发列表

## ✅ 已完成的修复

### 1. 修复 gradlew 脚本 (提交: 29968b42)
**文件**: `mobile/android/gradlew`
```bash
# 修改前
#!/bin/sh

# 修改后
#!/usr/bin/env bash
```

**文件**: `mobile/android/gradlew.bat`
- 更新为完整的 Windows Gradle Wrapper 脚本

### 2. 添加 Gradle Wrapper 文件 (提交: 29968b42)
**新增文件**:
- ✅ `mobile/android/gradle/wrapper/gradle-wrapper.jar`
- ✅ `mobile/android/gradle/wrapper/gradle-wrapper.properties`

### 3. 更新 GitHub Actions Workflow (提交: f78bc706)
**文件**: `.github/workflows/android-build.yml`
```yaml
- name: Build Android APK
  run: |
    cd mobile/android
    chmod +x gradlew
    ./gradlew assembleRelease
  shell: bash  # ← 明确指定使用 bash
```

### 4. 添加 mobile-app 分支触发 (提交: c36b7599)
**文件**: `.github/workflows/android-build.yml`
```yaml
on:
  push:
    branches: [ main, dev, mobile-app ]  # ← 添加 mobile-app
```

## 📊 提交历史

```
c36b7599 - ci: Add mobile-app branch to workflow triggers and build status guide
f78bc706 - fix: Use bash for gradlew script to fix 'Bad substitution' error
29968b42 - fix: Add complete Gradle wrapper files for Android build
```

## 🚀 构建状态

### 查看构建
访问以下链接查看实时构建状态：

**Actions 页面**:
```
https://github.com/xiehaibo11/shouj/actions
```

**Android Build Workflow**:
```
https://github.com/xiehaibo11/shouj/actions/workflows/android-build.yml
```

### 预期结果
✅ Workflow 应该自动触发（刚刚推送触发）
✅ gradlew 脚本使用 bash 执行
✅ 找到 gradle-wrapper.jar
✅ 成功构建 APK
✅ 上传 artifact: `clash-verge-mobile-release`

## 🔧 技术细节

### 修复的关键点

1. **Shebang 选择**
   - `#!/bin/sh` → 使用系统默认 shell (通常是 dash)
   - `#!/usr/bin/env bash` → 明确使用 bash，支持高级语法

2. **Bash vs Dash 差异**
   - Bash: 支持 `${var%suffix}`, `${var#prefix}` 等参数扩展
   - Dash: POSIX 兼容，不支持 bash 扩展语法

3. **CI/CD 环境**
   - Ubuntu runners 默认使用 dash 作为 `/bin/sh`
   - 必须明确指定 `shell: bash` 或使用 bash shebang

4. **Gradle Wrapper 结构**
   ```
   mobile/android/
   ├── gradlew           (Unix 脚本)
   ├── gradlew.bat       (Windows 脚本)
   └── gradle/
       └── wrapper/
           ├── gradle-wrapper.jar        (必需)
           └── gradle-wrapper.properties (必需)
   ```

## 📝 文件清单

### 修改的文件
- ✅ `mobile/android/gradlew` - 更新 shebang 和完整脚本
- ✅ `mobile/android/gradlew.bat` - 完整的 Windows 脚本
- ✅ `.github/workflows/android-build.yml` - 添加 bash shell 和分支触发

### 新增的文件
- ✅ `mobile/android/gradle/wrapper/gradle-wrapper.jar`
- ✅ `mobile/android/gradle/wrapper/gradle-wrapper.properties`
- ✅ `check-build-status.md` - 构建状态检查指南
- ✅ `BUILD_FIX_SUMMARY.md` - 本文档

## 🎯 验证清单

构建成功的标志：

- [ ] Workflow 自动触发
- [ ] Checkout 代码成功
- [ ] Setup Node.js 18 成功
- [ ] Setup Java 17 成功
- [ ] Setup Android SDK 成功
- [ ] Install pnpm 成功
- [ ] Install dependencies 成功
- [ ] `chmod +x gradlew` 成功
- [ ] `./gradlew assembleRelease` 成功执行
- [ ] 无 "Bad substitution" 错误
- [ ] 无 "gradle-wrapper.jar not found" 错误
- [ ] 生成 APK: `mobile/android/app/build/outputs/apk/release/app-release.apk`
- [ ] Upload artifact 成功

## 🔄 后续步骤

1. **监控构建**: 访问 Actions 页面查看构建进度
2. **下载 APK**: 构建成功后从 Artifacts 下载
3. **测试 APK**: 在 Android 设备上安装测试
4. **合并分支**: 如果一切正常，合并到 main 分支

## 📚 参考资源

- [Gradle Wrapper 文档](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- [GitHub Actions Shell 选项](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions#jobsjob_idstepsshell)
- [Bash vs Dash 差异](https://wiki.ubuntu.com/DashAsBinSh)

## ✨ 总结

所有已知的构建问题都已修复：
1. ✅ Gradle wrapper 脚本使用正确的 bash shebang
2. ✅ Gradle wrapper JAR 文件已添加到仓库
3. ✅ GitHub Actions 明确使用 bash shell
4. ✅ Workflow 配置为在 mobile-app 分支触发

**当前状态**: 🟢 已推送到 GitHub，workflow 应该正在运行

**仓库**: https://github.com/xiehaibo11/shouj
**分支**: mobile-app
**最新提交**: c36b7599

