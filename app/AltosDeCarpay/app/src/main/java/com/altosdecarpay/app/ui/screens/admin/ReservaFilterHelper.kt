package com.altosdecarpay.app.ui.screens.admin

/**
 * Filtros de la agenda del administrador. Se compara siempre con el estado que entrega el servidor
 * (antes se comparaba con la etiqueta traducida y «Confirmadas» nunca coincidía).
 */
object ReservaFilterHelper {

    val filtros = listOf("Próximas", "Hoy", "En curso", "Confirmadas", "Pendientes", "No se presentó", "Canceladas", "Todas")

    private val vigentes = setOf("Pendiente", "Confirmado", "En Proceso")

    fun filterReservas(
        reservas: List<ReservaItemUi>,
        filter: String,
        searchQuery: String,
        hoy: String = java.time.LocalDate.now().toString()
    ): List<ReservaItemUi> {
        val filtradas = reservas.filter { reserva ->
            val matchesFilter = when (filter) {
                "Próximas" -> reserva.estado in vigentes && reserva.fecha >= hoy
                "Hoy" -> reserva.fecha == hoy && reserva.estado != "Cancelado"
                "En curso" -> reserva.estado == "En Proceso"
                "Confirmadas" -> reserva.estado == "Confirmado"
                "Pendientes" -> reserva.estado == "Pendiente"
                "No se presentó" -> reserva.estado == "No se presentó"
                "Canceladas" -> reserva.estado == "Cancelado"
                "Finalizadas" -> reserva.estado == "Finalizado"
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    reserva.titulo.contains(searchQuery, ignoreCase = true) ||
                    reserva.cliente.contains(searchQuery, ignoreCase = true) ||
                    reserva.espacio.contains(searchQuery, ignoreCase = true) ||
                    reserva.id.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
        // Las próximas y las de hoy se leen en orden cronológico; el historial, de la más reciente a la más antigua.
        return if (filter == "Próximas" || filter == "Hoy") {
            filtradas.sortedWith(compareBy({ it.fecha }, { it.hora }))
        } else {
            filtradas.sortedWith(compareByDescending<ReservaItemUi> { it.fecha }.thenByDescending { it.hora })
        }
    }
}
