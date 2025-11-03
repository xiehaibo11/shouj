#!/bin/bash
set -e

echo "设置环境变量..."
export PATH=/usr/local/go/bin:$PATH
export ANDROID_NDK_HOME=/mnt/c/Users/Administrator/AppData/Local/Android/Sdk/ndk/25.2.9519653
export ANDROID_HOME=/mnt/c/Users/Administrator/AppData/Local/Android/Sdk
export TMPDIR=/tmp
export TEMP=/tmp
export TMP=/tmp

echo "验证 Go 安装..."
go version

echo "切换到项目目录..."
cd /mnt/c/Users/Administrator/Desktop/clash-verge-rev/mobile/scripts

echo "开始编译..."
./build-go.sh

