package com.altosdecarpay.app.ui.screens.admin

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.utils.QrCodeGenerator
import com.altosdecarpay.app.ui.viewmodel.ReservasViewModel

@Composable
fun AdminReservaDetailScreen(
    reservaId: String = "RES-0001",
    reservasViewModel: ReservasViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToSpacesControl: (String) -> Unit = {},
    onNavigateToTickets: () -> Unit = {},
    onNavigateToBitacora: (String) -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    val numericId = remember(reservaId) { QrCodeGenerator.parseReservaId(reservaId) ?: 1 }
    val detalleState by reservasViewModel.reservaDetalle.collectAsState()

    LaunchedEffect(numericId) {
        reservasViewModel.loadReservaDetalle(numericId)
    }

    val fetchedReserva = (detalleState as? NetworkResult.Success)?.data
    val reserva = fetchedReserva ?: com.altosdecarpay.app.data.model.ReservaResponse(
        idReserva = 248,
        usuario = 2,
        cliente = "Camila Rojas",
        correoCliente = "camila@carpay.com",
        fecha = "2025-02-20",
        horaInicio = "16:00:00",
        horaFin = "20:00:00",
        horasReservadas = 4,
        estado = "En curso",
        montoTotal = "40000.00",
        montoAnticipo = "20000.00",
        montoPagado = "20000.00",
        saldoPendiente = "20000.00",
        plazoAnticipoVence = null,
        fechaCreacion = "2025-02-15T12:00:00Z",
        tokenQr = "QR-RES-0248",
        sectores = listOf(com.altosdecarpay.app.data.model.SectorReservadoResponse(1, "Quincho Los Aromos", 4, "10000.00", "40000.00")),
        servicios = emptyList(),
        pagos = emptyList()
    )

    Scaffold(
        topBar = { AdminTopBar() },
        bottomBar = {
            AdminBottomBar(
                selectedTab = AdminTab.RESERVAS,
                onTabSelected = onTabSelected
            )
        },
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
                subtitle = "Información general y gestión de la reserva $reservaId",
                showBackButton = true,
                onBackClick = onBackClick
            )

            if (detalleState is NetworkResult.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
            } else {
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
                            Text(
                                text = reserva.idFormateado,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarpayPrimaryGreen
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CarpayGreenLight
                            ) {
                                Text(
                                    text = reserva.estado,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CarpayPrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Reserva ${reserva.sectorPrincipal}",
                            fontFamily = FontFamily.Serif,
                            fontSize = 22.sp,
                            color = CarpayTextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        DetailItem(Icons.Outlined.Person, "Titular", reserva.cliente)
                        DetailItem(Icons.Outlined.Email, "Correo", reserva.correoCliente)
                        DetailItem(Icons.Outlined.Place, "Sectores", reserva.sectores.joinToString(", ") { "${it.nombre} (${it.horas}h)" })
                        DetailItem(Icons.Outlined.CalendarToday, "Fecha", reserva.fecha)
                        DetailItem(Icons.Outlined.Schedule, "Horario", "${reserva.horaInicio.take(5)} – ${reserva.horaFin?.take(5) ?: "${reserva.horasReservadas}h"}")
                        DetailItem(Icons.Outlined.Payments, "Monto total", "$${reserva.montoTotal} (Pagado: $${reserva.montoPagado ?: "0.00"})")
                    }
                }
            }

            Text(
                text = "Acciones y gestión",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = CarpayTextPrimary
            )

            ActionCard(
                icon = Icons.Outlined.DoorSliding,
                title = "Control de espacios",
                subtitle = "Ver y gestionar disponibilidad de sectores",
                onClick = { onNavigateToSpacesControl(reservaId) }
            )

            ActionCard(
                icon = Icons.Outlined.Forum,
                title = "Tickets y conversación",
                subtitle = "Ver consultas y responder a clientes",
                onClick = onNavigateToTickets
            )

            ActionCard(
                icon = Icons.Outlined.EditNote,
                title = "Bitácora y auditoría",
                subtitle = "Historial de modificaciones registradas en el sistema",
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
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CarpayTextPrimary)
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
