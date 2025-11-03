# 自动在 WSL 中安装 Go 并编译 Android Go 核心
# 此脚本会：
# 1. 检查 WSL 是否已安装 Go，如果没有则安装
# 2. 设置环境变量
# 3. 编译所有架构的 Go 核心库

Write-Host "================================" -ForegroundColor Cyan
Write-Host "自动编译 Go 核心（WSL 环境）" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# 检查 WSL
Write-Host "[1/5] 检查 WSL..." -ForegroundColor Yellow
$wslCheck = wsl --list --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ WSL 未安装或未启动" -ForegroundColor Red
    Write-Host "请先安装 WSL: https://aka.ms/wslinstall" -ForegroundColor Red
    exit 1
}
Write-Host "✓ WSL 已就绪" -ForegroundColor Green
Write-Host ""

# 检查/安装 Go
Write-Host "[2/5] 检查/安装 Go..." -ForegroundColor Yellow
$goVersion = wsl bash -c "go version 2>/dev/null"
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Go 未安装，正在安装 Go 1.23.3..." -ForegroundColor Yellow
    
    $installScript = @'
set -e
cd /tmp
echo '  → 下载 Go 1.23.3...'
wget -q --show-progress https://go.dev/dl/go1.23.3.linux-amd64.tar.gz
echo '  → 解压安装...'
sudo rm -rf /usr/local/go
sudo tar -C /usr/local -xzf go1.23.3.linux-amd64.tar.gz
echo '  → 设置环境变量...'
if ! grep -q '/usr/local/go/bin' ~/.bashrc; then
    echo 'export PATH=$PATH:/usr/local/go/bin' >> ~/.bashrc
fi
echo '  → 清理临时文件...'
rm go1.23.3.linux-amd64.tar.gz
'@
    wsl bash -c $installScript
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ Go 安装失败" -ForegroundColor Red
        exit 1
    }
    
    $goVersion = wsl bash -c "source ~/.bashrc && go version"
}

Write-Host "✓ $goVersion" -ForegroundColor Green
Write-Host ""

# 检查 Android NDK
Write-Host "[3/5] 检查 Android NDK..." -ForegroundColor Yellow
$ndkPath = "C:\Users\Administrator\AppData\Local\Android\Sdk\ndk\25.2.9519653"
if (-not (Test-Path $ndkPath)) {
    Write-Host "✗ Android NDK 未找到: $ndkPath" -ForegroundColor Red
    Write-Host "请安装 Android NDK 25.2.9519653" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Android NDK 已找到" -ForegroundColor Green
Write-Host ""

# 设置项目路径
$projectPath = "/mnt/c/Users/Administrator/Desktop/clash-verge-rev/mobile"
$scriptPath = "$projectPath/scripts"

Write-Host "[4/5] 进入项目目录..." -ForegroundColor Yellow
Write-Host "  路径: $scriptPath" -ForegroundColor Gray
Write-Host ""

# 编译
Write-Host "[5/5] 开始编译..." -ForegroundColor Yellow
Write-Host ""

$buildScript = @"
set -e
export PATH=/usr/local/go/bin:`$PATH
export ANDROID_NDK_HOME=/mnt/c/Users/Administrator/AppData/Local/Android/Sdk/ndk/25.2.9519653
export ANDROID_HOME=/mnt/c/Users/Administrator/AppData/Local/Android/Sdk

cd $scriptPath

echo '执行 build-go.sh...'
./build-go.sh
"@

wsl bash -c $buildScript

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "================================" -ForegroundColor Green
    Write-Host "✅ 编译成功！" -ForegroundColor Green
    Write-Host "================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "编译输出位置:" -ForegroundColor Cyan
    Write-Host "  C:\Users\Administrator\Desktop\clash-verge-rev\mobile\app\src\main\jniLibs\" -ForegroundColor White
    Write-Host ""
    Write-Host "下一步:" -ForegroundColor Cyan
    Write-Host "  cd C:\Users\Administrator\Desktop\clash-verge-rev\mobile" -ForegroundColor White
    Write-Host "  .\gradlew assembleDebug" -ForegroundColor White
    Write-Host ""
    
    # 显示编译结果
    Write-Host "编译文件列表:" -ForegroundColor Cyan
    Get-ChildItem "C:\Users\Administrator\Desktop\clash-verge-rev\mobile\app\src\main\jniLibs" -Recurse -Filter "*.so" | 
        ForEach-Object { 
            $size = [math]::Round($_.Length/1MB, 2)
            Write-Host "  ✓ $($_.Directory.Name)/$($_.Name) - ${size} MB - $($_.LastWriteTime)" -ForegroundColor Green
        }
} else {
    Write-Host ""
    Write-Host "✗ 编译失败" -ForegroundColor Red
    Write-Host "请检查上面的错误信息" -ForegroundColor Red
    exit 1
}

