#!/bin/bash

# Clash Verge Rev Android - Go 核心编译脚本 (Linux/macOS)
# 编译 Go 代码为 Android 共享库

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Clash Verge Rev - Go 核心编译${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

# 检查 Go 是否安装
if ! command -v go &> /dev/null; then
    echo -e "${RED}错误: 未找到 Go 编译器${NC}"
    echo "请先安装 Go 1.21 或更高版本"
    echo "下载地址: https://golang.org/dl/"
    exit 1
fi

GO_VERSION=$(go version | awk '{print $3}')
echo -e "${GREEN}✓${NC} Go 版本: $GO_VERSION"

# 检查 Android NDK
if [ -z "$ANDROID_NDK_HOME" ]; then
    if [ -n "$ANDROID_HOME" ]; then
        # 尝试从 ANDROID_HOME 推断
        ANDROID_NDK_HOME="$ANDROID_HOME/ndk/25.2.9519653"
    fi
fi

if [ -z "$ANDROID_NDK_HOME" ] || [ ! -d "$ANDROID_NDK_HOME" ]; then
    echo -e "${RED}错误: 未找到 Android NDK${NC}"
    echo "请设置环境变量 ANDROID_NDK_HOME"
    echo "或确保 NDK 安装在: \$ANDROID_HOME/ndk/25.2.9519653"
    exit 1
fi

echo -e "${GREEN}✓${NC} Android NDK: $ANDROID_NDK_HOME"

# 进入 Go 源码目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
GO_SRC_DIR="$PROJECT_ROOT/app/src/main/golang"
JNI_LIBS_DIR="$PROJECT_ROOT/app/src/main/jniLibs"

cd "$GO_SRC_DIR"
echo -e "${GREEN}✓${NC} 工作目录: $GO_SRC_DIR"
echo ""

# 下载 Go 依赖
echo -e "${YELLOW}下载 Go 依赖...${NC}"
go mod download
echo -e "${GREEN}✓${NC} 依赖下载完成"
echo ""

# 编译函数
build_for_arch() {
    local ARCH=$1
    local GOARCH=$2
    local GOARM=$3
    local CC_PREFIX=$4
    
    echo -e "${YELLOW}编译 ${ARCH}...${NC}"
    
    OUTPUT_DIR="$JNI_LIBS_DIR/$ARCH"
    mkdir -p "$OUTPUT_DIR"
    
    # 设置环境变量
    export GOOS=android
    export GOARCH=$GOARCH
    export CGO_ENABLED=1
    export CC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$(uname | tr '[:upper:]' '[:lower:]')-x86_64/bin/${CC_PREFIX}-clang"
    
    if [ -n "$GOARM" ]; then
        export GOARM=$GOARM
    fi
    
    # 编译
    go build -buildmode=c-shared \
        -ldflags="-s -w -extldflags=-Wl,-soname,libclash.so" \
        -tags="with_gvisor" \
        -trimpath \
        -o "$OUTPUT_DIR/libclash.so" \
        .
    
    if [ $? -eq 0 ]; then
        # 获取文件大小
        SIZE=$(du -h "$OUTPUT_DIR/libclash.so" | cut -f1)
        echo -e "${GREEN}✓${NC} ${ARCH} 编译成功 (大小: $SIZE)"
        
        # 删除 .h 文件
        rm -f "$OUTPUT_DIR/libclash.h"
    else
        echo -e "${RED}✗${NC} ${ARCH} 编译失败"
        return 1
    fi
    
    echo ""
}

# 编译所有架构
echo -e "${GREEN}开始编译所有架构...${NC}"
echo ""

# ARM64 (推荐，现代设备)
build_for_arch "arm64-v8a" "arm64" "" "aarch64-linux-android21"

# ARMv7 (老设备)
build_for_arch "armeabi-v7a" "arm" "7" "armv7a-linux-androideabi21"

# x86_64 (模拟器)
build_for_arch "x86_64" "amd64" "" "x86_64-linux-android21"

# x86 (32位模拟器，可选)
build_for_arch "x86" "386" "" "i686-linux-android21"

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}编译完成！${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo -e "输出目录: ${YELLOW}$JNI_LIBS_DIR${NC}"
echo ""
echo "下一步: 运行 Gradle 构建"
echo "  cd .. && ./gradlew assembleRelease"
echo ""
