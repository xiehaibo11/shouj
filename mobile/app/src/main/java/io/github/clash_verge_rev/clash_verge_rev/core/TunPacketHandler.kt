package io.github.clash_verge_rev.clash_verge_rev.core

import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * TUN数据包处理器 - Kotlin实现
 * 
 * 功能：
 * - 从VPN接口读取IP数据包
 * - 解析目标地址和端口
 * - 通过SOCKS5代理转发流量
 * - 写回响应到VPN接口
 * - 跟踪连接和统计流量
 */
class TunPacketHandler(
    private val vpnFd: ParcelFileDescriptor,
    private val mtu: Int = 9000,
    private val mixedProxyPort: Int = 7897
) {
    companion object {
        private const val TAG = "TunPacketHandler"
        
        // IP协议号
        private const val PROTOCOL_TCP = 6
        private const val PROTOCOL_UDP = 17
        private const val PROTOCOL_ICMP = 1
        
        // IP版本
        private const val IP_VERSION_4 = 4
        private const val IP_VERSION_6 = 6
    }
    
    private val isRunning = AtomicBoolean(false)
    private val inputStream = FileInputStream(vpnFd.fileDescriptor)
    private val outputStream = FileOutputStream(vpnFd.fileDescriptor)
    
    // 统计数据
    private val packetsReceived = AtomicLong(0)
    private val packetsSent = AtomicLong(0)
    private val bytesReceived = AtomicLong(0)
    private val bytesSent = AtomicLong(0)
    
    // 连接跟踪器和流量统计管理器
    private val connectionTracker = ConnectionTracker.getInstance()
    private val trafficStatsManager = TrafficStatsManager.getInstance()
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * 启动数据包处理
     */
    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            Log.i(TAG, "🚀 Starting TUN packet handler (MTU: $mtu, Proxy: 127.0.0.1:$mixedProxyPort)")
            scope.launch {
                processPackets()
            }
        }
    }
    
    /**
     * 停止数据包处理
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            Log.i(TAG, "⏹️ Stopping TUN packet handler")
            scope.cancel()
            try {
                inputStream.close()
                outputStream.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing streams", e)
            }
        }
    }
    
    /**
     * 获取流量统计
     */
    fun getStats(): TrafficStats {
        return TrafficStats(
            packetsRx = packetsReceived.get(),
            packetsTx = packetsSent.get(),
            bytesRx = bytesReceived.get(),
            bytesTx = bytesSent.get()
        )
    }
    
    /**
     * 主数据包处理循环
     */
    private suspend fun processPackets() {
        val buffer = ByteBuffer.allocate(mtu)
        buffer.order(ByteOrder.BIG_ENDIAN)
        
        Log.i(TAG, "📦 Packet processing loop started")
        
        while (isRunning.get()) {
            try {
                buffer.clear()
                val length = inputStream.read(buffer.array())
                
                if (length > 0) {
                    packetsReceived.incrementAndGet()
                    bytesReceived.addAndGet(length.toLong())
                    
                    buffer.limit(length)
                    processPacket(buffer)
                } else if (length < 0) {
                    Log.w(TAG, "EOF reached on TUN interface")
                    break
                }
            } catch (e: CancellationException) {
                Log.i(TAG, "Packet processing cancelled")
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error processing packet", e)
                delay(10) // 避免死循环
            }
        }
        
        Log.i(TAG, "📦 Packet processing loop stopped")
    }
    
    /**
     * 处理单个IP数据包
     */
    private suspend fun processPacket(buffer: ByteBuffer) {
        try {
            // 读取IP版本
            val versionAndHeaderLen = buffer.get(0).toInt()
            val version = (versionAndHeaderLen and 0xF0) shr 4
            
            when (version) {
                IP_VERSION_4 -> processIPv4Packet(buffer)
                IP_VERSION_6 -> processIPv6Packet(buffer)
                else -> {
                    Log.w(TAG, "⚠️ Unknown IP version: $version")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in processPacket", e)
        }
    }
    
    /**
     * 处理IPv4数据包
     */
    private suspend fun processIPv4Packet(buffer: ByteBuffer) {
        try {
            // 解析IP头
            val versionAndHeaderLen = buffer.get(0).toInt() and 0xFF
            val headerLen = (versionAndHeaderLen and 0x0F) * 4
            val protocol = buffer.get(9).toInt() and 0xFF
            
            // 源地址和目标地址（IPv4）
            val srcAddr = ByteArray(4)
            val dstAddr = ByteArray(4)
            buffer.position(12)
            buffer.get(srcAddr)
            buffer.get(dstAddr)
            
            val srcIp = formatIPv4(srcAddr)
            val dstIp = formatIPv4(dstAddr)
            
            val packetSize = buffer.limit().toLong()
            
            when (protocol) {
                PROTOCOL_TCP -> {
                    val (srcPort, dstPort) = parsePorts(buffer, headerLen)
                    Log.d(TAG, "📨 TCP: $srcIp:$srcPort -> $dstIp:$dstPort ($packetSize bytes)")
                    
                    // 记录连接
                    connectionTracker.addConnection(
                        protocol = "TCP",
                        srcIp = srcIp,
                        srcPort = srcPort,
                        dstIp = dstIp,
                        dstPort = dstPort,
                        proxy = "DIRECT" // TODO: 从配置获取实际代理
                    )
                    
                    // 更新流量（上传）
                    connectionTracker.updateTraffic(
                        protocol = "TCP",
                        srcIp = srcIp,
                        srcPort = srcPort,
                        dstIp = dstIp,
                        dstPort = dstPort,
                        uploadBytes = packetSize
                    )
                    trafficStatsManager.recordUpload(packetSize)
                    
                    // TODO: 通过SOCKS5代理转发TCP连接
                }
                PROTOCOL_UDP -> {
                    val (srcPort, dstPort) = parsePorts(buffer, headerLen)
                    Log.d(TAG, "📨 UDP: $srcIp:$srcPort -> $dstIp:$dstPort ($packetSize bytes)")
                    
                    // 记录连接
                    connectionTracker.addConnection(
                        protocol = "UDP",
                        srcIp = srcIp,
                        srcPort = srcPort,
                        dstIp = dstIp,
                        dstPort = dstPort,
                        proxy = "DIRECT"
                    )
                    
                    // 更新流量（上传）
                    connectionTracker.updateTraffic(
                        protocol = "UDP",
                        srcIp = srcIp,
                        srcPort = srcPort,
                        dstIp = dstIp,
                        dstPort = dstPort,
                        uploadBytes = packetSize
                    )
                    trafficStatsManager.recordUpload(packetSize)
                    
                    // TODO: 通过SOCKS5代理转发UDP数据
                }
                PROTOCOL_ICMP -> {
                    Log.d(TAG, "📨 ICMP: $srcIp -> $dstIp ($packetSize bytes)")
                    trafficStatsManager.recordUpload(packetSize)
                    // TODO: 处理ICMP（ping等）
                }
                else -> {
                    Log.d(TAG, "📨 Protocol $protocol: $srcIp -> $dstIp ($packetSize bytes)")
                    trafficStatsManager.recordUpload(packetSize)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing IPv4 packet", e)
        }
    }
    
    /**
     * 处理IPv6数据包
     */
    private suspend fun processIPv6Packet(buffer: ByteBuffer) {
        // TODO: 实现IPv6支持
        Log.d(TAG, "📨 IPv6 packet (not yet supported)")
    }
    
    /**
     * 解析TCP/UDP端口
     */
    private fun parsePorts(buffer: ByteBuffer, ipHeaderLen: Int): Pair<Int, Int> {
        buffer.position(ipHeaderLen)
        val srcPort = buffer.short.toInt() and 0xFFFF
        val dstPort = buffer.short.toInt() and 0xFFFF
        return Pair(srcPort, dstPort)
    }
    
    /**
     * 格式化IPv4地址
     */
    private fun formatIPv4(addr: ByteArray): String {
        return "${addr[0].toInt() and 0xFF}.${addr[1].toInt() and 0xFF}.${addr[2].toInt() and 0xFF}.${addr[3].toInt() and 0xFF}"
    }
    
    /**
     * 流量统计数据类
     */
    data class TrafficStats(
        val packetsRx: Long,
        val packetsTx: Long,
        val bytesRx: Long,
        val bytesTx: Long
    )
}

