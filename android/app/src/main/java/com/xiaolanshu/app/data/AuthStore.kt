package com.xiaolanshu.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

/** 本地登录态：token + 用户信息 */
class AuthStore private constructor(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("xls_auth", Context.MODE_PRIVATE)

    var token: String?
        get() = sp.getString(KEY_TOKEN, null)
        set(value) = sp.edit().putString(KEY_TOKEN, value).apply()

    private var userJson: String?
        get() = sp.getString(KEY_USER, null)
        set(value) = sp.edit().putString(KEY_USER, value).apply()

    val currentUser: User?
        get() = userJson?.let { runCatching { gson.fromJson(it, User::class.java) }.getOrNull() }

    fun saveAuth(token: String, user: User) {
        this.token = token
        userJson = gson.toJson(user)
    }

    fun saveUser(user: User) {
        userJson = gson.toJson(user)
    }

    fun logout() {
        sp.edit().clear().apply()
    }

    // ---- 搜索历史（本地最近 10 条）----
    fun searchHistory(): List<String> {
        return sp.getString(KEY_HISTORY, null)
            ?.split("\u0001")?.filter { it.isNotBlank() } ?: emptyList()
    }

    fun addSearchHistory(word: String) {
        val w = word.trim()
        if (w.isEmpty()) return
        val list = searchHistory().filterNot { it == w }.toMutableList()
        list.add(0, w)
        if (list.size > 10) list.removeAt(list.size - 1)
        sp.edit().putString(KEY_HISTORY, list.joinToString("\u0001")).apply()
    }

    fun clearSearchHistory() {
        sp.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER = "user"
        private const val KEY_HISTORY = "search_history"
        private val gson = Gson()

        @Volatile
        private var instance: AuthStore? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = AuthStore(context.applicationContext)
                    }
                }
            }
        }

        fun get(): AuthStore = instance
            ?: throw IllegalStateException("AuthStore 未初始化，请在 Application 中调用 init")
    }
}
