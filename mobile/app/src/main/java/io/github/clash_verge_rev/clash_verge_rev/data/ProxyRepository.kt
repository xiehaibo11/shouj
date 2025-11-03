package io.github.clash_verge_rev.clash_verge_rev.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 代理数据仓库
 * 负责从配置文件加载真实的代理节点和代理组数据
 */
class ProxyRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "ProxyRepository"
        private const val PREFS_NAME = "proxy_state"
        private const val KEY_SELECTED_GROUP_PREFIX = "selected_group_"
        private const val KEY_SCROLL_POSITION_PREFIX = "scroll_position_"
        private const val KEY_SELECTED_PROXY_PREFIX = "selected_proxy_"
        
        @Volatile
        private var INSTANCE: ProxyRepository? = null
        
        fun getInstance(context: Context): ProxyRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProxyRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // SharedPreferences 用于持久化存储（类似桌面端的localStorage）
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 缓存：配置文件路径 -> 代理数据
    private val proxyCache = mutableMapOf<String, CachedProxyData>()
    
    // 保存最后加载的配置路径，用于快速恢复
    private var lastLoadedConfigPath: String? = null
    
    // Mihomo HTTP API 基础URL
    private val baseUrl = "http://127.0.0.1:9090"
    
    // HTTP客户端（用于API调用）
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    
    private data class CachedProxyData(
        val proxiesState: ProxiesState,
        val lastModified: Long,
        val fileSize: Long
    )
    
    /**
     * 从配置文件加载代理数据（带缓存）
     */
    suspend fun loadProxiesFromConfig(configFile: File): ProxiesState = withContext(Dispatchers.IO) {
        val filePath = configFile.absolutePath
        val lastModified = configFile.lastModified()
        val fileSize = configFile.length()
        
        // 检查缓存
        val cached = proxyCache[filePath]
        if (cached != null && 
            cached.lastModified == lastModified && 
            cached.fileSize == fileSize) {
            Log.i(TAG, "Using cached proxy data for: $filePath")
            return@withContext cached.proxiesState
        }
        
        // 重新加载
        val proxiesState = loadProxiesFromConfigInternal(configFile)
        
        // 更新缓存
        if (proxiesState.error == null) {
            proxyCache[filePath] = CachedProxyData(
                proxiesState = proxiesState,
                lastModified = lastModified,
                fileSize = fileSize
            )
            lastLoadedConfigPath = filePath
            Log.i(TAG, "Cached proxy data for: $filePath")
        }
        
        proxiesState
    }
    
    /**
     * 获取缓存的代理状态（如果存在且有效）
     */
    fun getCachedProxiesState(configFile: File): ProxiesState? {
        val filePath = configFile.absolutePath
        val cached = proxyCache[filePath] ?: return null
        
        // 检查文件是否被修改
        if (configFile.lastModified() == cached.lastModified && 
            configFile.length() == cached.fileSize) {
            Log.i(TAG, "Found valid cache for: $filePath")
            return cached.proxiesState
        }
        
        Log.i(TAG, "Cache invalid for: $filePath")
        return null
    }
    
    /**
     * 检查是否有可用的缓存
     */
    fun hasCachedData(configPath: String): Boolean {
        return proxyCache.containsKey(configPath) && configPath == lastLoadedConfigPath
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        proxyCache.clear()
        lastLoadedConfigPath = null
        Log.i(TAG, "Proxy cache cleared")
    }
    
    /**
     * 清除特定文件的缓存
     */
    fun clearCache(filePath: String) {
        proxyCache.remove(filePath)
        if (lastLoadedConfigPath == filePath) {
            lastLoadedConfigPath = null
        }
        Log.i(TAG, "Cleared cache for: $filePath")
    }
    
    /**
     * 保存选中的代理组索引（持久化到SharedPreferences，类似桌面端的localStorage）
     */
    fun saveSelectedGroupIndex(configPath: String, index: Int) {
        val key = KEY_SELECTED_GROUP_PREFIX + configPath.hashCode()
        prefs.edit().putInt(key, index).apply()
        Log.d(TAG, "Saved selected group index for $configPath: $index")
    }
    
    /**
     * 获取选中的代理组索引（从SharedPreferences恢复）
     */
    fun getSelectedGroupIndex(configPath: String): Int {
        val key = KEY_SELECTED_GROUP_PREFIX + configPath.hashCode()
        val index = prefs.getInt(key, 0)
        Log.d(TAG, "Restored selected group index for $configPath: $index")
        return index
    }
    
    /**
     * 保存选中的代理节点（持久化到SharedPreferences，类似桌面端的localStorage）
     */
    fun saveSelectedProxy(configPath: String, groupName: String, proxyName: String) {
        val key = KEY_SELECTED_PROXY_PREFIX + "${configPath.hashCode()}_${groupName.hashCode()}"
        prefs.edit().putString(key, proxyName).apply()
        Log.d(TAG, "Saved selected proxy for $configPath group $groupName: $proxyName")
    }
    
    /**
     * 获取选中的代理节点（从SharedPreferences恢复）
     */
    fun getSelectedProxy(configPath: String, groupName: String): String? {
        val key = KEY_SELECTED_PROXY_PREFIX + "${configPath.hashCode()}_${groupName.hashCode()}"
        val proxyName = prefs.getString(key, null)
        Log.d(TAG, "Restored selected proxy for $configPath group $groupName: $proxyName")
        return proxyName
    }
    
    /**
     * 保存滚动位置（持久化到SharedPreferences，类似桌面端的localStorage）
     */
    fun saveScrollPosition(configPath: String, groupIndex: Int, position: Int) {
        val key = KEY_SCROLL_POSITION_PREFIX + "${configPath.hashCode()}_$groupIndex"
        prefs.edit().putInt(key, position).apply()
        Log.d(TAG, "Saved scroll position for $configPath group $groupIndex: $position")
    }
    
    /**
     * 获取滚动位置（从SharedPreferences恢复）
     */
    fun getScrollPosition(configPath: String, groupIndex: Int): Int {
        val key = KEY_SCROLL_POSITION_PREFIX + "${configPath.hashCode()}_$groupIndex"
        val position = prefs.getInt(key, 0)
        Log.d(TAG, "Restored scroll position for $configPath group $groupIndex: $position")
        return position
    }
    
    /**
     * 内部方法：从配置文件加载代理数据（无缓存）
     */
    private suspend fun loadProxiesFromConfigInternal(configFile: File): ProxiesState = withContext(Dispatchers.IO) {
        try {
            if (!configFile.exists()) {
                Log.e(TAG, "Config file not found: ${configFile.absolutePath}")
                return@withContext ProxiesState(error = "配置文件不存在")
            }
            
            Log.i(TAG, "Loading proxies from: ${configFile.absolutePath}")
            
            // 读取YAML配置
            val yaml = Yaml()
            val config = FileInputStream(configFile).use { input ->
                yaml.load<Map<String, Any>>(input) as? Map<String, Any>
            }
            
            if (config == null) {
                Log.e(TAG, "Failed to parse config file")
                return@withContext ProxiesState(error = "配置文件解析失败")
            }
            
            // 解析代理节点
            val proxiesData = config["proxies"] as? List<Map<String, Any>> ?: emptyList()
            Log.i(TAG, "Found ${proxiesData.size} proxies")
            
            val allProxies = mutableMapOf<String, ProxyNode>()
            proxiesData.forEach { proxyData ->
                try {
                    val name = proxyData["name"] as? String ?: return@forEach
                    val type = proxyData["type"] as? String ?: "unknown"
                    val server = proxyData["server"] as? String
                    val port = (proxyData["port"] as? Number)?.toInt()
                    val udp = proxyData["udp"] as? Boolean ?: false
                    val xudp = proxyData["xudp"] as? Boolean ?: false
                    val tfo = proxyData["tfo"] as? Boolean ?: false
                    val mptcp = proxyData["mptcp"] as? Boolean ?: false
                    val smux = proxyData["smux"] as? Boolean ?: false
                    val provider = proxyData["provider"] as? String
                    
                    val proxy = ProxyNode(
                        name = name,
                        type = type,
                        server = server,
                        port = port,
                        udp = udp,
                        xudp = xudp,
                        tfo = tfo,
                        mptcp = mptcp,
                        smux = smux,
                        provider = provider
                    )
                    allProxies[name] = proxy
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse proxy: ${e.message}")
                }
            }
            
            // 解析代理组
            val groupsData = config["proxy-groups"] as? List<Map<String, Any>> ?: emptyList()
            Log.i(TAG, "Found ${groupsData.size} proxy groups")
            
            val configPath = configFile.absolutePath
            val groups = mutableListOf<ProxyGroup>()
            groupsData.forEach { groupData ->
                try {
                    val name = groupData["name"] as? String ?: return@forEach
                    val type = groupData["type"] as? String ?: "select"
                    val proxiesList = groupData["proxies"] as? List<String> ?: emptyList()
                    val use = groupData["use"] as? List<String> ?: emptyList()
                    
                    // 尝试从SharedPreferences恢复选中的代理（类似桌面端的localStorage）
                    val savedProxy = getSelectedProxy(configPath, name)
                    val now = if (type.lowercase() in listOf("select", "fallback", "urltest", "loadbalance")) {
                        // 优先使用保存的代理，如果存在且在列表中
                        if (savedProxy != null && proxiesList.contains(savedProxy)) {
                            Log.i(TAG, "Restored selected proxy for group $name: $savedProxy")
                            savedProxy
                        } else {
                            proxiesList.firstOrNull() ?: ""
                        }
                    } else ""
                    
                    // 获取该组的代理节点详情
                    val groupProxies = mutableListOf<ProxyNode>()
                    proxiesList.forEach { proxyName ->
                        allProxies[proxyName]?.let { proxy ->
                            groupProxies.add(proxy)
                        } ?: run {
                            // 可能是代理组名称
                            when (proxyName) {
                                "DIRECT" -> groupProxies.add(ProxyNode(name = "DIRECT", type = "direct"))
                                "REJECT" -> groupProxies.add(ProxyNode(name = "REJECT", type = "reject"))
                                else -> {} // 忽略未知的代理名称
                            }
                        }
                    }
                    
                    val group = ProxyGroup(
                        name = name,
                        type = type,
                        now = now,
                        all = proxiesList,
                        proxies = groupProxies
                    )
                    groups.add(group)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse proxy group: ${e.message}")
                }
            }
            
            // 获取当前模式
            val modeStr = config["mode"] as? String ?: "rule"
            val mode = ProxyMode.fromString(modeStr)
            
            Log.i(TAG, "Loaded ${allProxies.size} proxies, ${groups.size} groups, mode: $mode")
            
            ProxiesState(
                mode = mode,
                groups = groups,
                allProxies = allProxies,
                isLoading = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load proxies", e)
            ProxiesState(
                error = "加载失败: ${e.message}",
                isLoading = false
            )
        }
    }
    
    /**
     * 测试代理节点延迟
     */
    suspend fun testProxyDelay(
        proxyName: String,
        testUrl: String = "https://www.gstatic.com/generate_204",
        timeout: Int = 5000
    ): TestStatus = withContext(Dispatchers.IO) {
        try {
            // 简单的HTTP ping测速
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(timeout.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(timeout.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            
            val request = okhttp3.Request.Builder()
                .url(testUrl)
                .get()
                .build()
            
            val startTime = System.currentTimeMillis()
            client.newCall(request).execute().use { response ->
                val delay = (System.currentTimeMillis() - startTime).toInt()
                if (response.isSuccessful) {
                    TestStatus.Success(delay)
                } else {
                    TestStatus.Failed("HTTP ${response.code}")
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            TestStatus.Failed("超时")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to test proxy delay for $proxyName", e)
            TestStatus.Failed(e.message ?: "测试失败")
        }
    }
    
    /**
     * 测试代理组所有节点延迟
     */
    suspend fun testGroupDelay(
        group: ProxyGroup,
        testUrl: String = "https://www.gstatic.com/generate_204",
        timeout: Int = 5000
    ): Map<String, TestStatus> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, TestStatus>()
        group.proxies.forEach { proxy ->
            if (proxy.type != "direct" && proxy.type != "reject") {
                results[proxy.name] = testProxyDelay(proxy.name, testUrl, timeout)
            }
        }
        results
    }
    
    /**
     * 切换代理节点
     */
    suspend fun switchProxy(
        groupName: String,
        proxyName: String,
        configPath: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 直接通过HTTP API调用Mihomo（临时方案）
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            val url = "http://127.0.0.1:9090/proxies/$groupName"
            val body = okhttp3.RequestBody.create(
                "application/json".toMediaType(),
                """{"name":"$proxyName"}"""
            )
            
            val request = okhttp3.Request.Builder()
                .url(url)
                .put(body)
                .build()
            
            val response = client.newCall(request).execute()
            val success = response.isSuccessful || response.code == 204
            response.close()
            
            if (success) {
                Log.i(TAG, "✓ Switch proxy success: $groupName -> $proxyName")
                
                // 保存选中状态到SharedPreferences
                val path = configPath ?: lastLoadedConfigPath
                if (path != null) {
                    saveSelectedProxy(path, groupName, proxyName)
                    
                    // ✅ 关键修复：同步更新缓存中的代理组状态
                    updateCachedProxySelection(path, groupName, proxyName)
                }
            } else {
                Log.e(TAG, "✗ Failed to switch proxy (HTTP ${response.code}): $groupName -> $proxyName")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to switch proxy (exception): ${e.message}", e)
            false
        }
    }
    
    /**
     * 更新缓存中的代理选择状态（修复切换页面后状态丢失问题）
     */
    private fun updateCachedProxySelection(configPath: String, groupName: String, proxyName: String) {
        val cached = proxyCache[configPath] ?: return
        
        // 查找并更新对应的代理组
        val updatedGroups = cached.proxiesState.groups.map { group ->
            if (group.name == groupName) {
                group.copy(now = proxyName)
            } else {
                group
            }
        }
        
        // 更新缓存
        val updatedState = cached.proxiesState.copy(groups = updatedGroups)
        proxyCache[configPath] = cached.copy(proxiesState = updatedState)
        
        Log.i(TAG, "Updated cache: $groupName -> $proxyName")
    }
    
    /**
     * 切换代理模式
     */
    suspend fun switchMode(mode: ProxyMode): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "⚙️ Switching mode to: ${mode.value}")
            
            // 构建请求体
            val requestBody = JSONObject().apply {
                put("mode", mode.value)
            }.toString()
            
            val request = Request.Builder()
                .url("$baseUrl/configs")
                .patch(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful || response.code == 204) {
                Log.i(TAG, "✅ Mode switched successfully to: ${mode.value}")
                return@withContext true
            } else {
                Log.e(TAG, "✗ Failed to switch mode: ${response.code} ${response.message}")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to switch mode (exception): ${e.message}", e)
            false
        }
    }
}

