package com.xiaolanshu.app

import android.app.Application
import com.xiaolanshu.app.data.ApiClient
import com.xiaolanshu.app.data.AuthStore
import com.xiaolanshu.app.data.SettingsStore

class XiaoLanShuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        AuthStore.init(this)
        SettingsStore.init(this)
    }
}
