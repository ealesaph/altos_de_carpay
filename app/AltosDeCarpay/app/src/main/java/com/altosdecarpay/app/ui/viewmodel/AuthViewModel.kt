package com.altosdecarpay.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altosdecarpay.app.data.SessionState
import com.altosdecarpay.app.data.model.LoginRequest
import com.altosdecarpay.app.data.model.LogoutRequest
import com.altosdecarpay.app.data.model.PerfilResponse
import com.altosdecarpay.app.data.remote.ApiClient
import com.altosdecarpay.app.data.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.getApiService(application)
    private val tokenManager = ApiClient.getTokenManager(application)

    private val _loginState = MutableStateFlow<NetworkResult<String>?>(null)
    val loginState: StateFlow<NetworkResult<String>?> = _loginState

    private val _perfil = MutableStateFlow<NetworkResult<PerfilResponse>?>(null)
    val perfil: StateFlow<NetworkResult<PerfilResponse>?> = _perfil

    val isLoggedIn: Boolean get() = tokenManager.isLoggedIn

    fun login(identificador: String, password: String) {
        viewModelScope.launch {
            _loginState.value = NetworkResult.Loading
            try {
                val response = api.login(LoginRequest(correo = identificador, password = password))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    tokenManager.saveTokens(body.access, body.refresh)
                    tokenManager.userInfo = body.usuario
                    SessionState.userInfo = body.usuario
                    _loginState.value = NetworkResult.Success(body.usuario.rol)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    _loginState.value = NetworkResult.Error(parseError(errorBody), response.code())
                }
            } catch (e: Exception) {
                _loginState.value = NetworkResult.Error(
                    e.message ?: "Error de conexión. Verifica tu red."
                )
            }
        }
    }

    fun loadPerfil() {
        viewModelScope.launch {
            _perfil.value = NetworkResult.Loading
            try {
                val response = api.getPerfil()
                if (response.isSuccessful) {
                    val body = response.body()!!
                    SessionState.currentUser = body
                    _perfil.value = NetworkResult.Success(body)
                } else {
                    _perfil.value = NetworkResult.Error("No se pudo cargar el perfil")
                }
            } catch (e: Exception) {
                _perfil.value = NetworkResult.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val refresh = tokenManager.refreshToken
                if (refresh != null) {
                    api.logout(LogoutRequest(refresh))
                }
            } catch (_: Exception) {
                // Ignorar errores de logout
            } finally {
                tokenManager.clearSession()
                SessionState.clear()
                onDone()
            }
        }
    }

    private val _registroState = MutableStateFlow<NetworkResult<PerfilResponse>?>(null)
    val registroState: StateFlow<NetworkResult<PerfilResponse>?> = _registroState

    fun registro(request: com.altosdecarpay.app.data.model.RegistroRequest) {
        viewModelScope.launch {
            _registroState.value = NetworkResult.Loading
            try {
                val response = api.registro(request)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _registroState.value = NetworkResult.Success(body)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error en el registro"
                    _registroState.value = NetworkResult.Error(parseError(errorBody), response.code())
                }
            } catch (e: Exception) {
                _registroState.value = NetworkResult.Error(e.message ?: "Error de conexión al registrarse.")
            }
        }
    }

    fun resetRegistroState() {
        _registroState.value = null
    }

    fun resetLoginState() {
        _loginState.value = null
    }

    private fun parseError(json: String): String {
        return try {
            val jsonObject = com.google.gson.JsonParser.parseString(json).asJsonObject
            val firstEntry = jsonObject.entrySet().firstOrNull() ?: return "Error en la solicitud."
            val element = firstEntry.value
            if (element.isJsonArray && element.asJsonArray.size() > 0) {
                element.asJsonArray[0].asString
            } else if (element.isJsonPrimitive) {
                element.asString
            } else {
                "Error en el campo ${firstEntry.key}"
            }
        } catch (_: Exception) {
            "Credenciales incorrectas o error en el servidor."
        }
    }
}
