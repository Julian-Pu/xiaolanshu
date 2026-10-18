package com.xiaolanshu.app.data

// 与后端 API 对齐的数据模型

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?,
) {
    fun requireData(): T = data ?: throw IllegalStateException(message)
}

data class AuthData(val token: String, val user: User)

/** 发送短信验证码的响应：开发模式下 code 为明文验证码（便于联调），生产为 null */
data class SmsCodeData(val code: String? = null)

data class UserBrief(
    val id: Long,
    val nickname: String,
    val avatar: String?,
    val isFollowing: Boolean,
)

data class User(
    val id: Long,
    val nickname: String,
    val avatar: String?,
    val bio: String?,
    val gender: String,
    val followingCount: Int,
    val followerCount: Int,
    val postCount: Int,
    val createdAt: String,
    val isFollowing: Boolean,
)

data class TopicBrief(val id: Long, val name: String)

data class Topic(
    val id: Long,
    val name: String,
    val cover: String?,
    val postCount: Int,
)

data class PostCard(
    val id: Long,
    val title: String?,
    val cover: String?,
    val coverThumb: String? = null,
    val images: List<String>,
    val videoUrl: String? = null,
    val likeCount: Int,
    val collectCount: Int,
    val commentCount: Int,
    val viewCount: Int,
    val createdAt: String,
    val author: UserBrief,
    val topic: TopicBrief?,
    val isLiked: Boolean,
    val isCollected: Boolean,
)

data class PostDetail(
    val id: Long,
    val title: String?,
    val content: String?,
    val cover: String?,
    val images: List<String>,
    val videoUrl: String? = null,
    val videoCover: String? = null,
    val likeCount: Int,
    val collectCount: Int,
    val commentCount: Int,
    val viewCount: Int,
    val createdAt: String,
    val author: UserBrief,
    val topic: TopicBrief?,
    val isLiked: Boolean,
    val isCollected: Boolean,
)

data class CommentItem(
    val id: Long,
    val content: String,
    val likeCount: Int,
    val createdAt: String,
    val user: UserBrief,
    val replyTo: UserBrief?,
    val replies: List<CommentItem>,
)

data class PageResult<T>(
    val list: List<T>,
    val page: Int,
    val size: Int,
    val total: Long,
    val hasMore: Boolean,
)

data class NotificationItem(
    val id: Long,
    val type: String,
    val refId: Long?,
    val content: String,
    val isRead: Boolean,
    val createdAt: String,
    val actor: UserBrief?,
)

data class Conversation(
    val id: Long,
    val lastMessage: String?,
    val updatedAt: String,
    val peer: UserBrief,
    val unreadCount: Long = 0,
)

data class Message(
    val id: Long,
    val conversationId: Long,
    val senderId: Long,
    val content: String,
    val isRead: Boolean,
    val createdAt: String,
)

data class SearchResult(val posts: List<PostCard>, val users: List<User>, val snippets: List<String?>? = null)

data class HotWord(val word: String, val cnt: Int)

data class LikeResult(val liked: Boolean, val likeCount: Int)

data class CollectResult(val collected: Boolean, val collectCount: Int)

/** 举报结果：duplicated=true 表示已提交过同类举报 */
data class ReportResult(val id: Long, val duplicated: Boolean = false)

data class UploadResult(val url: String, val thumbUrl: String? = null)

/** 收藏专辑 */
data class Album(
    val id: Long,
    val name: String,
    val postCount: Int,
    val createdAt: String,
)
