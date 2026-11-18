package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altosdecarpay.app.ui.theme.AltosDeCarpayTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminDashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun adminDashboard_displaysHeaderAndKpis() {
        composeTestRule.setContent {
            AltosDeCarpayTheme {
                AdminDashboardScreen()
            }
        }

        composeTestRule.onNodeWithText("La agenda de hoy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reservas hoy").assertIsDisplayed()
        composeTestRule.onNodeWithText("En curso").assertIsDisplayed()
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

        composeTestRule.onNodeWithText("Cumpleaños de Camila").performClick()

        assertEquals("RES-0248", selectedReservaId)
    }
}
