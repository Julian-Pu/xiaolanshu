package com.xiaolanshu.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import com.xiaolanshu.app.data.SettingsStore
import com.xiaolanshu.app.ui.navigation.AppNav
import com.xiaolanshu.app.ui.theme.XiaoLanShuTheme

class MainActivity : ComponentActivity() {

    /** 深链解析结果（xls://post/{id} → "post/{id}"），onNewIntent 也会更新 */
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute.value = routeFromIntent(intent)
        enableEdgeToEdge()
        setContent {
            val mode = SettingsStore.get().themeMode
            val dark = when (mode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            XiaoLanShuTheme(darkTheme = dark) {
                AppNav(pendingRoute = pendingRoute.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = routeFromIntent(intent)
    }

    /** 解析分享深链：xls://post/{id} → 路由 "post/{id}"；其它返回 null */
    private fun routeFromIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        val id = data.lastPathSegment?.toLongOrNull() ?: return null
        return when {
            data.scheme == "xls" && data.host == "post" -> "post/$id"
            else -> null
        }
    }
}
