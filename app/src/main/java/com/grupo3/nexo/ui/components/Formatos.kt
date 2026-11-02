package com.grupo3.nexo.ui.components

import com.grupo3.nexo.domain.model.PLAZO_RESPUESTA_HORAS
import com.grupo3.nexo.domain.model.diaYMes
import com.grupo3.nexo.domain.model.fechaCalendario
import com.grupo3.nexo.domain.model.mesesCortos
import com.grupo3.nexo.domain.model.siglasUniversidad
import java.time.LocalDate
import java.util.Locale

/** Decimales con coma, como se escriben en Colombia: 4,9 y 1,5 h. */
private val localeColombia = Locale("es", "CO")

fun formatoValoracion(valor: Double): String = String.format(localeColombia, "%.1f", valor)
/** Nombre corto de la universidad (sigla si se conoce), para filas densas. */
fun universidadCorta(nombre: String): String = siglasUniversidad[nombre] ?: nombre

/** Pesos colombianos con punto de miles: 25000.0 → "$25.000". */
fun formatoPrecio(valor: Double): String =
    "$" + valor.toLong().toString().reversed().chunked(3).joinToString(".").reversed()
fun formatoHoras(horas: Double): String =
    if (horas % 1.0 == 0.0) "${horas.toInt()} h" else String.format(localeColombia, "%.1f h", horas)

/** Fecha corta en español: "24 oct", "30 sept" → "30 sep". Si el texto no es una fecha, se devuelve tal cual. */
fun formatoFecha(texto: String): String =
    diaYMes(texto)?.let { (dia, mes) -> "$dia ${mesesCortos[mes - 1]}" } ?: texto

/** Fecha de un día del calendario con el mismo mes corto: LocalDate 30-sep → "30 sep". */
fun formatoFecha(fecha: LocalDate): String = "${fecha.dayOfMonth} ${mesesCortos[fecha.monthValue - 1]}"

private fun String.mayusculaInicial() = replaceFirstChar { it.titlecase(localeColombia) }

/** Fecha larga con el día de la semana: "24 oct" → "Sábado 24 de octubre". Si el texto no es una fecha, se devuelve tal cual. */
fun fechaLarga(texto: String): String = fechaCalendario(texto)?.let {
    "${it.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, localeColombia).mayusculaInicial()} ${it.dayOfMonth} de " +
        it.month.getDisplayName(java.time.format.TextStyle.FULL, localeColombia)
} ?: texto

/** Día de la semana y número: "26 oct" → "Lun 26". Si el texto no es una fecha, se devuelve tal cual. */
fun fechaCorta(texto: String): String = fechaCalendario(texto)?.let {
    "${it.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, localeColombia).trimEnd('.').mayusculaInicial()} ${it.dayOfMonth}"
} ?: texto

/** Plazo de respuesta del dominio, ya escrito para los textos que lo mencionan. */
val PLAZO_RESPUESTA = formatoHoras(PLAZO_RESPUESTA_HORAS.toDouble())

/** Sugerencia que cierra los avisos de fallo de red. */
const val SUGERENCIA_CONEXION = "Revisa tu conexión e inténtalo de nuevo."

/** Aviso de fallo con su sugerencia: avisoFallo("enviar el reporte") → "No se pudo enviar el reporte. Revisa tu conexión…". */
fun avisoFallo(accion: String): String = "No se pudo $accion. $SUGERENCIA_CONEXION"

/** Singular si [n] es 1 y plural en cualquier otro caso: plural(2, "estrella", "estrellas"). */
fun plural(n: Int, uno: String, varios: String): String = if (n == 1) uno else varios
