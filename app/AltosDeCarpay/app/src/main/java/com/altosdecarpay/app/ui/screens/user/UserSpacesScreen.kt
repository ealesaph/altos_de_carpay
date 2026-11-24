package com.altosdecarpay.app.ui.screens.user

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.components.MensajeError
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.utils.Formato
import com.altosdecarpay.app.ui.viewmodel.ServiciosViewModel

@Composable
fun UserSpacesScreen(
    serviciosViewModel: ServiciosViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onReservar: () -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    val sectoresState by serviciosViewModel.sectores.collectAsState()

    LaunchedEffect(Unit) {
        serviciosViewModel.loadSectores()
    }

    val sectores = (sectoresState as? NetworkResult.Success)?.data ?: emptyList()

    Scaffold(
        topBar = { UserTopBar() },
        bottomBar = { UserBottomBar(selectedTab = UserTab.EXPLORAR, onTabSelected = onTabSelected) },
        containerColor = CarpayBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserScreenHeader(
                title = "Espacios del recinto",
                subtitle = "Sectores disponibles en Altos de Carpay."
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Para compartir y desconectar", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                Text("${sectores.size} espacios", fontSize = 12.sp, color = CarpayTextSecondary)
            }

            val estadoCarga = sectoresState
            if (estadoCarga is NetworkResult.Error) {
                MensajeError(estadoCarga.message) { serviciosViewModel.loadSectores() }
            } else if (sectoresState is NetworkResult.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
            } else {
                sectores.forEachIndexed { index, sector ->
                    val bgColor = if (index % 2 == 0) Color(0xFF3E5641) else Color(0xFF4F6D52)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CarpayWhite,
                        border = BorderStroke(1.dp, CarpayInputBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(bgColor)
                            ) {
                                Icon(Icons.Outlined.Park, null, tint = CarpayWhite.copy(alpha = 0.85f), modifier = Modifier.size(44.dp))
                            }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sector.nombre, fontFamily = FontFamily.Serif, fontSize = 21.sp, color = CarpayTextPrimary)
                                    Text("${Formato.clp(sector.valorHora)} / hora", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CarpayPrimaryGreen)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(sector.descripcion.ifBlank { "Sector del recinto" }, fontSize = 13.sp, color = CarpayTextSecondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sector.estado == "Disponible") CarpayGreenLight else CarpayAmberBg
                                ) {
                                    Text(
                                        if (sector.estado == "Mantencion") "En mantención" else sector.estado,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (sector.estado == "Disponible") CarpayPrimaryGreen else CarpayAmberText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onReservar,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Outlined.EventAvailable, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reservar uno o varios espacios", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}
