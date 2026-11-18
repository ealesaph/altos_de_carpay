package com.altosdecarpay.app.ui.screens.user

import androidx.compose.foundation.BorderStroke
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
fun UserTicketsScreen(
    onBackClick: () -> Unit = {},
    onNavigateToObservations: () -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    var nuevoMensaje by remember { mutableStateOf("") }
    var mensajesExtra by remember { mutableStateOf(listOf<String>()) }

    Scaffold(
        topBar = { UserTopBar() },
        bottomBar = { UserBottomBar(selectedTab = UserTab.AYUDA, onTabSelected = onTabSelected) },
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
                title = "Estamos para ayudarte",
                subtitle = "Consultas y reclamos vinculados a tu reserva",
                showBackButton = true,
                onBackClick = onBackClick
            )

            // Contexto reserva
            Surface(shape = RoundedCornerShape(14.dp), color = CarpayGreenLight, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("RES-0248 · FINALIZADA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                    Text("Cumpleaños de Camila", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
                    Text("24 oct. 2026 · Quincho Los Aromos", fontSize = 12.sp, color = CarpayTextSecondary)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("T-018 · Reclamo", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CarpayTextPrimary)
                Surface(shape = RoundedCornerShape(8.dp), color = CarpayAmberBg) {
                    Text("Abierto", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CarpayAmberText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            Text("Revisión del cargo extra", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CarpayTextPrimary)

            // Burbujas del chat (vista cliente: Camila a la derecha, Equipo a la izquierda)
            UserChatBubble("Tú · Camila", "Hola, ¿me pueden explicar el cargo extra de mi reserva?", "21:32", isUser = true)
            UserChatBubble("Equipo Altos de carpay", "La salida quedó registrada a las 21:25: son 25 min adicionales. El cargo propuesto es de $12.500 y aún no se ha cobrado.", "21:35", isUser = false)
            UserChatBubble("Tú · Camila", "Gracias. ¿Me comparten el detalle del cálculo?", "21:37 · Enviado", isUser = true)

            mensajesExtra.forEach { msg ->
                UserChatBubble("Tú · Camila", msg, "Ahora · Enviado", isUser = true)
            }

            // Input de respuesta
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = nuevoMensaje,
                        onValueChange = { nuevoMensaje = it },
                        placeholder = { Text("Escribe tu mensaje...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AttachFile, null, tint = CarpayTextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Adjuntar", fontSize = 12.sp, color = CarpayTextSecondary)
                        }
                        TextButton(onClick = {
                            if (nuevoMensaje.isNotBlank()) {
                                mensajesExtra = mensajesExtra + nuevoMensaje
                                nuevoMensaje = ""
                            }
                        }) {
                            Text("Enviar", fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Outlined.Send, null, tint = CarpayPrimaryGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onNavigateToObservations,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CarpayInputBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = CarpayWhite, contentColor = CarpayPrimaryGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nueva consulta u observación previa", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun UserChatBubble(author: String, message: String, time: String, isUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isUser) CarpayGreenLight else CarpayWhite,
            border = if (isUser) null else BorderStroke(1.dp, CarpayInputBorder),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(author, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Text(message, fontSize = 13.sp, color = CarpayTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(time, fontSize = 11.sp, color = CarpayTextSecondary, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}