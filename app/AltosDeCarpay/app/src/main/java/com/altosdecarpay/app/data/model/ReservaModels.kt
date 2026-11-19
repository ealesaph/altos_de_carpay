package com.altosdecarpay.app.data.model

import com.google.gson.annotations.SerializedName

data class SectorResponse(
    @SerializedName("id_sector") val idSector: Int,
    val nombre: String,
    val descripcion: String,
    val fotografias: List<String>,
    @SerializedName("valor_hora") val valorHora: String,
    val estado: String
)

data class ServicioResponse(
    @SerializedName("id_servicio") val idServicio: Int,
    val nombre: String,
    val descripcion: String,
    val valor: String,
    val estado: String
)

data class SectorReservadoResponse(
    @SerializedName("id_sector") val idSector: Int,
    val nombre: String,
    val horas: Int,
    @SerializedName("valor_hora") val valorHora: String,
    val subtotal: String
)

data class ServicioReservadoResponse(
    @SerializedName("id_servicio") val idServicio: Int,
    val nombre: String,
    val cantidad: Int,
    @SerializedName("valor_unitario") val valorUnitario: String,
    val subtotal: String
)

data class PagoResponse(
    @SerializedName("id_pago") val idPago: Int,
    val monto: String,
    @SerializedName("tipo_pago") val tipoPago: String,
    @SerializedName("metodo_pago") val metodoPago: String,
    @SerializedName("transaccion_id") val transaccionId: String?,
    @SerializedName("estado_pago") val estadoPago: String,
    @SerializedName("fecha_pago") val fechaPago: String
)

data class ReservaResponse(
    @SerializedName("id_reserva") val idReserva: Int,
    val usuario: Int,
    val cliente: String,
    @SerializedName("correo_cliente") val correoCliente: String,
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    @SerializedName("hora_fin") val horaFin: String?,
    @SerializedName("horas_reservadas") val horasReservadas: Int,
    val estado: String,
    @SerializedName("monto_total") val montoTotal: String,
    @SerializedName("monto_anticipo") val montoAnticipo: String?,
    @SerializedName("monto_pagado") val montoPagado: String?,
    @SerializedName("saldo_pendiente") val saldoPendiente: String?,
    @SerializedName("plazo_anticipo_vence") val plazoAnticipoVence: String?,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    @SerializedName("token_qr") val tokenQr: String?,
    val sectores: List<SectorReservadoResponse>,
    val servicios: List<ServicioReservadoResponse>,
    val pagos: List<PagoResponse>
) {
    val idFormateado: String get() = "RES-%04d".format(idReserva)

    val sectorPrincipal: String
        get() = sectores.firstOrNull()?.nombre ?: "Sin sector"
}

data class PaginatedResponse<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)

data class ReservaCrearRequest(
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    val sectores: List<SectorEntradaRequest>,
    val servicios: List<ServicioEntradaRequest> = emptyList(),
    @SerializedName("acepta_condiciones") val aceptaCondiciones: Boolean = true,
    val cliente: Int? = null
)

data class SectorEntradaRequest(
    val sector: Int,
    val horas: Int
)

data class ServicioEntradaRequest(
    val servicio: Int,
    val cantidad: Int
)

data class PagoCrearRequest(
    @SerializedName("tipo_pago") val tipoPago: String,
    @SerializedName("metodo_pago") val metodoPago: String = "Webpay",
    @SerializedName("simular_rechazo") val simularRechazo: Boolean = false
)

data class CambioEstadoRequest(
    val estado: String
)
