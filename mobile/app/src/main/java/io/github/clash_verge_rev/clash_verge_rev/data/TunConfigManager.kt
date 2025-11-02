package io.github.clash_verge_rev.clash_verge_rev.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * TUN配置管理器
 * 
 * 管理TUN模式的所有配置参数，类似桌面端的tun配置
 */
class TunConfigManager private constructor(context: Context) {
    
    private val preferences: SharedPreferences =
        context.getSharedPreferences("tun_config", Context.MODE_PRIVATE)
    
    // Stack模式 (gvisor/system/mixed)
    private val _stack = mutableStateOf(preferences.getString("stack", "gvisor") ?: "gvisor")
    val stack: State<String> = _stack
    fun setStack(value: String) {
        _stack.value = value
        preferences.edit().putString("stack", value).apply()
    }
    
    // 设备名称
    private val _device = mutableStateOf(preferences.getString("device", "Mihomo") ?: "Mihomo")
    val device: State<String> = _device
    fun setDevice(value: String) {
        _device.value = value
        preferences.edit().putString("device", value).apply()
    }
    
    // 自动路由
    private val _autoRoute = mutableStateOf(preferences.getBoolean("auto_route", true))
    val autoRoute: State<Boolean> = _autoRoute
    fun setAutoRoute(value: Boolean) {
        _autoRoute.value = value
        preferences.edit().putBoolean("auto_route", value).apply()
    }
    
    // 严格路由
    private val _strictRoute = mutableStateOf(preferences.getBoolean("strict_route", false))
    val strictRoute: State<Boolean> = _strictRoute
    fun setStrictRoute(value: Boolean) {
        _strictRoute.value = value
        preferences.edit().putBoolean("strict_route", value).apply()
    }
    
    // 自动检测接口
    private val _autoDetectInterface = mutableStateOf(preferences.getBoolean("auto_detect_interface", true))
    val autoDetectInterface: State<Boolean> = _autoDetectInterface
    fun setAutoDetectInterface(value: Boolean) {
        _autoDetectInterface.value = value
        preferences.edit().putBoolean("auto_detect_interface", value).apply()
    }
    
    // DNS劫持
    private val _dnsHijack = mutableStateOf(preferences.getString("dns_hijack", "any:53") ?: "any:53")
    val dnsHijack: State<String> = _dnsHijack
    fun setDnsHijack(value: String) {
        _dnsHijack.value = value
        preferences.edit().putString("dns_hijack", value).apply()
    }
    
    // MTU
    private val _mtu = mutableStateOf(preferences.getInt("mtu", 1500))
    val mtu: State<Int> = _mtu
    fun setMtu(value: Int) {
        _mtu.value = value
        preferences.edit().putInt("mtu", value).apply()
    }
    
    /**
     * 重置为默认配置
     */
    fun resetToDefault() {
        setStack("gvisor")
        setDevice("Mihomo")
        setAutoRoute(true)
        setStrictRoute(false)
        setAutoDetectInterface(true)
        setDnsHijack("any:53")
        setMtu(1500)
    }
    
    /**
     * 获取完整配置（用于传递给Core）
     */
    fun getConfig(): Map<String, Any> {
        return mapOf(
            "stack" to _stack.value,
            "device" to _device.value,
            "auto-route" to _autoRoute.value,
            "strict-route" to _strictRoute.value,
            "auto-detect-interface" to _autoDetectInterface.value,
            "dns-hijack" to _dnsHijack.value.split(",").map { it.trim() },
            "mtu" to _mtu.value
        )
    }
    
    companion object {
        @Volatile
        private var INSTANCE: TunConfigManager? = null
        
        fun getInstance(context: Context): TunConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TunConfigManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

