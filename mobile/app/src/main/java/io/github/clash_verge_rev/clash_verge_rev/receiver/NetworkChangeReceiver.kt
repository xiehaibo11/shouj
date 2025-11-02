package io.github.clash_verge_rev.clash_verge_rev.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

/**
 * 网络变化监听 BroadcastReceiver
 * 监听网络状态变化，自动重连或切换代理
 */
class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NetworkChangeReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                Log.i(TAG, "Network connectivity changed")
                handleNetworkChange(context)
            }
            "android.net.wifi.WIFI_STATE_CHANGED" -> {
                Log.i(TAG, "WiFi state changed")
                handleNetworkChange(context)
            }
        }
    }

    /**
     * 处理网络变化
     */
    private fun handleNetworkChange(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            Log.w(TAG, "ConnectivityManager not available")
            return
        }

        val isConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo?.isConnected == true
        }

        if (isConnected) {
            Log.i(TAG, "Network connected")
            // TODO: 触发代理重连或规则刷新
            // - 检查 VPN 服务是否运行
            // - 重新连接代理节点
            // - 刷新订阅（如果需要）
        } else {
            Log.i(TAG, "Network disconnected")
            // TODO: 处理断网情况
        }
    }

    /**
     * 获取网络类型
     */
    private fun getNetworkType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) return "unknown"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return "none"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "unknown"

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
                else -> "other"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return "none"
            @Suppress("DEPRECATION")
            return when (networkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> "wifi"
                ConnectivityManager.TYPE_MOBILE -> "cellular"
                ConnectivityManager.TYPE_ETHERNET -> "ethernet"
                else -> "other"
            }
        }
    }
}
