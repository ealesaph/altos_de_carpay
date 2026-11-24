package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.components.DialogoConfirmacion
import com.altosdecarpay.app.ui.components.EstadoChip
import com.altosdecarpay.app.ui.components.MensajeError
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.utils.Formato
import com.altosdecarpay.app.ui.utils.QrCodeGenerator
import com.altosdecarpay.app.ui.viewmodel.ReservasViewModel

private data class AccionPendiente(
    val titulo: String,
    val mensaje: String,
    val textoConfirmar: String,
    val peligro: Boolean = false,
    val ejecutar: () -> Unit
)

/** Transiciones que el administrador puede hacer desde la app (mismas reglas que el panel web). */
private val SIGUIENTES = mapOf(
    "Pendiente" to listOf("Confirmado"),
    "Confirmado" to listOf("En Proceso", "No se presentó"),
    "En Proceso" to listOf("Finalizado")
)

@Composable
fun AdminReservaDetailScreen(
    reservaId: String,
    reservasViewModel: ReservasViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToSpacesControl: (String) -> Unit = {},
    onNavigateToTickets: () -> Unit = {},
    onNavigateToBitacora: (String) -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    val numericId = remember(reservaId) { QrCodeGenerator.parseReservaId(reservaId) ?: 0 }
    val detalleState by reservasViewModel.reservaDetalle.collectAsState()
    var accion by remember { mutableStateOf<AccionPendiente?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var metodoPago by remember { mutableStateOf("Transferencia") }
    var cancelacion by remember { mutableStateOf<Pair<com.altosdecarpay.app.data.model.SimulacionCancelacionResponse, com.altosdecarpay.app.data.model.SimulacionCancelacionResponse>?>(null) }
    var origenCancelacion by remember { mutableStateOf("Cliente") }

    LaunchedEffect(numericId) {
        reservasViewModel.loadReservaDetalle(numericId)
    }

    // Sin reserva de ejemplo: si no existe o falla la carga se muestra el error.
    val reserva = (detalleState as? NetworkResult.Success)?.data?.takeIf { it.idReserva == numericId }
    val alTerminar: (String) -> Unit = { texto -> mensaje = texto }
    val alFallar: (String) -> Unit = { texto -> mensaje = texto }

    accion?.let { a ->
        DialogoConfirmacion(
            titulo = a.titulo,
            mensaje = a.mensaje,
            textoConfirmar = a.textoConfirmar,
            peligro = a.peligro,
            onCancelar = { accion = null },
            onConfirmar = { accion = null; a.ejecutar() }
        )
    }

    cancelacion?.let { (porCliente, porRecinto) ->
        DialogoConfirmacion(
            titulo = "¿Cancelar la reserva $reservaId?",
            mensaje = "Pagado: ${Formato.clp(porCliente.montoPagado)}. La cancelación no se puede deshacer. Indica quién cancela para calcular la devolución:",
            textoConfirmar = "Cancelar reserva",
            peligro = true,
            onCancelar = { cancelacion = null },
            onConfirmar = {
                cancelacion = null
                reservasViewModel.cancelarReserva(numericId, origenCancelacion, onSuccess = { r ->
                    alTerminar("Reserva cancelada. Devolución a registrar: ${Formato.clp(r?.montoDevolucion)}.")
                }, onError = alFallar)
            },
            contenidoExtra = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Cliente" to porCliente, "Recinto" to porRecinto).forEach { (origen, sim) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { origenCancelacion = origen }
                        ) {
                            RadioButton(selected = origenCancelacion == origen, onClick = { origenCancelacion = origen })
                            Column {
                                Text(
                                    "${if (origen == "Cliente") "El arrendatario" else "Altos de Carpay"} · devolución ${Formato.clp(sim.montoDevolucion)}",
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                                )
                                Text(sim.regla, fontSize = 11.sp, color = CarpayTextSecondary)
                            }
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = { AdminTopBar() },
        bottomBar = { AdminBottomBar(selectedTab = AdminTab.RESERVAS, onTabSelected = onTabSelected) },
        containerColor = CarpayBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AdminScreenHeader(
                title = "Detalle de reserva",
                subtitle = "Información y gestión de la reserva $reservaId",
                showBackButton = true,
                onBackClick = onBackClick
            )

            val estadoCarga = detalleState
            if (estadoCarga is NetworkResult.Error) {
                MensajeError(estadoCarga.message) { reservasViewModel.loadReservaDetalle(numericId) }
                return@Column
            }
            if (reserva == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
                return@Column
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(reserva.idFormateado, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                        EstadoChip(reserva.estado)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Reserva ${reserva.sectorPrincipal}", fontFamily = FontFamily.Serif, fontSize = 22.sp, color = CarpayTextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailItem(Icons.Outlined.Person, "Titular", reserva.cliente)
                    DetailItem(Icons.Outlined.Email, "Correo", reserva.correoCliente)
                    DetailItem(Icons.Outlined.Place, "Sectores", reserva.sectores.joinToString(", ") { "${it.nombre} (${it.horas} h)" })
                    DetailItem(Icons.Outlined.CalendarToday, "Fecha", Formato.fecha(reserva.fecha))
                    DetailItem(Icons.Outlined.Schedule, "Horario", "${Formato.hora(reserva.horaInicio)} – ${reserva.horaFin?.let { Formato.hora(it) } ?: "${reserva.horasReservadas} h"}")
                    if (reserva.servicios.isNotEmpty()) {
                        DetailItem(Icons.Outlined.Storefront, "Extras", reserva.servicios.joinToString(", ") { "${it.nombre} ×${it.cantidad}" })
                    }
                    DetailItem(Icons.Outlined.Payments, "Total", Formato.clp(reserva.montoTotal))
                    DetailItem(Icons.Outlined.Paid, "Pagado", Formato.clp(reserva.montoPagado))
                    DetailItem(Icons.Outlined.AccountBalanceWallet, "Saldo", Formato.clp(reserva.saldoPendiente))
                    if (reserva.estado == "Cancelado") {
                        DetailItem(
                            Icons.AutoMirrored.Outlined.Undo, "Devolución",
                            "${Formato.clp(reserva.montoDevolucion)} · " +
                                (if (reserva.devolucionRealizadaEn != null) "realizada ${Formato.fechaHora(reserva.devolucionRealizadaEn)}" else "pendiente") +
                                " · canceló ${if (reserva.canceladaPor == "Recinto") "Altos de Carpay" else "el cliente"}"
                        )
                    }
                }
            }

            if (mensaje != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = CarpayGreenLight, modifier = Modifier.fillMaxWidth()) {
                    Text(mensaje!!, fontSize = 13.sp, color = CarpayPrimaryGreen, modifier = Modifier.padding(12.dp))
                }
            }

            // ── Acciones sobre la reserva (antes el detalle no permitía gestionarla) ──
            Text("Gestionar la reserva", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)

            SIGUIENTES[reserva.estado].orEmpty().forEach { destino ->
                val detalle = when (destino) {
                    "Confirmado" -> "La reserva quedará confirmada aunque el anticipo no se haya pagado en el sistema."
                    "En Proceso" -> "Registra la llegada del cliente. Solo es posible el día del evento, desde 1 hora antes."
                    "Finalizado" -> "La reserva quedará cerrada y su bitácora pasará a solo lectura."
                    else -> "La reserva se cerrará sin asistencia y se conservará el anticipo pagado."
                }
                ActionCard(
                    icon = if (destino == "No se presentó") Icons.Outlined.EventBusy else Icons.Outlined.CheckCircle,
                    title = "Pasar a «${Formato.estado(destino)}»",
                    subtitle = detalle,
                    onClick = {
                        accion = AccionPendiente(
                            "¿Pasar ${reserva.idFormateado} a «${Formato.estado(destino)}»?", "$detalle Este cambio no se puede deshacer.",
                            "Confirmar", destino == "No se presentó"
                        ) { reservasViewModel.cambiarEstado(numericId, destino, { alTerminar("Reserva actualizada a «${Formato.estado(destino)}».") }, alFallar) }
                    }
                )
            }

            if (reserva.saldo.signum() > 0 && reserva.estado in setOf("Pendiente", "Confirmado", "En Proceso", "Finalizado")) {
                val tipo = if ((reserva.montoPagado?.toBigDecimalOrNull()?.signum() ?: 0) > 0) "Saldo" else "Anticipo"
                val monto = if (tipo == "Saldo") reserva.saldoPendiente else reserva.montoAnticipo
                Surface(
                    shape = RoundedCornerShape(16.dp), color = CarpayWhite, border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Registrar ${tipo.lowercase()} · ${Formato.clp(monto)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CarpayTextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Transferencia", "Efectivo").forEach { m ->
                                FilterChip(selected = metodoPago == m, onClick = { metodoPago = m }, label = { Text(m) })
                            }
                        }
                        Button(
                            onClick = {
                                accion = AccionPendiente(
                                    "¿Registrar ${tipo.lowercase()} de ${reserva.idFormateado}?",
                                    "Se registrará un pago de ${Formato.clp(monto)} por ${metodoPago.lowercase()} a nombre de ${reserva.cliente}. Un pago registrado no se puede eliminar.",
                                    "Registrar pago"
                                ) { reservasViewModel.registrarPago(numericId, tipo, metodoPago, { alTerminar("Pago registrado (${tipo.lowercase()} · ${metodoPago.lowercase()}).") }, alFallar) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Registrar pago") }
                    }
                }
            }

            if (reserva.estado in setOf("Pendiente", "Confirmado")) {
                ActionCard(
                    icon = Icons.Outlined.Cancel,
                    title = "Cancelar reserva",
                    subtitle = "Calcula la devolución según quién cancela y la anticipación",
                    onClick = {
                        origenCancelacion = "Cliente"
                        reservasViewModel.simularCancelacion(numericId, "Cliente", { cliente ->
                            reservasViewModel.simularCancelacion(numericId, "Recinto", { recinto -> cancelacion = cliente to recinto }, alFallar)
                        }, alFallar)
                    }
                )
            }

            if (reserva.estado == "Cancelado" && (reserva.montoDevolucion?.toBigDecimalOrNull()?.signum() ?: 0) > 0 && reserva.devolucionRealizadaEn == null) {
                ActionCard(
                    icon = Icons.AutoMirrored.Outlined.Undo,
                    title = "Registrar devolución realizada",
                    subtitle = "Marca que ya se transfirieron ${Formato.clp(reserva.montoDevolucion)} al cliente",
                    onClick = {
                        accion = AccionPendiente(
                            "¿Registrar la devolución de ${reserva.idFormateado}?",
                            "Confirma que ya transferiste ${Formato.clp(reserva.montoDevolucion)} a ${reserva.cliente}. El registro no se puede deshacer.",
                            "Registrar devolución"
                        ) { reservasViewModel.registrarDevolucion(numericId, { alTerminar("Devolución registrada.") }, alFallar) }
                    }
                )
            }

            Text("Más información", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
            ActionCard(
                icon = Icons.Outlined.DoorSliding,
                title = "Estado de los espacios",
                subtitle = "Disponibilidad general de los sectores del recinto",
                onClick = { onNavigateToSpacesControl(reservaId) }
            )
            ActionCard(
                icon = Icons.Outlined.Forum,
                title = "Consultas de clientes",
                subtitle = "Ver y responder consultas",
                onClick = onNavigateToTickets
            )
            ActionCard(
                icon = Icons.Outlined.EditNote,
                title = "Bitácora de la reserva",
                subtitle = "Incidencias registradas durante el evento",
                onClick = { onNavigateToBitacora(reservaId) }
            )
        }
    }
}

@Composable
private fun DetailItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = CarpayTextSecondary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("$label: ", fontSize = 13.sp, color = CarpayTextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CarpayTextPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ActionCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CarpayWhite,
        border = BorderStroke(1.dp, CarpayInputBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(icon, null, tint = CarpayPrimaryGreen, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CarpayTextPrimary)
                    Text(subtitle, fontSize = 12.sp, color = CarpayTextSecondary)
                }
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = CarpayTextSecondary)
        }
    }
}
