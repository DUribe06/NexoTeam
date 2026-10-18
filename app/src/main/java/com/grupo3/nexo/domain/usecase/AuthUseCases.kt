package com.grupo3.nexo.domain.usecase

import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.repository.AuthRepository
import javax.inject.Inject

/** Inicia sesión con correo y contraseña. */
class IniciarSesionUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(correo: String, contrasena: String): Boolean =
        repositorio.iniciarSesion(correo, contrasena)
}

/** Registra una cuenta nueva. */
class RegistrarseUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(nombre: String, correo: String, contrasena: String, rol: Rol): Boolean =
        repositorio.registrarse(nombre, correo, contrasena, rol)
}

/** Inicia sesión con Google (flujo simplificado para el prototipo). */
class IniciarSesionGoogleUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(): Boolean = repositorio.iniciarSesionGoogle()
}

/** Cambia el modo de uso: Estudiante ↔ Tutor. */
class CambiarModoUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    operator fun invoke(rol: Rol) = repositorio.cambiarModo(rol)
}

/** Cierra la sesión actual. */
class CerrarSesionUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    operator fun invoke() = repositorio.cerrarSesion()
}

/** Usuario en sesión (para personalizar la interfaz según su rol). */
class ObtenerUsuarioUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    operator fun invoke(): kotlinx.coroutines.flow.StateFlow<Usuario> = repositorio.usuarioActual
}
