package com.xiaolanshu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = White,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandBlueDark,
    secondary = AccentBlue,
    background = BgLight,
    onBackground = TextPrimary,
    surface = White,
    onSurface = TextPrimary,
    surfaceVariant = BgLight,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    error = DangerRed,
)

private val DarkColors = darkColorScheme(
    primary = DarkBrandBlue,
    onPrimary = Color.White,
    primaryContainer = DarkBrandBlueLight,
    onPrimaryContainer = Color(0xFF9CC1FF),
    secondary = AccentBlue,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkDivider,
    error = DarkDanger,
)

@Composable
fun XiaoLanShuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            content = content,
        )
    }
}
