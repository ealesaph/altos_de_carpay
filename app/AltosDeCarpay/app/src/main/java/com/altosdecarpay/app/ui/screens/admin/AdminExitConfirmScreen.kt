package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

@Composable
fun AdminExitConfirmScreen(
    reservaId: String = "RES-0248",
    onBackClick: () -> Unit = {},
    onConfirmExit: (String) -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    Scaffold(
        topBar = { AdminTopBar() },
        bottomBar = {
            AdminBottomBar(
                selectedTab = AdminTab.ESCANEAR,
                onTabSelected = onTabSelected
            )
        },
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
                title = "Confirmar salida",
                subtitle = "Lectura de ejemplo · salida aún sin confirmar",
                showBackButton = true,
                onBackClick = onBackClick
            )

            // Banner QR identificado
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CarpayGreenLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = CarpayPrimaryGreen
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "QR de muestra identificado",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarpayTextPrimary
                        )
                        Text(
                            text = "Movimiento seleccionado: salida",
                            fontSize = 12.sp,
                            color = CarpayTextSecondary
                        )
                    }
                }
            }

            // Tarjeta resumen reserva
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(reservaId, fontSize = 12.sp, color = CarpayTextSecondary)
                        Surface(shape = RoundedCornerShape(10.dp), color = CarpayGreenLight) {
                            Text(
                                "En curso",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CarpayPrimaryGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Cumpleaños de Camila",
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        color = CarpayTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarToday, null, tint = CarpayTextSecondary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("24 de octubre de 2026", fontSize = 13.sp, color = CarpayTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Place, null, tint = CarpayTextSecondary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Quincho Los Aromos · 30 personas", fontSize = 13.sp, color = CarpayTextSecondary)
                    }
                    HorizontalDivider(color = CarpayInputBorder, modifier = Modifier.padding(vertical = 10.dp))
                    Text("Titular: Camila Rojas", fontSize = 12.sp, color = CarpayTextSecondary)
                }
            }

            // Tarjeta Verde Oscuro: Salida detectada por QR
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayPrimaryGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = CarpayWhite.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Salida detectada por QR",
                            color = CarpayWhite.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "21:25",
                            fontFamily = FontFamily.Serif,
                            fontSize = 40.sp,
                            color = CarpayWhite
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Cierre agendado", fontSize = 11.sp, color = CarpayWhite.copy(alpha = 0.75f))
                            Text("21:00", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CarpayWhite)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "24 de octubre de 2026 · hora local",
                        fontSize = 11.sp,
                        color = CarpayWhite.copy(alpha = 0.75f)
                    )
                }
            }

            // Tarjeta Ingreso registrado
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ingreso ya registrado", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CarpayTextPrimary)
                        Text("16:08", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
                    }
                    HorizontalDivider(color = CarpayInputBorder, modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Inicio agendado", fontSize = 13.sp, color = CarpayTextSecondary)
                        Text("16:00", fontSize = 13.sp, color = CarpayTextSecondary)
                    }
                }
            }

            // Tarjeta Ámbar: Tiempo excedido
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CarpayAmberBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, null, tint = CarpayAmberText, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "25 min de tiempo excedido",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CarpayAmberText
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$500/min × 25 min", fontSize = 13.sp, color = CarpayAmberText)
                        Text("$12.500", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CarpayAmberText)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Tarifa de ejemplo. Cargo extra propuesto; aún no se ha cobrado.",
                        fontSize = 11.sp,
                        color = CarpayAmberText
                    )
                }
            }

            Text(
                text = "Al confirmar, la reserva pasará a finalizada y se guardará la salida a las 21:25. El cargo extra quedará propuesto, no cobrado.",
                fontSize = 12.sp,
                color = CarpayTextSecondary,
                lineHeight = 16.sp
            )

            // Botones
            Button(
                onClick = { onConfirmExit(reservaId) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CarpayPrimaryGreen,
                    contentColor = CarpayWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar salida · 21:25", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CarpayInputBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CarpayWhite,
                    contentColor = CarpayPrimaryGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Volver sin registrar", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}