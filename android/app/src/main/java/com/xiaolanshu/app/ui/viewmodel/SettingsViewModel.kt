package com.xiaolanshu.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaolanshu.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val oldPwd: String = "",
    val newPwd: String = "",
    val pwdMsg: String = "",
    val pwdErr: String = "",
    val busy: Boolean = false,
)

/** 设置页 ViewModel：修改密码（主题/清缓存为纯本地操作，留在 UI 层） */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui

    fun setOldPwd(v: String) { _ui.value = _ui.value.copy(oldPwd = v) }
    fun setNewPwd(v: String) { _ui.value = _ui.value.copy(newPwd = v) }

    fun changePassword() {
        val s = _ui.value
        if (s.oldPwd.isBlank() || s.newPwd.length < 6) {
            _ui.value = s.copy(pwdErr = "请输入原密码，且新密码至少 6 位", pwdMsg = "")
            return
        }
        _ui.value = s.copy(busy = true, pwdErr = "", pwdMsg = "")
        viewModelScope.launch {
            runCatching { UserRepository.changePassword(mapOf("oldPassword" to s.oldPwd, "newPassword" to s.newPwd)) }
                .onSuccess { resp ->
                    if (resp.code == 0) {
                        _ui.value = _ui.value.copy(pwdMsg = "密码修改成功", oldPwd = "", newPwd = "")
                    } else {
                        _ui.value = _ui.value.copy(pwdErr = resp.message)
                    }
                }
                .onFailure { _ui.value = _ui.value.copy(pwdErr = it.message ?: "网络错误") }
            _ui.value = _ui.value.copy(busy = false)
        }
    }
}
