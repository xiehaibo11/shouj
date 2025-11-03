package main

/*
#include <android/log.h>
#define LOG_TAG "MihomoAPI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
*/
import "C"

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"sync"

	"gopkg.in/yaml.v3"
)

var (
	apiServer     *http.Server
	apiServerOnce sync.Once
	apiMutex      sync.RWMutex
)

// startAPIServer 启动HTTP API服务器
func startAPIServer(port string) error {
	apiServerOnce.Do(func() {
		mux := http.NewServeMux()

		// GET /version - 返回版本信息
		mux.HandleFunc("/version", handleVersion)

		// GET /proxies - 获取所有代理
		mux.HandleFunc("/proxies", handleGetProxies)

		// PUT /proxies/{group} - 选择代理
		mux.HandleFunc("/proxies/", handleSelectProxy)

		// GET /configs - 获取配置
		mux.HandleFunc("/configs", handleGetConfigs)

		apiServer = &http.Server{
			Addr:    "127.0.0.1:" + port,
			Handler: mux,
		}

		go func() {
			// C.LOGI(C.CString(fmt.Sprintf("Starting API server on %s", apiServer.Addr)))
			if err := apiServer.ListenAndServe(); err != nil && err != http.ErrServerClosed {
				// C.LOGE(C.CString(fmt.Sprintf("API server error: %v", err)))
			}
		}()

		// C.LOGI(C.CString("API server started successfully"))
	})

	return nil
}

// stopAPIServer 停止HTTP API服务器
func stopAPIServer() error {
	if apiServer != nil {
		return apiServer.Close()
	}
	return nil
}

// handleVersion 处理版本请求
func handleVersion(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	response := map[string]interface{}{
		"version": "Mihomo Android 1.18.1",
		"premium": true,
		"meta":    true,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// handleGetProxies 处理获取代理列表请求
func handleGetProxies(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	apiMutex.RLock()
	defer apiMutex.RUnlock()

	// 从当前配置读取代理信息
	proxies := make(map[string]interface{})

	if currentConfig != nil {
		// 添加代理组信息
		for _, group := range currentConfig.ProxyGroups {
			proxies[group.Name] = map[string]interface{}{
				"type":    group.Type,
				"now":     group.Proxies[0], // 默认第一个
				"all":     group.Proxies,
				"history": []interface{}{},
			}
		}

		// 添加单个代理信息
		for _, proxy := range currentConfig.Proxies {
			proxies[proxy.Name] = map[string]interface{}{
				"type":    proxy.Type,
				"name":    proxy.Name,
				"history": []interface{}{},
			}
		}
	}

	response := map[string]interface{}{
		"proxies": proxies,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// handleSelectProxy 处理选择代理请求
func handleSelectProxy(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPut {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// 提取组名（/proxies/{group}）
	groupName := r.URL.Path[len("/proxies/"):]
	if groupName == "" {
		http.Error(w, "Group name required", http.StatusBadRequest)
		return
	}

	// 读取请求体
	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "Failed to read request body", http.StatusBadRequest)
		return
	}
	defer r.Body.Close()

	// 解析JSON
	var req struct {
		Name string `json:"name"`
	}
	if err := json.Unmarshal(body, &req); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	proxyName := req.Name

	// C.LOGI(C.CString(fmt.Sprintf("API: Select proxy %s -> %s", groupName, proxyName)))

	// 更新配置文件中的选中项
	apiMutex.Lock()
	err = updateProxySelection(groupName, proxyName)
	apiMutex.Unlock()

	if err != nil {
		// C.LOGE(C.CString(fmt.Sprintf("Failed to update proxy selection: %v", err)))
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	// 返回成功（204 No Content）
	w.WriteHeader(http.StatusNoContent)
}

// handleGetConfigs 处理获取配置请求
func handleGetConfigs(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	apiMutex.RLock()
	defer apiMutex.RUnlock()

	response := map[string]interface{}{
		"port":                7890,
		"socks-port":          7891,
		"mixed-port":          7897,
		"allow-lan":           false,
		"mode":                "rule",
		"log-level":           "info",
		"external-controller": "127.0.0.1:9090",
	}

	if currentConfig != nil {
		response["mixed-port"] = currentConfig.MixedPort
		response["allow-lan"] = currentConfig.AllowLan
		response["mode"] = currentConfig.Mode
		response["log-level"] = currentConfig.LogLevel
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// updateProxySelection 更新配置文件中的代理选择
func updateProxySelection(groupName, proxyName string) error {
	if currentConfig == nil || mihomoCore == nil {
		return fmt.Errorf("core not initialized")
	}

	// 查找代理组
	var found bool
	for i, group := range currentConfig.ProxyGroups {
		if group.Name == groupName {
			found = true

			// 验证代理名称是否在组中
			var validProxy bool
			for _, p := range group.Proxies {
				if p == proxyName {
					validProxy = true
					break
				}
			}

			if !validProxy {
				return fmt.Errorf("proxy %s not found in group %s", proxyName, groupName)
			}

			// 将选中的代理移到第一位
			newProxies := []string{proxyName}
			for _, p := range group.Proxies {
				if p != proxyName {
					newProxies = append(newProxies, p)
				}
			}
			currentConfig.ProxyGroups[i].Proxies = newProxies

			// 保存到配置文件
			if mihomoCore.configPath != "" {
				if err := saveConfigToFile(mihomoCore.configPath); err != nil {
					return fmt.Errorf("failed to save config: %w", err)
				}
			}

			return nil
		}
	}

	if !found {
		return fmt.Errorf("proxy group %s not found", groupName)
	}

	return nil
}

// saveConfigToFile 保存配置到文件
func saveConfigToFile(configPath string) error {
	if currentConfig == nil {
		return fmt.Errorf("no config to save")
	}

	// 创建备份
	backupPath := configPath + ".backup"
	if err := copyFile(configPath, backupPath); err != nil {
		// 忽略备份错误
	}

	// 转换为YAML
	data, err := yaml.Marshal(currentConfig)
	if err != nil {
		return fmt.Errorf("failed to marshal config: %w", err)
	}

	// 写入文件
	if err := os.WriteFile(configPath, data, 0644); err != nil {
		return fmt.Errorf("failed to write config file: %w", err)
	}

	return nil
}

// copyFile 复制文件
func copyFile(src, dst string) error {
	sourceFile, err := os.Open(src)
	if err != nil {
		return err
	}
	defer sourceFile.Close()

	destFile, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer destFile.Close()

	_, err = io.Copy(destFile, sourceFile)
	return err
}

// ensureAPIServerRunning 确保API服务器正在运行
func ensureAPIServerRunning() error {
	// 检查服务器是否已启动
	resp, err := http.Get("http://127.0.0.1:9090/version")
	if err == nil {
		resp.Body.Close()
		return nil // 服务器已运行
	}

	// 启动服务器
	return startAPIServer("9090")
}

