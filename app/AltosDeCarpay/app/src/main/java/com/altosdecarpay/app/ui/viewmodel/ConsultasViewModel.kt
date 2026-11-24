package com.altosdecarpay.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altosdecarpay.app.data.model.ConsultaCrearRequest
import com.altosdecarpay.app.data.model.ConsultaResponse
import com.altosdecarpay.app.data.model.RespuestaRequest
import com.altosdecarpay.app.data.remote.ApiClient
import com.altosdecarpay.app.data.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConsultasViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.getApiService(application)

    private val _consultas = MutableStateFlow<NetworkResult<List<ConsultaResponse>>>(NetworkResult.Loading)
    val consultas: StateFlow<NetworkResult<List<ConsultaResponse>>> = _consultas

    private val _crearResult = MutableStateFlow<NetworkResult<ConsultaResponse?>?>(null)
    val crearResult: StateFlow<NetworkResult<ConsultaResponse?>?> = _crearResult

    fun loadConsultas(estado: String? = null) {
        viewModelScope.launch {
            _consultas.value = NetworkResult.Loading
            try {
                val response = api.getConsultas(estado = estado)
                if (response.isSuccessful) {
                    val list = response.body()?.results ?: emptyList()
                    _consultas.value = NetworkResult.Success(list)
                } else {
                    _consultas.value = NetworkResult.Error("No se pudieron cargar las consultas")
                }
            } catch (e: Exception) {
                _consultas.value = NetworkResult.Error(com.altosdecarpay.app.ui.utils.Formato.mensajeConexion(e))
            }
        }
    }

    fun crearConsulta(asunto: String, mensaje: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _crearResult.value = NetworkResult.Loading
            try {
                val response = api.crearConsulta(ConsultaCrearRequest(asunto, mensaje))
                if (response.isSuccessful) {
                    _crearResult.value = NetworkResult.Success(response.body())
                    loadConsultas()
                    onDone(true)
                } else {
                    _crearResult.value = NetworkResult.Error(
                        com.altosdecarpay.app.ui.utils.Formato.mensajeError(response.errorBody()?.string(), "No se pudo enviar la consulta.")
                    )
                    onDone(false)
                }
            } catch (e: Exception) {
                _crearResult.value = NetworkResult.Error(com.altosdecarpay.app.ui.utils.Formato.mensajeConexion(e))
                onDone(false)
            }
        }
    }

    fun responderConsulta(id: Int, respuesta: String, onDone: (String?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = api.responderConsulta(id, RespuestaRequest(respuesta))
                if (response.isSuccessful) {
                    loadConsultas()
                    onDone(null)
                } else {
                    onDone(com.altosdecarpay.app.ui.utils.Formato.mensajeError(response.errorBody()?.string(), "No se pudo enviar la respuesta."))
                }
            } catch (e: Exception) {
                onDone(com.altosdecarpay.app.ui.utils.Formato.mensajeConexion(e))
            }
        }
    }

    fun resetCrearResult() {
        _crearResult.value = null
    }
}
