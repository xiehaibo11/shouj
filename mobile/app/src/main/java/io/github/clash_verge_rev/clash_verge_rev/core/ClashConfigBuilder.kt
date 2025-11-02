package io.github.clash_verge_rev.clash_verge_rev.core

import android.content.Context
import android.util.Log
import io.github.clash_verge_rev.clash_verge_rev.data.SettingsManager
import io.github.clash_verge_rev.clash_verge_rev.data.TunConfigManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Clash配置构建器
 * 
 * 根据用户设置生成Clash核心配置文件
 * 类似桌面端的patchClash功能
 */
class ClashConfigBuilder(private val context: Context) {
    
    companion object {
        private const val TAG = "ClashConfigBuilder"
        private const val CONFIG_FILE = "config.yaml"
    }
    
    private val settingsManager = SettingsManager.getInstance(context)
    private val tunConfigManager = TunConfigManager.getInstance(context)
    
    /**
     * 构建完整的Clash配置
     */
    fun buildConfig(baseConfig: Map<String, Any>? = null): String {
        Log.d(TAG, "Building Clash configuration")
        
        val config = mutableMapOf<String, Any>()
        
        // 从基础配置开始（如果有的话）
        baseConfig?.let { config.putAll(it) }
        
        // 应用用户设置
        applySystemSettings(config)
        applyClashSettings(config)
        applyPortSettings(config)
        applyTunSettings(config)
        
        return convertToYaml(config)
    }
    
    /**
     * 应用系统设置
     */
    private fun applySystemSettings(config: MutableMap<String, Any>) {
        // TUN模式在启动VPN服务时单独处理
        // 这里只记录配置
        val tunMode = settingsManager.tunMode.value
        Log.d(TAG, "TUN Mode: $tunMode")
    }
    
    /**
     * 应用Clash设置
     */
    private fun applyClashSettings(config: MutableMap<String, Any>) {
        // 局域网连接
        val allowLan = settingsManager.allowLan.value
        config["allow-lan"] = allowLan
        Log.d(TAG, "Allow LAN: $allowLan")
        
        // IPv6支持
        val ipv6 = settingsManager.ipv6.value
        config["ipv6"] = ipv6
        Log.d(TAG, "IPv6: $ipv6")
        
        // 统一延迟
        val unifiedDelay = settingsManager.unifiedDelay.value
        config["unified-delay"] = unifiedDelay
        Log.d(TAG, "Unified Delay: $unifiedDelay")
        
        // 日志等级
        val logLevel = settingsManager.logLevel.value.lowercase()
        config["log-level"] = when (logLevel) {
            "warn" -> "warning"
            else -> logLevel
        }
        Log.d(TAG, "Log Level: $logLevel")
        
        // DNS覆写
        if (settingsManager.dnsOverwrite.value) {
            config["dns"] = buildDnsConfig()
        }
    }
    
    /**
     * 构建DNS配置
     */
    private fun buildDnsConfig(): Map<String, Any> {
        return mapOf(
            "enable" to true,
            "enhanced-mode" to "fake-ip",
            "fake-ip-range" to "198.18.0.1/16",
            "nameserver" to listOf(
                "223.5.5.5",
                "119.29.29.29",
                "1.1.1.1",
                "8.8.8.8"
            ),
            "fallback" to listOf(
                "1.1.1.1",
                "8.8.8.8",
                "1.0.0.1",
                "8.8.4.4"
            ),
            "fallback-filter" to mapOf(
                "geoip" to true,
                "geoip-code" to "CN"
            )
        )
    }
    
    /**
     * 应用端口设置
     */
    private fun applyPortSettings(config: MutableMap<String, Any>) {
        // 混合端口
        val mixedPort = settingsManager.mixedPort.value.toIntOrNull() ?: 7890
        config["mixed-port"] = mixedPort
        Log.d(TAG, "Mixed Port: $mixedPort")
        
        // 外部控制
        val externalController = settingsManager.externalController.value
        if (externalController.isNotEmpty()) {
            config["external-controller"] = externalController
            Log.d(TAG, "External Controller: $externalController")
            
            // 外部UI（如果设置了网页界面）
            // config["external-ui"] = "dashboard"
        }
    }
    
    /**
     * 应用TUN设置
     */
    private fun applyTunSettings(config: MutableMap<String, Any>) {
        if (!settingsManager.tunMode.value) {
            return
        }
        
        val tunConfig = tunConfigManager.getConfig()
        config["tun"] = mutableMapOf<String, Any>(
            "enable" to true,
            "stack" to tunConfig["stack"]!!,
            "device" to tunConfig["device"]!!,
            "auto-route" to tunConfig["auto-route"]!!,
            "strict-route" to tunConfig["strict-route"]!!,
            "auto-detect-interface" to tunConfig["auto-detect-interface"]!!,
            "dns-hijack" to tunConfig["dns-hijack"]!!,
            "mtu" to tunConfig["mtu"]!!
        )
        
        Log.d(TAG, "TUN Config: $tunConfig")
    }
    
    /**
     * 转换配置为YAML格式
     * 注意：这是简化版本，实际应该使用YAML库
     */
    private fun convertToYaml(config: Map<String, Any>, indent: Int = 0): String {
        val sb = StringBuilder()
        val indentStr = "  ".repeat(indent)
        
        for ((key, value) in config) {
            when (value) {
                is Map<*, *> -> {
                    sb.append("$indentStr$key:\n")
                    @Suppress("UNCHECKED_CAST")
                    sb.append(convertToYaml(value as Map<String, Any>, indent + 1))
                }
                is List<*> -> {
                    sb.append("$indentStr$key:\n")
                    value.forEach { item ->
                        when (item) {
                            is Map<*, *> -> {
                                sb.append("$indentStr  -\n")
                                @Suppress("UNCHECKED_CAST")
                                sb.append(convertToYaml(item as Map<String, Any>, indent + 2))
                            }
                            else -> sb.append("$indentStr  - $item\n")
                        }
                    }
                }
                is String -> sb.append("$indentStr$key: \"$value\"\n")
                is Boolean -> sb.append("$indentStr$key: $value\n")
                is Number -> sb.append("$indentStr$key: $value\n")
                else -> sb.append("$indentStr$key: $value\n")
            }
        }
        
        return sb.toString()
    }
    
    /**
     * 保存配置文件
     */
    fun saveConfig(config: String, filename: String = CONFIG_FILE): File {
        val configFile = File(context.filesDir, filename)
        configFile.writeText(config)
        Log.i(TAG, "Config saved to: ${configFile.absolutePath}")
        return configFile
    }
    
    /**
     * 加载基础配置
     */
    fun loadBaseConfig(configPath: String): Map<String, Any>? {
        return try {
            val file = File(configPath)
            if (!file.exists()) {
                Log.w(TAG, "Config file not found: $configPath")
                return null
            }
            
            // TODO: 实际应该使用YAML解析器
            // 这里简化处理
            val content = file.readText()
            parseYamlSimple(content)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load base config", e)
            null
        }
    }
    
    /**
     * 简单YAML解析（仅用于演示）
     * 实际应该使用snakeyaml等库
     */
    private fun parseYamlSimple(yaml: String): Map<String, Any> {
        // 这是一个非常简化的实现
        // 实际生产环境应该使用专业的YAML解析库
        return emptyMap()
    }
    
    /**
     * 应用配置到Clash核心
     */
    fun applyConfigToCore(baseConfig: Map<String, Any>? = null): Boolean {
        return try {
            // 构建配置
            val config = buildConfig(baseConfig)
            
            // 保存配置文件
            val configFile = saveConfig(config)
            
            // 加载到核心
            val result = ClashCore.loadConfig(configFile)
            
            if (result == 0) {
                Log.i(TAG, "Configuration applied successfully")
                true
            } else {
                Log.e(TAG, "Failed to apply configuration: $result")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying configuration", e)
            false
        }
    }
    
    /**
     * 获取当前配置摘要
     */
    fun getConfigSummary(): Map<String, String> {
        return mapOf(
            "TUN模式" to if (settingsManager.tunMode.value) "启用" else "禁用",
            "系统代理" to if (settingsManager.systemProxy.value) "启用" else "禁用",
            "局域网连接" to if (settingsManager.allowLan.value) "启用" else "禁用",
            "IPv6" to if (settingsManager.ipv6.value) "启用" else "禁用",
            "统一延迟" to if (settingsManager.unifiedDelay.value) "启用" else "禁用",
            "日志等级" to settingsManager.logLevel.value,
            "混合端口" to settingsManager.mixedPort.value,
            "外部控制" to settingsManager.externalController.value.ifEmpty { "未设置" },
            "主题" to settingsManager.theme.value
        )
    }
}

