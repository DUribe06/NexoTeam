package com.grupo3.nexo.ui.components

import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.PLAZO_RESPUESTA_HORAS
import com.grupo3.nexo.ui.auth.mensajeDeError
import com.grupo3.nexo.domain.model.siglasUniversidad
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Decimales con coma, como se escriben en Colombia: 4,9 y 1,5 h. */
private val localeColombia = Locale("es", "CO")

fun formatoValoracion(valor: Double): String = String.format(localeColombia, "%.1f", valor)
/** Nombre corto de la universidad (sigla si se conoce), para filas densas. */
fun universidadCorta(nombre: String): String = siglasUniversidad[nombre] ?: nombre

/** Pesos colombianos con punto de miles: 25000.0 → "$25.000". */
fun formatoPrecio(valor: Double): String =
    "$" + valor.toLong().toString().reversed().chunked(3).joinToString(".").reversed()
/** Tarifa por hora para mostrar; un tutor que aún no la fija aparece "a convenir" en lugar de $0. */
fun tarifaTexto(precioHora: Double): String =
    if (precioHora > 0) "${formatoPrecio(precioHora)} la hora" else "precio a convenir"

/** Lo escrito en un campo de precio: solo cifras, sin ceros a la izquierda y con el tope del servidor (8 cifras). */
fun soloCifras(texto: String): String = texto.filter(Char::isDigit).trimStart('0').take(8)

fun formatoHoras(horas: Double): String =
    if (horas % 1.0 == 0.0) "${horas.toInt()} h" else String.format(localeColombia, "%.1f h", horas)

private val mesesCortos = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

private val formatoReloj = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

/** Fecha corta en español: 2026-10-24 → "24 oct". */
fun formatoFecha(fecha: LocalDate): String = "${fecha.dayOfMonth} ${mesesCortos[fecha.monthValue - 1]}"

/** Hora de reloj de 24 h con dos dígitos: 10:00 → "10:00", 08:30 → "08:30". */
fun formatoHora(hora: LocalTime): String = hora.format(formatoReloj)

private fun String.mayusculaInicial() = replaceFirstChar { it.titlecase(localeColombia) }

private fun LocalDate.nombreDia(estilo: TextStyle): String =
    dayOfWeek.getDisplayName(estilo, localeColombia).trimEnd('.').mayusculaInicial()

/** Fecha larga con el día de la semana: 2026-10-24 → "Sábado 24 de octubre". */
fun fechaLarga(fecha: LocalDate): String =
    "${fecha.nombreDia(TextStyle.FULL)} ${fecha.dayOfMonth} de ${fecha.month.getDisplayName(TextStyle.FULL, localeColombia)}"

/** Día y mes completo: 2026-11-09 → "9 de noviembre". */
fun diaYMes(fecha: LocalDate): String =
    "${fecha.dayOfMonth} de ${fecha.month.getDisplayName(TextStyle.FULL, localeColombia)}"

/** Día de la semana y número: 2026-10-26 → "Lun 26". */
fun fechaCorta(fecha: LocalDate): String = "${fecha.nombreDia(TextStyle.SHORT)} ${fecha.dayOfMonth}"

/**
 * Momento de un mensaje en una lista: la hora si es de hoy ("10:24"), "Ahora" si es de hace menos de un minuto,
 * "Ayer", el día de la semana dentro de la última semana ("Lun") y, más atrás, la fecha ("7 oct").
 */
fun formatoMomento(momento: LocalDateTime, ahora: LocalDateTime = LocalDateTime.now()): String {
    val dias = ChronoUnit.DAYS.between(momento.toLocalDate(), ahora.toLocalDate())
    return when {
        dias == 0L -> {
            val desde = Duration.between(momento, ahora)
            if (!desde.isNegative && desde < Duration.ofMinutes(1)) "Ahora" else momento.format(formatoReloj)
        }
        dias == 1L -> "Ayer"
        dias in 2L..6L -> momento.toLocalDate().nombreDia(TextStyle.SHORT)
        else -> formatoFecha(momento.toLocalDate())
    }
}

/** Separador de día en una conversación: "Hoy", "Ayer" o la fecha larga ("Lunes 26 de octubre"). */
fun formatoDia(fecha: LocalDate, hoy: LocalDate = LocalDate.now()): String =
    when (ChronoUnit.DAYS.between(fecha, hoy)) {
        0L -> "Hoy"
        1L -> "Ayer"
        else -> fechaLarga(fecha)
    }

/** Antigüedad de una publicación: "Hoy", "hace 3 días", "hace 2 sem", "hace 1 mes", "hace 2 meses". */
fun formatoHace(fecha: LocalDate, hoy: LocalDate = LocalDate.now()): String {
    val dias = ChronoUnit.DAYS.between(fecha, hoy)
    return when {
        dias <= 0L -> "Hoy"
        dias == 1L -> "hace 1 día"
        dias < 7L -> "hace $dias días"
        dias < 30L -> "hace ${dias / 7} sem"
        dias < 365L -> (dias / 30).toInt().let { "hace $it ${plural(it, "mes", "meses")}" }
        else -> (dias / 365).toInt().let { "hace $it ${plural(it, "año", "años")}" }
    }
}

/** Plazo de respuesta del dominio, ya escrito para los textos que lo mencionan. */
val PLAZO_RESPUESTA = formatoHoras(PLAZO_RESPUESTA_HORAS.toDouble())

/** Sugerencia que cierra los avisos de fallo de red. */
const val SUGERENCIA_CONEXION = "Revisa tu conexión e inténtalo de nuevo."

/**
 * Aviso de fallo de una acción. Si el servidor la rechazó ([fallo] es `ErrorApi`) dice por qué, sin culpar a la conexión;
 * si no: avisoFallo("enviar el reporte") → "No se pudo enviar el reporte. Revisa tu conexión…".
 */
fun avisoFallo(accion: String, fallo: Throwable? = null): String =
    if (fallo is ErrorApi) mensajeDeError(fallo) else "No se pudo $accion. $SUGERENCIA_CONEXION"

/** Singular si [n] es 1 y plural en cualquier otro caso: plural(2, "estrella", "estrellas"). */
fun plural(n: Int, uno: String, varios: String): String = if (n == 1) uno else varios
