@echo off
REM Clash Verge Rev Android - Go 核心编译脚本 (Windows)
REM 编译 Go 代码为 Android 共享库

setlocal EnableDelayedExpansion

echo ================================
echo Clash Verge Rev - Go 核心编译
echo ================================
echo.

REM 检查 Go 是否安装
where go >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 未找到 Go 编译器
    echo 请先安装 Go 1.21 或更高版本
    echo 下载地址: https://golang.org/dl/
    exit /b 1
)

for /f "tokens=3" %%i in ('go version') do set GO_VERSION=%%i
echo [+] Go 版本: %GO_VERSION%

REM 检查 Android NDK
if "%ANDROID_NDK_HOME%"=="" (
    if not "%ANDROID_HOME%"=="" (
        set "ANDROID_NDK_HOME=%ANDROID_HOME%\ndk\25.2.9519653"
    )
)

if "%ANDROID_NDK_HOME%"=="" (
    echo [错误] 未找到 Android NDK
    echo 请设置环境变量 ANDROID_NDK_HOME
    echo 或确保 NDK 安装在: %%ANDROID_HOME%%\ndk\25.2.9519653
    exit /b 1
)

if not exist "%ANDROID_NDK_HOME%" (
    echo [错误] NDK 目录不存在: %ANDROID_NDK_HOME%
    exit /b 1
)

echo [+] Android NDK: %ANDROID_NDK_HOME%

REM 进入 Go 源码目录
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "GO_SRC_DIR=%PROJECT_ROOT%\app\src\main\golang"
set "JNI_LIBS_DIR=%PROJECT_ROOT%\app\src\main\jniLibs"

cd /d "%GO_SRC_DIR%"
echo [+] 工作目录: %GO_SRC_DIR%
echo.

REM 下载 Go 依赖
echo 下载 Go 依赖...
go mod download
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 依赖下载失败
    exit /b 1
)
echo [+] 依赖下载完成
echo.

REM 设置 NDK 工具链路径
set "NDK_HOST=windows-x86_64"
set "TOOLCHAIN=%ANDROID_NDK_HOME%\toolchains\llvm\prebuilt\%NDK_HOST%\bin"

if not exist "%TOOLCHAIN%" (
    echo [错误] NDK 工具链不存在: %TOOLCHAIN%
    exit /b 1
)

REM 编译函数 (通过标签模拟)
:build_arch
set ARCH=%1
set GOARCH=%2
set GOARM=%3
set CC_PREFIX=%4

echo 编译 %ARCH%...

set "OUTPUT_DIR=%JNI_LIBS_DIR%\%ARCH%"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM 设置环境变量
set "GOOS=android"
set "GOARCH=%GOARCH%"
set "CGO_ENABLED=1"
set "CC=%TOOLCHAIN%\%CC_PREFIX%-clang.cmd"

if not "%GOARM%"=="" (
    set "GOARM=%GOARM%"
)

REM 编译
go build -buildmode=c-shared ^
    -ldflags="-s -w -extldflags=-Wl,-soname,libclash.so" ^
    -tags="with_gvisor" ^
    -trimpath ^
    -o "%OUTPUT_DIR%\libclash.so" ^
    .

if %ERRORLEVEL% EQU 0 (
    echo [+] %ARCH% 编译成功
    REM 删除 .h 文件
    if exist "%OUTPUT_DIR%\libclash.h" del "%OUTPUT_DIR%\libclash.h"
) else (
    echo [错误] %ARCH% 编译失败
    exit /b 1
)

echo.
goto :eof

REM 编译所有架构
echo 开始编译所有架构...
echo.

REM ARM64 (推荐，现代设备)
call :build_arch "arm64-v8a" "arm64" "" "aarch64-linux-android21"

REM ARMv7 (老设备)
call :build_arch "armeabi-v7a" "arm" "7" "armv7a-linux-androideabi21"

REM x86_64 (模拟器)
call :build_arch "x86_64" "amd64" "" "x86_64-linux-android21"

REM x86 (32位模拟器，可选)
call :build_arch "x86" "386" "" "i686-linux-android21"

echo ================================
echo 编译完成！
echo ================================
echo.
echo 输出目录: %JNI_LIBS_DIR%
echo.
echo 下一步: 运行 Gradle 构建
echo   cd .. ^&^& gradlew.bat assembleRelease
echo.

endlocal
