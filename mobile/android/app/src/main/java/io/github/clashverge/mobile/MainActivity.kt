package io.github.clashverge.mobile

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.clashverge.mobile.vpn.ClashVpnService
import io.github.clashverge.mobile.service.VpnNotificationService

class MainActivity : AppCompatActivity() {
    
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusDesc: TextView
    private lateinit var btnToggleVPN: Button
    private lateinit var tvUpload: TextView
    private lateinit var tvDownload: TextView
    private lateinit var tvCurrentProxy: TextView
    
    private var isVPNConnected = false
    private lateinit var notificationService: VpnNotificationService
    
    companion object {
        private const val VPN_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        notificationService = VpnNotificationService(this)
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusDesc = findViewById(R.id.tvStatusDesc)
        btnToggleVPN = findViewById(R.id.btnToggleVPN)
        tvUpload = findViewById(R.id.tvUpload)
        tvDownload = findViewById(R.id.tvDownload)
        tvCurrentProxy = findViewById(R.id.tvCurrentProxy)
    }
    
    private fun setupListeners() {
        btnToggleVPN.setOnClickListener {
            if (isVPNConnected) {
                stopVPN()
            } else {
                requestVPNPermission()
            }
        }
    }
    
    private fun requestVPNPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            // 已经授权，直接启动
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVPN()
            } else {
                Toast.makeText(this, "VPN 权限被拒绝", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startVPN() {
        val intent = Intent(this, ClashVpnService::class.java).apply {
            action = ClashVpnService.ACTION_START
            putExtra(ClashVpnService.EXTRA_SERVER_ADDRESS, "127.0.0.1")
            putExtra(ClashVpnService.EXTRA_SERVER_PORT, 7897)
            putExtra(ClashVpnService.EXTRA_DNS, arrayOf("8.8.8.8", "8.8.4.4"))
        }
        startService(intent)
        
        updateVPNStatus(true)
        Toast.makeText(this, "VPN 已启动", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopVPN() {
        val intent = Intent(this, ClashVpnService::class.java).apply {
            action = ClashVpnService.ACTION_STOP
        }
        startService(intent)
        
        updateVPNStatus(false)
        Toast.makeText(this, "VPN 已停止", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateVPNStatus(connected: Boolean) {
        isVPNConnected = connected
        
        if (connected) {
            tvStatus.text = "已连接"
            tvStatusDesc.text = "VPN 正在运行"
            btnToggleVPN.text = "断开"
            tvCurrentProxy.text = "本地代理 (127.0.0.1:7897)"
        } else {
            tvStatus.text = "未连接"
            tvStatusDesc.text = "点击下方按钮启用 VPN"
            btnToggleVPN.text = "连接"
            tvCurrentProxy.text = "未选择"
        }
        
        // 更新通知
        notificationService.updateNotification(connected)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (!isVPNConnected) {
            notificationService.cancelNotification()
        }
    }
}
