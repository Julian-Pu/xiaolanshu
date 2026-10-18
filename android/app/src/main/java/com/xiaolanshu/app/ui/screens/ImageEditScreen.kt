package com.xiaolanshu.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.xiaolanshu.app.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/** 预设滤镜：ColorMatrix 4x5（原图/鲜明/暖阳/冷调/黑白/胶片） */
private val FILTERS: List<Pair<String, ColorMatrix?>> = listOf(
    "原图" to null,
    "鲜明" to ColorMatrix(floatArrayOf(
        1.3f, 0f, 0f, 0f, 0f,
        0f, 1.3f, 0f, 0f, 0f,
        0f, 0f, 1.3f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )),
    "暖阳" to ColorMatrix(floatArrayOf(
        1.1f, 0f, 0f, 0f, 20f,
        0f, 1.05f, 0f, 0f, 10f,
        0f, 0f, 0.9f, 0f, -15f,
        0f, 0f, 0f, 1f, 0f,
    )),
    "冷调" to ColorMatrix(floatArrayOf(
        0.95f, 0f, 0f, 0f, -10f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1.15f, 0f, 15f,
        0f, 0f, 0f, 1f, 0f,
    )),
    "黑白" to ColorMatrix(floatArrayOf(
        0.33f, 0.59f, 0.11f, 0f, 0f,
        0.33f, 0.59f, 0.11f, 0f, 0f,
        0.33f, 0.59f, 0.11f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )),
    "胶片" to ColorMatrix(floatArrayOf(
        0.9f, 0.1f, 0f, 0f, 5f,
        0f, 0.95f, 0.05f, 0f, 5f,
        0.05f, 0f, 0.9f, 0f, 5f,
        0f, 0f, 0f, 1f, 0f,
    )),
)

private val RATIOS = listOf(
    "自由" to 0f,     // 0 = 原图比例
    "1:1" to 1f,
    "3:4" to 3f / 4f,
)

/**
 * 图片编辑（裁剪 + 滤镜）：
 * 下载原图 → 双指缩放/平移（裁剪框居中固定，所见即所得）→ 应用滤镜 → 确认输出 JPEG 回调。
 */
@Composable
fun ImageEditScreen(
    imageUrl: String,
    onCancel: () -> Unit,
    onConfirm: (ByteArray) -> Unit,
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadError by remember { mutableStateOf("") }
    var ratioIdx by remember { mutableIntStateOf(1) } // 默认 1:1
    var filterIdx by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) } // 归一化偏移（相对画布宽，-1..1）
    var offsetY by remember { mutableFloatStateOf(0f) } // 归一化偏移（相对画布高）
    var saving by remember { mutableStateOf(false) }

    // 下载并解码原图
    LaunchedEffect(imageUrl) {
        withContext(Dispatchers.IO) {
            runCatching {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()
                val resp = client.newCall(Request.Builder().url(imageUrl).build()).execute()
                resp.body?.bytes()?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            }.onSuccess { bmp ->
                if (bmp != null) {
                    bitmap = bmp
                    // 初始 cover 铺满（fit 计算在绘制时基于画布尺寸，此处先给默认值）
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                } else {
                    loadError = "图片加载失败"
                }
            }.onFailure { loadError = it.message ?: "图片加载失败" }
        }
    }

    // 输出尺寸（由比例与画布比例决定，先按预览区比例近似，导出时取整）
    val outW = 1080
    val outH = when (RATIOS[ratioIdx].second) {
        0f -> 1080
        1f -> 1080
        else -> (1080 * 4f / 3f).toInt() // 3:4 → 810x1080（宽:高=3:4）
    }

    Column(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("‹ 返回", color = colors.brandBlue, modifier = Modifier.clickable { onCancel() }.padding(4.dp))
            Text("编辑图片", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val bmp = bitmap ?: return@Button
                    saving = true
                    scope.launch {
                        val jpeg = withContext(Dispatchers.IO) {
                            renderEdited(bmp, outW, outH, scale, offsetX, offsetY, FILTERS[filterIdx].second)
                        }
                        if (jpeg != null) onConfirm(jpeg)
                        else {
                            loadError = "处理失败"
                            saving = false
                        }
                    }
                },
                enabled = bitmap != null && !saving,
                colors = ButtonDefaults.buttonColors(containerColor = colors.brandBlue),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(if (saving) "处理中…" else "完成")
            }
        }

        if (bitmap == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (loadError.isNotEmpty()) loadError else "加载中…", color = colors.textSecondary)
            }
        } else {
            // 预览：画布比例 = 输出比例，图片在其上按 scale/offset 变换（与导出同一变换，所见即所得）
            val bw = bitmap!!.width.toFloat()
            val bh = bitmap!!.height.toFloat()
            val ratio = RATIOS[ratioIdx].second
            val previewRatio = if (ratio == 0f) bw / bh else ratio
            var previewW by remember { mutableFloatStateOf(1f) }
            var previewH by remember { mutableFloatStateOf(1f) }
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .aspectRatio(previewRatio)
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                    .onSizeChanged { previewW = it.width.toFloat(); previewH = it.height.toFloat() }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offsetX = (offsetX + pan.x / previewW).coerceIn(-1f, 1f)
                            offsetY = (offsetY + pan.y / previewH).coerceIn(-1f, 1f)
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val cw = size.width
                    val ch = size.height
                    // cover 初始铺满：图片 fit 到画布（min scale 保证铺满）
                    val fit = maxOf(cw / bw, ch / bh)
                    val s = fit * scale
                    val dx = (cw - bw * s) / 2f + offsetX * cw
                    val dy = (ch - bh * s) / 2f + offsetY * ch
                    val paint = Paint().apply {
                        FILTERS[filterIdx].second?.let { colorFilter = ColorMatrixColorFilter(it) }
                    }
                    drawIntoCanvas { canvas ->
                        val m = Matrix().apply {
                            setScale(s, s)
                            postTranslate(dx, dy)
                        }
                        canvas.nativeCanvas.drawBitmap(bitmap!!, m, paint)
                    }
                }
            }
        }

        // 比例与滤镜选择
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RATIOS.forEachIndexed { i, (label, _) ->
                Text(
                    label,
                    color = if (ratioIdx == i) colors.white else colors.textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (ratioIdx == i) colors.brandBlue else colors.brandBlueLight)
                        .clickable { ratioIdx = i }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FILTERS.forEachIndexed { i, (label, _) ->
                Text(
                    label,
                    color = if (filterIdx == i) colors.white else colors.textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (filterIdx == i) colors.brandBlue else colors.brandBlueLight)
                        .clickable { filterIdx = i }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** 按同一变换把图片绘制到输出画布（裁剪框 = 画布区域），应用滤镜，压缩 JPEG 返回 */
private fun renderEdited(
    src: Bitmap,
    outW: Int,
    outH: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    filter: ColorMatrix?,
): ByteArray? = runCatching {
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val bw = src.width.toFloat()
    val bh = src.height.toFloat()
    val fit = maxOf(outW / bw, outH / bh)
    val s = fit * scale
    val dx = (outW - bw * s) / 2f + offsetX * outW
    val dy = (outH - bh * s) / 2f + offsetY * outH
    val paint = Paint().apply {
        isFilterBitmap = true
        filter?.let { colorFilter = ColorMatrixColorFilter(it) }
    }
    val m = Matrix().apply {
        setScale(s, s)
        postTranslate(dx, dy)
    }
    canvas.drawBitmap(src, m, paint)
    val baos = ByteArrayOutputStream()
    out.compress(Bitmap.CompressFormat.JPEG, 90, baos)
    baos.toByteArray()
}.getOrNull()
