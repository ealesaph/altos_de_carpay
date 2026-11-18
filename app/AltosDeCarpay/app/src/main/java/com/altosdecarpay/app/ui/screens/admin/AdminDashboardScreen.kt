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
import com.altosdecarpay.app.ui.theme.*

data class ReservaItemUi(
    val id: String,
    val hora: String,
    val titulo: String,
    val cliente: String,
    val espacio: String,
    val estado: String, // "En curso", "Confirmada", "Finalizada"
    val detalleHorario: String,
    val alertaTiempo: String? = null
)

@Composable
fun AdminDashboardScreen(
    onNavigateToScanner: () -> Unit = {},
    onSelectReserva: (String) -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todas") }

    val reservasMock = remember {
        listOf(
            ReservaItemUi(
                id = "RES-0248",
                hora = "16:00",
                titulo = "Cumpleaños de Camila",
                cliente = "Camila Rojas",
                espacio = "Quincho Los Aromos",
                estado = "En curso",
                detalleHorario = "Ingreso 16:08 · cierre agendado 21:00",
                alertaTiempo = "Cierre superado · salida aún por registrar"
            ),
            ReservaItemUi(
                id = "RES-0249",
                hora = "18:00",
                titulo = "Encuentro de equipo",
                cliente = "Diego Muñoz",
                espacio = "Salón del Roble",
                estado = "En curso",
                detalleHorario = "Ingreso 18:02 · cierre agendado 23:00"
            ),
            ReservaItemUi(
                id = "RES-0250",
                hora = "22:00",
                titulo = "Cena de aniversario",
                cliente = "Valentina Soto",
                espacio = "Jardín del Maitén",
                estado = "Confirmada",
                detalleHorario = "Sin ingreso · cierre 02:00 del 25 oct."
            )
        )
    }

    val filteredReservas = remember(selectedFilter, searchQuery) {
        ReservaFilterHelper.filterReservas(reservasMock, selectedFilter, searchQuery)
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
            // Título y fecha
            item {
                AdminScreenHeader(
                    title = "La agenda de hoy",
                    subtitle = "Sábado 24 de octubre de 2026 · 21:25"
                )
            }

            // Tarjetas KPI (6 Reservas hoy | 2 En curso | 3 Tickets abiertos)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(number = "6", label = "Reservas hoy", modifier = Modifier.weight(1f))
                    KpiCard(number = "2", label = "En curso", modifier = Modifier.weight(1f))
                    KpiCard(number = "3", label = "Tickets abiertos", modifier = Modifier.weight(1f))
                }
            }

            // Botón principal: Escanear QR
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

            // Buscador
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Buscar por cliente, evento o reserva",
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

            // Filtros (Chips)
            item {
                val filters = listOf("Todas", "En curso", "Confirmadas", "Finalizadas")
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

            // Encabezado lista
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reservas de hoy",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarpayTextPrimary
                    )
                    Text(
                        text = "Ver las 6",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CarpayPrimaryGreen
                    )
                }
            }

            // Tarjetas de Reserva
            items(filteredReservas) { reserva ->
                ReservaCard(
                    reserva = reserva,
                    onClick = { onSelectReserva(reserva.id) }
                )
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
                        text = reserva.id,
                        fontSize = 12.sp,
                        color = CarpayTextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CarpayGreenLight
                ) {
                    Text(
                        text = reserva.estado,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CarpayPrimaryGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
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
                color = if (reserva.alertaTiempo != null) CarpayAmberText else CarpayTextSecondary
            )

            if (reserva.alertaTiempo != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CarpayAmberBg)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = CarpayAmberText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = reserva.alertaTiempo,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CarpayAmberText
                    )
                }
            }
        }
    }
}