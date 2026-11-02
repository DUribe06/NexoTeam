package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Usuario
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de datos de la sesión y el usuario.
 *
 * Las lecturas (`Flow`) y las escrituras (`suspend`) de una implementación remota deben lanzar una excepción
 * ante fallos de red o del servidor: la UI la captura, muestra el error con "Reintentar" o un aviso breve.
 * Aquí `false` significa credenciales rechazadas; la excepción, que no hubo conexión.
 */
interface AuthRepository {
    val autenticado: StateFlow<Boolean>
    val usuarioActual: StateFlow<Usuario>
    suspend fun iniciarSesion(correo: String, contrasena: String): Boolean
    suspend fun registrarse(nombre: String, correo: String, contrasena: String): Boolean
    suspend fun iniciarSesionGoogle(): Boolean
    fun cambiarModo(rol: Rol)
    fun cerrarSesion()
}
