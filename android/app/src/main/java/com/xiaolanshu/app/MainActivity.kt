package com.xiaolanshu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import com.xiaolanshu.app.data.SettingsStore
import com.xiaolanshu.app.ui.navigation.AppNav
import com.xiaolanshu.app.ui.theme.XiaoLanShuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode = SettingsStore.get().themeMode
            val dark = when (mode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            XiaoLanShuTheme(darkTheme = dark) {
                AppNav()
            }
        }
    }
}
