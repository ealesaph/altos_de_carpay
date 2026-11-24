package com.altosdecarpay.app.ui.screens.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservaFilterHelperTest {

    private val hoy = "2026-11-23"

    private fun item(id: String, fecha: String, hora: String, cliente: String, estado: String) = ReservaItemUi(
        id = id, fecha = fecha, hora = hora, titulo = "Reserva Mirador", cliente = cliente,
        espacio = "Mirador", estado = estado, detalleHorario = ""
    )

    private val sampleReservas = listOf(
        item("RES-0010", "2026-11-23", "13:00", "Camila Rojas", "En Proceso"),
        item("RES-0011", "2026-12-20", "19:00", "Diego Muñoz", "Confirmado"),
        item("RES-0012", "2026-12-01", "13:00", "Valentina Soto", "Pendiente"),
        item("RES-0013", "2026-11-10", "13:00", "Roberto Gómez", "Finalizado"),
        item("RES-0014", "2026-12-05", "13:00", "Ana Pérez", "Cancelado"),
        item("RES-0015", "2026-11-20", "19:00", "Luis Díaz", "No se presentó")
    )

    @Test
    fun todas_devuelveTodas() {
        assertEquals(6, ReservaFilterHelper.filterReservas(sampleReservas, "Todas", "", hoy).size)
    }

    @Test
    fun proximas_soloVigentesDesdeHoyEnOrdenAscendente() {
        val result = ReservaFilterHelper.filterReservas(sampleReservas, "Próximas", "", hoy)
        assertEquals(listOf("RES-0010", "RES-0012", "RES-0011"), result.map { it.id })
    }

    @Test
    fun confirmadas_comparaConElEstadoDelServidor() {
        val result = ReservaFilterHelper.filterReservas(sampleReservas, "Confirmadas", "", hoy)
        assertEquals(listOf("RES-0011"), result.map { it.id })
    }

    @Test
    fun enCurso_yNoSePresento() {
        assertEquals(listOf("RES-0010"), ReservaFilterHelper.filterReservas(sampleReservas, "En curso", "", hoy).map { it.id })
        assertEquals(listOf("RES-0015"), ReservaFilterHelper.filterReservas(sampleReservas, "No se presentó", "", hoy).map { it.id })
    }

    @Test
    fun hoy_excluyeCanceladas() {
        val result = ReservaFilterHelper.filterReservas(sampleReservas, "Hoy", "", hoy)
        assertTrue(result.all { it.fecha == hoy && it.estado != "Cancelado" })
        assertEquals(1, result.size)
    }

    @Test
    fun busquedaPorClienteYPorId() {
        assertEquals("Camila Rojas", ReservaFilterHelper.filterReservas(sampleReservas, "Todas", "Camila", hoy).single().cliente)
        assertEquals("RES-0011", ReservaFilterHelper.filterReservas(sampleReservas, "Todas", "0011", hoy).single().id)
    }

    @Test
    fun combinaFiltroYBusqueda() {
        val result = ReservaFilterHelper.filterReservas(sampleReservas, "Pendientes", "Valentina", hoy)
        assertEquals("RES-0012", result.single().id)
    }
}
