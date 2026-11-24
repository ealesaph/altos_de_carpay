package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.viewmodel.ConsultasViewModel

@Composable
fun AdminTicketsScreen(
    consultasViewModel: ConsultasViewModel = viewModel(),
    onTabSelected: (AdminTab) -> Unit = {}
) {
    var filterSelected by remember { mutableStateOf("Todos") }
    var selectedConsultaId by remember { mutableStateOf<Int?>(null) }
    var respuestaBorrador by remember { mutableStateOf("") }
    var confirmarEnvio by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }

    val consultasState by consultasViewModel.consultas.collectAsState()

    LaunchedEffect(Unit) {
        consultasViewModel.loadConsultas()
    }

    val allConsultas = (consultasState as? NetworkResult.Success)?.data ?: emptyList()
    val filteredConsultas = remember(allConsultas, filterSelected) {
        if (filterSelected == "Pendiente") {
            allConsultas.filter { it.estado == "Pendiente" }
        } else {
            allConsultas
        }
    }

    // La conversación se abre en su propia ventana al tocar una consulta
    // (antes aparecía al final de la lista, fuera de la vista).
    val activeConsulta = remember(allConsultas, selectedConsultaId) {
        allConsultas.firstOrNull { it.idConsulta == selectedConsultaId }
    }

    Scaffold(
        topBar = { AdminTopBar() },
        bottomBar = { AdminBottomBar(selectedTab = AdminTab.TICKETS, onTabSelected = onTabSelected) },
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
                title = "Consultas y reclamos",
                subtitle = "Bandeja de mensajes enviados por los clientes."
            )

            // Filtros
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val countTodos = allConsultas.size
                val countPend = allConsultas.count { it.estado == "Pendiente" }
                listOf("Todos · $countTodos" to "Todos", "Pendientes · $countPend" to "Pendiente").forEach { (label, key) ->
                    val active = filterSelected == key
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (active) CarpayPrimaryGreen else CarpayGreenLight.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { filterSelected = key }
                    ) {
                        Text(
                            text = label,
                            color = if (active) CarpayWhite else CarpayTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (aviso != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = CarpayGreenLight, modifier = Modifier.fillMaxWidth()) {
                    Text(aviso!!, fontSize = 13.sp, color = CarpayPrimaryGreen, modifier = Modifier.padding(12.dp))
                }
            }

            val estadoCarga = consultasState
            if (estadoCarga is NetworkResult.Error) {
                com.altosdecarpay.app.ui.components.MensajeError(estadoCarga.message) { consultasViewModel.loadConsultas() }
            } else if (consultasState is NetworkResult.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
            } else if (filteredConsultas.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Forum, null, tint = CarpayTextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No hay consultas registradas.", fontSize = 14.sp, color = CarpayTextSecondary)
                    }
                }
            } else {
                // Selector de tickets
                filteredConsultas.forEach { item ->
                    val isSelected = item.idConsulta == activeConsulta?.idConsulta
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CarpayWhite,
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) CarpayPrimaryGreen else CarpayInputBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedConsultaId = item.idConsulta; respuestaBorrador = ""; aviso = null }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.idFormateado} · ${item.clienteNombre ?: "Cliente"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarpayPrimaryGreen
                                )
                                Text(item.asunto, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary,
                                    maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text(com.altosdecarpay.app.ui.utils.Formato.fechaHora(item.fechaEnvio), fontSize = 11.sp, color = CarpayTextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            com.altosdecarpay.app.ui.components.EstadoChip(item.estado, etiqueta = com.altosdecarpay.app.ui.utils.Formato.estadoConsulta(item.estado))
                        }
                    }
                }

                if (activeConsulta != null) androidx.compose.ui.window.Dialog(
                    onDismissRequest = { selectedConsultaId = null },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                  Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CarpayBackground,
                    modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 640.dp)
                  ) {
                  Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${activeConsulta.idFormateado} · ${activeConsulta.asunto}",
                            fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { selectedConsultaId = null }) { Icon(Icons.Outlined.Close, "Cerrar") }
                    }
                    if (!activeConsulta.correoRespuesta.isNullOrBlank()) {
                        Text("La respuesta se enviará a ${activeConsulta.correoRespuesta}" +
                            (if (activeConsulta.cliente == null) " (consulta sin cuenta)" else ""), fontSize = 12.sp, color = CarpayTextSecondary)
                    }


                    ChatBubble(
                        author = activeConsulta.clienteNombre ?: "Cliente",
                        message = activeConsulta.mensaje,
                        time = com.altosdecarpay.app.ui.utils.Formato.fechaHora(activeConsulta.fechaEnvio),
                        isAdmin = false
                    )

                    if (!activeConsulta.respuesta.isNullOrBlank()) {
                        ChatBubble(
                            author = "Equipo Altos de carpay",
                            message = activeConsulta.respuesta,
                            time = activeConsulta.fechaRespuesta?.let { com.altosdecarpay.app.ui.utils.Formato.fechaHora(it) } ?: "Respondida",
                            isAdmin = true
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CarpayWhite,
                            border = BorderStroke(1.dp, CarpayInputBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("TU RESPUESTA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarpayTextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = respuestaBorrador,
                                    onValueChange = { respuestaBorrador = it },
                                    placeholder = { Text("Escribe la respuesta para el cliente...") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CarpayPrimaryGreen,
                                        unfocusedBorderColor = CarpayInputBorder
                                    )
                                )
                            }
                        }

                        if (confirmarEnvio) {
                            com.altosdecarpay.app.ui.components.DialogoConfirmacion(
                                titulo = "¿Enviar esta respuesta?",
                                mensaje = "Se enviará por correo a ${activeConsulta.correoRespuesta ?: "la persona"} y la consulta quedará cerrada. No se puede editar después.",
                                textoConfirmar = "Enviar respuesta",
                                onCancelar = { confirmarEnvio = false },
                                onConfirmar = {
                                    confirmarEnvio = false
                                    enviando = true
                                    consultasViewModel.responderConsulta(activeConsulta.idConsulta, respuestaBorrador.trim()) { error ->
                                        enviando = false
                                        if (error == null) {
                                            aviso = "${activeConsulta.idFormateado} respondida."
                                            respuestaBorrador = ""
                                            selectedConsultaId = null
                                        } else aviso = error
                                    }
                                }
                            )
                        }
                        Button(
                            onClick = { confirmarEnvio = true },
                            enabled = respuestaBorrador.isNotBlank() && !enviando,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar respuesta", fontWeight = FontWeight.SemiBold)
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
private fun ChatBubble(author: String, message: String, time: String, isAdmin: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAdmin) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isAdmin) CarpayGreenLight else CarpayWhite,
            border = if (isAdmin) null else BorderStroke(1.dp, CarpayInputBorder),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(author, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Text(message, fontSize = 13.sp, color = CarpayTextPrimary, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(time, fontSize = 11.sp, color = CarpayTextSecondary, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}
