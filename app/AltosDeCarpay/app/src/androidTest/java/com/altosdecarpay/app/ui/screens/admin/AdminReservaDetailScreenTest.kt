package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altosdecarpay.app.ui.theme.AltosDeCarpayTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminReservaDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun adminReservaDetail_displaysReservationInfo() {
        composeTestRule.setContent {
            AltosDeCarpayTheme {
                AdminReservaDetailScreen(reservaId = "RES-0248")
            }
        }

        composeTestRule.onNodeWithText("Detalle de reserva").assertIsDisplayed()
        composeTestRule.onNodeWithText("RES-0248").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cumpleaños de Camila").assertIsDisplayed()
        composeTestRule.onNodeWithText("Camila Rojas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quincho Los Aromos").assertIsDisplayed()
    }

    @Test
    fun adminReservaDetail_clickSpacesControl_triggersCallback() {
        var clickedReservaId: String? = null

        composeTestRule.setContent {
            AltosDeCarpayTheme {
                AdminReservaDetailScreen(
                    reservaId = "RES-0248",
                    onNavigateToSpacesControl = { id -> clickedReservaId = id }
                )
            }
        }

        composeTestRule.onNodeWithText("Control de espacios").performClick()

        assertEquals("RES-0248", clickedReservaId)
    }
}
