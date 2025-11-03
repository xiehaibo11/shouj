package io.github.clash_verge_rev.clash_verge_rev.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 订阅配置管理器
 * 对应桌面端的create_profile功能
 * 负责下载订阅、解析配置、保存文件
 */
class ProfileManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ProfileManager"
        private const val TIMEOUT_SECONDS = 30L
        private const val USER_AGENT = "clash-verge-rev-mobile/2.4.3"
        
        @Volatile
        private var INSTANCE: ProfileManager? = null
        
        fun getInstance(context: Context): ProfileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProfileManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val profileStorage = ProfileStorage.getInstance(context)
    private val configDir = File(context.filesDir, "configs").apply { mkdirs() }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    
    /**
     * 从URL导入订阅
     * 对应桌面端的createProfile功能
     */
    suspend fun importSubscription(
        url: String,
        withProxy: Boolean = false,
        userAgent: String? = null
    ): Result<ProfileInfo> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Downloading subscription from: $url")
            
            // 1. 下载订阅内容
            val (content, responseHeaders) = downloadSubscriptionWithHeaders(url, userAgent ?: USER_AGENT)
            
            if (content.isEmpty()) {
                return@withContext Result.failure(Exception("订阅内容为空"))
            }
            
            Log.i(TAG, "Downloaded ${content.length} bytes")
            
            // 2. 从响应头或内容中提取名称
            val profileName = extractProfileName(content, url, responseHeaders)
            
            // 3. 自动注入 external-controller（如果缺失）
            var processedContent = content
            if (!content.contains("external-controller:", ignoreCase = true)) {
                Log.i(TAG, "Adding external-controller to config")
                processedContent = """
                    # Auto-injected by Clash Verge Rev
                    external-controller: 127.0.0.1:9090
                    secret: ""
                    
                """.trimIndent() + content
            }
            
            // 4. 自动注入 DNS 配置（关键修复 - TUN 模式必需）
            if (!processedContent.contains("dns:", ignoreCase = true) || 
                !processedContent.contains("enhanced-mode:", ignoreCase = true)) {
                Log.i(TAG, "Adding DNS configuration for TUN mode compatibility (Android optimized)")
                processedContent = """
                    # Auto-injected DNS configuration for TUN mode (Android optimized)
                    # Android 非 root 无法绑定 53 端口，使用 1053 + DNSHijack
                    dns:
                      enable: true
                      listen: 127.0.0.1:1053
                      enhanced-mode: fake-ip
                      fake-ip-range: 198.18.0.1/16
                      nameserver:
                        - https://1.1.1.1/dns-query
                        - https://8.8.8.8/dns-query
                      fallback:
                        - https://dns.alidns.com/dns-query
                        - https://doh.pub/dns-query
                      fallback-filter:
                        geoip: true
                        ipcidr:
                          - 240.0.0.0/4
                    
                """.trimIndent() + processedContent
            }
            
            // 4. 保存配置文件
            val configFile = saveProfile(profileName, processedContent)
            val uid = configFile.nameWithoutExtension
            
            Log.i(TAG, "Profile saved: ${configFile.absolutePath}")
            
            // 5. 提取供应商信息
            val providers = extractProviders(processedContent)
            
            val nodeCount = countNodes(processedContent)
            Log.i(TAG, "Profile imported: name=$profileName, nodes=$nodeCount, providers=${providers.size}")
            
            // 6. 解析流量信息
            val trafficInfo = parseTrafficInfo(responseHeaders)
            
            // 7. 保存元数据
            val metadata = ProfileStorage.ProfileMetadata(
                uid = uid,
                name = profileName,
                url = url,
                type = ProfileStorage.ProfileType.REMOTE,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                trafficTotal = trafficInfo["total"] ?: 0,
                trafficUsed = trafficInfo["used"] ?: 0,
                trafficUpload = trafficInfo["upload"] ?: 0,
                trafficDownload = trafficInfo["download"] ?: 0,
                expireTime = trafficInfo["expire"] ?: 0,
                updateInterval = 0,
                nodeCount = nodeCount,
                providers = providers
            )
            profileStorage.saveProfile(metadata)
            
            Result.success(ProfileInfo(
                uid = uid,
                name = profileName,
                url = url,
                file = configFile,
                providers = providers,
                nodeCount = nodeCount,
                trafficTotal = trafficInfo["total"] ?: 0,
                trafficUsed = trafficInfo["used"] ?: 0,
                trafficUpload = trafficInfo["upload"] ?: 0,
                trafficDownload = trafficInfo["download"] ?: 0,
                expireTime = trafficInfo["expire"] ?: 0,
                updatedAt = System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import subscription", e)
            Result.failure(e)
        }
    }
    
    /**
     * 下载订阅内容并返回响应头
     */
    private fun downloadSubscriptionWithHeaders(url: String, userAgent: String): Pair<String, Map<String, String>> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .get()
            .build()
        
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("下载失败: HTTP ${response.code}")
            }
            
            // 提取响应头
            val headers = mutableMapOf<String, String>()
            response.headers.forEach { pair ->
                headers[pair.first] = pair.second
            }
            
            val body = response.body ?: throw IOException("响应体为空")
            var content = body.string()
            
            // 检查是否是HTML错误页面
            if (isHtmlContent(content)) {
                throw IOException("订阅链接返回了HTML页面，可能需要登录或验证\n请检查订阅链接是否正确")
            }
            
            // 尝试base64解码（某些订阅服务器返回base64编码的内容）
            if (isBase64Content(content)) {
                try {
                    content = String(android.util.Base64.decode(content.trim(), android.util.Base64.DEFAULT))
                    Log.i(TAG, "Content decoded from base64")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decode base64, using original content")
                }
            }
            
            // 验证是否是有效的YAML配置
            if (!isValidClashConfig(content)) {
                throw IOException("下载的内容不是有效的Clash配置文件\n请确认订阅链接返回的是Clash配置")
            }
            
            return Pair(content, headers)
        }
    }
    
    
    /**
     * 检查内容是否是HTML
     */
    private fun isHtmlContent(content: String): Boolean {
        val trimmed = content.trim().take(1000) // 只检查前1000字符
        
        // 检查HTML特征
        return trimmed.startsWith("<!DOCTYPE", ignoreCase = true) ||
               trimmed.startsWith("<!doctype", ignoreCase = true) ||
               trimmed.startsWith("<html", ignoreCase = true) ||
               trimmed.contains("<head>", ignoreCase = true) ||
               trimmed.contains("<body>", ignoreCase = true) ||
               trimmed.contains("<title>", ignoreCase = true) ||
               trimmed.contains("<meta", ignoreCase = true) ||
               trimmed.contains("<script", ignoreCase = true) ||
               trimmed.contains("<div", ignoreCase = true) ||
               trimmed.contains("</html>", ignoreCase = true) ||
               // 检查是否包含HTML实体
               (trimmed.contains("&lt;") && trimmed.contains("&gt;")) ||
               // 检查文件名
               trimmed.contains(".html", ignoreCase = true) ||
               trimmed.contains(".htm", ignoreCase = true)
    }
    
    /**
     * 检查内容是否是base64编码
     */
    private fun isBase64Content(content: String): Boolean {
        val trimmed = content.trim()
        // base64只包含A-Za-z0-9+/=，且通常没有换行
        return trimmed.length > 50 && 
               trimmed.matches(Regex("^[A-Za-z0-9+/=\\s]+$")) &&
               !trimmed.contains(":") &&
               !trimmed.contains("-")
    }
    
    /**
     * 验证是否是有效的Clash配置
     */
    private fun isValidClashConfig(content: String): Boolean {
        val lines = content.lines()
        
        // 检查是否有YAML结构（至少有一些冒号和缩进）
        val hasYamlStructure = lines.any { line ->
            val trimmed = line.trim()
            trimmed.contains(":") && !trimmed.startsWith("#")
        }
        
        if (!hasYamlStructure) {
            Log.w(TAG, "Content doesn't have YAML structure")
            return false
        }
        
        // 检查是否包含Clash配置的关键字段
        var hasProxies = false
        var hasProxyGroups = false
        var hasRules = false
        var hasPort = false
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#")) continue // 跳过注释
            
            when {
                trimmed.startsWith("proxies:", ignoreCase = true) -> hasProxies = true
                trimmed.startsWith("proxy-providers:", ignoreCase = true) -> hasProxies = true
                trimmed.startsWith("proxy-groups:", ignoreCase = true) -> hasProxyGroups = true
                trimmed.startsWith("rules:", ignoreCase = true) -> hasRules = true
                trimmed.startsWith("rule-providers:", ignoreCase = true) -> hasRules = true
                trimmed.startsWith("port:", ignoreCase = true) -> hasPort = true
                trimmed.startsWith("mixed-port:", ignoreCase = true) -> hasPort = true
                trimmed.startsWith("socks-port:", ignoreCase = true) -> hasPort = true
            }
        }
        
        // 至少要有proxies或proxy-groups，并且最好有规则或端口配置
        val isValid = (hasProxies || hasProxyGroups) && (hasRules || hasPort)
        
        if (!isValid) {
            Log.w(TAG, "Invalid Clash config: hasProxies=$hasProxies, hasProxyGroups=$hasProxyGroups, hasRules=$hasRules, hasPort=$hasPort")
        }
        
        return isValid
    }
    
    /**
     * 从订阅内容中提取配置名称
     * 优先级：
     * 1. subscription-userinfo header中的name
     * 2. content-disposition header中的filename
     * 3. YAML注释中的名称
     * 4. URL中的文件名
     * 5. 使用时间戳
     */
    private fun extractProfileName(content: String, url: String, headers: Map<String, String> = emptyMap()): String {
        // 1. 从subscription-userinfo响应头提取
        headers["subscription-userinfo"]?.let { userInfo ->
            // 格式: upload=xxx; download=xxx; total=xxx; expire=xxx; name=xxx
            val nameMatch = Regex("name=([^;]+)").find(userInfo)
            nameMatch?.groupValues?.getOrNull(1)?.let { name ->
                val decoded = try {
                    java.net.URLDecoder.decode(name, "UTF-8")
                } catch (e: Exception) {
                    name
                }
                if (decoded.isNotEmpty()) {
                    Log.i(TAG, "Extracted name from subscription-userinfo: $decoded")
                    return sanitizeFileName(decoded)
                }
            }
        }
        
        // 2. 从content-disposition响应头提取
        headers["content-disposition"]?.let { disposition ->
            val filenameMatch = Regex("filename=\"?([^\"]+)\"?").find(disposition)
            filenameMatch?.groupValues?.getOrNull(1)?.let { filename ->
                val nameWithoutExt = filename.removeSuffix(".yaml").removeSuffix(".yml")
                if (nameWithoutExt.isNotEmpty()) {
                    Log.i(TAG, "Extracted name from content-disposition: $nameWithoutExt")
                    return sanitizeFileName(nameWithoutExt)
                }
            }
        }
        try {
            // 尝试从YAML注释中提取名称
            val lines = content.lines()
            for (line in lines.take(20)) {  // 只检查前20行
                if (line.trim().startsWith("#")) {
                    // 检查是否包含名称信息
                    val cleanLine = line.trim().removePrefix("#").trim()
                    if (cleanLine.startsWith("Name:") || cleanLine.startsWith("name:")) {
                        val name = cleanLine.substringAfter(":").trim()
                        if (name.isNotEmpty()) {
                            return sanitizeFileName(name)
                        }
                    }
                    if (cleanLine.startsWith("Subscription:") || cleanLine.startsWith("subscription:")) {
                        val name = cleanLine.substringAfter(":").trim()
                        if (name.isNotEmpty()) {
                            return sanitizeFileName(name)
                        }
                    }
                }
            }
            
            // 从URL中提取名称
            val urlName = url.substringAfterLast("/")
                .substringBefore("?")
                .substringBefore("&")
            
            // 检查是否是技术性文件名（如 .htm, .php 等）
            if (urlName.isNotEmpty() && urlName != url) {
                val lowerName = urlName.lowercase()
                
                // 如果是常见的技术文件扩展名，使用友好的中文名称
                val isTechnicalFile = lowerName.endsWith(".htm") || 
                                     lowerName.endsWith(".html") ||
                                     lowerName.endsWith(".php") ||
                                     lowerName.endsWith(".asp") ||
                                     lowerName.endsWith(".aspx") ||
                                     lowerName.endsWith(".jsp") ||
                                     lowerName.contains("verify") ||
                                     lowerName.contains("download") ||
                                     lowerName.contains("config") ||
                                     lowerName.contains("sub")
                
                if (!isTechnicalFile) {
                    return sanitizeFileName(urlName)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract profile name", e)
        }
        
        // 使用更友好的中文默认名称
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
        val timestamp = dateFormat.format(java.util.Date())
        return "订阅配置 $timestamp"
    }
    
    /**
     * 清理文件名，移除非法字符
     */
    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")  // 移除非法字符
            .replace(Regex("\\s+"), "_")              // 空格替换为下划线
            .take(50)                                  // 限制长度
            .trim('_')                                 // 移除首尾下划线
    }
    
    /**
     * 保存配置到文件
     */
    private fun saveProfile(name: String, content: String): File {
        val configDir = File(context.filesDir, "configs")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        
        // 生成唯一文件名
        var filename = "$name.yaml"
        var file = File(configDir, filename)
        var counter = 1
        
        while (file.exists()) {
            filename = "${name}_${counter}.yaml"
            file = File(configDir, filename)
            counter++
        }
        
        file.writeText(content)
        return file
    }
    
    /**
     * 从配置中提取供应商信息
     */
    private fun extractProviders(content: String): List<String> {
        val providers = mutableListOf<String>()
        try {
            val lines = content.lines()
            var inProxyProviders = false
            
            for (line in lines) {
                val trimmed = line.trim()
                
                if (trimmed.startsWith("proxy-providers:")) {
                    inProxyProviders = true
                    continue
                }
                
                if (inProxyProviders) {
                    // 遇到下一个顶级key，退出
                    if (trimmed.isNotEmpty() && !trimmed.startsWith(" ") && !trimmed.startsWith("#") && trimmed.endsWith(":")) {
                        break
                    }
                    
                    // 提取provider名称
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        if (line.startsWith("  ") && !line.startsWith("    ") && trimmed.endsWith(":")) {
                            val providerName = trimmed.removeSuffix(":")
                            providers.add(providerName)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract providers", e)
        }
        
        return providers
    }
    
    /**
     * 统计节点数量
     */
    private fun countNodes(content: String): Int {
        return try {
            val lines = content.lines()
            var count = 0
            var inProxies = false
            
            for (line in lines) {
                val trimmed = line.trim()
                
                if (trimmed == "proxies:") {
                    inProxies = true
                    continue
                }
                
                if (inProxies) {
                    // 遇到下一个顶级key，退出
                    if (trimmed.isNotEmpty() && !trimmed.startsWith(" ") && !trimmed.startsWith("-") && !trimmed.startsWith("#") && trimmed.endsWith(":")) {
                        break
                    }
                    
                    // 统计代理节点
                    if (trimmed.startsWith("- name:") || trimmed.startsWith("-name:")) {
                        count++
                    }
                }
            }
            
            count
        } catch (e: Exception) {
            Log.w(TAG, "Failed to count nodes", e)
            0
        }
    }
    
    /**
     * 解析订阅流量信息
     * 从 subscription-userinfo 响应头中提取流量数据
     * 格式: upload=xxx; download=xxx; total=xxx; expire=xxx
     */
    private fun parseTrafficInfo(headers: Map<String, String>): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        
        try {
            val userInfo = headers["subscription-userinfo"] ?: return result
            
            // 解析键值对
            userInfo.split(";").forEach { part ->
                val trimmed = part.trim()
                val keyValue = trimmed.split("=", limit = 2)
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim()
                    val value = keyValue[1].trim().toLongOrNull() ?: 0
                    
                    when (key) {
                        "upload" -> result["upload"] = value
                        "download" -> result["download"] = value
                        "total" -> result["total"] = value
                        "expire" -> result["expire"] = value
                    }
                }
            }
            
            // 计算已用流量
            val upload = result["upload"] ?: 0
            val download = result["download"] ?: 0
            result["used"] = upload + download
            
            Log.i(TAG, "Parsed traffic info: $result")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse traffic info", e)
        }
        
        return result
    }
    
    /**
     * 更新订阅（重新下载）
     */
    suspend fun updateSubscription(file: File, url: String): Result<ProfileInfo> = withContext(Dispatchers.IO) {
        try {
            val (content, headers) = downloadSubscriptionWithHeaders(url, USER_AGENT)
            
            if (content.isEmpty()) {
                return@withContext Result.failure(Exception("订阅内容为空"))
            }
            
            // 自动注入 external-controller（如果缺失）
            var processedContent = content
            if (!content.contains("external-controller:", ignoreCase = true)) {
                Log.i(TAG, "Adding external-controller to updated config")
                processedContent = """
                    # Auto-injected by Clash Verge Rev
                    external-controller: 127.0.0.1:9090
                    secret: ""
                    
                """.trimIndent() + content
            }
            
            // 自动注入 DNS 配置（关键修复 - TUN 模式必需）
            if (!processedContent.contains("dns:", ignoreCase = true) || 
                !processedContent.contains("enhanced-mode:", ignoreCase = true)) {
                Log.i(TAG, "Adding DNS configuration to updated config for TUN mode (Android optimized)")
                processedContent = """
                    # Auto-injected DNS configuration for TUN mode (Android optimized)
                    # Android 非 root 无法绑定 53 端口，使用 1053 + DNSHijack
                    dns:
                      enable: true
                      listen: 127.0.0.1:1053
                      enhanced-mode: fake-ip
                      fake-ip-range: 198.18.0.1/16
                      nameserver:
                        - https://1.1.1.1/dns-query
                        - https://8.8.8.8/dns-query
                      fallback:
                        - https://dns.alidns.com/dns-query
                        - https://doh.pub/dns-query
                      fallback-filter:
                        geoip: true
                        ipcidr:
                          - 240.0.0.0/4
                    
                """.trimIndent() + processedContent
            }
            
            // 覆盖原文件
            file.writeText(processedContent)
            val uid = file.nameWithoutExtension
            
            val providers = extractProviders(processedContent)
            val nodeCount = countNodes(processedContent)
            
            // 解析流量信息
            val trafficInfo = parseTrafficInfo(headers)
            
            // 更新元数据
            val existingMetadata = profileStorage.getProfile(uid)
            if (existingMetadata != null) {
                val updatedMetadata = existingMetadata.copy(
                    updatedAt = System.currentTimeMillis(),
                    trafficTotal = trafficInfo["total"] ?: 0,
                    trafficUsed = trafficInfo["used"] ?: 0,
                    trafficUpload = trafficInfo["upload"] ?: 0,
                    trafficDownload = trafficInfo["download"] ?: 0,
                    expireTime = trafficInfo["expire"] ?: 0,
                    nodeCount = nodeCount,
                    providers = providers
                )
                profileStorage.saveProfile(updatedMetadata)
            }
            
            Result.success(ProfileInfo(
                uid = uid,
                name = file.nameWithoutExtension,
                url = url,
                file = file,
                providers = providers,
                nodeCount = nodeCount,
                trafficTotal = trafficInfo["total"] ?: 0,
                trafficUsed = trafficInfo["used"] ?: 0,
                trafficUpload = trafficInfo["upload"] ?: 0,
                trafficDownload = trafficInfo["download"] ?: 0,
                expireTime = trafficInfo["expire"] ?: 0,
                updatedAt = System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update subscription", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取所有配置文件
     */
    fun getAllProfiles(): List<File> {
        val configDir = File(context.filesDir, "configs")
        if (!configDir.exists()) {
            return emptyList()
        }
        
        return configDir.listFiles { file ->
            file.extension == "yaml" || file.extension == "yml"
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * 获取所有订阅的完整信息（包括元数据）
     */
    suspend fun getAllProfilesWithMetadata(): List<Pair<File, ProfileStorage.ProfileMetadata?>> = withContext(Dispatchers.IO) {
        val files = getAllProfiles()
        files.map { file ->
            val uid = file.nameWithoutExtension
            val metadata = profileStorage.getProfile(uid)
            file to metadata
        }
    }
    
    /**
     * 检查订阅URL是否已存在
     */
    suspend fun isSubscriptionExists(url: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val allProfiles = profileStorage.getAllProfiles()
        Log.d(TAG, "Checking subscription existence for URL: $url")
        Log.d(TAG, "Total profiles in storage: ${allProfiles.size}")
        
        allProfiles.forEach { profile ->
            Log.d(TAG, "  - Profile: ${profile.name} (${profile.uid}), URL: ${profile.url}, Type: ${profile.type}")
        }
        
        val existing = allProfiles.find { it.url == url && it.type == ProfileStorage.ProfileType.REMOTE }
        if (existing != null) {
            Log.i(TAG, "Subscription exists: ${existing.name}")
            true to existing.name
        } else {
            Log.i(TAG, "Subscription does not exist")
            false to null
        }
    }
    
    /**
     * 删除配置文件
     */
    fun deleteProfile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete profile", e)
            false
        }
    }
}

/**
 * 配置信息
 */
data class ProfileInfo(
    val uid: String,            // 唯一标识（文件名）
    val name: String,           // 配置名称
    val url: String,            // 订阅URL
    val file: File,             // 配置文件
    val providers: List<String>, // 供应商列表
    val nodeCount: Int,         // 节点数量
    val trafficTotal: Long = 0,      // 总流量（字节）
    val trafficUsed: Long = 0,       // 已用流量（字节）
    val trafficUpload: Long = 0,     // 上传流量（字节）
    val trafficDownload: Long = 0,   // 下载流量（字节）
    val expireTime: Long = 0,        // 过期时间（秒）
    val updatedAt: Long = System.currentTimeMillis() // 更新时间
)

