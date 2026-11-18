package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

@Composable
fun AdminProfileScreen(
    onNavigateToBitacora: () -> Unit = {},
    onNavigateToProducts: () -> Unit = {},
    onLogout: () -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    Scaffold(
        topBar = { AdminTopBar() },
        bottomBar = { AdminBottomBar(selectedTab = AdminTab.PERFIL, onTabSelected = onTabSelected) },
        containerColor = CarpayBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AdminScreenHeader(
                title = "Administración",
                subtitle = "Herramientas internas y configuración de Altos de Carpay."
            )

            ProfileOptionCard(
                icon = Icons.Outlined.EditNote,
                title = "Bitácora de incidencias",
                subtitle = "Observaciones internas por reserva y auditoría",
                onClick = onNavigateToBitacora
            )

            ProfileOptionCard(
                icon = Icons.Outlined.Storefront,
                title = "Productos externos",
                subtitle = "Menú de servicios extra publicados para reservas",
                onClick = onNavigateToProducts
            )

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CarpayRedText.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CarpayRedText),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ProfileOptionCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CarpayWhite,
        border = BorderStroke(1.dp, CarpayInputBorder),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(icon, null, tint = CarpayPrimaryGreen, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                    Text(subtitle, fontSize = 12.sp, color = CarpayTextSecondary)
                }
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = CarpayTextSecondary)
        }
    }
}