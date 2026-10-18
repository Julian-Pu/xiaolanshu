package com.xiaolanshu.app.ui.screens
import com.xiaolanshu.app.ui.theme.LocalAppColors

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.imageLoader
import com.xiaolanshu.app.BuildConfig
import com.xiaolanshu.app.data.AuthStore
import com.xiaolanshu.app.data.SettingsStore
import com.xiaolanshu.app.ui.viewmodel.SettingsViewModel

/** 设置页：深色模式 / 修改密码 / 清缓存 / 关于 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel()
    val ui by vm.ui.collectAsState()
    var themeMode by remember { mutableStateOf(SettingsStore.get().themeMode) }
    var cacheMsg by remember { mutableStateOf("") }

    fun applyTheme(mode: String) {
        themeMode = mode
        SettingsStore.get().themeMode = mode
        // 主题模式变更后重建 Activity 以生效
        (context as? Activity)?.recreate()
    }

    fun clearCache() {
        AuthStore.get().clearSearchHistory()
        // 清图片缓存（Coil）
        context.imageLoader.memoryCache?.clear()
        context.imageLoader.diskCache?.clear()
        // 清发布草稿
        context.getSharedPreferences("xls_draft", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
        cacheMsg = "已清除搜索历史、图片缓存与发布草稿"
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") }
            Text("设置", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {

            // 深色模式
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("外观", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    listOf(
                        "system" to "跟随系统",
                        "light" to "浅色",
                        "dark" to "深色",
                    ).forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { applyTheme(mode) }.padding(vertical = 2.dp),
                        ) {
                            RadioButton(selected = themeMode == mode, onClick = { applyTheme(mode) })
                            Text(label, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // 修改密码
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("修改密码", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ui.oldPwd,
                        onValueChange = vm::setOldPwd,
                        label = { Text("原密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ui.newPwd,
                        onValueChange = vm::setNewPwd,
                        label = { Text("新密码（至少 6 位）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (ui.pwdMsg.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(ui.pwdMsg, color = LocalAppColors.current.brandBlue, style = MaterialTheme.typography.labelMedium)
                    }
                    if (ui.pwdErr.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(ui.pwdErr, color = LocalAppColors.current.dangerRed, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { vm.changePassword() },
                        enabled = !ui.busy,
                        colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.brandBlue),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(if (ui.busy) "提交中…" else "确认修改")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // 清缓存
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("存储", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("清除本地数据", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "搜索历史、图片缓存、发布草稿",
                                style = MaterialTheme.typography.labelSmall,
                                color = LocalAppColors.current.textTertiary,
                            )
                        }
                        Text(
                            "清缓存",
                            color = LocalAppColors.current.brandBlue,
                            modifier = Modifier.clickable { clearCache() }.padding(8.dp),
                        )
                    }
                    if (cacheMsg.isNotBlank()) {
                        Text(cacheMsg, color = LocalAppColors.current.brandBlue, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // 关于
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("关于", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("小蓝书 · 对标小红书的全栈演示应用", style = MaterialTheme.typography.bodyMedium)
                    Text("版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.labelMedium, color = LocalAppColors.current.textSecondary)
                    Text("演示账号：13800000001~5 / 123456", style = MaterialTheme.typography.labelSmall, color = LocalAppColors.current.textTertiary)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
