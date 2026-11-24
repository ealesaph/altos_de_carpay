package com.altosdecarpay.app.ui.utils

import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatos de presentación comunes de la app (mismos criterios que la web):
 * montos en pesos chilenos, fechas legibles, RUT y teléfono, y nombres de estado.
 */
object Formato {

    private val localeCl = Locale.forLanguageTag("es-CL")
    private val zonaChile: ZoneId = ZoneId.of("America/Santiago")
    private val fechaCorta = DateTimeFormatter.ofPattern("d MMM yyyy", localeCl)
    private val fechaHoraCorta = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", localeCl)

    /** "25000.00" → "$25.000" */
    fun clp(valor: String?): String {
        val numero = valor?.toBigDecimalOrNull() ?: return "$0"
        val formato = NumberFormat.getIntegerInstance(localeCl)
        return "$" + formato.format(numero.setScale(0, java.math.RoundingMode.HALF_UP))
    }

    /** "2026-12-07" → "7 dic 2026" */
    fun fecha(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            LocalDate.parse(iso.take(10)).format(fechaCorta).replace(".", "")
        } catch (_: Exception) {
            iso
        }
    }

    /** "2026-11-23T19:45:00-03:00" → "23 nov 2026 · 19:45" (hora de Chile) */
    fun fechaHora(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            OffsetDateTime.parse(iso).atZoneSameInstant(zonaChile).format(fechaHoraCorta).replace(".", "")
        } catch (_: Exception) {
            iso.take(16).replace("T", " ")
        }
    }

    /** "13:00:00" → "13:00" */
    fun hora(texto: String?): String = texto?.take(5) ?: ""

    /** "198765430" → "19.876.543-0" */
    fun rut(valor: String?): String {
        val limpio = valor?.replace(".", "")?.replace("-", "")?.uppercase()?.trim() ?: return ""
        if (limpio.length < 2) return limpio
        val cuerpo = limpio.dropLast(1)
        val dv = limpio.last()
        val conPuntos = cuerpo.reversed().chunked(3).joinToString(".").reversed()
        return "$conPuntos-$dv"
    }

    /** "+56922223333" → "+56 9 2222 3333" */
    fun telefono(valor: String?): String {
        val digitos = valor?.filter { it.isDigit() } ?: return ""
        return when {
            digitos.length == 11 && digitos.startsWith("569") ->
                "+56 9 ${digitos.substring(3, 7)} ${digitos.substring(7)}"
            digitos.length == 9 && digitos.startsWith("9") ->
                "+56 9 ${digitos.substring(1, 5)} ${digitos.substring(5)}"
            else -> valor
        }
    }

    /** Nombre del estado para mostrar (el filtro siempre usa el valor del servidor). */
    fun estado(estado: String): String = when (estado) {
        "Pendiente" -> "Pendiente de pago"
        "Confirmado" -> "Confirmada"
        "En Proceso" -> "En curso"
        "Finalizado" -> "Finalizada"
        "Cancelado" -> "Cancelada"
        else -> estado // "No se presentó" y cualquier estado nuevo se muestran tal cual
    }

    /** Estado de una consulta (no confundir con el «Pendiente» de pago de una reserva). */
    fun estadoConsulta(estado: String?): String = if (estado == "Pendiente") "Sin responder" else (estado ?: "")

    /** Estados en los que la reserva sigue vigente (próximas). */
    val estadosVigentes = setOf("Pendiente", "Confirmado", "En Proceso")

    /** Estados en los que se muestra el QR de acceso. */
    val estadosConAcceso = setOf("Confirmado", "En Proceso")

    fun numeroConsulta(id: Int) = "Consulta N° $id"

    fun numeroIncidencia(id: Int) = "Incidencia N° $id"

    /**
     * Mensaje legible a partir del cuerpo de error de la API: {"detail": "..."} o {"campo": ["..."]}.
     */
    fun mensajeError(cuerpo: String?, porDefecto: String = "No se pudo completar la operación."): String {
        if (cuerpo.isNullOrBlank()) return porDefecto
        return try {
            val json = com.google.gson.JsonParser.parseString(cuerpo)
            if (!json.isJsonObject) return porDefecto
            val obj = json.asJsonObject
            val elemento = obj.get("detail") ?: obj.entrySet().firstOrNull()?.value ?: return porDefecto
            when {
                elemento.isJsonPrimitive -> elemento.asString
                elemento.isJsonArray && elemento.asJsonArray.size() > 0 -> elemento.asJsonArray[0].asString
                else -> porDefecto
            }
        } catch (_: Exception) {
            if (cuerpo.trimStart().startsWith("{") || cuerpo.trimStart().startsWith("<")) porDefecto else cuerpo
        }
    }

    /** Mensaje para errores de red (sin texto técnico de Java/OkHttp). */
    fun mensajeConexion(e: Throwable?): String =
        "No pudimos conectarnos con el servidor. Revisa tu conexión e inténtalo nuevamente."
}
