package com.altosdecarpay.app.ui.screens.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.utils.QrCodeGenerator
import com.altosdecarpay.app.ui.viewmodel.ReservasViewModel

@Composable
fun UserReservaDetailScreen(
    reservaId: String = "RES-0001",
    reservasViewModel: ReservasViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onOpenTickets: () -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    val numericId = remember(reservaId) { QrCodeGenerator.parseReservaId(reservaId) ?: 1 }
    val detalleState by reservasViewModel.reservaDetalle.collectAsState()

    LaunchedEffect(numericId) {
        reservasViewModel.loadReservaDetalle(numericId)
    }

    val reserva = (detalleState as? NetworkResult.Success)?.data
    val qrBitmap = remember(reserva?.tokenQr, reserva?.idFormateado) {
        val qrContent = reserva?.tokenQr ?: reserva?.idFormateado ?: reservaId
        QrCodeGenerator.generateQrBitmap(qrContent, 512)
    }

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
                title = "Tu reserva",
                subtitle = "Detalle y código QR de acceso",
                showBackButton = true,
                onBackClick = onBackClick
            )

            when (detalleState) {
                is NetworkResult.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CarpayPrimaryGreen)
                    }
                }
                is NetworkResult.Error -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CarpayRedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = (detalleState as NetworkResult.Error).message,
                            color = CarpayRedText,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> {
                    if (reserva != null) {
                        // Tarjeta Evento Real
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CarpayWhite,
                            border = BorderStroke(1.dp, CarpayInputBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(reserva.idFormateado, fontSize = 12.sp, color = CarpayTextSecondary)
                                    Surface(shape = RoundedCornerShape(10.dp), color = CarpayGreenLight) {
                                        Text(
                                            text = reserva.estado,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CarpayPrimaryGreen,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = reserva.sectorPrincipal,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 22.sp,
                                    color = CarpayTextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.CalendarToday, null, tint = CarpayTextSecondary, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${reserva.fecha} · ${reserva.horaInicio.take(5)} – ${reserva.horaFin?.take(5) ?: "${reserva.horasReservadas}h"}",
                                        fontSize = 13.sp,
                                        color = CarpayTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Place, null, tint = CarpayTextSecondary, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = reserva.sectores.joinToString(", ") { "${it.nombre} (${it.horas}h)" },
                                        fontSize = 13.sp,
                                        color = CarpayTextSecondary
                                    )
                                }
                            }
                        }

                        // Tarjeta QR de acceso REAL
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CarpayWhite,
                            border = BorderStroke(1.dp, CarpayInputBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Tu QR de acceso", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
                                Text("Muéstralo al equipo para registrar tu ingreso y salida.", fontSize = 12.sp, color = CarpayTextSecondary)
                                Spacer(modifier = Modifier.height(16.dp))
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "Código QR de acceso",
                                        modifier = Modifier.size(190.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.QrCode2,
                                        contentDescription = "QR de acceso",
                                        tint = CarpayPrimaryGreen,
                                        modifier = Modifier.size(170.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${reserva.idFormateado} · Estado: ${reserva.estado}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarpayPrimaryGreen
                                )
                            }
                        }

                        // Resumen financiero real
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CarpayWhite,
                            border = BorderStroke(1.dp, CarpayInputBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Resumen de pagos", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Monto total", fontSize = 13.sp, color = CarpayTextSecondary)
                                    Text("$${reserva.montoTotal}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CarpayTextPrimary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Pagado", fontSize = 13.sp, color = CarpayTextSecondary)
                                    Text("$${reserva.montoPagado ?: "0.00"}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CarpayPrimaryGreen)
                                }
                                HorizontalDivider(color = CarpayInputBorder)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Saldo pendiente", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CarpayTextPrimary)
                                    Text("$${reserva.saldoPendiente ?: "0.00"}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CarpayAmberText)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenTickets,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CarpayInputBorder),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = CarpayWhite, contentColor = CarpayPrimaryGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Consultas y reclamos de esta reserva", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
