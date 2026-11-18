package com.altosdecarpay.app.ui.screens

object LoginHelper {
    fun validateInputs(identificador: String, password: String): String? {
        if (identificador.isBlank() || password.isBlank()) {
            return "Por favor, completa ambos campos."
        }
        return null
    }

    fun determineRole(identificador: String): String {
        return if (identificador.contains("admin", ignoreCase = true)) {
            "Admin"
        } else {
            "Operador"
        }
    }
}
