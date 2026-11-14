package com.maou.bodyplanapp.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 本地保存登录态，避免每次打开 App 都要重新登录。
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bodyplan", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var userId: Long?
        get() = prefs.getLong("userId", 0).takeIf { it > 0 }
        set(value) { prefs.edit().putLong("userId", value ?: 0).apply() }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USERNAME = "username"
    }
}
