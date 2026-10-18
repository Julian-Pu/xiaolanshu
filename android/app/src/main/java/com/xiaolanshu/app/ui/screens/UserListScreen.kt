package com.xiaolanshu.app.ui.screens
import com.xiaolanshu.app.ui.theme.LocalAppColors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaolanshu.app.data.AuthStore
import com.xiaolanshu.app.ui.components.Avatar
import com.xiaolanshu.app.ui.components.EmptyView
import com.xiaolanshu.app.ui.components.FollowButton
import com.xiaolanshu.app.ui.components.LoadingView
import com.xiaolanshu.app.ui.viewmodel.UserListViewModel

/** 关注 / 粉丝列表页。mode: following | followers */
@Composable
fun UserListScreen(
    userId: Long,
    mode: String,
    onBack: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onRequireLogin: () -> Unit,
) {
    val vm: UserListViewModel = viewModel { UserListViewModel(userId, mode) }
    val ui by vm.ui.collectAsState()
    val title = if (mode == "following") "关注" else "粉丝"
    val me = AuthStore.get().currentUser

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
            Text("${title} ${ui.users.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (ui.loading) {
            LoadingView()
        } else if (ui.users.isEmpty()) {
            EmptyView("还没有${title}的人")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(ui.users, key = { it.id }) { u ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenUser(u.id) }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(u.avatar, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.nickname, fontWeight = FontWeight.SemiBold)
                            if (!u.bio.isNullOrBlank()) {
                                Text(u.bio!!, color = LocalAppColors.current.textTertiary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            }
                            Text(
                                "${u.followerCount} 粉丝 · ${u.postCount} 笔记",
                                color = LocalAppColors.current.textSecondary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        if (me != null && u.id != me.id) {
                            FollowButton(
                                userId = u.id,
                                isFollowing = u.isFollowing,
                                onLoginRequired = onRequireLogin,
                                onChanged = { vm.updateFollowState(u.id, it) },
                            )
                        }
                    }
                }
            }
        }
    }
}
