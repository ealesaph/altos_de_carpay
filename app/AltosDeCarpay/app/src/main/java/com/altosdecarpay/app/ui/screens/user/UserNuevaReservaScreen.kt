package com.altosdecarpay.app.ui.screens.user

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.model.ReservaCrearRequest
import com.altosdecarpay.app.data.model.SectorEntradaRequest
import com.altosdecarpay.app.data.model.SectorResponse
import com.altosdecarpay.app.data.model.ServicioEntradaRequest
import com.altosdecarpay.app.data.model.ServicioResponse
import com.altosdecarpay.app.data.remote.ApiClient
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.components.MensajeError
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.utils.Formato
import com.altosdecarpay.app.ui.utils.ReglasArriendo
import com.altosdecarpay.app.ui.viewmodel.ReservasViewModel
import com.altosdecarpay.app.ui.viewmodel.ServiciosViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Nueva reserva desde la app. Se pueden elegir uno o varios espacios a la vez: todos quedan en la
 * misma reserva, con la misma fecha, hora de inicio y cantidad de horas (igual que en la web).
 * Al crearla se pasa al pago del anticipo o del total (UserPagoScreen).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserNuevaReservaScreen(
    sectorInicial: Int? = null,
    serviciosViewModel: ServiciosViewModel = viewModel(),
    reservasViewModel: ReservasViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onReservaCreada: (Int) -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    val sectoresState by serviciosViewModel.sectores.collectAsState()
    val serviciosState by serviciosViewModel.servicios.collectAsState()
    LaunchedEffect(Unit) {
        serviciosViewModel.loadSectores()
        serviciosViewModel.loadServicios()
    }
    val sectores = ((sectoresState as? NetworkResult.Success)?.data ?: emptyList()).filter { it.estado == "Disponible" }
    val servicios = ((serviciosState as? NetworkResult.Success)?.data ?: emptyList()).filter { it.estado == "Activo" }

    var fecha by remember { mutableStateOf(ReglasArriendo.fechaMinima()) }
    var horaInicio by remember { mutableIntStateOf(13) }
    var horas by remember { mutableIntStateOf(5) }
    val seleccionados = remember { mutableStateListOf<Int>().apply { sectorInicial?.let { add(it) } } }
    val cantidades = remember { mutableStateMapOf<Int, Int>() }
    var aceptaCondiciones by remember { mutableStateOf(false) }
    var mostrarCalendario by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val horasMaximas = ReglasArriendo.maximoHoras(horaInicio)
    val elegidos = sectores.filter { it.idSector in seleccionados }
    fun valor(texto: String) = texto.toBigDecimalOrNull()?.toLong() ?: 0L
    val totalEspacios = elegidos.sumOf { valor(it.valorHora) * horas }
    val totalServicios = servicios.sumOf { valor(it.valor) * (cantidades[it.idServicio] ?: 0) }
    val totalEstimado = totalEspacios + totalServicios

    Scaffold(
        topBar = { UserTopBar() },
        bottomBar = { UserBottomBar(selectedTab = UserTab.RESERVAS, onTabSelected = onTabSelected) },
        containerColor = CarpayBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserScreenHeader(
                title = "Nueva reserva",
                subtitle = "Se arrienda por horas completas: bloque ${ReglasArriendo.textoBloques}.",
                showBackButton = true,
                onBackClick = onBackClick
            )

            val errorCarga = sectoresState as? NetworkResult.Error
            if (errorCarga != null) {
                MensajeError(errorCarga.message) { serviciosViewModel.loadSectores(); serviciosViewModel.loadServicios() }
                return@Column
            }
            if (sectoresState is NetworkResult.Loading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
                return@Column
            }

            // 1. Fecha y horario
            Seccion("1. Fecha y horario") {
                OutlinedButton(
                    onClick = { mostrarCalendario = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.CalendarMonth, null, tint = CarpayPrimaryGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Formato.fecha(fecha.toString()), color = CarpayTextPrimary, modifier = Modifier.weight(1f))
                    Text("Cambiar", color = CarpayPrimaryGreen, fontSize = 13.sp)
                }

                Text("Hora de inicio", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CarpayTextPrimary)
                ReglasArriendo.bloques.forEach { bloque ->
                    Text("Bloque ${bloque.nombre.lowercase()}", fontSize = 12.sp, color = CarpayTextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReglasArriendo.horasDeInicio(bloque).forEach { h ->
                            FilterChip(
                                selected = horaInicio == h,
                                onClick = {
                                    horaInicio = h
                                    horas = horas.coerceAtMost(ReglasArriendo.maximoHoras(h))
                                },
                                label = { Text(ReglasArriendo.textoHora(h)) }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Duración", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CarpayTextPrimary)
                        Text(
                            "${ReglasArriendo.textoHora(horaInicio)} – ${ReglasArriendo.horaTermino(horaInicio, horas)} · máximo $horasMaximas h",
                            fontSize = 12.sp, color = CarpayTextSecondary
                        )
                    }
                    Contador(valor = horas, minimo = 1, maximo = horasMaximas, sufijo = if (horas == 1) "hora" else "horas") { horas = it }
                }
            }

            // 2. Espacios (uno o varios)
            Seccion("2. Espacios", "Elige uno o varios; todos se reservan en el mismo horario.") {
                if (sectores.isEmpty()) {
                    Text("No hay espacios disponibles para reservar.", fontSize = 13.sp, color = CarpayTextSecondary)
                }
                sectores.forEach { sector ->
                    TarjetaEspacio(
                        sector = sector,
                        horas = horas,
                        seleccionado = sector.idSector in seleccionados,
                        onCambiar = { marcado ->
                            if (marcado) seleccionados.add(sector.idSector) else seleccionados.remove(sector.idSector)
                            error = null
                        }
                    )
                }
            }

            // 3. Servicios adicionales
            if (servicios.isNotEmpty()) {
                Seccion("3. Servicios adicionales", "Opcional. Puedes indicar la cantidad.") {
                    servicios.forEach { servicio ->
                        FilaServicio(servicio, cantidades[servicio.idServicio] ?: 0) { cantidades[servicio.idServicio] = it }
                    }
                }
            }

            // Resumen
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Resumen", fontWeight = FontWeight.Bold, color = CarpayTextPrimary)
                    Text(
                        "${Formato.fecha(fecha.toString())} · ${ReglasArriendo.textoHora(horaInicio)} – ${ReglasArriendo.horaTermino(horaInicio, horas)}",
                        fontSize = 13.sp, color = CarpayTextSecondary
                    )
                    if (elegidos.isEmpty()) {
                        Text("Selecciona al menos un espacio.", fontSize = 13.sp, color = CarpayTextSecondary)
                    }
                    elegidos.forEach { FilaMonto("${it.nombre} · $horas h", valor(it.valorHora) * horas) }
                    servicios.filter { (cantidades[it.idServicio] ?: 0) > 0 }.forEach {
                        val cantidad = cantidades[it.idServicio] ?: 0
                        FilaMonto("${it.nombre} × $cantidad", valor(it.valor) * cantidad)
                    }
                    HorizontalDivider(color = CarpayInputBorder)
                    FilaMonto("Total estimado", totalEstimado, destacado = true)
                    FilaMonto("Anticipo (${ReglasArriendo.ANTICIPO_PORCENTAJE}%)", ReglasArriendo.anticipoEstimado(totalEstimado))
                    Text(
                        "El monto final lo confirma el servidor al crear la reserva. El anticipo se paga dentro de 24 horas y el saldo hasta 24 horas antes del evento.",
                        fontSize = 11.sp, color = CarpayTextSecondary
                    )
                }
            }

            val uriHandler = LocalUriHandler.current
            val contexto = LocalContext.current
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(checked = aceptaCondiciones, onCheckedChange = { aceptaCondiciones = it; error = null })
                Column(Modifier.padding(top = 12.dp)) {
                    Text("Acepto las condiciones de arriendo y la política de cancelación.", fontSize = 13.sp, color = CarpayTextPrimary)
                    Text(
                        "Ver condiciones",
                        fontSize = 13.sp,
                        color = CarpayPrimaryGreen,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { uriHandler.openUri(ApiClient.urlWeb(contexto, "/condiciones")) }
                    )
                }
            }

            error?.let {
                Surface(shape = RoundedCornerShape(12.dp), color = CarpayRedBg, modifier = Modifier.fillMaxWidth()) {
                    Text(it, color = CarpayRedText, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
            }

            Button(
                onClick = {
                    error = when {
                        elegidos.isEmpty() -> "Selecciona al menos un espacio."
                        horas !in 1..horasMaximas -> "Desde las ${ReglasArriendo.textoHora(horaInicio)} puedes reservar entre 1 y $horasMaximas horas."
                        !aceptaCondiciones -> "Debes aceptar las condiciones de arriendo antes de reservar."
                        else -> null
                    }
                    if (error != null) return@Button
                    enviando = true
                    reservasViewModel.crearReserva(
                        ReservaCrearRequest(
                            fecha = fecha.toString(),
                            horaInicio = ReglasArriendo.textoHora(horaInicio),
                            sectores = elegidos.map { SectorEntradaRequest(sector = it.idSector, horas = horas) },
                            servicios = cantidades.filter { it.value > 0 }.map { ServicioEntradaRequest(servicio = it.key, cantidad = it.value) },
                            aceptaCondiciones = true
                        ),
                        onSuccess = { reserva -> enviando = false; onReservaCreada(reserva.idReserva) },
                        onError = { mensaje -> enviando = false; error = mensaje }
                    )
                },
                enabled = !enviando && elegidos.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (enviando) {
                    CircularProgressIndicator(color = CarpayWhite, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    val cantidad = elegidos.size
                    Text(
                        if (cantidad > 1) "Reservar $cantidad espacios y pagar" else "Reservar y pagar",
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (mostrarCalendario) {
        val minimoMillis = ReglasArriendo.fechaMinima().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minimoMillis
                override fun isSelectableYear(year: Int) = year >= LocalDate.now().year
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { fecha = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    mostrarCalendario = false
                    error = null
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") } }
        ) { DatePicker(state = estado) }
    }
}

@Composable
private fun Seccion(titulo: String, subtitulo: String? = null, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CarpayWhite,
        border = BorderStroke(1.dp, CarpayInputBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
            subtitulo?.let { Text(it, fontSize = 12.sp, color = CarpayTextSecondary) }
            contenido()
        }
    }
}

@Composable
private fun TarjetaEspacio(sector: SectorResponse, horas: Int, seleccionado: Boolean, onCambiar: (Boolean) -> Unit) {
    val subtotal = (sector.valorHora.toBigDecimalOrNull()?.toLong() ?: 0L) * horas
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionado) CarpayGreenLight else CarpayWhite,
        border = BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) CarpayPrimaryGreen else CarpayInputBorder),
        modifier = Modifier.fillMaxWidth().clickable { onCambiar(!seleccionado) }
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = seleccionado, onCheckedChange = onCambiar)
            Column(Modifier.weight(1f)) {
                Text(sector.nombre, fontWeight = FontWeight.SemiBold, color = CarpayTextPrimary)
                if (sector.descripcion.isNotBlank()) {
                    Text(sector.descripcion, fontSize = 12.sp, color = CarpayTextSecondary, maxLines = 2)
                }
                Text("${Formato.clp(sector.valorHora)} / hora", fontSize = 12.sp, color = CarpayTextSecondary)
            }
            if (seleccionado) {
                Text(Formato.clp(subtotal.toString()), fontWeight = FontWeight.SemiBold, color = CarpayPrimaryGreen, modifier = Modifier.padding(end = 8.dp))
            }
        }
    }
}

@Composable
private fun FilaServicio(servicio: ServicioResponse, cantidad: Int, onCambiar: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(servicio.nombre, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CarpayTextPrimary)
            Text(Formato.clp(servicio.valor) + " c/u", fontSize = 12.sp, color = CarpayTextSecondary)
        }
        Contador(valor = cantidad, minimo = 0, maximo = 500, onCambiar = onCambiar)
    }
}

@Composable
private fun Contador(valor: Int, minimo: Int, maximo: Int, sufijo: String? = null, onCambiar: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onCambiar(valor - 1) }, enabled = valor > minimo) {
            Icon(Icons.Outlined.RemoveCircleOutline, "Menos", tint = if (valor > minimo) CarpayPrimaryGreen else CarpayInputBorder)
        }
        Text(
            if (sufijo != null) "$valor $sufijo" else "$valor",
            fontWeight = FontWeight.SemiBold, color = CarpayTextPrimary,
            modifier = Modifier.widthIn(min = 24.dp)
        )
        IconButton(onClick = { onCambiar(valor + 1) }, enabled = valor < maximo) {
            Icon(Icons.Outlined.AddCircleOutline, "Más", tint = if (valor < maximo) CarpayPrimaryGreen else CarpayInputBorder)
        }
    }
}

@Composable
internal fun FilaMonto(etiqueta: String, monto: Long, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            etiqueta, fontSize = if (destacado) 15.sp else 13.sp,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal,
            color = CarpayTextPrimary, modifier = Modifier.weight(1f)
        )
        Text(
            Formato.clp(monto.toString()), fontSize = if (destacado) 15.sp else 13.sp,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.SemiBold, color = CarpayTextPrimary
        )
    }
}
