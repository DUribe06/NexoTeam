package com.grupo3.nexo.ui.auth

import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.ResultadoAcceso
import com.grupo3.nexo.ui.components.SUGERENCIA_CONEXION
import com.grupo3.nexo.ui.components.plural

private const val MENSAJE_SIN_CONEXION = "No pudimos conectarnos. $SUGERENCIA_CONEXION"
private const val SEGUNDOS_POR_MINUTO = 60

/** Qué decirle a la persona según cómo terminó el intento de entrar; null si no hay nada que avisar. */
fun mensajeDeAcceso(resultado: ResultadoAcceso): String? = when (resultado) {
    ResultadoAcceso.Entro, is ResultadoAcceso.FaltaVerificarCorreo -> null
    ResultadoAcceso.CredencialesIncorrectas -> "No pudimos validar tus datos. Revisa el correo y la contraseña."
    is ResultadoAcceso.CuentaBloqueada -> {
        val cuando = resultado.minutos?.let { "en $it ${plural(it, "minuto", "minutos")}" } ?: "en unos minutos"
        "Bloqueamos tu cuenta por varios intentos fallidos. Inténtalo de nuevo $cuando."
    }
    ResultadoAcceso.CuentaSuspendida -> "Tu cuenta está suspendida."
}

/** Una excepción que no es del servidor se trata como fallo de conexión. */
fun mensajeDeError(fallo: Throwable): String =
    if (fallo is ErrorApi) mensajeDeErrorApi(fallo) else MENSAJE_SIN_CONEXION

private fun mensajeDeErrorApi(error: ErrorApi): String = when (error.codigo) {
    CodigoError.DATOS_INVALIDOS -> mensajeDeDatosInvalidos(error)
    CodigoError.CORREO_DUPLICADO -> "Ya hay una cuenta con ese correo. Inicia sesión o usa otro."
    CodigoError.CORREO_SIN_MX -> "Ese correo no puede recibir mensajes. Revisa que esté bien escrito."
    CodigoError.CORREO_DESECHABLE -> "No aceptamos correos temporales. Usa tu correo personal o institucional."
    CodigoError.CONTRASENA_DEBIL -> "Esa contraseña es débil. Usa al menos 8 caracteres con letras y números, y evita las comunes."
    CodigoError.DEMASIADAS_SOLICITUDES -> mensajeDeEspera(error.reintentarEnSegundos)
    CodigoError.CODIGO_INVALIDO -> "El código no es válido. Revísalo o pide uno nuevo."
    CodigoError.CODIGO_VENCIDO -> "El código venció. Pide uno nuevo."
    CodigoError.CODIGO_INTENTOS_AGOTADOS -> "Superaste los intentos. Pide un código nuevo."
    CodigoError.CUENTA_NO_ENCONTRADA -> "No hay una cuenta por confirmar con ese correo."
    CodigoError.TOKEN_INVALIDO, CodigoError.NO_AUTENTICADO -> "Tu sesión venció. Inicia sesión de nuevo."
    CodigoError.GOOGLE_NO_DISPONIBLE -> "Inicio con Google disponible pronto."
    CodigoError.CUENTA_NO_USA_APP, CodigoError.ADMIN_SIN_MODOS ->
        "Esta cuenta es de administración y no puede usar la app. Entra con una cuenta de estudiante o tutor."
    CodigoError.NO_ES_TUTOR -> "Esto solo lo puede hacer el tutor. Revisa que estés en modo Tutor."
    CodigoError.NO_ES_ESTUDIANTE -> "Esto solo lo puede hacer el estudiante que envió la solicitud."
    CodigoError.CONTRASENA_INCORRECTA -> "La contraseña no es correcta."
    CodigoError.CUENTA_CON_SESIONES -> "Tienes tutorías agendadas. Termínalas o cancélalas antes de eliminar tu cuenta."
    CodigoError.CORREO_NO_DISPONIBLE -> "No pudimos enviar el correo. Inténtalo de nuevo en un momento."
    CodigoError.CUENTA_EN_PAUSA -> error.message ?: "Por tus penalizaciones, tu cuenta está en pausa. Mira hasta cuándo en tu Perfil."
    CodigoError.TUTOR_EN_PAUSA -> "Este tutor no está recibiendo solicitudes por ahora."
    CodigoError.SIN_PENALIZACION -> "Esta sesión no te dejó ninguna penalización."
    CodigoError.REVISION_DUPLICADA -> "Ya pediste la revisión de esta penalización."
    CodigoError.REVISION_FUERA_DE_PLAZO -> "Pasaron los 7 días para pedir la revisión."
    CodigoError.TUTOR_NO_ENCONTRADO -> "Este tutor ya no está disponible."
    CodigoError.REPORTE_DUPLICADO -> "Ya reportaste a este tutor. Estamos revisando tu reporte."
    CodigoError.REPORTE_A_SI_MISMO -> "No puedes reportarte a ti mismo."
    CodigoError.CALENDARIO_NO_CREADO -> "Primero crea tu semana con \"Agregar horario\"."
    CodigoError.HORA_NO_VALIDA -> "Esa hora no está en el calendario. Elige otra."
    CodigoError.SOLICITUD_DUPLICADA -> "Ya tienes una solicitud con este tutor para ese día y hora."
    CodigoError.FECHA_NO_VALIDA -> "Esa fecha ya no se puede pedir. Elige un día de los próximos 14."
    CodigoError.HORA_NO_DISPONIBLE -> "Esa hora ya no está libre. Elige otra."
    CodigoError.MODALIDAD_NO_DISPONIBLE -> "El tutor no ofrece esa modalidad a esa hora. Elige otra."
    CodigoError.MATERIA_NO_OFRECIDA -> "El tutor ya no enseña esa materia. Elige otra."
    CodigoError.SOLICITUD_A_SI_MISMO -> "No puedes enviarte una solicitud a ti mismo."
    CodigoError.SOLICITUD_NO_ENCONTRADA -> "Esta solicitud ya no existe."
    CodigoError.SOLICITUD_NO_PENDIENTE -> "Esta solicitud ya fue respondida."
    CodigoError.SOLICITUD_EXPIRADA -> "Se venció el plazo para responder esta solicitud."
    CodigoError.SOLICITUD_NO_CANCELABLE -> "Ya no se puede cancelar: la solicitud se cerró o la sesión ya empezó."
    CodigoError.SESION_NO_ENCONTRADA -> "Esta sesión ya no existe."
    CodigoError.SESION_NO_COMPLETADA -> "Podrás calificar cuando termine la sesión."
    CodigoError.SESION_YA_CALIFICADA -> "Ya calificaste esta sesión."
    CodigoError.SESION_NO_PROGRAMADA -> "Esta sesión ya no está programada."
    CodigoError.SESION_SIN_PIN -> "Las sesiones virtuales no usan PIN."
    CodigoError.SESION_AUN_NO_EMPIEZA -> "Aún es muy temprano: podrás enviarlo desde 30 minutos antes de la hora."
    CodigoError.SESION_YA_INICIADA -> "La sesión ya empezó."
    CodigoError.PIN_SIN_ENVIAR -> "Primero envía el PIN al estudiante."
    CodigoError.PIN_INCORRECTO -> error.message ?: "El PIN no coincide."
    CodigoError.PIN_BLOQUEADO -> mensajeDeEspera(error.reintentarEnSegundos)
    CodigoError.ACUERDO_PENDIENTE -> "Ya hay una propuesta sin responder."
    CodigoError.ACUERDO_NO_PENDIENTE -> "Esa propuesta ya se respondió."
    CodigoError.ACUERDO_PROPIO -> "Debe aceptarlo la otra persona."
    CodigoError.RETARDO_YA_ACORDADO -> "Esta sesión ya tuvo un retardo pactado."
    CodigoError.RETARDO_NO_VALIDO -> "Esa hora ya pasó o no alcanza para la sesión. Elige otra."
    CodigoError.CHAT_NO_HABILITADO -> "El chat se habilita cuando el tutor acepta una solicitud."
    CodigoError.CHAT_NO_ENCONTRADO -> "Esta conversación ya no existe."
    CodigoError.CHAT_SOLO_LECTURA -> "Esta conversación ya no admite mensajes."
    else -> "Algo falló en el servidor. Inténtalo de nuevo en un momento."
}

private fun mensajeDeDatosInvalidos(error: ErrorApi): String =
    when (error.detalles.firstOrNull()?.campo) {
        "nombre" -> "El nombre debe tener entre 2 y 100 caracteres."
        "correo" -> "Escribe un correo válido, por ejemplo correo@ejemplo.com."
        "contrasena", "nuevaContrasena" -> "La contraseña debe tener entre 8 y 72 caracteres."
        "codigo" -> "El código debe tener 6 dígitos."
        "tema" -> "Escribe el tema a reforzar, en máximo 200 caracteres."
        "motivo" -> "Revisa el motivo: es muy corto o muy largo."
        "texto" -> "El mensaje es muy largo. Acórtalo e inténtalo de nuevo."
        "mensaje" -> "El mensaje para el tutor es muy largo: máximo 500 caracteres."
        "comentario" -> "El comentario es muy largo: máximo 500 caracteres."
        else -> "Revisa los datos e inténtalo de nuevo."
    }

private fun mensajeDeEspera(segundos: Int?): String = when {
    segundos == null -> "Hiciste muchos intentos. Espera un momento y vuelve a intentarlo."
    segundos < 2 * SEGUNDOS_POR_MINUTO ->
        "Hiciste muchos intentos. Espera $segundos ${plural(segundos, "segundo", "segundos")} y vuelve a intentarlo."
    else -> "Hiciste muchos intentos. Espera ${(segundos + SEGUNDOS_POR_MINUTO - 1) / SEGUNDOS_POR_MINUTO} minutos y vuelve a intentarlo."
}
