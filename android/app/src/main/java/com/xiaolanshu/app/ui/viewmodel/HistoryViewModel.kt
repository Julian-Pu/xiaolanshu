package com.xiaolanshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaolanshu.app.data.PostCard
import com.xiaolanshu.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val posts: List<PostCard> = emptyList(),
    val loading: Boolean = true,
)

/** 浏览记录页 ViewModel：最近浏览的笔记（推荐流排斥已读的数据来源） */
class HistoryViewModel : ViewModel() {

    private val _ui = MutableStateFlow(HistoryUiState())
    val ui: StateFlow<HistoryUiState> = _ui

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            runCatching { UserRepository.myHistory(1, 30) }
                .onSuccess {
                    if (it.code == 0) {
                        _ui.value = _ui.value.copy(posts = it.data?.list ?: emptyList(), loading = false)
                    }
                }
                .onFailure { _ui.value = _ui.value.copy(loading = false) }
        }
    }
}
