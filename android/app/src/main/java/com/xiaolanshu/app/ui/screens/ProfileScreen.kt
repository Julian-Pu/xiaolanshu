package com.xiaolanshu.app.ui.screens
import com.xiaolanshu.app.ui.theme.LocalAppColors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaolanshu.app.data.AuthStore
import com.xiaolanshu.app.ui.components.Avatar
import com.xiaolanshu.app.ui.components.EmptyView
import com.xiaolanshu.app.ui.components.FeedCard
import com.xiaolanshu.app.ui.components.FollowButton
import com.xiaolanshu.app.ui.components.LoadingView
import com.xiaolanshu.app.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    userId: Long?,
    onBack: () -> Unit,
    onOpenPost: (Long) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenFollowers: (Long) -> Unit,
    onOpenFollowing: (Long) -> Unit,
    onRequireLogin: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
) {
    val me = AuthStore.get().currentUser
    val targetId = userId ?: me?.id ?: 0L
    val isMe = userId == null || (me != null && userId == me.id)
    val vm: ProfileViewModel = viewModel { ProfileViewModel(targetId, isMe) }
    val ui by vm.ui.collectAsState()
    var showCreateAlbum by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    var showDeleteAlbumConfirm by remember { mutableStateOf(false) }

    if (ui.loading && ui.user == null) {
        LoadingView()
        return
    }
    val u = ui.user ?: run {
        Column(Modifier.fillMaxSize()) { Text("加载失败", modifier = Modifier.padding(16.dp)) }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (userId != null) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
            }
            Spacer(Modifier.weight(1f))
            if (isMe) {
                Text(
                    "设置",
                    color = LocalAppColors.current.textSecondary,
                    modifier = Modifier.clickable { onOpenSettings() }.padding(12.dp),
                )
                Text(
                    "退出登录",
                    color = LocalAppColors.current.textSecondary,
                    modifier = Modifier.clickable {
                        AuthStore.get().logout()
                        onRequireLogin()
                    }.padding(12.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(u.avatar, size = 72.dp)
            Spacer(Modifier.width(20.dp))
            Column {
                Text(u.nickname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (!u.bio.isNullOrBlank()) {
                    Text(u.bio!!, color = LocalAppColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "${u.followingCount} 关注",
                        color = LocalAppColors.current.textSecondary,
                        modifier = Modifier.clickable { onOpenFollowing(u.id) },
                    )
                    Text(
                        "${u.followerCount} 粉丝",
                        color = LocalAppColors.current.textSecondary,
                        modifier = Modifier.clickable { onOpenFollowers(u.id) },
                    )
                    Text("${u.postCount} 笔记", color = LocalAppColors.current.textSecondary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (!isMe) {
            Row(Modifier.padding(horizontal = 20.dp)) {
                FollowButton(
                    userId = u.id,
                    isFollowing = u.isFollowing,
                    onLoginRequired = onRequireLogin,
                    onChanged = { vm.updateFollowState(it) },
                )
            }
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(8.dp))
        }

        // 内容 tab 栏 + 浏览记录入口（仅自己）
        val tabs = if (isMe) listOf("笔记", "收藏", "赞过") else listOf("笔记")
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                tabs.forEachIndexed { i, label ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { vm.selectTab(i) }
                            .padding(vertical = 6.dp),
                    ) {
                        Text(
                            text = label,
                            color = if (ui.tab == i) LocalAppColors.current.brandBlue else LocalAppColors.current.textTertiary,
                            fontWeight = if (ui.tab == i) FontWeight.Bold else FontWeight.Normal,
                        )
                        if (ui.tab == i) {
                            Spacer(Modifier.height(2.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(LocalAppColors.current.brandBlue),
                            )
                        }
                    }
                }
            }
            if (isMe) {
                Text(
                    "浏览记录 ›",
                    color = LocalAppColors.current.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { onOpenHistory() }.padding(vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // 收藏 tab：专辑筛选胶囊（仅自己）
        if (isMe && ui.tab == 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    val selected = ui.selectedAlbumId == null
                    Text(
                        "全部",
                        color = if (selected) LocalAppColors.current.white else LocalAppColors.current.textSecondary,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) LocalAppColors.current.brandBlue else LocalAppColors.current.divider)
                            .clickable { vm.selectAlbum(null) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                items(ui.albums) { a ->
                    val selected = ui.selectedAlbumId == a.id
                    Text(
                        "${a.name}(${a.postCount})",
                        color = if (selected) LocalAppColors.current.white else LocalAppColors.current.textSecondary,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) LocalAppColors.current.brandBlue else LocalAppColors.current.divider)
                            .clickable { vm.selectAlbum(a.id) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                item {
                    Text(
                        "＋ 新建",
                        color = LocalAppColors.current.brandBlue,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                newAlbumName = ""
                                showCreateAlbum = true
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                item {
                    if (ui.selectedAlbumId != null) {
                        Text(
                            "删除此专辑",
                            color = LocalAppColors.current.dangerRed,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showDeleteAlbumConfirm = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (ui.posts.isEmpty()) {
            EmptyView(
                when {
                    ui.tab == 1 && ui.selectedAlbumId != null -> "这个专辑还没有笔记"
                    ui.tab == 1 -> "还没有收藏的笔记"
                    ui.tab == 2 -> "还没有赞过的笔记"
                    else -> "还没有发布笔记"
                },
                Modifier.padding(top = 48.dp),
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(ui.posts, key = { it.id }) { post ->
                    Box {
                        FeedCard(post = post, onClick = { onOpenPost(post.id) })
                        if (ui.tab == 1 && ui.selectedAlbumId != null) {
                            // 专辑视图：右上角移除出专辑
                            Text(
                                "✕",
                                color = LocalAppColors.current.white,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(androidx.compose.ui.graphics.Color(0x99000000))
                                    .clickable { vm.removeFromAlbum(post.id) }
                                    .padding(top = 3.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }

    // 新建专辑对话框
    if (showCreateAlbum) {
        AlertDialog(
            onDismissRequest = { showCreateAlbum = false },
            title = { Text("新建专辑") },
            text = {
                OutlinedTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    placeholder = { Text("专辑名称（最多 50 字）") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Text(
                    "创建",
                    color = LocalAppColors.current.brandBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable {
                            vm.createAlbum(newAlbumName) { err ->
                                if (err == null) showCreateAlbum = false
                            }
                        },
                )
            },
            dismissButton = {
                Text(
                    "取消",
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { showCreateAlbum = false },
                )
            },
        )
    }

    // 删除专辑确认
    if (showDeleteAlbumConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAlbumConfirm = false },
            title = { Text("删除专辑") },
            text = { Text("专辑内的收藏会被一并移除（收藏本身不受影响），确定删除？") },
            confirmButton = {
                Text(
                    "删除",
                    color = LocalAppColors.current.dangerRed,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable {
                            showDeleteAlbumConfirm = false
                            vm.deleteSelectedAlbum { }
                        },
                )
            },
            dismissButton = {
                Text(
                    "取消",
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { showDeleteAlbumConfirm = false },
                )
            },
        )
    }
}
