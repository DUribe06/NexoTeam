package com.grupo3.nexo.domain.model

/** Cómo terminó un intento de entrar o de crear una cuenta. */
sealed interface ResultadoAcceso {
    /** Hay sesión abierta: la app sigue al Inicio. */
    data object Entro : ResultadoAcceso

    /** La cuenta existe pero falta confirmar el correo con el código de 6 dígitos. */
    data class FaltaVerificarCorreo(val correo: String) : ResultadoAcceso

    data object CredencialesIncorrectas : ResultadoAcceso

    /** Bloqueo temporal por intentos fallidos; [minutos] es null si el servidor no dijo cuánto falta. */
    data class CuentaBloqueada(val minutos: Int?) : ResultadoAcceso

    /** Suspendida por el administrador (RF12). */
    data object CuentaSuspendida : ResultadoAcceso
}

/** Un campo rechazado por el servidor, o un dato adicional del error. */
data class DetalleError(val campo: String, val problema: String)

/**
 * Error que respondió el servidor. [codigo] es estable y decide qué se muestra; `message` es el texto del
 * servidor. Un fallo de red no es este error sino una `IOException`.
 */
class ErrorApi(
    val codigo: String,
    mensaje: String,
    val detalles: List<DetalleError> = emptyList(),
    /** Segundos de la cabecera `Retry-After`, si vino. */
    val reintentarEnSegundos: Int? = null
) : Exception(mensaje)

/** Códigos de error del contrato (`openapi/nexo-api.yaml`) y los dos propios de la app, marcados abajo. */
object CodigoError {
    const val DATOS_INVALIDOS = "DATOS_INVALIDOS"
    const val CORREO_DUPLICADO = "CORREO_DUPLICADO"
    const val CORREO_SIN_MX = "CORREO_SIN_MX"
    const val CORREO_DESECHABLE = "CORREO_DESECHABLE"
    const val CONTRASENA_DEBIL = "CONTRASENA_DEBIL"
    const val DEMASIADAS_SOLICITUDES = "DEMASIADAS_SOLICITUDES"
    const val CODIGO_INVALIDO = "CODIGO_INVALIDO"
    const val CODIGO_VENCIDO = "CODIGO_VENCIDO"
    const val CODIGO_INTENTOS_AGOTADOS = "CODIGO_INTENTOS_AGOTADOS"
    const val CUENTA_NO_ENCONTRADA = "CUENTA_NO_ENCONTRADA"
    const val CREDENCIALES_INVALIDAS = "CREDENCIALES_INVALIDAS"
    const val CORREO_NO_VERIFICADO = "CORREO_NO_VERIFICADO"
    const val CUENTA_SUSPENDIDA = "CUENTA_SUSPENDIDA"
    const val CUENTA_BLOQUEADA = "CUENTA_BLOQUEADA"
    const val TOKEN_INVALIDO = "TOKEN_INVALIDO"
    const val NO_AUTENTICADO = "NO_AUTENTICADO"

    /** Propio de la app: el servidor respondió un error sin el cuerpo del contrato. */
    const val ERROR_SERVIDOR = "ERROR_SERVIDOR"

    /** Propio de la app: el backend todavía no tiene inicio con Google. */
    const val GOOGLE_NO_DISPONIBLE = "GOOGLE_NO_DISPONIBLE"
}
