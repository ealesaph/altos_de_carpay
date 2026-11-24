package com.altosdecarpay.app

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altosdecarpay.app.data.SessionState
import com.altosdecarpay.app.data.model.ConsentimientoRequest
import com.altosdecarpay.app.data.remote.ApiClient
import com.altosdecarpay.app.ui.screens.LoginScreen
import com.altosdecarpay.app.ui.screens.PoliticaPrivacidadDialog
import com.altosdecarpay.app.ui.screens.RegisterScreen
import com.altosdecarpay.app.ui.screens.admin.*
import com.altosdecarpay.app.ui.screens.user.*
import com.altosdecarpay.app.ui.theme.AltosDeCarpayTheme
import com.altosdecarpay.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

private const val PERMISO_RED_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK"

class MainActivity : ComponentActivity() {

    // Android 17: sin este permiso el sistema descarta en silencio las conexiones a 10.0.2.2 y a IPs privadas,
    // y la app solo ve «failed to connect ... after 30000ms».
    private val solicitarRedLocal = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (!concedido) {
            Toast.makeText(
                this,
                "Sin el permiso «Dispositivos cercanos» la app no puede conectarse al servidor de la red local. " +
                    "Actívalo en Ajustes > Apps > Altos de Carpay > Permisos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun verificarPermisoRedLocal() {
        if (Build.VERSION.SDK_INT < 37 || !ApiClient.usaRedLocal(this)) return
        if (ContextCompat.checkSelfPermission(this, PERMISO_RED_LOCAL) != PackageManager.PERMISSION_GRANTED) {
            solicitarRedLocal.launch(PERMISO_RED_LOCAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        verificarPermisoRedLocal()
        setContent {
            AltosDeCarpayTheme {
                AppNavigationHost(
                    onShowToast = { mensaje ->
                        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

sealed class AppRoute {
    data object Login : AppRoute()
    data object Register : AppRoute()

    // --- Rutas Administrador ---
    data object AdminDashboard : AppRoute()
    data object AdminScanner : AppRoute()
    data class AdminExitConfirm(val reservaId: String) : AppRoute()
    data class AdminReservaDetail(val reservaId: String) : AppRoute()
    data class AdminSpacesControl(val reservaId: String) : AppRoute()
    data object AdminTickets : AppRoute()
    data class AdminBitacora(val reservaId: String?) : AppRoute()   // null = bitácora general
    data object AdminProducts : AppRoute()
    data object AdminProfile : AppRoute()

    // --- Rutas Usuario / Cliente ---
    data object UserReservations : AppRoute()
    data class UserReservaDetail(val reservaId: String) : AppRoute()
    data class UserExtraServices(val reservaId: String) : AppRoute()
    data object UserSpaces : AppRoute()
    data object UserNuevaReserva : AppRoute()
    data class UserPago(val reservaId: String) : AppRoute()
    data object UserTickets : AppRoute()
    data class UserObservations(val reservaId: String) : AppRoute()
    data object UserProfile : AppRoute()
}

@Composable
fun AppNavigationHost(
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { ApiClient.getTokenManager(context) }
    val authViewModel: AuthViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient.getApiService(context) }

    // Ley N° 21.719: si el titular no ha aceptado la versión vigente de la política, se le solicita
    // antes de usar la app (también al reabrir la app con una sesión guardada).
    var consentimientoPendiente by remember {
        mutableStateOf(tokenManager.isLoggedIn && tokenManager.userInfo?.requiereConsentimiento == true)
    }
    var guardandoConsentimiento by remember { mutableStateOf(false) }

    var currentRoute by remember {
        mutableStateOf<AppRoute>(
            if (tokenManager.isLoggedIn) {
                if (tokenManager.userRole == "Admin") AppRoute.AdminDashboard else AppRoute.UserReservations
            } else {
                AppRoute.Login
            }
        )
    }

    val handleAdminTabSelection: (AdminTab) -> Unit = { tab ->
        currentRoute = when (tab) {
            AdminTab.RESERVAS -> AppRoute.AdminDashboard
            AdminTab.ESCANEAR -> AppRoute.AdminScanner
            AdminTab.TICKETS -> AppRoute.AdminTickets
            AdminTab.PERFIL -> AppRoute.AdminProfile
        }
    }

    val handleUserTabSelection: (UserTab) -> Unit = { tab ->
        currentRoute = when (tab) {
            UserTab.RESERVAS -> AppRoute.UserReservations
            UserTab.EXPLORAR -> AppRoute.UserSpaces
            UserTab.AYUDA -> AppRoute.UserTickets
            UserTab.PERFIL -> AppRoute.UserProfile
        }
    }

    BackHandler(enabled = currentRoute != AppRoute.Login) {
        currentRoute = when (currentRoute) {
            // Back Acceso
            is AppRoute.Register -> AppRoute.Login

            // Back Admin
            is AppRoute.AdminExitConfirm -> AppRoute.AdminScanner
            is AppRoute.AdminSpacesControl -> AppRoute.AdminReservaDetail((currentRoute as AppRoute.AdminSpacesControl).reservaId)
            is AppRoute.AdminReservaDetail -> AppRoute.AdminDashboard
            is AppRoute.AdminBitacora -> (currentRoute as AppRoute.AdminBitacora).reservaId
                ?.let { AppRoute.AdminReservaDetail(it) } ?: AppRoute.AdminProfile
            is AppRoute.AdminProducts -> AppRoute.AdminProfile
            is AppRoute.AdminScanner,
            is AppRoute.AdminTickets,
            is AppRoute.AdminProfile -> AppRoute.AdminDashboard

            // Back Usuario
            is AppRoute.UserReservaDetail,
            is AppRoute.UserExtraServices,
            is AppRoute.UserSpaces,
            is AppRoute.UserNuevaReserva,
            is AppRoute.UserObservations,
            is AppRoute.UserTickets,
            is AppRoute.UserProfile -> AppRoute.UserReservations
            is AppRoute.UserPago -> AppRoute.UserReservaDetail((currentRoute as AppRoute.UserPago).reservaId)

            else -> AppRoute.Login
        }
    }

    // Si el servidor rechaza la sesión y no se puede renovar, se vuelve al inicio de sesión con un aviso
    // (antes la app seguía en pantalla sin sesión y mostraba datos de ejemplo).
    val sesionExpirada by SessionState.sesionExpirada.collectAsState()
    LaunchedEffect(sesionExpirada) {
        if (sesionExpirada) {
            SessionState.sesionExpirada.value = false
            if (currentRoute != AppRoute.Login && currentRoute != AppRoute.Register) {
                currentRoute = AppRoute.Login
                onShowToast("Tu sesión expiró. Inicia sesión nuevamente.")
            }
        }
    }
    val terminarSesion: (String) -> Unit = { mensaje ->
        onShowToast(mensaje)
        currentRoute = AppRoute.Login
    }

    when (val route = currentRoute) {
        is AppRoute.Login -> {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { rol ->
                    SessionState.sesionExpirada.value = false
                    consentimientoPendiente = SessionState.userInfo?.requiereConsentimiento == true
                    if (rol == "Admin") {
                        onShowToast("Acceso Administración")
                        currentRoute = AppRoute.AdminDashboard
                    } else {
                        val nombre = SessionState.userName.ifBlank { "Área de Usuario" }
                        onShowToast("Bienvenido/a ($nombre)")
                        currentRoute = AppRoute.UserReservations
                    }
                },
                onNavigateToRegister = {
                    currentRoute = AppRoute.Register
                }
            )
        }

        is AppRoute.Register -> {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = {
                    onShowToast("¡Cuenta creada con éxito! Inicia sesión para continuar.")
                    currentRoute = AppRoute.Login
                },
                onNavigateToLogin = {
                    currentRoute = AppRoute.Login
                }
            )
        }

        // --- MÓDULO ADMINISTRACIÓN ---
        is AppRoute.AdminDashboard -> AdminDashboardScreen(
            onNavigateToScanner = { currentRoute = AppRoute.AdminScanner },
            onSelectReserva = { id -> currentRoute = AppRoute.AdminReservaDetail(id) },
            onTabSelected = handleAdminTabSelection
        )
        is AppRoute.AdminScanner -> AdminScannerScreen(
            onBackClick = { currentRoute = AppRoute.AdminDashboard },
            onQrReadSuccess = { id, mov ->
                if (mov == "Salida") currentRoute = AppRoute.AdminExitConfirm(id)
                else {
                    onShowToast("Ingreso registrado · $id")
                    currentRoute = AppRoute.AdminReservaDetail(id)
                }
            },
            onTabSelected = handleAdminTabSelection
        )
        is AppRoute.AdminExitConfirm -> AdminExitConfirmScreen(
            reservaId = route.reservaId,
            onBackClick = { currentRoute = AppRoute.AdminScanner },
            onConfirmExit = { id ->
                onShowToast("Salida confirmada · $id finalizada")
                currentRoute = AppRoute.AdminReservaDetail(id)
            },
            onTabSelected = handleAdminTabSelection
        )
        is AppRoute.AdminReservaDetail -> AdminReservaDetailScreen(
            reservaId = route.reservaId,
            onBackClick = { currentRoute = AppRoute.AdminDashboard },
            onNavigateToSpacesControl = { id -> currentRoute = AppRoute.AdminSpacesControl(id) },
            onNavigateToTickets = { currentRoute = AppRoute.AdminTickets },
            onNavigateToBitacora = { id -> currentRoute = AppRoute.AdminBitacora(id) },
            onTabSelected = handleAdminTabSelection
        )
        is AppRoute.AdminSpacesControl -> AdminSpacesControlScreen(
            reservaId = route.reservaId,
            onBackClick = { currentRoute = AppRoute.AdminReservaDetail(route.reservaId) },
            onTabSelected = handleAdminTabSelection
        )
        is AppRoute.AdminTickets -> AdminTicketsScreen(onTabSelected = handleAdminTabSelection)
        is AppRoute.AdminBitacora -> AdminBitacoraScreen(
            reservaId = route.reservaId,
            onBackClick = { currentRoute = route.reservaId?.let { AppRoute.AdminReservaDetail(it) } ?: AppRoute.AdminProfile },
            onTabSelected = handleAdminTabSelection
        )
        is AppRoute.AdminProducts -> AdminProductsScreen(
            onBackClick = { currentRoute = AppRoute.AdminProfile },
            onTabSelected = handleAdminTabSelection
        )
        is AppRoute.AdminProfile -> AdminProfileScreen(
            authViewModel = authViewModel,
            onNavigateToBitacora = { currentRoute = AppRoute.AdminBitacora(null) },
            onNavigateToProducts = { currentRoute = AppRoute.AdminProducts },
            onLogout = { currentRoute = AppRoute.Login },
            onSesionTerminada = terminarSesion,
            onTabSelected = handleAdminTabSelection
        )

        // --- MÓDULO USUARIO / CLIENTE ---
        is AppRoute.UserReservations -> UserReservationsScreen(
            onOpenReservaDetail = { id -> currentRoute = AppRoute.UserReservaDetail(id) },
            onOpenExtraServices = { id -> currentRoute = AppRoute.UserExtraServices(id) },
            onOpenMySpaces = { currentRoute = AppRoute.UserSpaces },
            onOpenObservations = { id -> currentRoute = AppRoute.UserObservations(id) },
            onNuevaReserva = { currentRoute = AppRoute.UserNuevaReserva },
            onTabSelected = handleUserTabSelection
        )
        is AppRoute.UserNuevaReserva -> UserNuevaReservaScreen(
            onBackClick = { currentRoute = AppRoute.UserReservations },
            onReservaCreada = { id -> currentRoute = AppRoute.UserPago("RES-%04d".format(id)) },
            onTabSelected = handleUserTabSelection
        )
        is AppRoute.UserPago -> UserPagoScreen(
            reservaId = route.reservaId,
            onBackClick = { currentRoute = AppRoute.UserReservaDetail(route.reservaId) },
            onVerReserva = { id -> currentRoute = AppRoute.UserReservaDetail(id) },
            onTabSelected = handleUserTabSelection
        )
        is AppRoute.UserReservaDetail -> UserReservaDetailScreen(
            reservaId = route.reservaId,
            onBackClick = { currentRoute = AppRoute.UserReservations },
            // La consulta desde el detalle queda asociada a la reserva (asunto «RES-XXXX: …»).
            onOpenTickets = { currentRoute = AppRoute.UserObservations(route.reservaId) },
            onPagar = { currentRoute = AppRoute.UserPago(route.reservaId) },
            onTabSelected = handleUserTabSelection
        )
        is AppRoute.UserExtraServices -> UserExtraServicesScreen(
            reservaId = route.reservaId,
            onBackClick = { currentRoute = AppRoute.UserReservations },
            onRequestSubmitted = {
                onShowToast("Solicitud de extras enviada al equipo")
                currentRoute = AppRoute.UserReservations
            },
            onTabSelected = handleUserTabSelection
        )
        is AppRoute.UserSpaces -> UserSpacesScreen(
            onBackClick = { currentRoute = AppRoute.UserReservations },
            onReservar = { currentRoute = AppRoute.UserNuevaReserva },
            onTabSelected = handleUserTabSelection
        )
        is AppRoute.UserTickets -> UserTicketsScreen(
            onBackClick = { currentRoute = AppRoute.UserReservations },
            onTabSelected = handleUserTabSelection
        )
        is AppRoute.UserObservations -> UserObservationsScreen(
            reservaId = route.reservaId,
            onBackClick = { currentRoute = AppRoute.UserReservations },
            onTabSelected = handleUserTabSelection
        )
        is AppRoute.UserProfile -> UserProfileScreen(
            authViewModel = authViewModel,
            onLogout = { currentRoute = AppRoute.Login },
            onSesionTerminada = terminarSesion,
            onTabSelected = handleUserTabSelection
        )
    }

    if (consentimientoPendiente) {
        PoliticaPrivacidadDialog(
            onCerrar = {},
            textoIntro = "Actualizamos nuestra política de tratamiento de datos personales conforme a la " +
                "Ley N° 21.719. Para seguir usando la app debes leerla y aceptarla.",
            procesando = guardandoConsentimiento,
            onAceptar = { version ->
                scope.launch {
                    guardandoConsentimiento = true
                    try {
                        val response = api.registrarConsentimiento(ConsentimientoRequest(acepta = true, version = version))
                        if (response.isSuccessful) {
                            val actualizado = tokenManager.userInfo?.copy(requiereConsentimiento = false)
                            tokenManager.userInfo = actualizado
                            SessionState.userInfo = actualizado
                            consentimientoPendiente = false
                            onShowToast("Consentimiento registrado. ¡Gracias!")
                        } else {
                            onShowToast("No se pudo registrar el consentimiento. Intenta nuevamente.")
                        }
                    } catch (e: Exception) {
                        onShowToast("Sin conexión con el servidor. Intenta nuevamente.")
                    } finally {
                        guardandoConsentimiento = false
                    }
                }
            },
            onRechazar = {
                authViewModel.logout {
                    consentimientoPendiente = false
                    currentRoute = AppRoute.Login
                    onShowToast("Sin tu consentimiento no podemos tratar tus datos. Se cerró la sesión.")
                }
            }
        )
    }
}
