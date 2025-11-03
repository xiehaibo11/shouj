# Go 核心编译解决方案

## 当前状态

- ✅ Go 1.21.5 已安装（Windows）
- ✅ Android NDK 25.2.9519653 已安装
- ✅ 源代码已更新（DNS 1053 配置）
- ⚠️ 已编译的 .so 文件过旧（2024/11/2）
- ⚠️ Docker Desktop 无法启动
- ⚠️ WSL 中未安装 Go

---

## 🚀 推荐解决方案

### 方案 1：在 WSL 中安装 Go 并编译（推荐）

这是最可靠的方法，因为交叉编译 Android 库在 Linux 环境中更稳定。

#### 步骤：

```powershell
# 1. 在 WSL 中安装 Go
wsl bash -c "
cd /tmp &&
wget -q https://go.dev/dl/go1.23.3.linux-amd64.tar.gz &&
sudo rm -rf /usr/local/go &&
sudo tar -C /usr/local -xzf go1.23.3.linux-amd64.tar.gz &&
echo 'export PATH=\$PATH:/usr/local/go/bin' >> ~/.bashrc &&
source ~/.bashrc &&
go version
"

# 2. 设置环境变量并编译
wsl bash -c "
export ANDROID_NDK_HOME=/mnt/c/Users/Administrator/AppData/Local/Android/Sdk/ndk/25.2.9519653 &&
export ANDROID_HOME=/mnt/c/Users/Administrator/AppData/Local/Android/Sdk &&
cd /mnt/c/Users/Administrator/Desktop/clash-verge-rev/mobile/scripts &&
./build-go.sh
"
```

---

### 方案 2：修复 Docker Desktop 并使用容器编译

如果 Docker Desktop 问题可以解决：

```powershell
# 1. 重启 Docker Desktop
# 方法1: 通过任务管理器结束 Docker Desktop 进程，然后重新启动
# 方法2: 重启 WSL
wsl --shutdown
# 然后启动 Docker Desktop

# 2. 清理并重试
docker system prune -a -f
docker pull mingc/android-build-box:latest

# 3. 编译
cd c:\Users\Administrator\Desktop\clash-verge-rev\mobile\scripts
bash build-docker.sh
```

---

### 方案 3：使用 GitHub Actions 在线编译

利用项目的 CI/CD 流程：

#### 步骤：

1. 提交当前的源代码更改到 Git
2. 推送到 GitHub
3. 触发 Android 构建 Workflow
4. 下载编译好的 APK

```powershell
cd c:\Users\Administrator\Desktop\clash-verge-rev

# 提交更改
git add mobile/app/src/main/golang/*.go
git add mobile/test-config.yaml
git add mobile/app/src/main/java/.../CreateLocalConfigDialog.kt
git commit -m "fix: 修改 DNS 监听端口从 53 到 1053 解决证书错误"

# 推送（将触发 CI 构建）
git push
```

然后在 GitHub Actions 中查看构建进度并下载 APK。

---

### 方案 4：快速单架构编译（仅用于测试）

如果只需要测试模拟器（x86_64），可以尝试简化编译：

```powershell
# 在当前 PowerShell 中手动编译 x86_64
cd c:\Users\Administrator\Desktop\clash-verge-rev\mobile\app\src\main\golang

$env:GOOS = "android"
$env:GOARCH = "amd64"
$env:CGO_ENABLED = "1"
$env:CC = "C:\Users\Administrator\AppData\Local\Android\Sdk\ndk\25.2.9519653\toolchains\llvm\prebuilt\windows-x86_64\bin\x86_64-linux-android21-clang.cmd"

go build `
    -buildmode=c-shared `
    -ldflags="-s -w" `
    -tags="with_gvisor" `
    -trimpath `
    -o ..\jniLibs\x86_64\libclash.so `
    .
```

⚠️ **注意**：此方法可能因 Windows CGO 环境问题而失败。

---

## 📊 方案对比

| 方案 | 难度 | 成功率 | 编译时间 | 推荐度 |
|------|------|--------|---------|--------|
| WSL + Go | 中 | ⭐⭐⭐⭐⭐ | 15-20分钟 | ⭐⭐⭐⭐⭐ |
| Docker | 低 | ⭐⭐⭐⭐ | 20-30分钟 | ⭐⭐⭐⭐ |
| GitHub Actions | 低 | ⭐⭐⭐⭐⭐ | 30-45分钟 | ⭐⭐⭐ |
| Windows 直接编译 | 高 | ⭐⭐ | 10分钟 | ⭐ |

---

## 🎯 我的推荐

**立即执行：方案 1（WSL + Go）**

理由：
1. ✅ 环境最接近 Linux（原生编译环境）
2. ✅ 一次设置，永久使用
3. ✅ 支持所有架构编译
4. ✅ 避免 Docker 复杂性

---

## 🛠️ 自动化脚本

我可以为您创建一个一键安装和编译脚本。是否需要？


