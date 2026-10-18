package com.xiaolanshu.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.xiaolanshu.app.data.Topic
import com.xiaolanshu.app.data.repository.PostRepository
import com.xiaolanshu.app.data.repository.TopicRepository
import com.xiaolanshu.app.data.repository.UploadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** 发布草稿数据类（Gson 序列化到 xls_draft SharedPreferences） */
data class PublishDraft(
    val title: String = "",
    val content: String = "",
    val images: List<String> = emptyList(),
    val videoUrl: String? = null,
    val videoCover: String? = null,
    val topicId: Long? = null,
)

data class PublishUiState(
    val title: String = "",
    val content: String = "",
    val topicId: Long? = null,
    val topics: List<Topic> = emptyList(),
    val images: List<String> = emptyList(),
    val videoUrl: String? = null,
    val videoCover: String? = null,
    val uploading: Boolean = false,
    val uploadingVideo: Boolean = false,
    val uploadingCover: Boolean = false,
    val submitting: Boolean = false,
    val error: String = "",
    val hasDraft: Boolean = false,
    val publishedId: Long? = null,
)

/** 发布页 ViewModel：草稿持久化 + 话题 + 图片上传 + 发布 */
class PublishViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("xls_draft", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _ui = MutableStateFlow(PublishUiState())
    val ui: StateFlow<PublishUiState> = _ui

    init {
        loadDraft()
        loadTopics()
    }

    fun loadTopics() {
        viewModelScope.launch {
            runCatching { TopicRepository.topics() }
                .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(topics = it.data ?: emptyList()) }
        }
    }

    fun setTitle(v: String) {
        _ui.value = _ui.value.copy(title = v)
        saveDraft()
    }

    fun setContent(v: String) {
        _ui.value = _ui.value.copy(content = v)
        saveDraft()
    }

    fun setTopicId(v: Long?) {
        _ui.value = _ui.value.copy(topicId = v)
        saveDraft()
    }

    /** 展示错误提示（如图片数量超限） */
    fun showError(msg: String) {
        _ui.value = _ui.value.copy(error = msg)
    }

    /** 粘贴图片链接加入列表 */
    fun addImageUrl(url: String) {
        if (url.isBlank()) return
        addImageUrlInternal(url)
    }

    /** 移除某张图片 */
    fun removeImage(url: String) {
        _ui.value = _ui.value.copy(images = _ui.value.images - url)
        saveDraft()
    }

    /** 设为封面：把该图移到列表首位（后端 images[0] 即封面） */
    fun setCover(url: String) {
        val imgs = _ui.value.images
        if (imgs.size <= 1 || imgs.firstOrNull() == url) return
        _ui.value = _ui.value.copy(images = listOf(url) + imgs.filter { it != url })
        saveDraft()
    }

    /** 图片编辑（裁剪/滤镜）完成后替换：上传编辑结果 → 替换原 URL */
    fun replaceImage(url: String, jpegBytes: ByteArray, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                val part = MultipartBody.Part.createFormData(
                    "file",
                    "edited_${System.currentTimeMillis()}.jpg",
                    jpegBytes.toRequestBody("image/jpeg".toMediaType()),
                )
                UploadRepository.image(part).data?.url
            }.onSuccess { newUrl ->
                if (newUrl != null) {
                    _ui.value = _ui.value.copy(images = _ui.value.images.map { if (it == url) newUrl else it })
                    saveDraft()
                }
            }.onFailure { _ui.value = _ui.value.copy(error = it.message ?: "图片编辑上传失败") }
            onDone()
        }
    }

    fun clearDraft() {
        prefs.edit().remove("draft").apply()
        _ui.value = _ui.value.copy(
            title = "", content = "", images = emptyList(), topicId = null,
            videoUrl = null, videoCover = null, hasDraft = false,
        )
    }

    /** 上传所选视频（Android 视频选择器），成功后写入 videoUrl */
    fun uploadVideo(uri: Uri) {
        _ui.value = _ui.value.copy(uploadingVideo = true)
        viewModelScope.launch {
            runCatching {
                val bytes = getApplication<Application>()
                    .contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("读取视频失败")
                val part = MultipartBody.Part.createFormData(
                    "file",
                    "video_${System.currentTimeMillis()}.mp4",
                    bytes.toRequestBody("video/mp4".toMediaType()),
                )
                UploadRepository.video(part).data?.url
            }.onSuccess { url ->
                if (url != null) {
                    _ui.value = _ui.value.copy(videoUrl = url)
                    saveDraft()
                }
            }.onFailure { _ui.value = _ui.value.copy(error = it.message ?: "视频上传失败") }
            _ui.value = _ui.value.copy(uploadingVideo = false)
        }
    }

    /** 上传视频封面（图片），成功后写入 videoCover */
    fun uploadVideoCover(uri: Uri) {
        _ui.value = _ui.value.copy(uploadingCover = true)
        viewModelScope.launch {
            runCatching {
                val bytes = getApplication<Application>()
                    .contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("读取封面失败")
                val part = MultipartBody.Part.createFormData(
                    "file",
                    "cover_${System.currentTimeMillis()}.jpg",
                    bytes.toRequestBody("image/jpeg".toMediaType()),
                )
                UploadRepository.image(part).data?.url
            }.onSuccess { url ->
                if (url != null) {
                    _ui.value = _ui.value.copy(videoCover = url)
                    saveDraft()
                }
            }.onFailure { _ui.value = _ui.value.copy(error = it.message ?: "封面上传失败") }
            _ui.value = _ui.value.copy(uploadingCover = false)
        }
    }

    /** 移除已选视频 */
    fun removeVideo() {
        _ui.value = _ui.value.copy(videoUrl = null, videoCover = null)
        saveDraft()
    }

    /** 上传所选图片（GetMultipleContents 结果），成功后追加到 images 并存草稿 */
    fun uploadImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _ui.value = _ui.value.copy(uploading = true)
        viewModelScope.launch {
            for (uri in uris) {
                runCatching {
                    val bytes = getApplication<Application>()
                        .contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("读取图片失败")
                    val part = MultipartBody.Part.createFormData(
                        "file",
                        "image_${System.currentTimeMillis()}.jpg",
                        bytes.toRequestBody("image/jpeg".toMediaType()),
                    )
                    UploadRepository.image(part).data?.url
                }.onSuccess { url -> if (url != null) addImageUrlInternal(url) }
            }
            _ui.value = _ui.value.copy(uploading = false)
        }
    }

    private fun addImageUrlInternal(url: String) {
        _ui.value = _ui.value.copy(images = _ui.value.images + url)
        saveDraft()
    }

    fun submit() {
        val s = _ui.value
        if (s.title.isBlank() && s.content.isBlank()) {
            _ui.value = s.copy(error = "请填写标题或正文")
            return
        }
        if (s.images.isEmpty() && s.videoUrl == null) {
            _ui.value = s.copy(error = "请至少添加一张图片或一个视频")
            return
        }
        _ui.value = s.copy(submitting = true, error = "")
        viewModelScope.launch {
            val body = mutableMapOf<String, Any?>(
                "title" to s.title,
                "content" to s.content,
                "images" to s.images,
                "topicId" to s.topicId,
            )
            s.videoUrl?.let { body["videoUrl"] = it }
            s.videoCover?.let { body["videoCover"] = it }
            runCatching { PostRepository.create(body) }
                .onSuccess { resp ->
                    if (resp.code == 0 && resp.data != null) {
                        prefs.edit().remove("draft").apply()
                        _ui.value = _ui.value.copy(hasDraft = false, publishedId = resp.data.id)
                    } else {
                        _ui.value = _ui.value.copy(error = resp.message)
                    }
                }
                .onFailure { _ui.value = _ui.value.copy(error = it.message ?: "发布失败") }
            _ui.value = _ui.value.copy(submitting = false)
        }
    }

    private fun loadDraft() {
        val json = prefs.getString("draft", null) ?: return
        runCatching { gson.fromJson(json, PublishDraft::class.java) }
            .onSuccess { d ->
                if (d.title.isNotBlank() || d.content.isNotBlank() || d.images.isNotEmpty()
                    || d.videoUrl != null || d.topicId != null
                ) {
                    _ui.value = _ui.value.copy(
                        title = d.title, content = d.content, images = d.images,
                        videoUrl = d.videoUrl, videoCover = d.videoCover,
                        topicId = d.topicId, hasDraft = true,
                    )
                }
            }
    }

    private fun saveDraft() {
        val s = _ui.value
        if (s.title.isBlank() && s.content.isBlank() && s.images.isEmpty() && s.videoUrl == null && s.topicId == null) return
        val json = gson.toJson(PublishDraft(s.title, s.content, s.images, s.videoUrl, s.videoCover, s.topicId))
        prefs.edit().putString("draft", json).apply()
        _ui.value = s.copy(hasDraft = true)
    }
}
