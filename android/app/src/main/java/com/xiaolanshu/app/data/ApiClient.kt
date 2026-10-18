package com.xiaolanshu.app.data

import android.content.Context
import com.xiaolanshu.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Retrofit 单例：自动携带 JWT；401 时清除登录态 */
object ApiClient {

    @Volatile
    private var service: ApiService? = null

    fun init(context: Context) {
        AuthStore.init(context)
    }

    fun api(): ApiService {
        val s = service
        if (s != null) return s
        synchronized(this) {
            return service ?: build().also { service = it }
        }
    }

    private fun build(): ApiService {
        val authInterceptor = Interceptor { chain ->
            val token = AuthStore.get().token
            val request = chain.request().newBuilder()
                .apply { if (token != null) header("Authorization", "Bearer $token") }
                .build()
            chain.proceed(request)
        }
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
