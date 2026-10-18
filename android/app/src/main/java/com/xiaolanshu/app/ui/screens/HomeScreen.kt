package com.xiaolanshu.app.ui.screens
import com.xiaolanshu.app.ui.theme.LocalAppColors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaolanshu.app.data.PostCard
import com.xiaolanshu.app.ui.components.FeedCard
import com.xiaolanshu.app.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.sample

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenPost: (Long) -> Unit, onRequireLogin: () -> Unit) {
    val vm: HomeViewModel = viewModel()
    val ui by vm.ui.collectAsState()
    val gridState = rememberLazyStaggeredGridState()

    LaunchedEffect(Unit) { vm.loadTopics() }

    // 切换频道重置列表
    LaunchedEffect(ui.selected, ui.channels.size) {
        vm.resetAndLoad()
    }

    // 上拉自动加载：滑到接近末尾自动翻页
    LaunchedEffect(gridState, ui.posts.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collectLatest { last ->
                if (last != null && ui.hasMore && !ui.loading && ui.posts.isNotEmpty() &&
                    last >= gridState.layoutInfo.totalItemsCount - 3
                ) {
                    vm.loadMore()
                }
            }
    }

    Column(Modifier.fillMaxSize()) {
        // 顶部频道胶囊栏（横向可滑动）
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ui.channels.size) { i ->
                val ch = ui.channels[i]
                val active = ui.selected == i
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) LocalAppColors.current.brandBlue else LocalAppColors.current.brandBlueLight)
                        .clickable { vm.select(i) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = ch.label,
                        color = if (active) LocalAppColors.current.white else LocalAppColors.current.brandBlue,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = ui.refreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (ui.loading && ui.posts.isEmpty()) {
                SkeletonGrid()
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp,
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(ui.posts, key = { it.id }) { post ->
                        FeedCard(post = post, onClick = { onOpenPost(post.id) })
                    }
                    item {
                        if (ui.hasMore) {
                            Text(
                                "加载中…",
                                color = LocalAppColors.current.textTertiary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                            )
                        } else if (ui.posts.isNotEmpty()) {
                            Text(
                                "— 已经到底啦 —",
                                color = LocalAppColors.current.textTertiary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 加载中的骨架屏占位 */
@Composable
private fun SkeletonGrid() {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(6) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalAppColors.current.brandBlueLight),
            )
        }
    }
}
