@echo off
REM Mihomo 核心编译脚本 (Windows)
REM 编译 Mihomo Go 核心为 Android 共享库

setlocal enabledelayedexpansion

echo.
echo 🔨 Building Mihomo Core for Android (Windows)
echo.

REM 检查环境变量
if "%ANDROID_NDK_HOME%"=="" (
    if "%NDK_HOME%"=="" (
        echo ❌ Error: ANDROID_NDK_HOME or NDK_HOME not set
        echo Please set one of these environment variables to your Android NDK path
        exit /b 1
    )
    set NDK_PATH=%NDK_HOME%
) else (
    set NDK_PATH=%ANDROID_NDK_HOME%
)

echo ✓ Using NDK: %NDK_PATH%
echo.

REM 切换到 Go 源码目录
cd /d "%~dp0\..\app\src\main\golang"

REM 下载依赖
echo 📦 Downloading Go dependencies...
go mod download
go mod tidy

REM 输出目录
set JNI_LIBS_DIR=..\jniLibs

REM Android API 级别
set API_LEVEL=21

REM NDK 工具链路径
set TOOLCHAIN_PATH=%NDK_PATH%\toolchains\llvm\prebuilt\windows-x86_64\bin

REM 检查工具链路径是否存在
if not exist "%TOOLCHAIN_PATH%" (
    echo ❌ Error: Toolchain not found at %TOOLCHAIN_PATH%
    exit /b 1
)

echo.
echo Select architectures to build:
echo   1) ARM64 (arm64-v8a) - Modern devices
echo   2) ARMv7 (armeabi-v7a) - Older devices
echo   3) x86_64 - Emulators
echo   4) All architectures
echo.
set /p selection="Enter selection (1-4) [default: 1]: "
if "%selection%"=="" set selection=1

if "%selection%"=="1" goto build_arm64
if "%selection%"=="2" goto build_armv7
if "%selection%"=="3" goto build_x86_64
if "%selection%"=="4" goto build_all
echo ❌ Invalid selection
exit /b 1

:build_arm64
echo.
echo 🔧 Building for ARM64 (arm64-v8a)...
call :build_arch arm64-v8a arm64 aarch64-linux-android 0
goto end

:build_armv7
echo.
echo 🔧 Building for ARMv7 (armeabi-v7a)...
call :build_arch armeabi-v7a arm armv7a-linux-androideabi 7
goto end

:build_x86_64
echo.
echo 🔧 Building for x86_64...
call :build_arch x86_64 amd64 x86_64-linux-android 0
goto end

:build_all
echo.
echo 🔧 Building for ARM64 (arm64-v8a)...
call :build_arch arm64-v8a arm64 aarch64-linux-android 0
echo.
echo 🔧 Building for ARMv7 (armeabi-v7a)...
call :build_arch armeabi-v7a arm armv7a-linux-androideabi 7
echo.
echo 🔧 Building for x86_64...
call :build_arch x86_64 amd64 x86_64-linux-android 0
goto end

:build_arch
set ARCH_NAME=%1
set GOARCH=%2
set CC_PREFIX=%3
set GOARM=%4

REM 移除引号
set ARCH_NAME=%ARCH_NAME:"=%
set GOARCH=%GOARCH:"=%
set CC_PREFIX=%CC_PREFIX:"=%
set GOARM=%GOARM:"=%

set OUTPUT_DIR=%JNI_LIBS_DIR%\%ARCH_NAME%
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

set CGO_ENABLED=1
set GOOS=android
set CC=%TOOLCHAIN_PATH%\%CC_PREFIX%%API_LEVEL%-clang.cmd

if not exist "%CC%" (
    echo   ❌ Error: Compiler not found: %CC%
    exit /b 1
)

echo   Compiler: %CC%
echo   GOARCH: %GOARCH%
echo   Output: %OUTPUT_DIR%\libclash.so

go build -buildmode=c-shared -trimpath -ldflags="-s -w" -o "%OUTPUT_DIR%\libclash.so" .

if %ERRORLEVEL% EQU 0 (
    echo   ✅ Built successfully
    del /q "%OUTPUT_DIR%\libclash.h" 2>nul
) else (
    echo   ❌ Build failed for %ARCH_NAME%
    exit /b 1
)
exit /b 0

:end
echo.
echo 🎉 Build complete!
echo.
echo Output libraries:
dir /b "%JNI_LIBS_DIR%\*\libclash.so" 2>nul
echo.
echo Next steps:
echo   1. Run: cd ..\..\..\.. ^&^& gradlew.bat assembleDebug
echo   2. Install the APK on your device
echo   3. Load a Mihomo config file to start using proxy features
echo.

