package com.xiaolanshu.app.data.repository

import com.xiaolanshu.app.data.ApiClient
import com.xiaolanshu.app.data.ApiResponse
import com.xiaolanshu.app.data.Album
import com.xiaolanshu.app.data.AuthData
import com.xiaolanshu.app.data.CollectResult
import com.xiaolanshu.app.data.CommentItem
import com.xiaolanshu.app.data.ReportResult
import com.xiaolanshu.app.data.Conversation
import com.xiaolanshu.app.data.HotWord
import com.xiaolanshu.app.data.LikeResult
import com.xiaolanshu.app.data.Message
import com.xiaolanshu.app.data.NotificationItem
import com.xiaolanshu.app.data.PageResult
import com.xiaolanshu.app.data.PostCard
import com.xiaolanshu.app.data.PostDetail
import com.xiaolanshu.app.data.SearchResult
import com.xiaolanshu.app.data.SmsCodeData
import com.xiaolanshu.app.data.Topic
import com.xiaolanshu.app.data.UploadResult
import com.xiaolanshu.app.data.User
import okhttp3.MultipartBody

/**
 * 数据仓库层（MVVM 分层第一期）：
 * 统一封装所有网络请求，UI 层通过 Repository 获取数据，不再直接接触 Retrofit。
 * 后续页面逐步迁移到 ViewModel + Repository 模式。
 */
object PostRepository {
    suspend fun feed(tab: String, page: Int, size: Int, topicId: Long?): ApiResponse<PageResult<PostCard>> =
        ApiClient.api().feed(tab, page, size, topicId)

    suspend fun detail(id: Long): ApiResponse<PostDetail> = ApiClient.api().postDetail(id)

    suspend fun related(id: Long): ApiResponse<List<PostCard>> = ApiClient.api().related(id)

    suspend fun create(body: Map<String, Any?>): ApiResponse<PostDetail> = ApiClient.api().createPost(body)

    suspend fun update(id: Long, body: Map<String, Any?>): ApiResponse<PostDetail> =
        ApiClient.api().updatePost(id, body)

    suspend fun delete(id: Long): ApiResponse<Void> = ApiClient.api().deletePost(id)

    suspend fun like(id: Long): ApiResponse<LikeResult> = ApiClient.api().like(id)

    suspend fun unlike(id: Long): ApiResponse<LikeResult> = ApiClient.api().unlike(id)

    suspend fun collect(id: Long): ApiResponse<CollectResult> = ApiClient.api().collect(id)

    suspend fun uncollect(id: Long): ApiResponse<CollectResult> = ApiClient.api().uncollect(id)

    suspend fun comments(id: Long, page: Int, size: Int): ApiResponse<PageResult<CommentItem>> =
        ApiClient.api().comments(id, page, size)

    suspend fun addComment(id: Long, body: Map<String, Any?>): ApiResponse<CommentItem> =
        ApiClient.api().addComment(id, body)

    suspend fun report(postId: Long, reason: String, detail: String): ApiResponse<ReportResult> =
        ApiClient.api().report(mapOf("postId" to postId.toString(), "reason" to reason, "detail" to detail))
}

object UserRepository {
    suspend fun profile(id: Long): ApiResponse<User> = ApiClient.api().profile(id)

    suspend fun updateMe(body: Map<String, String?>): ApiResponse<User> = ApiClient.api().updateMe(body)

    suspend fun changePassword(body: Map<String, String>): ApiResponse<Void> = ApiClient.api().changePassword(body)

    suspend fun follow(id: Long): ApiResponse<Void> = ApiClient.api().follow(id)

    suspend fun unfollow(id: Long): ApiResponse<Void> = ApiClient.api().unfollow(id)

    suspend fun following(id: Long, page: Int, size: Int): ApiResponse<PageResult<User>> =
        ApiClient.api().followingList(id, page, size)

    suspend fun followers(id: Long, page: Int, size: Int): ApiResponse<PageResult<User>> =
        ApiClient.api().followersList(id, page, size)

    suspend fun userPosts(id: Long, page: Int, size: Int): ApiResponse<PageResult<PostCard>> =
        ApiClient.api().userPosts(id, page, size)

    suspend fun myCollects(page: Int, size: Int): ApiResponse<PageResult<PostCard>> =
        ApiClient.api().myCollects(page, size)

    suspend fun myLikes(page: Int, size: Int): ApiResponse<PageResult<PostCard>> =
        ApiClient.api().myLikes(page, size)

    suspend fun myHistory(page: Int, size: Int): ApiResponse<PageResult<PostCard>> =
        ApiClient.api().myHistory(page, size)
}

object SearchRepository {
    suspend fun search(q: String, type: String): ApiResponse<SearchResult> = ApiClient.api().search(q, type)

    suspend fun hot(): ApiResponse<List<HotWord>> = ApiClient.api().searchHot()
}

object NotificationRepository {
    suspend fun list(type: String?, page: Int, size: Int): ApiResponse<PageResult<NotificationItem>> =
        ApiClient.api().notifications(type, page, size)

    suspend fun unreadCount(type: String? = null): ApiResponse<Int> =
        ApiClient.api().notificationUnreadCount(type)

    suspend fun readAll(): ApiResponse<Int> = ApiClient.api().readAllNotifications()
}

object ConversationRepository {
    suspend fun conversations(): ApiResponse<List<Conversation>> = ApiClient.api().conversations()

    suspend fun messages(id: Long, page: Int, size: Int): ApiResponse<PageResult<Message>> =
        ApiClient.api().messages(id, page, size)

    suspend fun send(id: Long, body: Map<String, String>): ApiResponse<Message> =
        ApiClient.api().sendMessage(id, body)

    suspend fun markRead(id: Long): ApiResponse<Map<String, Long>> =
        ApiClient.api().markConversationRead(id)
}

object TopicRepository {
    suspend fun topics(): ApiResponse<List<Topic>> = ApiClient.api().topics()
}

object UploadRepository {
    suspend fun image(part: MultipartBody.Part): ApiResponse<UploadResult> = ApiClient.api().uploadImage(part)

    suspend fun video(part: MultipartBody.Part): ApiResponse<UploadResult> = ApiClient.api().uploadVideo(part)
}

object AuthRepository {
    suspend fun login(body: Map<String, String>): ApiResponse<AuthData> = ApiClient.api().login(body)

    suspend fun register(body: Map<String, String>): ApiResponse<AuthData> = ApiClient.api().register(body)

    suspend fun sendSmsCode(body: Map<String, String>): ApiResponse<SmsCodeData> = ApiClient.api().sendSmsCode(body)

    suspend fun resetPassword(body: Map<String, String>): ApiResponse<Map<String, String>> =
        ApiClient.api().resetPassword(body)

    suspend fun me(): ApiResponse<User> = ApiClient.api().me()
}

object AlbumRepository {
    suspend fun albums(): ApiResponse<List<Album>> = ApiClient.api().albums()

    suspend fun create(name: String): ApiResponse<Album> = ApiClient.api().createAlbum(mapOf("name" to name))

    suspend fun delete(id: Long): ApiResponse<Void> = ApiClient.api().deleteAlbum(id)

    suspend fun posts(id: Long, page: Int, size: Int): ApiResponse<PageResult<PostCard>> =
        ApiClient.api().albumPosts(id, page, size)

    suspend fun addPost(albumId: Long, postId: Long): ApiResponse<Void> =
        ApiClient.api().addPostToAlbum(albumId, mapOf("postId" to postId))

    suspend fun removePost(albumId: Long, postId: Long): ApiResponse<Void> =
        ApiClient.api().removePostFromAlbum(albumId, postId)
}
