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

/** Códigos de error del contrato (`openapi/nexo-api.yaml`) y los propios de la app, marcados abajo. */
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
    const val ADMIN_SIN_MODOS = "ADMIN_SIN_MODOS"
    const val NO_ES_TUTOR = "NO_ES_TUTOR"
    const val NO_ES_ESTUDIANTE = "NO_ES_ESTUDIANTE"
    const val CONTRASENA_INCORRECTA = "CONTRASENA_INCORRECTA"
    const val CUENTA_CON_SESIONES = "CUENTA_CON_SESIONES"
    const val CORREO_NO_DISPONIBLE = "CORREO_NO_DISPONIBLE"
    const val CUENTA_EN_PAUSA = "CUENTA_EN_PAUSA"
    const val TUTOR_EN_PAUSA = "TUTOR_EN_PAUSA"
    const val SIN_PENALIZACION = "SIN_PENALIZACION"
    const val REVISION_DUPLICADA = "REVISION_DUPLICADA"
    const val REVISION_FUERA_DE_PLAZO = "REVISION_FUERA_DE_PLAZO"
    const val TUTOR_NO_ENCONTRADO = "TUTOR_NO_ENCONTRADO"
    const val REPORTE_DUPLICADO = "REPORTE_DUPLICADO"
    const val REPORTE_A_SI_MISMO = "REPORTE_A_SI_MISMO"
    const val CALENDARIO_NO_CREADO = "CALENDARIO_NO_CREADO"
    const val HORA_NO_VALIDA = "HORA_NO_VALIDA"
    const val SOLICITUD_DUPLICADA = "SOLICITUD_DUPLICADA"
    const val FECHA_NO_VALIDA = "FECHA_NO_VALIDA"
    const val HORA_NO_DISPONIBLE = "HORA_NO_DISPONIBLE"
    const val MODALIDAD_NO_DISPONIBLE = "MODALIDAD_NO_DISPONIBLE"
    const val MATERIA_NO_OFRECIDA = "MATERIA_NO_OFRECIDA"
    const val SOLICITUD_A_SI_MISMO = "SOLICITUD_A_SI_MISMO"
    const val SOLICITUD_NO_ENCONTRADA = "SOLICITUD_NO_ENCONTRADA"
    const val SOLICITUD_NO_PENDIENTE = "SOLICITUD_NO_PENDIENTE"
    const val SOLICITUD_EXPIRADA = "SOLICITUD_EXPIRADA"
    const val SOLICITUD_NO_CANCELABLE = "SOLICITUD_NO_CANCELABLE"
    const val SESION_NO_ENCONTRADA = "SESION_NO_ENCONTRADA"
    const val SESION_NO_COMPLETADA = "SESION_NO_COMPLETADA"
    const val SESION_YA_CALIFICADA = "SESION_YA_CALIFICADA"
    const val SESION_NO_PROGRAMADA = "SESION_NO_PROGRAMADA"
    const val SESION_SIN_PIN = "SESION_SIN_PIN"
    const val SESION_AUN_NO_EMPIEZA = "SESION_AUN_NO_EMPIEZA"
    const val SESION_YA_INICIADA = "SESION_YA_INICIADA"
    const val PIN_SIN_ENVIAR = "PIN_SIN_ENVIAR"
    const val PIN_INCORRECTO = "PIN_INCORRECTO"
    const val PIN_BLOQUEADO = "PIN_BLOQUEADO"
    const val ACUERDO_PENDIENTE = "ACUERDO_PENDIENTE"
    const val ACUERDO_NO_PENDIENTE = "ACUERDO_NO_PENDIENTE"
    const val ACUERDO_PROPIO = "ACUERDO_PROPIO"
    const val RETARDO_YA_ACORDADO = "RETARDO_YA_ACORDADO"
    const val RETARDO_NO_VALIDO = "RETARDO_NO_VALIDO"
    const val CHAT_NO_HABILITADO = "CHAT_NO_HABILITADO"
    const val CHAT_NO_ENCONTRADO = "CHAT_NO_ENCONTRADO"
    const val CHAT_SOLO_LECTURA = "CHAT_SOLO_LECTURA"

    /** Propio de la app: el servidor respondió un error sin el cuerpo del contrato. */
    const val ERROR_SERVIDOR = "ERROR_SERVIDOR"

    /** Propio de la app: la cuenta es de administrador (o de un rol que no conoce), que no usa la app (RF12). */
    const val CUENTA_NO_USA_APP = "CUENTA_NO_USA_APP"

    /** Propio de la app: el backend todavía no tiene inicio con Google. */
    const val GOOGLE_NO_DISPONIBLE = "GOOGLE_NO_DISPONIBLE"
}
