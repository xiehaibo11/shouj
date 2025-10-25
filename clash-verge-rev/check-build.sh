#!/bin/bash

echo "🔍 检查移动端构建环境..."

# 检查Node.js
if command -v node &> /dev/null; then
    echo "✅ Node.js: $(node --version)"
else
    echo "❌ Node.js 未安装"
    exit 1
fi

# 检查pnpm
if command -v pnpm &> /dev/null; then
    echo "✅ pnpm: $(pnpm --version)"
else
    echo "⚠️  pnpm 未安装，尝试安装..."
    npm install -g pnpm
fi

# 检查Java
if command -v java &> /dev/null; then
    echo "✅ Java: $(java -version 2>&1 | head -n 1)"
else
    echo "❌ Java 未安装"
    exit 1
fi

# 检查Android SDK
if [ -n "$ANDROID_HOME" ]; then
    echo "✅ Android SDK: $ANDROID_HOME"
else
    echo "❌ ANDROID_HOME 未设置"
    exit 1
fi

# 检查依赖
echo ""
echo "📦 检查依赖..."
cd "$(dirname "$0")"

if [ ! -d "node_modules" ]; then
    echo "⚠️  依赖未安装，开始安装..."
    pnpm install
else
    echo "✅ 依赖已安装"
fi

# 检查Android项目
echo ""
echo "🤖 检查Android项目..."
if [ -f "android/gradlew" ]; then
    echo "✅ Gradle wrapper 存在"
    cd android
    chmod +x gradlew
    
    echo ""
    echo "🔨 尝试构建Debug APK..."
    ./gradlew assembleDebug --stacktrace
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ 构建成功！"
        echo "📱 APK位置: android/app/build/outputs/apk/debug/app-debug.apk"
    else
        echo ""
        echo "❌ 构建失败，请查看上面的错误信息"
        exit 1
    fi
else
    echo "❌ android/gradlew 不存在"
    exit 1
fi

