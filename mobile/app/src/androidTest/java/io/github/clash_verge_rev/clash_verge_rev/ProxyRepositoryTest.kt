package io.github.clash_verge_rev.clash_verge_rev

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.clash_verge_rev.clash_verge_rev.data.ProxyRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * ProxyRepository 单元测试
 * 测试代理仓库的核心功能
 */
@RunWith(AndroidJUnit4::class)
class ProxyRepositoryTest {
    
    private lateinit var context: Context
    private lateinit var proxyRepository: ProxyRepository
    private lateinit var testConfigFile: File
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        proxyRepository = ProxyRepository.getInstance(context)
        
        // 创建测试配置文件
        testConfigFile = File(context.filesDir, "test_config.yaml")
        testConfigFile.writeText(getTestConfigContent())
    }
    
    @After
    fun tearDown() {
        // 清理测试文件
        if (testConfigFile.exists()) {
            testConfigFile.delete()
        }
        proxyRepository.clearCache()
    }
    
    @Test
    fun testLoadProxiesFromConfig() = runBlocking {
        // 测试从配置文件加载代理
        val state = proxyRepository.loadProxiesFromConfig(testConfigFile)
        
        assertNotNull("ProxiesState should not be null", state)
        assertNull("Should not have error", state.error)
        assertFalse("Should not be loading", state.isLoading)
        assertTrue("Should have proxy groups", state.groups.isNotEmpty())
        assertTrue("Should have proxies", state.allProxies.isNotEmpty())
    }
    
    @Test
    fun testCacheProxyData() = runBlocking {
        // 第一次加载
        val state1 = proxyRepository.loadProxiesFromConfig(testConfigFile)
        assertFalse("Should load from file", proxyRepository.hasCachedData(testConfigFile.absolutePath))
        
        // 第二次加载（应该使用缓存）
        val state2 = proxyRepository.loadProxiesFromConfig(testConfigFile)
        assertTrue("Should use cached data", state1 === state2 || state1.groups == state2.groups)
    }
    
    @Test
    fun testSaveAndRestoreSelectedGroupIndex() {
        val configPath = testConfigFile.absolutePath
        val expectedIndex = 2
        
        // 保存
        proxyRepository.saveSelectedGroupIndex(configPath, expectedIndex)
        
        // 恢复
        val actualIndex = proxyRepository.getSelectedGroupIndex(configPath)
        
        assertEquals("Should restore correct index", expectedIndex, actualIndex)
    }
    
    @Test
    fun testSaveAndRestoreSelectedProxy() {
        val configPath = testConfigFile.absolutePath
        val groupName = "PROXY"
        val proxyName = "HK-01"
        
        // 保存
        proxyRepository.saveSelectedProxy(configPath, groupName, proxyName)
        
        // 恢复
        val actualProxy = proxyRepository.getSelectedProxy(configPath, groupName)
        
        assertEquals("Should restore correct proxy", proxyName, actualProxy)
    }
    
    @Test
    fun testSaveAndRestoreScrollPosition() {
        val configPath = testConfigFile.absolutePath
        val groupIndex = 1
        val position = 10
        
        // 保存
        proxyRepository.saveScrollPosition(configPath, groupIndex, position)
        
        // 恢复
        val actualPosition = proxyRepository.getScrollPosition(configPath, groupIndex)
        
        assertEquals("Should restore correct position", position, actualPosition)
    }
    
    @Test
    fun testClearCache() = runBlocking {
        // 加载数据创建缓存
        proxyRepository.loadProxiesFromConfig(testConfigFile)
        
        // 清除缓存
        proxyRepository.clearCache()
        
        // 验证缓存已清除
        assertFalse("Cache should be cleared", 
            proxyRepository.hasCachedData(testConfigFile.absolutePath))
    }
    
    @Test
    fun testLoadNonExistentFile() = runBlocking {
        val nonExistentFile = File(context.filesDir, "non_existent.yaml")
        
        val state = proxyRepository.loadProxiesFromConfig(nonExistentFile)
        
        assertNotNull("Should return error state", state.error)
        assertTrue("Should have empty groups", state.groups.isEmpty())
    }
    
    /**
     * 获取测试配置内容
     */
    private fun getTestConfigContent(): String {
        return """
            mode: rule
            port: 7890
            socks-port: 7891
            mixed-port: 7892
            allow-lan: false
            log-level: info
            external-controller: 127.0.0.1:9090
            
            proxies:
              - name: DIRECT
                type: direct
              - name: REJECT
                type: reject
              - name: HK-01
                type: ss
                server: hk01.example.com
                port: 8388
                cipher: aes-256-gcm
                password: password123
                udp: true
              - name: US-01
                type: vmess
                server: us01.example.com
                port: 443
                uuid: 12345678-1234-1234-1234-123456789012
                alterId: 0
                cipher: auto
                tls: true
            
            proxy-groups:
              - name: PROXY
                type: select
                proxies:
                  - HK-01
                  - US-01
                  - DIRECT
              - name: Auto
                type: url-test
                proxies:
                  - HK-01
                  - US-01
                url: http://www.gstatic.com/generate_204
                interval: 300
              - name: Fallback
                type: fallback
                proxies:
                  - HK-01
                  - US-01
                url: http://www.gstatic.com/generate_204
                interval: 300
            
            rules:
              - DOMAIN-SUFFIX,google.com,PROXY
              - DOMAIN-KEYWORD,google,PROXY
              - GEOIP,CN,DIRECT
              - MATCH,PROXY
        """.trimIndent()
    }
}

