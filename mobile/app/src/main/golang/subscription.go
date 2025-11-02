package main

/*
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "ClashCore-Go", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "ClashCore-Go", __VA_ARGS__)
*/
import "C"

import (
	"crypto/md5"
	"encoding/hex"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

// Subscription 订阅信息
type Subscription struct {
	Name        string
	URL         string
	FilePath    string
	UpdatedAt   time.Time
	UserAgent   string
	LastError   string
}

var (
	subscriptions     map[string]*Subscription
	subscriptionMutex sync.RWMutex
)

func init() {
	subscriptions = make(map[string]*Subscription)
}

// addSubscription 添加订阅
func addSubscription(name, url string) error {
	subscriptionMutex.Lock()
	defer subscriptionMutex.Unlock()
	
	// C.LOGI(C.CString(fmt.Sprintf("Adding subscription: %s", name)))
	
	// 检查是否已存在
	if _, exists := subscriptions[name]; exists {
		return fmt.Errorf("subscription already exists: %s", name)
	}
	
	// 创建订阅对象
	sub := &Subscription{
		Name:      name,
		URL:       url,
		FilePath:  filepath.Join(workingDir, "profiles", name+".yaml"),
		UserAgent: "clash-verge-rev/" + appVersion,
	}
	
	subscriptions[name] = sub
	
	// C.LOGI(C.CString(fmt.Sprintf("Subscription added: %s", name)))
	return nil
}

// removeSubscription 移除订阅
func removeSubscription(name string) error {
	subscriptionMutex.Lock()
	defer subscriptionMutex.Unlock()
	
	// C.LOGI(C.CString(fmt.Sprintf("Removing subscription: %s", name)))
	
	sub, exists := subscriptions[name]
	if !exists {
		return fmt.Errorf("subscription not found: %s", name)
	}
	
	// 删除配置文件
	if err := os.Remove(sub.FilePath); err != nil && !os.IsNotExist(err) {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to remove file: %v", err)))
	}
	
	delete(subscriptions, name)
	
	// C.LOGI(C.CString(fmt.Sprintf("Subscription removed: %s", name)))
	return nil
}

// updateSubscription 更新订阅
func updateSubscription(name string) error {
	subscriptionMutex.RLock()
	sub, exists := subscriptions[name]
	subscriptionMutex.RUnlock()
	
	if !exists {
		return fmt.Errorf("subscription not found: %s", name)
	}
	
	// C.LOGI(C.CString(fmt.Sprintf("Updating subscription: %s", name)))
	
	// 下载配置
	config, err := downloadSubscription(sub)
	if err != nil {
		sub.LastError = err.Error()
		// C.LOGE(C.CString(fmt.Sprintf("Failed to download subscription: %v", err)))
		return err
	}
	
	// 保存配置到文件
	if err := saveSubscriptionConfig(sub, config); err != nil {
		sub.LastError = err.Error()
		// C.LOGE(C.CString(fmt.Sprintf("Failed to save subscription: %v", err)))
		return err
	}
	
	// 更新时间
	sub.UpdatedAt = time.Now()
	sub.LastError = ""
	
	// C.LOGI(C.CString(fmt.Sprintf("Subscription updated: %s", name)))
	return nil
}

// downloadSubscription 下载订阅配置
func downloadSubscription(sub *Subscription) (string, error) {
	// C.LOGI(C.CString(fmt.Sprintf("Downloading from: %s", sub.URL)))
	
	// 创建HTTP客户端
	client := &http.Client{
		Timeout: 30 * time.Second,
	}
	
	// 创建请求
	req, err := http.NewRequest("GET", sub.URL, nil)
	if err != nil {
		return "", fmt.Errorf("failed to create request: %w", err)
	}
	
	// 设置User-Agent
	req.Header.Set("User-Agent", sub.UserAgent)
	
	// 发送请求
	resp, err := client.Do(req)
	if err != nil {
		return "", fmt.Errorf("failed to download: %w", err)
	}
	defer resp.Body.Close()
	
	// 检查状态码
	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("bad status code: %d", resp.StatusCode)
	}
	
	// 读取内容
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("failed to read body: %w", err)
	}
	
	config := string(body)
	
	// C.LOGI(C.CString(fmt.Sprintf("Downloaded %d bytes", len(config))))
	return config, nil
}

// saveSubscriptionConfig 保存订阅配置
func saveSubscriptionConfig(sub *Subscription, config string) error {
	// 确保目录存在
	dir := filepath.Dir(sub.FilePath)
	if err := os.MkdirAll(dir, 0755); err != nil {
		return fmt.Errorf("failed to create directory: %w", err)
	}
	
	// 写入文件
	if err := os.WriteFile(sub.FilePath, []byte(config), 0644); err != nil {
		return fmt.Errorf("failed to write file: %w", err)
	}
	
	// C.LOGI(C.CString(fmt.Sprintf("Config saved to: %s", sub.FilePath)))
	return nil
}

// updateAllSubscriptions 更新所有订阅
func updateAllSubscriptions() error {
	subscriptionMutex.RLock()
	subs := make([]*Subscription, 0, len(subscriptions))
	for _, sub := range subscriptions {
		subs = append(subs, sub)
	}
	subscriptionMutex.RUnlock()
	
	// C.LOGI(C.CString(fmt.Sprintf("Updating %d subscriptions", len(subs))))
	
	for _, sub := range subs {
		if err := updateSubscription(sub.Name); err != nil {
			// C.LOGE(C.CString(fmt.Sprintf("Failed to update %s: %v", sub.Name, err)))
		}
	}
	
	// C.LOGI(C.CString("All subscriptions updated"))
	return nil
}

// listSubscriptions 列出所有订阅
func listSubscriptions() []*Subscription {
	subscriptionMutex.RLock()
	defer subscriptionMutex.RUnlock()
	
	subs := make([]*Subscription, 0, len(subscriptions))
	for _, sub := range subscriptions {
		subs = append(subs, sub)
	}
	
	return subs
}

// getSubscription 获取订阅信息
func getSubscription(name string) (*Subscription, error) {
	subscriptionMutex.RLock()
	defer subscriptionMutex.RUnlock()
	
	sub, exists := subscriptions[name]
	if !exists {
		return nil, fmt.Errorf("subscription not found: %s", name)
	}
	
	return sub, nil
}

// generateSubscriptionHash 生成订阅哈希
func generateSubscriptionHash(url string) string {
	hash := md5.Sum([]byte(url))
	return hex.EncodeToString(hash[:])[:8]
}

// parseSubscriptionURL 解析订阅URL
func parseSubscriptionURL(url string) (string, error) {
	// 支持的协议
	supportedSchemes := []string{"http://", "https://"}
	
	hasScheme := false
	for _, scheme := range supportedSchemes {
		if strings.HasPrefix(url, scheme) {
			hasScheme = true
			break
		}
	}
	
	if !hasScheme {
		return "", fmt.Errorf("unsupported URL scheme")
	}
	
	return url, nil
}

// autoUpdateSubscriptions 自动更新订阅
func autoUpdateSubscriptions(interval time.Duration) {
	ticker := time.NewTicker(interval)
	defer ticker.Stop()
	
	// C.LOGI(C.CString(fmt.Sprintf("Auto-update started: interval=%v", interval)))
	
	for {
		select {
		case <-ticker.C:
			// C.LOGI(C.CString("Auto-updating subscriptions"))
			if err := updateAllSubscriptions(); err != nil {
				// C.LOGE(C.CString(fmt.Sprintf("Auto-update failed: %v", err)))
			}
		case <-mihomoCtx.Done():
			// C.LOGI(C.CString("Auto-update stopped"))
			return
		}
	}
}

