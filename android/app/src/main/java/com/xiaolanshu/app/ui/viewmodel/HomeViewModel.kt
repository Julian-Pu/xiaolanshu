package com.xiaolanshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaolanshu.app.data.PageResult
import com.xiaolanshu.app.data.PostCard
import com.xiaolanshu.app.data.repository.PostRepository
import com.xiaolanshu.app.data.repository.TopicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 首页频道：0=推荐 1=关注 其余=话题频道 */
data class Channel(val label: String, val topicId: Long?)

data class HomeUiState(
    val channels: List<Channel> = listOf(Channel("推荐", null), Channel("关注", null)),
    val selected: Int = 0,
    val posts: List<PostCard> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
)

/** 首页 ViewModel：频道切换 / 分页 / 下拉刷新 */
class HomeViewModel : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui

    fun currentTab(): String = when (_ui.value.selected) {
        0 -> "recommend"
        1 -> "following"
        else -> "topic"
    }

    fun loadTopics() {
        viewModelScope.launch {
            runCatching { TopicRepository.topics() }
                .onSuccess { resp ->
                    if (resp.code == 0 && resp.data != null) {
                        _ui.value = _ui.value.copy(
                            channels = listOf(Channel("推荐", null), Channel("关注", null)) +
                                    resp.data.map { Channel("#${it.name}", it.id) },
                        )
                    }
                }
        }
    }

    fun select(index: Int) {
        if (_ui.value.selected != index) _ui.value = _ui.value.copy(selected = index)
    }

    /** 加载指定页；append=true 追加 */
    fun load(page: Int, append: Boolean) {
        val s = _ui.value
        val topicId = s.channels.getOrNull(s.selected)?.topicId
        viewModelScope.launch {
            runCatching { PostRepository.feed(currentTab(), page, 10, topicId) }
                .onSuccess { resp ->
                    if (resp.code == 0 && resp.data != null) {
                        val r: PageResult<PostCard> = resp.data
                        _ui.value = _ui.value.copy(
                            posts = if (append) s.posts + r.list else r.list,
                            page = r.page,
                            hasMore = r.hasMore,
                        )
                    }
                }
            _ui.value = _ui.value.copy(loading = false, refreshing = false)
        }
    }

    /** 切换频道：重置列表并加载第一页 */
    fun resetAndLoad() {
        _ui.value = _ui.value.copy(posts = emptyList(), page = 1, hasMore = false, loading = true)
        load(1, false)
    }

    fun refresh() {
        _ui.value = _ui.value.copy(refreshing = true)
        load(1, false)
    }

    fun loadMore() {
        val s = _ui.value
        if (s.hasMore && !s.loading && s.posts.isNotEmpty()) {
            load(s.page + 1, true)
        }
    }
}
