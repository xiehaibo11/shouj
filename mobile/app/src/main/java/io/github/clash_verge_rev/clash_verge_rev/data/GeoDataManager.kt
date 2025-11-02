package io.github.clash_verge_rev.clash_verge_rev.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

/**
 * GeoData管理器 - 下载和管理GeoIP/GeoSite数据库
 * 
 * 用于TUN模式的规则匹配，类似桌面端的updateGeo功能
 */
class GeoDataManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GeoDataManager"
        
        // GeoData下载地址（使用Mihomo官方源）
        private const val GEOIP_URL = "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geoip.dat"
        private const val GEOSITE_URL = "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geosite.dat"
        private const val MMDB_URL = "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/country.mmdb"
        
        // 备用CDN地址（国内加速）
        private const val GEOIP_CDN_URL = "https://cdn.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geoip.dat"
        private const val GEOSITE_CDN_URL = "https://cdn.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geosite.dat"
        private const val MMDB_CDN_URL = "https://cdn.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/country.mmdb"
        
        // 文件名
        private const val GEOIP_FILE = "geoip.dat"
        private const val GEOSITE_FILE = "geosite.dat"
        private const val MMDB_FILE = "country.mmdb"
    }
    
    private val geoDataDir: File by lazy {
        File(context.filesDir, "geodata").apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * 检查GeoData文件是否存在
     */
    fun isGeoDataAvailable(): Boolean {
        val geoipFile = File(geoDataDir, GEOIP_FILE)
        val geositeFile = File(geoDataDir, GEOSITE_FILE)
        val mmdbFile = File(geoDataDir, MMDB_FILE)
        
        val exists = geoipFile.exists() && geositeFile.exists() && mmdbFile.exists()
        Log.d(TAG, "GeoData available: $exists")
        return exists
    }
    
    /**
     * 获取GeoData文件信息
     */
    fun getGeoDataInfo(): Map<String, Any> {
        val geoipFile = File(geoDataDir, GEOIP_FILE)
        val geositeFile = File(geoDataDir, GEOSITE_FILE)
        val mmdbFile = File(geoDataDir, MMDB_FILE)
        
        return mapOf(
            "geoip_exists" to geoipFile.exists(),
            "geoip_size" to (if (geoipFile.exists()) formatFileSize(geoipFile.length()) else "0 B"),
            "geoip_path" to geoipFile.absolutePath,
            "geosite_exists" to geositeFile.exists(),
            "geosite_size" to (if (geositeFile.exists()) formatFileSize(geositeFile.length()) else "0 B"),
            "geosite_path" to geositeFile.absolutePath,
            "mmdb_exists" to mmdbFile.exists(),
            "mmdb_size" to (if (mmdbFile.exists()) formatFileSize(mmdbFile.length()) else "0 B"),
            "mmdb_path" to mmdbFile.absolutePath
        )
    }
    
    /**
     * 下载所有GeoData文件
     */
    suspend fun downloadGeoData(
        useCDN: Boolean = false,
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "开始下载GeoData，使用CDN: $useCDN")
            
            // 下载GeoIP
            onProgress("下载 GeoIP...", 0)
            downloadFile(
                if (useCDN) GEOIP_CDN_URL else GEOIP_URL,
                File(geoDataDir, GEOIP_FILE),
                GEOIP_CDN_URL
            ) { progress ->
                onProgress("下载 GeoIP...", progress / 3)
            }
            
            // 下载GeoSite
            onProgress("下载 GeoSite...", 33)
            downloadFile(
                if (useCDN) GEOSITE_CDN_URL else GEOSITE_URL,
                File(geoDataDir, GEOSITE_FILE),
                GEOSITE_CDN_URL
            ) { progress ->
                onProgress("下载 GeoSite...", 33 + progress / 3)
            }
            
            // 下载MMDB
            onProgress("下载 Country.mmdb...", 66)
            downloadFile(
                if (useCDN) MMDB_CDN_URL else MMDB_URL,
                File(geoDataDir, MMDB_FILE),
                MMDB_CDN_URL
            ) { progress ->
                onProgress("下载 Country.mmdb...", 66 + progress / 3)
            }
            
            onProgress("下载完成", 100)
            Log.i(TAG, "GeoData下载完成")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "下载GeoData失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 下载单个文件（带重试和备用地址）
     */
    private suspend fun downloadFile(
        url: String,
        targetFile: File,
        fallbackUrl: String? = null,
        onProgress: (Int) -> Unit = {}
    ) {
        var lastException: Exception? = null
        
        // 尝试主地址
        try {
            downloadFileDirect(url, targetFile, onProgress)
            return
        } catch (e: Exception) {
            Log.w(TAG, "主地址下载失败: $url", e)
            lastException = e
        }
        
        // 尝试备用地址
        if (fallbackUrl != null && fallbackUrl != url) {
            try {
                Log.i(TAG, "尝试备用地址: $fallbackUrl")
                downloadFileDirect(fallbackUrl, targetFile, onProgress)
                return
            } catch (e: Exception) {
                Log.w(TAG, "备用地址下载失败: $fallbackUrl", e)
                lastException = e
            }
        }
        
        throw lastException ?: Exception("下载失败")
    }
    
    /**
     * 直接下载文件
     */
    private fun downloadFileDirect(
        url: String,
        targetFile: File,
        onProgress: (Int) -> Unit
    ) {
        Log.d(TAG, "开始下载: $url -> ${targetFile.name}")
        
        val connection = URL(url).openConnection()
        connection.connectTimeout = 30000
        connection.readTimeout = 30000
        connection.setRequestProperty("User-Agent", "Clash-Verge-Rev/2.4.3 (Android)")
        
        val contentLength = connection.contentLengthLong
        Log.d(TAG, "文件大小: ${formatFileSize(contentLength)}")
        
        connection.getInputStream().use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L
                var lastProgress = 0
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    if (contentLength > 0) {
                        val progress = ((totalBytesRead * 100) / contentLength).toInt()
                        if (progress != lastProgress) {
                            onProgress(progress)
                            lastProgress = progress
                        }
                    }
                }
            }
        }
        
        Log.i(TAG, "下载完成: ${targetFile.name} (${formatFileSize(targetFile.length())})")
    }
    
    /**
     * 删除所有GeoData文件
     */
    fun deleteGeoData(): Boolean {
        Log.i(TAG, "删除GeoData文件")
        var success = true
        
        listOf(GEOIP_FILE, GEOSITE_FILE, MMDB_FILE).forEach { filename ->
            val file = File(geoDataDir, filename)
            if (file.exists()) {
                if (!file.delete()) {
                    Log.w(TAG, "删除失败: $filename")
                    success = false
                }
            }
        }
        
        return success
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        
        return String.format(
            "%.2f %s",
            bytes / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups.coerceIn(0, units.size - 1)]
        )
    }
    
    /**
     * 计算文件MD5（用于校验）
     */
    fun calculateMD5(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

