package com.altosdecarpay.app.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Mismas reglas que el backend (BLOQUES_HORARIOS) y la web (reglas.js). */
class ReglasArriendoTest {

    @Test
    fun horasDeInicioPorBloque() {
        assertEquals(listOf(13, 14, 15, 16, 17), ReglasArriendo.horasDeInicio(ReglasArriendo.bloques[0]))
        assertEquals(listOf(19, 20, 21, 22, 23), ReglasArriendo.horasDeInicio(ReglasArriendo.bloques[1]))
    }

    @Test
    fun maximoDeHorasSinSalirDelBloque() {
        assertEquals(5, ReglasArriendo.maximoHoras(13))
        assertEquals(1, ReglasArriendo.maximoHoras(17))
        assertEquals(2, ReglasArriendo.maximoHoras(22))
        assertEquals(0, ReglasArriendo.maximoHoras(18))
        assertEquals("00:00", ReglasArriendo.horaTermino(19, 5))
    }

    @Test
    fun anticipoYFechaMinima() {
        // 2 espacios × 4 h: Mirador $25.000 + Piscina $30.000 por hora = $220.000 → anticipo $66.000
        assertEquals(66000L, ReglasArriendo.anticipoEstimado(220000L))
        assertEquals(LocalDate.of(2026, 11, 24), ReglasArriendo.fechaMinima(LocalDate.of(2026, 11, 23)))
    }
}
