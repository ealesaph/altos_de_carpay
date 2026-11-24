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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.viewmodel.ConsultasViewModel

@Composable
fun UserObservationsScreen(
    reservaId: String,
    consultasViewModel: ConsultasViewModel? = viewModel(),
    onBackClick: () -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    var asunto by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var aviso by remember { mutableStateOf<String?>(null) }
    // Las consultas enviadas desde una reserva llevan su número en el asunto, para que el equipo sepa de cuál se trata.
    val prefijo = "${reservaId}: "

    LaunchedEffect(Unit) {
        consultasViewModel?.loadConsultas()
    }

    val consultasState by consultasViewModel?.consultas?.collectAsState() ?: remember { mutableStateOf(null) }

    val observaciones = remember(consultasState) {
        when (val state = consultasState) {
            is NetworkResult.Success -> state.data.filter { it.asunto.startsWith(prefijo) }.map { c ->
                Triple(
                    "${c.idFormateado} · ${c.asunto.removePrefix(prefijo)}",
                    c.mensaje,
                    c.respuesta?.ifBlank { "Pendiente de respuesta del equipo" } ?: "Pendiente de respuesta del equipo"
                )
            }
            else -> emptyList()
        }
    }

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
                title = "Conversemos",
                subtitle = "Contacta al equipo de Altos de carpay. Estamos para acompañar tu próximo encuentro.",
                showBackButton = true,
                onBackClick = onBackClick
            )

            Surface(shape = RoundedCornerShape(14.dp), color = CarpayGreenLight, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("TU RESERVA · $reservaId", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                    Text("Canal directo con administración", fontFamily = FontFamily.Serif, fontSize = 18.sp, color = CarpayTextPrimary)
                }
            }

            // Formulario Deja una observación
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Escríbenos sobre esta reserva", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CarpayTextPrimary)
                    OutlinedTextField(
                        value = asunto,
                        onValueChange = { asunto = it },
                        placeholder = { Text("Ej.: Preparativos para el almuerzo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mensaje,
                        onValueChange = { mensaje = it },
                        placeholder = { Text("Cuéntanos tu consulta o qué necesitas que tengamos en cuenta...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (asunto.isBlank() || mensaje.isBlank()) {
                                aviso = "Escribe el asunto y el mensaje antes de enviar."
                            } else {
                                consultasViewModel?.crearConsulta(prefijo + asunto.trim(), mensaje.trim()) { ok ->
                                    aviso = if (ok) "Consulta enviada. Te avisaremos por correo cuando la respondamos."
                                    else "No se pudo enviar la consulta. Inténtalo nuevamente."
                                    if (ok) { asunto = ""; mensaje = "" }
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar consulta", fontWeight = FontWeight.SemiBold)
                    }
                    if (aviso != null) Text(aviso!!, fontSize = 13.sp, color = CarpayPrimaryGreen)
                }
            }

            Text("Consultas de esta reserva (${observaciones.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)

            observaciones.forEach { (titulo, consulta, respuesta) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                        Text(consulta, fontSize = 13.sp, color = CarpayTextPrimary)
                        Surface(shape = RoundedCornerShape(10.dp), color = CarpayGreenLight, modifier = Modifier.fillMaxWidth()) {
                            Text(respuesta, fontSize = 12.sp, color = CarpayTextPrimary, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }
    }
}