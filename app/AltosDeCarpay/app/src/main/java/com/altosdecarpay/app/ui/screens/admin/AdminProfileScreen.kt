package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.viewmodel.AuthViewModel

@Composable
fun AdminProfileScreen(
    authViewModel: AuthViewModel? = viewModel(),
    onNavigateToBitacora: () -> Unit = {},
    onNavigateToProducts: () -> Unit = {},
    onLogout: () -> Unit = {},
    onSesionTerminada: (String) -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    val perfilState by authViewModel?.perfil?.collectAsState() ?: remember { mutableStateOf(null) }
    LaunchedEffect(Unit) { authViewModel?.loadPerfil() }
    var perfil by remember(perfilState) {
        mutableStateOf((perfilState as? com.altosdecarpay.app.data.remote.NetworkResult.Success)?.data
            ?: com.altosdecarpay.app.data.SessionState.currentUser)
    }
    Scaffold(
        topBar = { AdminTopBar() },
        bottomBar = { AdminBottomBar(selectedTab = AdminTab.PERFIL, onTabSelected = onTabSelected) },
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
                title = "Administración",
                subtitle = "Herramientas internas y configuración de Altos de Carpay."
            )

            // Antes no se mostraba quién había iniciado sesión.
            com.altosdecarpay.app.ui.components.TarjetaPerfil(
                perfil = perfil,
                rolVisible = if (com.altosdecarpay.app.data.SessionState.userInfo?.isSuperuser == true) "Superusuario" else "Personal administrador"
            )

            ProfileOptionCard(
                icon = Icons.Outlined.EditNote,
                title = "Bitácora de incidencias",
                subtitle = "Todas las incidencias registradas; para agregar una, abre la reserva",
                onClick = onNavigateToBitacora
            )

            ProfileOptionCard(
                icon = Icons.Outlined.Storefront,
                title = "Servicios extra",
                subtitle = "Catálogo de servicios que los clientes pueden agregar",
                onClick = onNavigateToProducts
            )

            Text("Mi cuenta", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
            com.altosdecarpay.app.ui.components.OpcionesCuenta(
                perfil = perfil,
                esCliente = false,
                onPerfilActualizado = { perfil = it },
                onSesionTerminada = { mensaje -> authViewModel?.logout { onSesionTerminada(mensaje) } ?: onSesionTerminada(mensaje) }
            )

            Spacer(modifier = Modifier.height(8.dp))

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