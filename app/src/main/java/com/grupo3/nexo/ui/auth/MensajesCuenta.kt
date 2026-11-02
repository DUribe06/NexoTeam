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
    else -> "Algo falló en el servidor. Inténtalo de nuevo en un momento."
}

private fun mensajeDeDatosInvalidos(error: ErrorApi): String =
    when (error.detalles.firstOrNull()?.campo) {
        "nombre" -> "El nombre debe tener entre 2 y 100 caracteres."
        "correo" -> "Escribe un correo válido, por ejemplo correo@ejemplo.com."
        "contrasena", "nuevaContrasena" -> "La contraseña debe tener entre 8 y 72 caracteres."
        "codigo" -> "El código debe tener 6 dígitos."
        else -> "Revisa los datos e inténtalo de nuevo."
    }

private fun mensajeDeEspera(segundos: Int?): String = when {
    segundos == null -> "Hiciste muchos intentos. Espera un momento y vuelve a intentarlo."
    segundos < 2 * SEGUNDOS_POR_MINUTO ->
        "Hiciste muchos intentos. Espera $segundos ${plural(segundos, "segundo", "segundos")} y vuelve a intentarlo."
    else -> "Hiciste muchos intentos. Espera ${(segundos + SEGUNDOS_POR_MINUTO - 1) / SEGUNDOS_POR_MINUTO} minutos y vuelve a intentarlo."
}
