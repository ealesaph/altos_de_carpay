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
import com.altosdecarpay.app.ui.theme.*

data class ProductoExternoUi(
    val nombre: String,
    val detalle: String,
    val publicado: Boolean,
    val disponible: Boolean
)

@Composable
fun AdminProductsScreen(
    onBackClick: () -> Unit = {},
    onTabSelected: (AdminTab) -> Unit = {}
) {
    var productos by remember {
        mutableStateOf(
            listOf(
                ProductoExternoUi("Bebida 1,5 L", "Gaseosa para compartir · $2.500", true, true),
                ProductoExternoUi("Bolsa de hielo 2 kg", "Hielo en cubos · $2.000", true, true),
                ProductoExternoUi("Agua mineral 1,5 L", "Sin gas · $1.500", true, false),
                ProductoExternoUi("Carbón 3 kg", "Bolsa para parrilla · $4.500", false, true)
            )
        )
    }
    var nombreNuevo by remember { mutableStateOf("") }
    var precioNuevo by remember { mutableStateOf("") }

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
                title = "Productos externos",
                subtitle = "Perfil · Menú de productos para las reservas",
                showBackButton = true,
                onBackClick = onBackClick
            )

            Text("En el menú de usuario", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)

            productos.forEach { prod ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CarpayWhite,
                    border = BorderStroke(1.dp, CarpayInputBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(prod.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarpayTextPrimary)
                        Text(prod.detalle, fontSize = 12.sp, color = CarpayTextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (prod.publicado) CarpayGreenLight else CarpayInputBorder
                            ) {
                                Text(
                                    if (prod.publicado) "Visible en menú" else "No publicado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (prod.publicado) CarpayPrimaryGreen else CarpayTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                if (prod.disponible) "Disponible" else "No disponible",
                                fontSize = 11.sp,
                                color = if (prod.disponible) CarpayTextSecondary else CarpayRedText
                            )
                        }
                    }
                }
            }

            // Agregar producto rápido
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CarpayWhite,
                border = BorderStroke(1.dp, CarpayInputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nuevo producto", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarpayTextPrimary)
                    OutlinedTextField(
                        value = nombreNuevo,
                        onValueChange = { nombreNuevo = it },
                        placeholder = { Text("Ej: Jugo natural 1 L") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = precioNuevo,
                        onValueChange = { precioNuevo = it },
                        placeholder = { Text("Precio CLP (ej: $3.000)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (nombreNuevo.isNotBlank()) {
                                productos = productos + ProductoExternoUi(nombreNuevo, "Servicio extra · ${precioNuevo.ifBlank { "$3.000" }}", true, true)
                                nombreNuevo = ""
                                precioNuevo = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CarpayPrimaryGreen, contentColor = CarpayWhite),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar producto", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}