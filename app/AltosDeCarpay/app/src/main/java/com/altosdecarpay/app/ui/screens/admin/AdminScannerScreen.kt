package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

@Composable
fun AdminScannerScreen(
    onBackClick: () -> Unit = {},
    onQrReadSuccess: (reservaId: String, tipoMovimiento: String) -> Unit = { _, _ -> },
    onTabSelected: (AdminTab) -> Unit = {}
) {
    var movimientoSeleccionado by remember { mutableStateOf("Salida") }
    var codigoManual by remember { mutableStateOf("RES-0248") }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdminScreenHeader(
                title = "Control de acceso",
                subtitle = "Selecciona el movimiento que vas a registrar.",
                showBackButton = true,
                onBackClick = onBackClick
            )

            // Selector Ingreso / Salida
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CarpayGreenLight.copy(alpha = 0.7f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Ingreso", "Salida").forEach { tipo ->
                    val isSelected = movimientoSeleccionado == tipo
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) CarpayPrimaryGreen else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { movimientoSeleccionado = tipo }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (tipo == "Ingreso") Icons.AutoMirrored.Outlined.Login else Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                tint = if (isSelected) CarpayWhite else CarpayTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tipo,
                                color = if (isSelected) CarpayWhite else CarpayTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Visor de Cámara simulada con QR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A2B22))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• CÁMARA DE MUESTRA",
                        color = CarpayWhite.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Outlined.FlashlightOn,
                        contentDescription = "Linterna",
                        tint = CarpayWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Marco central QR
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CarpayWhite,
                        modifier = Modifier.size(140.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.QrCode2,
                                contentDescription = "QR Muestra",
                                tint = CarpayPrimaryGreen,
                                modifier = Modifier.size(116.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Centra el QR de la reserva en el marco",
                        color = CarpayWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Representación visual · no escanea códigos",
                        color = CarpayWhite.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            // Botón Leer QR
            Button(
                onClick = { onQrReadSuccess(codigoManual, movimientoSeleccionado) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CarpayPrimaryGreen,
                    contentColor = CarpayWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Leer QR de ${movimientoSeleccionado.lowercase()}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Aviso informativo
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = CarpayTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Revisa los datos antes de confirmar. La lectura no registra la salida automáticamente.",
                    fontSize = 12.sp,
                    color = CarpayTextSecondary,
                    lineHeight = 16.sp
                )
            }

            HorizontalDivider(color = CarpayInputBorder)

            // Búsqueda manual
            Text(
                text = "¿No puedes leer el QR?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CarpayTextPrimary
            )
            Text(
                text = "Identificador de reserva",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CarpayTextPrimary
            )
            OutlinedTextField(
                value = codigoManual,
                onValueChange = { codigoManual = it },
                leadingIcon = {
                    Text("#", fontWeight = FontWeight.Bold, color = CarpayTextSecondary, fontSize = 16.sp)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CarpayWhite,
                    unfocusedContainerColor = CarpayWhite,
                    focusedBorderColor = CarpayPrimaryGreen,
                    unfocusedBorderColor = CarpayInputBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = { onQrReadSuccess(codigoManual, movimientoSeleccionado) },
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
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscar reserva manualmente", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}