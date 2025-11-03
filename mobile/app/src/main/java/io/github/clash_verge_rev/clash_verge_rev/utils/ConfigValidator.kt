package io.github.clash_verge_rev.clash_verge_rev.utils

import android.util.Log
import java.io.File

/**
 * 配置文件验证和修复工具
 */
object ConfigValidator {
    private const val TAG = "ConfigValidator"
    
    /**
     * 验证并修复配置文件
     * 确保包含 external-controller 配置
     */
    fun validateAndFixConfig(configFile: File): Boolean {
        if (!configFile.exists()) {
            Log.e(TAG, "Config file not found: ${configFile.absolutePath}")
            return false
        }
        
        try {
            val content = configFile.readText()
            
            // 检查是否包含 external-controller
            if (!content.contains("external-controller:", ignoreCase = true)) {
                Log.w(TAG, "Config missing external-controller, adding it...")
                
                val fixedContent = """
                    # Auto-injected by Clash Verge Rev
                    external-controller: 127.0.0.1:9090
                    secret: ""
                    
                """.trimIndent() + content
                
                configFile.writeText(fixedContent)
                Log.i(TAG, "✓ Added external-controller to config")
                return true
            }
            
            Log.i(TAG, "✓ Config already has external-controller")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate config", e)
            return false
        }
    }
    
    /**
     * 检查配置是否包含必需的字段
     */
    fun checkRequiredFields(configFile: File): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()
        
        if (!configFile.exists()) {
            return results
        }
        
        try {
            val content = configFile.readText()
            
            results["external-controller"] = content.contains("external-controller:", ignoreCase = true)
            results["mixed-port"] = content.contains("mixed-port:", ignoreCase = true) || 
                                     content.contains("port:", ignoreCase = true)
            results["proxies"] = content.contains("proxies:", ignoreCase = true)
            results["proxy-groups"] = content.contains("proxy-groups:", ignoreCase = true)
            results["rules"] = content.contains("rules:", ignoreCase = true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check fields", e)
        }
        
        return results
    }
    
    /**
     * 获取配置中的 external-controller 端口
     */
    fun getExternalControllerPort(configFile: File): Int {
        try {
            if (!configFile.exists()) return 9090
            
            val content = configFile.readText()
            val lines = content.lines()
            
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("external-controller:", ignoreCase = true)) {
                    // 解析: "external-controller: 127.0.0.1:9090"
                    val parts = trimmed.split(":")
                    if (parts.size >= 3) {
                        return parts[2].trim().toIntOrNull() ?: 9090
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get port", e)
        }
        
        return 9090 // 默认端口
    }
}


