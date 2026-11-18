package com.altosdecarpay.app.ui.screens.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

@Composable
fun UserReservationsScreen(
    onOpenReservaDetail: (String) -> Unit = {},
    onOpenExtraServices: (String) -> Unit = {},
    onOpenMySpaces: (String) -> Unit = {},
    onOpenObservations: (String) -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    var selectedSegment by remember { mutableStateOf("Próximas") }

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
                title = "Tus buenos momentos",
                subtitle = "Hola, Camila. Tu próximo encuentro empieza aquí."
            )

            // Segmented Control: Próximas · 2 | Finalizadas · 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarpayGreenLight.copy(alpha = 0.6f))
                    .padding(4.dp)
            ) {
                listOf("Próximas · 2", "Finalizadas · 1").forEach { tabLabel ->
                    val isSelected = tabLabel.startsWith(selectedSegment)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) CarpayWhite else Color.Transparent,
                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSegment = tabLabel.substringBefore(" ") }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = tabLabel,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CarpayTextPrimary else CarpayTextSecondary
                            )
                        }
                    }
                }
            }

            if (selectedSegment == "Próximas") {
                // Tarjeta Principal: RES-0261 Un almuerzo en familia
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Banner visual del recinto
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(145.dp)
                                .background(Color(0xFF3A5243))
                                .clickable { onOpenMySpaces("RES-0261") }
                                .padding(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = CarpayWhite,
                                modifier = Modifier.align(Alignment.BottomStart)
                            ) {
                                Text(
                                    text = "Quincho Los Aromos",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarpayTextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = CarpayPrimaryGreen.copy(alpha = 0.85f),
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = "Ver tus espacios →",
                                    fontSize = 11.sp,
                                    color = CarpayWhite,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("RES-0261", fontSize = 12.sp, color = CarpayTextSecondary)
                                Surface(shape = RoundedCornerShape(10.dp), color = CarpayGreenLight) {
                                    Text(
                                        "Confirmada",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CarpayPrimaryGreen,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Un almuerzo en familia",
                                fontFamily = FontFamily.Serif,
                                fontSize = 22.sp,
                                color = CarpayTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CalendarToday, null, tint = CarpayTextSecondary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("7 nov. 2026 · 13:00–18:00", fontSize = 13.sp, color = CarpayTextPrimary)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Acción: Agregar servicios extra
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CarpayGreenLight.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenExtraServices("RES-0261") }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Add, null, tint = CarpayPrimaryGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Agregar servicios extra",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CarpayPrimaryGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Acción secundaria: Dejar observación previa
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CarpayBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenObservations("RES-0261") }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.ChatBubbleOutline, null, tint = CarpayTextPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Conversemos · Observaciones previas",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = CarpayTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Segunda reserva próxima: NOV 21
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CarpayGreenLight,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("NOV", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                                Text("21", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Encuentro de primavera", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                            Text("RES-0274 · Jardín del Maitén", fontSize = 12.sp, color = CarpayTextSecondary)
                            Text("12:00–17:00 · Confirmada", fontSize = 12.sp, color = CarpayTextSecondary)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = CarpayTextSecondary)
                    }
                }
            }

            // Sección Última finalizada (RES-0248)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Última finalizada", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
                Text("Ver historial", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CarpayPrimaryGreen)
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenReservaDetail("RES-0248") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("RES-0248 · 24 oct. 2026", fontSize = 12.sp, color = CarpayTextSecondary)
                        Surface(shape = RoundedCornerShape(10.dp), color = CarpayGreenLight) {
                            Text("Finalizada", fontSize = 11.sp, color = CarpayPrimaryGreen, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cumpleaños de Camila", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
                        Icon(Icons.AutoMirrored.Outlined.CallMade, null, tint = CarpayTextSecondary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "25 min adicionales · cargo pendiente de revisión",
                        fontSize = 12.sp,
                        color = CarpayAmberText
                    )
                }
            }

            // Botón Reservar un nuevo encuentro
            Button(
                onClick = { onTabSelected(UserTab.EXPLORAR) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reservar un nuevo encuentro", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Icon(Icons.Outlined.Eco, null, tint = CarpayPrimaryGreen, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Naturaleza para celebrar y compartir.", fontSize = 12.sp, color = CarpayTextSecondary)
            }
        }
    }
}