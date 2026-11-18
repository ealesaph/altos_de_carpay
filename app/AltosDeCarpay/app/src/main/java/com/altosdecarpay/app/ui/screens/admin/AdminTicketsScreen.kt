package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

@Composable
fun AdminTicketsScreen(
    onTabSelected: (AdminTab) -> Unit = {}
) {
    var filterSelected by remember { mutableStateOf("Todos") }
    var respuestaBorrador by remember {
        mutableStateOf("Claro, el cálculo es \$500/min × 25 min = \$12.500, con tarifa de ejemplo. Es un cargo propuesto y no se ha cobrado.")
    }
    var ticketResuelto by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { AdminTopBar() },
        bottomBar = { AdminBottomBar(selectedTab = AdminTab.TICKETS, onTabSelected = onTabSelected) },
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
                title = "Consultas y reclamos",
                subtitle = "Cada conversación, en el contexto de su reserva."
            )

            // Filtros
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todos · 4", "Abiertos · 3").forEach { item ->
                    val active = item.startsWith(filterSelected)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (active) CarpayPrimaryGreen else CarpayGreenLight.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { filterSelected = item.substringBefore(" ") }
                    ) {
                        Text(
                            text = item,
                            color = if (active) CarpayWhite else CarpayTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Tarjeta Reserva Seleccionada
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.5.dp, CarpayPrimaryGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("RES-0248 · Camila Rojas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                    Text("Cumpleaños de Camila", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CarpayTextPrimary)
                    Text("24 oct. · Reserva finalizada", fontSize = 12.sp, color = CarpayTextSecondary)

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(shape = RoundedCornerShape(12.dp), color = CarpayGreenLight, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("T-018 · Revisión del cargo extra", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CarpayTextPrimary)
                                Text("Reclamo · último mensaje 21:37", fontSize = 11.sp, color = CarpayTextSecondary)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (ticketResuelto) CarpayWhite else CarpayAmberBg
                            ) {
                                Text(
                                    text = if (ticketResuelto) "Resuelta" else "Abierto",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ticketResuelto) CarpayPrimaryGreen else CarpayAmberText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = CarpayInputBorder)

            // Hilo de conversación
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("T-018 · RES-0248", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CarpayTextPrimary)
                Text("24 oct. 2026", fontSize = 12.sp, color = CarpayTextSecondary)
            }

            // Mensaje Cliente 1
            ChatBubble(
                author = "Camila Rojas",
                message = "Hola, ¿me pueden explicar el cargo extra de mi reserva?",
                time = "21:32",
                isAdmin = false
            )

            // Mensaje Admin
            ChatBubble(
                author = "Equipo Altos de carpay",
                message = "La salida quedó registrada a las 21:25: son 25 min adicionales. El cargo propuesto es de $12.500 y aún no se ha cobrado.",
                time = "21:35 · Enviado",
                isAdmin = true
            )

            // Mensaje Cliente 2
            ChatBubble(
                author = "Camila Rojas",
                message = "Gracias. ¿Me comparten el detalle del cálculo?",
                time = "21:37",
                isAdmin = false
            )

            // Caja de respuesta
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("TU RESPUESTA · BORRADOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarpayTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = respuestaBorrador,
                        onValueChange = { respuestaBorrador = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CarpayPrimaryGreen,
                            unfocusedBorderColor = CarpayInputBorder
                        )
                    )
                }
            }

            Button(
                onClick = { respuestaBorrador = "" },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar respuesta", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = { ticketResuelto = !ticketResuelto },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CarpayInputBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = CarpayWhite, contentColor = CarpayPrimaryGreen),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Outlined.CheckCircleOutline, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (ticketResuelto) "Reabrir ticket" else "Marcar ticket como resuelto", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ChatBubble(author: String, message: String, time: String, isAdmin: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAdmin) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isAdmin) CarpayGreenLight else CarpayWhite,
            border = if (isAdmin) null else BorderStroke(1.dp, CarpayInputBorder),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(author, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Text(message, fontSize = 13.sp, color = CarpayTextPrimary, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(time, fontSize = 11.sp, color = CarpayTextSecondary, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}