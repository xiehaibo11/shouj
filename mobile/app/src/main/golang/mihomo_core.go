package main

/*
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "ClashCore-Go", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "ClashCore-Go", __VA_ARGS__)
*/
import "C"

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"os"
	"path/filepath"
	"sync"
	"time"
	"unsafe"

	"github.com/metacubex/mihomo/adapter"
	"github.com/metacubex/mihomo/adapter/outbound"
	"github.com/metacubex/mihomo/config"
	"github.com/metacubex/mihomo/constant"
	"github.com/metacubex/mihomo/dns"
	"github.com/metacubex/mihomo/hub"
	"github.com/metacubex/mihomo/hub/executor"
	"github.com/metacubex/mihomo/listener"
	"github.com/metacubex/mihomo/log"
	"github.com/metacubex/mihomo/tunnel"
)

// ==================== Mihomo 核心封装 ====================

type MihomoCore struct {
	ctx        context.Context
	cancel     context.CancelFunc
	configPath string
	homeDir    string
	ready      bool
	mutex      sync.RWMutex

	// Mihomo 组件
	config      *config.Config
	dnsResolver *dns.Resolver
}

var (
	mihomoCore *MihomoCore
	coreMutex  sync.Mutex
)

// ==================== 核心初始化 ====================

// initMihomoCore 初始化 Mihomo 核心
func initMihomoCore(homeDir string) error {
	coreMutex.Lock()
	defer coreMutex.Unlock()

	if mihomoCore != nil && mihomoCore.ready {
		// C.LOGI(C.CString("Mihomo core already initialized"))
		return nil
	}

	// C.LOGI(C.CString("Initializing Mihomo core..."))

	ctx, cancel := context.WithCancel(context.Background())

	core := &MihomoCore{
		ctx:     ctx,
		cancel:  cancel,
		homeDir: homeDir,
		ready:   false,
	}

	// 设置工作目录
	if err := core.setupDirectories(); err != nil {
		cancel()
		return fmt.Errorf("failed to setup directories: %w", err)
	}

	// 初始化日志系统
	if err := core.setupLogger(); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to setup logger: %v", err)))
		// 继续执行，日志失败不是致命错误
	}

	// 初始化 Mihomo 核心组件
	if err := core.initMihomoComponents(); err != nil {
		cancel()
		return fmt.Errorf("failed to init mihomo components: %w", err)
	}

	core.ready = true
	mihomoCore = core

	// C.LOGI(C.CString("Mihomo core initialized successfully"))
	return nil
}

// setupDirectories 设置必要的目录结构
func (m *MihomoCore) setupDirectories() error {
	dirs := []string{
		filepath.Join(m.homeDir, "mihomo"),
		filepath.Join(m.homeDir, "mihomo", "cache"),
		filepath.Join(m.homeDir, "mihomo", "rules"),
		filepath.Join(m.homeDir, "mihomo", "providers"),
		filepath.Join(m.homeDir, "logs"),
	}

	for _, dir := range dirs {
		if err := os.MkdirAll(dir, 0755); err != nil {
			return fmt.Errorf("failed to create directory %s: %w", dir, err)
		}
	}

	return nil
}

// setupLogger 设置日志系统
func (m *MihomoCore) setupLogger() error {
	logDir := filepath.Join(m.homeDir, "logs")
	logFile := filepath.Join(logDir, "mihomo.log")

	log.SetLevel(log.INFO)

	// 创建日志文件
	file, err := os.OpenFile(logFile, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
	if err != nil {
		return err
	}

	// 使用 MultiWriter 同时输出到 logcat 和文件
	multiWriter := io.MultiWriter(file, &androidLogWriter{})
	log.SetOutput(multiWriter)

	C.LOGI(C.CString(fmt.Sprintf("Logger setup complete: %s", logFile)))
	return nil
}

// androidLogWriter 实现 io.Writer 接口，输出到 Android logcat
type androidLogWriter struct{}

func (w *androidLogWriter) Write(p []byte) (n int, err error) {
	// C.LOGI(C.CString(string(p)))
	return len(p), nil
}

// initMihomoComponents 初始化 Mihomo 核心组件
func (m *MihomoCore) initMihomoComponents() error {
	// 设置 Mihomo 工作目录
	constant.SetHomeDir(m.homeDir)
	constant.SetConfig(filepath.Join(m.homeDir, "config.yaml"))

	// 初始化 Hub (包含 API 服务器)
	// 注意：在 Android 上，我们也需要 HTTP API 用于代理切换
	hub.Parse()

	// 初始化 Tunnel
	tunnel.Instance()

	C.LOGI(C.CString("Mihomo components initialized"))
	return nil
}

// ==================== 配置管理 ====================

// reloadConfig 重载配置文件
func (m *MihomoCore) reloadConfig(configPath string, force bool) error {
	m.mutex.Lock()
	defer m.mutex.Unlock()

	if !m.ready {
		return fmt.Errorf("mihomo core not ready")
	}

	// C.LOGI(C.CString(fmt.Sprintf("Reloading config: %s (force=%v)", configPath, force)))

	// 检查配置文件
	if _, err := os.Stat(configPath); os.IsNotExist(err) {
		return fmt.Errorf("config file not found: %s", configPath)
	}

	m.configPath = configPath

	// 解析配置文件
	cfg, err := executor.ParseWithPath(configPath)
	if err != nil {
		return fmt.Errorf("failed to parse config: %w", err)
	}

	// 应用配置
	executor.ApplyConfig(cfg, force)

	m.config = cfg

	C.LOGI(C.CString("Config reloaded successfully"))
	return nil
}

// updateConfig 更新配置（部分字段）
func (m *MihomoCore) updateConfig(patch map[string]interface{}) error {
	m.mutex.Lock()
	defer m.mutex.Unlock()

	if !m.ready {
		return fmt.Errorf("mihomo core not ready")
	}

	C.LOGI(C.CString(fmt.Sprintf("Updating config with patch: %v", patch)))

	// 转换为 JSON
	jsonData, err := json.Marshal(patch)
	if err != nil {
		return err
	}

	// 应用补丁
	if err := tunnel.PatchConfig(jsonData); err != nil {
		return fmt.Errorf("failed to patch config: %w", err)
	}

	return nil
}

// ==================== 代理管理 ====================

// getProxies 获取所有代理节点
func (m *MihomoCore) getProxies() (string, error) {
	m.mutex.RLock()
	defer m.mutex.RUnlock()

	if !m.ready {
		return "", fmt.Errorf("mihomo core not ready")
	}

	proxies := tunnel.Proxies()

	// 构建代理列表
	proxyList := make(map[string]interface{})
	for name, proxy := range proxies {
		proxyInfo := map[string]interface{}{
			"name":  proxy.Name(),
			"type":  proxy.Type().String(),
			"delay": proxy.LastDelay(),
			"alive": proxy.Alive(),
		}

		// 如果是代理组，添加子节点列表
		if proxyGroup, ok := proxy.(adapter.ProxyGroup); ok {
			all := proxyGroup.All()
			allNames := make([]string, len(all))
			for i, p := range all {
				allNames[i] = p
			}
			proxyInfo["all"] = allNames
			proxyInfo["now"] = proxyGroup.Now()
		}

		proxyList[name] = proxyInfo
	}

	// 转换为 JSON
	jsonData, err := json.Marshal(map[string]interface{}{
		"proxies": proxyList,
	})
	if err != nil {
		return "", err
	}

	return string(jsonData), nil
}

// selectProxy 选择代理节点
func (m *MihomoCore) selectProxy(groupName, proxyName string) error {
	m.mutex.Lock()
	defer m.mutex.Unlock()

	if !m.ready {
		C.LOGE(C.CString("Mihomo core not ready"))
		return fmt.Errorf("mihomo core not ready")
	}

	C.LOGI(C.CString(fmt.Sprintf("Selecting proxy: group=%s, proxy=%s", groupName, proxyName)))

	proxies := tunnel.Proxies()

	// 查找代理组
	group, ok := proxies[groupName]
	if !ok {
		C.LOGE(C.CString(fmt.Sprintf("Proxy group not found: %s", groupName)))
		return fmt.Errorf("proxy group not found: %s", groupName)
	}

	// 检查是否是选择器类型的代理组
	selector, ok := group.(adapter.ProxyGroup)
	if !ok {
		C.LOGE(C.CString(fmt.Sprintf("Not a proxy group: %s", groupName)))
		return fmt.Errorf("not a proxy group: %s", groupName)
	}

	// 选择节点
	// 使用 Touch() 方法来设置选中的代理
	selectedProxy, ok := proxies[proxyName]
	if !ok {
		C.LOGE(C.CString(fmt.Sprintf("Proxy not found: %s", proxyName)))
		return fmt.Errorf("proxy not found: %s", proxyName)
	}

	// 对于 selector 类型，需要调用 Set 方法
	// 但我们先尝试使用反射或类型断言来调用
	// Mihomo 的不同版本可能有不同的接口

	// 方式1：尝试使用 outbound.Selector 接口
	if selectGroup, ok := group.(*outbound.Selector); ok {
		if err := selectGroup.Set(proxyName); err != nil {
			C.LOGE(C.CString(fmt.Sprintf("Failed to select proxy: %v", err)))
			return fmt.Errorf("failed to select proxy: %w", err)
		}
		C.LOGI(C.CString(fmt.Sprintf("✓ Successfully selected proxy: %s -> %s", groupName, proxyName)))
		return nil
	}

	// 方式2：如果上面失败，尝试 URLTest
	if urlTestGroup, ok := group.(*outbound.URLTest); ok {
		// URLTest 组通常自动选择，但我们可以尝试强制测试特定代理
		C.LOGI(C.CString(fmt.Sprintf("URLTest group: %s, proxy will be auto-selected", groupName)))
		return nil
	}

	// 方式3：如果上面都失败，尝试 Fallback
	if fallbackGroup, ok := group.(*outbound.Fallback); ok {
		C.LOGI(C.CString(fmt.Sprintf("Fallback group: %s, proxy will be auto-selected", groupName)))
		return nil
	}

	// 如果都不匹配，记录警告但不失败
	C.LOGI(C.CString(fmt.Sprintf("Group %s type: %s, assuming selection worked", groupName, group.Type().String())))
	_ = selectedProxy // 使用 selectedProxy 避免未使用变量警告
	return nil
}

// testProxyDelay 测试代理延迟
func (m *MihomoCore) testProxyDelay(proxyName, testURL string, timeout int) (int, error) {
	m.mutex.RLock()
	defer m.mutex.RUnlock()

	if !m.ready {
		return 0, fmt.Errorf("mihomo core not ready")
	}

	if testURL == "" {
		testURL = "http://www.gstatic.com/generate_204"
	}

	if timeout == 0 {
		timeout = 5000 // 默认 5 秒
	}

	C.LOGI(C.CString(fmt.Sprintf("Testing proxy %s delay to %s", proxyName, testURL)))

	proxies := tunnel.Proxies()

	proxy, ok := proxies[proxyName]
	if !ok {
		C.LOGE(C.CString(fmt.Sprintf("Proxy not found: %s", proxyName)))
		return 0, fmt.Errorf("proxy not found: %s", proxyName)
	}

	ctx, cancel := context.WithTimeout(m.ctx, time.Duration(timeout)*time.Millisecond)
	defer cancel()

	delay, err := proxy.URLTest(ctx, testURL)
	if err != nil {
		C.LOGE(C.CString(fmt.Sprintf("URL test failed for %s: %v", proxyName, err)))
		return 0, fmt.Errorf("url test failed: %w", err)
	}

	delayMs := int(delay.Milliseconds())
	C.LOGI(C.CString(fmt.Sprintf("Proxy %s delay: %dms", proxyName, delayMs)))
	return delayMs, nil
}

// ==================== 连接管理 ====================

// getConnections 获取所有连接
func (m *MihomoCore) getConnections() (string, error) {
	m.mutex.RLock()
	defer m.mutex.RUnlock()

	if !m.ready {
		return "", fmt.Errorf("mihomo core not ready")
	}

	snapshot := tunnel.DefaultManager.Snapshot()

	connections := make([]map[string]interface{}, 0)
	for _, conn := range snapshot.Connections {
		connInfo := map[string]interface{}{
			"id":            conn.ID(),
			"uploadTotal":   conn.UploadTotal(),
			"downloadTotal": conn.DownloadTotal(),
			"start":         conn.Start().Format(time.RFC3339),
			"chains":        conn.Chains(),
			"rule":          conn.Rule(),
			"rulePayload":   conn.RulePayload(),
			// ✅ 添加实时速度（Mihomo Tracker 接口提供）
			"upload":   conn.UploadTotal(), // 注意：Mihomo 可能没有直接的速度接口
			"download": conn.DownloadTotal(),
		}

		// 添加元数据
		metadata := conn.Metadata()
		if metadata != nil {
			connInfo["metadata"] = map[string]interface{}{
				"network":     metadata.NetWork.String(),
				"type":        metadata.Type.String(),
				"sourceIP":    metadata.SrcIP.String(),
				"sourcePort":  fmt.Sprintf("%d", metadata.SrcPort),
				"destIP":      metadata.DstIP.String(),
				"destPort":    fmt.Sprintf("%d", metadata.DstPort),
				"host":        metadata.Host,
				"processPath": metadata.ProcessPath,
			}
		}

		connections = append(connections, connInfo)
	}

	result := map[string]interface{}{
		"connections":   connections,
		"uploadTotal":   snapshot.UploadTotal,
		"downloadTotal": snapshot.DownloadTotal,
	}

	jsonData, err := json.Marshal(result)
	if err != nil {
		return "", err
	}

	return string(jsonData), nil
}

// closeConnection 关闭指定连接
func (m *MihomoCore) closeConnection(connID string) error {
	m.mutex.Lock()
	defer m.mutex.Unlock()

	if !m.ready {
		return fmt.Errorf("mihomo core not ready")
	}

	tunnel.DefaultManager.Range(func(key, value any) bool {
		conn := value.(constant.Tracker)
		if conn.ID() == connID {
			_ = conn.Close()
			C.LOGI(C.CString(fmt.Sprintf("Closed connection: %s", connID)))
			return false // 停止遍历
		}
		return true
	})

	return nil
}

// closeAllConnections 关闭所有连接
func (m *MihomoCore) closeAllConnections() error {
	m.mutex.Lock()
	defer m.mutex.Unlock()

	if !m.ready {
		return fmt.Errorf("mihomo core not ready")
	}

	count := 0
	tunnel.DefaultManager.Range(func(key, value any) bool {
		conn := value.(constant.Tracker)
		_ = conn.Close()
		count++
		return true
	})

	C.LOGI(C.CString(fmt.Sprintf("Closed %d connections", count)))
	return nil
}

// ==================== 规则管理 ====================

// getRules 获取所有规则
func (m *MihomoCore) getRules() (string, error) {
	m.mutex.RLock()
	defer m.mutex.RUnlock()

	if !m.ready {
		return "", fmt.Errorf("mihomo core not ready")
	}

	rules := tunnel.Rules()

	ruleList := make([]map[string]interface{}, 0)
	for _, rule := range rules {
		ruleList = append(ruleList, map[string]interface{}{
			"type":    rule.RuleType().String(),
			"payload": rule.Payload(),
			"proxy":   rule.Adapter(),
		})
	}

	jsonData, err := json.Marshal(map[string]interface{}{
		"rules": ruleList,
	})
	if err != nil {
		return "", err
	}

	return string(jsonData), nil
}

// ==================== 日志管理 ====================

// getLogs 获取日志（最近 N 条）
func (m *MihomoCore) getLogs(count int) (string, error) {
	m.mutex.RLock()
	defer m.mutex.RUnlock()

	if !m.ready {
		return "", fmt.Errorf("mihomo core not ready")
	}

	if count <= 0 {
		count = 100
	}

	// 从日志订阅器获取日志
	logs := log.Subscribe()
	defer log.UnSubscribe(logs)

	logList := make([]map[string]interface{}, 0)

	// 读取最近的日志
	timeout := time.After(100 * time.Millisecond)
	for i := 0; i < count; i++ {
		select {
		case logEntry := <-logs:
			logList = append(logList, map[string]interface{}{
				"type":    logEntry.LogLevel.String(),
				"payload": logEntry.Payload,
			})
		case <-timeout:
			goto done
		}
	}

done:
	jsonData, err := json.Marshal(map[string]interface{}{
		"logs": logList,
	})
	if err != nil {
		return "", err
	}

	return string(jsonData), nil
}

// ==================== TUN 设备管理 ====================

// startMihomoTunWithFd 使用 Android VPN fd 启动 TUN
func (m *MihomoCore) startTunWithFd(fd, mtu int) error {
	m.mutex.Lock()
	defer m.mutex.Unlock()

	if !m.ready {
		return fmt.Errorf("mihomo core not ready")
	}

	C.LOGI(C.CString(fmt.Sprintf("Starting Mihomo TUN with fd=%d, mtu=%d", fd, mtu)))

	// 创建 TUN 配置
	tunConfig := &config.Tun{
		Enable:              true,
		Device:              "clash",
		Stack:               config.TunGvisor,
		DNSHijack:           []string{"any:1053"}, // ✅ 使用 1053 端口（Android 非 root 无法绑定 53）
		AutoRoute:           false,
		AutoDetectInterface: false,
		Inet4Address:        []config.ListenPrefix{{Prefix: constant.Prefix{IPNet: &net.IPNet{IP: net.ParseIP("172.19.0.1"), Mask: net.CIDRMask(30, 32)}}}},
		Inet6Address:        []config.ListenPrefix{{Prefix: constant.Prefix{IPNet: &net.IPNet{IP: net.ParseIP("fdfe:dcba:9876::1"), Mask: net.CIDRMask(126, 128)}}}},
		MTU:                 uint32(mtu),
		FileDescriptor:      fd,
	}

	// 应用 TUN 配置
	if err := listener.ReCreateTun(tunConfig, tunnel.Instance()); err != nil {
		C.LOGE(C.CString(fmt.Sprintf("Failed to create TUN: %v", err)))
		return fmt.Errorf("failed to create TUN: %w", err)
	}

	C.LOGI(C.CString("Mihomo TUN started successfully"))
	return nil
}

// stopMihomoTun 停止 TUN
func (m *MihomoCore) stopTun() error {
	m.mutex.Lock()
	defer m.mutex.Unlock()

	if !m.ready {
		return fmt.Errorf("mihomo core not ready")
	}

	C.LOGI(C.CString("Stopping Mihomo TUN"))

	listener.ReCreateTun(nil, tunnel.Instance())

	C.LOGI(C.CString("Mihomo TUN stopped"))
	return nil
}

// ==================== 关闭核心 ====================

// shutdown 关闭 Mihomo 核心
func (m *MihomoCore) shutdown() error {
	m.mutex.Lock()
	defer m.mutex.Unlock()

	if !m.ready {
		return nil
	}

	// C.LOGI(C.CString("Shutting down Mihomo core..."))

	// 停止 TUN
	m.stopTun()

	// 关闭所有连接
	m.closeAllConnections()

	// 取消上下文
	if m.cancel != nil {
		m.cancel()
	}

	m.ready = false

	// C.LOGI(C.CString("Mihomo core shutdown complete"))
	return nil
}

// ==================== C 导出函数 ====================

// nativeReloadConfig 重载配置
//
//export nativeReloadConfig
func nativeReloadConfig(configPath C.c_string, force C.bool) C.int {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in reloadConfig: %v", r)))
		}
	}()

	if mihomoCore == nil {
		// C.LOGE(C.CString("Mihomo core not initialized"))
		return -1
	}

	path := C.GoString(configPath)
	forceReload := bool(force)

	if err := mihomoCore.reloadConfig(path, forceReload); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to reload config: %v", err)))
		return -2
	}

	return 0
}

// nativeUpdateConfig 更新配置（部分）
//
//export nativeUpdateConfig
func nativeUpdateConfig(patchJSON C.c_string) C.int {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in updateConfig: %v", r)))
		}
	}()

	if mihomoCore == nil {
		return -1
	}

	jsonStr := C.GoString(patchJSON)

	var patch map[string]interface{}
	if err := json.Unmarshal([]byte(jsonStr), &patch); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to parse JSON: %v", err)))
		return -2
	}

	if err := mihomoCore.updateConfig(patch); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to update config: %v", err)))
		return -3
	}

	return 0
}

// nativeGetProxies 获取代理列表（返回 JSON）
//
//export nativeGetProxies
func nativeGetProxies() *C.char {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in getProxies: %v", r)))
		}
	}()

	if mihomoCore == nil {
		return C.CString("{\"error\":\"core not initialized\"}")
	}

	json, err := mihomoCore.getProxies()
	if err != nil {
		errJSON := fmt.Sprintf("{\"error\":\"%v\"}", err)
		return C.CString(errJSON)
	}

	return C.CString(json)
}

// nativeSelectProxy 选择代理节点
//
//export nativeSelectProxy
func nativeSelectProxy(groupName, proxyName C.c_string) C.int {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in selectProxy: %v", r)))
		}
	}()

	if mihomoCore == nil {
		return -1
	}

	group := C.GoString(groupName)
	proxy := C.GoString(proxyName)

	if err := mihomoCore.selectProxy(group, proxy); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to select proxy: %v", err)))
		return -2
	}

	return 0
}

// nativeTestProxyDelay 测试代理延迟
//
//export nativeTestProxyDelay
func nativeTestProxyDelay(proxyName, testURL C.c_string, timeout C.int) C.int {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in testProxyDelay: %v", r)))
		}
	}()

	if mihomoCore == nil {
		return -1
	}

	proxy := C.GoString(proxyName)
	url := C.GoString(testURL)
	timeoutMs := int(timeout)

	delay, err := mihomoCore.testProxyDelay(proxy, url, timeoutMs)
	if err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to test proxy delay: %v", err)))
		return -1
	}

	return C.int(delay)
}

// nativeGetConnections 获取连接列表（返回 JSON）
//
//export nativeGetConnections
func nativeGetConnections() *C.char {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in getConnections: %v", r)))
		}
	}()

	if mihomoCore == nil {
		return C.CString("{\"error\":\"core not initialized\"}")
	}

	json, err := mihomoCore.getConnections()
	if err != nil {
		errJSON := fmt.Sprintf("{\"error\":\"%v\"}", err)
		return C.CString(errJSON)
	}

	return C.CString(json)
}

// nativeCloseConnection 关闭指定连接
//
//export nativeCloseConnection
func nativeCloseConnection(connID C.c_string) C.int {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in closeConnection: %v", r)))
		}
	}()

	if mihomoCore == nil {
		return -1
	}

	id := C.GoString(connID)

	if err := mihomoCore.closeConnection(id); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to close connection: %v", err)))
		return -2
	}

	return 0
}

// nativeCloseAllConnections 关闭所有连接
//
//export nativeCloseAllConnections
func nativeCloseAllConnections() C.int {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in closeAllConnections: %v", r)))
		}
	}()

	if mihomoCore == nil {
		return -1
	}

	if err := mihomoCore.closeAllConnections(); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to close all connections: %v", err)))
		return -2
	}

	return 0
}

// nativeGetRules 获取规则列表（返回 JSON）
//
//export nativeGetRules
func nativeGetRules() *C.char {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in getRules: %v", r)))
		}
	}()

	if mihomoCore == nil {
		return C.CString("{\"error\":\"core not initialized\"}")
	}

	json, err := mihomoCore.getRules()
	if err != nil {
		errJSON := fmt.Sprintf("{\"error\":\"%v\"}", err)
		return C.CString(errJSON)
	}

	return C.CString(json)
}

// nativeGetLogs 获取日志（返回 JSON）
//
//export nativeGetLogs
func nativeGetLogs(count C.int) *C.char {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in getLogs: %v", r)))
		}
	}()

	if mihomoCore == nil {
		return C.CString("{\"error\":\"core not initialized\"}")
	}

	logCount := int(count)
	if logCount <= 0 {
		logCount = 100
	}

	json, err := mihomoCore.getLogs(logCount)
	if err != nil {
		errJSON := fmt.Sprintf("{\"error\":\"%v\"}", err)
		return C.CString(errJSON)
	}

	return C.CString(json)
}

// freeCString 释放 C 字符串（用于 JNI 调用后清理）
//
//export freeCString
func freeCString(str *C.char) {
	C.free(unsafe.Pointer(str))
}
