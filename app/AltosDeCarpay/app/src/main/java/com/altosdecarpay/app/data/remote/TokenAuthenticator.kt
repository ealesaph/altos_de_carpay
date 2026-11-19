package com.altosdecarpay.app.data.remote

import com.altosdecarpay.app.data.model.TokenRefreshResponse
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val tokenManager: TokenManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Evitar loop infinito
        if (response.request.header("X-Retry") != null) {
            tokenManager.clearSession()
            return null
        }

        val refreshToken = tokenManager.refreshToken ?: run {
            tokenManager.clearSession()
            return null
        }

        val json = """{"refresh":"$refreshToken"}"""
        val body = json.toRequestBody("application/json".toMediaType())

        val refreshRequest = Request.Builder()
            .url(ApiClient.BASE_URL + "auth/refresh/")
            .post(body)
            .build()

        val client = OkHttpClient()
        val refreshResponse = try {
            client.newCall(refreshRequest).execute()
        } catch (_: Exception) {
            tokenManager.clearSession()
            return null
        }

        if (!refreshResponse.isSuccessful) {
            tokenManager.clearSession()
            return null
        }

        val responseBody = refreshResponse.body?.string() ?: run {
            tokenManager.clearSession()
            return null
        }

        val tokens = try {
            Gson().fromJson(responseBody, TokenRefreshResponse::class.java)
        } catch (_: Exception) {
            tokenManager.clearSession()
            return null
        }

        tokenManager.accessToken = tokens.access
        if (tokens.refresh != null) {
            tokenManager.refreshToken = tokens.refresh
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${tokens.access}")
            .header("X-Retry", "true")
            .build()
    }
}
