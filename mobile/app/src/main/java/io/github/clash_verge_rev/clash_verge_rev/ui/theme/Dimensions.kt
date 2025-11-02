package io.github.clash_verge_rev.clash_verge_rev.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设计系统 - 尺寸规范
 * 
 * 遵循Material Design 3标准
 * 使用dp（密度无关像素）和sp（可缩放像素）
 * 适配所有Android设备（5.8-8.3英寸屏幕）
 */
object AppDimensions {
    
    // ============ 字体大小 (sp) ============
    // 使用Material Design 3 Typography Scale
    object Typography {
        // 正文文本: 14-16sp (推荐手机阅读)
        val bodySmall = 12.sp
        val bodyMedium = 14.sp
        val bodyLarge = 16.sp
        
        // 标题文本: 18-24sp
        val titleSmall = 14.sp
        val titleMedium = 16.sp
        val titleLarge = 22.sp
        
        // 大标题
        val headlineSmall = 24.sp
        val headlineMedium = 28.sp
        val headlineLarge = 32.sp
        
        // 标签和辅助文本
        val labelSmall = 11.sp
        val labelMedium = 12.sp
        val labelLarge = 14.sp
        
        // 显示文本（超大）
        val displaySmall = 36.sp
        val displayMedium = 45.sp
        val displayLarge = 57.sp
    }
    
    // ============ 间距 (dp) ============
    object Spacing {
        // 微小间距
        val none = 0.dp
        val extraSmall = 4.dp
        val small = 8.dp
        
        // 标准间距
        val medium = 12.dp
        val large = 16.dp
        val extraLarge = 24.dp
        
        // 大间距
        val huge = 32.dp
        val massive = 48.dp
        val giant = 64.dp
    }
    
    // ============ 内边距 (dp) ============
    object Padding {
        // 容器内边距
        val containerSmall = 8.dp
        val containerMedium = 12.dp
        val containerLarge = 16.dp
        val containerExtraLarge = 24.dp
        
        // 卡片内边距
        val cardSmall = 12.dp
        val cardMedium = 16.dp
        val cardLarge = 20.dp
        
        // 列表项内边距
        val listItemVertical = 12.dp
        val listItemHorizontal = 16.dp
    }
    
    // ============ 图标大小 (dp) ============
    object IconSize {
        val tiny = 12.dp
        val small = 16.dp
        val medium = 20.dp
        val large = 24.dp
        val extraLarge = 32.dp
        val huge = 48.dp
        val massive = 64.dp
    }
    
    // ============ 圆角 (dp) ============
    object CornerRadius {
        val none = 0.dp
        val small = 4.dp
        val medium = 8.dp
        val large = 12.dp
        val extraLarge = 16.dp
        val full = 999.dp
    }
    
    // ============ 按钮尺寸 (dp) ============
    object Button {
        val heightSmall = 32.dp
        val heightMedium = 40.dp
        val heightLarge = 48.dp
        val heightExtraLarge = 56.dp
        
        val minWidth = 64.dp
        val paddingHorizontal = 16.dp
        val paddingVertical = 8.dp
    }
    
    // ============ 卡片尺寸 (dp) ============
    object Card {
        val elevation = 2.dp
        val minHeight = 80.dp
    }
    
    // ============ 分隔线 (dp) ============
    object Divider {
        val thickness = 1.dp
        val thickThickness = 2.dp
    }
    
    // ============ 进度条 (dp) ============
    object ProgressBar {
        val heightThin = 4.dp
        val heightMedium = 8.dp
        val heightThick = 12.dp
        val strokeWidth = 2.dp
    }
    
    // ============ 触摸目标最小尺寸 (dp) ============
    // Material Design建议最小触摸目标为48dp
    object TouchTarget {
        val minimum = 48.dp
        val recommended = 56.dp
    }
    
    // ============ 安全边距 (dp) ============
    // 避开刘海屏、挖孔屏、手势区域
    object SafeArea {
        val statusBar = 24.dp  // 状态栏预留
        val navigationBar = 16.dp  // 底部导航栏/手势区域
        val notch = 32.dp  // 刘海屏/挖孔屏预留
    }
}

