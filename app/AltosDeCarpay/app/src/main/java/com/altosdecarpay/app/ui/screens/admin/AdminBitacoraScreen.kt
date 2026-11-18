package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

@Composable
fun AdminBitacoraScreen(
    reservaId: String = "RES-0248",
    onBackClick: () -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    var descripcion by remember { mutableStateOf("") }
    var registros by remember {
        mutableStateOf(
            listOf(
                Triple("INC-0043 · Ventana rota", "24 oct. 2026 · 21:55 · Ana Pérez · Terraza superior · Infraestructura", "En la revisión de cierre se observa un vidrio roto en la ventana lateral. La causa no está determinada.")
            )
        )
    }

    Scaffold(
        topBar = { AdminTopBar() },
        bottomBar = { AdminBottomBar(selectedTab = AdminTab.PERFIL, onTabSelected = onTabSelected) },
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
                title = "Bitácora de incidencias",
                subtitle = "Observaciones internas por reserva, para seguimiento y consulta posterior.",
                showBackButton = true,
                onBackClick = onBackClick
            )

            // Banner uso interno
            Surface(shape = RoundedCornerShape(14.dp), color = CarpayGreenLight, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, tint = CarpayPrimaryGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Uso interno · No visible para el cliente", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CarpayTextPrimary)
                        Text("Los mensajes de clientes se responden en Tickets · Bandeja por reserva.", fontSize = 11.sp, color = CarpayTextSecondary)
                    }
                }
            }

            // Reserva vinculada
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CarpayWhite,
                border = BorderStroke(1.5.dp, CarpayPrimaryGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(reservaId, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                        Surface(shape = RoundedCornerShape(8.dp), color = CarpayGreenLight) {
                            Text("Finalizada", fontSize = 11.sp, color = CarpayPrimaryGreen, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    Text("Cumpleaños de Camila", fontFamily = FontFamily.Serif, fontSize = 20.sp, color = CarpayTextPrimary)
                    Text("24 oct. 2026 · Terraza superior", fontSize = 12.sp, color = CarpayTextSecondary)
                }
            }

            Text("Historial · ${registros.size} registro(s)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)

            registros.forEach { (titulo, meta, detalle) ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(meta, fontSize = 11.sp, color = CarpayTextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(detalle, fontSize = 13.sp, color = CarpayTextPrimary)
                    }
                }
            }

            // Formulario Nueva Observación
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nueva observación", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CarpayTextPrimary)
                    Text("Describe lo observado de forma neutral, sin atribuir responsabilidades no verificadas.", fontSize = 12.sp, color = CarpayTextSecondary)

                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        placeholder = { Text("Indica qué se observó, cuándo y el seguimiento necesario...", fontSize = 13.sp) },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (descripcion.isNotBlank()) {
                                registros = listOf(
                                    Triple("INC-0044 · Nueva observación", "Hoy · Equipo Admin · $reservaId", descripcion)
                                ) + registros
                                descripcion = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Outlined.EditNote, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar en bitácora", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}