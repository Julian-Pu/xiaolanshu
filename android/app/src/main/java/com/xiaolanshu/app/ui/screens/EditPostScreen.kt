package com.xiaolanshu.app.ui.screens
import com.xiaolanshu.app.ui.theme.LocalAppColors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaolanshu.app.ui.components.LoadingView
import com.xiaolanshu.app.ui.viewmodel.EditPostViewModel

/** 编辑笔记页：标题 / 内容 / 话题（图片与视频保持原样） */
@Composable
fun EditPostScreen(
    postId: Long,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val vm: EditPostViewModel = viewModel { EditPostViewModel(postId) }
    val ui by vm.ui.collectAsState()

    if (ui.loading) {
        LoadingView()
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
            Text("编辑笔记", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                if (ui.saving) "保存中…" else "保存",
                color = if (ui.saving) LocalAppColors.current.textTertiary else LocalAppColors.current.brandBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(enabled = !ui.saving) {
                        vm.save(
                            onSaved = onSaved,
                            onError = { /* 可扩展为 Snackbar */ },
                        )
                    }
                    .padding(12.dp),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = ui.title,
                onValueChange = { vm.setTitle(it) },
                label = { Text("标题") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.content,
                onValueChange = { vm.setContent(it) },
                label = { Text("内容") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text("选择话题", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.topics) { t ->
                    val selected = ui.topicId == t.id
                    Text(
                        "#${t.name}",
                        color = if (selected) LocalAppColors.current.white else LocalAppColors.current.brandBlue,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) LocalAppColors.current.brandBlue else LocalAppColors.current.brandBlueLight)
                            .clickable { vm.setTopic(if (selected) null else t.id) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            if (ui.error.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(ui.error, color = LocalAppColors.current.dangerRed, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
