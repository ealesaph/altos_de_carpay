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

    val activeConsulta = remember(filteredConsultas, selectedConsultaId) {
        filteredConsultas.firstOrNull { it.idConsulta == selectedConsultaId } ?: filteredConsultas.firstOrNull()
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

            if (consultasState is NetworkResult.Loading) {
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
                            .clickable { selectedConsultaId = item.idConsulta }
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
                                Text(item.asunto, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                                Text(item.fechaEnvio.take(10), fontSize = 11.sp, color = CarpayTextSecondary)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (item.estado == "Respondida") CarpayGreenLight else CarpayAmberBg
                            ) {
                                Text(
                                    text = item.estado,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.estado == "Respondida") CarpayPrimaryGreen else CarpayAmberText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                if (activeConsulta != null) {
                    HorizontalDivider(color = CarpayInputBorder)

                    Text(
                        text = "Conversación · ${activeConsulta.idFormateado}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CarpayTextPrimary
                    )

                    ChatBubble(
                        author = activeConsulta.clienteNombre ?: "Cliente",
                        message = activeConsulta.mensaje,
                        time = activeConsulta.fechaEnvio.take(16).replace("T", " "),
                        isAdmin = false
                    )

                    if (!activeConsulta.respuesta.isNullOrBlank()) {
                        ChatBubble(
                            author = "Equipo Altos de carpay",
                            message = activeConsulta.respuesta,
                            time = activeConsulta.fechaRespuesta?.take(16)?.replace("T", " ") ?: "Respondida",
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

                        Button(
                            onClick = {
                                if (respuestaBorrador.isNotBlank()) {
                                    consultasViewModel.responderConsulta(activeConsulta.idConsulta, respuestaBorrador)
                                    respuestaBorrador = ""
                                }
                            },
                            enabled = respuestaBorrador.isNotBlank(),
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
