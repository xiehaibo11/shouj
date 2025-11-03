package main

/*
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "ClashCore-Go", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "ClashCore-Go", __VA_ARGS__)
*/
import "C"

import (
	"encoding/binary"
	"fmt"
	"io"
	"net"
	"strconv"
	"sync"
	"time"
)

// SimpleProxy 简单的 SOCKS5 代理服务器
// 用于临时方案，直接转发（DIRECT 模式）
type SimpleProxy struct {
	listener net.Listener
	running  bool
	mutex    sync.Mutex
}

var simpleProxy *SimpleProxy

// startSimpleProxy 启动简单的 SOCKS5 代理服务器
func startSimpleProxy(port int) error {
	if simpleProxy != nil && simpleProxy.running {
		C.LOGI(C.CString("Simple proxy already running"))
		return nil
	}

	C.LOGI(C.CString(fmt.Sprintf("🚀 Starting simple SOCKS5 proxy on port %d", port)))

	listener, err := net.Listen("tcp", fmt.Sprintf("127.0.0.1:%d", port))
	if err != nil {
		C.LOGE(C.CString(fmt.Sprintf("Failed to start proxy: %v", err)))
		return err
	}

	proxy := &SimpleProxy{
		listener: listener,
		running:  true,
	}

	simpleProxy = proxy

	// 启动接受连接的协程
	go proxy.acceptLoop()

	C.LOGI(C.CString(fmt.Sprintf("✅ Simple SOCKS5 proxy started on 127.0.0.1:%d", port)))
	return nil
}

// stopSimpleProxy 停止代理服务器
func stopSimpleProxy() {
	if simpleProxy == nil {
		return
	}

	simpleProxy.mutex.Lock()
	defer simpleProxy.mutex.Unlock()

	if !simpleProxy.running {
		return
	}

	C.LOGI(C.CString("Stopping simple proxy..."))
	simpleProxy.running = false
	simpleProxy.listener.Close()
	simpleProxy = nil
	C.LOGI(C.CString("✅ Simple proxy stopped"))
}

// acceptLoop 接受连接循环
func (p *SimpleProxy) acceptLoop() {
	for p.running {
		conn, err := p.listener.Accept()
		if err != nil {
			if p.running {
				C.LOGE(C.CString(fmt.Sprintf("Accept error: %v", err)))
			}
			continue
		}

		// 处理连接
		go p.handleConnection(conn)
	}
}

// handleConnection 处理单个连接
func (p *SimpleProxy) handleConnection(conn net.Conn) {
	defer conn.Close()

	// 设置超时
	conn.SetDeadline(time.Now().Add(30 * time.Second))

	// SOCKS5 握手
	if err := p.socks5Handshake(conn); err != nil {
		C.LOGE(C.CString(fmt.Sprintf("SOCKS5 handshake failed: %v", err)))
		return
	}

	// 读取请求
	targetAddr, err := p.readSocks5Request(conn)
	if err != nil {
		C.LOGE(C.CString(fmt.Sprintf("Read request failed: %v", err)))
		return
	}

	C.LOGI(C.CString(fmt.Sprintf("📡 Connecting to: %s", targetAddr)))

	// 连接目标
	target, err := net.DialTimeout("tcp", targetAddr, 10*time.Second)
	if err != nil {
		C.LOGE(C.CString(fmt.Sprintf("Failed to connect to %s: %v", targetAddr, err)))
		p.sendSocks5Reply(conn, 0x05) // Connection refused
		return
	}
	defer target.Close()

	// 发送成功响应
	if err := p.sendSocks5Reply(conn, 0x00); err != nil {
		return
	}

	C.LOGI(C.CString(fmt.Sprintf("✅ Connected: %s", targetAddr)))

	// 双向转发
	p.relay(conn, target)
}

// socks5Handshake 执行 SOCKS5 握手
func (p *SimpleProxy) socks5Handshake(conn net.Conn) error {
	// 读取客户端问候
	buf := make([]byte, 258)
	n, err := conn.Read(buf)
	if err != nil {
		return err
	}

	if n < 2 {
		return fmt.Errorf("invalid handshake")
	}

	// 检查版本
	if buf[0] != 0x05 {
		return fmt.Errorf("unsupported SOCKS version: %d", buf[0])
	}

	// 发送认证方法响应（无需认证）
	_, err = conn.Write([]byte{0x05, 0x00})
	return err
}

// readSocks5Request 读取 SOCKS5 请求
func (p *SimpleProxy) readSocks5Request(conn net.Conn) (string, error) {
	buf := make([]byte, 4)
	if _, err := io.ReadFull(conn, buf); err != nil {
		return "", err
	}

	// 检查版本和命令
	if buf[0] != 0x05 {
		return "", fmt.Errorf("invalid version")
	}

	if buf[1] != 0x01 { // CONNECT
		return "", fmt.Errorf("unsupported command: %d", buf[1])
	}

	// 读取地址
	addrType := buf[3]
	var addr string

	switch addrType {
	case 0x01: // IPv4
		ipBuf := make([]byte, 4)
		if _, err := io.ReadFull(conn, ipBuf); err != nil {
			return "", err
		}
		addr = net.IP(ipBuf).String()

	case 0x03: // Domain
		lenBuf := make([]byte, 1)
		if _, err := io.ReadFull(conn, lenBuf); err != nil {
			return "", err
		}
		domainLen := int(lenBuf[0])
		domainBuf := make([]byte, domainLen)
		if _, err := io.ReadFull(conn, domainBuf); err != nil {
			return "", err
		}
		addr = string(domainBuf)

	case 0x04: // IPv6
		ipBuf := make([]byte, 16)
		if _, err := io.ReadFull(conn, ipBuf); err != nil {
			return "", err
		}
		addr = net.IP(ipBuf).String()

	default:
		return "", fmt.Errorf("unsupported address type: %d", addrType)
	}

	// 读取端口
	portBuf := make([]byte, 2)
	if _, err := io.ReadFull(conn, portBuf); err != nil {
		return "", err
	}
	port := binary.BigEndian.Uint16(portBuf)

	return net.JoinHostPort(addr, strconv.Itoa(int(port))), nil
}

// sendSocks5Reply 发送 SOCKS5 响应
func (p *SimpleProxy) sendSocks5Reply(conn net.Conn, rep byte) error {
	// VER, REP, RSV, ATYP, BND.ADDR, BND.PORT
	reply := []byte{
		0x05,       // VER
		rep,        // REP
		0x00,       // RSV
		0x01,       // ATYP (IPv4)
		0, 0, 0, 0, // BND.ADDR
		0, 0, // BND.PORT
	}

	_, err := conn.Write(reply)
	return err
}

// relay 双向转发数据
func (p *SimpleProxy) relay(client, target net.Conn) {
	var wg sync.WaitGroup
	wg.Add(2)

	// 客户端 -> 目标
	go func() {
		defer wg.Done()
		written, _ := io.Copy(target, client)
		uploadBytes.Add(written)
	}()

	// 目标 -> 客户端
	go func() {
		defer wg.Done()
		written, _ := io.Copy(client, target)
		downloadBytes.Add(written)
	}()

	wg.Wait()
}
