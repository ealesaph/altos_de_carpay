package com.altosdecarpay.app.ui.screens.user

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.viewmodel.ServiciosViewModel

@Composable
fun UserExtraServicesScreen(
    reservaId: String = "RES-0001",
    serviciosViewModel: ServiciosViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onRequestSubmitted: () -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    val serviciosState by serviciosViewModel.servicios.collectAsState()
    val cantidades = remember { mutableStateMapOf<Int, Int>() }

    LaunchedEffect(Unit) {
        serviciosViewModel.loadServicios()
    }

    val servicios = (serviciosState as? NetworkResult.Success)?.data ?: emptyList()

    val totalEstimado = servicios.sumOf { s ->
        val qty = cantidades[s.idServicio] ?: 0
        val precio = s.valor.toDoubleOrNull()?.toInt() ?: 0
        qty * precio
    }

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
                title = "Servicios extra",
                subtitle = "Catálogo disponible para tu reserva $reservaId.",
                showBackButton = true,
                onBackClick = onBackClick
            )

            if (serviciosState is NetworkResult.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
            } else if (servicios.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No hay servicios adicionales activos en este momento.",
                        fontSize = 13.sp,
                        color = CarpayTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                servicios.forEach { prod ->
                    val qty = cantidades[prod.idServicio] ?: 0
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CarpayWhite,
                        border = BorderStroke(1.dp, if (qty > 0) CarpayPrimaryGreen else CarpayInputBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                                Text(
                                    "${prod.descripcion.ifBlank { "Servicio" }} · $${prod.valor}",
                                    fontSize = 12.sp,
                                    color = CarpayTextSecondary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (qty > 0) cantidades[prod.idServicio] = qty - 1 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Outlined.Remove, null, tint = CarpayPrimaryGreen)
                                }
                                Text("$qty", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(
                                    onClick = { cantidades[prod.idServicio] = qty + 1 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, null, tint = CarpayPrimaryGreen)
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total seleccionado", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                        Text("$$totalEstimado", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                    }
                }

                Button(
                    onClick = onRequestSubmitted,
                    enabled = totalEstimado > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Solicitar extras para mi reserva", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}
