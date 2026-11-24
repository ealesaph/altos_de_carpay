package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.viewmodel.ConsultasViewModel
import com.altosdecarpay.app.ui.viewmodel.ReservasViewModel
import com.altosdecarpay.app.ui.components.EstadoChip
import com.altosdecarpay.app.ui.components.MensajeError
import com.altosdecarpay.app.ui.utils.Formato
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ReservaItemUi(
    val id: String,
    val fecha: String,
    val hora: String,
    val titulo: String,
    val cliente: String,
    val espacio: String,
    val estado: String,          // estado tal como lo entrega el servidor
    val detalleHorario: String,
    val alertaTiempo: String? = null
)

@Composable
fun AdminDashboardScreen(
    reservasViewModel: ReservasViewModel = viewModel(),
    consultasViewModel: ConsultasViewModel = viewModel(),
    onNavigateToScanner: () -> Unit = {},
    onSelectReserva: (String) -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Próximas") }

    val reservasState by reservasViewModel.reservas.collectAsState()
    val consultasState by consultasViewModel.consultas.collectAsState()

    LaunchedEffect(Unit) {
        reservasViewModel.loadReservas()
        consultasViewModel.loadConsultas(estado = "Pendiente")
    }

    val fechaActual = remember {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy · HH:mm", Locale.forLanguageTag("es-CL"))
        now.format(formatter).replaceFirstChar { it.uppercase() }
    }

    // Solo datos reales del servidor: si la carga falla se muestra el error con «Reintentar».
    val reservasReales = remember(reservasState) {
        (reservasState as? NetworkResult.Success)?.data.orEmpty().map { r ->
            ReservaItemUi(
                id = r.idFormateado,
                fecha = r.fecha,
                hora = Formato.hora(r.horaInicio),
                titulo = "Reserva ${r.sectorPrincipal}",
                cliente = r.cliente,
                espacio = r.sectores.joinToString(", ") { it.nombre }.ifBlank { r.sectorPrincipal },
                estado = r.estado,
                detalleHorario = "${Formato.fecha(r.fecha)} · ${Formato.hora(r.horaInicio)} – ${r.horaFin?.let { Formato.hora(it) } ?: "${r.horasReservadas} h"} · Total ${Formato.clp(r.montoTotal)}",
                alertaTiempo = if (r.estado in Formato.estadosVigentes && r.saldo.signum() > 0) "Saldo pendiente ${Formato.clp(r.saldoPendiente)}" else null
            )
        }
    }

    val filteredReservas = remember(reservasReales, selectedFilter, searchQuery) {
        ReservaFilterHelper.filterReservas(reservasReales, selectedFilter, searchQuery)
    }

    val hoy = LocalDate.now().toString()
    val totalHoy = reservasReales.count { it.fecha == hoy && it.estado != "Cancelado" }.toString()
    val totalEnCurso = reservasReales.count { it.estado == "En Proceso" }.toString()
    val totalTicketsPendientes = when (val cState = consultasState) {
        is NetworkResult.Success -> cState.data.size.toString()
        else -> "–"
    }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
        ) {
            item {
                AdminScreenHeader(
                    title = "La agenda",
                    subtitle = fechaActual
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(number = totalHoy, label = "Eventos hoy", modifier = Modifier.weight(1f))
                    KpiCard(number = totalEnCurso, label = "En curso", modifier = Modifier.weight(1f))
                    KpiCard(number = totalTicketsPendientes, label = "Consultas pendientes", modifier = Modifier.weight(1f))
                }
            }

            item {
                Button(
                    onClick = onNavigateToScanner,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CarpayPrimaryGreen,
                        contentColor = CarpayWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Escanear QR · ingreso o salida",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Buscar por cliente, espacio o reserva",
                            fontSize = 14.sp,
                            color = CarpayTextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = CarpayTextSecondary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CarpayWhite,
                        unfocusedContainerColor = CarpayWhite,
                        focusedBorderColor = CarpayPrimaryGreen,
                        unfocusedBorderColor = CarpayInputBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                val filters = ReservaFilterHelper.filtros
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) CarpayPrimaryGreen else CarpayGreenLight.copy(alpha = 0.6f),
                            modifier = Modifier.clickable { selectedFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) CarpayWhite else CarpayTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (selectedFilter == "Todas") "Reservas" else selectedFilter} (${filteredReservas.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarpayTextPrimary
                    )
                    TextButton(onClick = {
                        reservasViewModel.loadReservas()
                        consultasViewModel.loadConsultas(estado = "Pendiente")
                    }) {
                        Text(
                            text = "Actualizar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CarpayPrimaryGreen
                        )
                    }
                }
            }

            val estadoCarga = reservasState
            if (estadoCarga is NetworkResult.Error) {
                item {
                    MensajeError(estadoCarga.message) {
                        reservasViewModel.loadReservas()
                        consultasViewModel.loadConsultas(estado = "Pendiente")
                    }
                }
            } else if (reservasState is NetworkResult.Loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CarpayPrimaryGreen)
                    }
                }
            } else if (filteredReservas.isEmpty()) {
                item {
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
                            Icon(Icons.Outlined.EventBusy, null, tint = CarpayTextSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedFilter == "Próximas") "No hay reservas próximas." else "No hay reservas con este filtro.",
                                fontSize = 14.sp,
                                color = CarpayTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(filteredReservas) { reserva ->
                    ReservaCard(
                        reserva = reserva,
                        onClick = { onSelectReserva(reserva.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiCard(number: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CarpayWhite,
        border = BorderStroke(1.dp, CarpayInputBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = number,
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = CarpayPrimaryGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = CarpayTextSecondary
            )
        }
    }
}

@Composable
private fun ReservaCard(reserva: ReservaItemUi, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CarpayWhite,
        border = BorderStroke(1.dp, CarpayInputBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reserva.hora,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarpayTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Formato.fecha(reserva.fecha),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CarpayTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = reserva.id,
                        fontSize = 12.sp,
                        color = CarpayTextSecondary
                    )
                }

                EstadoChip(reserva.estado)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reserva.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarpayTextPrimary
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = CarpayTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${reserva.cliente} · ${reserva.espacio}",
                fontSize = 13.sp,
                color = CarpayTextSecondary
            )

            HorizontalDivider(
                color = CarpayInputBorder,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            Text(
                text = reserva.detalleHorario,
                fontSize = 12.sp,
                color = CarpayTextSecondary
            )
            if (reserva.alertaTiempo != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = reserva.alertaTiempo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CarpayAmberText)
            }
        }
    }
}
