#!/bin/bash
set -e

echo "================================"
echo "在 WSL 内部编译 Go 核心"
echo "================================"
echo ""

# 设置环境变量
export PATH=/usr/local/go/bin:$PATH
export TMPDIR=/tmp

# NDK 路径
export ANDROID_NDK_HOME=/mnt/c/Users/Administrator/AppData/Local/Android/Sdk/ndk/25.2.9519653

# 创建 WSL 内部工作目录
WORK_DIR="/tmp/clash-build-$$"
echo "创建临时工作目录: $WORK_DIR"
mkdir -p "$WORK_DIR"

# 复制源代码到 WSL 内部
SOURCE_DIR="/mnt/c/Users/Administrator/Desktop/clash-verge-rev/mobile/app/src/main/golang"
echo "复制源代码到 WSL..."
cp -r "$SOURCE_DIR"/* "$WORK_DIR/"

# 进入工作目录
cd "$WORK_DIR"
echo "工作目录: $(pwd)"
echo ""

# 验证
echo "Go 版本: $(go version)"
echo "Android NDK: $ANDROID_NDK_HOME"
echo ""

# 下载依赖
echo "下载 Go 依赖..."
go mod download
echo "✓ 依赖下载完成"
echo ""

# 编译函数
build_arch() {
    local ARCH=$1
    local GOARCH=$2
    local GOARM=$3
    local CC_PREFIX=$4
    
    echo "================================"
    echo "编译 $ARCH"
    echo "================================"
    
    export GOOS=android
    export GOARCH=$GOARCH
    export CGO_ENABLED=1
    export CC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/windows-x86_64/bin/${CC_PREFIX}-clang.cmd"
    
    if [ -n "$GOARM" ]; then
        export GOARM=$GOARM
    fi
    
    echo "GOARCH=$GOARCH"
    echo "CC=${CC_PREFIX}-clang"
    
    OUTPUT_FILE="libclash-${ARCH}.so"
    
    go build \
        -buildmode=c-shared \
        -ldflags="-s -w -extldflags=-Wl,-soname,libclash.so" \
        -tags="with_gvisor" \
        -trimpath \
        -o "$OUTPUT_FILE" \
        .
    
    if [ $? -eq 0 ]; then
        SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
        echo "✓ $ARCH 编译成功 (大小: $SIZE)"
        
        # 复制到目标目录
        TARGET_DIR="/mnt/c/Users/Administrator/Desktop/clash-verge-rev/mobile/app/src/main/jniLibs/$ARCH"
        mkdir -p "$TARGET_DIR"
        cp "$OUTPUT_FILE" "$TARGET_DIR/libclash.so"
        echo "✓ 已复制到 $TARGET_DIR"
    else
        echo "✗ $ARCH 编译失败"
        return 1
    fi
    
    echo ""
}

# 编译所有架构
build_arch "arm64-v8a" "arm64" "" "aarch64-linux-android21"
build_arch "armeabi-v7a" "arm" "7" "armv7a-linux-androideabi21"
build_arch "x86_64" "amd64" "" "x86_64-linux-android21"
build_arch "x86" "386" "" "i686-linux-android21"

# 清理
echo "================================"
echo "清理临时文件..."
cd /
rm -rf "$WORK_DIR"
echo "✓ 清理完成"

echo ""
echo "================================"
echo "✅ 所有架构编译完成！"
echo "================================"

