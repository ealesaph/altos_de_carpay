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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.viewmodel.ServiciosViewModel

@Composable
fun AdminBitacoraScreen(
    reservaId: String = "RES-0001",
    serviciosViewModel: ServiciosViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    val logsState by serviciosViewModel.logs.collectAsState()

    LaunchedEffect(Unit) {
        serviciosViewModel.loadLogs()
    }

    val logs = (logsState as? NetworkResult.Success)?.data ?: emptyList()

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
                title = "Bitácora de auditoría",
                subtitle = "Registro de modificaciones del sistema en tiempo real.",
                showBackButton = true,
                onBackClick = onBackClick
            )

            Surface(shape = RoundedCornerShape(14.dp), color = CarpayGreenLight, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, tint = CarpayPrimaryGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Uso interno · Auditoría automática", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CarpayTextPrimary)
                        Text("Todas las acciones críticas quedan respaldadas en PostgreSQL.", fontSize = 11.sp, color = CarpayTextSecondary)
                    }
                }
            }

            Text("Historial · ${logs.size} registro(s)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)

            if (logsState is NetworkResult.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
            } else if (logs.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No hay eventos de auditoría registrados aún.",
                        fontSize = 13.sp,
                        color = CarpayTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                logs.forEach { log ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CarpayWhite,
                        border = BorderStroke(1.dp, CarpayInputBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "LOG-${log.idLog} · Tabla: ${log.tablaModificada} (ID #${log.idRegistro})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CarpayTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${log.fecha.take(19).replace("T", " ")} · ${log.usuarioCorreo ?: "Sistema"}",
                                fontSize = 11.sp,
                                color = CarpayTextSecondary
                            )
                            if (log.valorNuevo != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Cambio: ${log.valorNuevo}", fontSize = 12.sp, color = CarpayTextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
