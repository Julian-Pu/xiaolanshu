package com.xiaolanshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaolanshu.app.data.User
import com.xiaolanshu.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UserListUiState(
    val users: List<User> = emptyList(),
    val loading: Boolean = true,
)

/** 关注/粉丝列表 ViewModel。mode: following | followers */
class UserListViewModel(private val userId: Long, private val mode: String) : ViewModel() {

    private val _ui = MutableStateFlow(UserListUiState())
    val ui: StateFlow<UserListUiState> = _ui

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            runCatching {
                if (mode == "following") UserRepository.following(userId, 1, 50)
                else UserRepository.followers(userId, 1, 50)
            }.onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(users = it.data?.list ?: emptyList()) }
            _ui.value = _ui.value.copy(loading = false)
        }
    }

    /** 关注/取关后由 FollowButton 回调更新列表项 */
    fun updateFollowState(id: Long, isFollowing: Boolean) {
        _ui.value = _ui.value.copy(
            users = _ui.value.users.map { if (it.id == id) it.copy(isFollowing = isFollowing) else it },
        )
    }
}
