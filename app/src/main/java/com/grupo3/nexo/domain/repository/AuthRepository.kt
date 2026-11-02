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
    fun cambiarModo(rol: Rol)
    fun cerrarSesion()
}
