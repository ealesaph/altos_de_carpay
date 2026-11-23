package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.altosdecarpay.app.data.SessionState
import com.altosdecarpay.app.data.remote.TokenManager
import com.altosdecarpay.app.ui.theme.AltosDeCarpayTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminDashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Las pruebas de UI no deben depender de la sesión guardada en el emulador:
     * sin token la API responde 401 y la pantalla usa sus datos de demostración (RES-0248 / RES-0249).
     */
    @Before
    fun limpiarSesion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TokenManager(context).clearSession()
        SessionState.clear()
    }

    @Test
    fun adminDashboard_displaysHeaderAndKpis() {
        composeTestRule.setContent {
            AltosDeCarpayTheme {
                AdminDashboardScreen()
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("La agenda").assertIsDisplayed()
        // "Reservas" y "En curso" aparecen también en la barra inferior, los filtros y las tarjetas
        composeTestRule.onAllNodesWithText("Reservas").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("En curso").onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText("Tickets abiertos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Escanear QR · ingreso o salida").assertIsDisplayed()
    }

    @Test
    fun adminDashboard_clickScannerButton_triggersCallback() {
        var scannerClicked = false

        composeTestRule.setContent {
            AltosDeCarpayTheme {
                AdminDashboardScreen(
                    onNavigateToScanner = { scannerClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Escanear QR · ingreso o salida").performClick()

        assertTrue(scannerClicked)
    }

    @Test
    fun adminDashboard_clickReserva_triggersSelectReservaCallback() {
        var selectedReservaId: String? = null

        composeTestRule.setContent {
            AltosDeCarpayTheme {
                AdminDashboardScreen(
                    onSelectReserva = { id -> selectedReservaId = id }
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Cumpleaños de Camila").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Cumpleaños de Camila").performClick()

        assertEquals("RES-0248", selectedReservaId)
    }
}
