package com.xiaolanshu.app.ui.components
import com.xiaolanshu.app.ui.theme.LocalAppColors

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** 圆形头像（无图时显示占位色块） */
@Composable
fun Avatar(url: String?, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val placeholderColor = LocalAppColors.current.brandBlueLight
    val placeholder = remember { ColorPainter(placeholderColor) }
    if (url.isNullOrBlank()) {
        Box(modifier.size(size).clip(CircleShape).background(placeholderColor))
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            placeholder = placeholder,
            error = placeholder,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
    }
}
