package com.altosdecarpay.app.data.model

import com.google.gson.annotations.SerializedName

data class ConsultaResponse(
    @SerializedName("id_consulta") val idConsulta: Int,
    val cliente: Int,
    @SerializedName("cliente_nombre") val clienteNombre: String?,
    val asunto: String,
    val mensaje: String,
    @SerializedName("fecha_envio") val fechaEnvio: String,
    val respuesta: String?,
    @SerializedName("fecha_respuesta") val fechaRespuesta: String?,
    val estado: String
) {
    val idFormateado: String get() = "OBS-%04d".format(idConsulta)
}

data class ConsultaCrearRequest(
    val asunto: String,
    val mensaje: String
)

data class RespuestaRequest(
    val respuesta: String
)
