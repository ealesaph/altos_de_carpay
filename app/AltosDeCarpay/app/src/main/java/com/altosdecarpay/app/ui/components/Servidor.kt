package com.altosdecarpay.app.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.data.remote.ApiClient
import com.altosdecarpay.app.ui.theme.*

/**
 * Enlace «Servidor: …» de la pantalla de inicio de sesión. Permite usar la app en el emulador y en un
 * celular a la vez: el emulador usa 10.0.2.2, el celular por USB usa 127.0.0.1 (con `adb reverse`) y,
 * por Wi-Fi, se escribe la IP local del PC.
 */
@Composable
fun EnlaceServidor(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var abierto by remember { mutableStateOf(false) }
    val host = remember(abierto) { android.net.Uri.parse(ApiClient.getBaseUrl(context)).let { "${it.host}:${if (it.port == -1) 80 else it.port}" } }

    TextButton(onClick = { abierto = true }, modifier = modifier) {
        Text("Servidor: $host", fontSize = 12.sp, color = CarpayTextSecondary)
    }
    if (abierto) ServidorDialog(onCerrar = { abierto = false }, onGuardado = {
        abierto = false
        // Se recrea la pantalla para usar la nueva dirección y, si es una IP de la red local en Android 17,
        // pedir el permiso de red local.
        (context as? Activity)?.recreate()
    })
}

@Composable
private fun ServidorDialog(onCerrar: () -> Unit, onGuardado: () -> Unit) {
    val context = LocalContext.current
    var texto by remember { mutableStateOf(ApiClient.getBaseUrl(context)) }
    var error by remember { mutableStateOf<String?>(null) }
    val predeterminada = ApiClient.DEFAULT_BASE_URL

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Dirección del servidor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (ApiClient.esEmulador()) "Estás en el emulador: lo normal es 10.0.2.2:8000."
                    else "Celular por USB: 127.0.0.1:8000 (en el PC: adb reverse tcp:8000 tcp:8000 y tcp:80 tcp:80). Por Wi-Fi: la IP local del PC, por ejemplo 192.168.1.20.",
                    fontSize = 13.sp, color = CarpayTextSecondary
                )
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it; error = null },
                    singleLine = true,
                    label = { Text("Dirección") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                if (ApiClient.tieneUrlPersonalizada(context)) {
                    TextButton(onClick = { ApiClient.restablecerBaseUrl(context); onGuardado() }) {
                        Text("Usar la predeterminada (${android.net.Uri.parse(predeterminada).host})")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val url = ApiClient.normalizarUrl(texto)
                if (url == null) error = "Escribe una dirección como 192.168.1.20 o 192.168.1.20:8000"
                else {
                    if (url == predeterminada) ApiClient.restablecerBaseUrl(context) else ApiClient.setBaseUrl(context, url)
                    onGuardado()
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
