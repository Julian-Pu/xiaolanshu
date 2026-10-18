package com.xiaolanshu.app.ui.components
import com.xiaolanshu.app.ui.theme.LocalAppColors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.xiaolanshu.app.data.PostCard
import com.xiaolanshu.app.util.TimeUtil

/** 双列信息流卡片（snippet 为搜索高亮片段，仅搜索结果显示） */
@Composable
fun FeedCard(post: PostCard, onClick: () -> Unit, snippet: String? = null) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
                val placeholderColor = LocalAppColors.current.brandBlueLight
                val placeholder = remember { ColorPainter(placeholderColor) }
                if (!post.cover.isNullOrBlank()) {
                    AsyncImage(
                        model = post.coverThumb ?: post.cover,
                        contentDescription = null,
                        placeholder = placeholder,
                        error = placeholder,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                    )
                }
                post.topic?.let { topic ->
                    Text(
                        text = "#${topic.name}",
                        color = LocalAppColors.current.brandBlue,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(LocalAppColors.current.brandBlueLight)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                if (!post.videoUrl.isNullOrBlank()) {
                    // 视频笔记：封面居中播放角标
                    Text(
                        text = "▶",
                        color = LocalAppColors.current.white,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = post.title ?: "分享生活点滴",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!snippet.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = snippet,
                        color = LocalAppColors.current.textTertiary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(url = post.author.avatar, size = 20.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = post.author.nickname,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "♥ ${post.likeCount}",
                        color = LocalAppColors.current.textTertiary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    text = TimeUtil.ago(post.createdAt),
                    color = LocalAppColors.current.textTertiary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
