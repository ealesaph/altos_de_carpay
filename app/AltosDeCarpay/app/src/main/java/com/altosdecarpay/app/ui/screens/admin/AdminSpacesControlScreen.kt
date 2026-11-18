package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

data class EspacioPermisoUi(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val etiquetaTipo: String,
    val icon: ImageVector,
    val estado: String // "Autorizado", "Pendiente", "Revocado"
)

@Composable
fun AdminSpacesControlScreen(
    reservaId: String = "RES-0261",
    onBackClick: () -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    var espacios by remember {
        mutableStateOf(
            listOf(
                EspacioPermisoUi("1", "Terraza superior", "Parrilla y mesas para compartir el almuerzo.", "Espacio principal", Icons.Outlined.Home, "Autorizado"),
                EspacioPermisoUi("2", "Mirador", "Espacio al aire libre para descansar y conversar.", "Al aire libre", Icons.Outlined.Park, "Autorizado"),
                EspacioPermisoUi("3", "Piscina", "Zona de baño para disfrutar en familia.", "Espacio opcional", Icons.Outlined.Pool, "Pendiente"),
                EspacioPermisoUi("4", "Terraza inferior", "Rincón con sombra para descansar.", "Espacio opcional", Icons.Outlined.Weekend, "Revocado")
            )
        )
    }

    val countAutorizados = espacios.count { it.estado == "Autorizado" }
    val countPendientes = espacios.count { it.estado == "Pendiente" }
    val countRevocados = espacios.count { it.estado == "Revocado" }

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
                title = "Control de espacios",
                subtitle = "Gestiona las áreas que podrá usar el cliente durante su encuentro.",
                showBackButton = true,
                onBackClick = onBackClick
            )

            // Resumen Reserva
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
                            Text("Confirmada", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CarpayPrimaryGreen, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Un almuerzo en familia", fontFamily = FontFamily.Serif, fontSize = 20.sp, color = CarpayTextPrimary)
                    Text("Terraza superior · 7 nov. 2026 · 13:00–18:00", fontSize = 12.sp, color = CarpayTextSecondary)
                }
            }

            // Contadores
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Áreas para este encuentro", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                Text("${espacios.size} espacios", fontSize = 12.sp, color = CarpayTextSecondary)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CounterBox("$countAutorizados", "Autorizados", CarpayPrimaryGreen, Modifier.weight(1f))
                CounterBox("$countPendientes", "Pendiente", CarpayAmberText, Modifier.weight(1f))
                CounterBox("$countRevocados", "Revocado", CarpayRedText, Modifier.weight(1f))
            }

            // Lista de espacios
            espacios.forEach { espacio ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CarpayGreenLight.copy(alpha = 0.6f))
                            ) {
                                Icon(espacio.icon, null, tint = CarpayPrimaryGreen)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(espacio.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
                                Text(espacio.descripcion, fontSize = 12.sp, color = CarpayTextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (bg, fg, labelEstado) = when (espacio.estado) {
                                "Autorizado" -> Triple(CarpayGreenLight, CarpayPrimaryGreen, "Autorizado")
                                "Pendiente" -> Triple(CarpayAmberBg, CarpayAmberText, "Pendiente de autorización")
                                else -> Triple(CarpayRedBg, CarpayRedText, "Acceso revocado")
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = bg) {
                                Text(labelEstado, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(espacio.etiquetaTipo, fontSize = 11.sp, color = CarpayTextSecondary)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (espacio.estado == "Autorizado") {
                            OutlinedButton(
                                onClick = {
                                    espacios = espacios.map { if (it.id == espacio.id) it.copy(estado = "Revocado") else it }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CarpayRedText.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CarpayRedText),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Revocar acceso", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    espacios = espacios.map { if (it.id == espacio.id) it.copy(estado = "Autorizado") else it }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.VerifiedUser, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Autorizar acceso", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterBox(count: String, label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CarpayWhite,
        border = BorderStroke(1.dp, CarpayInputBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(count, fontFamily = FontFamily.Serif, fontSize = 22.sp, color = color)
            Text(label, fontSize = 11.sp, color = CarpayTextSecondary)
        }
    }
}