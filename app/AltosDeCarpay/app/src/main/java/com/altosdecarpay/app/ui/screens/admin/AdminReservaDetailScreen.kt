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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

@Composable
fun AdminReservaDetailScreen(
    reservaId: String = "RES-0248",
    onBackClick: () -> Unit = {},
    onNavigateToSpacesControl: (String) -> Unit = {},
    onNavigateToTickets: () -> Unit = {},
    onNavigateToBitacora: (String) -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
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

            // Tarjeta Principal de la Reserva
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
                            text = reservaId,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarpayPrimaryGreen
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CarpayGreenLight
                        ) {
                            Text(
                                text = "En curso",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CarpayPrimaryGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Cumpleaños de Camila",
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        color = CarpayTextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DetailItem(
                        icon = Icons.Outlined.Person,
                        label = "Titular",
                        value = "Camila Rojas"
                    )
                    DetailItem(
                        icon = Icons.Outlined.Place,
                        label = "Espacio principal",
                        value = "Quincho Los Aromos"
                    )
                    DetailItem(
                        icon = Icons.Outlined.CalendarToday,
                        label = "Fecha",
                        value = "Sábado 24 de octubre de 2026"
                    )
                    DetailItem(
                        icon = Icons.Outlined.Schedule,
                        label = "Horario agendado",
                        value = "16:00 – 21:00 (Ingreso 16:08)"
                    )
                    DetailItem(
                        icon = Icons.Outlined.Groups,
                        label = "Asistentes",
                        value = "30 personas"
                    )
                }
            }

            Text(
                text = "Acciones y gestión",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = CarpayTextPrimary
            )

            // Opción: Control de Espacios
            ActionCard(
                icon = Icons.Outlined.DoorSliding,
                title = "Control de espacios",
                subtitle = "Autorizar o revocar accesos a áreas para esta reserva",
                onClick = { onNavigateToSpacesControl(reservaId) }
            )

            // Opción: Tickets y Reclamos
            ActionCard(
                icon = Icons.Outlined.Forum,
                title = "Tickets y conversación",
                subtitle = "Ver canal de comunicación directo con la cliente",
                onClick = onNavigateToTickets
            )

            // Opción: Bitácora de Incidencias
            ActionCard(
                icon = Icons.Outlined.EditNote,
                title = "Bitácora interna",
                subtitle = "Registrar o ver observaciones sobre esta reserva",
                onClick = { onNavigateToBitacora(reservaId) }
            )
        }
    }
}

@Composable
private fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CarpayTextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            fontSize = 13.sp,
            color = CarpayTextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = CarpayTextPrimary
        )
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CarpayPrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CarpayTextPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = CarpayTextSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = CarpayTextSecondary
            )
        }
    }
}
