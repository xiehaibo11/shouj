# Clash Verge Rev Mobile 使用指南

## 🚀 快速开始

### 1. 准备配置文件

应用启动时会创建默认配置，但**默认配置只有 DIRECT（直连）**，无法访问被墙网站。

您需要：

#### 方法 A：导入订阅链接（推荐）
1. 打开应用
2. 点击右上角 ⚙️ 设置
3. 选择「配置管理」
4. 点击「添加订阅」
5. 粘贴您的 Clash 订阅链接
6. 点击「更新订阅」

#### 方法 B：手动配置节点
1. 在手机上创建配置文件：`/sdcard/Android/data/io.github.clash_verge_rev.clash_verge_rev/files/config.yaml`
2. 使用以下示例配置：

```yaml
# Clash 配置示例
mixed-port: 7897
allow-lan: false
mode: rule
log-level: info
ipv6: true
external-controller: 127.0.0.1:9090
secret: ""

dns:
  enable: true
  listen: 0.0.0.0:1053
  enhanced-mode: fake-ip
  nameserver:
    - 8.8.8.8
    - 1.1.1.1
    - 223.5.5.5

# ⚠️ 这里需要添加您的真实代理节点
proxies:
  # 示例 1: Shadowsocks
  - name: "🇭🇰 香港节点"
    type: ss
    server: your-server.com
    port: 8388
    cipher: aes-256-gcm
    password: your-password
    udp: true

  # 示例 2: VMess
  - name: "🇺🇸 美国节点"
    type: vmess
    server: your-server.com
    port: 443
    uuid: your-uuid
    alterId: 0
    cipher: auto
    tls: true
    network: ws
    ws-opts:
      path: /path
      headers:
        Host: your-server.com

  # 示例 3: Trojan
  - name: "🇯🇵 日本节点"
    type: trojan
    server: your-server.com
    port: 443
    password: your-password
    udp: true
    sni: your-server.com

proxy-groups:
  - name: "🚀 节点选择"
    type: select
    proxies:
      - "🇭🇰 香港节点"
      - "🇺🇸 美国节点"
      - "🇯🇵 日本节点"
      - DIRECT

  - name: "🌍 国外网站"
    type: select
    proxies:
      - "🚀 节点选择"
      - DIRECT

rules:
  # 国内直连
  - DOMAIN-SUFFIX,cn,DIRECT
  - DOMAIN-KEYWORD,baidu,DIRECT
  - GEOIP,CN,DIRECT
  
  # 其他走代理
  - MATCH,🌍 国外网站
```

### 2. 启动 VPN

1. 在应用主页点击「开启」按钮
2. 首次启动会请求 VPN 权限，点击「确定」
3. 状态栏出现 🔑 图标表示 VPN 已启动
4. 在「代理」页面可以切换节点

### 3. 测试连接

启动 VPN 后：

1. 打开浏览器访问 https://google.com 测试
2. 在应用中点击「连接」查看活动连接
3. 在「日志」页面查看详细日志

## 📱 功能说明

### 主页
- **开启/关闭 VPN**：一键启动或停止代理
- **网络状态**：显示当前网络连接状态
- **流量统计**：实时显示上传/下载流量

### 代理页面
- **节点列表**：显示所有可用代理节点
- **切换节点**：点击节点切换代理
- **延迟测试**：测试节点响应速度
- **代理组**：支持节点分组管理

### 连接页面
- **活动连接**：实时显示所有网络连接
- **流量监控**：查看每个连接的流量
- **关闭连接**：手动关闭指定连接
- **搜索过滤**：按主机名、IP、进程搜索

### 规则页面
- **规则列表**：显示所有匹配规则
- **规则匹配**：查看流量匹配的规则
- **规则统计**：统计各规则匹配次数

### 日志页面
- **实时日志**：显示 Mihomo 核心日志
- **日志过滤**：按级别过滤日志
- **日志导出**：导出日志用于调试

## ⚠️ 常见问题

### 1. 为什么访问网站失败？

**错误代码 -130 / ERR_PROXY_CONNECTION_FAILED**

原因：
- ❌ 没有配置真实代理节点（默认只有 DIRECT）
- ❌ 代理节点信息错误或已失效
- ❌ 网络连接问题

解决方法：
1. 检查配置文件是否包含有效的代理节点
2. 在「代理」页面测试节点延迟
3. 尝试切换到其他节点
4. 查看「日志」页面的错误信息

### 2. VPN 已开启但无法上网

检查：
1. 配置文件路径是否正确
2. 代理节点是否可用（测试延迟）
3. DNS 是否正确配置
4. 查看日志是否有错误

### 3. 如何查看详细日志？

1. 打开应用
2. 点击底部「日志」标签
3. 查看实时日志输出
4. 或通过 ADB 查看：`adb logcat -s ClashCore-Go:I ClashCore:I`

### 4. 如何添加订阅？

目前版本需要手动配置。后续版本将支持：
- 📋 订阅链接导入
- 🔄 自动更新订阅
- ☁️ 配置云同步

## 🔧 高级配置

### 配置文件位置

```
/sdcard/Android/data/io.github.clash_verge_rev.clash_verge_rev/files/config.yaml
```

### 通过 ADB 推送配置

```bash
# 推送配置文件
adb push config.yaml /sdcard/Android/data/io.github.clash_verge_rev.clash_verge_rev/files/config.yaml

# 重启应用使配置生效
adb shell am force-stop io.github.clash_verge_rev.clash_verge_rev
adb shell am start -n io.github.clash_verge_rev.clash_verge_rev/.MainActivity
```

### 测试配置

使用之前创建的 `test-config.yaml`：

```bash
adb push test-config.yaml /sdcard/Android/data/io.github.clash_verge_rev.clash_verge_rev/files/config.yaml
```

## 📝 配置模板

### 最小配置（仅直连）

```yaml
mixed-port: 7897
mode: rule
dns:
  enable: true
  nameserver: [8.8.8.8, 1.1.1.1]
proxies:
  - {name: DIRECT, type: direct}
proxy-groups:
  - {name: PROXY, type: select, proxies: [DIRECT]}
rules:
  - MATCH,PROXY
```

### 完整配置（包含代理）

参考上面的「方法 B：手动配置节点」部分。

## 🆘 获取帮助

如果遇到问题：

1. **查看日志**：应用内日志页面或 ADB 日志
2. **检查配置**：确保 YAML 格式正确
3. **测试节点**：在代理页面测试延迟
4. **提交 Issue**：https://github.com/clash-verge-rev/clash-verge-rev/issues

---

## ✅ 成功案例

配置正确后，您应该看到：

- ✅ VPN 状态显示「运行中」
- ✅ 代理页面显示节点列表
- ✅ 可以访问 Google、GitHub 等网站
- ✅ 连接页面显示活动连接
- ✅ 流量统计正常更新

如果以上都正常，恭喜您已成功配置 Clash Verge Rev Mobile！🎉


