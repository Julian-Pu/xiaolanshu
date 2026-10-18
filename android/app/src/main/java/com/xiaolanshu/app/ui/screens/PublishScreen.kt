package com.xiaolanshu.app.ui.screens
import com.xiaolanshu.app.ui.theme.LocalAppColors

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaolanshu.app.ui.viewmodel.PublishViewModel

@Composable
fun PublishScreen(onPublished: (Long) -> Unit, onRequireLogin: () -> Unit) {
    val vm: PublishViewModel = viewModel()
    val ui by vm.ui.collectAsState()
    var imageUrlInput by remember { mutableStateOf("") }

    // 发布成功后跳转详情
    LaunchedEffect(ui.publishedId) {
        ui.publishedId?.let { onPublished(it) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        vm.uploadImages(uris)
    }

    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { vm.uploadVideo(it) }
    }

    val coverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { vm.uploadVideoCover(it) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("发布笔记", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (ui.hasDraft) {
                Text(
                    "● 草稿已保存",
                    color = LocalAppColors.current.textTertiary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clickable { vm.clearDraft() }.padding(4.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = ui.title,
            onValueChange = vm::setTitle,
            label = { Text("标题（选填）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = ui.content,
            onValueChange = vm::setContent,
            label = { Text("正文") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Text("图片（相册选择或粘贴图片链接）", style = MaterialTheme.typography.labelMedium, color = LocalAppColors.current.textTertiary)
        Spacer(Modifier.height(8.dp))
        if (ui.images.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.images) { url ->
                    Box {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).aspectRatio(1f),
                        )
                        Text(
                            "×",
                            color = LocalAppColors.current.white,
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                .clickable { vm.removeImage(url) }
                                .size(20.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                enabled = !ui.uploading,
                colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.brandBlue),
            ) {
                Text(if (ui.uploading) "上传中…" else "从相册选择")
            }
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = imageUrlInput,
                onValueChange = { imageUrlInput = it },
                placeholder = { Text("或粘贴图片链接") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "添加",
                color = LocalAppColors.current.brandBlue,
                modifier = Modifier.clickable {
                    if (imageUrlInput.isNotBlank()) {
                        vm.addImageUrl(imageUrlInput.trim())
                        imageUrlInput = ""
                    }
                },
            )
        }
        Spacer(Modifier.height(16.dp))

        // 视频笔记区（可选：添加视频 + 封面）
        Text("视频（可选，添加后仅保留视频笔记）", style = MaterialTheme.typography.labelMedium, color = LocalAppColors.current.textTertiary)
        Spacer(Modifier.height(8.dp))
        if (ui.videoUrl == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { videoLauncher.launch("video/*") },
                    enabled = !ui.uploadingVideo && ui.images.isEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.brandBlue),
                ) {
                    Text(if (ui.uploadingVideo) "上传中…" else "从相册选择视频")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "视频笔记不叠加图集",
                    color = LocalAppColors.current.textTertiary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    if (!ui.videoCover.isNullOrBlank()) {
                        AsyncImage(
                            model = ui.videoCover,
                            contentDescription = "视频封面",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).aspectRatio(3f / 4f),
                        )
                    } else {
                        Box(
                            Modifier.size(96.dp).aspectRatio(3f / 4f)
                                .background(LocalAppColors.current.brandBlueLight, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("▶", color = LocalAppColors.current.brandBlue, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("视频已添加", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { coverLauncher.launch("image/*") },
                            enabled = !ui.uploadingCover,
                            colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.brandBlue),
                        ) {
                            Text(if (ui.uploadingCover) "上传中…" else "更换封面")
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "移除",
                            color = LocalAppColors.current.dangerRed,
                            modifier = Modifier.clickable { vm.removeVideo() },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (ui.topics.isNotEmpty()) {
            Text("选择话题", style = MaterialTheme.typography.labelMedium, color = LocalAppColors.current.textTertiary)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.topics) { t ->
                    Card(
                        onClick = { vm.setTopicId(if (ui.topicId == t.id) null else t.id) },
                    ) {
                        Text(
                            "#${t.name}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = if (ui.topicId == t.id) LocalAppColors.current.brandBlue else LocalAppColors.current.textTertiary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (ui.error.isNotBlank()) {
            Text(ui.error, color = LocalAppColors.current.dangerRed, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = { vm.submit() },
            enabled = !ui.submitting,
            colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.brandBlue),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp),
        ) {
            Text(if (ui.submitting) "发布中…" else "发布")
        }
    }
}
