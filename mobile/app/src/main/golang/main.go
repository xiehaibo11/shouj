package main

/*
#cgo LDFLAGS: -llog

#include <stdlib.h>
#include <android/log.h>

typedef const char* c_string;
typedef long long c_long_long;

#define LOG_TAG "ClashCore-Go"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
*/
import "C"

import (
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"runtime/debug"
	"unsafe"
)

var (
	coreInitialized bool
	workingDir      string
	appVersion      string
)

func main() {
	// Stub - 不会被调用，只用于编译
}

// nativeInit 初始化核心
//
//export nativeInit
func nativeInit(homeDir, versionName C.c_string) {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in coreInit: %v", r)))
		}
	}()

	home := C.GoString(homeDir)
	version := C.GoString(versionName)

	// C.LOGI(C.CString(fmt.Sprintf("Initializing core version: %s, home: %s", version, home)))

	workingDir = home
	appVersion = version

	// 初始化核心配置
	if err := initCore(home, version); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to init core: %v", err)))
		return
	}

	// 初始化 Mihomo 核心（新的集成方式）
	if err := initMihomoCore(home); err != nil {
		C.LOGE(C.CString(fmt.Sprintf("Failed to init Mihomo core: %v", err)))
		return
	}

	// Mihomo 核心初始化后会自动启动 HTTP API 服务器（通过 hub.Parse()）
	// 无需单独启动 API 服务器
	C.LOGI(C.CString("✅ Mihomo HTTP API will be available after config is loaded"))

	// 启动简单的 SOCKS5 代理服务器（临时方案）
	// 监听 7897 端口（与 Mihomo 的 mixed-port 一致）
	if err := startSimpleProxy(7897); err != nil {
		C.LOGE(C.CString(fmt.Sprintf("Failed to start proxy server: %v", err)))
	} else {
		C.LOGI(C.CString("✅ Simple SOCKS5 proxy started on 127.0.0.1:7897"))
		C.LOGI(C.CString("  → Mode: DIRECT (for testing TUN connectivity)"))
	}

	coreInitialized = true
	nativeReset()

	C.LOGI(C.CString("✅ Core initialization completed"))
}

// nativeReset 重置核心状态
//
//export nativeReset
func nativeReset() {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in reset: %v", r)))
		}
	}()

	// C.LOGI(C.CString("Resetting core state"))

	// 重置流量统计
	resetTrafficStats()

	// 重置配置
	currentConfig = nil

	// 关闭所有连接 (TODO: 实现)

	runtime.GC()
	debug.FreeOSMemory()

	// C.LOGI(C.CString("Core reset completed"))
}

// nativeForceGc 强制垃圾回收
//
//export nativeForceGc
func nativeForceGc() {
	go func() {
		defer func() {
			if r := recover(); r != nil {
				// C.LOGE(C.CString(fmt.Sprintf("Panic in forceGc: %v", r)))
			}
		}()

		// C.LOGI(C.CString("Force GC requested"))
		runtime.GC()
		debug.FreeOSMemory()
	}()
}

// ==================== JNI 兼容的导出函数 ====================
// 注意：函数名必须遵循 JNI 命名规范

// Java_开头的函数是 JNI 标准命名，Kotlin 的 external 函数会自动查找这些
// 包名中的 _ 需要转义为 _1，. 需要转义为 _

//export Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeInit
func Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeInit(env, obj unsafe.Pointer, homeDir, versionName *C.char) {
	nativeInit(homeDir, versionName)
}

//export Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeReset
func Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeReset(env, obj unsafe.Pointer) {
	nativeReset()
}

//export Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeForceGc
func Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeForceGc(env, obj unsafe.Pointer) {
	nativeForceGc()
}

//export Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeStartTun
func Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeStartTun(env, obj unsafe.Pointer, fd, mtu C.int) C.int {
	return nativeStartTun(fd, mtu)
}

//export Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeStopTun
func Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeStopTun(env, obj unsafe.Pointer) {
	nativeStopTun()
}

//export Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeLoadConfig
func Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeLoadConfig(env, obj unsafe.Pointer, configPath *C.char) C.int {
	return nativeLoadConfig(configPath)
}

//export Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeQueryTraffic
func Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeQueryTraffic(env, obj unsafe.Pointer) C.longlong {
	return nativeQueryTraffic()
}

//export Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeGetVersion
func Java_io_github_clash_1verge_1rev_clash_1verge_1rev_core_ClashCore_nativeGetVersion(env, obj unsafe.Pointer) *C.char {
	return nativeGetVersion()
}

// ==================== 原有的内部函数（保持不变） ====================

// nativeStartTun 启动 TUN 设备（内部实现）
//
//export nativeStartTun
func nativeStartTun(fd C.int, mtu C.int) C.int {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in startTun: %v", r)))
		}
	}()

	tunFd := int(fd)
	tunMtu := int(mtu)

	// C.LOGI(C.CString(fmt.Sprintf("Starting TUN with fd: %d, mtu: %d", tunFd, tunMtu)))

	if !coreInitialized {
		// C.LOGE(C.CString("Core not initialized"))
		return -1
	}

	// 使用新的 Mihomo 核心启动 TUN
	if mihomoCore != nil {
		if err := mihomoCore.startTunWithFd(tunFd, tunMtu); err != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Failed to start Mihomo TUN: %v", err)))
			return -2
		}
	} else {
		// 回退到旧的 TUN 处理逻辑
		if err := startTunDevice(tunFd, tunMtu); err != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Failed to start TUN: %v", err)))
			return -2
		}
	}

	// C.LOGI(C.CString("TUN started successfully"))
	return 0
}

// nativeStopTun 停止 TUN 设备
//
//export nativeStopTun
func nativeStopTun() {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in stopTun: %v", r)))
		}
	}()

	// C.LOGI(C.CString("Stopping TUN"))

	// 使用新的 Mihomo 核心停止 TUN
	if mihomoCore != nil {
		if err := mihomoCore.stopTun(); err != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Failed to stop Mihomo TUN: %v", err)))
		}
	} else {
		// 回退到旧的 TUN 处理逻辑
		if err := stopTunDevice(); err != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Failed to stop TUN: %v", err)))
		}
	}

	// C.LOGI(C.CString("TUN stopped"))
}

// nativeLoadConfig 加载配置文件
//
//export nativeLoadConfig
func nativeLoadConfig(configPath C.c_string) C.int {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in loadConfig: %v", r)))
		}
	}()

	path := C.GoString(configPath)

	// C.LOGI(C.CString(fmt.Sprintf("Loading config from: %s", path)))

	// 检查文件是否存在
	if _, err := os.Stat(path); os.IsNotExist(err) {
		// C.LOGE(C.CString(fmt.Sprintf("Config file not found: %s", path)))
		return -1
	}

	// 解析配置文件
	config, err := parseConfig(path)
	if err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to parse config: %v", err)))
		return -2
	}

	// 应用配置
	if err := applyConfig(config); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to apply config: %v", err)))
		return -3
	}

	// 使用新的 Mihomo 核心重载配置
	if mihomoCore != nil {
		if err := mihomoCore.reloadConfig(path, false); err != nil {
			C.LOGE(C.CString(fmt.Sprintf("Failed to reload Mihomo config: %v", err)))
			return -4
		}
		C.LOGI(C.CString("✅ Mihomo config reloaded, API server started"))
	}

	C.LOGI(C.CString("✅ Config loaded and applied successfully"))
	return 0
}

// nativeQueryTraffic 查询流量统计
//
//export nativeQueryTraffic
func nativeQueryTraffic() C.c_long_long {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in queryTraffic: %v", r)))
		}
	}()

	// 获取流量统计（上传 + 下载）
	upload, download := getTrafficStats()
	total := upload + download

	return C.c_long_long(total)
}

// nativeGetVersion 获取核心版本
//
//export nativeGetVersion
func nativeGetVersion() *C.char {
	version := "Mihomo 1.18.1"
	return C.CString(version)
}

// freeString 释放 C 字符串内存
//
//export freeString
func freeString(str *C.char) {
	C.free(unsafe.Pointer(str))
}

// ==================== 新增 Mihomo API 导出函数 ====================

// nativeReloadConfig 重载配置文件
//
//export nativeReloadConfig
func nativeReloadConfig(configPath *C.char, force C.bool) C.int {
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

// nativeUpdateConfig 更新配置（JSON patch）
//
//export nativeUpdateConfig
func nativeUpdateConfig(patchJSON *C.char) C.int {
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in updateConfig: %v", r)))
		}
	}()

	if mihomoCore == nil {
		// C.LOGE(C.CString("Mihomo core not initialized"))
		return -1
	}

	patch := C.GoString(patchJSON)

	if err := mihomoCore.updateConfig(patch); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to update config: %v", err)))
		return -2
	}

	return 0
}

// nativeGetProxies 获取代理节点列表
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

	proxies, err := mihomoCore.getProxies()
	if err != nil {
		return C.CString(fmt.Sprintf("{\"error\":\"%v\"}", err))
	}

	return C.CString(proxies)
}

// nativeSelectProxy 选择代理节点
//
//export nativeSelectProxy
func nativeSelectProxy(groupName, proxyName *C.char) C.int {
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
func nativeTestProxyDelay(proxyName, testURL *C.char, timeout C.int) C.int {
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
		return -2
	}

	return C.int(delay)
}

// nativeGetConnections 获取活动连接列表
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

	connections, err := mihomoCore.getConnections()
	if err != nil {
		return C.CString(fmt.Sprintf("{\"error\":\"%v\"}", err))
	}

	return C.CString(connections)
}

// nativeCloseConnection 关闭指定连接
//
//export nativeCloseConnection
func nativeCloseConnection(connID *C.char) C.int {
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

// nativeGetRules 获取规则列表
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

	rules, err := mihomoCore.getRules()
	if err != nil {
		return C.CString(fmt.Sprintf("{\"error\":\"%v\"}", err))
	}

	return C.CString(rules)
}

// nativeGetLogs 获取日志列表
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

	n := int(count)
	logs, err := mihomoCore.getLogs(n)
	if err != nil {
		return C.CString(fmt.Sprintf("{\"error\":\"%v\"}", err))
	}

	return C.CString(logs)
}

// freeCString 释放 C 字符串内存（别名）
//
//export freeCString
func freeCString(str *C.char) {
	C.free(unsafe.Pointer(str))
}

// 辅助函数
func initCore(homeDir, version string) error {
	// C.LOGI(C.CString(fmt.Sprintf("Initializing core in: %s", homeDir)))

	// 设置工作目录
	if err := os.MkdirAll(homeDir, 0755); err != nil {
		return fmt.Errorf("failed to create home dir: %w", err)
	}

	// 创建必要的子目录
	dirs := []string{"config", "cache", "logs", "profiles"}
	for _, dir := range dirs {
		dirPath := filepath.Join(homeDir, dir)
		if err := os.MkdirAll(dirPath, 0755); err != nil {
			return fmt.Errorf("failed to create %s dir: %w", dir, err)
		}
	}

	// 设置 Mihomo 日志
	if err := setupMihomoLogger(); err != nil {
		// C.LOGI(C.CString(fmt.Sprintf("Warning: Failed to setup logger: %v", err)))
	}

	// C.LOGI(C.CString("Core directories created"))
	return nil
}
