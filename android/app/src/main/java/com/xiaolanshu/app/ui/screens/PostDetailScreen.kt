package com.xiaolanshu.app.ui.screens
import com.xiaolanshu.app.ui.theme.LocalAppColors

import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.xiaolanshu.app.BuildConfig
import com.xiaolanshu.app.data.AuthStore
import com.xiaolanshu.app.data.CommentItem
import com.xiaolanshu.app.ui.components.Avatar
import com.xiaolanshu.app.ui.components.FeedCard
import com.xiaolanshu.app.ui.components.FollowButton
import com.xiaolanshu.app.ui.components.LoadingView
import com.xiaolanshu.app.ui.viewmodel.PostDetailViewModel
import com.xiaolanshu.app.util.TimeUtil

@Composable
fun PostDetailScreen(
    postId: Long,
    onBack: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenPost: (Long) -> Unit,
    onOpenTopic: (Long, String) -> Unit,
    onRequireLogin: () -> Unit,
    onEdit: (Long) -> Unit = {},
) {
    val vm: PostDetailViewModel = viewModel { PostDetailViewModel(postId) }
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (ui.loading) {
        LoadingView()
        return
    }
    val d = ui.detail
    if (d == null) {
        Column(Modifier.fillMaxSize()) {
            Text(ui.error.ifBlank { "加载失败" }, color = LocalAppColors.current.textTertiary, modifier = Modifier.padding(16.dp))
        }
        return
    }

    // 点赞 / 收藏点击动效（状态切换时弹簧缩放）
    val likeScale by animateFloatAsState(
        targetValue = if (d.isLiked) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "likeScale",
    )
    val collectScale by animateFloatAsState(
        targetValue = if (d.isCollected) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "collectScale",
    )
    val me = AuthStore.get().currentUser

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(url = d.author.avatar, size = 40.dp, modifier = Modifier.clickable { onOpenUser(d.author.id) })
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.author.nickname, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onOpenUser(d.author.id) })
                            Text("${TimeUtil.ago(d.createdAt)} · ${d.viewCount} 浏览", style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.textTertiary)
                        }
                        if (me != null && d.author.id != me.id) {
                            FollowButton(
                                userId = d.author.id,
                                isFollowing = d.author.isFollowing,
                                onLoginRequired = onRequireLogin,
                                onChanged = { /* 已反馈，无需刷新详情 */ },
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        // 自己的笔记：编辑 / 删除（带确认）；所有人：分享
                        if (me != null && d.author.id == me.id) {
                            Text(
                                "编辑",
                                color = LocalAppColors.current.brandBlue,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onEdit(postId) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                            Text(
                                "删除",
                                color = LocalAppColors.current.dangerRed,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showDeleteDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        } else {
                            Text(
                                "分享",
                                color = LocalAppColors.current.brandBlue,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "【小蓝书】${d.title ?: "分享一条笔记"}\n" +
                                                    "App 打开：xls://post/$postId\n" +
                                                    "网页打开：${BuildConfig.API_BASE_URL.trimEnd('/')}/post/$postId",
                                            )
                                        }
                                        runCatching { context.startActivity(Intent.createChooser(send, "分享到")) }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (!d.title.isNullOrBlank()) {
                        Text(d.title!!, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (!d.content.isNullOrBlank()) {
                        Text(d.content!!, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(12.dp))
                    }
                    // 话题聚合入口
                    d.topic?.let { topic ->
                        Text(
                            "#${topic.name}",
                            color = LocalAppColors.current.brandBlue,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(LocalAppColors.current.brandBlueLight)
                                .clickable { onOpenTopic(topic.id, topic.name) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
            if (!d.videoUrl.isNullOrBlank()) {
                item {
                    VideoPlayerBlock(url = d.videoUrl!!)
                }
            }
            items(d.images) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { vm.toggleLike(onRequireLogin) }) {
                        Icon(
                            if (d.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "点赞",
                            tint = if (d.isLiked) LocalAppColors.current.dangerRed else LocalAppColors.current.textTertiary,
                            modifier = Modifier.scale(likeScale),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${d.likeCount}", color = LocalAppColors.current.textTertiary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { vm.toggleCollect(onRequireLogin) }) {
                        Icon(
                            if (d.isCollected) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "收藏",
                            tint = if (d.isCollected) LocalAppColors.current.brandBlue else LocalAppColors.current.textTertiary,
                            modifier = Modifier.scale(collectScale),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${d.collectCount}", color = LocalAppColors.current.textTertiary)
                    }
                    Text("评论 ${d.commentCount}", color = LocalAppColors.current.textTertiary)
                }
            }
            if (ui.related.isNotEmpty()) {
                item {
                    Text(
                        "相关推荐",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(ui.related) { r ->
                            Box(Modifier.width(150.dp)) {
                                FeedCard(post = r, onClick = { onOpenPost(r.id) })
                            }
                        }
                    }
                }
            }
            if (ui.comments.isEmpty()) {
                item { Text("还没有评论，来抢沙发～", color = LocalAppColors.current.textTertiary, modifier = Modifier.padding(16.dp)) }
            }
            items(ui.comments) { c ->
                CommentRow(c, onOpenUser)
            }
            item {
                Spacer(Modifier.height(70.dp))
            }
        }

        // 底部评论输入条
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(LocalAppColors.current.white)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = ui.commentText,
                onValueChange = { vm.setCommentText(it) },
                placeholder = { Text("说点什么…") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "发送",
                color = LocalAppColors.current.brandBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { vm.sendComment(onRequireLogin) },
            )
        }
    }

    // 删除确认对话框（不可恢复）
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除笔记") },
            text = { Text("删除后不可恢复，确定删除这条笔记吗？") },
            confirmButton = {
                Text(
                    "删除",
                    color = LocalAppColors.current.dangerRed,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable {
                            showDeleteDialog = false
                            vm.delete(
                                onDeleted = { onBack() },
                                onError = { /* 失败静默，可由下拉刷新恢复 */ },
                            )
                        },
                )
            },
            dismissButton = {
                Text(
                    "取消",
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { showDeleteDialog = false },
                )
            },
        )
    }
}

/** 视频笔记播放器（Media3 ExoPlayer）：相对地址拼接后播放，离开页面释放 */
@Composable
private fun VideoPlayerBlock(url: String) {
    val context = LocalContext.current
    val fullUrl = remember(url) { BuildConfig.API_BASE_URL.trimEnd('/') + url }
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(fullUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true
                player = exoPlayer
            }
        },
        modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun CommentRow(c: CommentItem, onOpenUser: (Long) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Avatar(c.user.avatar, size = 32.dp, modifier = Modifier.clickable { onOpenUser(c.user.id) })
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.user.nickname, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                    if (c.replyTo != null) {
                        Text(" 回复 ", color = LocalAppColors.current.textTertiary, style = MaterialTheme.typography.labelMedium)
                        Text(c.replyTo.nickname, color = LocalAppColors.current.brandBlue, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(TimeUtil.ago(c.createdAt), color = LocalAppColors.current.textTertiary, style = MaterialTheme.typography.labelSmall)
                }
                Text(c.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
            }
        }
        c.replies.forEach { reply ->
            Column(Modifier.padding(start = 40.dp, top = 6.dp)) {
                Text(
                    "${reply.user.nickname}${if (reply.replyTo != null) " 回复 ${reply.replyTo!!.nickname}" else ""}：${reply.content}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalAppColors.current.textTertiary,
                )
            }
        }
    }
}
