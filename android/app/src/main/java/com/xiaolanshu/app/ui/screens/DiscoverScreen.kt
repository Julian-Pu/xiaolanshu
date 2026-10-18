package com.xiaolanshu.app.ui.screens
import com.xiaolanshu.app.ui.theme.LocalAppColors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaolanshu.app.ui.components.Avatar
import com.xiaolanshu.app.ui.components.EmptyView
import com.xiaolanshu.app.ui.components.FeedCard
import com.xiaolanshu.app.ui.components.LoadingView
import com.xiaolanshu.app.ui.viewmodel.DiscoverViewModel

@Composable
fun DiscoverScreen(onOpenPost: (Long) -> Unit, onOpenUser: (Long) -> Unit) {
    val vm: DiscoverViewModel = viewModel()
    val ui by vm.ui.collectAsState()

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ui.keyword,
                onValueChange = { vm.setKeyword(it) },
                placeholder = { Text("搜索笔记 / 用户") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "搜索",
                color = LocalAppColors.current.brandBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { vm.doSearch(ui.keyword) },
            )
        }
        Spacer(Modifier.height(12.dp))

        if (!ui.searched) {
            if (ui.loading) {
                LoadingView()
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    // ---- 热搜榜 ----
                    if (ui.hotWords.isNotEmpty()) {
                        Text("热搜榜", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(ui.hotWords.size) { i ->
                                val hw = ui.hotWords[i]
                                Column(
                                    Modifier
                                        .clickable { vm.doSearch(hw.word) }
                                        .padding(end = 6.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${i + 1}",
                                            color = if (i < 3) LocalAppColors.current.dangerRed else LocalAppColors.current.textTertiary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(hw.word, fontWeight = FontWeight.Medium)
                                    }
                                    Text("${hw.cnt} 次搜索", color = LocalAppColors.current.textTertiary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // ---- 搜索历史 ----
                    if (ui.history.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("搜索历史", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "清空",
                                color = LocalAppColors.current.textTertiary,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.clickable { vm.clearHistory() },
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ui.history.forEach { h ->
                                Text(
                                    h,
                                    color = LocalAppColors.current.textTertiary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable { vm.doSearch(h) },
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // ---- 话题广场 ----
                    Text("话题广场", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    ui.topics.chunked(2).forEach { rowTopics ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            rowTopics.forEach { topic ->
                                Card(
                                    onClick = { vm.doSearch(topic.name) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text("#${topic.name}", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${topic.postCount} 篇笔记",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LocalAppColors.current.brandBlue,
                                        )
                                    }
                                }
                            }
                            if (rowTopics.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (ui.users.isNotEmpty()) {
                    item {
                        Text("用户", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(ui.users.size) { i ->
                        val u = ui.users[i]
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenUser(u.id) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(u.avatar, size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(u.nickname, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${u.followerCount} 粉丝 · ${u.postCount} 笔记",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("笔记", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (ui.posts.isEmpty() && ui.users.isEmpty()) {
                    item { EmptyView("没有找到相关内容") }
                }
                items(ui.posts, key = { it.id }) { post ->
                    FeedCard(post = post, onClick = { onOpenPost(post.id) })
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}
