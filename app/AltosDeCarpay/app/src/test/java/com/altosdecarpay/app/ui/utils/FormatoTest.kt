package com.altosdecarpay.app.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/** Formatos de presentación (hallazgos APP-B1, B2, B3 y A4 del informe de pruebas de la app). */
class FormatoTest {

    @Test
    fun montosEnPesosChilenos() {
        assertEquals("$25.000", Formato.clp("25000.00"))
        assertEquals("$1.500", Formato.clp("1500.00"))
        assertEquals("$0", Formato.clp(null))
    }

    @Test
    fun rutYTelefonoConFormato() {
        assertEquals("19.876.543-0", Formato.rut("198765430"))
        assertEquals("11.111.111-1", Formato.rut("11111111-1"))
        assertEquals("+56 9 2222 3333", Formato.telefono("+56922223333"))
        assertEquals("+56 9 1234 5678", Formato.telefono("912345678"))
    }

    @Test
    fun fechaLegible() {
        assertEquals("7 dic 2026", Formato.fecha("2026-12-07"))
        assertEquals("13:00", Formato.hora("13:00:00"))
    }

    @Test
    fun mensajeDeErrorSinJsonCrudo() {
        assertEquals("El acceso se habilita el 16/12/2026.", Formato.mensajeError("""{"detail":"El acceso se habilita el 16/12/2026."}"""))
        assertEquals("Este campo es obligatorio.", Formato.mensajeError("""{"titulo":["Este campo es obligatorio."]}"""))
        assertEquals("Falla", Formato.mensajeError(null, "Falla"))
    }

    @Test
    fun nombresDeEstado() {
        assertEquals("En curso", Formato.estado("En Proceso"))
        assertEquals("No se presentó", Formato.estado("No se presentó"))
        assertEquals("Pendiente de pago", Formato.estado("Pendiente"))
        assertEquals("Sin responder", Formato.estadoConsulta("Pendiente"))
        assertEquals("Respondida", Formato.estadoConsulta("Respondida"))
    }
}
