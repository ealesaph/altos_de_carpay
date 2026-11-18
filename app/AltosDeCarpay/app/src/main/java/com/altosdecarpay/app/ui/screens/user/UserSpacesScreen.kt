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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

@Composable
fun UserSpacesScreen(
    reservaId: String = "RES-0261",
    onBackClick: () -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
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
                title = "Tus espacios",
                subtitle = "Estos son los rincones que puedes disfrutar en tu próximo encuentro.",
                showBackButton = true,
                onBackClick = onBackClick
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CarpayGreenLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TU RESERVA · $reservaId", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CarpayPrimaryGreen)
                        Text("7 nov. 2026", fontSize = 11.sp, color = CarpayTextSecondary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Un almuerzo en familia", fontFamily = FontFamily.Serif, fontSize = 20.sp, color = CarpayTextPrimary)
                    Text("Para disfrutar de 13:00 a 18:00", fontSize = 12.sp, color = CarpayPrimaryGreen)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Para compartir y desconectar", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                Text("2 espacios", fontSize = 12.sp, color = CarpayTextSecondary)
            }

            SpaceShowcaseCard(
                title = "Terraza superior",
                description = "Parrilla y mesas para compartir el almuerzo.",
                icon = Icons.Outlined.Home,
                bannerColor = Color(0xFF3E5641)
            )

            SpaceShowcaseCard(
                title = "Mirador",
                description = "Espacio al aire libre para descansar y conversar.",
                icon = Icons.Outlined.Park,
                bannerColor = Color(0xFF4F6D52)
            )
        }
    }
}

@Composable
private fun SpaceShowcaseCard(title: String, description: String, icon: ImageVector, bannerColor: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CarpayWhite,
        border = BorderStroke(1.dp, CarpayInputBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(bannerColor)
            ) {
                Icon(icon, null, tint = CarpayWhite.copy(alpha = 0.85f), modifier = Modifier.size(48.dp))
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, fontFamily = FontFamily.Serif, fontSize = 22.sp, color = CarpayTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, fontSize = 13.sp, color = CarpayTextSecondary)
            }
        }
    }
}