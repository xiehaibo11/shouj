package io.github.clash_verge_rev.clash_verge_rev.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream

/**
 * 代理数据仓库
 * 负责从配置文件加载真实的代理节点和代理组数据
 */
class ProxyRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "ProxyRepository"
        
        @Volatile
        private var INSTANCE: ProxyRepository? = null
        
        fun getInstance(context: Context): ProxyRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProxyRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 从配置文件加载代理数据
     */
    suspend fun loadProxiesFromConfig(configFile: File): ProxiesState = withContext(Dispatchers.IO) {
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
                    
                    val proxy = ProxyNode(
                        name = name,
                        type = type,
                        server = server,
                        port = port,
                        udp = udp
                    )
                    allProxies[name] = proxy
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse proxy: ${e.message}")
                }
            }
            
            // 解析代理组
            val groupsData = config["proxy-groups"] as? List<Map<String, Any>> ?: emptyList()
            Log.i(TAG, "Found ${groupsData.size} proxy groups")
            
            val groups = mutableListOf<ProxyGroup>()
            groupsData.forEach { groupData ->
                try {
                    val name = groupData["name"] as? String ?: return@forEach
                    val type = groupData["type"] as? String ?: "select"
                    val proxiesList = groupData["proxies"] as? List<String> ?: emptyList()
                    val use = groupData["use"] as? List<String> ?: emptyList()
                    val now = if (type.lowercase() in listOf("select", "fallback", "urltest", "loadbalance")) {
                        proxiesList.firstOrNull() ?: ""
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
        proxyName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // TODO: 调用Clash API切换代理
            // PUT /proxies/{groupName}
            // Body: { "name": proxyName }
            Log.i(TAG, "Switch proxy: $groupName -> $proxyName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch proxy", e)
            false
        }
    }
    
    /**
     * 切换代理模式
     */
    suspend fun switchMode(mode: ProxyMode): Boolean = withContext(Dispatchers.IO) {
        try {
            // TODO: 调用Clash API切换模式
            // PATCH /configs
            // Body: { "mode": mode.value }
            Log.i(TAG, "Switch mode: ${mode.value}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch mode", e)
            false
        }
    }
}

