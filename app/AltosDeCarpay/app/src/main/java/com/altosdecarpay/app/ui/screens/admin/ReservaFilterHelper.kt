package com.altosdecarpay.app.ui.screens.admin

object ReservaFilterHelper {
    fun filterReservas(
        reservas: List<ReservaItemUi>,
        filter: String,
        searchQuery: String
    ): List<ReservaItemUi> {
        return reservas.filter { reserva ->
            val matchesFilter = when (filter) {
                "En curso" -> reserva.estado == "En curso"
                "Confirmadas" -> reserva.estado == "Confirmada"
                "Finalizadas" -> reserva.estado == "Finalizada"
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    reserva.titulo.contains(searchQuery, ignoreCase = true) ||
                    reserva.cliente.contains(searchQuery, ignoreCase = true) ||
                    reserva.id.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }
}
