package io.github.clash_verge_rev.clash_verge_rev.data

/**
 * 连接数据模型 - 完全对应桌面端的 IConnectionsItem
 */
data class Connection(
    val id: String,
    val metadata: ConnectionMetadata,
    val upload: Long,
    val download: Long,
    val start: String,
    val chains: List<String>,
    val rule: String,
    val rulePayload: String,
    val curUpload: Long = 0,
    val curDownload: Long = 0
)

data class ConnectionMetadata(
    val network: String,
    val type: String,
    val sourceIP: String,
    val destinationIP: String,
    val sourcePort: String,
    val destinationPort: String,
    val host: String,
    val dnsMode: String,
    val uid: Int,
    val process: String,
    val processPath: String,
    val specialProxy: String,
    val specialRules: String,
    val remoteDestination: String,
    val sniffHost: String
)

/**
 * 连接响应数据 - 对应桌面端的 IConnections
 */
data class ConnectionsResponse(
    val downloadTotal: Long = 0,
    val uploadTotal: Long = 0,
    val connections: List<Connection> = emptyList()
)

/**
 * 连接排序类型
 */
enum class ConnectionSortType {
    DEFAULT,      // 按开始时间排序
    UPLOAD,       // 按上传速度排序
    DOWNLOAD      // 按下载速度排序
}

