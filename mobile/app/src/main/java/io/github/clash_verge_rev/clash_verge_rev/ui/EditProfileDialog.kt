package io.github.clash_verge_rev.clash_verge_rev.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.clash_verge_rev.clash_verge_rev.data.ProfileStorage
import io.github.clash_verge_rev.clash_verge_rev.ui.theme.AppDimensions
import kotlinx.coroutines.launch

/**
 * 编辑订阅信息对话框（对应桌面端 ProfileViewer）
 * 支持编辑订阅名称、描述、URL、代理设置等
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    metadata: ProfileStorage.ProfileMetadata?,
    onDismiss: () -> Unit,
    onSave: (ProfileStorage.ProfileMetadata) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // 表单状态
    var name by remember { mutableStateOf(metadata?.name ?: "") }
    var desc by remember { mutableStateOf(metadata?.desc ?: "") }
    var url by remember { mutableStateOf(metadata?.url ?: "") }
    var home by remember { mutableStateOf(metadata?.home ?: "") }
    var withProxy by remember { mutableStateOf(metadata?.option?.withProxy ?: false) }
    var selfProxy by remember { mutableStateOf(metadata?.option?.selfProxy ?: false) }
    var timeout by remember { mutableStateOf((metadata?.option?.timeoutSeconds ?: 30).toString()) }
    var userAgent by remember { mutableStateOf(metadata?.option?.userAgent ?: "") }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    
    // 代理选项互斥逻辑（对应桌面端）
    LaunchedEffect(withProxy) {
        if (withProxy) selfProxy = false
    }
    LaunchedEffect(selfProxy) {
        if (selfProxy) withProxy = false
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (metadata?.uid != null) "编辑订阅" else "新建订阅",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, "关闭")
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = AppDimensions.Spacing.medium))
                
                // 错误提示
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = AppDimensions.Spacing.medium),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(AppDimensions.Padding.cardMedium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(AppDimensions.Spacing.small))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                // 表单内容（可滚动）
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 订阅名称
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("订阅名称") },
                        placeholder = { Text("为订阅设置一个名称") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Label, null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(AppDimensions.Spacing.medium))
                    
                    // 订阅描述
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("描述") },
                        placeholder = { Text("可选：添加订阅描述") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Description, null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        minLines = 2,
                        maxLines = 4
                    )
                    
                    Spacer(modifier = Modifier.height(AppDimensions.Spacing.medium))
                    
                    // 订阅 URL（仅远程订阅）
                    if (metadata?.type == ProfileStorage.ProfileType.REMOTE || metadata == null) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("订阅 URL") },
                            placeholder = { Text("https://example.com/profile.yaml") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Rounded.Link, null) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            isError = url.isEmpty()
                        )
                        
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing.medium))
                        
                        // 主页 URL
                        OutlinedTextField(
                            value = home,
                            onValueChange = { home = it },
                            label = { Text("主页 URL（可选）") },
                            placeholder = { Text("https://example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Rounded.Home, null) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing.large))
                        
                        // 更新选项标题
                        Text(
                            text = "更新选项",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing.medium))
                        
                        // 使用系统代理
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("使用系统代理", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "更新时使用系统代理",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = withProxy,
                                onCheckedChange = { withProxy = it }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing.small))
                        
                        // 使用自身代理
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("使用自身代理", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "更新时使用订阅中的代理节点",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = selfProxy,
                                onCheckedChange = { selfProxy = it }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing.medium))
                        
                        // 超时设置
                        OutlinedTextField(
                            value = timeout,
                            onValueChange = { 
                                if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                                    timeout = it
                                }
                            },
                            label = { Text("超时时间（秒）") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Rounded.Timer, null) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing.medium))
                        
                        // User Agent
                        OutlinedTextField(
                            value = userAgent,
                            onValueChange = { userAgent = it },
                            label = { Text("User Agent（可选）") },
                            placeholder = { Text("留空使用默认") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, null) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            singleLine = true
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(AppDimensions.Spacing.large))
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(AppDimensions.Spacing.medium))
                    
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // 验证表单
                                if (name.isEmpty()) {
                                    errorMessage = "订阅名称不能为空"
                                    return@launch
                                }
                                
                                if (metadata?.type == ProfileStorage.ProfileType.REMOTE || metadata == null) {
                                    if (url.isEmpty()) {
                                        errorMessage = "订阅 URL 不能为空"
                                        return@launch
                                    }
                                }
                                
                                val timeoutSeconds = timeout.toIntOrNull() ?: 30
                                if (timeoutSeconds < 1 || timeoutSeconds > 300) {
                                    errorMessage = "超时时间必须在 1-300 秒之间"
                                    return@launch
                                }
                                
                                isSaving = true
                                errorMessage = null
                                
                                try {
                                    // 构建更新后的元数据
                                    val updatedMetadata = ProfileStorage.ProfileMetadata(
                                        uid = metadata?.uid ?: "",
                                        type = metadata?.type ?: ProfileStorage.ProfileType.REMOTE,
                                        name = name,
                                        desc = desc.ifEmpty { null },
                                        url = url,
                                        home = home.ifEmpty { null },
                                        selected = metadata?.selected ?: emptyList(),
                                        option = ProfileStorage.ProfileOption(
                                            withProxy = withProxy,
                                            selfProxy = selfProxy,
                                            timeoutSeconds = timeoutSeconds,
                                            userAgent = userAgent.ifEmpty { null }
                                        ),
                                        createdAt = metadata?.createdAt ?: System.currentTimeMillis(),
                                        updatedAt = metadata?.updatedAt ?: System.currentTimeMillis(),
                                        trafficTotal = metadata?.trafficTotal ?: 0,
                                        trafficUsed = metadata?.trafficUsed ?: 0,
                                        expireTime = metadata?.expireTime ?: 0,
                                        nodeCount = metadata?.nodeCount ?: 0
                                    )
                                    
                                    onSave(updatedMetadata)
                                } catch (e: Exception) {
                                    errorMessage = "保存失败: ${e.message}"
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(AppDimensions.Spacing.small))
                        }
                        Text(if (metadata?.uid != null) "保存" else "创建")
                    }
                }
            }
        }
    }
}

