package com.altosdecarpay.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.data.remote.ApiClient
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.utils.Formato
import java.time.LocalDate

/** Etiqueta de estado con ancho propio: nunca se comprime ni parte el texto en varias líneas. */
@Composable
fun EstadoChip(estado: String, modifier: Modifier = Modifier, etiqueta: String = Formato.estado(estado)) {
    val (fondo, texto) = when (estado) {
        "Pendiente" -> CarpayAmberBg to CarpayAmberText
        "Cancelado", "No se presentó" -> CarpayRedBg to CarpayRedText
        "Respondida" -> CarpayGreenLight to CarpayPrimaryGreen
        else -> CarpayGreenLight to CarpayPrimaryGreen
    }
    Surface(shape = RoundedCornerShape(10.dp), color = fondo, modifier = modifier) {
        Text(
            text = etiqueta,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = texto,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Aviso de error con botón para reintentar (reemplaza los datos de ejemplo que se mostraban antes). */
@Composable
fun MensajeError(mensaje: String, onReintentar: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CarpayRedBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.CloudOff, null, tint = CarpayRedText, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(mensaje, fontSize = 14.sp, color = CarpayRedText, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onReintentar) {
                Text("Reintentar", fontWeight = FontWeight.SemiBold, color = CarpayPrimaryGreen)
            }
        }
    }
}

/** Confirmación antes de cualquier acción que no se puede deshacer. */
@Composable
fun DialogoConfirmacion(
    titulo: String,
    mensaje: String,
    textoConfirmar: String,
    peligro: Boolean = false,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    contenidoExtra: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(mensaje, fontSize = 14.sp)
                contenidoExtra?.invoke()
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (peligro) CarpayRedText else CarpayPrimaryGreen,
                    contentColor = CarpayWhite
                )
            ) { Text(textoConfirmar) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Volver", color = CarpayTextPrimary) } }
    )
}

private data class Aviso(val titulo: String, val detalle: String)

/**
 * Panel de notificaciones de la campana. Se arma con datos que la app ya consulta:
 * cliente → respuestas a sus consultas y pagos por vencer; administrador → consultas pendientes y eventos de hoy.
 */
@Composable
fun NotificacionesDialog(esAdmin: Boolean, onCerrar: () -> Unit) {
    val context = LocalContext.current
    var avisos by remember { mutableStateOf<List<Aviso>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(esAdmin) {
        try {
            val api = ApiClient.getApiService(context)
            val lista = mutableListOf<Aviso>()
            if (esAdmin) {
                val consultas = api.getConsultas(estado = "Pendiente").body()?.results.orEmpty()
                consultas.forEach {
                    lista += Aviso("${Formato.numeroConsulta(it.idConsulta)} sin responder", "${it.clienteNombre ?: "Cliente"} · ${it.asunto}")
                }
                val hoy = LocalDate.now().toString()
                val reservas = api.getReservas(vista = "proximas").body()?.results.orEmpty()
                reservas.filter { it.fecha == hoy }.forEach {
                    lista += Aviso("Evento hoy a las ${Formato.hora(it.horaInicio)}", "${it.idFormateado} · ${it.cliente} · ${it.sectorPrincipal}")
                }
            } else {
                val consultas = api.getConsultas().body()?.results.orEmpty()
                consultas.filter { it.estado == "Respondida" }.take(5).forEach {
                    lista += Aviso("Respondimos tu consulta", it.asunto)
                }
                val reservas = api.getReservas().body()?.results.orEmpty()
                reservas.forEach { r ->
                    when {
                        r.estado == "Pendiente" -> lista += Aviso(
                            "Paga el anticipo de ${r.idFormateado}",
                            "Si no se paga a tiempo la fecha se libera: ${Formato.fechaHora(r.plazoAnticipoVence)}"
                        )
                        r.estado == "Confirmado" && (r.saldoPendiente?.toBigDecimalOrNull()?.signum() ?: 0) > 0 -> lista += Aviso(
                            "Saldo pendiente de ${r.idFormateado}: ${Formato.clp(r.saldoPendiente)}",
                            "Págalo hasta 24 horas antes del evento: ${Formato.fechaHora(r.plazoSaldoVence)}"
                        )
                    }
                }
            }
            avisos = lista
        } catch (e: Exception) {
            error = Formato.mensajeConexion(e)
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Notificaciones", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    error != null -> Text(error!!, color = CarpayRedText, fontSize = 14.sp)
                    avisos == null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CarpayPrimaryGreen)
                    }
                    avisos!!.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.NotificationsNone, null, tint = CarpayTextSecondary)
                        Spacer(Modifier.width(8.dp))
                        Text("No tienes avisos pendientes.", color = CarpayTextSecondary, fontSize = 14.sp)
                    }
                    else -> avisos!!.forEach { aviso ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CarpayWhite,
                            border = BorderStroke(1.dp, CarpayInputBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(aviso.titulo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CarpayTextPrimary)
                                Text(aviso.detalle, fontSize = 12.sp, color = CarpayTextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar", color = CarpayPrimaryGreen) } }
    )
}
