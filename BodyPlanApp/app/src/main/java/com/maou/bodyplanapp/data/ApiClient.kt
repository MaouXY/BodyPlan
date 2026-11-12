package com.maou.bodyplanapp.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.maou.bodyplanapp.BuildConfig

/**
 * Retrofit 单例。
 *
 * 注意 [BASE_URL]：
 * - Android 模拟器访问电脑上的后端必须用 10.0.2.2（10.0.2.2 才是宿主机）；
 * - 真机调试时改成电脑的局域网 IP，例如 http://192.168.1.10:8993/ ；
 * - 后端端口与 doc/个性化健身管理系统_分阶段实现计划.md 的约定一致（8993）。
 */
object ApiClient {

    const val BASE_URL = BuildConfig.API_BASE_URL

    @Volatile
    var token: String? = null

    val service: ApiService by lazy { build() }

    private fun build(): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                token?.takeIf { it.isNotBlank() }?.let {
                    builder.addHeader("Authorization", "Bearer $it")
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
