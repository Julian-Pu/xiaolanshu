package com.xiaolanshu.app.ui.components
import com.xiaolanshu.app.ui.theme.LocalAppColors

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaolanshu.app.data.ApiClient
import com.xiaolanshu.app.data.AuthStore
import kotlinx.coroutines.launch

/** 关注 / 取消关注按钮；未登录时回调 onLoginRequired */
@Composable
fun FollowButton(
    userId: Long,
    isFollowing: Boolean,
    modifier: Modifier = Modifier,
    onLoginRequired: () -> Unit = {},
    onChanged: (Boolean) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var following by rememberSaveable(userId) { mutableStateOf(isFollowing) }
    var busy by remember { mutableStateOf(false) }
    val me = AuthStore.get().currentUser

    if (me?.id == userId) {
        OutlinedButton(onClick = {}, modifier = modifier) {
            Text("我")
        }
        return
    }

    fun toggle() {
        if (AuthStore.get().token == null) {
            onLoginRequired()
            return
        }
        busy = true
        scope.launch {
            runCatching {
                if (following) ApiClient.api().unfollow(userId)
                else ApiClient.api().follow(userId)
            }.onSuccess {
                following = !following
                onChanged(following)
            }
            busy = false
        }
    }

    if (following) {
        OutlinedButton(onClick = { toggle() }, enabled = !busy, modifier = modifier) {
            Text("已关注")
        }
    } else {
        Button(
            onClick = { toggle() },
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.brandBlue),
            modifier = modifier,
        ) {
            Text("关注")
        }
    }
}
