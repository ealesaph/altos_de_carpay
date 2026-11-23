package com.altosdecarpay.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.data.model.PoliticaPrivacidadResponse
import com.altosdecarpay.app.data.remote.ApiClient
import com.altosdecarpay.app.ui.theme.*

/**
 * Muestra la política de tratamiento de datos personales vigente (Ley N° 21.719), obtenida del
 * servidor para que el titular lea siempre la misma versión que acepta.
 *
 * - Solo lectura: [onAceptar] = null (se muestra un único botón "Cerrar").
 * - Consentimiento: [onAceptar] recibe la versión leída; [onRechazar] permite no aceptar.
 */
@Composable
fun PoliticaPrivacidadDialog(
    onCerrar: () -> Unit,
    onAceptar: ((version: String) -> Unit)? = null,
    onRechazar: (() -> Unit)? = null,
    textoIntro: String? = null,
    procesando: Boolean = false
) {
    val context = LocalContext.current
    val api = remember { ApiClient.getApiService(context) }
    var politica by remember { mutableStateOf<PoliticaPrivacidadResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = api.getPoliticaPrivacidad()
            if (response.isSuccessful) politica = response.body()
            else error = "No se pudo cargar la política de datos. Intenta nuevamente."
        } catch (e: Exception) {
            error = "Sin conexión con el servidor. Intenta nuevamente."
        }
    }

    AlertDialog(
        onDismissRequest = { if (onAceptar == null) onCerrar() },
        containerColor = CarpayWhite,
        title = {
            Text(
                text = politica?.titulo ?: "Política de tratamiento de datos personales",
                color = CarpayTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (textoIntro != null) {
                    Text(textoIntro, color = CarpayAmberText, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                }
                when {
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    politica == null -> CircularProgressIndicator(color = CarpayPrimaryGreen)
                    else -> {
                        val p = politica!!
                        Text(
                            "Versión ${p.version} · Responsable: ${p.responsable["nombre"] ?: ""} " +
                                "(${p.responsable["contacto"] ?: ""})",
                            color = CarpayTextSecondary,
                            fontSize = 12.sp
                        )
                        p.secciones.forEach { seccion ->
                            Spacer(Modifier.height(10.dp))
                            Text(seccion.titulo, color = CarpayTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(seccion.contenido, color = CarpayTextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (onAceptar != null) {
                Button(
                    onClick = { politica?.let { onAceptar(it.version) } },
                    enabled = politica != null && !procesando,
                    colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite)
                ) { Text(if (procesando) "Guardando..." else "Acepto") }
            } else {
                TextButton(onClick = onCerrar) { Text("Cerrar", color = CarpayPrimaryGreen) }
            }
        },
        dismissButton = {
            if (onAceptar != null && onRechazar != null) {
                TextButton(onClick = onRechazar, enabled = !procesando) {
                    Text("No acepto", color = CarpayRedText)
                }
            }
        }
    )
}
