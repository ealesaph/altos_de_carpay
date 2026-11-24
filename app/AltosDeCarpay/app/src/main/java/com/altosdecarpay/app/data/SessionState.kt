package com.altosdecarpay.app.data

import com.altosdecarpay.app.data.model.PerfilResponse
import com.altosdecarpay.app.data.model.UsuarioInfo

/**
 * Estado de sesión global en memoria. Se llena al hacer login y se usa
 * para acceder rápidamente al perfil del usuario actual.
 */
object SessionState {
    /** Se activa cuando el servidor rechaza la sesión y no se pudo renovar: la app vuelve al inicio de sesión. */
    val sesionExpirada = kotlinx.coroutines.flow.MutableStateFlow(false)

    var currentUser: PerfilResponse? = null
    var userInfo: UsuarioInfo? = null

    val isAdmin: Boolean get() = userInfo?.rol == "Admin"
    val isCliente: Boolean get() = userInfo?.rol == "Cliente"
    val userName: String get() = currentUser?.nombre ?: userInfo?.nombre ?: ""

    fun clear() {
        currentUser = null
        userInfo = null
    }
}
