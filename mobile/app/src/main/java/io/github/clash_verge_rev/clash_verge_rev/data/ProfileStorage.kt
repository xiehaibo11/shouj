package io.github.clash_verge_rev.clash_verge_rev.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 订阅配置元数据存储
 * 存储订阅URL、流量信息、更新时间等
 */
class ProfileStorage private constructor(private val context: Context) {

    private val TAG = "ProfileStorage"
    private val preferences: SharedPreferences =
        context.getSharedPreferences("profile_metadata", Context.MODE_PRIVATE)

    /**
     * 订阅配置信息
     */
    data class ProfileMetadata(
        val uid: String,                    // 唯一标识（文件名）
        val name: String,                   // 显示名称
        val url: String,                    // 订阅URL
        val type: ProfileType,              // 类型（远程/本地）
        val createdAt: Long,                // 创建时间
        val updatedAt: Long,                // 最后更新时间
        val trafficTotal: Long = 0,         // 总流量（字节）
        val trafficUsed: Long = 0,          // 已用流量（字节）
        val trafficUpload: Long = 0,        // 上传流量（字节）
        val trafficDownload: Long = 0,      // 下载流量（字节）
        val expireTime: Long = 0,           // 过期时间（秒时间戳）
        val updateInterval: Int = 0,        // 更新间隔（秒）
        val nodeCount: Int = 0,             // 节点数量
        val providers: List<String> = emptyList(), // 供应商列表
        val desc: String? = null,           // 描述（对应桌面端）
        val home: String? = null,           // 主页URL（对应桌面端）
        val selected: List<String> = emptyList(), // 选中的代理节点
        val option: ProfileOption? = null   // 更新选项（对应桌面端）
    )
    
    /**
     * 订阅更新选项（对应桌面端的option字段）
     */
    data class ProfileOption(
        val withProxy: Boolean = false,    // 使用系统代理
        val selfProxy: Boolean = false,    // 使用自身代理
        val timeoutSeconds: Int = 30,      // 超时时间（秒）
        val userAgent: String? = null      // 自定义User Agent
    )

    enum class ProfileType {
        REMOTE, LOCAL
    }

    /**
     * 保存订阅元数据
     */
    suspend fun saveProfile(metadata: ProfileMetadata) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("uid", metadata.uid)
                put("name", metadata.name)
                put("url", metadata.url)
                put("type", metadata.type.name)
                put("createdAt", metadata.createdAt)
                put("updatedAt", metadata.updatedAt)
                put("trafficTotal", metadata.trafficTotal)
                put("trafficUsed", metadata.trafficUsed)
                put("trafficUpload", metadata.trafficUpload)
                put("trafficDownload", metadata.trafficDownload)
                put("expireTime", metadata.expireTime)
                put("updateInterval", metadata.updateInterval)
                put("nodeCount", metadata.nodeCount)
                put("providers", JSONArray(metadata.providers))
                
                // 新增字段（对应桌面端）
                metadata.desc?.let { put("desc", it) }
                metadata.home?.let { put("home", it) }
                if (metadata.selected.isNotEmpty()) {
                    put("selected", JSONArray(metadata.selected))
                }
                metadata.option?.let { option ->
                    put("option", JSONObject().apply {
                        put("withProxy", option.withProxy)
                        put("selfProxy", option.selfProxy)
                        put("timeoutSeconds", option.timeoutSeconds)
                        option.userAgent?.let { put("userAgent", it) }
                    })
                }
            }

            // 使用 commit() 确保同步保存，避免时序问题
            val success = preferences.edit().putString(metadata.uid, json.toString()).commit()
            if (success) {
                Log.i(TAG, "Saved profile metadata: ${metadata.uid}")
            } else {
                Log.e(TAG, "Failed to commit save for: ${metadata.uid}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save profile metadata", e)
        }
    }

    /**
     * 获取订阅元数据
     */
    suspend fun getProfile(uid: String): ProfileMetadata? = withContext(Dispatchers.IO) {
        try {
            val jsonStr = preferences.getString(uid, null) ?: return@withContext null
            val json = JSONObject(jsonStr)

            ProfileMetadata(
                uid = json.getString("uid"),
                name = json.getString("name"),
                url = json.optString("url", ""),
                type = ProfileType.valueOf(json.getString("type")),
                createdAt = json.getLong("createdAt"),
                updatedAt = json.getLong("updatedAt"),
                trafficTotal = json.optLong("trafficTotal", 0),
                trafficUsed = json.optLong("trafficUsed", 0),
                trafficUpload = json.optLong("trafficUpload", 0),
                trafficDownload = json.optLong("trafficDownload", 0),
                expireTime = json.optLong("expireTime", 0),
                updateInterval = json.optInt("updateInterval", 0),
                nodeCount = json.optInt("nodeCount", 0),
                providers = json.optJSONArray("providers")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList(),
                // 新增字段（对应桌面端）
                desc = json.optString("desc")?.takeIf { it.isNotEmpty() },
                home = json.optString("home")?.takeIf { it.isNotEmpty() },
                selected = json.optJSONArray("selected")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList(),
                option = json.optJSONObject("option")?.let { optJson ->
                    ProfileOption(
                        withProxy = optJson.optBoolean("withProxy", false),
                        selfProxy = optJson.optBoolean("selfProxy", false),
                        timeoutSeconds = optJson.optInt("timeoutSeconds", 30),
                        userAgent = optJson.optString("userAgent")?.takeIf { it.isNotEmpty() }
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get profile metadata: $uid", e)
            null
        }
    }

    /**
     * 获取所有订阅元数据
     */
    suspend fun getAllProfiles(): List<ProfileMetadata> = withContext(Dispatchers.IO) {
        val profiles = mutableListOf<ProfileMetadata>()
        try {
            preferences.all.forEach { (uid, _) ->
                getProfile(uid)?.let { profiles.add(it) }
            }
            profiles.sortByDescending { it.updatedAt }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all profiles", e)
        }
        profiles
    }

    /**
     * 删除订阅元数据
     */
    suspend fun deleteProfile(uid: String) = withContext(Dispatchers.IO) {
        try {
            val exists = preferences.contains(uid)
            if (exists) {
                // 使用 commit() 确保同步删除，避免时序问题
                val success = preferences.edit().remove(uid).commit()
                if (success) {
                    Log.i(TAG, "Deleted profile metadata: $uid")
                } else {
                    Log.e(TAG, "Failed to commit deletion for: $uid")
                }
            } else {
                Log.w(TAG, "Profile metadata not found: $uid")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete profile metadata: $uid", e)
        }
    }

    /**
     * 更新流量信息
     */
    suspend fun updateTrafficInfo(
        uid: String,
        trafficTotal: Long,
        trafficUsed: Long,
        trafficUpload: Long,
        trafficDownload: Long
    ) = withContext(Dispatchers.IO) {
        getProfile(uid)?.let { profile ->
            saveProfile(
                profile.copy(
                    trafficTotal = trafficTotal,
                    trafficUsed = trafficUsed,
                    trafficUpload = trafficUpload,
                    trafficDownload = trafficDownload,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 更新节点数量
     */
    suspend fun updateNodeCount(uid: String, nodeCount: Int) = withContext(Dispatchers.IO) {
        getProfile(uid)?.let { profile ->
            saveProfile(profile.copy(nodeCount = nodeCount, updatedAt = System.currentTimeMillis()))
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ProfileStorage? = null

        fun getInstance(context: Context): ProfileStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProfileStorage(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

