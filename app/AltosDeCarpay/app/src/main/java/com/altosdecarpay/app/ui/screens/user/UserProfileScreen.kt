package com.altosdecarpay.app.ui.screens.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.SessionState
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.viewmodel.AuthViewModel

@Composable
fun UserProfileScreen(
    authViewModel: AuthViewModel? = viewModel(),
    onLogout: () -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    val perfilState by authViewModel?.perfil?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        authViewModel?.loadPerfil()
    }

    val user = (perfilState as? NetworkResult.Success)?.data ?: SessionState.currentUser
    val nombre = user?.nombreCompleto?.ifBlank { "Usuario Carpay" } ?: "Camila Rojas"
    val correo = user?.correo?.ifBlank { "usuario@correo.cl" } ?: "camila.rojas@correo.cl"
    val telefono = user?.telefono?.ifBlank { "Sin teléfono registrado" } ?: "+56 9 8765 4321"
    val rut = user?.rut ?: ""
    val iniciales = user?.iniciales ?: "CR"

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
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(CarpayGreenLight)
                    ) {
                        Text(iniciales, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CarpayPrimaryGreen)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(nombre, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CarpayTextPrimary)
                        Text(correo, fontSize = 13.sp, color = CarpayTextSecondary)
                        Text(telefono, fontSize = 12.sp, color = CarpayTextSecondary)
                        if (rut.isNotBlank()) {
                            Text("RUT: $rut", fontSize = 11.sp, color = CarpayTextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = {
                    if (authViewModel != null) {
                        authViewModel.logout(onDone = onLogout)
                    } else {
                        onLogout()
                    }
                },
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