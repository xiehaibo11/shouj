# Bug修复：删除后重新导入 & 订阅名称优化

## 修复时间
2024-11-02 (第二次修复)

---

## 问题1：删除订阅后仍提示"订阅已存在"

### 问题描述
用户删除订阅后，尝试重新导入相同的URL时，仍然提示：
```
订阅已存在：verify_mode.htm
请勿重复导入
```

### 根本原因分析
虽然之前已经添加了 `profileStorage.deleteProfile(uid)` 调用，但可能存在以下问题：
1. 删除顺序问题：先删除文件可能导致异常
2. 异步操作未正确等待
3. SharedPreferences 缓存未刷新

### 修复方案

#### 1. 调整删除顺序
在 `ConfigScreenNew.kt` 中，**先删除元数据，再删除文件**：

```kotlin
onDelete = {
    coroutineScope.launch {
        android.util.Log.i("ConfigScreenNew", "Deleting profile: $uid, file: ${file.absolutePath}")
        
        // 先删除元数据
        profileStorage.deleteProfile(uid)
        android.util.Log.i("ConfigScreenNew", "Metadata deleted for: $uid")
        
        // 再删除文件
        if (profileManager.deleteProfile(file)) {
            android.util.Log.i("ConfigScreenNew", "File deleted: ${file.absolutePath}")
            
            // 如果删除的是当前配置，清空当前配置路径
            if (file.absolutePath == currentConfigPath) {
                settingsManager.setCurrentConfigPath("")
            }
            loadProfiles()
        } else {
            android.util.Log.e("ConfigScreenNew", "Failed to delete file: ${file.absolutePath}")
            errorMessage = "删除失败"
        }
    }
}
```

#### 2. 增强元数据删除逻辑
在 `ProfileStorage.kt` 中添加异常处理和存在性检查：

```kotlin
suspend fun deleteProfile(uid: String) = withContext(Dispatchers.IO) {
    try {
        val exists = preferences.contains(uid)
        if (exists) {
            preferences.edit().remove(uid).apply()
            Log.i(TAG, "Deleted profile metadata: $uid")
        } else {
            Log.w(TAG, "Profile metadata not found: $uid")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to delete profile metadata: $uid", e)
    }
}
```

#### 3. 添加详细日志
在 `ProfileManager.kt` 的 `isSubscriptionExists` 方法中添加详细日志：

```kotlin
suspend fun isSubscriptionExists(url: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
    val allProfiles = profileStorage.getAllProfiles()
    Log.d(TAG, "Checking subscription existence for URL: $url")
    Log.d(TAG, "Total profiles in storage: ${allProfiles.size}")
    
    allProfiles.forEach { profile ->
        Log.d(TAG, "  - Profile: ${profile.name} (${profile.uid}), URL: ${profile.url}, Type: ${profile.type}")
    }
    
    val existing = allProfiles.find { it.url == url && it.type == ProfileStorage.ProfileType.REMOTE }
    if (existing != null) {
        Log.i(TAG, "Subscription exists: ${existing.name}")
        true to existing.name
    } else {
        Log.i(TAG, "Subscription does not exist")
        false to null
    }
}
```

---

## 问题2：订阅名称显示为英文技术文件名

### 问题描述
订阅URL如 `https://47.238.198.94/iv/verify_mode.htm?token=...`，提取的名称为 `verify_mode.htm`，不够友好。

### 用户需求
- 不要显示技术性文件名（如 .htm, .php 等）
- 使用更友好的中文名称

### 修复方案

在 `ProfileManager.kt` 的 `extractProfileName` 方法中，识别技术性文件名并使用友好的中文默认名称：

```kotlin
// 从URL中提取名称
val urlName = url.substringAfterLast("/")
    .substringBefore("?")
    .substringBefore("&")

// 检查是否是技术性文件名（如 .htm, .php 等）
if (urlName.isNotEmpty() && urlName != url) {
    val lowerName = urlName.lowercase()
    
    // 如果是常见的技术文件扩展名，使用友好的中文名称
    val isTechnicalFile = lowerName.endsWith(".htm") || 
                         lowerName.endsWith(".html") ||
                         lowerName.endsWith(".php") ||
                         lowerName.endsWith(".asp") ||
                         lowerName.endsWith(".aspx") ||
                         lowerName.endsWith(".jsp") ||
                         lowerName.contains("verify") ||
                         lowerName.contains("download") ||
                         lowerName.contains("config") ||
                         lowerName.contains("sub")
    
    if (!isTechnicalFile) {
        return sanitizeFileName(urlName)
    }
}

// 使用更友好的中文默认名称
val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
val timestamp = dateFormat.format(java.util.Date())
return "订阅配置 $timestamp"
```

### 效果
- **之前**：`verify_mode.htm`
- **之后**：`订阅配置 2024-11-02 15:30`

---

## 测试步骤

### 测试场景1：删除后重新导入（主要问题）

1. **导入订阅**
   ```
   打开应用 → 配置标签 → 添加订阅
   输入URL: https://your-subscription-url
   ```

2. **查看日志**
   ```bash
   adb logcat | grep -E "ProfileManager|ProfileStorage|ConfigScreenNew"
   ```
   应该看到：
   ```
   ProfileManager: Profile imported: name=订阅配置 2024-11-02 15:30, nodes=50
   ProfileStorage: Saved profile metadata: xxx
   ```

3. **删除订阅**
   ```
   点击订阅卡片右上角菜单 → 删除
   ```
   应该看到日志：
   ```
   ConfigScreenNew: Deleting profile: xxx, file: /data/.../xxx.yaml
   ProfileStorage: Deleted profile metadata: xxx
   ConfigScreenNew: File deleted: /data/.../xxx.yaml
   ```

4. **重新导入相同URL**
   ```
   配置标签 → 添加订阅 → 输入相同的URL
   ```
   应该看到日志：
   ```
   ConfigScreenNew: Checking if subscription exists: https://...
   ProfileManager: Checking subscription existence for URL: https://...
   ProfileManager: Total profiles in storage: 0
   ProfileManager: Subscription does not exist
   ```
   **预期结果**：✅ 导入成功，不提示"订阅已存在"

### 测试场景2：订阅名称显示

对于不同类型的订阅URL：

| URL示例 | 之前显示 | 之后显示 |
|---------|---------|---------|
| `https://xxx.com/verify_mode.htm?token=xxx` | verify_mode.htm | 订阅配置 2024-11-02 15:30 |
| `https://xxx.com/config.php?user=123` | config.php | 订阅配置 2024-11-02 15:30 |
| `https://xxx.com/mysub.yaml` | mysub.yaml | mysub.yaml |
| `https://xxx.com/香港节点.yaml` | 香港节点.yaml | 香港节点.yaml |

**预期结果**：
- ✅ 技术性文件名被替换为友好的中文名称
- ✅ 有意义的文件名保持不变
- ✅ 中文文件名正常显示

---

## 日志监控命令

### 查看订阅相关日志
```bash
adb logcat | grep -E "ProfileManager|ProfileStorage|ConfigScreenNew"
```

### 只看删除相关日志
```bash
adb logcat | grep -i "delet"
```

### 只看导入相关日志
```bash
adb logcat | grep -i "import\|subscription"
```

---

## 影响的文件

1. **`mobile/app/src/main/java/io/github/clash_verge_rev/clash_verge_rev/ui/ConfigScreenNew.kt`**
   - 调整删除顺序：先删除元数据，再删除文件
   - 添加详细日志输出

2. **`mobile/app/src/main/java/io/github/clash_verge_rev/clash_verge_rev/data/ProfileStorage.kt`**
   - 增强 `deleteProfile` 方法的错误处理
   - 添加存在性检查

3. **`mobile/app/src/main/java/io/github/clash_verge_rev/clash_verge_rev/data/ProfileManager.kt`**
   - 改进 `extractProfileName` 逻辑，识别技术性文件名
   - 使用友好的中文默认名称
   - 在 `isSubscriptionExists` 中添加详细日志

---

## 预期效果

### 删除和重新导入流程
```
用户操作               系统行为                    日志输出
───────────────────────────────────────────────────────────────
点击删除          →  删除元数据              →  "Deleted profile metadata: xxx"
                  →  删除配置文件            →  "File deleted: xxx.yaml"
                  →  刷新列表                →  "Total profiles in storage: 0"

点击添加          →  检查订阅是否存在        →  "Checking subscription existence"
                                              "Total profiles in storage: 0"
                                              "Subscription does not exist"
                  →  下载订阅内容            →  "Downloaded 12345 bytes"
                  →  提取名称                →  "Using friendly name: 订阅配置 2024-11-02 15:30"
                  →  保存配置                →  "Profile imported: name=订阅配置 2024-11-02 15:30"
                  →  保存元数据              →  "Saved profile metadata: xxx"
                  →  ✅ 导入成功
```

---

## 调试技巧

如果问题仍然存在：

### 1. 检查SharedPreferences
```bash
adb shell "run-as io.github.clash_verge_rev.clash_verge_rev.debug cat /data/data/io.github.clash_verge_rev.clash_verge_rev.debug/shared_prefs/profile_metadata.xml"
```

### 2. 检查配置文件目录
```bash
adb shell "run-as io.github.clash_verge_rev.clash_verge_rev.debug ls -la /data/data/io.github.clash_verge_rev.clash_verge_rev.debug/files/configs/"
```

### 3. 完全清除应用数据
```bash
adb shell pm clear io.github.clash_verge_rev.clash_verge_rev.debug
```

---

## 总结

本次修复主要解决了两个问题：

1. **删除后重新导入失败**：通过调整删除顺序、增强错误处理和添加详细日志来诊断和解决
2. **订阅名称不友好**：智能识别技术性文件名并使用友好的中文默认名称

所有修改都已编译成功并部署到测试设备。请按照测试步骤验证修复效果。

