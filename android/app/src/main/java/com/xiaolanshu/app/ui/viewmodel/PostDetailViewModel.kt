package com.xiaolanshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaolanshu.app.data.AuthStore
import com.xiaolanshu.app.data.CommentItem
import com.xiaolanshu.app.data.PostCard
import com.xiaolanshu.app.data.PostDetail
import com.xiaolanshu.app.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PostDetailUiState(
    val detail: PostDetail? = null,
    val comments: List<CommentItem> = emptyList(),
    val related: List<PostCard> = emptyList(),
    val commentText: String = "",
    val loading: Boolean = true,
    val error: String = "",
)

/** 详情页 ViewModel：详情 / 点赞收藏 / 评论 / 相关推荐 */
class PostDetailViewModel(private val postId: Long) : ViewModel() {

    private val _ui = MutableStateFlow(PostDetailUiState())
    val ui: StateFlow<PostDetailUiState> = _ui

    init {
        reload()
        loadComments()
        loadRelated()
    }

    fun reload() {
        viewModelScope.launch {
            runCatching { PostRepository.detail(postId) }
                .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(detail = it.data, error = "") else _ui.value = _ui.value.copy(error = it.message) }
                .onFailure { _ui.value = _ui.value.copy(error = it.message ?: "网络错误") }
            _ui.value = _ui.value.copy(loading = false)
        }
    }

    fun loadComments() {
        viewModelScope.launch {
            runCatching { PostRepository.comments(postId, 1, 50) }
                .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(comments = it.data?.list ?: emptyList()) }
        }
    }

    fun loadRelated() {
        viewModelScope.launch {
            runCatching { PostRepository.related(postId) }
                .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(related = it.data ?: emptyList()) }
        }
    }

    fun toggleLike(onRequireLogin: () -> Unit) {
        val d = _ui.value.detail ?: return
        if (AuthStore.get().token == null) { onRequireLogin(); return }
        viewModelScope.launch {
            val r = if (d.isLiked) PostRepository.unlike(postId) else PostRepository.like(postId)
            if (r.code == 0 && r.data != null) {
                _ui.value = _ui.value.copy(detail = d.copy(isLiked = r.data.liked, likeCount = r.data.likeCount))
            }
        }
    }

    fun toggleCollect(onRequireLogin: () -> Unit) {
        val d = _ui.value.detail ?: return
        if (AuthStore.get().token == null) { onRequireLogin(); return }
        viewModelScope.launch {
            val r = if (d.isCollected) PostRepository.uncollect(postId) else PostRepository.collect(postId)
            if (r.code == 0 && r.data != null) {
                _ui.value = _ui.value.copy(detail = d.copy(isCollected = r.data.collected, collectCount = r.data.collectCount))
            }
        }
    }

    fun setCommentText(text: String) {
        _ui.value = _ui.value.copy(commentText = text)
    }

    fun sendComment(onRequireLogin: () -> Unit) {
        val text = _ui.value.commentText.trim()
        if (text.isEmpty()) return
        if (AuthStore.get().token == null) { onRequireLogin(); return }
        viewModelScope.launch {
            runCatching { PostRepository.addComment(postId, mapOf("content" to text)) }
                .onSuccess {
                    _ui.value = _ui.value.copy(commentText = "")
                    loadComments()
                    reload()
                }
        }
    }

    /** 删除笔记（仅作者）：成功回调后由页面返回上一页 */
    fun delete(onDeleted: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { PostRepository.delete(postId) }
                .onSuccess { if (it.code == 0) onDeleted() else onError(it.message) }
                .onFailure { onError(it.message ?: "网络错误") }
        }
    }
}
