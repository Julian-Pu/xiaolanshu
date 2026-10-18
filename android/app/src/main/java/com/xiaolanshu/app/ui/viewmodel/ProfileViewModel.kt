package com.xiaolanshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaolanshu.app.data.Album
import com.xiaolanshu.app.data.PostCard
import com.xiaolanshu.app.data.User
import com.xiaolanshu.app.data.repository.AlbumRepository
import com.xiaolanshu.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val posts: List<PostCard> = emptyList(),
    val tab: Int = 0, // 0 笔记 / 1 收藏 / 2 赞过
    val albums: List<Album> = emptyList(),
    val selectedAlbumId: Long? = null, // null = 全部收藏
    val loading: Boolean = true,
)

/** 个人主页 ViewModel：资料 + 三 tab 笔记流 + 收藏专辑筛选/管理 */
class ProfileViewModel(private val targetId: Long, private val isMe: Boolean) : ViewModel() {

    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            if (_ui.value.user == null) {
                runCatching { UserRepository.profile(targetId) }
                    .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(user = it.data) }
            }
            if (isMe) {
                runCatching { AlbumRepository.albums() }
                    .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(albums = it.data ?: emptyList()) }
            }
            loadPosts()
            _ui.value = _ui.value.copy(loading = false)
        }
    }

    private suspend fun loadPosts() {
        val tab = _ui.value.tab
        when (tab) {
            0 -> runCatching { UserRepository.userPosts(targetId, 1, 30) }
                .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(posts = it.data?.list ?: emptyList()) }
            1 -> {
                val albumId = _ui.value.selectedAlbumId
                if (albumId == null) {
                    runCatching { UserRepository.myCollects(1, 30) }
                        .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(posts = it.data?.list ?: emptyList()) }
                } else {
                    runCatching { AlbumRepository.posts(albumId, 1, 30) }
                        .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(posts = it.data?.list ?: emptyList()) }
                }
            }
            else -> runCatching { UserRepository.myLikes(1, 30) }
                .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(posts = it.data?.list ?: emptyList()) }
        }
    }

    fun selectTab(i: Int) {
        if (_ui.value.tab != i) {
            _ui.value = _ui.value.copy(tab = i, posts = emptyList(), loading = true)
            load()
        }
    }

    /** 切换收藏专辑筛选（null = 全部收藏） */
    fun selectAlbum(albumId: Long?) {
        _ui.value = _ui.value.copy(selectedAlbumId = albumId, posts = emptyList(), loading = true)
        load()
    }

    /** 新建专辑（收藏 tab） */
    fun createAlbum(name: String, onDone: (String?) -> Unit) {
        if (name.isBlank()) {
            onDone("专辑名称不能为空")
            return
        }
        viewModelScope.launch {
            runCatching { AlbumRepository.create(name.trim()) }
                .onSuccess {
                    if (it.code == 0) {
                        _ui.value = _ui.value.copy(albums = _ui.value.albums + listOfNotNull(it.data))
                        onDone(null)
                    } else onDone(it.message)
                }
                .onFailure { onDone(it.message ?: "网络错误") }
        }
    }

    /** 删除当前选中的专辑 */
    fun deleteSelectedAlbum(onDone: (String?) -> Unit) {
        val albumId = _ui.value.selectedAlbumId ?: return
        viewModelScope.launch {
            runCatching { AlbumRepository.delete(albumId) }
                .onSuccess {
                    if (it.code == 0) {
                        _ui.value = _ui.value.copy(
                            albums = _ui.value.albums.filterNot { a -> a.id == albumId },
                            selectedAlbumId = null,
                        )
                        loadPosts()
                        onDone(null)
                    } else onDone(it.message)
                }
                .onFailure { onDone(it.message ?: "网络错误") }
        }
    }

    /** 从当前专辑移除一篇笔记 */
    fun removeFromAlbum(postId: Long) {
        val albumId = _ui.value.selectedAlbumId ?: return
        viewModelScope.launch {
            runCatching { AlbumRepository.removePost(albumId, postId) }
                .onSuccess {
                    if (it.code == 0) {
                        _ui.value = _ui.value.copy(
                            posts = _ui.value.posts.filterNot { p -> p.id == postId },
                        )
                    }
                }
        }
    }

    /** 关注/取关后由 FollowButton 回调更新本地状态 */
    fun updateFollowState(isFollowing: Boolean) {
        _ui.value.user?.let { u ->
            _ui.value = _ui.value.copy(user = u.copy(isFollowing = isFollowing))
        }
    }
}
