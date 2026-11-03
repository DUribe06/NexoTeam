package com.grupo3.nexo.ui.inicio

import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.inicio
import com.grupo3.nexo.ui.components.formatoHora
import com.grupo3.nexo.ui.components.motivoDeCierre
import com.grupo3.nexo.ui.components.textoDePenalizacion
import java.time.Duration
import java.time.LocalDateTime

/** Desde cuántos minutos antes de empezar la sesión pasa a "Ya casi". */
private const val MINUTOS_YA_CASI = 30L

/** Días que "¿Cómo te fue?" sigue a la vista: una sesión vieja sin calificar no debe tapar lo que viene. */
private const val DIAS_PARA_CALIFICAR = 3L

private val Sesion.fin: LocalDateTime get() = inicio.plusMinutes((duracionHoras * 60).toLong())

/** La sesión que aún no termina: la próxima, o la que está en curso. */
internal fun List<Sesion>.vigente(ahora: LocalDateTime): Sesion? = firstOrNull { it.fin > ahora }

/** Minutos enteros que faltan para que empiece la sesión, redondeados hacia arriba como un reloj de cuenta atrás. */
internal fun Sesion.minutosPara(ahora: LocalDateTime): Long =
    (Duration.between(ahora, inicio).seconds + 59) / 60

/**
 * En qué paso va la sesión, solo con datos reales: la hora y los estados que da el servidor.
 *
 * Una presencial no empieza por la hora: empieza cuando el tutor escribe el PIN. Hasta entonces sigue en "Ya casi", y
 * "Llegó" solo aparece cuando el tutor de verdad envió el PIN.
 *
 * @param proximas sesiones programadas, en orden. Una que ya terminó puede seguir aquí hasta la siguiente lectura del
 * servidor, que es quien la marca como completada.
 * @param historial sesiones que ya no están programadas.
 * @param pendientes solicitudes sin responder.
 */
internal fun faseDe(
    proximas: List<Sesion>,
    historial: List<Sesion>,
    pendientes: Int,
    ahora: LocalDateTime
): FaseSesion {
    val sesion = proximas.vigente(ahora)
    if (sesion != null) {
        if (sesion.modalidad == Modalidad.VIRTUAL) {
            if (ahora >= sesion.inicio) return FaseSesion.EN_SESION_VIRTUAL
            if (sesion.minutosPara(ahora) <= MINUTOS_YA_CASI) return FaseSesion.YA_CASI_VIRTUAL
        } else {
            if (sesion.iniciada) return FaseSesion.EN_SESION
            if (sesion.pinEnviado) return FaseSesion.LLEGO
            if (sesion.minutosPara(ahora) <= MINUTOS_YA_CASI) return FaseSesion.YA_CASI
        }
        if (sesion.fecha == ahora.toLocalDate()) return FaseSesion.HOY
    }
    val limite = ahora.minusDays(DIAS_PARA_CALIFICAR)
    val hayPorCalificar = proximas.any { it.fin <= ahora } || historial.any { it.porCalificar && it.fin > limite }
    return when {
        hayPorCalificar -> FaseSesion.CALIFICAR
        sesion != null -> FaseSesion.AGENDADA
        pendientes > 0 -> FaseSesion.ENVIADA
        else -> FaseSesion.SOLA
    }
}

/** Lo que el tutor puede hacer tocando la barra. */
enum class AccionPin(val etiqueta: String) {
    ENVIAR("Enviar PIN"),
    ESCRIBIR("Escribir PIN")
}

/** Lo que dice la barra mientras una presencial espera el PIN: reemplaza el texto general de la fase. */
data class AvisoPin(val titulo: String, val detalle: String, val pin: String? = null, val accion: AccionPin? = null)

/**
 * Null si la barra no tiene nada especial que decir: no es presencial, ya empezó o aún es temprano.
 * El tutor toca la barra para enviar el PIN y después para escribirlo; el estudiante solo lo lee.
 */
internal fun avisoDePin(s: Sesion, ahora: LocalDateTime): AvisoPin? {
    val limite = s.limite?.toLocalTime()?.let(::formatoHora)
    if (s.modalidad != Modalidad.PRESENCIAL || s.iniciada || limite == null || s.minutosPara(ahora) > MINUTOS_YA_CASI) return null
    val nombre = s.conQuien.substringBefore(' ')
    return when {
        s.comoTutor && !s.pinEnviado -> AvisoPin(
            "¿Ya llegaste?",
            "Envíale el PIN a $nombre antes de las $limite o la sesión se cancela.",
            accion = AccionPin.ENVIAR
        )
        s.comoTutor -> AvisoPin("PIN enviado", "Escribe el que te diga $nombre antes de las $limite.", accion = AccionPin.ESCRIBIR)
        s.pinEnviado -> AvisoPin(
            s.pin?.let { "Tu PIN es $it" } ?: "Tu tutor llegó",
            "Díselo a tu tutor antes de las $limite para empezar.",
            pin = s.pin
        )
        s.horaPactada != null -> AvisoPin(
            "Nueva hora: ${formatoHora(s.horaPactada)}",
            "Si tu tutor no envía el PIN antes de las $limite, la sesión se cancela."
        )
        ahora >= s.inicio -> AvisoPin("Tu tutor aún no llega", "Si no envía el PIN antes de las $limite, la sesión se cancela.")
        else -> null
    }
}

/** Horas que la barra sigue contando por qué se canceló una sesión: quien iba a ella debe enterarse aunque no abra el chat. */
private const val HORAS_AVISO_CIERRE = 3L

/** Una sesión que se canceló hace poco por no presentarse o de común acuerdo. */
internal fun avisoDeCierre(historial: List<Sesion>, ahora: LocalDateTime): AvisoPin? =
    historial.filter { it.cierre != null && it.inicio.plusHours(HORAS_AVISO_CIERRE) > ahora }
        // Si hubo varias, primero la que dejó una penalización: es lo que más debe saber
        .let { recientes -> recientes.firstOrNull { it.penalizacion != null } ?: recientes.firstOrNull() }
        ?.let { s -> AvisoPin("Sesión cancelada", listOfNotNull(s.motivoDeCierre(), s.textoDePenalizacion()).joinToString(" ")) }
