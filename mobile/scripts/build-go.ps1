# Clash Verge Rev Android - Go Build Script for Windows
# Compiles Go code to Android shared library

Write-Host "================================" -ForegroundColor Green
Write-Host "Clash Verge Rev - Go Build" -ForegroundColor Green  
Write-Host "================================" -ForegroundColor Green
Write-Host ""

# Check Go installation
try {
    $goVersion = go version
    Write-Host "[OK] Go version: $goVersion" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Go compiler not found" -ForegroundColor Red
    Write-Host "Please install Go 1.21 or higher from: https://golang.org/dl/"
    exit 1
}

# Check Android NDK
$ndkHome = $env:ANDROID_NDK_HOME
if ([string]::IsNullOrEmpty($ndkHome) -and ![string]::IsNullOrEmpty($env:ANDROID_HOME)) {
    $ndkHome = Join-Path $env:ANDROID_HOME "ndk\25.2.9519653"
}

if ([string]::IsNullOrEmpty($ndkHome) -or !(Test-Path $ndkHome)) {
    Write-Host "[ERROR] Android NDK not found" -ForegroundColor Red
    Write-Host "Please set ANDROID_NDK_HOME environment variable"
    exit 1
}

Write-Host "[OK] Android NDK: $ndkHome" -ForegroundColor Green

# Navigate to Go source directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$goSrcDir = Join-Path $projectRoot "app\src\main\golang"
$jniLibsDir = Join-Path $projectRoot "app\src\main\jniLibs"

Set-Location $goSrcDir
Write-Host "[OK] Working directory: $goSrcDir" -ForegroundColor Green
Write-Host ""

# Download dependencies
Write-Host "Downloading Go dependencies..." -ForegroundColor Yellow
go mod download
Write-Host "[OK] Dependencies downloaded" -ForegroundColor Green
Write-Host ""

# Build for x86_64 (emulator)
Write-Host "Building for x86_64 (emulator)..." -ForegroundColor Yellow
$outputDir = Join-Path $jniLibsDir "x86_64"
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

$env:GOOS = "android"
$env:GOARCH = "amd64"
$env:CGO_ENABLED = "1"
$env:CC = Join-Path $ndkHome "toolchains\llvm\prebuilt\windows-x86_64\bin\x86_64-linux-android21-clang.cmd"

$outputFile = Join-Path $outputDir "libclash.so"

go build -buildmode=c-shared -ldflags="-s -w -extldflags=-Wl,-soname,libclash.so" -tags="with_gvisor" -trimpath -o $outputFile .

if ($LASTEXITCODE -eq 0) {
    $size = (Get-Item $outputFile).Length / 1MB
    $sizeStr = "{0:N2}" -f $size
    Write-Host "[OK] x86_64 compiled successfully (size: $sizeStr MB)" -ForegroundColor Green
    
    # Remove .h file
    $headerFile = Join-Path $outputDir "libclash.h"
    if (Test-Path $headerFile) {
        Remove-Item $headerFile
    }
    
    Write-Host ""
    Write-Host "================================" -ForegroundColor Green
    Write-Host "Build completed!" -ForegroundColor Green
    Write-Host "================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Output directory: $jniLibsDir" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Next step: Run Gradle build and install"
    Write-Host "  .\gradlew.bat assembleDebug installDebug" -ForegroundColor Cyan
    Write-Host ""
} else {
    Write-Host "[ERROR] Build failed!" -ForegroundColor Red
    exit 1
}
