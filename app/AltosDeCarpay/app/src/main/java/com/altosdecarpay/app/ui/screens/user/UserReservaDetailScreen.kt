package com.altosdecarpay.app.ui.screens.user

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

@Composable
fun UserReservaDetailScreen(
    reservaId: String = "RES-0248",
    onBackClick: () -> Unit = {},
    onOpenTickets: () -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            UserScreenHeader(
                title = "Tu reserva",
                subtitle = "Vista de ejemplo · después de registrar la salida",
                showBackButton = true,
                onBackClick = onBackClick
            )

            // Tarjeta Evento
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(reservaId, fontSize = 12.sp, color = CarpayTextSecondary)
                        Surface(shape = RoundedCornerShape(10.dp), color = CarpayGreenLight) {
                            Text("Finalizada", fontSize = 11.sp, color = CarpayPrimaryGreen, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Cumpleaños de Camila", fontFamily = FontFamily.Serif, fontSize = 22.sp, color = CarpayTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarToday, null, tint = CarpayTextSecondary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("24 de octubre de 2026", fontSize = 13.sp, color = CarpayTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Place, null, tint = CarpayTextSecondary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Quincho Los Aromos · 30 personas", fontSize = 13.sp, color = CarpayTextSecondary)
                    }
                }
            }

            // Tarjeta QR de acceso
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Tu QR de acceso", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
                    Text("Muéstralo al equipo para registrar tu ingreso y salida.", fontSize = 12.sp, color = CarpayTextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Icon(
                        imageVector = Icons.Outlined.QrCode2,
                        contentDescription = "QR de acceso",
                        tint = CarpayPrimaryGreen,
                        modifier = Modifier.size(170.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("SOLO MUESTRA · CÓDIGO NO OPERATIVO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CarpayTextSecondary)
                    Text("$reservaId · Reserva ya finalizada", fontSize = 12.sp, color = CarpayPrimaryGreen)
                }
            }

            // Así quedó tu visita
            Text("Así quedó tu visita", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("MOVIMIENTO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarpayTextSecondary, modifier = Modifier.weight(1f))
                        Text("AGENDADO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarpayTextSecondary, modifier = Modifier.weight(1f))
                        Text("REGISTRADO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarpayTextSecondary, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Inicio", fontSize = 14.sp, color = CarpayTextPrimary, modifier = Modifier.weight(1f))
                        Text("16:00", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CarpayTextPrimary, modifier = Modifier.weight(1f))
                        Text("16:08", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CarpayTextPrimary, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(color = CarpayInputBorder, modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Cierre", fontSize = 14.sp, color = CarpayTextPrimary, modifier = Modifier.weight(1f))
                        Text("21:00", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CarpayTextPrimary, modifier = Modifier.weight(1f))
                        Text("21:25", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CarpayAmberText, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Bloque Ámbar Tiempo Excedido
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CarpayAmberBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, null, tint = CarpayAmberText, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("25 min de tiempo excedido", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CarpayAmberText)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$500/min × 25 min", fontSize = 13.sp, color = CarpayAmberText)
                        Text("$12.500", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CarpayAmberText)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Tarifa de ejemplo. Cargo extra propuesto; aún no se ha cobrado.", fontSize = 11.sp, color = CarpayAmberText)
                }
            }

            OutlinedButton(
                onClick = onOpenTickets,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CarpayInputBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = CarpayWhite, contentColor = CarpayPrimaryGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Consultas y reclamos de esta reserva", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}