package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.altosdecarpay.app.ui.utils.QrCodeGenerator
import com.altosdecarpay.app.ui.viewmodel.ReservasViewModel
import com.altosdecarpay.app.ui.viewmodel.ServiciosViewModel

@Composable
fun AdminBitacoraScreen(
    reservaId: String = "RES-0001",
    serviciosViewModel: ServiciosViewModel = viewModel(),
    reservasViewModel: ReservasViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    val numericId = remember(reservaId) { QrCodeGenerator.parseReservaId(reservaId) ?: 1 }
    val incidenciasState by serviciosViewModel.incidencias.collectAsState()
    val detalleState by reservasViewModel.reservaDetalle.collectAsState()

    val sectoresState by serviciosViewModel.sectores.collectAsState()

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    // HU-05: área donde ocurrió la incidencia y alerta si no estaba contratada.
    var sectorSeleccionado by remember { mutableStateOf<Int?>(null) }
    var alertaArea by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(numericId) {
        serviciosViewModel.loadIncidencias(numericId)
        reservasViewModel.loadReservaDetalle(numericId)
        serviciosViewModel.loadSectores()
    }

    val incidencias = (incidenciasState as? NetworkResult.Success)?.data ?: emptyList()
    val reserva = (detalleState as? NetworkResult.Success)?.data
    val esSoloLectura = reserva?.estado == "Finalizado"
    val sectores = (sectoresState as? NetworkResult.Success)?.data ?: emptyList()
    val contratados = reserva?.sectores?.map { it.idSector }?.toSet() ?: emptySet()

    if (alertaArea != null) {
        AlertDialog(
            onDismissRequest = { alertaArea = null },
            icon = { Icon(Icons.Outlined.Warning, null, tint = CarpayRedText) },
            title = { Text("Área no contratada", fontWeight = FontWeight.Bold) },
            text = { Text(alertaArea!!) },
            confirmButton = {
                TextButton(onClick = { alertaArea = null }) { Text("Entendido") }
            }
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
                subtitle = "Observaciones e incumplimientos de la reserva $reservaId.",
                showBackButton = true,
                onBackClick = onBackClick
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (esSoloLectura) CarpayAmberBg else CarpayGreenLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Lock,
                        null,
                        tint = if (esSoloLectura) CarpayAmberText else CarpayPrimaryGreen
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (esSoloLectura) "Modo Solo Lectura · Evento Finalizado" else "Uso interno · Registro inmutable",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (esSoloLectura) CarpayAmberText else CarpayTextPrimary
                        )
                        Text(
                            text = if (esSoloLectura) "Conforme a la Ley N° 19.799, esta bitácora no admite cambios." else "Cada registro incluye marca de tiempo del servidor y autor.",
                            fontSize = 11.sp,
                            color = if (esSoloLectura) CarpayAmberText else CarpayTextSecondary
                        )
                    }
                }
            }

            Text("Historial · ${incidencias.size} registro(s)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)

            if (incidenciasState is NetworkResult.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
            } else if (incidencias.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No hay incidencias registradas para esta reserva.",
                        fontSize = 13.sp,
                        color = CarpayTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                incidencias.forEach { inc ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CarpayWhite,
                        border = BorderStroke(1.dp, CarpayInputBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("${inc.idFormateado} · ${inc.titulo}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${inc.fechaCreacion.take(19).replace("T", " ")} · ${inc.autorNombre ?: "Equipo Admin"}",
                                fontSize = 11.sp,
                                color = CarpayTextSecondary
                            )
                            if (inc.sectorNombre != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (inc.areaNoContratada) "Área: ${inc.sectorNombre} · No contratada" else "Área: ${inc.sectorNombre}",
                                    fontSize = 12.sp,
                                    fontWeight = if (inc.areaNoContratada) FontWeight.Bold else FontWeight.Normal,
                                    color = if (inc.areaNoContratada) CarpayRedText else CarpayTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(inc.descripcion, fontSize = 13.sp, color = CarpayTextPrimary)
                        }
                    }
                }
            }

            if (!esSoloLectura) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Nueva observación", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CarpayTextPrimary)
                        OutlinedTextField(
                            value = titulo,
                            onValueChange = {
                                titulo = it
                                errorMsg = null
                            },
                            placeholder = { Text("Título breve (ej: Atraso en salida / Daño menor)", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = descripcion,
                            onValueChange = {
                                descripcion = it
                                errorMsg = null
                            },
                            placeholder = { Text("Describe lo observado de forma neutral...", fontSize = 13.sp) },
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (sectores.isNotEmpty()) {
                            Text("Área afectada (opcional)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CarpayTextPrimary)
                            sectores.forEach { sector ->
                                val seleccionado = sectorSeleccionado == sector.idSector
                                val contratado = sector.idSector in contratados
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (seleccionado) CarpayGreenLight else CarpayWhite,
                                    border = BorderStroke(1.dp, if (seleccionado) CarpayPrimaryGreen else CarpayInputBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            sectorSeleccionado = if (seleccionado) null else sector.idSector
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(sector.nombre, fontSize = 13.sp, color = CarpayTextPrimary, modifier = Modifier.weight(1f))
                                        Text(
                                            text = if (contratado) "Contratada" else "No contratada",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (contratado) CarpayPrimaryGreen else CarpayRedText
                                        )
                                    }
                                }
                            }
                        }
                        if (errorMsg != null) {
                            Text(errorMsg!!, color = CarpayRedText, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (titulo.isNotBlank() && descripcion.isNotBlank()) {
                                    serviciosViewModel.crearIncidencia(
                                        reservaId = numericId,
                                        titulo = titulo,
                                        descripcion = descripcion,
                                        sectorId = sectorSeleccionado,
                                        onSuccess = { creada ->
                                            titulo = ""
                                            descripcion = ""
                                            sectorSeleccionado = null
                                            if (creada?.areaNoContratada == true) {
                                                alertaArea = creada.alerta
                                                    ?: "El área ${creada.sectorNombre ?: ""} no forma parte de la reserva."
                                            }
                                        },
                                        onError = { errorMsg = it }
                                    )
                                }
                            },
                            enabled = titulo.isNotBlank() && descripcion.isNotBlank(),
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
}
