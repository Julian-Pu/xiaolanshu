package com.xiaolanshu.app.data

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // 鉴权
    @POST("api/auth/register")
    suspend fun register(@Body body: Map<String, String>): ApiResponse<AuthData>

    @POST("api/auth/sms-code")
    suspend fun sendSmsCode(@Body body: Map<String, String>): ApiResponse<SmsCodeData>

    @POST("api/auth/login")
    suspend fun login(@Body body: Map<String, String>): ApiResponse<AuthData>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: Map<String, String>): ApiResponse<Map<String, String>>

    @POST("api/reports")
    suspend fun report(@Body body: Map<String, String>): ApiResponse<ReportResult>

    @GET("api/auth/me")
    suspend fun me(): ApiResponse<User>

    // 信息流
    @GET("api/feed")
    suspend fun feed(
        @Query("tab") tab: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("topicId") topicId: Long? = null,
    ): ApiResponse<PageResult<PostCard>>

    // 笔记
    @GET("api/posts/{id}")
    suspend fun postDetail(@Path("id") id: Long): ApiResponse<PostDetail>

    @GET("api/posts/{id}/related")
    suspend fun related(@Path("id") id: Long): ApiResponse<List<PostCard>>

    @POST("api/posts")
    suspend fun createPost(@Body body: Map<String, Any?>): ApiResponse<PostDetail>

    @PUT("api/posts/{id}")
    suspend fun updatePost(@Path("id") id: Long, @Body body: Map<String, Any?>): ApiResponse<PostDetail>

    @DELETE("api/posts/{id}")
    suspend fun deletePost(@Path("id") id: Long): ApiResponse<Void>

    @POST("api/posts/{id}/like")
    suspend fun like(@Path("id") id: Long): ApiResponse<LikeResult>

    @DELETE("api/posts/{id}/like")
    suspend fun unlike(@Path("id") id: Long): ApiResponse<LikeResult>

    @POST("api/posts/{id}/collect")
    suspend fun collect(@Path("id") id: Long): ApiResponse<CollectResult>

    @DELETE("api/posts/{id}/collect")
    suspend fun uncollect(@Path("id") id: Long): ApiResponse<CollectResult>

    // 评论
    @GET("api/posts/{id}/comments")
    suspend fun comments(
        @Path("id") id: Long,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ApiResponse<PageResult<CommentItem>>

    @POST("api/posts/{id}/comments")
    suspend fun addComment(@Path("id") id: Long, @Body body: Map<String, Any?>): ApiResponse<CommentItem>

    // 话题 / 搜索
    @GET("api/topics")
    suspend fun topics(): ApiResponse<List<Topic>>

    @GET("api/search")
    suspend fun search(@Query("q") q: String, @Query("type") type: String): ApiResponse<SearchResult>

    @GET("api/search/hot")
    suspend fun searchHot(): ApiResponse<List<HotWord>>

    // 用户
    @GET("api/users/{id}")
    suspend fun profile(@Path("id") id: Long): ApiResponse<User>

    @GET("api/users/{id}/following")
    suspend fun followingList(
        @Path("id") id: Long,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ApiResponse<PageResult<User>>

    @GET("api/users/{id}/followers")
    suspend fun followersList(
        @Path("id") id: Long,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ApiResponse<PageResult<User>>

    @POST("api/users/{id}/follow")
    suspend fun follow(@Path("id") id: Long): ApiResponse<Void>

    @DELETE("api/users/{id}/follow")
    suspend fun unfollow(@Path("id") id: Long): ApiResponse<Void>

    @GET("api/users/{id}/posts")
    suspend fun userPosts(
        @Path("id") id: Long,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ApiResponse<PageResult<PostCard>>

    // 我的收藏 / 赞过 / 浏览历史（个人主页 tab）
    @GET("api/users/me/collects")
    suspend fun myCollects(@Query("page") page: Int, @Query("size") size: Int): ApiResponse<PageResult<PostCard>>

    @GET("api/users/me/likes")
    suspend fun myLikes(@Query("page") page: Int, @Query("size") size: Int): ApiResponse<PageResult<PostCard>>

    @GET("api/users/me/history")
    suspend fun myHistory(@Query("page") page: Int, @Query("size") size: Int): ApiResponse<PageResult<PostCard>>

    // 收藏专辑
    @GET("api/albums")
    suspend fun albums(): ApiResponse<List<Album>>

    @POST("api/albums")
    suspend fun createAlbum(@Body body: Map<String, String>): ApiResponse<Album>

    @DELETE("api/albums/{id}")
    suspend fun deleteAlbum(@Path("id") id: Long): ApiResponse<Void>

    @GET("api/albums/{id}/posts")
    suspend fun albumPosts(
        @Path("id") id: Long,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ApiResponse<PageResult<PostCard>>

    @POST("api/albums/{id}/posts")
    suspend fun addPostToAlbum(@Path("id") id: Long, @Body body: Map<String, Long>): ApiResponse<Void>

    @DELETE("api/albums/{id}/posts/{postId}")
    suspend fun removePostFromAlbum(@Path("id") id: Long, @Path("postId") postId: Long): ApiResponse<Void>

    @retrofit2.http.PUT("api/users/me")
    suspend fun updateMe(@Body body: Map<String, String?>): ApiResponse<User>

    @retrofit2.http.POST("api/users/me/password")
    suspend fun changePassword(@Body body: Map<String, String>): ApiResponse<Void>

    // 通知 / 私信
    @GET("api/notifications")
    suspend fun notifications(
        @Query("type") type: String?,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ApiResponse<PageResult<NotificationItem>>

    @GET("api/notifications/unread-count")
    suspend fun notificationUnreadCount(@Query("type") type: String? = null): ApiResponse<Int>

    @POST("api/notifications/read-all")
    suspend fun readAllNotifications(): ApiResponse<Int>

    @GET("api/conversations")
    suspend fun conversations(): ApiResponse<List<Conversation>>

    @GET("api/conversations/{id}/messages")
    suspend fun messages(
        @Path("id") id: Long,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ApiResponse<PageResult<Message>>

    @POST("api/conversations/{id}/messages")
    suspend fun sendMessage(@Path("id") id: Long, @Body body: Map<String, String>): ApiResponse<Message>

    @POST("api/conversations/{id}/read")
    suspend fun markConversationRead(@Path("id") id: Long): ApiResponse<Map<String, Long>>

    // 上传
    @Multipart
    @POST("api/upload/image")
    suspend fun uploadImage(@Part file: MultipartBody.Part): ApiResponse<UploadResult>

    @Multipart
    @POST("api/upload/video")
    suspend fun uploadVideo(@Part file: MultipartBody.Part): ApiResponse<UploadResult>
}
