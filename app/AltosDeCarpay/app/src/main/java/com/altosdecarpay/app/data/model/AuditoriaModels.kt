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

data class ValidacionAccesoRequest(
    @SerializedName("tipo_registro") val tipoRegistro: String
)

data class ValidacionAccesoResponse(
    @SerializedName("id_validacion") val idValidacion: Int,
    val reserva: Int,
    @SerializedName("token_qr") val tokenQr: String,
    @SerializedName("tipo_registro") val tipoRegistro: String,
    @SerializedName("fecha_hora") val fechaHora: String,
    @SerializedName("validado_por") val validadoPor: Int,
    @SerializedName("validado_por_correo") val validadoPorCorreo: String?,
    @SerializedName("minutos_excedidos") val minutosExcedidos: Int,
    @SerializedName("cargo_sobretiempo_propuesto") val cargoSobretiempoPropuesto: String
)

data class BitacoraIncidenciaRequest(
    val reserva: Int,
    val titulo: String,
    val descripcion: String
)

data class BitacoraIncidenciaResponse(
    @SerializedName("id_incidencia") val idIncidencia: Int,
    val reserva: Int,
    @SerializedName("reserva_estado") val reservaEstado: String?,
    val autor: Int,
    @SerializedName("autor_nombre") val autorNombre: String?,
    val titulo: String,
    val descripcion: String,
    @SerializedName("fecha_creacion") val fechaCreacion: String
) {
    val idFormateado: String get() = "INC-%04d".format(idIncidencia)
}
