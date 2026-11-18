package com.altosdecarpay.app.ui.screens.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altosdecarpay.app.ui.theme.*

@Composable
fun UserProfileScreen(
    onLogout: () -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    Scaffold(
        topBar = { UserTopBar() },
        bottomBar = { UserBottomBar(selectedTab = UserTab.PERFIL, onTabSelected = onTabSelected) },
        containerColor = CarpayBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserScreenHeader(
                title = "Tu perfil",
                subtitle = "Datos de tu cuenta en Altos de Carpay."
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(CarpayGreenLight)
                    ) {
                        Text("CR", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CarpayPrimaryGreen)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Camila Rojas", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CarpayTextPrimary)
                        Text("camila.rojas@correo.cl", fontSize = 13.sp, color = CarpayTextSecondary)
                        Text("+56 9 8765 4321", fontSize = 12.sp, color = CarpayTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CarpayRedText.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CarpayRedText),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}