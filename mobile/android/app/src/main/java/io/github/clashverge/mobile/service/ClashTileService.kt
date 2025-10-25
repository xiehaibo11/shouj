package io.github.clashverge.mobile.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import io.github.clashverge.mobile.R

@RequiresApi(Build.VERSION_CODES.N)
class ClashTileService : TileService() {
    
    private var isVpnActive = false
    
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }
    
    override fun onClick() {
        super.onClick()
        
        // 切换 VPN 状态
        isVpnActive = !isVpnActive
        
        // TODO: 调用实际的 VPN 切换逻辑
        // 这里需要与 VPN 服务通信
        
        updateTile()
    }
    
    private fun updateTile() {
        qsTile?.apply {
            state = if (isVpnActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            
            label = if (isVpnActive) "Clash VPN 已连接" else "Clash VPN"
            
            subtitle = if (isVpnActive) "点击断开" else "点击连接"
            
            icon = Icon.createWithResource(
                applicationContext,
                if (isVpnActive) R.drawable.ic_tile else R.drawable.ic_tile
            )
            
            updateTile()
        }
    }
    
    fun setVpnState(active: Boolean) {
        isVpnActive = active
        updateTile()
    }
}

