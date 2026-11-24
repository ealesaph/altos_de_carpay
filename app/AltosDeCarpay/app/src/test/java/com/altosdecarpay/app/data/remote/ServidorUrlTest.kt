package com.altosdecarpay.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Dirección del servidor escrita a mano en «Servidor» (uso en emulador y celular a la vez). */
class ServidorUrlTest {

    @Test
    fun completaEsquemaPuertoYRuta() {
        assertEquals("http://192.168.1.20:8000/api/v1/", ApiClient.normalizarUrl("192.168.1.20"))
        assertEquals("http://192.168.1.20:8000/api/v1/", ApiClient.normalizarUrl(" 192.168.1.20:8000/ "))
        assertEquals("http://127.0.0.1:8000/api/v1/", ApiClient.normalizarUrl("http://127.0.0.1:8000/api/v1/"))
        assertEquals("https://carpay.cl/api/v1/", ApiClient.normalizarUrl("https://carpay.cl"))
    }

    @Test
    fun rechazaTextoInvalido() {
        assertNull(ApiClient.normalizarUrl(""))
        assertNull(ApiClient.normalizarUrl("mi servidor"))
    }
}
