package main

import (
	"fmt"
	"os"
	"path/filepath"

	"gopkg.in/yaml.v3"
)

// ClashConfig 表示 Clash 配置结构
type ClashConfig struct {
	MixedPort          int                `yaml:"mixed-port"`
	AllowLan           bool               `yaml:"allow-lan"`
	Mode               string             `yaml:"mode"`
	LogLevel           string             `yaml:"log-level"`
	IPv6               bool               `yaml:"ipv6"`
	ExternalController string             `yaml:"external-controller"`
	Secret             string             `yaml:"secret"`
	DNS                DNSConfig          `yaml:"dns"`
	Proxies            []ProxyConfig      `yaml:"proxies"`
	ProxyGroups        []ProxyGroupConfig `yaml:"proxy-groups"`
	Rules              []string           `yaml:"rules"`
}

// DNSConfig DNS 配置
type DNSConfig struct {
	Enable       bool     `yaml:"enable"`
	Listen       string   `yaml:"listen"`
	EnhancedMode string   `yaml:"enhanced-mode"`
	Nameserver   []string `yaml:"nameserver"`
}

// ProxyConfig 代理节点配置
type ProxyConfig struct {
	Name     string `yaml:"name"`
	Type     string `yaml:"type"`
	Server   string `yaml:"server,omitempty"`
	Port     int    `yaml:"port,omitempty"`
	Cipher   string `yaml:"cipher,omitempty"`
	Password string `yaml:"password,omitempty"`
}

// ProxyGroupConfig 代理组配置
type ProxyGroupConfig struct {
	Name    string   `yaml:"name"`
	Type    string   `yaml:"type"`
	Proxies []string `yaml:"proxies"`
}

var (
	currentConfig *ClashConfig
	configPath    string
)

// parseConfig 解析配置文件
func parseConfig(path string) (*ClashConfig, error) {
	// // C.LOGI(C.CString(fmt.Sprintf("Parsing config file: %s", path)))

	// 读取文件
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("failed to read config file: %w", err)
	}

	// 解析 YAML
	var config ClashConfig
	if err := yaml.Unmarshal(data, &config); err != nil {
		return nil, fmt.Errorf("failed to parse YAML: %w", err)
	}

	// 设置默认值
	if config.MixedPort == 0 {
		config.MixedPort = 7897
	}
	if config.Mode == "" {
		config.Mode = "rule"
	}
	if config.LogLevel == "" {
		config.LogLevel = "info"
	}

	// C.LOGI(C.CString(fmt.Sprintf("Config parsed: mode=%s, port=%d, proxies=%d",
	//	config.Mode, config.MixedPort, len(config.Proxies))))

	return &config, nil
}

// validateConfig 验证配置有效性
func validateConfig(config *ClashConfig) error {
	if config == nil {
		return fmt.Errorf("config is nil")
	}

	if config.MixedPort < 1024 || config.MixedPort > 65535 {
		return fmt.Errorf("invalid mixed-port: %d", config.MixedPort)
	}

	if config.Mode != "rule" && config.Mode != "global" && config.Mode != "direct" {
		return fmt.Errorf("invalid mode: %s", config.Mode)
	}

	if len(config.Proxies) == 0 {
		// // C.LOGI(C.CString("Warning: No proxies configured"))
	}

	if len(config.Rules) == 0 {
		// // C.LOGI(C.CString("Warning: No rules configured"))
	}

	return nil
}

// applyConfig 应用配置
func applyConfig(config *ClashConfig) error {
	// // C.LOGI(C.CString("Applying configuration..."))

	// 验证配置
	if err := validateConfig(config); err != nil {
		return fmt.Errorf("invalid config: %w", err)
	}

	// 保存当前配置
	currentConfig = config

	// 初始化代理节点
	if err := initProxies(config); err != nil {
		return fmt.Errorf("failed to init proxies: %w", err)
	}

	// // C.LOGI(C.CString("Configuration applied successfully"))
	return nil
}

// saveConfig 保存配置到文件
func saveConfig(config *ClashConfig, path string) error {
	data, err := yaml.Marshal(config)
	if err != nil {
		return fmt.Errorf("failed to marshal config: %w", err)
	}

	// 确保目录存在
	dir := filepath.Dir(path)
	if err := os.MkdirAll(dir, 0755); err != nil {
		return fmt.Errorf("failed to create directory: %w", err)
	}

	// 写入文件
	if err := os.WriteFile(path, data, 0644); err != nil {
		return fmt.Errorf("failed to write config: %w", err)
	}

	// // C.LOGI(C.CString(fmt.Sprintf("Config saved to: %s", path)))
	return nil
}

// getDefaultConfig 获取默认配置
func getDefaultConfig() *ClashConfig {
	return &ClashConfig{
		MixedPort:          7897,
		AllowLan:           false,
		Mode:               "rule",
		LogLevel:           "info",
		IPv6:               true,
		ExternalController: "127.0.0.1:9090",
		Secret:             "",
		DNS: DNSConfig{
			Enable:       true,
			Listen:       "0.0.0.0:1053",
			EnhancedMode: "fake-ip",
			Nameserver:   []string{"8.8.8.8", "1.1.1.1"},
		},
		Proxies: []ProxyConfig{
			{
				Name: "DIRECT",
				Type: "direct",
			},
		},
		ProxyGroups: []ProxyGroupConfig{
			{
				Name:    "PROXY",
				Type:    "select",
				Proxies: []string{"DIRECT"},
			},
		},
		Rules: []string{
			"MATCH,PROXY",
		},
	}
}
