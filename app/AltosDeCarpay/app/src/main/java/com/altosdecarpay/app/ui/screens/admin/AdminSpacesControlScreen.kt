package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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
    var sectorAEliminar by remember { mutableStateOf<com.altosdecarpay.app.data.model.SectorResponse?>(null) }
    var sectorAEditar by remember { mutableStateOf<com.altosdecarpay.app.data.model.SectorResponse?>(null) }
    var mostrarCrearDialog by remember { mutableStateOf(false) }
    var errorAccion by remember { mutableStateOf<String?>(null) }

    cambioPendiente?.let { (sector, nuevo) ->
        val (titulo, mensaje, textoConfirmar) = when (nuevo) {
            "Mantencion" -> Triple(
                "¿Poner «${sector.nombre}» en mantención?",
                "El sector dejará de ofrecerse en la web y en la app para todas las fechas, no solo para esta reserva. Las reservas ya hechas no se cancelan.",
                "Poner en mantención"
            )
            "Inactivo" -> Triple(
                "¿Inhabilitar «${sector.nombre}»?",
                "El sector dejará de mostrarse en el catálogo público y no recibirá nuevas reservas. Las reservas ya hechas no se cancelan.",
                "Inhabilitar"
            )
            else -> Triple(
                "¿Habilitar «${sector.nombre}»?",
                "El sector volverá a ofrecerse en la web y en la app para nuevas reservas.",
                "Habilitar"
            )
        }
        com.altosdecarpay.app.ui.components.DialogoConfirmacion(
            titulo = titulo,
            mensaje = mensaje,
            textoConfirmar = textoConfirmar,
            peligro = nuevo != "Disponible",
            onCancelar = { cambioPendiente = null },
            onConfirmar = {
                cambioPendiente = null
                serviciosViewModel.actualizarEstadoSector(sector.idSector, nuevo)
            }
        )
    }

    sectorAEliminar?.let { sector ->
        com.altosdecarpay.app.ui.components.DialogoConfirmacion(
            titulo = "¿Eliminar «${sector.nombre}»?",
            mensaje = "El espacio se eliminará del catálogo. Si tiene reservas o incidencias asociadas, no podrá eliminarse.",
            textoConfirmar = "Eliminar",
            peligro = true,
            onCancelar = { sectorAEliminar = null },
            onConfirmar = {
                sectorAEliminar = null
                errorAccion = null
                serviciosViewModel.eliminarSector(sector.idSector) { ok, err ->
                    if (!ok && err != null) errorAccion = err
                }
            }
        )
    }

    if (mostrarCrearDialog) {
        var nombre by remember { mutableStateOf("") }
        var descripcion by remember { mutableStateOf("") }
        var valorHora by remember { mutableStateOf("") }
        var guardando by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!guardando) mostrarCrearDialog = false },
            containerColor = CarpayWhite,
            title = { Text("Agregar espacio", fontWeight = FontWeight.Bold, color = CarpayTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre del espacio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = valorHora,
                        onValueChange = { valorHora = it },
                        label = { Text("Tarifa por hora (CLP)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción (opcional)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = nombre.isNotBlank() && valorHora.isNotBlank() && !guardando,
                    colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen),
                    onClick = {
                        guardando = true
                        errorAccion = null
                        serviciosViewModel.crearSector(nombre, descripcion, valorHora) { ok, err ->
                            guardando = false
                            if (ok) {
                                mostrarCrearDialog = false
                            } else if (err != null) {
                                errorAccion = err
                                mostrarCrearDialog = false
                            }
                        }
                    }
                ) {
                    Text(if (guardando) "Guardando..." else "Crear")
                }
            },
            dismissButton = {
                TextButton(enabled = !guardando, onClick = { mostrarCrearDialog = false }) {
                    Text("Cancelar", color = CarpayTextSecondary)
                }
            }
        )
    }

    sectorAEditar?.let { sector ->
        var nombre by remember(sector.idSector) { mutableStateOf(sector.nombre) }
        var descripcion by remember(sector.idSector) { mutableStateOf(sector.descripcion) }
        var valorHora by remember(sector.idSector) {
            mutableStateOf(sector.valorHora.substringBefore("."))
        }
        var estado by remember(sector.idSector) { mutableStateOf(sector.estado) }
        var guardando by remember(sector.idSector) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!guardando) sectorAEditar = null },
            containerColor = CarpayWhite,
            title = { Text("Modificar espacio", fontWeight = FontWeight.Bold, color = CarpayTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre del espacio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = valorHora,
                        onValueChange = { valorHora = it },
                        label = { Text("Tarifa por hora (CLP)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Disponible" to "Habilitado",
                            "Mantencion" to "Mantención",
                            "Inactivo" to "Inhabilitado"
                        ).forEach { (valor, etiqueta) ->
                            FilterChip(
                                selected = estado == valor,
                                onClick = { estado = valor },
                                label = { Text(etiqueta, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = nombre.isNotBlank() && valorHora.isNotBlank() && !guardando,
                    colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen),
                    onClick = {
                        guardando = true
                        errorAccion = null
                        serviciosViewModel.actualizarSector(
                            id = sector.idSector,
                            nombre = nombre,
                            descripcion = descripcion,
                            valorHora = valorHora,
                            estado = estado
                        ) { ok, err ->
                            guardando = false
                            if (ok) {
                                sectorAEditar = null
                            } else if (err != null) {
                                errorAccion = err
                                sectorAEditar = null
                            }
                        }
                    }
                ) {
                    Text(if (guardando) "Guardando..." else "Guardar")
                }
            },
            dismissButton = {
                TextButton(enabled = !guardando, onClick = { sectorAEditar = null }) {
                    Text("Cancelar", color = CarpayTextSecondary)
                }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Sectores del recinto", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                    Text("${sectores.size} sectores", fontSize = 12.sp, color = CarpayTextSecondary)
                }
                Button(
                    onClick = { mostrarCrearDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite)
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            errorAccion?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CarpayRedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        msg,
                        color = CarpayRedText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sector.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
                                    Text(sector.descripcion.ifBlank { "Sector del recinto" }, fontSize = 12.sp, color = CarpayTextSecondary)
                                }
                                IconButton(onClick = { sectorAEditar = sector }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Modificar espacio", tint = CarpayPrimaryGreen)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                val (bg, fg, labelEstado) = when (sector.estado) {
                                    "Disponible" -> Triple(CarpayGreenLight, CarpayPrimaryGreen, "Habilitado")
                                    "Mantencion" -> Triple(CarpayAmberBg, CarpayAmberText, "En mantención")
                                    else -> Triple(CarpayRedBg, CarpayRedText, "Inhabilitado")
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = bg) {
                                    Text(labelEstado, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Text("${com.altosdecarpay.app.ui.utils.Formato.clp(sector.valorHora)} / hora", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CarpayTextPrimary)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (sector.estado == "Disponible") {
                                    OutlinedButton(
                                        onClick = { cambioPendiente = sector to "Mantencion" },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, CarpayAmberText.copy(alpha = 0.4f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CarpayAmberText),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.Build, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mantención", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = { cambioPendiente = sector to "Inactivo" },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, CarpayRedText.copy(alpha = 0.4f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CarpayRedText),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.Block, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Inhabilitar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    Button(
                                        onClick = { cambioPendiente = sector to "Disponible" },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.VerifiedUser, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Habilitar", fontWeight = FontWeight.SemiBold)
                                    }
                                    if (sector.estado == "Mantencion") {
                                        OutlinedButton(
                                            onClick = { cambioPendiente = sector to "Inactivo" },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, CarpayRedText.copy(alpha = 0.4f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CarpayRedText),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Outlined.Block, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Inhabilitar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = { sectorAEliminar = sector },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, CarpayRedText.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CarpayRedText)
                                ) {
                                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp))
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
