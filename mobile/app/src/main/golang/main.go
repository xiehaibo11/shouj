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
	
	// 初始化 Mihomo
	if err := initMihomo(); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to init Mihomo: %v", err)))
		return
	}
	
	coreInitialized = true
	nativeReset()
	
	// C.LOGI(C.CString("Core initialization completed"))
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

// nativeStartTun 启动 TUN 设备
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
	
	// 启动 TUN 设备处理
	if err := startTunDevice(tunFd, tunMtu); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to start TUN: %v", err)))
		return -2
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
	
	// 停止 TUN 设备
	if err := stopTunDevice(); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to stop TUN: %v", err)))
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
	
	// 应用 Mihomo 配置
	if err := applyMihomoConfig(config); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to apply Mihomo config: %v", err)))
		return -4
	}
	
	// 保存配置路径
	// configPath = path
	
	// C.LOGI(C.CString("Config loaded and applied successfully"))
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

