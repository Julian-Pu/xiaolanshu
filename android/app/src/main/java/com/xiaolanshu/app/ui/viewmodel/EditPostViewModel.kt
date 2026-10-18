package com.xiaolanshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaolanshu.app.data.Topic
import com.xiaolanshu.app.data.repository.PostRepository
import com.xiaolanshu.app.data.repository.TopicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditPostUiState(
    val title: String = "",
    val content: String = "",
    val topicId: Long? = null,
    val topics: List<Topic> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String = "",
)

/** 编辑笔记页 ViewModel：加载详情与话题列表，提交 PUT 更新 */
class EditPostViewModel(private val postId: Long) : ViewModel() {

    private val _ui = MutableStateFlow(EditPostUiState())
    val ui: StateFlow<EditPostUiState> = _ui

    init {
        viewModelScope.launch {
            val detail = runCatching { PostRepository.detail(postId) }.getOrNull()
            val topics = runCatching { TopicRepository.topics() }.getOrNull()
            val d = detail?.data
            _ui.value = EditPostUiState(
                title = d?.title.orEmpty(),
                content = d?.content.orEmpty(),
                topicId = d?.topic?.id,
                topics = topics?.data ?: emptyList(),
                loading = false,
                error = if (d == null) (detail?.message ?: "加载失败") else "",
            )
        }
    }

    fun setTitle(t: String) = _ui.update { it.copy(title = t) }

    fun setContent(c: String) = _ui.update { it.copy(content = c) }

    fun setTopic(id: Long?) = _ui.update { it.copy(topicId = id) }

    /** 保存：媒体（图片/视频）保持原样，只更新标题、内容、话题 */
    fun save(onSaved: (Long) -> Unit, onError: (String) -> Unit) {
        val s = _ui.value
        if (s.title.isBlank() && s.content.isBlank()) {
            onError("标题和内容至少填一项")
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(saving = true) }
            runCatching {
                val d = PostRepository.detail(postId).data
                    ?: throw IllegalStateException("笔记不存在")
                PostRepository.update(
                    postId,
                    mapOf(
                        "title" to s.title,
                        "content" to s.content,
                        "images" to d.images,
                        "videoUrl" to d.videoUrl,
                        "videoCover" to d.videoCover,
                        "topicId" to s.topicId,
                    ),
                )
            }
                .onSuccess { if (it.code == 0) onSaved(postId) else onError(it.message) }
                .onFailure { onError(it.message ?: "网络错误") }
            _ui.update { it.copy(saving = false) }
        }
    }
}
