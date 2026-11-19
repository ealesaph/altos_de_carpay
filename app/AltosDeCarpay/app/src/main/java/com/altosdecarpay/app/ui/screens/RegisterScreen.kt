package com.altosdecarpay.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
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
import com.altosdecarpay.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    // Control de paso (1 = Cuenta y RUT, 2 = Datos personales)
    var currentStep by remember { mutableIntStateOf(1) }

    // Estados Paso 1
    var correo by remember { mutableStateOf("") }
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rut by remember { mutableStateOf("") }

    // Estados Paso 2
    var nombre by remember { mutableStateOf("") }
    var apellidoPaterno by remember { mutableStateOf("") }
    var apellidoMaterno by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var diaNac by remember { mutableStateOf("") }
    var mesNac by remember { mutableStateOf("") }
    var anioNac by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CarpayBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // --- HEADER ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
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

            Spacer(modifier = Modifier.height(28.dp))

            if (currentStep == 1) {
                // ==========================================
                // PASO 1 DE 2: CREA TU CUENTA
                // ==========================================
                Text(
                    text = "COMIENZA AQUÍ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = CarpayPrimaryGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Crea tu cuenta",
                    fontFamily = FontFamily.Serif,
                    fontSize = 32.sp,
                    color = CarpayTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Completa tus datos para registrarte.",
                    fontSize = 14.sp,
                    color = CarpayTextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Correo
                RegisterLabel("Correo:")
                RegisterTextField(
                    value = correo,
                    onValueChange = { correo = it; errorMessage = null },
                    placeholder = "nombre@correo.cl",
                    keyboardType = KeyboardType.Email
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Usuario
                RegisterLabel("Usuario:")
                RegisterTextField(
                    value = usuario,
                    onValueChange = { usuario = it; errorMessage = null },
                    placeholder = "Elige tu nombre de usuario"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Contraseña con botón textual "Mostrar" / "Ocultar"
                RegisterLabel("Contraseña:")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    placeholder = {
                        Text("Crea una contraseña", color = CarpayTextSecondary, fontSize = 14.sp)
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        Text(
                            text = if (passwordVisible) "Ocultar" else "Mostrar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CarpayPrimaryGreen,
                            modifier = Modifier
                                .clickable { passwordVisible = !passwordVisible }
                                .padding(end = 14.dp)
                        )
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Usa al menos 8 caracteres.",
                    fontSize = 12.sp,
                    color = CarpayTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Rut
                RegisterLabel("Rut:")
                RegisterTextField(
                    value = rut,
                    onValueChange = { rut = it; errorMessage = null },
                    placeholder = "12.345.678-9"
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ingresa tu RUT con dígito verificador.",
                    fontSize = 12.sp,
                    color = CarpayTextSecondary
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón Pasar al Paso 2
                Button(
                    onClick = {
                        when {
                            correo.isBlank() || usuario.isBlank() || password.isBlank() || rut.isBlank() -> {
                                errorMessage = "Completa todos los campos para continuar."
                            }
                            password.length < 8 -> {
                                errorMessage = "La contraseña debe tener al menos 8 caracteres."
                            }
                            else -> {
                                errorMessage = null
                                currentStep = 2
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CarpayPrimaryGreen,
                        contentColor = CarpayWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Crear cuenta",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "¿Ya tienes cuenta? ",
                        fontSize = 14.sp,
                        color = CarpayTextSecondary
                    )
                    Text(
                        text = "Inicia sesión",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarpayPrimaryGreen,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }

            } else {
                // ==========================================
                // PASO 2 DE 2: TUS DATOS PERSONALES
                // ==========================================
                Text(
                    text = "PASO 2 DE 2",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = CarpayPrimaryGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tus datos personales",
                    fontFamily = FontFamily.Serif,
                    fontSize = 32.sp,
                    color = CarpayTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Completa tu perfil para finalizar el registro.",
                    fontSize = 14.sp,
                    color = CarpayTextSecondary
                )

                Spacer(modifier = Modifier.height(22.dp))

                RegisterLabel("Nombre:")
                RegisterTextField(
                    value = nombre,
                    onValueChange = { nombre = it; errorMessage = null },
                    placeholder = "Ingresa tu nombre"
                )

                Spacer(modifier = Modifier.height(14.dp))

                RegisterLabel("Apellido Paterno:")
                RegisterTextField(
                    value = apellidoPaterno,
                    onValueChange = { apellidoPaterno = it; errorMessage = null },
                    placeholder = "Ingresa tu apellido paterno"
                )

                Spacer(modifier = Modifier.height(14.dp))

                RegisterLabel("Apellido Materno:")
                RegisterTextField(
                    value = apellidoMaterno,
                    onValueChange = { apellidoMaterno = it; errorMessage = null },
                    placeholder = "Ingresa tu apellido materno"
                )

                Spacer(modifier = Modifier.height(14.dp))

                RegisterLabel("Dirección:")
                RegisterTextField(
                    value = direccion,
                    onValueChange = { direccion = it; errorMessage = null },
                    placeholder = "Calle, número y comuna"
                )

                Spacer(modifier = Modifier.height(14.dp))

                RegisterLabel("Numero de contacto:")
                RegisterTextField(
                    value = telefono,
                    onValueChange = { telefono = it; errorMessage = null },
                    placeholder = "+56 9 1234 5678",
                    keyboardType = KeyboardType.Phone
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Año de nacimiento (DD / MM / YYYY)
                RegisterLabel("Año de nacimiento:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RegisterTextField(
                        value = diaNac,
                        onValueChange = { if (it.length <= 2) diaNac = it },
                        placeholder = "DD",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    RegisterTextField(
                        value = mesNac,
                        onValueChange = { if (it.length <= 2) mesNac = it },
                        placeholder = "MM",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    RegisterTextField(
                        value = anioNac,
                        onValueChange = { if (it.length <= 4) anioNac = it },
                        placeholder = "YYYY",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón Finalizar registro
                Button(
                    onClick = {
                        if (nombre.isBlank() || apellidoPaterno.isBlank() || telefono.isBlank()) {
                            errorMessage = "Por favor completa al menos tu nombre, apellido y teléfono."
                            return@Button
                        }
                        coroutineScope.launch {
                            isLoading = true
                            delay(1000) // Simula POST /api/auth/register/ hacia Django
                            isLoading = false
                            onRegisterSuccess()
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
                            text = "Finalizar registro",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Volver al paso anterior",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarpayPrimaryGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            errorMessage = null
                            currentStep = 1
                        }
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun RegisterLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = CarpayTextPrimary
    )
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun RegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = CarpayTextSecondary, fontSize = 14.sp)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CarpayWhite,
            unfocusedContainerColor = CarpayWhite,
            focusedBorderColor = CarpayPrimaryGreen,
            unfocusedBorderColor = CarpayInputBorder,
            focusedTextColor = CarpayTextPrimary,
            unfocusedTextColor = CarpayTextPrimary
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen()
}