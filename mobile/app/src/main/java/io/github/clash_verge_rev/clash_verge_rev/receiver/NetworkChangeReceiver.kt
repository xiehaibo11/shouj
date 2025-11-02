package io.github.clash_verge_rev.clash_verge_rev.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

/**
 * Network Change Receiver
 * 网络状态变化接收器
 */
class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NetworkChangeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION,
            "android.net.wifi.WIFI_STATE_CHANGED" -> {
                Log.d(TAG, "Network state changed")
                
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
                    as ConnectivityManager
                
                val isConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val network = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                } else {
                    @Suppress("DEPRECATION")
                    val networkInfo = connectivityManager.activeNetworkInfo
                    networkInfo?.isConnected == true
                }
                
                Log.d(TAG, "Network connected: $isConnected")
                
                // TODO: 通知 Mihomo 核心网络状态变化
                // 可能需要重新建立连接或更新路由
                
                // 广播网络状态
                val statusIntent = Intent("io.github.clash_verge_rev.NETWORK_STATUS")
                statusIntent.putExtra("connected", isConnected)
                context.sendBroadcast(statusIntent)
            }
        }
    }
}

