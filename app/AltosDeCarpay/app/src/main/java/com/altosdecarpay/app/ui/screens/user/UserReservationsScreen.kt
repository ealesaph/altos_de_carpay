package com.altosdecarpay.app.ui.screens.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.SessionState
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.components.EstadoChip
import com.altosdecarpay.app.ui.components.MensajeError
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.utils.Formato
import com.altosdecarpay.app.ui.viewmodel.ReservasViewModel

@Composable
fun UserReservationsScreen(
    reservasViewModel: ReservasViewModel = viewModel(),
    onOpenReservaDetail: (String) -> Unit = {},
    onOpenExtraServices: (String) -> Unit = {},
    onOpenMySpaces: (String) -> Unit = {},
    onOpenObservations: (String) -> Unit = {},
    onNuevaReserva: () -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    var selectedSegment by remember { mutableStateOf("Próximas") }
    val reservasState by reservasViewModel.reservas.collectAsState()

    LaunchedEffect(Unit) {
        reservasViewModel.loadReservas()
    }

    // Solo reservas reales: ya no se muestra una reserva de ejemplo cuando la lista está vacía o falla.
    val allReservas = (reservasState as? NetworkResult.Success)?.data ?: emptyList()
    val proximas = remember(allReservas) {
        allReservas.filter { it.estado in Formato.estadosVigentes }.sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
    }
    // Historial: todo lo que ya no está vigente (Finalizado, Cancelado, No se presentó y estados futuros).
    val historial = remember(allReservas) {
        allReservas.filter { it.estado !in Formato.estadosVigentes }
    }

    val nombreUsuario = SessionState.userName
    val reservasMostradas = if (selectedSegment == "Próximas") proximas else historial

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
            UserScreenHeader(
                title = "Tus buenos momentos",
                subtitle = if (nombreUsuario.isBlank()) "Tu próximo encuentro empieza aquí." else "Hola, $nombreUsuario. Tu próximo encuentro empieza aquí."
            )

            // Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarpayGreenLight.copy(alpha = 0.6f))
                    .padding(4.dp)
            ) {
                listOf("Próximas · ${proximas.size}", "Historial · ${historial.size}").forEach { tabLabel ->
                    val key = tabLabel.substringBefore(" ")
                    val isSelected = selectedSegment == key
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) CarpayWhite else Color.Transparent,
                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSegment = key }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = tabLabel,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CarpayTextPrimary else CarpayTextSecondary
                            )
                        }
                    }
                }
            }

            val estadoCarga = reservasState
            if (estadoCarga is NetworkResult.Error) {
                MensajeError(estadoCarga.message) { reservasViewModel.loadReservas() }
            } else if (reservasState is NetworkResult.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
            } else if (reservasMostradas.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.EventAvailable, null, tint = CarpayPrimaryGreen, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedSegment == "Próximas") "No tienes reservas activas por ahora." else "No hay reservas en tu historial.",
                            fontSize = 14.sp,
                            color = CarpayTextSecondary
                        )
                    }
                }
            } else {
                reservasMostradas.forEach { reserva ->
                    val vigente = reserva.estado in Formato.estadosVigentes
                    val conAcceso = reserva.estado in Formato.estadosConAcceso
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = CarpayWhite,
                        border = BorderStroke(1.dp, CarpayInputBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenReservaDetail(reserva.idFormateado) }
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(Color(0xFF3A5243))
                                    .padding(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = CarpayWhite,
                                    modifier = Modifier.align(Alignment.BottomStart)
                                ) {
                                    Text(
                                        text = reserva.sectorPrincipal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CarpayTextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = CarpayPrimaryGreen.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .clickable { onOpenReservaDetail(reserva.idFormateado) }
                                ) {
                                    Text(
                                        text = if (conAcceso) "Ver QR y detalle →" else "Ver detalle →",
                                        fontSize = 11.sp,
                                        color = CarpayWhite,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(reserva.idFormateado, fontSize = 12.sp, color = CarpayTextSecondary)
                                    EstadoChip(reserva.estado)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Reserva en ${reserva.sectorPrincipal}",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 20.sp,
                                    color = CarpayTextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.CalendarToday, null, tint = CarpayTextSecondary, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "${Formato.fecha(reserva.fecha)} · ${Formato.hora(reserva.horaInicio)} – ${reserva.horaFin?.let { Formato.hora(it) } ?: "${reserva.horasReservadas} h"}",
                                        fontSize = 13.sp,
                                        color = CarpayTextPrimary
                                    )
                                }

                                if (reserva.estado == "Confirmado" && reserva.saldo.signum() > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Saldo pendiente ${Formato.clp(reserva.saldoPendiente)} · págalo hasta ${Formato.fechaHora(reserva.plazoSaldoVence)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CarpayAmberText
                                    )
                                }
                                if (vigente) {
                                Spacer(modifier = Modifier.height(14.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CarpayGreenLight.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenExtraServices(reserva.idFormateado) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Add, null, tint = CarpayPrimaryGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Ver servicios extra",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CarpayPrimaryGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CarpayBackground,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenObservations(reserva.idFormateado) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = CarpayTextPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Conversemos · Enviar consulta",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = CarpayTextPrimary
                                        )
                                    }
                                }
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onNuevaReserva,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CarpayPrimaryGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Outlined.AddCircleOutline, null, tint = CarpayPrimaryGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nueva reserva", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = CarpayPrimaryGreen)
            }

            Button(
                onClick = { onTabSelected(UserTab.EXPLORAR) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Outlined.Explore, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Explorar espacios disponibles", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Icon(Icons.Outlined.Eco, null, tint = CarpayPrimaryGreen, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Naturaleza para celebrar y compartir.", fontSize = 12.sp, color = CarpayTextSecondary)
            }
        }
    }
}
