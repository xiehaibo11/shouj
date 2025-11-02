package io.github.clash_verge_rev.clash_verge_rev.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

/**
 * 设置管理器 - 持久化和管理所有应用设置
 */
class SettingsManager private constructor(context: Context) {
    
    companion object {
        private const val TAG = "SettingsManager"
        private const val PREFS_NAME = "clash_verge_settings"
        
        @Volatile
        private var instance: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // ============ 系统设置 ============
    private val _tunMode = mutableStateOf(prefs.getBoolean("tun_mode", false))
    val tunMode: State<Boolean> = _tunMode
    
    private val _systemProxy = mutableStateOf(prefs.getBoolean("system_proxy", false))
    val systemProxy: State<Boolean> = _systemProxy
    
    private val _autoStart = mutableStateOf(prefs.getBoolean("auto_start", false))
    val autoStart: State<Boolean> = _autoStart
    
    private val _silentStart = mutableStateOf(prefs.getBoolean("silent_start", false))
    val silentStart: State<Boolean> = _silentStart
    
    // ============ Clash 设置 ============
    private val _allowLan = mutableStateOf(prefs.getBoolean("allow_lan", false))
    val allowLan: State<Boolean> = _allowLan
    
    private val _dnsOverwrite = mutableStateOf(prefs.getBoolean("dns_overwrite", false))
    val dnsOverwrite: State<Boolean> = _dnsOverwrite
    
    private val _ipv6 = mutableStateOf(prefs.getBoolean("ipv6", false))
    val ipv6: State<Boolean> = _ipv6
    
    private val _unifiedDelay = mutableStateOf(prefs.getBoolean("unified_delay", false))
    val unifiedDelay: State<Boolean> = _unifiedDelay
    
    private val _logLevel = mutableStateOf(prefs.getString("log_level", "Info") ?: "Info")
    val logLevel: State<String> = _logLevel
    
    private val _clashMode = mutableStateOf(prefs.getString("clash_mode", "rule") ?: "rule")
    val clashMode: State<String> = _clashMode
    
    // ============ 端口设置 ============
    private val _mixedPort = mutableStateOf(prefs.getString("mixed_port", "7890") ?: "7890")
    val mixedPort: State<String> = _mixedPort
    
    private val _externalController = mutableStateOf(
        prefs.getString("external_controller", "127.0.0.1:9090") ?: "127.0.0.1:9090"
    )
    val externalController: State<String> = _externalController
    
    // ============ 外观设置 ============
    private val _theme = mutableStateOf(prefs.getString("theme", "跟随系统") ?: "跟随系统")
    val theme: State<String> = _theme
    
    // ============ 当前配置 ============
    private val _currentConfigPath = mutableStateOf(prefs.getString("current_config_path", "") ?: "")
    val currentConfigPath: State<String> = _currentConfigPath
    
    // ============ 设置方法 ============
    
    fun setTunMode(enabled: Boolean) {
        _tunMode.value = enabled
        prefs.edit().putBoolean("tun_mode", enabled).apply()
        Log.i(TAG, "TUN模式: $enabled")
    }
    
    fun setSystemProxy(enabled: Boolean) {
        _systemProxy.value = enabled
        prefs.edit().putBoolean("system_proxy", enabled).apply()
        Log.i(TAG, "系统代理: $enabled")
    }
    
    fun setAutoStart(enabled: Boolean) {
        _autoStart.value = enabled
        prefs.edit().putBoolean("auto_start", enabled).apply()
        Log.i(TAG, "开机自启: $enabled")
    }
    
    fun setSilentStart(enabled: Boolean) {
        _silentStart.value = enabled
        prefs.edit().putBoolean("silent_start", enabled).apply()
        Log.i(TAG, "静默启动: $enabled")
    }
    
    fun setAllowLan(enabled: Boolean) {
        _allowLan.value = enabled
        prefs.edit().putBoolean("allow_lan", enabled).apply()
        Log.i(TAG, "局域网连接: $enabled")
    }
    
    fun setDnsOverwrite(enabled: Boolean) {
        _dnsOverwrite.value = enabled
        prefs.edit().putBoolean("dns_overwrite", enabled).apply()
        Log.i(TAG, "DNS覆写: $enabled")
    }
    
    fun setIpv6(enabled: Boolean) {
        _ipv6.value = enabled
        prefs.edit().putBoolean("ipv6", enabled).apply()
        Log.i(TAG, "IPv6: $enabled")
    }
    
    fun setUnifiedDelay(enabled: Boolean) {
        _unifiedDelay.value = enabled
        prefs.edit().putBoolean("unified_delay", enabled).apply()
        Log.i(TAG, "统一延迟: $enabled")
    }
    
    fun setLogLevel(level: String) {
        _logLevel.value = level
        prefs.edit().putString("log_level", level).apply()
        Log.i(TAG, "日志等级: $level")
    }
    
    fun setClashMode(mode: String) {
        _clashMode.value = mode
        prefs.edit().putString("clash_mode", mode).apply()
        Log.i(TAG, "Clash模式: $mode")
    }
    
    fun setMixedPort(port: String) {
        _mixedPort.value = port
        prefs.edit().putString("mixed_port", port).apply()
        Log.i(TAG, "混合端口: $port")
    }
    
    fun setExternalController(controller: String) {
        _externalController.value = controller
        prefs.edit().putString("external_controller", controller).apply()
        Log.i(TAG, "外部控制: $controller")
    }
    
    fun setTheme(theme: String) {
        _theme.value = theme
        prefs.edit().putString("theme", theme).apply()
        Log.i(TAG, "主题模式: $theme")
    }
    
    fun setCurrentConfigPath(path: String) {
        _currentConfigPath.value = path
        prefs.edit().putString("current_config_path", path).apply()
        Log.i(TAG, "当前配置: $path")
    }
    
    /**
     * 生成Clash配置的部分内容
     */
    fun getClashConfig(): Map<String, Any> {
        return mapOf(
            "mixed-port" to (_mixedPort.value.toIntOrNull() ?: 7890),
            "allow-lan" to _allowLan.value,
            "ipv6" to _ipv6.value,
            "log-level" to _logLevel.value.lowercase(),
            "external-controller" to _externalController.value
        )
    }
    
    /**
     * 导出所有设置
     */
    fun exportSettings(): Map<String, Any> {
        return mapOf(
            "tun_mode" to _tunMode.value,
            "system_proxy" to _systemProxy.value,
            "auto_start" to _autoStart.value,
            "silent_start" to _silentStart.value,
            "allow_lan" to _allowLan.value,
            "dns_overwrite" to _dnsOverwrite.value,
            "ipv6" to _ipv6.value,
            "unified_delay" to _unifiedDelay.value,
            "log_level" to _logLevel.value,
            "mixed_port" to _mixedPort.value,
            "external_controller" to _externalController.value,
            "theme" to _theme.value
        )
    }
    
    /**
     * 重置所有设置为默认值
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _tunMode.value = false
        _systemProxy.value = false
        _autoStart.value = false
        _silentStart.value = false
        _allowLan.value = false
        _dnsOverwrite.value = false
        _ipv6.value = false
        _unifiedDelay.value = false
        _logLevel.value = "Info"
        _mixedPort.value = "7890"
        _externalController.value = "127.0.0.1:9090"
        _theme.value = "跟随系统"
        Log.i(TAG, "所有设置已重置为默认值")
    }
}

