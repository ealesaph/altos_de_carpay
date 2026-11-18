package com.altosdecarpay.app.ui.screens.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

@Composable
fun UserExtraServicesScreen(
    reservaId: String = "RES-0261",
    onBackClick: () -> Unit = {},
    onRequestSubmitted: () -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    var incluirPiscina by remember { mutableStateOf(true) }
    var extenderHorario by remember { mutableStateOf(true) }
    var cantBebida by remember { mutableIntStateOf(2) }
    var cantHielo by remember { mutableIntStateOf(1) }

    val totalProductos = (cantBebida * 2500) + (cantHielo * 2000)

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            UserScreenHeader(
                title = "Servicios extra",
                subtitle = "Suma algo especial a tu próximo encuentro.",
                showBackButton = true,
                onBackClick = onBackClick
            )

            // Resumen Reserva
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(reservaId, fontSize = 12.sp, color = CarpayTextSecondary)
                        Surface(shape = RoundedCornerShape(10.dp), color = CarpayGreenLight) {
                            Text("Confirmada", fontSize = 11.sp, color = CarpayPrimaryGreen, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                        }
                    }
                    Text("Un almuerzo en familia", fontFamily = FontFamily.Serif, fontSize = 20.sp, color = CarpayTextPrimary)
                    Text("Quincho Los Aromos · 7 nov. 2026 · 13:00–18:00", fontSize = 12.sp, color = CarpayTextSecondary)
                }
            }

            // Opción: Incluir Piscina
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, if (incluirPiscina) CarpayPrimaryGreen else CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Pool, null, tint = CarpayPrimaryGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Incluir Piscina", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                            Text("Precio por confirmar", fontSize = 12.sp, color = CarpayTextSecondary)
                        }
                    }
                    Checkbox(
                        checked = incluirPiscina,
                        onCheckedChange = { incluirPiscina = it },
                        colors = CheckboxDefaults.colors(checkedColor = CarpayPrimaryGreen)
                    )
                }
            }

            // Opción: Extender Horario
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, if (extenderHorario) CarpayPrimaryGreen else CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Schedule, null, tint = CarpayPrimaryGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Extender Horario", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                                Text("Precio por confirmar", fontSize = 12.sp, color = CarpayTextSecondary)
                            }
                        }
                        Checkbox(
                            checked = extenderHorario,
                            onCheckedChange = { extenderHorario = it },
                            colors = CheckboxDefaults.colors(checkedColor = CarpayPrimaryGreen)
                        )
                    }
                    if (extenderHorario) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CarpayGreenLight.copy(alpha = 0.6f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Cierre actual", fontSize = 11.sp, color = CarpayTextSecondary)
                                Text("18:00", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CarpayTextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Cierre solicitado (+1 h)", fontSize = 11.sp, color = CarpayTextSecondary)
                                Text("19:00", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                            }
                        }
                    }
                }
            }

            Text("Productos externos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)

            // Bebida 1,5 L
            ProductQuantityCard(
                nombre = "Bebida 1,5 L",
                detalle = "Gaseosa para compartir · $2.500 / unidad",
                cantidad = cantBebida,
                onDecrease = { if (cantBebida > 0) cantBebida-- },
                onIncrease = { cantBebida++ }
            )

            // Bolsa de hielo
            ProductQuantityCard(
                nombre = "Bolsa de hielo 2 kg",
                detalle = "Hielo en cubos · $2.000 / unidad",
                cantidad = cantHielo,
                onDecrease = { if (cantHielo > 0) cantHielo-- },
                onIncrease = { cantHielo++ }
            )

            // Resumen Tu Selección
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tu selección", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
                    if (incluirPiscina) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Piscina", fontSize = 13.sp, color = CarpayTextPrimary)
                            Text("Precio por confirmar", fontSize = 12.sp, color = CarpayTextSecondary)
                        }
                    }
                    if (extenderHorario) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Extensión · +1 h, hasta 19:00", fontSize = 13.sp, color = CarpayTextPrimary)
                            Text("Precio por confirmar", fontSize = 12.sp, color = CarpayTextSecondary)
                        }
                    }
                    if (cantBebida > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$cantBebida × Bebida 1,5 L", fontSize = 13.sp, color = CarpayTextPrimary)
                            Text("$${cantBebida * 2500}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (cantHielo > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$cantHielo × Bolsa de hielo 2 kg", fontSize = 13.sp, color = CarpayTextPrimary)
                            Text("$${cantHielo * 2000}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(color = CarpayInputBorder, modifier = Modifier.padding(vertical = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Productos · ejemplo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CarpayTextPrimary)
                        Text("$$totalProductos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                    }
                }
            }

            Button(
                onClick = onRequestSubmitted,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Solicitar extras para mi reserva", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ProductQuantityCard(
    nombre: String,
    detalle: String,
    cantidad: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CarpayWhite,
        border = BorderStroke(1.dp, if (cantidad > 0) CarpayPrimaryGreen else CarpayInputBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                Text(detalle, fontSize = 12.sp, color = CarpayTextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Remove, null, tint = CarpayPrimaryGreen)
                }
                Text("$cantidad", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Add, null, tint = CarpayPrimaryGreen)
                }
            }
        }
    }
}