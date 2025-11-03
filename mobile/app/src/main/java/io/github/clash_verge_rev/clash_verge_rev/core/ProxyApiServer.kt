package io.github.clash_verge_rev.clash_verge_rev.core

import android.util.Log
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * Mihomo HTTP API Server (Pure Kotlin Implementation)
 * 提供与Mihomo兼容的HTTP API，用于代理切换等功能
 */
class ProxyApiServer(private val port: Int = 9090) : NanoHTTPD("127.0.0.1", port) {
    
    private val TAG = "ProxyApiServer"
    private val gson = Gson()
    private val yaml = Yaml()
    
    // 当前配置路径和内容
    private var currentConfigPath: String? = null
    private var currentConfigData: Map<String, Any>? = null
    
    init {
        Log.i(TAG, "API Server created on port $port")
    }
    
    /**
     * 从文件加载配置
     */
    fun loadConfigFromFile(configPath: String) {
        try {
            val file = File(configPath)
            if (!file.exists()) {
                Log.e(TAG, "Config file not found: $configPath")
                return
            }
            
            @Suppress("UNCHECKED_CAST")
            currentConfigData = yaml.load(file.inputStream()) as? Map<String, Any>
            currentConfigPath = configPath
            Log.i(TAG, "✅ Config loaded from file: $configPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config from file", e)
        }
    }
    
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        
        Log.d(TAG, "API Request: $method $uri")
        
        return try {
            when {
                uri == "/version" && method == Method.GET -> handleVersion()
                uri == "/proxies" && method == Method.GET -> handleGetProxies()
                uri.startsWith("/proxies/") && method == Method.PUT -> handleSelectProxy(session)
                uri == "/configs" && method == Method.GET -> handleGetConfigs()
                uri == "/configs" && method == Method.PATCH -> handlePatchConfigs(session)
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "API Error: ${e.message}", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Internal error: ${e.message}"
            )
        }
    }
    
    /**
     * GET /version - 返回版本信息
     */
    private fun handleVersion(): Response {
        val response = mapOf(
            "version" to "Mihomo Android 1.18.1",
            "premium" to true,
            "meta" to true
        )
        return newJsonResponse(Response.Status.OK, response)
    }
    
    /**
     * GET /proxies - 获取所有代理信息
     */
    private fun handleGetProxies(): Response {
        val proxies = mutableMapOf<String, Any>()
        
        currentConfigData?.let { config ->
            @Suppress("UNCHECKED_CAST")
            val proxyGroups = config["proxy-groups"] as? List<Map<String, Any>>
            val proxyList = config["proxies"] as? List<Map<String, Any>>
            
            // 添加代理组
            proxyGroups?.forEach { group ->
                val name = group["name"] as? String ?: return@forEach
                val type = group["type"] as? String ?: "select"
                @Suppress("UNCHECKED_CAST")
                val groupProxies = group["proxies"] as? List<String> ?: emptyList()
                
                proxies[name] = mapOf(
                    "type" to type,
                    "now" to (groupProxies.firstOrNull() ?: ""),
                    "all" to groupProxies,
                    "history" to emptyList<Any>()
                )
            }
            
            // 添加单个代理
            proxyList?.forEach { proxy ->
                val name = proxy["name"] as? String ?: return@forEach
                val type = proxy["type"] as? String ?: "ss"
                
                proxies[name] = mapOf(
                    "type" to type,
                    "name" to name,
                    "history" to emptyList<Any>()
                )
            }
        }
        
        val response = mapOf("proxies" to proxies)
        return newJsonResponse(Response.Status.OK, response)
    }
    
    /**
     * PUT /proxies/{group} - 选择代理
     */
    private fun handleSelectProxy(session: IHTTPSession): Response {
        // 提取组名
        val groupName = session.uri.substringAfter("/proxies/")
        Log.i(TAG, "PUT /proxies - URI: ${session.uri}, Group: $groupName")
        
        if (groupName.isEmpty()) {
            Log.e(TAG, "❌ Group name is empty")
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Group name required")
        }
        
        // ✅ 正确读取PUT请求体（NanoHTTPD会保存到临时文件）
        val files = mutableMapOf<String, String>()
        try {
            session.parseBody(files)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to parse body", e)
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Failed to parse body: ${e.message}")
        }
        
        // NanoHTTPD把请求体保存到临时文件中，files["postData"]或files["content"]是文件路径
        val tempFilePath = files["postData"] ?: files["content"]
        Log.i(TAG, "Temp file path: $tempFilePath")
        
        if (tempFilePath.isNullOrEmpty()) {
            Log.e(TAG, "❌ No temp file path")
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Empty request body")
        }
        
        // ✅ 从临时文件读取JSON内容
        val postData = try {
            java.io.File(tempFilePath).readText()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to read temp file: $tempFilePath", e)
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Failed to read body: ${e.message}")
        }
        
        Log.i(TAG, "Request body JSON: $postData")
        
        if (postData.isEmpty()) {
            Log.e(TAG, "❌ Request body is empty")
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Empty request body")
        }
        
        // 解析JSON
        @Suppress("UNCHECKED_CAST")
        val request = try {
            gson.fromJson(postData, Map::class.java) as? Map<String, Any> ?: emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Invalid JSON: $postData", e)
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid JSON: ${e.message}")
        }
        
        val proxyName = request["name"]?.toString() ?: ""
        if (proxyName.isEmpty()) {
            Log.e(TAG, "❌ Proxy name is empty, request: $request")
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Proxy name required")
        }
        
        Log.i(TAG, "✅ Selecting proxy: $groupName -> $proxyName")
        
        // 更新配置
        val result = updateProxySelection(groupName, proxyName)
        return if (result) {
            Log.i(TAG, "✅ Proxy selection successful")
            newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "")
        } else {
            Log.e(TAG, "❌ Failed to update proxy selection")
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Failed to update proxy")
        }
    }
    
    /**
     * GET /configs - 获取配置信息
     */
    private fun handleGetConfigs(): Response {
        val response = mutableMapOf<String, Any>(
            "port" to 7890,
            "socks-port" to 7891,
            "mixed-port" to 7897,
            "allow-lan" to false,
            "mode" to "rule",
            "log-level" to "info",
            "external-controller" to "127.0.0.1:9090"
        )
        
        currentConfigData?.let { config ->
            config["mixed-port"]?.let { response["mixed-port"] = it }
            config["allow-lan"]?.let { response["allow-lan"] = it }
            config["mode"]?.let { response["mode"] = it }
            config["log-level"]?.let { response["log-level"] = it }
        }
        
        return newJsonResponse(Response.Status.OK, response)
    }
    
    /**
     * PATCH /configs - 更新配置（用于切换模式等）
     */
    private fun handlePatchConfigs(session: IHTTPSession): Response {
        // 读取请求体
        val body = mutableMapOf<String, String>()
        try {
            session.parseBody(body)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse body", e)
        }
        
        val postData = body["postData"] ?: ""
        
        // 解析JSON
        @Suppress("UNCHECKED_CAST")
        val request = try {
            gson.fromJson(postData, Map::class.java) as Map<String, Any>
        } catch (e: Exception) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid JSON")
        }
        
        Log.i(TAG, "PATCH /configs: $request")
        
        // 更新模式
        request["mode"]?.let { mode ->
            val modeStr = mode.toString().lowercase()
            if (modeStr in listOf("rule", "global", "direct")) {
                updateMode(modeStr)
                Log.i(TAG, "✅ Mode changed to: $modeStr")
            } else {
                Log.w(TAG, "Invalid mode: $modeStr")
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid mode")
            }
        }
        
        // 更新其他配置项
        request["allow-lan"]?.let { updateConfigValue("allow-lan", it) }
        request["log-level"]?.let { updateConfigValue("log-level", it) }
        
        return newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "")
    }
    
    /**
     * 更新模式
     */
    private fun updateMode(mode: String) {
        val configPath = currentConfigPath ?: return
        val configData = currentConfigData?.toMutableMap() ?: return
        
        try {
            // 更新内存中的配置
            configData["mode"] = mode
            currentConfigData = configData
            
            // 保存到文件
            saveConfigToFile(configData, configPath)
            
            Log.i(TAG, "Mode updated to $mode and saved to config")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update mode", e)
        }
    }
    
    /**
     * 更新配置值
     */
    private fun updateConfigValue(key: String, value: Any) {
        val configPath = currentConfigPath ?: return
        val configData = currentConfigData?.toMutableMap() ?: return
        
        try {
            configData[key] = value
            currentConfigData = configData
            saveConfigToFile(configData, configPath)
            Log.i(TAG, "Config $key updated to $value")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update config $key", e)
        }
    }
    
    /**
     * 更新代理选择
     */
    private fun updateProxySelection(groupName: String, proxyName: String): Boolean {
        val configPath = currentConfigPath ?: return false
        val configData = currentConfigData ?: return false
        
        try {
            @Suppress("UNCHECKED_CAST")
            val proxyGroups = configData["proxy-groups"] as? MutableList<MutableMap<String, Any>> ?: return false
            
            // 查找代理组
            var updated = false
            for (group in proxyGroups) {
                if (group["name"] == groupName) {
                    @Suppress("UNCHECKED_CAST")
                    val proxies = group["proxies"] as? List<String> ?: continue
                    
                    // 验证代理是否在组中
                    if (!proxies.contains(proxyName)) {
                        Log.e(TAG, "Proxy $proxyName not found in group $groupName")
                        return false
                    }
                    
                    // 将选中的代理移到第一位
                    val newProxies = mutableListOf(proxyName)
                    newProxies.addAll(proxies.filter { it != proxyName })
                    group["proxies"] = newProxies
                    updated = true
                    break
                }
            }
            
            if (!updated) {
                Log.e(TAG, "Proxy group $groupName not found")
                return false
            }
            
            // 保存到文件
            saveConfigToFile(configData, configPath)
            Log.i(TAG, "✅ Proxy selection saved: $groupName -> $proxyName")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update proxy selection", e)
            return false
        }
    }
    
    /**
     * 保存配置到文件
     */
    private fun saveConfigToFile(configData: Map<String, Any>, configPath: String) {
        try {
            val file = File(configPath)
            
            // 创建备份
            val backupFile = File(configPath + ".backup")
            if (file.exists()) {
                file.copyTo(backupFile, overwrite = true)
            }
            
            // 写入YAML
            file.writer().use { writer ->
                yaml.dump(configData, writer)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save config file", e)
            throw e
        }
    }
    
    /**
     * 创建JSON响应
     */
    private fun newJsonResponse(status: Response.Status, data: Any): Response {
        val json = gson.toJson(data)
        return newFixedLengthResponse(status, "application/json", json)
    }
    
    companion object {
        private var instance: ProxyApiServer? = null
        
        /**
         * 启动API服务器
         */
        fun start(port: Int = 9090): ProxyApiServer {
            if (instance == null) {
                instance = ProxyApiServer(port)
                instance?.start()
                Log.i("ProxyApiServer", "✅ API Server started on port $port")
            }
            return instance!!
        }
        
        /**
         * 停止API服务器
         */
        fun stop() {
            instance?.stop()
            instance = null
            Log.i("ProxyApiServer", "API Server stopped")
        }
        
        /**
         * 获取实例
         */
        fun getInstance(): ProxyApiServer? = instance
    }
}
