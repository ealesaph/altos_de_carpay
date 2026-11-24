package com.altosdecarpay.app.data.model

import com.google.gson.annotations.SerializedName

// ── Request ──

data class LoginRequest(
    val correo: String,
    val password: String
)

data class RegistroRequest(
    val correo: String,
    val rut: String,
    val nombre: String,
    @SerializedName("apellido_paterno") val apellidoPaterno: String,
    @SerializedName("apellido_materno") val apellidoMaterno: String,
    val telefono: String,
    val password: String,
    @SerializedName("acepta_politica_datos") val aceptaPoliticaDatos: Boolean = true
)

data class LogoutRequest(
    val refresh: String
)

data class CambioPasswordRequest(
    @SerializedName("password_actual") val passwordActual: String,
    @SerializedName("password_nueva") val passwordNueva: String
)

// ── Response ──

data class LoginResponse(
    val access: String,
    val refresh: String,
    val usuario: UsuarioInfo
)

data class TokenRefreshResponse(
    val access: String,
    val refresh: String? = null
)

data class UsuarioInfo(
    @SerializedName("id_usuario") val idUsuario: Int,
    val nombre: String,
    val rol: String,
    @SerializedName("is_superuser") val isSuperuser: Boolean = false,
    // Ley N° 21.719: true si el titular aún no acepta la versión vigente de la política de datos
    @SerializedName("requiere_consentimiento") val requiereConsentimiento: Boolean = false
)

// ── Ley N° 21.719: política de datos y consentimiento ──

data class SeccionPolitica(
    val titulo: String,
    val contenido: String
)

data class PoliticaPrivacidadResponse(
    val version: String,
    val titulo: String,
    val responsable: Map<String, String>,
    val secciones: List<SeccionPolitica>
)

data class ConsentimientoRequest(
    val acepta: Boolean,
    val version: String
)

data class PerfilResponse(
    @SerializedName("id_usuario") val idUsuario: Int,
    val correo: String,
    val rut: String,
    val nombre: String,
    @SerializedName("apellido_paterno") val apellidoPaterno: String,
    @SerializedName("apellido_materno") val apellidoMaterno: String,
    val telefono: String,
    val rol: String,
    val estado: String,
    @SerializedName("ultima_conexion") val ultimaConexion: String?
) {
    val nombreCompleto: String
        get() = "$nombre $apellidoPaterno $apellidoMaterno".trim()

    val iniciales: String
        get() {
            val parts = nombreCompleto.split(" ").filter { it.isNotBlank() }
            return when {
                parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
                parts.size == 1 -> parts[0].take(2).uppercase()
                else -> "??"
            }
        }
}
