package main

/*
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "ClashCore-Go", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "ClashCore-Go", __VA_ARGS__)
*/
import "C"

import (
	"fmt"
	"net"
	"strings"
	"sync"
)

type ProxyNode struct {
	Name     string
	Type     string
	Server   string
	Port     int
	Selected bool
}

var (
	proxyNodes   []*ProxyNode
	proxyMutex   sync.RWMutex
	activeProxy  *ProxyNode
	directProxy  = &ProxyNode{Name: "DIRECT", Type: "direct"}
)

func initProxies(config *ClashConfig) error {
	proxyMutex.Lock()
	defer proxyMutex.Unlock()
	
	proxyNodes = make([]*ProxyNode, 0)
	
	for _, p := range config.Proxies {
		node := &ProxyNode{
			Name:   p.Name,
			Type:   p.Type,
			Server: p.Server,
			Port:   p.Port,
		}
		proxyNodes = append(proxyNodes, node)
		// C.LOGI(C.CString(fmt.Sprintf("Loaded proxy: %s (%s)", node.Name, node.Type)))
	}
	
	if len(proxyNodes) > 0 {
		activeProxy = proxyNodes[0]
		activeProxy.Selected = true
	} else {
		activeProxy = directProxy
	}
	
	// C.LOGI(C.CString(fmt.Sprintf("Initialized %d proxies, active: %s", len(proxyNodes), activeProxy.Name)))
	return nil
}

func matchRule(host string, ip net.IP) *ProxyNode {
	if currentConfig == nil {
		return directProxy
	}
	
	for _, rule := range currentConfig.Rules {
		parts := strings.Split(rule, ",")
		if len(parts) < 2 {
			continue
		}
		
		ruleType := strings.TrimSpace(parts[0])
		ruleValue := strings.TrimSpace(parts[1])
		action := "PROXY"
		if len(parts) >= 3 {
			action = strings.TrimSpace(parts[2])
		}
		
		matched := false
		switch ruleType {
		case "DOMAIN":
			matched = (host == ruleValue)
		case "DOMAIN-SUFFIX":
			matched = strings.HasSuffix(host, ruleValue)
		case "DOMAIN-KEYWORD":
			matched = strings.Contains(host, ruleValue)
		case "IP-CIDR":
			if ip != nil {
				_, cidr, err := net.ParseCIDR(ruleValue)
				if err == nil {
					matched = cidr.Contains(ip)
				}
			}
		case "MATCH":
			matched = true
		}
		
		if matched {
			if action == "DIRECT" {
				return directProxy
			}
			return activeProxy
		}
	}
	
	return directProxy
}

func getProxyForPacket(destIP net.IP, destPort uint16) *ProxyNode {
	if destIP == nil {
		return directProxy
	}
	
	host := destIP.String()
	proxy := matchRule(host, destIP)
	
	return proxy
}

func getActiveProxy() *ProxyNode {
	proxyMutex.RLock()
	defer proxyMutex.RUnlock()
	return activeProxy
}

func setActiveProxy(name string) error {
	proxyMutex.Lock()
	defer proxyMutex.Unlock()
	
	for _, node := range proxyNodes {
		node.Selected = false
		if node.Name == name {
			activeProxy = node
			node.Selected = true
			// C.LOGI(C.CString(fmt.Sprintf("Switched to proxy: %s", name)))
			return nil
		}
	}
	
	return fmt.Errorf("proxy not found: %s", name)
}

func listProxies() []*ProxyNode {
	proxyMutex.RLock()
	defer proxyMutex.RUnlock()
	return proxyNodes
}



