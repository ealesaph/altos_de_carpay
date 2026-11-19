package com.altosdecarpay.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.altosdecarpay.app.data.model.UsuarioInfo
import com.google.gson.Gson

class TokenManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("altos_de_carpay_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_USER = "user_info"
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_ACCESS, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_REFRESH, value).apply()

    var userInfo: UsuarioInfo?
        get() {
            val json = prefs.getString(KEY_USER, null) ?: return null
            return try { Gson().fromJson(json, UsuarioInfo::class.java) } catch (_: Exception) { null }
        }
        set(value) {
            if (value != null) {
                prefs.edit().putString(KEY_USER, Gson().toJson(value)).apply()
            } else {
                prefs.edit().remove(KEY_USER).apply()
            }
        }

    val isLoggedIn: Boolean get() = accessToken != null

    val userRole: String? get() = userInfo?.rol

    fun saveTokens(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_USER)
            .apply()
    }
}
