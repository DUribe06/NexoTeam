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

/** Valida los datos del acceso; devuelve el mensaje de error o null si todo está en orden. */
class ValidarAccesoUseCase @Inject constructor() {
    operator fun invoke(
        esRegistro: Boolean,
        nombre: String,
        correo: String,
        contrasena: String,
        aceptaCondiciones: Boolean
    ): String? = when {
        esRegistro && nombre.isBlank() -> "Escribe tu nombre o un alias"
        correo.isBlank() || contrasena.isBlank() -> "Completa todos los campos para continuar"
        !correo.contains("@") -> "Escribe un correo válido, por ejemplo correo@ejemplo.com"
        contrasena.length < 4 -> "La contraseña debe tener al menos 4 caracteres"
        esRegistro && !aceptaCondiciones -> "Acepta las condiciones de uso para vincularte"
        else -> null
    }
}

/** Registra una cuenta nueva. */
class RegistrarseUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(nombre: String, correo: String, contrasena: String): Boolean =
        repositorio.registrarse(nombre, correo, contrasena)
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
