package com.altosdecarpay.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altosdecarpay.app.data.model.CambioEstadoRequest
import com.altosdecarpay.app.data.model.CancelacionRequest
import com.altosdecarpay.app.data.model.PagoCrearRequest
import com.altosdecarpay.app.data.model.PagoResponse
import com.altosdecarpay.app.data.model.ReservaCrearRequest
import com.altosdecarpay.app.data.model.ReservaResponse
import com.altosdecarpay.app.data.model.SimulacionCancelacionResponse
import com.altosdecarpay.app.data.model.ValidacionAccesoRequest
import com.altosdecarpay.app.data.model.ValidacionAccesoResponse
import com.altosdecarpay.app.data.remote.ApiClient
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.utils.Formato
import com.altosdecarpay.app.ui.utils.QrCodeGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class ReservasViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.getApiService(application)

    private val _reservas = MutableStateFlow<NetworkResult<List<ReservaResponse>>>(NetworkResult.Loading)
    val reservas: StateFlow<NetworkResult<List<ReservaResponse>>> = _reservas

    private val _reservaDetalle = MutableStateFlow<NetworkResult<ReservaResponse?>>(NetworkResult.Loading)
    val reservaDetalle: StateFlow<NetworkResult<ReservaResponse?>> = _reservaDetalle

    private fun <T> error(response: Response<T>, porDefecto: String): String =
        Formato.mensajeError(response.errorBody()?.string(), porDefecto)

    /** vista = "proximas" devuelve solo las vigentes desde hoy, en orden ascendente (igual que la web). */
    fun loadReservas(estado: String? = null, vista: String? = null) {
        viewModelScope.launch {
            _reservas.value = NetworkResult.Loading
            try {
                val response = api.getReservas(estado = estado, vista = vista)
                if (response.isSuccessful) {
                    _reservas.value = NetworkResult.Success(response.body()?.results ?: emptyList())
                } else {
                    _reservas.value = NetworkResult.Error(error(response, "No se pudieron cargar las reservas."), response.code())
                }
            } catch (e: Exception) {
                _reservas.value = NetworkResult.Error(Formato.mensajeConexion(e))
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
                } else if (response.code() == 404) {
                    _reservaDetalle.value = NetworkResult.Error("La reserva N° $id no existe o no tienes acceso a ella.", 404)
                } else {
                    _reservaDetalle.value = NetworkResult.Error(error(response, "No se pudo cargar la reserva N° $id."), response.code())
                }
            } catch (e: Exception) {
                _reservaDetalle.value = NetworkResult.Error(Formato.mensajeConexion(e))
            }
        }
    }

    /** Busca la reserva por número, código RES-0000 o token QR, sin registrar ningún movimiento. */
    fun buscarReservaPorCodigo(
        codigoRaw: String,
        onFound: (ReservaResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val codigo = codigoRaw.trim()
                val numericId = QrCodeGenerator.parseReservaId(codigo)
                if (numericId != null) {
                    val res = api.getReserva(numericId)
                    if (res.isSuccessful && res.body() != null) {
                        onFound(res.body()!!)
                        return@launch
                    }
                }
                val listRes = api.getReservas()
                if (listRes.isSuccessful) {
                    val found = listRes.body()?.results?.firstOrNull {
                        it.tokenQr == codigo || it.idFormateado.equals(codigo, ignoreCase = true)
                    }
                    if (found != null) {
                        onFound(found)
                        return@launch
                    }
                }
                onError("No se encontró ninguna reserva para el código «$codigo».")
            } catch (e: Exception) {
                onError(Formato.mensajeConexion(e))
            }
        }
    }

    /** Crea una reserva con uno o varios espacios (misma fecha y hora de inicio). */
    fun crearReserva(
        request: ReservaCrearRequest,
        onSuccess: (ReservaResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = api.crearReserva(request)
                val creada = response.body()
                if (response.isSuccessful && creada != null) {
                    loadReservas()
                    onSuccess(creada)
                } else {
                    onError(error(response, "No se pudo crear la reserva."))
                }
            } catch (e: Exception) {
                onError(Formato.mensajeConexion(e))
            }
        }
    }

    /**
     * Pago por Webpay (simulador del backend, el mismo que usa la web). Devuelve el pago con su estado:
     * «Aprobado» o «Rechazado». Un rechazo no es un error de conexión: la pantalla lo muestra y permite reintentar.
     */
    fun pagarWebpay(
        id: Int,
        tipoPago: String,
        simularRechazo: Boolean = false,
        onResult: (PagoResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = api.crearPago(id, PagoCrearRequest(tipoPago = tipoPago, metodoPago = "Webpay", simularRechazo = simularRechazo))
                val pago = response.body()
                if (response.isSuccessful && pago != null) {
                    alTerminar(id, null)
                    onResult(pago)
                } else {
                    onError(error(response, "No se pudo procesar el pago."))
                }
            } catch (e: Exception) {
                onError(Formato.mensajeConexion(e))
            }
        }
    }

    private fun alTerminar(id: Int, actualizada: ReservaResponse?) {
        if (actualizada != null) _reservaDetalle.value = NetworkResult.Success(actualizada) else loadReservaDetalle(id)
        loadReservas()
    }

    fun cambiarEstado(id: Int, estado: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = api.cambiarEstadoReserva(id, CambioEstadoRequest(estado))
                if (response.isSuccessful) {
                    alTerminar(id, response.body())
                    onSuccess()
                } else {
                    onError(error(response, "No se pudo cambiar el estado a «$estado»."))
                }
            } catch (e: Exception) {
                onError(Formato.mensajeConexion(e))
            }
        }
    }

    fun simularCancelacion(id: Int, origen: String?, onResult: (SimulacionCancelacionResponse) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.simularCancelacion(id, origen)
                if (response.isSuccessful && response.body() != null) onResult(response.body()!!)
                else onError(error(response, "No se pudo calcular la devolución."))
            } catch (e: Exception) {
                onError(Formato.mensajeConexion(e))
            }
        }
    }

    fun cancelarReserva(id: Int, origen: String? = null, onSuccess: (ReservaResponse?) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = api.cancelarReserva(id, CancelacionRequest(origen))
                if (response.isSuccessful) {
                    alTerminar(id, response.body())
                    onSuccess(response.body())
                } else {
                    onError(error(response, "No se pudo cancelar la reserva."))
                }
            } catch (e: Exception) {
                onError(Formato.mensajeConexion(e))
            }
        }
    }

    fun registrarDevolucion(id: Int, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = api.registrarDevolucion(id)
                if (response.isSuccessful) {
                    alTerminar(id, response.body())
                    onSuccess()
                } else {
                    onError(error(response, "No se pudo registrar la devolución."))
                }
            } catch (e: Exception) {
                onError(Formato.mensajeConexion(e))
            }
        }
    }

    /** Pago manual del administrador (transferencia o efectivo), igual que en el panel web. */
    fun registrarPago(
        id: Int,
        tipoPago: String,
        metodoPago: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val response = api.crearPago(id, PagoCrearRequest(tipoPago = tipoPago, metodoPago = metodoPago))
                if (response.isSuccessful) {
                    alTerminar(id, null)
                    onSuccess()
                } else {
                    onError(error(response, "No se pudo registrar el pago."))
                }
            } catch (e: Exception) {
                onError(Formato.mensajeConexion(e))
            }
        }
    }

    fun validarAcceso(
        id: Int,
        tipoRegistro: String,
        onSuccess: (ValidacionAccesoResponse) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val response = api.validarAcceso(id, ValidacionAccesoRequest(tipoRegistro = tipoRegistro))
                if (response.isSuccessful && response.body() != null) {
                    alTerminar(id, null)
                    onSuccess(response.body()!!)
                } else {
                    onError(error(response, "No se pudo registrar el acceso."))
                }
            } catch (e: Exception) {
                onError(Formato.mensajeConexion(e))
            }
        }
    }
}
