#!/bin/bash

# Mihomo 核心编译脚本
# 编译 Mihomo Go 核心为 Android 共享库

set -e

echo "🔨 Building Mihomo Core for Android"

# 检查环境变量
if [ -z "$ANDROID_NDK_HOME" ] && [ -z "$NDK_HOME" ]; then
    echo "❌ Error: ANDROID_NDK_HOME or NDK_HOME not set"
    echo "Please set one of these environment variables to your Android NDK path"
    exit 1
fi

NDK_PATH="${ANDROID_NDK_HOME:-$NDK_HOME}"
echo "✓ Using NDK: $NDK_PATH"

# 切换到 Go 源码目录
cd "$(dirname "$0")/../app/src/main/golang"

# 下载依赖
echo "📦 Downloading Go dependencies..."
go mod download
go mod tidy

# 输出目录
JNI_LIBS_DIR="../jniLibs"
mkdir -p "$JNI_LIBS_DIR"

# 编译目标架构
ARCHS=("arm64" "arm" "amd64")
ARCH_NAMES=("arm64-v8a" "armeabi-v7a" "x86_64")
CC_PREFIX=("aarch64-linux-android" "armv7a-linux-androideabi" "x86_64-linux-android")
GOARCH_NAMES=("arm64" "arm" "amd64")

# Android API 级别
API_LEVEL=21

# 选择 NDK 工具链路径
if [ "$(uname)" == "Darwin" ]; then
    TOOLCHAIN_PATH="$NDK_PATH/toolchains/llvm/prebuilt/darwin-x86_64/bin"
elif [ "$(uname)" == "Linux" ]; then
    TOOLCHAIN_PATH="$NDK_PATH/toolchains/llvm/prebuilt/linux-x86_64/bin"
else
    echo "❌ Unsupported OS: $(uname)"
    exit 1
fi

# 检查工具链路径是否存在
if [ ! -d "$TOOLCHAIN_PATH" ]; then
    echo "❌ Error: Toolchain not found at $TOOLCHAIN_PATH"
    exit 1
fi

# 编译函数
build_for_arch() {
    local idx=$1
    local arch=${ARCHS[$idx]}
    local arch_name=${ARCH_NAMES[$idx]}
    local cc_prefix=${CC_PREFIX[$idx]}
    local goarch=${GOARCH_NAMES[$idx]}
    
    echo ""
    echo "🔧 Building for $arch_name..."
    
    # 设置环境变量
    export CGO_ENABLED=1
    export GOOS=android
    export GOARCH=$goarch
    
    # ARM 需要特殊设置
    if [ "$goarch" == "arm" ]; then
        export GOARM=7
    fi
    
    # 设置 C 编译器
    export CC="$TOOLCHAIN_PATH/${cc_prefix}${API_LEVEL}-clang"
    
    # 检查编译器是否存在
    if [ ! -f "$CC" ]; then
        echo "❌ Error: Compiler not found: $CC"
        return 1
    fi
    
    # 输出目录
    OUTPUT_DIR="$JNI_LIBS_DIR/$arch_name"
    mkdir -p "$OUTPUT_DIR"
    
    # 编译
    echo "  Compiler: $CC"
    echo "  GOARCH: $GOARCH"
    echo "  Output: $OUTPUT_DIR/libclash.so"
    
    go build \
        -buildmode=c-shared \
        -trimpath \
        -ldflags="-s -w" \
        -o "$OUTPUT_DIR/libclash.so" \
        .
    
    if [ $? -eq 0 ]; then
        # 显示文件大小
        SIZE=$(du -h "$OUTPUT_DIR/libclash.so" | cut -f1)
        echo "  ✅ Built successfully: $SIZE"
        
        # 删除不需要的 .h 文件
        rm -f "$OUTPUT_DIR/libclash.h"
    else
        echo "  ❌ Build failed for $arch_name"
        return 1
    fi
}

# 询问用户要编译哪些架构
echo ""
echo "Select architectures to build:"
echo "  1) ARM64 (arm64-v8a) - Modern devices"
echo "  2) ARMv7 (armeabi-v7a) - Older devices"
echo "  3) x86_64 - Emulators"
echo "  4) All architectures"
echo ""
read -p "Enter selection (1-4) [default: 1]: " selection
selection=${selection:-1}

case $selection in
    1)
        build_for_arch 0
        ;;
    2)
        build_for_arch 1
        ;;
    3)
        build_for_arch 2
        ;;
    4)
        for i in 0 1 2; do
            build_for_arch $i || echo "⚠️  Warning: Build failed for ${ARCH_NAMES[$i]}"
        done
        ;;
    *)
        echo "❌ Invalid selection"
        exit 1
        ;;
esac

echo ""
echo "🎉 Build complete!"
echo ""
echo "Output libraries:"
ls -lh "$JNI_LIBS_DIR"/*/libclash.so 2>/dev/null || echo "No libraries found"
echo ""
echo "Next steps:"
echo "  1. Run: cd ../../../.. && ./gradlew assembleDebug"
echo "  2. Install the APK on your device"
echo "  3. Load a Mihomo config file to start using proxy features"
echo ""

