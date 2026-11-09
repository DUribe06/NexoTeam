package com.grupo3.nexo.ui.solicitudes

import com.grupo3.nexo.ui.components.formatoHoras

/**
 * Plazo de respuesta (RF6). El tutor ve cuánto le queda para responder;
 * el estudiante, cuándo expira su solicitud si no recibe respuesta.
 */
fun textoPlazo(horas: Double, horasEnteras: Double, esTutor: Boolean): String = when {
    esTutor && horas < 1 -> "Queda menos de ${formatoHoras(1.0)} para responder"
    esTutor && horasEnteras < 2 -> "Queda ${formatoHoras(horasEnteras)} para responder"
    esTutor -> "Quedan ${formatoHoras(horasEnteras)} para responder"
    horas < 1 -> "Expira en menos de ${formatoHoras(1.0)} si no responde"
    else -> "Expira en ${formatoHoras(horasEnteras)} si no responde"
}
