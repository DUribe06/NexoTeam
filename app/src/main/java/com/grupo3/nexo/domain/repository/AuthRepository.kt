package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.ResultadoAcceso
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Usuario
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de datos de la sesión y el usuario.
 *
 * Las lecturas (`Flow`) y las escrituras (`suspend`) de una implementación remota deben lanzar una excepción
 * ante fallos de red o del servidor: la UI la captura, muestra el error con "Reintentar" o un aviso breve.
 * `IOException` significa que no hubo conexión; `ErrorApi`, que el servidor rechazó la acción y por qué.
 * Los desenlaces normales de entrar (clave errada, correo sin confirmar, bloqueo) son un [ResultadoAcceso].
 */
interface AuthRepository {
    /** Si pasa a false con la app abierta (sesión vencida), la interfaz vuelve al acceso. */
    val autenticado: StateFlow<Boolean>
    val usuarioActual: StateFlow<Usuario>
    suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoAcceso

    /** Con el servidor real devuelve [ResultadoAcceso.FaltaVerificarCorreo]: la sesión se abre al confirmar el código. */
    suspend fun registrarse(nombre: String, correo: String, contrasena: String): ResultadoAcceso

    /** Si el código es correcto, confirma el correo y abre la sesión. */
    suspend fun verificarCorreo(correo: String, codigo: String)
    suspend fun reenviarCodigo(correo: String)

    /** Pide el código para restablecer; responde igual exista o no la cuenta. */
    suspend fun recuperarContrasena(correo: String)
    suspend fun restablecerContrasena(correo: String, codigo: String, nueva: String)
    suspend fun iniciarSesionGoogle(): Boolean

    /** Si falla lanza la excepción y el modo no cambia: con el servidor real, el modo guardado es el suyo. */
    suspend fun cambiarModo(rol: Rol)
    /** Reemplaza las materias que enseña el tutor; el servidor exige al menos una. */
    suspend fun definirMaterias(materias: List<String>)
    fun cerrarSesion()

    /** Si la contraseña no coincide o quedan tutorías agendadas lanza el error y la cuenta sigue igual. */
    suspend fun eliminarCuenta(contrasena: String)
}
