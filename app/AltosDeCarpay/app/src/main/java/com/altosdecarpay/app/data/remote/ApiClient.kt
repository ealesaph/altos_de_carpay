package com.altosdecarpay.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Emulador Android: 10.0.2.2 apunta al localhost de la máquina host.
    // Dispositivo físico en la misma red: usar la IP local del servidor (ej. 192.168.1.X:8000).
    const val DEFAULT_BASE_URL = "http://192.168.1.85:8000/api/v1/"
    private const val PREF_KEY_BASE_URL = "custom_base_url"

    private var retrofit: Retrofit? = null
    private var tokenManager: TokenManager? = null
    private var currentBaseUrl: String? = null

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences("altos_de_carpay_prefs", Context.MODE_PRIVATE)
        return prefs.getString(PREF_KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(context: Context, newUrl: String) {
        val formatted = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        val prefs = context.getSharedPreferences("altos_de_carpay_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY_BASE_URL, formatted).apply()
        retrofit = null // Forzar reinicialización con la nueva URL
    }

    fun getTokenManager(context: Context): TokenManager {
        if (tokenManager == null) {
            tokenManager = TokenManager(context.applicationContext)
        }
        return tokenManager!!
    }

    fun getApiService(context: Context): ApiService {
        val url = getBaseUrl(context)
        if (retrofit == null || currentBaseUrl != url) {
            currentBaseUrl = url
            val tm = getTokenManager(context)

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tm))
                .addInterceptor(logging)
                .authenticator(TokenAuthenticator(context.applicationContext, tm))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val gson = GsonBuilder()
                .setLenient()
                .create()

            retrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }
}
