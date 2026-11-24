package com.altosdecarpay.app.ui.screens.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
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
    onSesionTerminada: (String) -> Unit = {},
    onTabSelected: (UserTab) -> Unit = {}
) {
    val perfilState by authViewModel?.perfil?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        authViewModel?.loadPerfil()
    }

    // Sin datos de ejemplo: mientras carga se muestra «Cargando tus datos…».
    var perfil by remember(perfilState) {
        mutableStateOf((perfilState as? NetworkResult.Success)?.data ?: SessionState.currentUser)
    }

    Scaffold(
        topBar = { UserTopBar() },
        bottomBar = { UserBottomBar(selectedTab = UserTab.PERFIL, onTabSelected = onTabSelected) },
        containerColor = CarpayBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserScreenHeader(
                title = "Tu perfil",
                subtitle = "Datos de tu cuenta en Altos de Carpay."
            )

            com.altosdecarpay.app.ui.components.TarjetaPerfil(perfil = perfil, rolVisible = "Cliente")

            com.altosdecarpay.app.ui.components.OpcionesCuenta(
                perfil = perfil,
                esCliente = true,
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