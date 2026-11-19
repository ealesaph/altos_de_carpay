package com.altosdecarpay.app.data.remote

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Emulador Android: 10.0.2.2 apunta al localhost de la máquina host.
    // Dispositivo físico en la misma red: usar la IP local del servidor.
    const val BASE_URL = "http://10.0.2.2:8000/api/v1/"

    private var retrofit: Retrofit? = null
    private var tokenManager: TokenManager? = null

    fun getTokenManager(context: Context): TokenManager {
        if (tokenManager == null) {
            tokenManager = TokenManager(context.applicationContext)
        }
        return tokenManager!!
    }

    fun getApiService(context: Context): ApiService {
        if (retrofit == null) {
            val tm = getTokenManager(context)

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tm))
                .addInterceptor(logging)
                .authenticator(TokenAuthenticator(tm))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val gson = GsonBuilder()
                .setLenient()
                .create()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }
}
