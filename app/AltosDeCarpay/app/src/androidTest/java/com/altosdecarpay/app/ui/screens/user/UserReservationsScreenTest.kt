package com.altosdecarpay.app.ui.screens.user

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altosdecarpay.app.ui.theme.AltosDeCarpayTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserReservationsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun userReservations_displaysHeaderAndTitle() {
        composeTestRule.setContent {
            AltosDeCarpayTheme {
                UserReservationsScreen()
            }
        }

        composeTestRule.onNodeWithText("Tus buenos momentos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hola, Camila. Tu próximo encuentro empieza aquí.").assertIsDisplayed()
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

        composeTestRule.onNodeWithText("Un almuerzo en familia").performClick()

        assertEquals("RES-0261", selectedReservaId)
    }
}
