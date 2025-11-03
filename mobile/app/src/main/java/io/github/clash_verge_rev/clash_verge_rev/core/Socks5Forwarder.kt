package io.github.clash_verge_rev.clash_verge_rev.core

import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap

/**
 * SOCKS5 TCP转发器
 * 将TUN接口的TCP连接转发到本地SOCKS5代理
 */
class Socks5Forwarder(
    private val proxyHost: String = "127.0.0.1",
    private val proxyPort: Int = 7897
) {
    companion object {
        private const val TAG = "Socks5Forwarder"
        private const val SOCKS5_VERSION = 0x05.toByte()
        private const val CMD_CONNECT = 0x01.toByte()
        private const val ATYP_IPV4 = 0x01.toByte()
        private const val ATYP_DOMAIN = 0x03.toByte()
    }

    private val activeConnections = ConcurrentHashMap<String, Job>()

    /**
     * 转发TCP连接到SOCKS5代理
     */
    fun forwardTcpConnection(
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int,
        scope: CoroutineScope
    ): Job {
        val connectionKey = "TCP:$srcIp:$srcPort->$dstIp:$dstPort"
        
        // 如果连接已存在，返回现有Job
        activeConnections[connectionKey]?.let {
            if (it.isActive) {
                Log.d(TAG, "Connection already exists: $connectionKey")
                return it
            }
        }

        val job = scope.launch(Dispatchers.IO) {
            var proxySocket: Socket? = null
            try {
                Log.d(TAG, "⚡ Establishing SOCKS5 connection: $connectionKey")
                
                // 1. 连接到SOCKS5代理
                proxySocket = Socket()
                proxySocket.connect(InetSocketAddress(proxyHost, proxyPort), 5000)
                proxySocket.soTimeout = 30000 // 30s 超时
                
                val inputStream = proxySocket.getInputStream()
                val outputStream = proxySocket.getOutputStream()
                
                // 2. SOCKS5握手
                // 发送: VER=5, NMETHODS=1, METHODS=[0x00] (无认证)
                outputStream.write(byteArrayOf(SOCKS5_VERSION, 0x01, 0x00))
                outputStream.flush()
                
                // 接收: VER=5, METHOD=0x00
                val greeting = ByteArray(2)
                if (inputStream.read(greeting) != 2 || greeting[0] != SOCKS5_VERSION) {
                    Log.e(TAG, "SOCKS5 handshake failed for $connectionKey")
                    return@launch
                }
                
                // 3. 发送连接请求
                // VER=5, CMD=CONNECT, RSV=0, ATYP=IPv4
                val request = ByteBuffer.allocate(10)
                request.put(SOCKS5_VERSION)
                request.put(CMD_CONNECT)
                request.put(0x00) // RSV
                request.put(ATYP_IPV4)
                
                // 目标IP (4字节)
                val ipParts = dstIp.split(".")
                ipParts.forEach { request.put(it.toInt().toByte()) }
                
                // 目标端口 (2字节, 大端序)
                request.put((dstPort shr 8).toByte())
                request.put((dstPort and 0xFF).toByte())
                
                outputStream.write(request.array())
                outputStream.flush()
                
                // 4. 接收连接响应
                val response = ByteArray(10)
                val responseLen = inputStream.read(response)
                if (responseLen < 10 || response[1] != 0x00.toByte()) {
                    Log.e(TAG, "SOCKS5 connection failed: ${response[1].toInt()}")
                    return@launch
                }
                
                Log.i(TAG, "✅ SOCKS5 connection established: $connectionKey")
                
                // 5. TODO: 双向数据转发
                // 这里需要实现从TUN读取数据并写入proxySocket
                // 以及从proxySocket读取数据并写回TUN
                // 由于当前TunPacketHandler没有提供写入TUN的接口，
                // 这部分需要重构TunPacketHandler
                
                // 暂时保持连接打开，记录为活动连接
                delay(Long.MAX_VALUE)
                
            } catch (e: IOException) {
                Log.e(TAG, "SOCKS5 connection error for $connectionKey", e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error for $connectionKey", e)
            } finally {
                proxySocket?.close()
                activeConnections.remove(connectionKey)
                Log.d(TAG, "Connection closed: $connectionKey")
            }
        }
        
        activeConnections[connectionKey] = job
        return job
    }

    /**
     * 关闭所有连接
     */
    fun closeAll() {
        Log.i(TAG, "Closing all SOCKS5 connections (${activeConnections.size})")
        activeConnections.values.forEach { it.cancel() }
        activeConnections.clear()
    }
}


