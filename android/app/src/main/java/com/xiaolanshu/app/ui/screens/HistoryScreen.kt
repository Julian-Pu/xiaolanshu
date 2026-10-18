package com.xiaolanshu.app.ui.screens
import com.xiaolanshu.app.ui.theme.LocalAppColors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.xiaolanshu.app.ui.components.EmptyView
import com.xiaolanshu.app.ui.components.FeedCard
import com.xiaolanshu.app.ui.components.LoadingView
import com.xiaolanshu.app.ui.viewmodel.HistoryViewModel

/** 浏览记录页：最近看过的笔记（双列瀑布流） */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenPost: (Long) -> Unit,
) {
    val vm: HistoryViewModel = viewModel { HistoryViewModel() }
    val ui by vm.ui.collectAsState()

    if (ui.loading) {
        LoadingView()
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
            Text("浏览记录", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
        }
        if (ui.posts.isEmpty()) {
            EmptyView("还没有浏览记录", Modifier.padding(top = 48.dp))
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(ui.posts, key = { it.id }) { post ->
                    FeedCard(post = post, onClick = { onOpenPost(post.id) })
                }
            }
        }
    }
}
