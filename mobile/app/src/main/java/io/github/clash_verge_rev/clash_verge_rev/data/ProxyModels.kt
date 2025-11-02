package io.github.clash_verge_rev.clash_verge_rev.data

/**
 * 代理相关数据模型
 * 对应桌面端的IProxyItem, IProxyGroupItem等
 */

/**
 * 代理节点
 */
data class ProxyNode(
    val name: String,
    val type: String,  // ss, vmess, trojan, http, socks5, direct, reject等
    val server: String? = null,
    val port: Int? = null,
    val udp: Boolean = false,
    val delay: Int? = null,  // 延迟ms，null表示未测试
    val history: List<DelayHistory> = emptyList()
)

/**
 * 延迟历史
 */
data class DelayHistory(
    val time: Long,
    val delay: Int
)

/**
 * 代理组
 */
data class ProxyGroup(
    val name: String,
    val type: String,  // Selector, URLTest, Fallback, LoadBalance, Relay等
    val now: String,  // 当前选中的代理节点名称
    val all: List<String>,  // 所有代理节点名称列表
    val proxies: List<ProxyNode> = emptyList(),  // 代理节点详情
    val udp: Boolean = false,
    val hidden: Boolean = false
)

/**
 * 代理模式
 */
enum class ProxyMode(val value: String) {
    RULE("rule"),
    GLOBAL("global"),
    DIRECT("direct");

    companion object {
        fun fromString(value: String): ProxyMode {
            return values().find { it.value == value } ?: RULE
        }
    }
}

/**
 * 代理测试状态
 */
sealed class TestStatus {
    object Idle : TestStatus()
    object Testing : TestStatus()
    data class Success(val delay: Int) : TestStatus()
    data class Failed(val message: String) : TestStatus()
}

/**
 * 代理数据状态
 */
data class ProxiesState(
    val mode: ProxyMode = ProxyMode.RULE,
    val groups: List<ProxyGroup> = emptyList(),
    val allProxies: Map<String, ProxyNode> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

