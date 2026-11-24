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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.viewmodel.ServiciosViewModel

@Composable
fun AdminSpacesControlScreen(
    reservaId: String = "RES-0001",
    serviciosViewModel: ServiciosViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    val sectoresState by serviciosViewModel.sectores.collectAsState()
    // Cambiar el estado de un sector afecta al catálogo público para todos los clientes: se confirma antes.
    var cambioPendiente by remember { mutableStateOf<Pair<com.altosdecarpay.app.data.model.SectorResponse, String>?>(null) }
    cambioPendiente?.let { (sector, nuevo) ->
        com.altosdecarpay.app.ui.components.DialogoConfirmacion(
            titulo = if (nuevo == "Mantencion") "¿Poner «${sector.nombre}» en mantención?" else "¿Habilitar «${sector.nombre}»?",
            mensaje = if (nuevo == "Mantencion")
                "El sector dejará de ofrecerse en la web y en la app para todas las fechas, no solo para esta reserva. Las reservas ya hechas no se cancelan."
            else "El sector volverá a ofrecerse en la web y en la app para nuevas reservas.",
            textoConfirmar = if (nuevo == "Mantencion") "Poner en mantención" else "Habilitar",
            peligro = nuevo == "Mantencion",
            onCancelar = { cambioPendiente = null },
            onConfirmar = {
                cambioPendiente = null
                serviciosViewModel.actualizarEstadoSector(sector.idSector, nuevo)
            }
        )
    }

    LaunchedEffect(Unit) {
        serviciosViewModel.loadSectores()
    }

    val sectores = (sectoresState as? NetworkResult.Success)?.data ?: emptyList()
    val countDisponibles = sectores.count { it.estado == "Disponible" }
    val countMantencion = sectores.count { it.estado == "Mantencion" }
    val countInactivos = sectores.count { it.estado == "Inactivo" }

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
                title = "Estado de los espacios",
                subtitle = "Disponibilidad general de los sectores del recinto (afecta al catálogo público).",
                showBackButton = true,
                onBackClick = onBackClick
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sectores del recinto", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                Text("${sectores.size} sectores", fontSize = 12.sp, color = CarpayTextSecondary)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CounterBox("$countDisponibles", "Disponibles", CarpayPrimaryGreen, Modifier.weight(1f))
                CounterBox("$countMantencion", "Mantención", CarpayAmberText, Modifier.weight(1f))
                CounterBox("$countInactivos", "Inactivos", CarpayRedText, Modifier.weight(1f))
            }

            val estadoCarga = sectoresState
            if (estadoCarga is NetworkResult.Error) {
                com.altosdecarpay.app.ui.components.MensajeError(estadoCarga.message) { serviciosViewModel.loadSectores() }
            } else if (sectoresState is NetworkResult.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
            } else {
                sectores.forEach { sector ->
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
                                    Icon(Icons.Outlined.Park, null, tint = CarpayPrimaryGreen)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(sector.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
                                    Text(sector.descripcion.ifBlank { "Sector del recinto" }, fontSize = 12.sp, color = CarpayTextSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                val (bg, fg, labelEstado) = when (sector.estado) {
                                    "Disponible" -> Triple(CarpayGreenLight, CarpayPrimaryGreen, "Disponible")
                                    "Mantencion" -> Triple(CarpayAmberBg, CarpayAmberText, "En mantención")
                                    else -> Triple(CarpayRedBg, CarpayRedText, "Inactivo")
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = bg) {
                                    Text(labelEstado, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Text("${com.altosdecarpay.app.ui.utils.Formato.clp(sector.valorHora)} / hora", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CarpayTextPrimary)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (sector.estado == "Disponible") {
                                OutlinedButton(
                                    onClick = { cambioPendiente = sector to "Mantencion" },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, CarpayAmberText.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CarpayAmberText),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.Build, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Poner en mantención", fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Button(
                                    onClick = { cambioPendiente = sector to "Disponible" },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.VerifiedUser, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Habilitar sector", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterBox(count: String, label: String, color: Color, modifier: Modifier = Modifier) {
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
