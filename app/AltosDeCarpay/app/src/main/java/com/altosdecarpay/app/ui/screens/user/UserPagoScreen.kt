package com.altosdecarpay.app.ui.screens.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.model.PagoResponse
import com.altosdecarpay.app.data.model.ReservaResponse
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.components.DialogoConfirmacion
import com.altosdecarpay.app.ui.components.MensajeError
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.utils.Formato
import com.altosdecarpay.app.ui.utils.QrCodeGenerator
import com.altosdecarpay.app.ui.viewmodel.ReservasViewModel

/** Qué se puede pagar según el estado de la reserva (mismas reglas que el backend y la web). */
internal data class OpcionPago(val tipo: String, val titulo: String, val detalle: String, val monto: String)

internal fun opcionesDePago(r: ReservaResponse): List<OpcionPago> {
    val pagado = r.montoPagado?.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
    return when {
        r.estado == "Pendiente" && pagado.signum() == 0 -> listOf(
            OpcionPago("Anticipo", "Anticipo (30%)", "El saldo de ${Formato.clp((r.montoTotal.toBigDecimal() - (r.montoAnticipo?.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO)).toPlainString())} se paga hasta 24 horas antes del evento.", r.montoAnticipo ?: "0"),
            OpcionPago("Total", "Pago total", "Pagas el arriendo completo de una vez.", r.montoTotal)
        )
        r.estado in setOf("Pendiente", "Confirmado", "En Proceso") && r.saldo.signum() > 0 -> listOf(
            OpcionPago("Saldo", "Saldo pendiente", "Completa el pago de tu reserva.", r.saldo.toPlainString())
        )
        else -> emptyList()
    }
}

/**
 * Pago de una reserva con el simulador de Webpay del backend (el mismo de la web). No se usan datos
 * financieros reales: el servidor aprueba o rechaza el pago y, al aprobar el anticipo, confirma la reserva.
 */
@Composable
fun UserPagoScreen(
    reservaId: String,
    reservasViewModel: ReservasViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onVerReserva: (String) -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    val numericId = remember(reservaId) { QrCodeGenerator.parseReservaId(reservaId) ?: 0 }
    val detalleState by reservasViewModel.reservaDetalle.collectAsState()
    LaunchedEffect(numericId) { reservasViewModel.loadReservaDetalle(numericId) }

    var tipoElegido by remember { mutableStateOf<String?>(null) }
    var tarjeta by remember { mutableStateOf("Débito") }
    var confirmar by remember { mutableStateOf<Boolean?>(null) }   // true = aprobar, false = simular rechazo
    var procesando by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<PagoResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val depurable = (LocalContext.current.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

    Scaffold(
        topBar = { UserTopBar() },
        bottomBar = { UserBottomBar(selectedTab = UserTab.RESERVAS, onTabSelected = onTabSelected) },
        containerColor = CarpayBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserScreenHeader(title = "Pago", subtitle = "Webpay de prueba · sin datos financieros reales", showBackButton = true, onBackClick = onBackClick)

            val estado = detalleState
            if (estado is NetworkResult.Error) {
                MensajeError(estado.message) { reservasViewModel.loadReservaDetalle(numericId) }
                return@Column
            }
            val reserva = (estado as? NetworkResult.Success)?.data?.takeIf { it.idReserva == numericId }
            if (reserva == null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
                return@Column
            }

            // Resultado de un pago aprobado
            val pagoAprobado = resultado?.takeIf { it.estadoPago == "Aprobado" }
            if (pagoAprobado != null) {
                Surface(shape = RoundedCornerShape(16.dp), color = CarpayGreenLight, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = CarpayPrimaryGreen, modifier = Modifier.size(44.dp))
                        Text(
                            if (reserva.estado == "Confirmado") "¡Reserva confirmada!" else "Pago registrado",
                            fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CarpayPrimaryGreen
                        )
                        Text(
                            "${pagoAprobado.tipoPago} de ${Formato.clp(pagoAprobado.monto)} aprobado · ${reserva.idFormateado}",
                            fontSize = 14.sp, color = CarpayTextPrimary
                        )
                        pagoAprobado.transaccionId?.let { Text("Transacción $it", fontSize = 12.sp, color = CarpayTextSecondary) }
                        Text("Te enviamos el detalle por correo.", fontSize = 12.sp, color = CarpayTextSecondary)
                    }
                }
                Button(
                    onClick = { onVerReserva(reserva.idFormateado) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Ver mi reserva y QR", fontWeight = FontWeight.SemiBold) }
                return@Column
            }

            ResumenReserva(reserva)

            val opciones = opcionesDePago(reserva)
            if (opciones.isEmpty()) {
                Surface(shape = RoundedCornerShape(12.dp), color = CarpayGreenLight, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        when (reserva.estado) {
                            "Cancelado", "No se presentó", "Finalizado" -> "Esta reserva está «${Formato.estado(reserva.estado)}» y no admite pagos."
                            else -> "Esta reserva no tiene pagos pendientes."
                        },
                        fontSize = 13.sp, color = CarpayTextPrimary, modifier = Modifier.padding(14.dp)
                    )
                }
                return@Column
            }
            val elegida = opciones.firstOrNull { it.tipo == tipoElegido } ?: opciones.first()

            Text("¿Cuánto quieres pagar?", fontWeight = FontWeight.Bold, color = CarpayTextPrimary)
            opciones.forEach { opcion ->
                val marcada = opcion.tipo == elegida.tipo
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (marcada) CarpayGreenLight else CarpayWhite,
                    border = BorderStroke(if (marcada) 2.dp else 1.dp, if (marcada) CarpayPrimaryGreen else CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth().clickable { tipoElegido = opcion.tipo }
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = marcada, onClick = { tipoElegido = opcion.tipo })
                        Column(Modifier.weight(1f)) {
                            Text(opcion.titulo, fontWeight = FontWeight.SemiBold, color = CarpayTextPrimary)
                            Text(opcion.detalle, fontSize = 12.sp, color = CarpayTextSecondary)
                        }
                        Text(Formato.clp(opcion.monto), fontWeight = FontWeight.Bold, color = CarpayTextPrimary)
                    }
                }
            }

            Text("Medio de pago", fontWeight = FontWeight.Bold, color = CarpayTextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Débito", "Crédito").forEach { t ->
                    FilterChip(
                        selected = tarjeta == t,
                        onClick = { tarjeta = t },
                        label = { Text("Tarjeta de ${t.lowercase()}") },
                        leadingIcon = { Icon(Icons.Outlined.CreditCard, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
            Text(
                "Simulador de Webpay: no se piden ni se guardan datos de tarjetas. El servidor registra el pago como en la web.",
                fontSize = 12.sp, color = CarpayTextSecondary
            )

            resultado?.takeIf { it.estadoPago != "Aprobado" }?.let {
                Surface(shape = RoundedCornerShape(12.dp), color = CarpayRedBg, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "El pago fue rechazado. No se hizo ningún cargo; puedes intentarlo de nuevo.",
                        color = CarpayRedText, fontSize = 13.sp, modifier = Modifier.padding(12.dp)
                    )
                }
            }
            error?.let {
                Surface(shape = RoundedCornerShape(12.dp), color = CarpayRedBg, modifier = Modifier.fillMaxWidth()) {
                    Text(it, color = CarpayRedText, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
            }

            Button(
                onClick = { error = null; confirmar = true },
                enabled = !procesando,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (procesando) CircularProgressIndicator(color = CarpayWhite, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Text("Pagar ${Formato.clp(elegida.monto)}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            if (depurable) {
                TextButton(onClick = { error = null; confirmar = false }, enabled = !procesando, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Probar un pago rechazado", fontSize = 12.sp, color = CarpayTextSecondary)
                }
            }

            confirmar?.let { aprobar ->
                DialogoConfirmacion(
                    titulo = if (aprobar) "¿Autorizar el pago?" else "¿Simular un rechazo?",
                    mensaje = if (aprobar) "${elegida.titulo} de ${Formato.clp(elegida.monto)} para ${reserva.idFormateado} con tarjeta de ${tarjeta.lowercase()} (Webpay de prueba)."
                    else "Se registrará un pago rechazado de prueba; la reserva no cambia.",
                    textoConfirmar = if (aprobar) "Autorizar" else "Simular rechazo",
                    onConfirmar = {
                        confirmar = null
                        procesando = true
                        reservasViewModel.pagarWebpay(
                            id = reserva.idReserva,
                            tipoPago = elegida.tipo,
                            simularRechazo = !aprobar,
                            onResult = { pago -> procesando = false; resultado = pago },
                            onError = { mensaje -> procesando = false; error = mensaje }
                        )
                    },
                    onCancelar = { confirmar = null }
                )
            }
        }
    }
}

@Composable
private fun ResumenReserva(r: ReservaResponse) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CarpayWhite,
        border = BorderStroke(1.dp, CarpayInputBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.idFormateado, fontWeight = FontWeight.Bold, color = CarpayTextPrimary, modifier = Modifier.weight(1f))
                com.altosdecarpay.app.ui.components.EstadoChip(r.estado)
            }
            Text(
                "${Formato.fecha(r.fecha)} · ${Formato.hora(r.horaInicio)} – ${Formato.hora(r.horaFin)}",
                fontSize = 13.sp, color = CarpayTextSecondary
            )
            fun monto(t: String?) = t?.toBigDecimalOrNull()?.toLong() ?: 0L
            r.sectores.forEach { FilaMonto("${it.nombre} · ${it.horas} h", monto(it.subtotal)) }
            r.servicios.forEach { FilaMonto("${it.nombre} × ${it.cantidad}", monto(it.subtotal)) }
            HorizontalDivider(color = CarpayInputBorder)
            FilaMonto("Total", monto(r.montoTotal), destacado = true)
            if (monto(r.montoPagado) > 0) FilaMonto("Pagado", monto(r.montoPagado))
            if (r.estado == "Pendiente" && monto(r.montoPagado) == 0L && r.plazoAnticipoVence != null) {
                Text(
                    "Paga el anticipo antes del ${Formato.fechaHora(r.plazoAnticipoVence)}; si no, la reserva se libera.",
                    fontSize = 12.sp, color = CarpayAmberText
                )
            }
        }
    }
}
