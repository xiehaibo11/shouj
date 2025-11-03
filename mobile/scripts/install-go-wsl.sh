#!/bin/bash

set -e

echo "📦 在 WSL 中安装 Go 1.23..."

cd ~
wget -q https://go.dev/dl/go1.23.3.linux-amd64.tar.gz
sudo rm -rf /usr/local/go
sudo tar -C /usr/local -xzf go1.23.3.linux-amd64.tar.gz
rm go1.23.3.linux-amd64.tar.gz

# 添加到 PATH
if ! grep -q "/usr/local/go/bin" ~/.bashrc; then
    echo 'export PATH=$PATH:/usr/local/go/bin' >> ~/.bashrc
fi

export PATH=$PATH:/usr/local/go/bin
go version

echo "✅ Go 安装完成"

