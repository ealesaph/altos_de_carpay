package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.viewmodel.ServiciosViewModel

@Composable
fun AdminProductsScreen(
    serviciosViewModel: ServiciosViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    val serviciosState by serviciosViewModel.servicios.collectAsState()

    var nombreNuevo by remember { mutableStateOf("") }
    var descripcionNuevo by remember { mutableStateOf("") }
    var precioNuevo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        serviciosViewModel.loadServicios()
    }

    val servicios = (serviciosState as? NetworkResult.Success)?.data ?: emptyList()

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
                title = "Servicios y productos",
                subtitle = "Catálogo de servicios extra registrados en la base de datos",
                showBackButton = true,
                onBackClick = onBackClick
            )

            Text("Catálogo actual (${servicios.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)

            if (serviciosState is NetworkResult.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CarpayPrimaryGreen)
                }
            } else {
                servicios.forEach { prod ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CarpayWhite,
                        border = BorderStroke(1.dp, CarpayInputBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(prod.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                            Text(
                                "${prod.descripcion.ifBlank { "Servicio extra" }} · $${prod.valor}",
                                fontSize = 12.sp,
                                color = CarpayTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (prod.estado == "Activo") CarpayGreenLight else CarpayInputBorder
                            ) {
                                Text(
                                    prod.estado,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (prod.estado == "Activo") CarpayPrimaryGreen else CarpayTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Agregar producto real en la BD
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nuevo servicio / producto", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
                    OutlinedTextField(
                        value = nombreNuevo,
                        onValueChange = { nombreNuevo = it },
                        placeholder = { Text("Ej: Jugo natural 1 L") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descripcionNuevo,
                        onValueChange = { descripcionNuevo = it },
                        placeholder = { Text("Descripción breve (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = precioNuevo,
                        onValueChange = { precioNuevo = it },
                        placeholder = { Text("Precio CLP (ej: 3000)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (nombreNuevo.isNotBlank() && precioNuevo.isNotBlank()) {
                                serviciosViewModel.crearServicio(nombreNuevo, descripcionNuevo, precioNuevo) {
                                    nombreNuevo = ""
                                    descripcionNuevo = ""
                                    precioNuevo = ""
                                }
                            }
                        },
                        enabled = nombreNuevo.isNotBlank() && precioNuevo.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar en base de datos", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
