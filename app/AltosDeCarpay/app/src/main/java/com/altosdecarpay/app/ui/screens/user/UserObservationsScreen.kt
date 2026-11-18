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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

@Composable
fun UserObservationsScreen(
    reservaId: String = "RES-0261",
    onBackClick: () -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    var asunto by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var observaciones by remember {
        mutableStateOf(
            listOf(
                Triple(
                    "OBS-0032 · Acceso para una persona mayor",
                    "Hola, para el almuerzo del 7 de noviembre vendrá mi abuela. ¿Hay un acceso sin escalones a la terraza?",
                    "Administrador · Altos de carpay: Sí, pueden ingresar por el acceso lateral sin escalones. Al llegar, avísennos y les indicaremos el camino a la terraza. ¡Los esperamos!"
                )
            )
        )
    }

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
                title = "Conversemos",
                subtitle = "Contacta al equipo de Altos de carpay. Estamos para acompañar tu próximo encuentro.",
                showBackButton = true,
                onBackClick = onBackClick
            )

            Surface(shape = RoundedCornerShape(14.dp), color = CarpayGreenLight, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("TU RESERVA · $reservaId", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                    Text("Un almuerzo en familia", fontFamily = FontFamily.Serif, fontSize = 20.sp, color = CarpayTextPrimary)
                    Text("7 nov. 2026 · 13:00–18:00 · Terraza superior · Próxima", fontSize = 12.sp, color = CarpayTextSecondary)
                }
            }

            // Formulario Deja una observación
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Deja una observación", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CarpayTextPrimary)
                    OutlinedTextField(
                        value = asunto,
                        onValueChange = { asunto = it },
                        placeholder = { Text("Ej.: Preparativos para el almuerzo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mensaje,
                        onValueChange = { mensaje = it },
                        placeholder = { Text("Cuéntanos tu consulta o qué necesitas que tengamos en cuenta...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (asunto.isNotBlank() && mensaje.isNotBlank()) {
                                observaciones = listOf(Triple("OBS-0033 · $asunto", mensaje, "Pendiente de respuesta del equipo")) + observaciones
                                asunto = ""
                                mensaje = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar observación", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Text("Tus observaciones (${observaciones.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)

            observaciones.forEach { (titulo, consulta, respuesta) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                        Text(consulta, fontSize = 13.sp, color = CarpayTextPrimary)
                        Surface(shape = RoundedCornerShape(10.dp), color = CarpayGreenLight, modifier = Modifier.fillMaxWidth()) {
                            Text(respuesta, fontSize = 12.sp, color = CarpayTextPrimary, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }
    }
}