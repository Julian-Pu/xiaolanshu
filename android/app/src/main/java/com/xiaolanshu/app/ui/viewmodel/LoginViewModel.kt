package com.xiaolanshu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaolanshu.app.data.AuthStore
import com.xiaolanshu.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val mode: Boolean = true, // true=登录 false=注册
    val phone: String = "",
    val password: String = "",
    val nickname: String = "",
    val smsCode: String = "",
    val smsHint: String = "",
    val sendingSms: Boolean = false,
    val busy: Boolean = false,
    val error: String = "",
    val success: Boolean = false,
)

/** 登录/注册 ViewModel：表单状态 + 验证码 + 提交 */
class LoginViewModel : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui

    fun setPhone(v: String) { _ui.value = _ui.value.copy(phone = v) }
    fun setPassword(v: String) { _ui.value = _ui.value.copy(password = v) }
    fun setNickname(v: String) { _ui.value = _ui.value.copy(nickname = v) }
    fun setSmsCode(v: String) { _ui.value = _ui.value.copy(smsCode = v) }
    fun setError(v: String) { _ui.value = _ui.value.copy(error = v) }

    fun switchMode() {
        _ui.value = _ui.value.copy(mode = !_ui.value.mode, error = "")
    }

    /** 忘记密码：验证码校验通过后重置密码；onDone(null)=成功 */
    fun resetPassword(phone: String, code: String, newPassword: String, onDone: (String?) -> Unit) {
        if (!phone.matches(Regex("^1\\d{10}$"))) {
            onDone("请输入正确的 11 位手机号")
            return
        }
        if (newPassword.length < 6) {
            onDone("新密码至少 6 位")
            return
        }
        viewModelScope.launch {
            runCatching {
                AuthRepository.resetPassword(
                    mapOf("phone" to phone, "smsCode" to code, "newPassword" to newPassword),
                )
            }
                .onSuccess { onDone(if (it.code == 0) null else it.message) }
                .onFailure { onDone(it.message ?: "网络错误") }
        }
    }

    fun sendSms() {
        val s = _ui.value
        if (!s.phone.matches(Regex("^1\\d{10}$"))) {
            _ui.value = s.copy(error = "请先输入正确的 11 位手机号")
            return
        }
        _ui.value = s.copy(sendingSms = true, error = "")
        viewModelScope.launch {
            runCatching { AuthRepository.sendSmsCode(mapOf("phone" to s.phone)) }
                .onSuccess { resp ->
                    if (resp.code == 0) {
                        val devCode = resp.data?.code
                        _ui.value = _ui.value.copy(
                            smsHint = if (devCode.isNullOrBlank()) {
                                "验证码已发送，请查看后端控制台日志"
                            } else {
                                "开发模式验证码：$devCode（已打印到服务器日志）"
                            },
                        )
                    } else {
                        _ui.value = _ui.value.copy(error = resp.message)
                    }
                }
                .onFailure { _ui.value = _ui.value.copy(error = it.message ?: "网络错误") }
            _ui.value = _ui.value.copy(sendingSms = false)
        }
    }

    fun submit() {
        val s = _ui.value
        if (!s.phone.matches(Regex("^1\\d{10}$"))) {
            _ui.value = s.copy(error = "请输入正确的 11 位手机号")
            return
        }
        if (s.password.length < 6) {
            _ui.value = s.copy(error = "密码至少 6 位")
            return
        }
        if (!s.mode && s.nickname.isBlank()) {
            _ui.value = s.copy(error = "请填写昵称")
            return
        }
        if (!s.mode && s.smsCode.isBlank()) {
            _ui.value = s.copy(error = "请先获取并输入短信验证码")
            return
        }
        _ui.value = s.copy(busy = true, error = "")
        viewModelScope.launch {
            val body = mapOf("phone" to s.phone, "password" to s.password) +
                    if (!s.mode) mapOf("nickname" to s.nickname, "smsCode" to s.smsCode) else emptyMap()
            runCatching { if (s.mode) AuthRepository.login(body) else AuthRepository.register(body) }
                .onSuccess { resp ->
                    if (resp.code == 0) {
                        resp.data?.let { AuthStore.get().saveAuth(it.token, it.user) }
                        _ui.value = _ui.value.copy(success = true)
                    } else {
                        _ui.value = _ui.value.copy(error = resp.message)
                    }
                }
                .onFailure { _ui.value = _ui.value.copy(error = it.message ?: "网络错误") }
            _ui.value = _ui.value.copy(busy = false)
        }
    }
}
