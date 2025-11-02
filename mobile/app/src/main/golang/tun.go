package main

/*
#cgo LDFLAGS: -llog
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "ClashCore-Go", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "ClashCore-Go", __VA_ARGS__)
*/
import "C"

import (
	"fmt"
	"io"
	"os"
	"sync"
	"sync/atomic"
)

var (
	tunRunning   atomic.Bool
	tunFile      *os.File
	tunStopChan  chan struct{}
	tunWaitGroup sync.WaitGroup
	
	// 流量统计
	uploadBytes   atomic.Int64
	downloadBytes atomic.Int64
)

// startTunDevice 启动 TUN 设备处理
func startTunDevice(fd int, mtu int) error {
	if tunRunning.Load() {
		return fmt.Errorf("TUN already running")
	}
	
	// C.LOGI(C.CString(fmt.Sprintf("Starting TUN device: fd=%d, mtu=%d", fd, mtu)))
	
	// 使用 fd 创建文件对象
	tunFile = os.NewFile(uintptr(fd), "tun")
	if tunFile == nil {
		return fmt.Errorf("failed to create file from fd")
	}
	
	// 创建停止信号通道
	tunStopChan = make(chan struct{})
	
	// 标记为运行中
	tunRunning.Store(true)
	
	// 启动 Mihomo TUN
	if err := startMihomoTun(fd, mtu); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to start Mihomo TUN: %v", err)))
		// 降级到简单处理模式
		// C.LOGI(C.CString("Fallback to simple packet processing"))
	}
	
	// 启动数据包处理协程
	tunWaitGroup.Add(1)
	go processTunPackets()
	
	// C.LOGI(C.CString("TUN device started successfully"))
	return nil
}

// stopTunDevice 停止 TUN 设备
func stopTunDevice() error {
	if !tunRunning.Load() {
		return fmt.Errorf("TUN not running")
	}
	
	// C.LOGI(C.CString("Stopping TUN device..."))
	
	// 标记为停止
	tunRunning.Store(false)
	
	// 停止 Mihomo TUN
	if err := stopMihomoTun(); err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Warning: Failed to stop Mihomo TUN: %v", err)))
	}
	
	// 发送停止信号
	close(tunStopChan)
	
	// 等待协程退出
	tunWaitGroup.Wait()
	
	// 关闭文件
	if tunFile != nil {
		tunFile.Close()
		tunFile = nil
	}
	
	// C.LOGI(C.CString("TUN device stopped"))
	return nil
}

// processTunPackets 处理 TUN 数据包
func processTunPackets() {
	defer tunWaitGroup.Done()
	defer func() {
		if r := recover(); r != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Panic in processTunPackets: %v", r)))
		}
	}()
	
	// C.LOGI(C.CString("TUN packet processing started"))
	
	buffer := make([]byte, 65535)
	
	for tunRunning.Load() {
		select {
		case <-tunStopChan:
			// C.LOGI(C.CString("TUN stop signal received"))
			return
		default:
			// 读取数据包
			n, err := tunFile.Read(buffer)
			if err != nil {
				if err != io.EOF && tunRunning.Load() {
					// C.LOGE(C.CString(fmt.Sprintf("TUN read error: %v", err)))
				}
				continue
			}
			
			if n > 0 {
				// 更新下载统计
				downloadBytes.Add(int64(n))
				
				// 处理数据包
				packet := buffer[:n]
				processPacket(packet)
			}
		}
	}
	
	// C.LOGI(C.CString("TUN packet processing stopped"))
}

// processPacket 处理单个数据包
func processPacket(packet []byte) {
	// 简单的数据包处理逻辑
	// TODO: 集成 Mihomo 的完整 TUN 栈
	
	if len(packet) < 20 {
		// 数据包太小，忽略
		return
	}
	
	// 解析 IP 版本
	version := packet[0] >> 4
	
	switch version {
	case 4:
		// IPv4 数据包
		processIPv4Packet(packet)
	case 6:
		// IPv6 数据包
		processIPv6Packet(packet)
	default:
		// C.LOGE(C.CString(fmt.Sprintf("Unknown IP version: %d", version)))
	}
}

// processIPv4Packet 处理 IPv4 数据包
func processIPv4Packet(packet []byte) {
	ipPacket, err := parseIPv4Packet(packet)
	if err != nil || ipPacket == nil {
		return
	}
	
	proxy := getProxyForPacket(ipPacket.DstIP, ipPacket.DstPort)
	
	if proxy.Type == "direct" {
		if tunFile != nil && tunRunning.Load() {
			n, err := tunFile.Write(packet)
			if err == nil && n > 0 {
				uploadBytes.Add(int64(n))
			}
		}
	} else {
		if tunFile != nil && tunRunning.Load() {
			n, err := tunFile.Write(packet)
			if err == nil && n > 0 {
				uploadBytes.Add(int64(n))
			}
		}
	}
}

// processIPv6Packet 处理 IPv6 数据包
func processIPv6Packet(packet []byte) {
	ipPacket, err := parseIPv6Packet(packet)
	if err != nil || ipPacket == nil {
		return
	}
	
	proxy := getProxyForPacket(ipPacket.DstIP, ipPacket.DstPort)
	
	if proxy.Type == "direct" {
		if tunFile != nil && tunRunning.Load() {
			n, err := tunFile.Write(packet)
			if err == nil && n > 0 {
				uploadBytes.Add(int64(n))
			}
		}
	} else {
		if tunFile != nil && tunRunning.Load() {
			n, err := tunFile.Write(packet)
			if err == nil && n > 0 {
				uploadBytes.Add(int64(n))
			}
		}
	}
}

// getTrafficStats 获取流量统计
func getTrafficStats() (upload, download int64) {
	return uploadBytes.Load(), downloadBytes.Load()
}

// resetTrafficStats 重置流量统计
func resetTrafficStats() {
	uploadBytes.Store(0)
	downloadBytes.Store(0)
	// C.LOGI(C.CString("Traffic stats reset"))
}

