package com.altosdecarpay.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginHelperTest {

    @Test
    fun validateInputs_emptyIdentificador_returnsError() {
        val result = LoginHelper.validateInputs("", "123456")
        assertEquals("Por favor, completa ambos campos.", result)
    }

    @Test
    fun validateInputs_blankIdentificador_returnsError() {
        val result = LoginHelper.validateInputs("   ", "123456")
        assertEquals("Por favor, completa ambos campos.", result)
    }

    @Test
    fun validateInputs_emptyPassword_returnsError() {
        val result = LoginHelper.validateInputs("user@carpay.com", "")
        assertEquals("Por favor, completa ambos campos.", result)
    }

    @Test
    fun validateInputs_validInputs_returnsNull() {
        val result = LoginHelper.validateInputs("user@carpay.com", "123456")
        assertNull(result)
    }
}
