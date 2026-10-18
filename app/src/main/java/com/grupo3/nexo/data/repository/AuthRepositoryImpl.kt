package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.mock.MockData
import com.grupo3.nexo.domain.model.EstadoVerificacion
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    private val _autenticado = MutableStateFlow(false)
    private val _usuario = MutableStateFlow(MockData.usuario)

    override val autenticado: StateFlow<Boolean> = _autenticado.asStateFlow()
    override val usuarioActual: StateFlow<Usuario> = _usuario.asStateFlow()

    override suspend fun iniciarSesion(correo: String, contrasena: String): Boolean {
        delay(800)
        val valido = correo.contains("@") && contrasena.length >= 4
        if (valido) {
            _usuario.update { it.copy(correo = correo) }
            _autenticado.value = true
        }
        return valido
    }

    override suspend fun registrarse(nombre: String, correo: String, contrasena: String, rol: Rol): Boolean {
        delay(900)
        val valido = nombre.isNotBlank() && correo.contains("@") && contrasena.length >= 4
        if (valido) {
            _usuario.update {
                // Cuenta nueva: sin historial; si se registra como tutor queda pendiente de verificación (RF1)
                Usuario(
                    id = it.id,
                    nombre = nombre,
                    correo = correo,
                    handle = "@" + nombre.lowercase().replace(" ", "."),
                    rol = rol,
                    verificacion = if (rol == Rol.TUTOR) EstadoVerificacion.PENDIENTE else null
                )
            }
            _autenticado.value = true
        }
        return valido
    }

    override suspend fun iniciarSesionGoogle(): Boolean {
        delay(700)
        _autenticado.value = true
        return true
    }

    override fun cambiarModo(rol: Rol) {
        // Pasar a tutor por primera vez abre la solicitud de verificación (RF12)
        _usuario.update { it.copy(rol = rol, verificacion = if (rol == Rol.TUTOR) it.verificacion ?: EstadoVerificacion.PENDIENTE else it.verificacion) }
    }

    override fun cerrarSesion() {
        _autenticado.value = false
        _usuario.value = MockData.usuario
    }
}
