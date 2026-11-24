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

    // Emulador Android: 10.0.2.2 apunta al localhost del PC.
    const val URL_EMULADOR = "http://10.0.2.2:8000/api/v1/"
    // Celular conectado por USB: 127.0.0.1 llega al PC con `adb reverse tcp:8000 tcp:8000` (y tcp:80 para la web).
    // Por Wi-Fi se configura la IP local del PC desde «Servidor» en la pantalla de inicio de sesión.
    const val URL_DISPOSITIVO_USB = "http://127.0.0.1:8000/api/v1/"
    val DEFAULT_BASE_URL: String get() = if (esEmulador()) URL_EMULADOR else URL_DISPOSITIVO_USB
    private const val PREF_KEY_BASE_URL = "custom_base_url"

    private var retrofit: Retrofit? = null
    private var tokenManager: TokenManager? = null
    private var currentBaseUrl: String? = null

    /** Detecta el emulador de Android Studio (imágenes «sdk_gphone», hardware ranchu/goldfish). */
    fun esEmulador(): Boolean {
        val b = android.os.Build.FINGERPRINT + " " + android.os.Build.MODEL + " " + android.os.Build.PRODUCT + " " + android.os.Build.HARDWARE
        return listOf("generic", "emulator", "sdk_gphone", "ranchu", "goldfish", "google_sdk").any { b.contains(it, ignoreCase = true) }
    }

    /** true si la dirección del servidor fue escrita a mano (no es la predeterminada del dispositivo). */
    fun tieneUrlPersonalizada(context: Context): Boolean =
        context.getSharedPreferences("altos_de_carpay_prefs", Context.MODE_PRIVATE).contains(PREF_KEY_BASE_URL)

    /** Vuelve a la dirección predeterminada (emulador o celular por USB). */
    fun restablecerBaseUrl(context: Context) {
        context.getSharedPreferences("altos_de_carpay_prefs", Context.MODE_PRIVATE).edit().remove(PREF_KEY_BASE_URL).apply()
        retrofit = null
    }

    /**
     * Normaliza lo que escribe el usuario: «192.168.1.20» → «http://192.168.1.20:8000/api/v1/».
     * Devuelve null si no parece una dirección válida.
     */
    fun normalizarUrl(texto: String): String? {
        var t = texto.trim().trimEnd('/')
        if (t.isEmpty() || t.contains(' ')) return null
        if (!t.startsWith("http://") && !t.startsWith("https://")) t = "http://$t"
        val uri = try { java.net.URI(t) } catch (e: java.net.URISyntaxException) { return null }
        val host = uri.host ?: return null
        val puerto = if (uri.port != -1) ":${uri.port}" else if (uri.scheme == "https") "" else ":8000"
        val ruta = uri.path?.trimEnd('/')?.takeIf { it.isNotEmpty() } ?: "/api/v1"
        return "${uri.scheme}://$host$puerto$ruta/"
    }

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

    /**
     * true si el servidor configurado está en la red local (emulador 10.0.2.2, IP privada del PC o localhost).
     * En Android 17 esas direcciones requieren el permiso ACCESS_LOCAL_NETWORK.
     */
    fun usaRedLocal(context: Context): Boolean {
        val host = android.net.Uri.parse(getBaseUrl(context)).host?.lowercase() ?: return false
        // 127.0.0.1/localhost (adb reverse) es la propia interfaz del equipo: no es red local.
        if (host.endsWith(".local")) return true
        val partes = host.split(".").mapNotNull { it.toIntOrNull() }
        if (partes.size != 4) return false
        val (a, b) = partes[0] to partes[1]
        return a == 10 || (a == 172 && b in 16..31) || (a == 192 && b == 168) || (a == 169 && b == 254)
    }

    /**
     * Dirección de la página web (mismo servidor que la API, servida por Nginx en el puerto 80).
     * La app no hace reservas ni pagos: los deriva a la web, que es donde están el checkout y Webpay.
     */
    fun urlWeb(context: Context, ruta: String = "/"): String {
        val uri = android.net.Uri.parse(getBaseUrl(context))
        val puerto = if (uri.port == 8000 || uri.port == -1) "" else ":${uri.port}"
        return "${uri.scheme}://${uri.host}$puerto$ruta"
    }

    fun getApiService(context: Context): ApiService {
        val url = getBaseUrl(context)
        if (retrofit == null || currentBaseUrl != url) {
            currentBaseUrl = url
            val tm = getTokenManager(context)

            // BASIC registra método, URL y código de respuesta. BODY escribía en Logcat el correo y la
            // contraseña del inicio de sesión; en una compilación de release no se registra nada.
            val depurable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val logging = HttpLoggingInterceptor().apply {
                level = if (depurable) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
                redactHeader("Authorization")
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
