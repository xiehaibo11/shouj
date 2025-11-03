#!/bin/bash

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "================================"
echo "编译 x86_64 (模拟器)"
echo "================================"

export PATH=/usr/local/go/bin:$PATH
export ANDROID_NDK_HOME="/mnt/c/Users/Administrator/AppData/Local/Android/Sdk/ndk/25.2.9519653"

# 检测 WSL
if grep -qi microsoft /proc/version 2>/dev/null; then
    NDK_HOST="windows-x86_64"
else
    NDK_HOST="$(uname | tr '[:upper:]' '[:lower:]')-x86_64"
fi

echo "NDK Host: $NDK_HOST"

# 进入源码目录
cd /mnt/c/Users/Administrator/Desktop/clash-verge-rev/mobile/app/src/main/golang

# 下载依赖
echo "下载依赖..."
go mod download
go mod tidy

# 创建输出目录
OUTPUT_DIR="../jniLibs/x86_64"
mkdir -p "$OUTPUT_DIR"

# 设置环境变量
export GOOS=android
export GOARCH=amd64
export CGO_ENABLED=1
export CC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/${NDK_HOST}/bin/x86_64-linux-android21-clang"
# WSL 中使用 Windows 可访问的临时目录
export TMPDIR="/mnt/c/Users/Administrator/AppData/Local/Temp"
export TMP="$TMPDIR"
export TEMP="$TMPDIR"

echo "开始编译..."
echo "CC: $CC"

# 编译
go build \
    -buildmode=c-shared \
    -ldflags="-s -w -extldflags=-Wl,-soname,libclash.so" \
    -tags="with_gvisor" \
    -trimpath \
    -o "$OUTPUT_DIR/libclash.so" \
    .

# 删除头文件
rm -f "$OUTPUT_DIR/libclash.h"

echo -e "${GREEN}✅ 编译成功！${NC}"
ls -lh "$OUTPUT_DIR/libclash.so"

