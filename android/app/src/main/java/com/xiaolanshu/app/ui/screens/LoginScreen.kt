package com.xiaolanshu.app.ui.screens
import com.xiaolanshu.app.ui.theme.LocalAppColors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaolanshu.app.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(onSuccess: () -> Unit) {
    val vm: LoginViewModel = viewModel()
    val ui by vm.ui.collectAsState()

    // 登录/注册成功后回调导航
    LaunchedEffect(ui.success) {
        if (ui.success) onSuccess()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))
        Text(
            text = "小蓝书",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAppColors.current.brandBlue,
        )
        Text("记录生活的美好", color = LocalAppColors.current.textTertiary, fontSize = 13.sp)
        Spacer(Modifier.height(32.dp))

        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
            Column(Modifier.padding(24.dp)) {
                OutlinedTextField(
                    value = ui.phone,
                    onValueChange = vm::setPhone,
                    label = { Text("手机号") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!ui.mode) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = ui.smsCode,
                            onValueChange = vm::setSmsCode,
                            label = { Text("短信验证码") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { vm.sendSms() },
                            enabled = !ui.busy && !ui.sendingSms,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(if (ui.sendingSms) "发送中…" else "获取验证码", fontSize = 13.sp)
                        }
                    }
                    if (ui.smsHint.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(ui.smsHint, color = LocalAppColors.current.textTertiary, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (!ui.mode) {
                    OutlinedTextField(
                        value = ui.nickname,
                        onValueChange = vm::setNickname,
                        label = { Text("昵称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = ui.password,
                    onValueChange = vm::setPassword,
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (ui.error.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(ui.error, color = LocalAppColors.current.dangerRed, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { vm.submit() },
                    enabled = !ui.busy,
                    colors = ButtonDefaults.buttonColors(containerColor = LocalAppColors.current.brandBlue),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                ) {
                    Text(if (ui.busy) "请稍候…" else if (ui.mode) "登录" else "注册并登录")
                }
                TextButton(onClick = { vm.switchMode() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (ui.mode) "没有账号？立即注册" else "已有账号？去登录", color = LocalAppColors.current.brandBlue)
                }
                Text(
                    "演示账号：13800000001~5 / 123456",
                    color = LocalAppColors.current.textTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}
