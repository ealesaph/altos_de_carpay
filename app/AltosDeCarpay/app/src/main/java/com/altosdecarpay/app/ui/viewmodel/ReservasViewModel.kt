package com.altosdecarpay.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altosdecarpay.app.data.model.CambioEstadoRequest
import com.altosdecarpay.app.data.model.ReservaResponse
import com.altosdecarpay.app.data.remote.ApiClient
import com.altosdecarpay.app.data.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReservasViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.getApiService(application)

    private val _reservas = MutableStateFlow<NetworkResult<List<ReservaResponse>>>(NetworkResult.Loading)
    val reservas: StateFlow<NetworkResult<List<ReservaResponse>>> = _reservas

    private val _reservaDetalle = MutableStateFlow<NetworkResult<ReservaResponse?>>(NetworkResult.Success(null))
    val reservaDetalle: StateFlow<NetworkResult<ReservaResponse?>> = _reservaDetalle

    fun loadReservas(estado: String? = null) {
        viewModelScope.launch {
            _reservas.value = NetworkResult.Loading
            try {
                val response = api.getReservas(estado = estado)
                if (response.isSuccessful) {
                    val list = response.body()?.results ?: emptyList()
                    _reservas.value = NetworkResult.Success(list)
                } else {
                    _reservas.value = NetworkResult.Error("No se pudieron cargar las reservas")
                }
            } catch (e: Exception) {
                _reservas.value = NetworkResult.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun loadReservaDetalle(id: Int) {
        viewModelScope.launch {
            _reservaDetalle.value = NetworkResult.Loading
            try {
                val response = api.getReserva(id)
                if (response.isSuccessful) {
                    _reservaDetalle.value = NetworkResult.Success(response.body())
                } else {
                    _reservaDetalle.value = NetworkResult.Error("No se pudo cargar la reserva")
                }
            } catch (e: Exception) {
                _reservaDetalle.value = NetworkResult.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun cambiarEstado(id: Int, estado: String) {
        viewModelScope.launch {
            try {
                val response = api.cambiarEstadoReserva(id, CambioEstadoRequest(estado))
                if (response.isSuccessful) {
                    _reservaDetalle.value = NetworkResult.Success(response.body())
                    loadReservas()
                }
            } catch (_: Exception) { }
        }
    }

    fun cancelarReserva(id: Int) {
        viewModelScope.launch {
            try {
                val response = api.cancelarReserva(id)
                if (response.isSuccessful) {
                    _reservaDetalle.value = NetworkResult.Success(response.body())
                    loadReservas()
                }
            } catch (_: Exception) { }
        }
    }
}
