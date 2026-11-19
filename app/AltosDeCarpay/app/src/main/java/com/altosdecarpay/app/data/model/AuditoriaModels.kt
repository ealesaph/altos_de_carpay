package com.altosdecarpay.app.data.model

import com.google.gson.annotations.SerializedName

data class LogModificacionResponse(
    @SerializedName("id_log") val idLog: Int,
    val usuario: Int?,
    @SerializedName("usuario_correo") val usuarioCorreo: String?,
    val fecha: String,
    @SerializedName("tabla_modificada") val tablaModificada: String,
    @SerializedName("id_registro") val idRegistro: Int,
    @SerializedName("valor_previo") val valorPrevio: Any?,
    @SerializedName("valor_nuevo") val valorNuevo: Any?
)

data class ServicioCrearRequest(
    val nombre: String,
    val descripcion: String = "",
    val valor: String,
    val estado: String = "Activo"
)
