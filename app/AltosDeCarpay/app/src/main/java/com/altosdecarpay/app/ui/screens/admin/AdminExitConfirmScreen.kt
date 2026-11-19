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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.utils.QrCodeGenerator
import com.altosdecarpay.app.ui.viewmodel.ReservasViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AdminExitConfirmScreen(
    reservaId: String = "RES-0001",
    reservasViewModel: ReservasViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onConfirmExit: (String) -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    val numericId = remember(reservaId) { QrCodeGenerator.parseReservaId(reservaId) ?: 1 }
    val detalleState by reservasViewModel.reservaDetalle.collectAsState()
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(numericId) {
        reservasViewModel.loadReservaDetalle(numericId)
    }

    val horaActual = remember {
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    val reserva = (detalleState as? NetworkResult.Success)?.data

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
                subtitle = "Verificación de QR y registro de salida de la reserva",
                showBackButton = true,
                onBackClick = onBackClick
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CarpayGreenLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = CarpayPrimaryGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Reserva validada en base de datos",
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

            if (reserva != null) {
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
                            Text(reserva.idFormateado, fontSize = 12.sp, color = CarpayTextSecondary)
                            Surface(shape = RoundedCornerShape(10.dp), color = CarpayGreenLight) {
                                Text(
                                    reserva.estado,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CarpayPrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Reserva ${reserva.sectorPrincipal}",
                            fontFamily = FontFamily.Serif,
                            fontSize = 22.sp,
                            color = CarpayTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarToday, null, tint = CarpayTextSecondary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(reserva.fecha, fontSize = 13.sp, color = CarpayTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Place, null, tint = CarpayTextSecondary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(reserva.sectores.joinToString(", ") { it.nombre }, fontSize = 13.sp, color = CarpayTextSecondary)
                        }
                        HorizontalDivider(color = CarpayInputBorder, modifier = Modifier.padding(vertical = 10.dp))
                        Text("Titular: ${reserva.cliente}", fontSize = 12.sp, color = CarpayTextSecondary)
                    }
                }

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
                                text = horaActual,
                                fontFamily = FontFamily.Serif,
                                fontSize = 40.sp,
                                color = CarpayWhite
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Cierre agendado", fontSize = 11.sp, color = CarpayWhite.copy(alpha = 0.75f))
                                Text(reserva.horaFin?.take(5) ?: "--:--", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CarpayWhite)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        isSaving = true
                        reservasViewModel.validarAcceso(
                            id = reserva.idReserva,
                            tipoRegistro = "Salida",
                            onSuccess = {
                                isSaving = false
                                onConfirmExit(reserva.idFormateado)
                            },
                            onError = {
                                isSaving = false
                            }
                        )
                    },
                    enabled = !isSaving,
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
                    Text("Confirmar salida · $horaActual", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
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
