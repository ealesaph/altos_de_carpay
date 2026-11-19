package com.altosdecarpay.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.remote.NetworkResult
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel? = viewModel(),
    onLoginSuccess: (rol: String) -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    var identificador by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val loginState by authViewModel?.loginState?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is NetworkResult.Success -> {
                val rol = state.data
                authViewModel?.resetLoginState()
                onLoginSuccess(rol)
            }
            is NetworkResult.Error -> {
                errorMessage = state.message
            }
            else -> {}
        }
    }

    val isLoading = loginState is NetworkResult.Loading

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CarpayBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- 1. HEADER ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Eco,
                    contentDescription = "Logo Altos de Carpay",
                    tint = CarpayPrimaryGreen,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Altos de carpay",
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    color = CarpayTextPrimary
                )
            }

            // --- 2. CUERPO CENTRAL (Título + Formulario) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
            ) {
                Text(
                    text = "Qué bueno verte",
                    fontFamily = FontFamily.Serif,
                    fontSize = 32.sp,
                    color = CarpayTextPrimary,
                    lineHeight = 38.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Inicia sesión para continuar.",
                    fontSize = 15.sp,
                    color = CarpayTextSecondary
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Campo: Usuario o correo
                Text(
                    text = "Usuario o correo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CarpayTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = identificador,
                    onValueChange = {
                        identificador = it
                        errorMessage = null
                    },
                    placeholder = {
                        Text("Ingresa tu usuario o correo", color = CarpayTextSecondary, fontSize = 14.sp)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CarpayWhite,
                        unfocusedContainerColor = CarpayWhite,
                        focusedBorderColor = CarpayPrimaryGreen,
                        unfocusedBorderColor = CarpayInputBorder,
                        focusedTextColor = CarpayTextPrimary,
                        unfocusedTextColor = CarpayTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Campo: Contraseña
                Text(
                    text = "Contraseña",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CarpayTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    placeholder = {
                        Text("Ingresa tu contraseña", color = CarpayTextSecondary, fontSize = 14.sp)
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = "Mostrar u ocultar contraseña",
                                tint = CarpayTextSecondary
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CarpayWhite,
                        unfocusedContainerColor = CarpayWhite,
                        focusedBorderColor = CarpayPrimaryGreen,
                        unfocusedBorderColor = CarpayInputBorder,
                        focusedTextColor = CarpayTextPrimary,
                        unfocusedTextColor = CarpayTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón Principal: Iniciar sesión
                Button(
                    onClick = {
                        val validationError = LoginHelper.validateInputs(identificador, password)
                        if (validationError != null) {
                            errorMessage = validationError
                            return@Button
                        }
                        if (authViewModel != null) {
                            authViewModel.login(identificador.trim(), password)
                        } else {
                            // Fallback modo offline
                            val rolSimulado = LoginHelper.determineRole(identificador)
                            onLoginSuccess(rolSimulado)
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CarpayPrimaryGreen,
                        contentColor = CarpayWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = CarpayWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = "Iniciar sesión",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Según tu cuenta, accederás a tu espacio de usuario o de administración.",
                    fontSize = 12.sp,
                    color = CarpayTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "¿Aún no tienes cuenta? ",
                        fontSize = 14.sp,
                        color = CarpayTextSecondary
                    )
                    Text(
                        text = "Registrarse",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarpayPrimaryGreen,
                        modifier = Modifier.clickable { onNavigateToRegister() }
                    )
                }
            }

            // --- 3. FOOTER ---
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Eco,
                    contentDescription = null,
                    tint = CarpayPrimaryGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Naturaleza para celebrar y compartir.",
                    fontSize = 12.sp,
                    color = CarpayTextSecondary
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(authViewModel = null)
}