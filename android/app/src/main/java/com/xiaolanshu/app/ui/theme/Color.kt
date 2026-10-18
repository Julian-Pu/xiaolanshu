package com.xiaolanshu.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 小蓝书品牌色：蓝
val BrandBlue = Color(0xFF1F6FFF)
val BrandBlueDark = Color(0xFF145BD6)
val BrandBlueLight = Color(0xFFEAF1FF)
val AccentBlue = Color(0xFF61A5FF)

val BgLight = Color(0xFFF6F7F9)
val TextPrimary = Color(0xFF1F2329)
val TextSecondary = Color(0xFF6B7280)
val TextTertiary = Color(0xFF9CA3AF)
val DividerColor = Color(0xFFECEFF3)
val DangerRed = Color(0xFFE5484D)
val White = Color(0xFFFFFFFF)

// ---- 深色语义色 ----
val DarkBrandBlue = Color(0xFF4D8DFF)
val DarkBrandBlueLight = Color(0xFF1E3352)
val DarkBg = Color(0xFF14161A)
val DarkSurface = Color(0xFF1E2126)
val DarkSurfaceVariant = Color(0xFF23272E)
val DarkTextPrimary = Color(0xFFE6E8EB)
val DarkTextSecondary = Color(0xFFA6ADBB)
val DarkTextTertiary = Color(0xFF767D8A)
val DarkDivider = Color(0xFF2A2F38)
val DarkDanger = Color(0xFFF2555A)
val DarkChatBubbleOther = Color(0xFF2A2F38)
val DarkSkeleton = Color(0xFF2A2F38)

/** 应用语义色（跟随主题），供页面统一取色 */
data class AppColors(
    val brandBlue: Color,
    val brandBlueLight: Color,
    val dangerRed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val white: Color,
    val chatBubbleOther: Color,
    val skeleton: Color,
)

val LightAppColors = AppColors(
    brandBlue = BrandBlue,
    brandBlueLight = BrandBlueLight,
    dangerRed = DangerRed,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textTertiary = TextTertiary,
    divider = DividerColor,
    white = Color.White,
    chatBubbleOther = Color(0xFFECEFF3),
    skeleton = Color(0xFFE6EAF0),
)

val DarkAppColors = AppColors(
    brandBlue = DarkBrandBlue,
    brandBlueLight = DarkBrandBlueLight,
    dangerRed = DarkDanger,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    divider = DarkDivider,
    white = DarkSurface,
    chatBubbleOther = DarkChatBubbleOther,
    skeleton = DarkSkeleton,
)

/** 当前主题语义色入口：`LocalAppColors.current.textSecondary` */
val LocalAppColors = staticCompositionLocalOf { LightAppColors }
