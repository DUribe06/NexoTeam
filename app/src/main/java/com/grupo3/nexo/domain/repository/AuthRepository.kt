package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Usuario
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val autenticado: StateFlow<Boolean>
    val usuarioActual: StateFlow<Usuario>
    suspend fun iniciarSesion(correo: String, contrasena: String): Boolean
    suspend fun registrarse(nombre: String, correo: String, contrasena: String, rol: Rol): Boolean
    suspend fun iniciarSesionGoogle(): Boolean
    fun cambiarModo(rol: Rol)
    fun cerrarSesion()
}
