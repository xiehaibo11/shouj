# Clash Verge Mobile

<div align="center">
  <img src="mobile/android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Clash Verge Mobile" width="128" />
  
  <h3>基于 Clash 的 Android 代理客户端</h3>
  
  [![Android Build](https://github.com/xiehaibo11/shouj/actions/workflows/android-build.yml/badge.svg)](https://github.com/xiehaibo11/shouj/actions/workflows/android-build.yml)
  [![License](https://img.shields.io/github/license/xiehaibo11/shouj)](./LICENSE)
  [![Release](https://img.shields.io/github/v/release/xiehaibo11/shouj)](https://github.com/xiehaibo11/shouj/releases)
</div>

## ✨ 特性

- 🚀 **完整的 VPN 功能** - 基于 Android VPN API 实现
- 📊 **流量统计** - 实时显示上传/下载速度和总流量
- 🎯 **代理管理** - 支持多种代理协议，灵活切换节点
- 🔍 **节点测速** - 单个和批量延迟测试
- 🔄 **Clash 模式** - Rule/Global/Direct 三种模式切换
- 📱 **通知栏控制** - 快速启动/停止 VPN
- ⚡ **快捷设置磁贴** - 下拉快捷面板快速切换
- 🎨 **Material Design** - 现代化的 UI 设计
- 🌐 **配置管理** - 支持订阅链接和本地配置
- 📈 **连接监控** - 实时查看活动连接

## 📱 系统要求

- Android 7.0 (API 24) 或更高版本
- 推荐 Android 10+ 以获得最佳体验

## 📦 下载安装

### 方式 1: GitHub Releases（推荐）
访问 [Releases 页面](https://github.com/xiehaibo11/shouj/releases) 下载最新版本的 APK

### 方式 2: GitHub Actions 构建
1. 访问 [Actions 页面](https://github.com/xiehaibo11/shouj/actions)
2. 选择最新的成功构建
3. 下载 Artifacts 中的 APK 文件

### 安装步骤
1. 下载 APK 文件到手机
2. 打开文件管理器，点击 APK 文件
3. 允许"未知来源"安装（如需要）
4. 完成安装

## 🚀 快速开始

### 1. 启动应用
首次启动会请求 VPN 权限，请点击"确定"授权

### 2. 添加配置
- 点击"配置"标签
- 点击"+"按钮
- 输入订阅链接或导入本地配置
- 点击"保存"

### 3. 选择节点
- 点击"代理"标签
- 选择代理组
- 点击想要使用的节点

### 4. 启动 VPN
- 返回"首页"
- 点击"连接"按钮
- VPN 启动成功后，状态栏会显示 VPN 图标

## 📖 功能说明

### 首页
- **VPN 状态** - 显示当前连接状态
- **Clash 模式切换** - Rule/Global/Direct
- **流量统计** - 实时上传/下载速度
- **Clash 信息** - 核心版本、端口、运行时间
- **IP 信息** - 当前 IP、国家、ISP

### 代理
- **节点列表** - 显示所有可用节点
- **搜索** - 快速查找节点
- **排序** - 按延迟或名称排序
- **过滤** - 筛选可用/不可用节点
- **批量测速** - 一键测试所有节点延迟

### 配置
- **订阅管理** - 添加/更新/删除订阅
- **本地配置** - 导入本地 YAML 文件
- **自动更新** - 定时更新订阅

### 连接
- **活动连接** - 查看所有活动连接
- **连接详情** - 域名、IP、流量、时长
- **关闭连接** - 单个或批量关闭

### 规则
- **规则列表** - 显示所有路由规则
- **规则搜索** - 快速查找规则

### 日志
- **实时日志** - 查看 Clash 核心日志
- **日志级别** - Info/Warning/Error

### 设置
- **常规** - 开机自启、自动连接、深色模式
- **代理** - 混合端口、Allow LAN、IPv6
- **性能** - 省电模式、数据压缩
- **高级** - DNS、日志级别
- **备份** - 导出/导入配置

## 🔧 开发构建

### 环境要求
- Node.js 18+
- JDK 17
- Android SDK (API 34)
- Android NDK

### 克隆项目
```bash
git clone https://github.com/xiehaibo11/shouj.git
cd shouj
```

### 安装依赖
```bash
cd clash-verge-rev
npm install
```

### 构建 APK
```bash
cd clash-verge-rev/android
./gradlew assembleDebug    # Debug 版本
./gradlew assembleRelease  # Release 版本
```

### 运行到设备
```bash
# 连接 Android 设备或启动模拟器
adb devices

# 安装并运行
cd clash-verge-rev/android
./gradlew installDebug
adb shell am start -n io.github.clashverge.mobile/.MainActivity
```

## 📚 文档

- [功能对比](clash-verge-rev/FEATURE_COMPARISON.md) - 桌面端与移动端功能对比
- [实现总结](clash-verge-rev/IMPLEMENTATION_SUMMARY.md) - 详细的实现说明
- [闪退排查](clash-verge-rev/CRASH_FIX_GUIDE.md) - 问题诊断和解决方案
- [快速开始](clash-verge-rev/QUICKSTART.md) - 新手入门指南
- [开发指南](clash-verge-rev/DEVELOPMENT.md) - 开发者文档

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 贡献指南
1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 GPL-3.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🙏 致谢

- [Clash](https://github.com/Dreamacro/clash) - 强大的代理内核
- [Clash Meta](https://github.com/MetaCubeX/mihomo) - Clash 的增强版本
- [React Native](https://reactnative.dev/) - 跨平台移动应用框架
- [Material Design](https://material.io/) - 优秀的设计系统

## ⚠️ 免责声明

本项目仅供学习交流使用，请勿用于非法用途。使用本软件所产生的一切后果由使用者自行承担。

## 📮 联系方式

- GitHub Issues: [提交问题](https://github.com/xiehaibo11/shouj/issues)
- Email: xiehaibo11@example.com

---

<div align="center">
  Made with ❤️ by Clash Verge Mobile Team
</div>
