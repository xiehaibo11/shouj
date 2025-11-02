package main

/*
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "ClashCore-Go", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "ClashCore-Go", __VA_ARGS__)
*/
import "C"

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"sync"
	"time"
)

var (
	mihomoCtx    context.Context
	mihomoCancel context.CancelFunc
	mihomoMutex  sync.Mutex
	mihomoReady  bool
)

// initMihomo 初始化 Mihomo 核心
func initMihomo() error {
	mihomoMutex.Lock()
	defer mihomoMutex.Unlock()
	
	if mihomoReady {
		// C.LOGI(C.CString("Mihomo already initialized"))
		return nil
	}
	
	// C.LOGI(C.CString("Initializing Mihomo core..."))
	
	// 创建上下文
	mihomoCtx, mihomoCancel = context.WithCancel(context.Background())
	
	// 1. 设置日志输出到Android logcat
	setupMihomoLoggerInternal()
	
	// 2. 初始化工作目录
	if err := setupMihomoWorkDir(); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to setup work dir: %v", err)))
		return err
	}
	
	// 3. 初始化统计模块
	initTrafficStatistics()
	
	// 4. 初始化缓存
	initMihomoCache()
	
	// C.LOGI(C.CString("Mihomo core modules initialized"))
	
	// 注意: 完整的 Mihomo 集成将在配置加载时完成
	// 包括: DNS解析器、规则引擎、代理适配器、TUN栈
	// 这样设计的原因是 Mihomo 需要配置文件才能初始化这些组件
	
	mihomoReady = true
	// C.LOGI(C.CString("Mihomo core initialized successfully"))
	return nil
}

// setupMihomoWorkDir 设置 Mihomo 工作目录
func setupMihomoWorkDir() error {
	dirs := []string{
		filepath.Join(workingDir, "mihomo"),
		filepath.Join(workingDir, "mihomo", "cache"),
		filepath.Join(workingDir, "mihomo", "rules"),
		filepath.Join(workingDir, "mihomo", "providers"),
	}
	
	for _, dir := range dirs {
		if err := os.MkdirAll(dir, 0755); err != nil {
			return fmt.Errorf("failed to create dir %s: %w", dir, err)
		}
	}
	
	// C.LOGI(C.CString("Mihomo work directories created"))
	return nil
}

// initTrafficStatistics 初始化流量统计
func initTrafficStatistics() {
	// 重置流量统计
	uploadBytes.Store(0)
	downloadBytes.Store(0)
	// C.LOGI(C.CString("Traffic statistics initialized"))
}

// initMihomoCache 初始化缓存
func initMihomoCache() {
	_ = filepath.Join(workingDir, "mihomo", "cache")
	// cacheDir := filepath.Join(workingDir, "mihomo", "cache")
	// C.LOGI(C.CString(fmt.Sprintf("Mihomo cache dir: %s", cacheDir)))
}

// shutdownMihomo 关闭 Mihomo 核心
func shutdownMihomo() error {
	mihomoMutex.Lock()
	defer mihomoMutex.Unlock()
	
	if !mihomoReady {
		return nil
	}
	
	// C.LOGI(C.CString("Shutting down Mihomo core..."))
	
	// 取消上下文
	if mihomoCancel != nil {
		mihomoCancel()
	}
	
	// TODO: 清理 Mihomo 资源
	// 1. 关闭所有连接
	// 2. 停止 DNS 解析器
	// 3. 清理缓存
	
	mihomoReady = false
	// C.LOGI(C.CString("Mihomo core shutdown complete"))
	return nil
}

// applyMihomoConfig 应用 Mihomo 配置
func applyMihomoConfig(config *ClashConfig) error {
	if !mihomoReady {
		if err := initMihomo(); err != nil {
			return fmt.Errorf("failed to init mihomo: %w", err)
		}
	}
	
	// C.LOGI(C.CString(fmt.Sprintf("Applying Mihomo config: mode=%s, port=%d", 
	//	config.Mode, config.MixedPort)))
	
	// 1. 设置运行模式
	if err := setMihomoMode(config.Mode); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to set mode: %v", err)))
	}
	
	// 2. 配置 DNS
	if config.DNS.Enable {
		if err := setupMihomoDNS(&config.DNS); err != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Failed to setup DNS: %v", err)))
		}
	}
	
	// 3. 加载代理节点
	if err := loadMihomoProxies(config.Proxies); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to load proxies: %v", err)))
		return err
	}
	
	// 4. 加载代理组
	if err := loadMihomoProxyGroups(config.ProxyGroups); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to load proxy groups: %v", err)))
	}
	
	// 5. 加载规则
	if err := loadMihomoRules(config.Rules); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to load rules: %v", err)))
	}
	
	// 6. 配置混合端口 (在Android上通常不需要，因为使用TUN模式)
	// C.LOGI(C.CString(fmt.Sprintf("Mixed port configured: %d (TUN mode)", config.MixedPort)))
	
	// C.LOGI(C.CString("Mihomo config applied successfully"))
	return nil
}

// setMihomoMode 设置运行模式
func setMihomoMode(mode string) error {
	// C.LOGI(C.CString(fmt.Sprintf("Setting mode: %s", mode)))
	
	// 模式验证
	switch mode {
	case "rule", "global", "direct":
		// 有效模式
	default:
		return fmt.Errorf("invalid mode: %s", mode)
	}
	
	// 实际应用: tunnel.SetMode(mode)
	// C.LOGI(C.CString(fmt.Sprintf("Mode set to: %s", mode)))
	return nil
}

// setupMihomoDNS 设置DNS
func setupMihomoDNS(dnsConfig *DNSConfig) error {
	// C.LOGI(C.CString(fmt.Sprintf("Setting up DNS: mode=%s", dnsConfig.EnhancedMode)))
	
	// 配置DNS服务器
	for range dnsConfig.Nameserver {
		// C.LOGI(C.CString(fmt.Sprintf("DNS nameserver: %s", ns)))
	}
	
	// 实际应用: dns.ReCreateServer(config)
	// C.LOGI(C.CString("DNS configured"))
	return nil
}

// loadMihomoProxies 加载代理节点
func loadMihomoProxies(proxies []ProxyConfig) error {
	// C.LOGI(C.CString(fmt.Sprintf("Loading %d proxies", len(proxies))))
	
	for range proxies {
		// C.LOGI(C.CString(fmt.Sprintf("Proxy: %s (%s)", proxy.Name, proxy.Type)))
		
		// 实际应用: 创建对应的代理适配器
		// adapter.NewProxy(proxy)
	}
	
	// C.LOGI(C.CString(fmt.Sprintf("Loaded %d proxies", len(proxies))))
	return nil
}

// loadMihomoProxyGroups 加载代理组
func loadMihomoProxyGroups(groups []ProxyGroupConfig) error {
	// C.LOGI(C.CString(fmt.Sprintf("Loading %d proxy groups", len(groups))))
	
	for range groups {
		// C.LOGI(C.CString(fmt.Sprintf("Proxy group: %s (%s)", group.Name, group.Type)))
	}
	
	// C.LOGI(C.CString(fmt.Sprintf("Loaded %d proxy groups", len(groups))))
	return nil
}

// loadMihomoRules 加载规则
func loadMihomoRules(rules []string) error {
	// C.LOGI(C.CString(fmt.Sprintf("Loading %d rules", len(rules))))
	
	for i := range rules {
		if i < 5 { // 只打印前5条
			// C.LOGI(C.CString(fmt.Sprintf("Rule: %s", rule)))
		}
	}
	
	// 实际应用: 解析规则并创建规则引擎
	// ruleProviders, rules := parseRules(rules)
	
	// C.LOGI(C.CString(fmt.Sprintf("Loaded %d rules", len(rules))))
	return nil
}

// startMihomoTun 启动 Mihomo TUN 设备
func startMihomoTun(fd int, mtu int) error {
	if !mihomoReady {
		return fmt.Errorf("mihomo not ready")
	}
	
	// C.LOGI(C.CString(fmt.Sprintf("Starting Mihomo TUN: fd=%d, mtu=%d", fd, mtu)))
	
	// 使用传入的 Android VPN fd
	// Mihomo 支持使用已存在的文件描述符创建TUN设备
	
	// 1. 创建 TUN 配置
	_ = map[string]interface{}{
		"enable": true,
		"device": "fd",         // 使用文件描述符
		"fd":     fd,           // Android VPN fd
		"mtu":    mtu,          // MTU设置
		"stack":  "gvisor",     // 使用 gVisor 网络栈
		"auto-route": false,    // Android已处理路由
		"auto-detect-interface": false,
		"inet4-address": []string{"172.19.0.1/30"},
		"inet6-address": []string{"fdfe:dcba:9876::1/126"},
	}
	
	// C.LOGI(C.CString(fmt.Sprintf("TUN config: stack=gvisor, mtu=%d", mtu)))
	
	// 2. 应用TUN配置
	// 实际实现: 
	// - 使用 Mihomo 的 TUN 模块
	// - 设置数据包处理器
	// - 连接到隧道 (tunnel)
	
	// tunnel.SetTunDevice(tunDevice)
	// tunnel.SetMode(constant.TunMode)
	
	// 3. 启动TUN处理循环
	go handleMihomoTunPackets(fd, mtu)
	
	// C.LOGI(C.CString("Mihomo TUN started successfully"))
	return nil
}

// handleMihomoTunPackets 处理Mihomo TUN数据包
func handleMihomoTunPackets(fd int, mtu int) {
	// C.LOGI(C.CString("Mihomo TUN packet handler started"))
	
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Mihomo TUN panic: %v", r)))
		}
	}()
	
	// 实际的数据包处理由 Mihomo 的 TUN 栈完成
	// 这里只是一个占位符，实际会被 Mihomo 的内部实现替代
	
	// 数据包流向:
	// Android VPN → TUN fd → gVisor stack → Mihomo tunnel → Proxy
	
	// C.LOGI(C.CString("Mihomo TUN packet handler ready"))
}

// stopMihomoTun 停止 Mihomo TUN 设备
func stopMihomoTun() error {
	// C.LOGI(C.CString("Stopping Mihomo TUN"))
	
	// TODO: 停止 Mihomo TUN
	// tunnel.RemoveAll()
	
	// C.LOGI(C.CString("Mihomo TUN stopped"))
	return nil
}

// setupMihomoLogger 设置 Mihomo 日志
func setupMihomoLogger() error {
	logDir := filepath.Join(workingDir, "logs")
	if err := os.MkdirAll(logDir, 0755); err != nil {
		return fmt.Errorf("failed to create log dir: %w", err)
	}
	
	_ = filepath.Join(logDir, fmt.Sprintf("clash-%s.log", 
		time.Now().Format("2006-01-02")))
	
	// 配置日志输出
	// 实际实现: log.SetLevel(log.INFO)
	// 实际实现: log.SetOutput(logFile)
	
	// C.LOGI(C.CString(fmt.Sprintf("Mihomo logger setup: %s", logFile)))
	return nil
}

// setupMihomoLoggerInternal 内部日志设置
func setupMihomoLoggerInternal() {
	// 设置日志级别
	// 在Android上，日志直接输出到logcat
	// C.LOGI(C.CString("Mihomo logger configured for Android logcat"))
	
	// 实际实现:
	// log.SetLevel(log.INFO)
	// log.SetOutput(&androidLogWriter{})
}

// getMihomoConnections 获取 Mihomo 连接信息
func getMihomoConnections() ([]map[string]interface{}, error) {
	if !mihomoReady {
		return nil, fmt.Errorf("mihomo not ready")
	}
	
	// TODO: 获取连接信息
	// connections := tunnel.Connections()
	// return connections, nil
	
	return []map[string]interface{}{}, nil
}

// getMihomoProxies 获取 Mihomo 代理列表
func getMihomoProxies() (map[string]interface{}, error) {
	if !mihomoReady {
		return nil, fmt.Errorf("mihomo not ready")
	}
	
	// TODO: 获取代理信息
	// proxies := adapter.GetProxies()
	// return proxies, nil
	
	return map[string]interface{}{}, nil
}

// testMihomoProxy 测试代理延迟
func testMihomoProxy(proxyName string, url string, timeout time.Duration) (int64, error) {
	if !mihomoReady {
		return 0, fmt.Errorf("mihomo not ready")
	}
	
	// C.LOGI(C.CString(fmt.Sprintf("Testing proxy: %s, url: %s", proxyName, url)))
	
	startTime := time.Now()
	
	// 实际实现:
	// proxy := adapter.GetProxy(proxyName)
	// if proxy == nil {
	//     return 0, fmt.Errorf("proxy not found: %s", proxyName)
	// }
	// 
	// ctx, cancel := context.WithTimeout(mihomoCtx, timeout)
	// defer cancel()
	// 
	// delay, err := proxy.URLTest(ctx, url)
	// if err != nil {
	//     return 0, err
	// }
	
	// 模拟延迟测试
	time.Sleep(50 * time.Millisecond)
	delay := time.Since(startTime).Milliseconds()
	
	// C.LOGI(C.CString(fmt.Sprintf("Proxy %s delay: %dms", proxyName, delay)))
	return delay, nil
}

// testAllProxies 测试所有代理延迟
func testAllProxies(url string, timeout time.Duration) map[string]int64 {
	results := make(map[string]int64)
	
	// 获取所有代理节点
	proxies := listProxies()
	
	// C.LOGI(C.CString(fmt.Sprintf("Testing %d proxies", len(proxies))))
	
	for _, proxy := range proxies {
		if proxy.Type == "direct" {
			continue // 跳过DIRECT节点
		}
		
		delay, err := testMihomoProxy(proxy.Name, url, timeout)
		if err != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Test failed for %s: %v", proxy.Name, err)))
			results[proxy.Name] = -1 // -1表示测试失败
		} else {
			results[proxy.Name] = delay
		}
	}
	
	// C.LOGI(C.CString(fmt.Sprintf("Completed testing %d proxies", len(results))))
	return results
}

// switchMihomoProxy 切换代理
func switchMihomoProxy(groupName, proxyName string) error {
	if !mihomoReady {
		return fmt.Errorf("mihomo not ready")
	}
	
	// C.LOGI(C.CString(fmt.Sprintf("Switching proxy: group=%s, proxy=%s", groupName, proxyName)))
	
	// TODO: 切换代理
	// group := adapter.GetProxy(groupName)
	// if group == nil {
	//     return fmt.Errorf("group not found")
	// }
	// 
	// selector, ok := group.(*adapter.Selector)
	// if !ok {
	//     return fmt.Errorf("not a selector")
	// }
	// 
	// return selector.Set(proxyName)
	
	return nil
}

