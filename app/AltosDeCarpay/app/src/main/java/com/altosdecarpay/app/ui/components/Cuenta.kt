package com.altosdecarpay.app.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.data.SessionState
import com.altosdecarpay.app.data.model.CambioPasswordRequest
import com.altosdecarpay.app.data.model.PerfilResponse
import com.altosdecarpay.app.data.remote.ApiClient
import com.altosdecarpay.app.ui.screens.PoliticaPrivacidadDialog
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.utils.Formato
import kotlinx.coroutines.launch

/** Tarjeta con los datos del usuario (RUT y teléfono con formato chileno). */
@Composable
fun TarjetaPerfil(perfil: PerfilResponse?, rolVisible: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CarpayWhite,
        border = BorderStroke(1.dp, CarpayInputBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (perfil == null) {
                Text("Cargando tus datos…", fontSize = 14.sp, color = CarpayTextSecondary)
                return@Column
            }
            Text(perfil.nombreCompleto, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CarpayTextPrimary)
            Text(rolVisible, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CarpayPrimaryGreen)
            Spacer(Modifier.height(6.dp))
            Text(perfil.correo, fontSize = 13.sp, color = CarpayTextSecondary)
            Text(perfil.telefono.takeIf { it.isNotBlank() }?.let { Formato.telefono(it) } ?: "Sin teléfono registrado", fontSize = 13.sp, color = CarpayTextSecondary)
            if (perfil.rut.isNotBlank()) Text("RUT ${Formato.rut(perfil.rut)}", fontSize = 13.sp, color = CarpayTextSecondary)
        }
    }
}

@Composable
private fun OpcionCuenta(icono: ImageVector, titulo: String, detalle: String, color: Color = CarpayTextPrimary, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CarpayWhite,
        border = BorderStroke(1.dp, CarpayInputBorder),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = color)
                Text(detalle, fontSize = 12.sp, color = CarpayTextSecondary)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = CarpayTextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Opciones de la cuenta propia (antes el perfil solo permitía cerrar sesión):
 * editar datos, cambiar contraseña, ver la política de datos, descargar mis datos y cerrar la cuenta
 * (esto último solo para clientes, igual que en la web).
 */
@Composable
fun OpcionesCuenta(
    perfil: PerfilResponse?,
    esCliente: Boolean,
    onPerfilActualizado: (PerfilResponse) -> Unit,
    onSesionTerminada: (String) -> Unit
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getApiService(context) }
    val scope = rememberCoroutineScope()
    var dialogo by remember { mutableStateOf<String?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var procesando by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OpcionCuenta(Icons.Outlined.Edit, "Editar mis datos", "Nombre, apellidos y teléfono") { dialogo = "datos" }
        OpcionCuenta(Icons.Outlined.Key, "Cambiar contraseña", "Se cerrará la sesión en todos tus dispositivos") { dialogo = "clave" }
        OpcionCuenta(Icons.Outlined.Shield, "Política de datos personales", "Cómo tratamos tus datos (Ley N° 21.719)") { dialogo = "politica" }
        OpcionCuenta(Icons.Outlined.Download, "Descargar mis datos", "Copia de tus datos para guardar o compartir") {
            scope.launch {
                try {
                    val r = api.getMisDatosArco()
                    if (r.isSuccessful) {
                        val json = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(r.body())
                        val envio = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "Mis datos en Altos de Carpay")
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(envio, "Guardar o compartir mis datos"))
                    } else aviso = Formato.mensajeError(r.errorBody()?.string(), "No se pudieron obtener tus datos.")
                } catch (e: Exception) {
                    aviso = Formato.mensajeConexion(e)
                }
            }
        }
        if (esCliente) {
            OpcionCuenta(Icons.Outlined.PersonOff, "Cerrar mi cuenta", "Desactiva tu cuenta; tu historial se conserva", CarpayRedText) { dialogo = "cerrar" }
        }
        if (aviso != null) Text(aviso!!, fontSize = 13.sp, color = CarpayPrimaryGreen)
    }

    when (dialogo) {
        "datos" -> if (perfil != null) {
            var nombre by remember { mutableStateOf(perfil.nombre) }
            var paterno by remember { mutableStateOf(perfil.apellidoPaterno) }
            var materno by remember { mutableStateOf(perfil.apellidoMaterno) }
            var telefono by remember { mutableStateOf(perfil.telefono) }
            AlertDialog(
                onDismissRequest = { dialogo = null },
                title = { Text("Editar mis datos", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true)
                        OutlinedTextField(paterno, { paterno = it }, label = { Text("Apellido paterno") }, singleLine = true)
                        OutlinedTextField(materno, { materno = it }, label = { Text("Apellido materno") }, singleLine = true)
                        OutlinedTextField(
                            telefono, { telefono = it }, label = { Text("Teléfono") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }
                },
                confirmButton = {
                    Button(enabled = !procesando, onClick = {
                        procesando = true
                        scope.launch {
                            try {
                                val r = api.updatePerfil(mapOf(
                                    "nombre" to nombre.trim(), "apellido_paterno" to paterno.trim(),
                                    "apellido_materno" to materno.trim(), "telefono" to telefono.trim()
                                ))
                                if (r.isSuccessful && r.body() != null) {
                                    SessionState.currentUser = r.body()
                                    onPerfilActualizado(r.body()!!)
                                    aviso = "Datos actualizados."
                                    dialogo = null
                                } else aviso = Formato.mensajeError(r.errorBody()?.string(), "No se pudieron guardar los cambios.")
                            } catch (e: Exception) {
                                aviso = Formato.mensajeConexion(e)
                            } finally { procesando = false }
                        }
                    }) { Text("Guardar") }
                },
                dismissButton = { TextButton(onClick = { dialogo = null }) { Text("Cancelar") } }
            )
        }
        "clave" -> {
            var actual by remember { mutableStateOf("") }
            var nueva by remember { mutableStateOf("") }
            var error by remember { mutableStateOf<String?>(null) }
            AlertDialog(
                onDismissRequest = { dialogo = null },
                title = { Text("Cambiar contraseña", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(actual, { actual = it }, label = { Text("Contraseña actual") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation())
                        OutlinedTextField(nueva, { nueva = it }, label = { Text("Nueva contraseña") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation())
                        Text("Al cambiarla se cerrará tu sesión y deberás ingresar de nuevo.", fontSize = 12.sp, color = CarpayTextSecondary)
                        if (error != null) Text(error!!, fontSize = 12.sp, color = CarpayRedText)
                    }
                },
                confirmButton = {
                    Button(enabled = !procesando && actual.isNotBlank() && nueva.isNotBlank(), onClick = {
                        procesando = true
                        scope.launch {
                            try {
                                val r = api.cambiarPassword(CambioPasswordRequest(actual, nueva))
                                if (r.isSuccessful) {
                                    dialogo = null
                                    onSesionTerminada("Contraseña actualizada. Inicia sesión con la nueva contraseña.")
                                } else error = Formato.mensajeError(r.errorBody()?.string(), "No se pudo cambiar la contraseña.")
                            } catch (e: Exception) {
                                error = Formato.mensajeConexion(e)
                            } finally { procesando = false }
                        }
                    }) { Text("Cambiar") }
                },
                dismissButton = { TextButton(onClick = { dialogo = null }) { Text("Cancelar") } }
            )
        }
        "politica" -> PoliticaPrivacidadDialog(onCerrar = { dialogo = null })
        "cerrar" -> DialogoConfirmacion(
            titulo = "¿Cerrar tu cuenta?",
            mensaje = "Tu cuenta quedará desactivada y no podrás volver a ingresar. Tus reservas se conservan por obligaciones legales. Esta acción no se puede deshacer desde la app.",
            textoConfirmar = "Cerrar mi cuenta",
            peligro = true,
            onCancelar = { dialogo = null },
            onConfirmar = {
                dialogo = null
                scope.launch {
                    try {
                        val r = api.desactivarCuentaArco()
                        if (r.isSuccessful) onSesionTerminada("Tu cuenta fue cerrada.")
                        else aviso = Formato.mensajeError(r.errorBody()?.string(), "No se pudo cerrar la cuenta.")
                    } catch (e: Exception) {
                        aviso = Formato.mensajeConexion(e)
                    }
                }
            }
        )
    }
}
