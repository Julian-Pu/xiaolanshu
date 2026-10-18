package com.xiaolanshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaolanshu.app.data.PostCard
import com.xiaolanshu.app.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TopicPostsUiState(
    val posts: List<PostCard> = emptyList(),
    val loading: Boolean = true,
    val hasMore: Boolean = false,
    val page: Int = 1,
)

/** 话题笔记流 ViewModel：分页加载 */
class TopicPostsViewModel(private val topicId: Long) : ViewModel() {

    private val _ui = MutableStateFlow(TopicPostsUiState())
    val ui: StateFlow<TopicPostsUiState> = _ui

    init {
        load(1, false)
    }

    fun load(p: Int, append: Boolean) {
        viewModelScope.launch {
            runCatching { PostRepository.feed("topic", p, 10, topicId) }
                .onSuccess { r ->
                    if (r.code == 0 && r.data != null) {
                        _ui.value = _ui.value.copy(
                            posts = if (append) _ui.value.posts + r.data.list else r.data.list,
                            hasMore = r.data.hasMore,
                            page = p,
                        )
                    }
                }
            _ui.value = _ui.value.copy(loading = false)
        }
    }

    fun loadMore() {
        val s = _ui.value
        if (s.hasMore && !s.loading) load(s.page + 1, true)
    }
}
