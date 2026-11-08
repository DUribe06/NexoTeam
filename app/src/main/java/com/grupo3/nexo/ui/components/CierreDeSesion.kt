package com.grupo3.nexo.ui.components

import com.grupo3.nexo.domain.model.CierreSesion
import com.grupo3.nexo.domain.model.EstadoRevision
import com.grupo3.nexo.domain.model.NivelPenalizacion
import com.grupo3.nexo.domain.model.Sesion

/** Por qué se canceló, contado desde quien la mira. Null si la canceló el estudiante antes de la hora. */
fun Sesion.motivoDeCierre(): String? = when (cierre) {
    CierreSesion.TUTOR_NO_LLEGO -> if (comoTutor) "No enviaste el PIN a tiempo." else "Tu tutor no llegó."
    CierreSesion.ESTUDIANTE_NO_LLEGO -> if (comoTutor) "El estudiante no llegó." else "No le diste el PIN a tu tutor a tiempo."
    CierreSesion.ACORDADA -> "Se canceló de común acuerdo."
    null -> null
}

fun Sesion.textoDePenalizacion(): String? = when (penalizacion) {
    NivelPenalizacion.NORMAL -> "Recibiste una penalización."
    NivelPenalizacion.GRAVE -> "Recibiste una penalización grave y tu cuenta queda en pausa."
    null -> null
}

fun Sesion.textoDeRevision(): String? = when (revision) {
    EstadoRevision.ABIERTA -> "Pediste revisión. Mientras tanto, la penalización sigue vigente."
    EstadoRevision.ACEPTADA -> "Revisamos el caso y retiramos la penalización."
    EstadoRevision.RECHAZADA -> "Revisamos el caso y la penalización se mantiene."
    null -> null
}

/** Sobre la solicitud, para el tutor; y en el perfil público, para el estudiante. */
fun textoDePenalizacionesRecientes(cantidad: Int): String =
    "$cantidad ${plural(cantidad, "penalización", "penalizaciones")} en los últimos 90 días"
