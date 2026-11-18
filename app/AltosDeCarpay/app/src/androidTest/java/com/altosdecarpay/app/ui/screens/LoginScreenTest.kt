package com.altosdecarpay.app.ui.screens

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
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysInitialElements() {
        composeTestRule.setContent {
            AltosDeCarpayTheme {
                LoginScreen()
            }
        }

        composeTestRule.onNodeWithText("Qué bueno verte").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inicia sesión para continuar.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Usuario o correo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña").assertIsDisplayed()
        composeTestRule.onNodeWithText("Iniciar sesión").assertIsDisplayed()
        composeTestRule.onNodeWithText("Registrarse").assertIsDisplayed()
    }

    @Test
    fun loginScreen_emptyFields_showsErrorMessage() {
        composeTestRule.setContent {
            AltosDeCarpayTheme {
                LoginScreen()
            }
        }

        composeTestRule.onNodeWithText("Iniciar sesión").performClick()

        composeTestRule.onNodeWithText("Por favor, completa ambos campos.").assertIsDisplayed()
    }

    @Test
    fun loginScreen_clickRegister_triggersCallback() {
        var registerClicked = false
        composeTestRule.setContent {
            AltosDeCarpayTheme {
                LoginScreen(onNavigateToRegister = { registerClicked = true })
            }
        }

        composeTestRule.onNodeWithText("Registrarse").performClick()

        assertTrue(registerClicked)
    }

    @Test
    fun loginScreen_enteringAdmin_callsSuccessWithAdminRole() {
        var successRole: String? = null

        composeTestRule.setContent {
            AltosDeCarpayTheme {
                LoginScreen(onLoginSuccess = { role -> successRole = role })
            }
        }

        composeTestRule.onNodeWithText("Ingresa tu usuario o correo").performTextInput("admin@carpay.com")
        composeTestRule.onNodeWithText("Ingresa tu contraseña").performTextInput("123456")
        composeTestRule.onNodeWithText("Iniciar sesión").performClick()

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            successRole != null
        }

        assertEquals("Admin", successRole)
    }
}
