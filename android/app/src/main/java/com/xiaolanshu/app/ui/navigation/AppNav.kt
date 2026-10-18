package com.xiaolanshu.app.ui.navigation
import com.xiaolanshu.app.ui.theme.LocalAppColors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xiaolanshu.app.data.repository.NotificationRepository
import com.xiaolanshu.app.data.AuthStore
import com.xiaolanshu.app.ui.state.AppState
import com.xiaolanshu.app.ui.screens.DiscoverScreen
import com.xiaolanshu.app.ui.screens.EditPostScreen
import com.xiaolanshu.app.ui.screens.HistoryScreen
import com.xiaolanshu.app.ui.screens.HomeScreen
import com.xiaolanshu.app.ui.screens.LoginScreen
import com.xiaolanshu.app.ui.screens.MessageScreen
import com.xiaolanshu.app.ui.screens.PostDetailScreen
import com.xiaolanshu.app.ui.screens.ProfileScreen
import com.xiaolanshu.app.ui.screens.PublishScreen
import com.xiaolanshu.app.ui.screens.SettingsScreen
import com.xiaolanshu.app.ui.screens.TopicPostsScreen
import com.xiaolanshu.app.ui.screens.UserListScreen
import kotlinx.coroutines.launch

/** 应用路由。pendingRoute：分享深链（如 "post/5"），未登录时先登录、成功后跳转 */
@Composable
fun AppNav(pendingRoute: String? = null) {
    val nav: NavHostController = rememberNavController()
    val start = if (AuthStore.get().token == null) "login" else "main"
    // 深链导航（已登录时直接跳）
    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null && AuthStore.get().token != null) {
            nav.navigate(pendingRoute) { launchSingleTop = true }
        }
    }
    NavHost(navController = nav, startDestination = start) {
        composable("login") {
            LoginScreen(onSuccess = {
                AppState.reset() // 切换账号清空跨页角标
                if (pendingRoute != null) {
                    // 深链目标：登录成功直接进入（登录页出栈，避免返回登录页）
                    nav.navigate(pendingRoute) {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    nav.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            })
        }
        composable("main") {
            MainScreen(
                onOpenPost = { nav.navigate("post/$it") },
                onOpenUser = { nav.navigate("profile/$it") },
                onOpenFollowers = { nav.navigate("followers/$it") },
                onOpenFollowing = { nav.navigate("following/$it") },
                onOpenSettings = { nav.navigate("settings") },
                onOpenHistory = { nav.navigate("history") },
                onOpenNotif = { type, refId ->
                    // like/collect/comment → 笔记详情；follow → 用户主页
                    if (refId != null) {
                        nav.navigate(if (type == "follow") "profile/$refId" else "post/$refId")
                    }
                },
                onRequireLogin = {
                    nav.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                },
            )
        }
        composable("post/{id}") { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
            PostDetailScreen(
                postId = id,
                onBack = { nav.popBackStack() },
                onOpenUser = { nav.navigate("profile/$it") },
                onOpenPost = { nav.navigate("post/$it") },
                onOpenTopic = { topicId, name ->
                    nav.navigate("topic/$topicId/${java.net.URLEncoder.encode(name, "UTF-8")}")
                },
                onRequireLogin = { nav.navigate("login") },
                onEdit = { nav.navigate("edit/$it") },
            )
        }
        composable("edit/{id}") { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
            EditPostScreen(
                postId = id,
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() },
            )
        }
        composable("topic/{id}/{name}") { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
            val name = entry.arguments?.getString("name")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "话题"
            TopicPostsScreen(
                topicId = id,
                topicName = name,
                onBack = { nav.popBackStack() },
                onOpenPost = { nav.navigate("post/$it") },
            )
        }
        composable("profile/{id}") { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull()
            ProfileScreen(
                userId = id,
                onBack = { nav.popBackStack() },
                onOpenPost = { nav.navigate("post/$it") },
                onOpenUser = { nav.navigate("profile/$it") },
                onOpenFollowers = { nav.navigate("followers/$it") },
                onOpenFollowing = { nav.navigate("following/$it") },
                onRequireLogin = { nav.navigate("login") },
            )
        }
        composable("followers/{id}") { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
            UserListScreen(
                userId = id,
                mode = "followers",
                onBack = { nav.popBackStack() },
                onOpenUser = { nav.navigate("profile/$it") },
                onRequireLogin = { nav.navigate("login") },
            )
        }
        composable("following/{id}") { entry ->
            val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
            UserListScreen(
                userId = id,
                mode = "following",
                onBack = { nav.popBackStack() },
                onOpenUser = { nav.navigate("profile/$it") },
                onRequireLogin = { nav.navigate("login") },
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable("history") {
            HistoryScreen(
                onBack = { nav.popBackStack() },
                onOpenPost = { nav.navigate("post/$it") },
            )
        }
    }
}

/** 主框架：底部 TabBar */
@Composable
private fun MainScreen(
    onOpenPost: (Long) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenFollowers: (Long) -> Unit,
    onOpenFollowing: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenNotif: (String, Long?) -> Unit,
    onRequireLogin: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    // 跨页状态：底部角标直接收集全局通知未读数（消息页收到 notify 帧/已读操作即时反映）
    val notifUnread by AppState.notifUnread.collectAsState()

    // 登录后加载消息未读数（写全局状态，供底部 tab 角标）
    LaunchedEffect(AuthStore.get().token) {
        if (AuthStore.get().token != null) {
            runCatching { NotificationRepository.unreadCount() }
                .onSuccess { if (it.code == 0) AppState.notifUnread.value = it.data ?: 0 }
        }
    }

    val tabs = listOf(
        Triple("首页", Icons.Filled.Home, 0),
        Triple("发现", Icons.Filled.Search, 1),
        Triple("发布", Icons.Filled.Add, 2),
        Triple("消息", Icons.Filled.MailOutline, 3),
        Triple("我的", Icons.Filled.Person, 4),
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = {
                            Box {
                                Icon(icon, contentDescription = label)
                                if (index == 3 && notifUnread > 0) {
                                    Badge(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-2).dp)
                                            .background(LocalAppColors.current.brandBlue, RoundedCornerShape(10.dp)),
                                    ) {
                                        Text(if (notifUnread > 99) "99+" else "$notifUnread", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeScreen(onOpenPost = onOpenPost, onRequireLogin = onRequireLogin)
                1 -> DiscoverScreen(onOpenPost = onOpenPost, onOpenUser = onOpenUser)
                2 -> PublishScreen(onPublished = onOpenPost, onRequireLogin = onRequireLogin)
                3 -> MessageScreen(
                    onOpenUser = onOpenUser,
                    onNotifClick = onOpenNotif,
                    // 未读数已由 ViewModel 写全局 AppState（跨页角标自动更新），无需回调传参
                )
                else -> ProfileScreen(
                    userId = null,
                    onBack = {},
                    onOpenPost = onOpenPost,
                    onOpenUser = onOpenUser,
                    onOpenFollowers = onOpenFollowers,
                    onOpenFollowing = onOpenFollowing,
                    onRequireLogin = onRequireLogin,
                    onOpenSettings = onOpenSettings,
                    onOpenHistory = onOpenHistory,
                )
            }
        }
    }
}
