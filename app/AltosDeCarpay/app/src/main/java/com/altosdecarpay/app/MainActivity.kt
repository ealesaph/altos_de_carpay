package com.altosdecarpay.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.altosdecarpay.app.ui.screens.LoginScreen
import com.altosdecarpay.app.ui.screens.RegisterScreen
import com.altosdecarpay.app.ui.screens.admin.*
import com.altosdecarpay.app.ui.screens.user.*
import com.altosdecarpay.app.ui.theme.AltosDeCarpayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    data class AdminBitacora(val reservaId: String) : AppRoute()
    data object AdminProducts : AppRoute()
    data object AdminProfile : AppRoute()

    // --- Rutas Usuario / Cliente ---
    data object UserReservations : AppRoute()
    data class UserReservaDetail(val reservaId: String) : AppRoute()
    data class UserExtraServices(val reservaId: String) : AppRoute()
    data class UserSpaces(val reservaId: String) : AppRoute()
    data object UserTickets : AppRoute()
    data class UserObservations(val reservaId: String) : AppRoute()
    data object UserProfile : AppRoute()
}

@Composable
fun AppNavigationHost(
    onShowToast: (String) -> Unit
) {
    var currentRoute by remember { mutableStateOf<AppRoute>(AppRoute.Login) }

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
            UserTab.EXPLORAR -> AppRoute.UserSpaces("RES-0261")
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
            is AppRoute.AdminBitacora,
            is AppRoute.AdminProducts -> AppRoute.AdminProfile
            is AppRoute.AdminScanner,
            is AppRoute.AdminTickets,
            is AppRoute.AdminProfile -> AppRoute.AdminDashboard

            // Back Usuario
            is AppRoute.UserReservaDetail,
            is AppRoute.UserExtraServices,
            is AppRoute.UserSpaces,
            is AppRoute.UserObservations,
            is AppRoute.UserTickets,
            is AppRoute.UserProfile -> AppRoute.UserReservations

            else -> AppRoute.Login
        }
    }

    when (val route = currentRoute) {
        is AppRoute.Login -> {
            LoginScreen(
                onLoginSuccess = { rol ->
                    if (rol == "Admin") {
                        onShowToast("Acceso Administración")
                        currentRoute = AppRoute.AdminDashboard
                    } else {
                        onShowToast("Bienvenida, Camila (Área de Usuario)")
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
                onRegisterSuccess = {
                    onShowToast("¡Cuenta creada con éxito!")
                    currentRoute = AppRoute.UserReservations
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
                    onShowToast("Ingreso registrado para $id")
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
            onBackClick = { currentRoute = AppRoute.AdminProfile },
            onTabSelected = handleAdminTabSelection
        )
        is AppRoute.AdminProducts -> AdminProductsScreen(
            onBackClick = { currentRoute = AppRoute.AdminProfile },
            onTabSelected = handleAdminTabSelection
        )
        is AppRoute.AdminProfile -> AdminProfileScreen(
            onNavigateToBitacora = { currentRoute = AppRoute.AdminBitacora("RES-0248") },
            onNavigateToProducts = { currentRoute = AppRoute.AdminProducts },
            onLogout = { currentRoute = AppRoute.Login },
            onTabSelected = handleAdminTabSelection
        )

        // --- MÓDULO USUARIO / CLIENTE ---
        is AppRoute.UserReservations -> UserReservationsScreen(
            onOpenReservaDetail = { id -> currentRoute = AppRoute.UserReservaDetail(id) },
            onOpenExtraServices = { id -> currentRoute = AppRoute.UserExtraServices(id) },
            onOpenMySpaces = { id -> currentRoute = AppRoute.UserSpaces(id) },
            onOpenObservations = { id -> currentRoute = AppRoute.UserObservations(id) },
            onTabSelected = handleUserTabSelection
        )
        is AppRoute.UserReservaDetail -> UserReservaDetailScreen(
            reservaId = route.reservaId,
            onBackClick = { currentRoute = AppRoute.UserReservations },
            onOpenTickets = { currentRoute = AppRoute.UserTickets },
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
            reservaId = route.reservaId,
            onBackClick = { currentRoute = AppRoute.UserReservations },
            onTabSelected = handleUserTabSelection
        )
        is AppRoute.UserTickets -> UserTicketsScreen(
            onBackClick = { currentRoute = AppRoute.UserReservations },
            onNavigateToObservations = { currentRoute = AppRoute.UserObservations("RES-0261") },
            onTabSelected = handleUserTabSelection
        )
        is AppRoute.UserObservations -> UserObservationsScreen(
            reservaId = route.reservaId,
            onBackClick = { currentRoute = AppRoute.UserReservations },
            onTabSelected = handleUserTabSelection
        )
        is AppRoute.UserProfile -> UserProfileScreen(
            onLogout = { currentRoute = AppRoute.Login },
            onTabSelected = handleUserTabSelection
        )
    }
}