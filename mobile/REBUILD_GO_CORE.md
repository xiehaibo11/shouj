# Go 核心重新编译指南

## 🎯 推荐方案：GitHub Actions（最简单）

### 优点
- ✅ 无需本地环境配置
- ✅ 云端 Linux 环境编译，100% 成功率
- ✅ 自动编译所有架构
- ✅ 可下载编译好的 APK

### 步骤

#### 1. 提交当前更改

```powershell
cd c:\Users\Administrator\Desktop\clash-verge-rev

# 查看更改
git status

# 添加所有修改
git add .

# 提交更改
git commit -m "fix(mobile): 修改 DNS 端口从 53 到 1053 解决 SSL 证书错误

- 更新 mihomo_core.go DNSHijack 配置为 any:1053
- 更新 config.go 默认 DNS listen 端口为 1053
- 更新 test-config.yaml DNS 端口配置
- 更新 CreateLocalConfigDialog.kt UI 模板
- 增强 Docker 编译脚本支持全架构
- 添加详细的修复文档

Fixes: YouTube 返回 Facebook 证书错误
Reason: Android 非 root 环境无法绑定 53 端口"
```

#### 2. 推送到 GitHub

```powershell
# 推送到远程仓库（将自动触发构建）
git push origin main
```

或者如果您在其他分支：

```powershell
git push origin YOUR_BRANCH_NAME
```

#### 3. 查看构建进度

1. 访问 GitHub 仓库
2. 点击 **Actions** 标签
3. 查看最新的 **Android Build** workflow
4. 等待编译完成（约 30-45 分钟）

#### 4. 下载 APK

构建完成后：
1. 进入完成的 workflow run
2. 滚动到 **Artifacts** 部分
3. 下载编译好的 APK 文件

---

## 💻 备选方案：修复 Docker 并本地编译

如果您需要本地编译，可以尝试修复 Docker：

### 修复 Docker Desktop

```powershell
# 1. 完全关闭 Docker Desktop
Get-Process '*docker*' | Stop-Process -Force

# 2. 重启 WSL
wsl --shutdown

# 3. 等待 30 秒，然后重新启动 Docker Desktop
Start-Sleep -Seconds 30
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# 4. 等待 Docker 完全启动（1-2分钟）
Start-Sleep -Seconds 120

# 5. 验证 Docker
docker version

# 6. 清理并编译
docker system prune -a -f
cd c:\Users\Administrator\Desktop\clash-verge-rev\mobile\scripts
bash build-docker.sh
```

---

## 🔧 终极方案：在真实 Linux 环境编译

如果上述方案都不可行，可以考虑：

### 选项 1：使用 Linux 虚拟机

1. 安装 VirtualBox + Ubuntu 22.04
2. 在虚拟机中安装 Go 和 Android NDK
3. 挂载 Windows 共享文件夹
4. 在虚拟机中编译

### 选项 2：使用云服务器

1. 租用临时云服务器（1-2小时）
2. 上传代码并编译
3. 下载编译好的 .so 文件

### 选项 3：请求他人帮助编译

1. 提交代码到 GitHub
2. 在 Issue 中请求编译帮助
3. 获取编译好的文件

---

## 📝 当前编译状态

### 已编译文件（旧版本）

```
arm64-v8a/libclash.so    - 3.15 MB - 2024/11/2 13:38:41
armeabi-v7a/libclash.so  - 2.95 MB - 2024/11/2 13:39:47
x86/libclash.so          - 2.93 MB - 2024/11/2 13:40:53
x86_64/libclash.so       - 3.28 MB - 2024/11/2 13:34:53
```

### 源代码状态

```
mihomo_core.go - 2024/11/4 5:44:05  ✅ 包含 DNS 1053 配置
main.go        - 2024/11/4 2:47:24  ✅ 最新
config.go      - 2024/11/3 22:34:49 ✅ 包含 DNS 1053 配置
```

**差距：约 2 天** → 必须重新编译才能应用 DNS 修复

---

## ✅ 验证修复是否生效

重新编译并安装后，检查日志：

```powershell
adb logcat -c  # 清除旧日志
adb logcat | Select-String "DNS|dns"
```

应该看到：
```
[DNS] Mihomo DNS server listening at 0.0.0.0:1053
[TUN] DNSHijack: any:1053
```

测试访问 YouTube，不应再出现 Facebook 证书错误。

---

## 🆘 需要帮助？

如果遇到问题：

1. 检查 GitHub Actions 构建日志
2. 查看 `DNS_PORT_FIX.md` 详细文档
3. 检查 `COMPILE_SOLUTIONS.md` 其他方案

---

**创建日期**：2024-11-04  
**状态**：等待重新编译

