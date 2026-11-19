package com.altosdecarpay.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altosdecarpay.app.data.model.CambioEstadoRequest
import com.altosdecarpay.app.data.model.PagoCrearRequest
import com.altosdecarpay.app.data.model.ReservaResponse
import com.altosdecarpay.app.data.remote.ApiClient
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.utils.QrCodeGenerator
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
                    _reservaDetalle.value = NetworkResult.Error("No se pudo cargar la reserva #$id")
                }
            } catch (e: Exception) {
                _reservaDetalle.value = NetworkResult.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun buscarReservaPorCodigo(
        codigoRaw: String,
        onFound: (ReservaResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val numericId = QrCodeGenerator.parseReservaId(codigoRaw)
                if (numericId != null) {
                    val res = api.getReserva(numericId)
                    if (res.isSuccessful && res.body() != null) {
                        onFound(res.body()!!)
                        return@launch
                    }
                }
                // Si no era un ID numérico o no se encontró por ID, buscar por token_qr en las reservas
                val listRes = api.getReservas()
                if (listRes.isSuccessful) {
                    val found = listRes.body()?.results?.firstOrNull {
                        it.tokenQr == codigoRaw.trim() || it.idFormateado.equals(codigoRaw.trim(), ignoreCase = true)
                    }
                    if (found != null) {
                        onFound(found)
                        return@launch
                    }
                }
                onError("No se encontró ninguna reserva para el código «$codigoRaw».")
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión al buscar reserva.")
            }
        }
    }

    fun cambiarEstado(
        id: Int,
        estado: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val response = api.cambiarEstadoReserva(id, CambioEstadoRequest(estado))
                if (response.isSuccessful) {
                    _reservaDetalle.value = NetworkResult.Success(response.body())
                    loadReservas()
                    onSuccess()
                } else {
                    onError("No se pudo cambiar el estado a $estado")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión")
            }
        }
    }

    fun cancelarReserva(id: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = api.cancelarReserva(id)
                if (response.isSuccessful) {
                    _reservaDetalle.value = NetworkResult.Success(response.body())
                    loadReservas()
                    onSuccess()
                }
            } catch (_: Exception) { }
        }
    }

    fun registrarPago(id: Int, tipoPago: String = "Anticipo", onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = api.crearPago(id, PagoCrearRequest(tipoPago = tipoPago))
                if (response.isSuccessful) {
                    loadReservaDetalle(id)
                    loadReservas()
                    onSuccess()
                }
            } catch (_: Exception) { }
        }
    }
}
