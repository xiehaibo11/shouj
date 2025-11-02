# Clash Verge Rev Mobile - 设计系统

## 概述

本设计系统基于 **Material Design 3** 标准，确保应用在所有Android设备（5.8-8.3英寸屏幕）上都有一致且良好的用户体验。

## 适配策略

### 屏幕尺寸支持

- **小屏手机**: 5.8-6.1英寸
- **主流手机**: 6.1-6.7英寸  
- **大屏手机**: 6.7-7.0英寸
- **折叠屏**: 7.6-8.3英寸（展开）

### 分辨率支持

- **全面屏标准**: 2340×1080, 2400×1080, 2436×1125
- **2K+高分辨率**: 3200×1440, 3168×1440, 3040×1440
- **特殊比例**: iPhone系列各种分辨率

## 设计系统组件

所有尺寸定义在 `AppDimensions` 对象中（位于 `ui/theme/Dimensions.kt`）

### 字体大小 (sp)

使用可缩放像素（sp），支持用户无障碍设置：

```kotlin
// 正文文本 (14-16sp推荐手机阅读)
AppDimensions.Typography.bodySmall     // 12sp
AppDimensions.Typography.bodyMedium    // 14sp
AppDimensions.Typography.bodyLarge     // 16sp

// 标题文本 (18-24sp)
AppDimensions.Typography.titleSmall    // 14sp
AppDimensions.Typography.titleMedium   // 16sp
AppDimensions.Typography.titleLarge    // 22sp

// 大标题
AppDimensions.Typography.headlineSmall  // 24sp
AppDimensions.Typography.headlineMedium // 28sp
AppDimensions.Typography.headlineLarge  // 32sp

// 标签和辅助文本
AppDimensions.Typography.labelSmall    // 11sp
AppDimensions.Typography.labelMedium   // 12sp
AppDimensions.Typography.labelLarge    // 14sp
```

### 间距 (dp)

使用密度无关像素（dp），自动适配不同屏幕密度：

```kotlin
// 微小间距
AppDimensions.Spacing.none          // 0dp
AppDimensions.Spacing.extraSmall    // 4dp
AppDimensions.Spacing.small         // 8dp

// 标准间距
AppDimensions.Spacing.medium        // 12dp
AppDimensions.Spacing.large         // 16dp
AppDimensions.Spacing.extraLarge    // 24dp

// 大间距
AppDimensions.Spacing.huge          // 32dp
AppDimensions.Spacing.massive       // 48dp
AppDimensions.Spacing.giant         // 64dp
```

### 内边距 (dp)

```kotlin
// 容器内边距
AppDimensions.Padding.containerSmall      // 8dp
AppDimensions.Padding.containerMedium     // 12dp
AppDimensions.Padding.containerLarge      // 16dp
AppDimensions.Padding.containerExtraLarge // 24dp

// 卡片内边距
AppDimensions.Padding.cardSmall    // 12dp
AppDimensions.Padding.cardMedium   // 16dp
AppDimensions.Padding.cardLarge    // 20dp

// 列表项内边距
AppDimensions.Padding.listItemVertical   // 12dp
AppDimensions.Padding.listItemHorizontal // 16dp
```

### 图标尺寸 (dp)

```kotlin
AppDimensions.IconSize.tiny        // 12dp
AppDimensions.IconSize.small       // 16dp
AppDimensions.IconSize.medium      // 20dp
AppDimensions.IconSize.large       // 24dp
AppDimensions.IconSize.extraLarge  // 32dp
AppDimensions.IconSize.huge        // 48dp
AppDimensions.IconSize.massive     // 64dp
```

### 按钮尺寸 (dp)

```kotlin
// 按钮高度
AppDimensions.Button.heightSmall      // 32dp
AppDimensions.Button.heightMedium     // 40dp
AppDimensions.Button.heightLarge      // 48dp
AppDimensions.Button.heightExtraLarge // 56dp

// 按钮内边距
AppDimensions.Button.minWidth           // 64dp
AppDimensions.Button.paddingHorizontal  // 16dp
AppDimensions.Button.paddingVertical    // 8dp
```

### 圆角 (dp)

```kotlin
AppDimensions.CornerRadius.none       // 0dp
AppDimensions.CornerRadius.small      // 4dp
AppDimensions.CornerRadius.medium     // 8dp
AppDimensions.CornerRadius.large      // 12dp
AppDimensions.CornerRadius.extraLarge // 16dp
AppDimensions.CornerRadius.full       // 999dp (完全圆形)
```

### 进度条 (dp)

```kotlin
AppDimensions.ProgressBar.heightThin   // 4dp
AppDimensions.ProgressBar.heightMedium // 8dp
AppDimensions.ProgressBar.heightThick  // 12dp
AppDimensions.ProgressBar.strokeWidth  // 2dp
```

### 触摸目标 (dp)

Material Design建议最小触摸目标为48dp：

```kotlin
AppDimensions.TouchTarget.minimum      // 48dp
AppDimensions.TouchTarget.recommended  // 56dp
```

### 安全边距 (dp)

处理刘海屏、挖孔屏、手势区域：

```kotlin
AppDimensions.SafeArea.statusBar      // 24dp
AppDimensions.SafeArea.navigationBar  // 16dp
AppDimensions.SafeArea.notch          // 32dp
```

## 使用示例

### 基础示例

```kotlin
import io.github.clash_verge_rev.clash_verge_rev.ui.theme.AppDimensions

@Composable
fun MyCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppDimensions.Spacing.large),
        elevation = CardDefaults.cardElevation(
            defaultElevation = AppDimensions.Card.elevation
        )
    ) {
        Column(modifier = Modifier.padding(AppDimensions.Padding.cardMedium)) {
            Text(
                text = "标题",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(AppDimensions.Spacing.small))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimensions.IconSize.small)
                )
                Spacer(modifier = Modifier.width(AppDimensions.Spacing.extraSmall))
                Text(
                    text = "详细信息",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
```

### 响应式布局示例

```kotlin
@Composable
fun ResponsiveLayout() {
    val configuration = LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp > 600
    
    if (isLargeScreen) {
        // 平板或大屏布局
        Row(modifier = Modifier.fillMaxSize()) {
            SidePanel(modifier = Modifier.weight(0.3f))
            MainContent(modifier = Modifier.weight(0.7f))
        }
    } else {
        // 手机布局
        Column(modifier = Modifier.fillMaxSize()) {
            MainContent()
        }
    }
}
```

### 安全区域处理

```kotlin
@Composable
fun SafeContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = AppDimensions.SafeArea.statusBar,
                bottom = AppDimensions.SafeArea.navigationBar
            )
    ) {
        // 内容
    }
}
```

## 最佳实践

### 1. 永远使用相对单位

❌ **错误**：
```kotlin
Text("标题", fontSize = 20.sp)
Spacer(modifier = Modifier.height(16.dp))
Icon(Icons.Default.Star, modifier = Modifier.size(24.dp))
```

✅ **正确**：
```kotlin
Text("标题", style = MaterialTheme.typography.titleLarge)
Spacer(modifier = Modifier.height(AppDimensions.Spacing.large))
Icon(Icons.Default.Star, modifier = Modifier.size(AppDimensions.IconSize.large))
```

### 2. 使用Material Theme Typography

优先使用 `MaterialTheme.typography` 而不是自定义字体大小：

```kotlin
Text("正文", style = MaterialTheme.typography.bodyMedium)
Text("标题", style = MaterialTheme.typography.titleMedium)
Text("标签", style = MaterialTheme.typography.labelSmall)
```

### 3. 使用fillMaxWidth/fillMaxHeight

避免硬编码宽度和高度：

```kotlin
// ✅ 响应式
Column(modifier = Modifier.fillMaxWidth())

// ❌ 固定尺寸
Column(modifier = Modifier.width(360.dp))
```

### 4. 使用weight进行比例布局

```kotlin
Row(modifier = Modifier.fillMaxWidth()) {
    Box(modifier = Modifier.weight(1f)) { /* 左侧 */ }
    Box(modifier = Modifier.weight(2f)) { /* 右侧占2/3 */ }
}
```

### 5. 使用Spacer而不是padding

在需要灵活间距时使用Spacer：

```kotlin
Column {
    Text("第一行")
    Spacer(modifier = Modifier.height(AppDimensions.Spacing.medium))
    Text("第二行")
}
```

### 6. 确保触摸目标足够大

Material Design建议最小48dp：

```kotlin
IconButton(
    onClick = { },
    modifier = Modifier.size(AppDimensions.TouchTarget.minimum)
) {
    Icon(Icons.Default.Close, null)
}
```

## 不同屏幕尺寸的测试

### 小屏设备 (5.8-6.1英寸)

- 确保所有文本可读（不小于12sp）
- 触摸目标不小于48dp
- 内容不被截断

### 主流设备 (6.1-6.7英寸)

- 最佳用户体验的目标尺寸
- 标准间距和字体大小

### 大屏设备 (6.7+英寸)

- 考虑使用更大的间距
- 可以增大卡片尺寸
- 考虑多列布局

### 折叠屏

- 支持横竖屏切换
- 提供大屏优化布局
- 适配不同展开状态

## 颜色系统

使用Material Theme颜色而不是硬编码颜色：

```kotlin
// ✅ 正确 - 自动适配深色/浅色主题
Text(
    "内容",
    color = MaterialTheme.colorScheme.onSurface
)

Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
) { }

// ❌ 错误 - 不适配主题
Text("内容", color = Color.Black)
```

## 参考资源

- [Material Design 3](https://m3.material.io/)
- [Android Compose Guidelines](https://developer.android.com/jetpack/compose/designsystems)
- [Material Design Typography](https://m3.material.io/styles/typography/overview)
- [Material Design Layout](https://m3.material.io/foundations/layout/understanding-layout/overview)

## 已优化的组件

以下组件已经完全采用新设计系统：

- ✅ `ProfileCard.kt` - 订阅配置卡片
- ✅ `ConfigScreenNew.kt` - 配置管理页面  
- ✅ `ProxyScreen.kt` - 代理节点页面
- ✅ `HomeScreen.kt` - 主页
- ✅ `SettingsScreen.kt` - 设置页面
- ✅ `TunSettingsDialog.kt` - TUN设置对话框
- ✅ `LogScreen.kt` - 日志页面

## 总结

遵循这个设计系统可以确保：

1. **一致性** - 所有界面使用相同的间距、字体和组件
2. **可维护性** - 统一修改设计系统即可更新所有界面
3. **响应式** - 自动适配不同屏幕尺寸和密度
4. **无障碍** - 支持系统字体缩放和触摸目标大小
5. **主题支持** - 完美适配深色/浅色模式

请在开发新功能时严格遵循此设计系统。

