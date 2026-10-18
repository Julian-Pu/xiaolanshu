package com.xiaolanshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaolanshu.app.data.AuthStore
import com.xiaolanshu.app.data.HotWord
import com.xiaolanshu.app.data.PostCard
import com.xiaolanshu.app.data.Topic
import com.xiaolanshu.app.data.User
import com.xiaolanshu.app.data.repository.SearchRepository
import com.xiaolanshu.app.data.repository.TopicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val keyword: String = "",
    val topics: List<Topic> = emptyList(),
    val hotWords: List<HotWord> = emptyList(),
    val history: List<String> = emptyList(),
    val posts: List<PostCard> = emptyList(),
    val snippets: List<String?> = emptyList(),
    val users: List<User> = emptyList(),
    val searched: Boolean = false,
    val loading: Boolean = true,
)

/** 发现页 ViewModel：热搜榜 / 搜索历史 / 搜索 */
class DiscoverViewModel : ViewModel() {

    private val _ui = MutableStateFlow(DiscoverUiState())
    val ui: StateFlow<DiscoverUiState> = _ui

    init {
        loadInit()
    }

    fun loadInit() {
        viewModelScope.launch {
            runCatching { TopicRepository.topics() }
                .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(topics = it.data ?: emptyList()) }
            runCatching { SearchRepository.hot() }
                .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(hotWords = it.data ?: emptyList()) }
            _ui.value = _ui.value.copy(history = AuthStore.get().searchHistory(), loading = false)
        }
    }

    fun setKeyword(k: String) {
        _ui.value = _ui.value.copy(keyword = k)
    }

    fun doSearch(q: String) {
        val kw = q.trim()
        if (kw.isEmpty()) return
        _ui.value = _ui.value.copy(keyword = kw)
        AuthStore.get().addSearchHistory(kw)
        _ui.value = _ui.value.copy(history = AuthStore.get().searchHistory())
        viewModelScope.launch {
            runCatching { SearchRepository.search(kw, "all") }
                .onSuccess { resp ->
                    if (resp.code == 0 && resp.data != null) {
                        _ui.value = _ui.value.copy(
                            posts = resp.data.posts,
                            snippets = resp.data.snippets ?: emptyList(),
                            users = resp.data.users,
                            searched = true,
                        )
                    }
                }
        }
    }

    fun clearHistory() {
        AuthStore.get().clearSearchHistory()
        _ui.value = _ui.value.copy(history = emptyList())
    }
}
