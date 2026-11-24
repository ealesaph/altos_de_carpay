package com.altosdecarpay.app.ui.utils

import java.time.LocalDate

/**
 * Reglas de arriendo acordadas con el cliente. Deben coincidir con BLOQUES_HORARIOS de
 * backend/altosDeCarpay/altosDeCarpay/settings.py y con frontend/src/reglas.js.
 */
object ReglasArriendo {
    data class Bloque(val nombre: String, val desde: Int, val hasta: Int)

    val bloques = listOf(Bloque("Diurno", 13, 18), Bloque("Vespertino", 19, 24))

    /** Porcentaje del anticipo (solo para el estimado; el monto definitivo lo calcula el servidor). */
    const val ANTICIPO_PORCENTAJE = 30

    private fun hh(hora: Int) = "%02d:00".format(hora % 24)

    val textoBloques: String get() = bloques.joinToString(" y ") { "${it.nombre.lowercase()} ${hh(it.desde)}–${hh(it.hasta)}" }

    /** Horas de inicio válidas, agrupadas por bloque. */
    fun horasDeInicio(bloque: Bloque): List<Int> = (bloque.desde until bloque.hasta).toList()

    fun bloqueDe(horaInicio: Int): Bloque? = bloques.firstOrNull { horaInicio >= it.desde && horaInicio < it.hasta }

    /** Máximo de horas que caben desde la hora de inicio sin salir de su bloque. */
    fun maximoHoras(horaInicio: Int): Int = bloqueDe(horaInicio)?.let { it.hasta - horaInicio } ?: 0

    fun textoHora(hora: Int): String = hh(hora)

    fun horaTermino(horaInicio: Int, horas: Int): String = hh(horaInicio + horas)

    /** Se reserva desde mañana (igual que la web). */
    fun fechaMinima(hoy: LocalDate = LocalDate.now()): LocalDate = hoy.plusDays(1)

    /** Anticipo estimado, redondeado al peso como el servidor (ROUND_HALF_UP). */
    fun anticipoEstimado(total: Long): Long = (total * ANTICIPO_PORCENTAJE + 50) / 100
}
