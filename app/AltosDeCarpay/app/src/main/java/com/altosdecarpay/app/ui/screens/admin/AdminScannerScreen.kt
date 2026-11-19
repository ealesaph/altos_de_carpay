package com.altosdecarpay.app.ui.screens.admin

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.ui.theme.*
import com.altosdecarpay.app.ui.viewmodel.ReservasViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun AdminScannerScreen(
    reservasViewModel: ReservasViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onQrReadSuccess: (reservaId: String, tipoMovimiento: String) -> Unit = { _, _ -> },
    onTabSelected: (AdminTab) -> Unit = {}
) {
    val context = LocalContext.current
    var movimientoSeleccionado by remember { mutableStateOf("Ingreso") }
    var codigoManual by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val procesarCodigo: (String) -> Unit = { codigo ->
        if (!isProcessing && codigo.isNotBlank()) {
            isProcessing = true
            errorMessage = null
            reservasViewModel.buscarReservaPorCodigo(
                codigoRaw = codigo,
                onFound = { reserva ->
                    if (movimientoSeleccionado == "Ingreso") {
                        if (reserva.estado == "Confirmado") {
                            reservasViewModel.cambiarEstado(
                                id = reserva.idReserva,
                                estado = "En Proceso",
                                onSuccess = {
                                    isProcessing = false
                                    onQrReadSuccess(reserva.idFormateado, "Ingreso")
                                },
                                onError = { msg ->
                                    isProcessing = false
                                    errorMessage = msg
                                }
                            )
                        } else {
                            isProcessing = false
                            onQrReadSuccess(reserva.idFormateado, "Ingreso")
                        }
                    } else {
                        isProcessing = false
                        onQrReadSuccess(reserva.idFormateado, "Salida")
                    }
                },
                onError = { msg ->
                    isProcessing = false
                    errorMessage = msg
                }
            )
        }
    }

    Scaffold(
        topBar = { AdminTopBar() },
        bottomBar = {
            AdminBottomBar(
                selectedTab = AdminTab.ESCANEAR,
                onTabSelected = onTabSelected
            )
        },
        containerColor = CarpayBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdminScreenHeader(
                title = "Control de acceso",
                subtitle = "Selecciona el movimiento y escanea el QR de la reserva.",
                showBackButton = true,
                onBackClick = onBackClick
            )

            // Selector Ingreso / Salida
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CarpayGreenLight.copy(alpha = 0.7f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Ingreso", "Salida").forEach { tipo ->
                    val isSelected = movimientoSeleccionado == tipo
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) CarpayPrimaryGreen else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { movimientoSeleccionado = tipo }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (tipo == "Ingreso") Icons.AutoMirrored.Outlined.Login else Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                tint = if (isSelected) CarpayWhite else CarpayTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tipo,
                                color = if (isSelected) CarpayWhite else CarpayTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Visor de Cámara Real con CameraX + ML Kit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A2B22))
            ) {
                if (hasCameraPermission) {
                    CameraQrPreview(
                        onCameraReady = { cameraInstance = it },
                        onQrDetected = { qrValue ->
                            if (!isProcessing) {
                                procesarCodigo(qrValue)
                            }
                        }
                    )

                    // Overlay superior
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "• CÁMARA ACTIVA",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                torchEnabled = !torchEnabled
                                cameraInstance?.cameraControl?.enableTorch(torchEnabled)
                            }
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Outlined.FlashlightOff else Icons.Outlined.FlashlightOn,
                                contentDescription = "Linterna",
                                tint = CarpayWhite
                            )
                        }
                    }

                    // Marco guía central
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .align(Alignment.Center)
                            .border(2.dp, CarpayWhite.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    )

                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CarpayWhite)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = CarpayWhite,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Se requiere permiso de cámara para escanear el QR de la reserva.",
                            color = CarpayWhite,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CarpayPrimaryGreen,
                                contentColor = CarpayWhite
                            )
                        ) {
                            Text("Permitir cámara")
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CarpayRedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = CarpayRedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Aviso informativo
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = CarpayTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Apunta la cámara al código QR de la reserva del cliente para validar el acceso en la base de datos.",
                    fontSize = 12.sp,
                    color = CarpayTextSecondary,
                    lineHeight = 16.sp
                )
            }

            HorizontalDivider(color = CarpayInputBorder)

            // Búsqueda manual
            Text(
                text = "¿No puedes leer el QR?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CarpayTextPrimary
            )
            Text(
                text = "Identificador o token de reserva",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CarpayTextPrimary
            )
            OutlinedTextField(
                value = codigoManual,
                onValueChange = {
                    codigoManual = it
                    errorMessage = null
                },
                placeholder = { Text("Ej: 1 o RES-0001") },
                leadingIcon = {
                    Text("#", fontWeight = FontWeight.Bold, color = CarpayTextSecondary, fontSize = 16.sp)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CarpayWhite,
                    unfocusedContainerColor = CarpayWhite,
                    focusedBorderColor = CarpayPrimaryGreen,
                    unfocusedBorderColor = CarpayInputBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = { procesarCodigo(codigoManual) },
                enabled = !isProcessing && codigoManual.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CarpayInputBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CarpayWhite,
                    contentColor = CarpayPrimaryGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscar reserva en BD", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraQrPreview(
    onCameraReady: (Camera) -> Unit,
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val firstQr = barcodes.firstOrNull()?.rawValue
                                if (!firstQr.isNullOrBlank()) {
                                    onQrDetected(firstQr)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    onCameraReady(camera)
                } catch (_: Exception) { }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
