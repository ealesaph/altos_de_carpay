package com.altosdecarpay.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altosdecarpay.app.data.model.BitacoraIncidenciaRequest
import com.altosdecarpay.app.data.model.BitacoraIncidenciaResponse
import com.altosdecarpay.app.data.model.LogModificacionResponse
import com.altosdecarpay.app.data.model.SectorCrearRequest
import com.altosdecarpay.app.data.model.SectorResponse
import com.altosdecarpay.app.data.model.ServicioCrearRequest
import com.altosdecarpay.app.data.model.ServicioResponse
import com.altosdecarpay.app.data.remote.ApiClient
import com.altosdecarpay.app.data.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServiciosViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient.getApiService(application)

    private val _sectores = MutableStateFlow<NetworkResult<List<SectorResponse>>>(NetworkResult.Loading)
    val sectores: StateFlow<NetworkResult<List<SectorResponse>>> = _sectores

    private val _servicios = MutableStateFlow<NetworkResult<List<ServicioResponse>>>(NetworkResult.Loading)
    val servicios: StateFlow<NetworkResult<List<ServicioResponse>>> = _servicios

    private val _logs = MutableStateFlow<NetworkResult<List<LogModificacionResponse>>>(NetworkResult.Loading)
    val logs: StateFlow<NetworkResult<List<LogModificacionResponse>>> = _logs

    private val _incidencias = MutableStateFlow<NetworkResult<List<BitacoraIncidenciaResponse>>>(NetworkResult.Loading)
    val incidencias: StateFlow<NetworkResult<List<BitacoraIncidenciaResponse>>> = _incidencias

    fun loadSectores() {
        viewModelScope.launch {
            _sectores.value = NetworkResult.Loading
            try {
                val response = api.getSectores()
                if (response.isSuccessful) {
                    _sectores.value = NetworkResult.Success(response.body()?.results ?: emptyList())
                } else {
                    _sectores.value = NetworkResult.Error("No se pudieron cargar los sectores")
                }
            } catch (e: Exception) {
                _sectores.value = NetworkResult.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun actualizarEstadoSector(id: Int, nuevoEstado: String) {
        viewModelScope.launch {
            try {
                val response = api.actualizarSector(id, mapOf("estado" to nuevoEstado))
                if (response.isSuccessful) {
                    loadSectores()
                }
            } catch (_: Exception) { }
        }
    }

    fun actualizarSector(
        id: Int,
        nombre: String,
        descripcion: String,
        valorHora: String,
        estado: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val cleanValor = valorHora.replace("$", "").replace(".", "").replace(",", "").trim()
                val response = api.actualizarSector(
                    id,
                    mapOf(
                        "nombre" to nombre.trim(),
                        "descripcion" to descripcion.trim(),
                        "valor_hora" to cleanValor.ifBlank { "0" },
                        "estado" to estado
                    )
                )
                if (response.isSuccessful) {
                    loadSectores()
                    onResult(true, null)
                } else {
                    val err = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: "No se pudo actualizar el espacio (HTTP ${response.code()})"
                    onResult(false, err)
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Error de conexión")
            }
        }
    }

    fun crearSector(
        nombre: String,
        descripcion: String,
        valorHora: String,
        estado: String = "Disponible",
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val cleanValor = valorHora.replace("$", "").replace(".", "").replace(",", "").trim()
                val response = api.crearSector(
                    SectorCrearRequest(
                        nombre = nombre.trim(),
                        descripcion = descripcion.trim(),
                        valorHora = cleanValor.ifBlank { "0" },
                        estado = estado
                    )
                )
                if (response.isSuccessful) {
                    loadSectores()
                    onResult(true, null)
                } else {
                    val err = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: "No se pudo crear el espacio (HTTP ${response.code()})"
                    onResult(false, err)
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Error de conexión")
            }
        }
    }

    fun eliminarSector(id: Int, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val response = api.eliminarSector(id)
                if (response.isSuccessful) {
                    loadSectores()
                    onResult(true, null)
                } else {
                    val msg = if (response.code() == 409) {
                        "No se puede eliminar este espacio porque tiene reservas o incidencias asociadas."
                    } else {
                        "No se pudo eliminar el espacio (HTTP ${response.code()})"
                    }
                    onResult(false, msg)
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Error de conexión")
            }
        }
    }

    fun loadServicios() {
        viewModelScope.launch {
            _servicios.value = NetworkResult.Loading
            try {
                val response = api.getServicios()
                if (response.isSuccessful) {
                    _servicios.value = NetworkResult.Success(response.body()?.results ?: emptyList())
                } else {
                    _servicios.value = NetworkResult.Error("No se pudieron cargar los servicios")
                }
            } catch (e: Exception) {
                _servicios.value = NetworkResult.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun crearServicio(nombre: String, descripcion: String, valor: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val cleanValor = valor.replace("$", "").replace(".", "").replace(",", "").trim()
                val response = api.crearServicio(
                    ServicioCrearRequest(
                        nombre = nombre.trim(),
                        descripcion = descripcion.trim(),
                        valor = cleanValor.ifBlank { "0" },
                        estado = "Activo"
                    )
                )
                if (response.isSuccessful) {
                    loadServicios()
                    onDone()
                }
            } catch (_: Exception) { }
        }
    }

    fun loadLogs(tabla: String? = null, idRegistro: Int? = null) {
        viewModelScope.launch {
            _logs.value = NetworkResult.Loading
            try {
                val response = api.getAuditoriaLogs(tabla = tabla, idRegistro = idRegistro)
                if (response.isSuccessful) {
                    _logs.value = NetworkResult.Success(response.body()?.results ?: emptyList())
                } else {
                    _logs.value = NetworkResult.Error("No se pudo cargar la bitácora")
                }
            } catch (e: Exception) {
                _logs.value = NetworkResult.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun loadIncidencias(reservaId: Int? = null) {
        viewModelScope.launch {
            _incidencias.value = NetworkResult.Loading
            try {
                val response = api.getIncidencias(reservaId = reservaId)
                if (response.isSuccessful) {
                    _incidencias.value = NetworkResult.Success(response.body()?.results ?: emptyList())
                } else {
                    _incidencias.value = NetworkResult.Error("No se pudieron cargar las incidencias")
                }
            } catch (e: Exception) {
                _incidencias.value = NetworkResult.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun crearIncidencia(
        reservaId: Int,
        titulo: String,
        descripcion: String,
        sectorId: Int? = null,
        onSuccess: (BitacoraIncidenciaResponse?) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val response = api.crearIncidencia(
                    BitacoraIncidenciaRequest(
                        reserva = reservaId,
                        titulo = titulo.trim(),
                        descripcion = descripcion.trim(),
                        sector = sectorId
                    )
                )
                if (response.isSuccessful) {
                    loadIncidencias(reservaId)
                    onSuccess(response.body())
                } else {
                    onError("No se pudo guardar (la reserva podría estar finalizada en solo lectura).")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión")
            }
        }
    }
}
