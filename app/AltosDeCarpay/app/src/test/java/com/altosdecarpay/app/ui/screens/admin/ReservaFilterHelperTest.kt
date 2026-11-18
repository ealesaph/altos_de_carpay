package com.altosdecarpay.app.ui.screens.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservaFilterHelperTest {

    private val sampleReservas = listOf(
        ReservaItemUi(
            id = "RES-0248",
            hora = "16:00",
            titulo = "Cumpleaños de Camila",
            cliente = "Camila Rojas",
            espacio = "Quincho Los Aromos",
            estado = "En curso",
            detalleHorario = "Ingreso 16:08"
        ),
        ReservaItemUi(
            id = "RES-0249",
            hora = "18:00",
            titulo = "Encuentro de equipo",
            cliente = "Diego Muñoz",
            espacio = "Salón del Roble",
            estado = "En curso",
            detalleHorario = "Ingreso 18:02"
        ),
        ReservaItemUi(
            id = "RES-0250",
            hora = "22:00",
            titulo = "Cena de aniversario",
            cliente = "Valentina Soto",
            espacio = "Jardín del Maitén",
            estado = "Confirmada",
            detalleHorario = "Sin ingreso"
        ),
        ReservaItemUi(
            id = "RES-0240",
            hora = "12:00",
            titulo = "Almuerzo corporativo",
            cliente = "Roberto Gómez",
            espacio = "Parrilla Central",
            estado = "Finalizada",
            detalleHorario = "Salida 15:30"
        )
    )

    @Test
    fun filterReservas_filterTodas_returnsAll() {
        val result = ReservaFilterHelper.filterReservas(sampleReservas, "Todas", "")
        assertEquals(4, result.size)
    }

    @Test
    fun filterReservas_filterEnCurso_returnsOnlyEnCurso() {
        val result = ReservaFilterHelper.filterReservas(sampleReservas, "En curso", "")
        assertEquals(2, result.size)
        assertTrue(result.all { it.estado == "En curso" })
    }

    @Test
    fun filterReservas_filterConfirmadas_returnsOnlyConfirmadas() {
        val result = ReservaFilterHelper.filterReservas(sampleReservas, "Confirmadas", "")
        assertEquals(1, result.size)
        assertEquals("RES-0250", result.first().id)
    }

    @Test
    fun filterReservas_filterFinalizadas_returnsOnlyFinalizadas() {
        val result = ReservaFilterHelper.filterReservas(sampleReservas, "Finalizadas", "")
        assertEquals(1, result.size)
        assertEquals("RES-0240", result.first().id)
    }

    @Test
    fun filterReservas_searchByClient_returnsMatching() {
        val result = ReservaFilterHelper.filterReservas(sampleReservas, "Todas", "Camila")
        assertEquals(1, result.size)
        assertEquals("Camila Rojas", result.first().cliente)
    }

    @Test
    fun filterReservas_searchById_returnsMatching() {
        val result = ReservaFilterHelper.filterReservas(sampleReservas, "Todas", "0249")
        assertEquals(1, result.size)
        assertEquals("RES-0249", result.first().id)
    }

    @Test
    fun filterReservas_combinesFilterAndSearch() {
        val result = ReservaFilterHelper.filterReservas(sampleReservas, "En curso", "Diego")
        assertEquals(1, result.size)
        assertEquals("RES-0249", result.first().id)
    }
}
