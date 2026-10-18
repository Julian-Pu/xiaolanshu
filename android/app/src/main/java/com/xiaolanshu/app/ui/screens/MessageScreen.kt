package com.xiaolanshu.app.ui.screens
import com.xiaolanshu.app.ui.theme.LocalAppColors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.xiaolanshu.app.MainActivity
import com.xiaolanshu.app.data.AuthStore
import com.xiaolanshu.app.data.NotificationItem
import com.xiaolanshu.app.ui.components.Avatar
import com.xiaolanshu.app.ui.components.EmptyView
import com.xiaolanshu.app.ui.components.LoadingView
import com.xiaolanshu.app.ui.viewmodel.MessageViewModel
import com.xiaolanshu.app.util.TimeUtil

private const val NOTIF_CHANNEL = "xls_notify"
private const val NOTIF_PERMISSION_REQUEST = 1001

private fun showNotif(context: Context, n: NotificationItem) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (manager.getNotificationChannel(NOTIF_CHANNEL) == null) {
        manager.createNotificationChannel(
            NotificationChannel(NOTIF_CHANNEL, "互动通知", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED) {
        return
    }
    val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    val pi = PendingIntent.getActivity(context, n.id.toInt(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val title = when (n.type) {
        "like" -> "赞了你的笔记"
        "collect" -> "收藏了你的笔记"
        "comment" -> "评论了你的笔记"
        "follow" -> "关注了你"
        else -> "小蓝书"
    }
    NotificationManagerCompat.from(context).notify(
        n.id.toInt(),
        NotificationCompat.Builder(context, NOTIF_CHANNEL)
            .setSmallIcon(com.xiaolanshu.app.R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(n.content)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
    )
}

private val NOTIF_TYPES = listOf(
    null to "全部",
    "like" to "赞",
    "collect" to "收藏",
    "comment" to "评论",
    "follow" to "关注",
)

@Composable
fun MessageScreen(onOpenUser: (Long) -> Unit, onNotifClick: (String, Long?) -> Unit = { _, _ -> }, onUnreadChange: (Int) -> Unit = {}) {
    var tab by rememberSaveable { mutableIntStateOf(0) } // 0 通知 1 私信
    var notifType by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    val vm: MessageViewModel = viewModel()
    val ui by vm.ui.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        // Android 13+ 通知栏权限（首次进入消息页请求；拒绝后仅站内红点提示）
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(
                context as android.app.Activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIF_PERMISSION_REQUEST,
            )
        }
        vm.initialLoad(onUnreadChange, onNotify = { n -> showNotif(context, n) })
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            listOf("通知", "私信").forEachIndexed { i, label ->
                Text(
                    label,
                    fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal,
                    color = if (tab == i) LocalAppColors.current.brandBlue else LocalAppColors.current.textSecondary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable { tab = i },
                )
            }
        }

        if (ui.loading) {
            LoadingView()
            return
        }

        if (ui.chatId != null) {
            // 聊天详情
            Column(Modifier.fillMaxSize()) {
                val conv = ui.conversations.firstOrNull { it.id == ui.chatId }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "‹ 返回",
                        color = LocalAppColors.current.brandBlue,
                        modifier = Modifier.clickable { vm.closeChat() },
                    )
                    Spacer(Modifier.width(12.dp))
                    if (conv != null) {
                        Avatar(conv.peer.avatar, size = 28.dp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(conv.peer.nickname, fontWeight = FontWeight.SemiBold)
                            if (ui.typingConversationId == ui.chatId) {
                                Text(
                                    "对方正在输入…",
                                    color = LocalAppColors.current.brandBlue,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(ui.messages, key = { it.id }) { m ->
                        val mine = AuthStore.get().currentUser?.id == m.senderId
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
                        ) {
                            Text(
                                m.content,
                                color = if (mine) LocalAppColors.current.white else LocalAppColors.current.textSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .then(
                                        if (mine) Modifier.background(
                                            LocalAppColors.current.brandBlue,
                                            RoundedCornerShape(12.dp),
                                        ) else Modifier.background(
                                            LocalAppColors.current.divider,
                                            RoundedCornerShape(12.dp),
                                        )
                                    )
                                    .padding(10.dp),
                            )
                            if (mine) {
                                Text(
                                    if (m.isRead) "已读" else "未读",
                                    color = LocalAppColors.current.textTertiary,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            val id = ui.chatId
                            if (id != null) vm.onTypingInput(id)
                        },
                        placeholder = { Text("发送消息") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "发送",
                        color = LocalAppColors.current.brandBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            val id = ui.chatId
                            val text = input
                            if (id != null) {
                                vm.send(id, text) { input = "" }
                            }
                        },
                    )
                }
            }
            return
        }

        if (tab == 0) {
            // 通知分类 tab + 全部已读
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(NOTIF_TYPES) { (type, label) ->
                        val selected = notifType == type
                        Text(
                            label,
                            color = if (selected) LocalAppColors.current.white else LocalAppColors.current.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) LocalAppColors.current.brandBlue else LocalAppColors.current.brandBlueLight)
                                .clickable {
                                    notifType = type
                                    vm.loadNotifications(type, onUnreadChange)
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
                Text(
                    "全部已读",
                    color = LocalAppColors.current.textTertiary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clickable { vm.markAllRead(onUnreadChange) }.padding(start = 8.dp),
                )
            }
            Spacer(Modifier.height(6.dp))

            if (ui.notifications.isEmpty()) {
                EmptyView("暂无通知")
            } else {
                LazyColumn {
                    items(ui.notifications, key = { it.id }) { n ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onNotifClick(n.type, n.refId) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(n.actor?.avatar, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(n.content, style = MaterialTheme.typography.bodyMedium)
                                Text(TimeUtil.ago(n.createdAt), color = LocalAppColors.current.textTertiary, style = MaterialTheme.typography.labelSmall)
                            }
                            if (!n.isRead) {
                                Spacer(Modifier.width(6.dp))
                                Text("●", color = LocalAppColors.current.brandBlue, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        } else {
            if (ui.conversations.isEmpty()) {
                EmptyView("暂无会话")
            } else {
                LazyColumn {
                    items(ui.conversations, key = { it.id }) { c ->
                        Card(onClick = { vm.openChat(c.id) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Avatar(c.peer.avatar, size = 44.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.peer.nickname, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        c.lastMessage ?: "开始聊天吧",
                                        color = LocalAppColors.current.textTertiary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(TimeUtil.ago(c.updatedAt), color = LocalAppColors.current.textTertiary, style = MaterialTheme.typography.labelSmall)
                                    if (c.unreadCount > 0) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            if (c.unreadCount > 99) "99+" else c.unreadCount.toString(),
                                            color = LocalAppColors.current.white,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(LocalAppColors.current.dangerRed)
                                                .padding(horizontal = 6.dp, vertical = 1.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
