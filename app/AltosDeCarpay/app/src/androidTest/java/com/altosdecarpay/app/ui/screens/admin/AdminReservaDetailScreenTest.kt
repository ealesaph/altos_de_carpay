package com.altosdecarpay.app.ui.screens.admin

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.altosdecarpay.app.data.SessionState
import com.altosdecarpay.app.data.remote.TokenManager
import com.altosdecarpay.app.ui.theme.AltosDeCarpayTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminReservaDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun limpiarSesion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TokenManager(context).clearSession()
        SessionState.clear()
    }

    @Test
    fun adminReservaDetail_displaysReservationInfo() {
        composeTestRule.setContent {
            AltosDeCarpayTheme {
                AdminReservaDetailScreen(reservaId = "RES-0248")
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Camila Rojas").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Detalle de reserva").assertIsDisplayed()
        composeTestRule.onNodeWithText("RES-0248").assertIsDisplayed()
        // El título de la tarjeta ahora es "Reserva <sector principal>"
        composeTestRule.onNodeWithText("Reserva Quincho Los Aromos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Camila Rojas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quincho Los Aromos (4h)").assertIsDisplayed()
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

        composeTestRule.onNodeWithText("Control de espacios").performScrollTo().performClick()

        assertEquals("RES-0248", clickedReservaId)
    }
}
