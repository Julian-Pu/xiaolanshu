package com.xiaolanshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaolanshu.app.data.AuthStore
import com.xiaolanshu.app.data.ChatSocket
import com.xiaolanshu.app.data.Conversation
import com.xiaolanshu.app.data.Message
import com.xiaolanshu.app.data.NotificationItem
import com.xiaolanshu.app.data.repository.ConversationRepository
import com.xiaolanshu.app.data.repository.NotificationRepository
import com.xiaolanshu.app.ui.state.AppState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MessageUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val loading: Boolean = true,
    val chatId: Long? = null,
    val messages: List<Message> = emptyList(),
    val typingConversationId: Long? = null,   // 对方正在输入的会话
)

/** 消息页 ViewModel：通知分类 / 会话列表 / 聊天 + WebSocket 实时收信 + 已读回执/正在输入 */
class MessageViewModel : ViewModel() {

    private val _ui = MutableStateFlow(MessageUiState())
    val ui: StateFlow<MessageUiState> = _ui

    fun loadNotifications(type: String?, onUnreadChange: (Int) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { NotificationRepository.list(type, 1, 50) }
                .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(notifications = it.data?.list ?: emptyList()) }
            runCatching { NotificationRepository.unreadCount() }
                .onSuccess {
                    if (it.code == 0) {
                        val unread = it.data ?: 0
                        // 跨页共享：写全局状态（底部角标任意页面即时可见）
                        AppState.notifUnread.value = unread
                        onUnreadChange(unread)
                    }
                }
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            runCatching { ConversationRepository.conversations() }
                .onSuccess {
                    if (it.code == 0) {
                        val convs = it.data ?: emptyList()
                        _ui.value = _ui.value.copy(conversations = convs)
                        // 跨页共享：私信未读总数
                        AppState.msgUnread.value = convs.sumOf { c -> c.unreadCount.toInt() }
                    }
                }
        }
    }

    fun initialLoad(onUnreadChange: (Int) -> Unit = {}, onNotify: (NotificationItem) -> Unit = {}) {
        loadNotifications(null, onUnreadChange)
        loadConversations()
        _ui.value = _ui.value.copy(loading = false)
        // 登录态开启 WebSocket 实时收信 + 实时通知
        if (AuthStore.get().token != null) {
            ChatSocket.connect(
                onMessage = { incoming ->
                    if (incoming.conversationId == _ui.value.chatId) {
                        appendMessage(incoming)
                    }
                    loadConversations()
                },
                onFrame = { type, convId ->
                    if ("typing" == type) {
                        // 仅当前打开的会话显示"对方正在输入"
                        if (convId == _ui.value.chatId) {
                            _ui.value = _ui.value.copy(typingConversationId = convId)
                            // 3 秒后自动消失（未继续输入）
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(3000)
                                if (_ui.value.typingConversationId == convId) {
                                    _ui.value = _ui.value.copy(typingConversationId = null)
                                }
                            }
                        }
                    } else if ("read_ack" == type) {
                        // 对方已读：本端消息全部标记已读（视觉状态刷新）
                        _ui.value = _ui.value.copy(
                            messages = _ui.value.messages.map {
                                if (it.conversationId == convId) it.copy(isRead = true) else it
                            }
                        )
                    }
                },
                onNotify = { notif ->
                    // 多端实时通知：刷新列表 + 未读数（写全局状态，跨页角标即时更新），并上抛（UI 层弹通知栏）
                    loadNotifications(null, onUnreadChange)
                    onNotify(NotificationItem(
                        id = notif.get("id")?.asLong ?: 0,
                        type = notif.get("type")?.asString ?: "",
                        refId = notif.get("refId")?.asLong,
                        content = notif.get("content")?.asString ?: "新通知",
                        isRead = false,
                        createdAt = notif.get("createdAt")?.asString ?: "",
                        actor = null,
                    ))
                },
            )
        }
    }

    override fun onCleared() {
        ChatSocket.disconnect()
        super.onCleared()
    }

    fun markAllRead(onUnreadChange: (Int) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { NotificationRepository.readAll() }
                .onSuccess {
                    AppState.notifUnread.value = 0
                    onUnreadChange(0)
                    loadNotifications(null)
                }
        }
    }

    fun openChat(id: Long) {
        _ui.value = _ui.value.copy(chatId = id)
        viewModelScope.launch {
            runCatching { ConversationRepository.messages(id, 1, 100) }
                .onSuccess { if (it.code == 0) _ui.value = _ui.value.copy(messages = it.data?.list ?: emptyList()) }
        }
        // 已读回执：WS read 帧 + REST 兜底，并刷新会话未读数
        ChatSocket.send("""{"type":"read","conversationId":$id}""")
        viewModelScope.launch {
            runCatching { ConversationRepository.markRead(id) }
            loadConversations()
        }
    }

    fun closeChat() {
        _ui.value = _ui.value.copy(chatId = null, messages = emptyList(), typingConversationId = null)
    }

    /** 输入变化时发送"正在输入"帧 */
    fun onTypingInput(chatId: Long) {
        ChatSocket.send("""{"type":"typing","conversationId":$chatId}""")
    }

    fun send(chatId: Long, text: String, onDone: () -> Unit = {}) {
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching { ConversationRepository.send(chatId, mapOf("content" to text)) }
                .onSuccess {
                    onDone()
                    openChat(chatId)
                }
        }
    }

    /** 供 WebSocket 实时消息追加 */
    fun appendMessage(m: Message) {
        _ui.value = _ui.value.copy(messages = _ui.value.messages + m)
    }
}
