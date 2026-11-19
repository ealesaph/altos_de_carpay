package com.altosdecarpay.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altosdecarpay.app.data.model.SectorResponse
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

    fun loadSectores() {
        viewModelScope.launch {
            _sectores.value = NetworkResult.Loading
            try {
                val response = api.getSectores()
                if (response.isSuccessful) {
                    _sectores.value = NetworkResult.Success(response.body() ?: emptyList())
                } else {
                    _sectores.value = NetworkResult.Error("No se pudieron cargar los sectores")
                }
            } catch (e: Exception) {
                _sectores.value = NetworkResult.Error(e.message ?: "Error de conexión")
            }
        }
    }

    fun loadServicios() {
        viewModelScope.launch {
            _servicios.value = NetworkResult.Loading
            try {
                val response = api.getServicios()
                if (response.isSuccessful) {
                    _servicios.value = NetworkResult.Success(response.body() ?: emptyList())
                } else {
                    _servicios.value = NetworkResult.Error("No se pudieron cargar los servicios")
                }
            } catch (e: Exception) {
                _servicios.value = NetworkResult.Error(e.message ?: "Error de conexión")
            }
        }
    }
}
