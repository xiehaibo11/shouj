#!/bin/bash

# 使用 Docker 在真实 Linux 环境中编译 Go 核心
# 支持所有 Android 架构：arm64-v8a, armeabi-v7a, x86_64, x86

set -e

echo "================================"
echo "使用 Docker 编译 Go 核心（全架构）"
echo "================================"
echo ""

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装"
    echo "请安装 Docker Desktop: https://www.docker.com/products/docker-desktop"
    exit 1
fi

echo "✓ Docker 已安装"

# 项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MOBILE_ROOT="$PROJECT_ROOT/mobile"

echo "✓ 项目路径: $MOBILE_ROOT"
echo ""

# 使用官方 Go + Android NDK 镜像
echo "正在启动 Docker 容器..."
docker run --rm \
    -v "$MOBILE_ROOT:/workspace" \
    -w /workspace \
    -e ANDROID_NDK_HOME=/opt/android-ndk \
    mingc/android-build-box:latest \
    bash -c "
        set -e
        
        echo '================================'
        echo '1. 设置 Go 环境'
        echo '================================'
        cd /tmp
        wget -q https://go.dev/dl/go1.23.3.linux-amd64.tar.gz
        tar -C /usr/local -xzf go1.23.3.linux-amd64.tar.gz
        export PATH=/usr/local/go/bin:\$PATH
        go version
        echo ''
        
        echo '================================'
        echo '2. 进入源码目录'
        echo '================================'
        cd /workspace/app/src/main/golang
        echo \"当前目录: \$(pwd)\"
        echo ''
        
        echo '================================'
        echo '3. 下载 Go 依赖'
        echo '================================'
        go mod download
        echo '✓ 依赖下载完成'
        echo ''
        
        # 编译函数
        build_arch() {
            local ARCH=\$1
            local GOARCH=\$2
            local GOARM=\$3
            local CC_PREFIX=\$4
            
            echo '================================'
            echo \"4. 编译 \${ARCH}\"
            echo '================================'
            
            mkdir -p ../jniLibs/\${ARCH}
            
            export GOOS=android
            export GOARCH=\${GOARCH}
            export CGO_ENABLED=1
            export CC=/opt/android-ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/\${CC_PREFIX}-clang
            
            if [ -n \"\$GOARM\" ]; then
                export GOARM=\${GOARM}
            fi
            
            echo \"  GOARCH=\${GOARCH}\"
            echo \"  CC=\${CC_PREFIX}-clang\"
            
            go build \
                -buildmode=c-shared \
                -ldflags='-s -w -extldflags=-Wl,-soname,libclash.so' \
                -tags='with_gvisor' \
                -trimpath \
                -o ../jniLibs/\${ARCH}/libclash.so \
                .
            
            rm -f ../jniLibs/\${ARCH}/libclash.h
            
            SIZE=\$(du -h ../jniLibs/\${ARCH}/libclash.so | cut -f1)
            echo \"  ✓ \${ARCH} 编译成功 (大小: \${SIZE})\"
            echo ''
        }
        
        # 编译所有架构
        build_arch 'arm64-v8a' 'arm64' '' 'aarch64-linux-android21'
        build_arch 'armeabi-v7a' 'arm' '7' 'armv7a-linux-androideabi21'
        build_arch 'x86_64' 'amd64' '' 'x86_64-linux-android21'
        build_arch 'x86' '386' '' 'i686-linux-android21'
        
        echo '================================'
        echo '编译结果'
        echo '================================'
        ls -lh ../jniLibs/*/libclash.so
    "

echo ""
echo "================================"
echo "✅ Docker 编译完成！"
echo "================================"
echo ""
echo "输出目录: $MOBILE_ROOT/app/src/main/jniLibs/"
echo ""
echo "下一步: 运行 Gradle 构建"
echo "  cd $MOBILE_ROOT && ./gradlew assembleRelease"
echo ""

