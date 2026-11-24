package com.altosdecarpay.app.ui.screens.user

import androidx.compose.foundation.BorderStroke
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
import com.altosdecarpay.app.data.SessionState
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.components.EstadoChip
import com.altosdecarpay.app.ui.components.MensajeError
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.utils.Formato
import com.altosdecarpay.app.ui.viewmodel.ConsultasViewModel

@Composable
fun UserTicketsScreen(
    consultasViewModel: ConsultasViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToObservations: () -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    var nuevoMensaje by remember { mutableStateOf("") }
    var nuevoAsunto by remember { mutableStateOf("Consulta general") }
    var aviso by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    val consultasState by consultasViewModel.consultas.collectAsState()

    LaunchedEffect(Unit) {
        consultasViewModel.loadConsultas()
    }

    val consultas = (consultasState as? NetworkResult.Success)?.data ?: emptyList()
    val nombreUsuario = SessionState.userName.ifBlank { "Tú" }

    Scaffold(
        topBar = { UserTopBar() },
        bottomBar = { UserBottomBar(selectedTab = UserTab.AYUDA, onTabSelected = onTabSelected) },
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
                title = "Estamos para ayudarte",
                subtitle = "Tus consultas y respuestas del equipo de Altos de Carpay"
            )

            val estadoCarga = consultasState
            if (estadoCarga is NetworkResult.Error) {
                MensajeError(estadoCarga.message) { consultasViewModel.loadConsultas() }
            } else if (consultasState is NetworkResult.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
            } else if (consultas.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aún no has enviado consultas. Escribe abajo para comunicarte con el equipo.",
                        fontSize = 13.sp,
                        color = CarpayTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                consultas.forEach { consulta ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // El asunto ocupa el espacio libre y se corta en dos líneas; la etiqueta conserva su ancho.
                        Text(
                            "${consulta.idFormateado} · ${consulta.asunto}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CarpayTextPrimary,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        EstadoChip(consulta.estado, etiqueta = com.altosdecarpay.app.ui.utils.Formato.estadoConsulta(consulta.estado))
                    }

                    UserChatBubble(
                        author = nombreUsuario,
                        message = consulta.mensaje,
                        time = Formato.fechaHora(consulta.fechaEnvio),
                        isUser = true
                    )

                    if (!consulta.respuesta.isNullOrBlank()) {
                        UserChatBubble(
                            author = "Equipo Altos de carpay",
                            message = consulta.respuesta,
                            time = consulta.fechaRespuesta?.let { Formato.fechaHora(it) } ?: "Respondida",
                            isUser = false
                        )
                    }
                    HorizontalDivider(color = CarpayInputBorder)
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nuevoAsunto,
                        onValueChange = { nuevoAsunto = it },
                        label = { Text("Asunto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nuevoMensaje,
                        onValueChange = { nuevoMensaje = it },
                        placeholder = { Text("Escribe tu mensaje...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (nuevoMensaje.isBlank() || nuevoAsunto.isBlank()) {
                                    aviso = "Escribe el asunto y el mensaje antes de enviar."
                                    return@TextButton
                                }
                                enviando = true
                                consultasViewModel.crearConsulta(nuevoAsunto.trim(), nuevoMensaje.trim()) { ok ->
                                    enviando = false
                                    aviso = if (ok) "Consulta enviada. Te avisaremos por correo cuando la respondamos." else
                                        ((consultasViewModel.crearResult.value as? NetworkResult.Error)?.message ?: "No se pudo enviar la consulta.")
                                    if (ok) nuevoMensaje = ""
                                }
                            },
                            enabled = !enviando
                        ) {
                            Text("Enviar", fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Outlined.Send, null, tint = CarpayPrimaryGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (aviso != null) {
                Text(aviso!!, fontSize = 13.sp, color = CarpayPrimaryGreen)
            }
        }
    }
}

@Composable
private fun UserChatBubble(author: String, message: String, time: String, isUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isUser) CarpayGreenLight else CarpayWhite,
            border = if (isUser) null else BorderStroke(1.dp, CarpayInputBorder),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(author, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Text(message, fontSize = 13.sp, color = CarpayTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(time, fontSize = 11.sp, color = CarpayTextSecondary, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}
