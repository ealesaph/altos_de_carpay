package com.altosdecarpay.app.ui.screens.user

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
class UserReservationsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun limpiarSesion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TokenManager(context).clearSession()
        SessionState.clear()
    }

    @Test
    fun userReservations_displaysHeaderAndTitle() {
        composeTestRule.setContent {
            AltosDeCarpayTheme {
                UserReservationsScreen()
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Un almuerzo en familia").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Tus buenos momentos").assertIsDisplayed()
        // El saludo usa el nombre de la sesión activa ("Usuario" si no hay sesión)
        composeTestRule.onNodeWithText("Tu próximo encuentro empieza aquí.", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Un almuerzo en familia").assertIsDisplayed()
    }

    @Test
    fun userReservations_clickReservaDetail_triggersCallback() {
        var selectedReservaId: String? = null

        composeTestRule.setContent {
            AltosDeCarpayTheme {
                UserReservationsScreen(
                    onOpenReservaDetail = { id -> selectedReservaId = id }
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Un almuerzo en familia").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Un almuerzo en familia").performClick()

        assertEquals("RES-0261", selectedReservaId)
    }
}
