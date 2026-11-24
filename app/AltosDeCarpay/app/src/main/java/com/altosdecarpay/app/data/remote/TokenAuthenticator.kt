package com.altosdecarpay.app.data.remote

import android.content.Context
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
    private val context: Context,
    private val tokenManager: TokenManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Si el token rechazado ya no es el vigente (p. ej. se inició sesión mientras la petición viajaba),
        // se reintenta con el actual en vez de borrar la sesión nueva.
        val tokenEnviado = response.request.header("Authorization")?.removePrefix("Bearer ")
        val tokenActual = tokenManager.accessToken
        if (tokenActual != null && tokenEnviado != tokenActual) {
            if (response.request.header("X-Retry") != null) return null
            return response.request.newBuilder()
                .header("Authorization", "Bearer $tokenActual")
                .header("X-Retry", "true")
                .build()
        }

        // Evitar loop infinito
        if (response.request.header("X-Retry") != null) {
            expirarSesion()
            return null
        }

        val refreshToken = tokenManager.refreshToken ?: return null

        val json = """{"refresh":"$refreshToken"}"""
        val body = json.toRequestBody("application/json".toMediaType())

        val baseUrl = ApiClient.getBaseUrl(context)
        val refreshRequest = Request.Builder()
            .url(baseUrl + "auth/refresh/")
            .post(body)
            .build()

        val client = OkHttpClient()
        val refreshResponse = try {
            client.newCall(refreshRequest).execute()
        } catch (_: Exception) {
            expirarSesion()
            return null
        }

        if (!refreshResponse.isSuccessful) {
            expirarSesion()
            return null
        }

        val responseBody = refreshResponse.body?.string() ?: run {
            expirarSesion()
            return null
        }

        val tokens = try {
            Gson().fromJson(responseBody, TokenRefreshResponse::class.java)
        } catch (_: Exception) {
            expirarSesion()
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

    @Synchronized
    private fun expirarSesion() {
        tokenManager.clearSession()
        com.altosdecarpay.app.data.SessionState.clear()
        com.altosdecarpay.app.data.SessionState.sesionExpirada.value = true
    }
}
