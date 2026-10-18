package com.xiaolanshu.app.data

import android.content.Context
import android.content.SharedPreferences

/** 应用设置：主题模式等（持久化） */
class SettingsStore private constructor(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("xls_settings", Context.MODE_PRIVATE)

    /** themeMode: system | light | dark */
    var themeMode: String
        get() = sp.getString(KEY_THEME, "system") ?: "system"
        set(value) = sp.edit().putString(KEY_THEME, value).apply()

    companion object {
        private const val KEY_THEME = "theme_mode"

        @Volatile
        private var instance: SettingsStore? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = SettingsStore(context.applicationContext)
                    }
                }
            }
        }

        fun get(): SettingsStore = instance
            ?: throw IllegalStateException("SettingsStore 未初始化，请在 Application 中调用 init")
    }
}
