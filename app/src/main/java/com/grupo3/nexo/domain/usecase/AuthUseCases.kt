package com.grupo3.nexo.domain.usecase

import com.grupo3.nexo.domain.model.PRECIO_MAXIMO
import com.grupo3.nexo.domain.model.ResultadoAcceso
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.repository.AuthRepository
import javax.inject.Inject

/** Inicia sesión con correo y contraseña. */
class IniciarSesionUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(correo: String, contrasena: String): ResultadoAcceso =
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
    suspend operator fun invoke(nombre: String, correo: String, contrasena: String): ResultadoAcceso =
        repositorio.registrarse(nombre, correo, contrasena)
}

/** Confirma el correo con el código de 6 dígitos y abre la sesión. */
class VerificarCorreoUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(correo: String, codigo: String) = repositorio.verificarCorreo(correo, codigo)
}

/** Pide otro código para confirmar el correo. */
class ReenviarCodigoUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(correo: String) = repositorio.reenviarCodigo(correo)
}

/** Pide el código para restablecer la contraseña. */
class RecuperarContrasenaUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(correo: String) = repositorio.recuperarContrasena(correo)
}

/** Cambia la contraseña con el código recibido. */
class RestablecerContrasenaUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(correo: String, codigo: String, nueva: String) =
        repositorio.restablecerContrasena(correo, codigo, nueva)
}

/** Valida los datos de la contraseña nueva; devuelve el mensaje de error o null si todo está en orden. */
class ValidarRestablecerUseCase @Inject constructor() {
    operator fun invoke(codigo: String, nueva: String): String? = when {
        codigo.length != LARGO_CODIGO -> "Escribe los $LARGO_CODIGO dígitos del código"
        nueva.length < MINIMO_CONTRASENA -> "La contraseña debe tener al menos $MINIMO_CONTRASENA caracteres"
        else -> null
    }

    private companion object {
        const val MINIMO_CONTRASENA = 8
    }
}

/** Dígitos del código que llega al correo. */
const val LARGO_CODIGO = 6

/** Dice si hay sesión abierta; si pasa a false, la interfaz vuelve al acceso. */
class ObtenerAutenticadoUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    operator fun invoke(): kotlinx.coroutines.flow.StateFlow<Boolean> = repositorio.autenticado
}

/** Inicia sesión con el token que entregó Google. */
class IniciarSesionGoogleUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(tokenId: String, aceptaCondiciones: Boolean): ResultadoAcceso =
        repositorio.iniciarSesionGoogle(tokenId, aceptaCondiciones)
}

/** Guarda las materias que enseña el tutor (RF4). */
class DefinirMateriasUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(materias: List<String>) = repositorio.definirMaterias(materias)
}

/** Guarda los datos públicos del tutor: universidad, carrera, descripción y tarifa por hora. */
class EditarDatosTutorUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(universidad: String, carrera: String, bio: String, precioHora: Double?) {
        require(precioHora == null || precioHora in 0.0..PRECIO_MAXIMO) { "La tarifa no es válida" }
        require(bio.trim().length <= LARGO_MAXIMO_DESCRIPCION) { "La descripción es muy larga" }
        repositorio.editarDatosTutor(universidad, carrera, bio, precioHora)
    }
}

/** Lo mismo que acepta el servidor. */
const val LARGO_MAXIMO_DESCRIPCION = 500
const val LARGO_MAXIMO_UNIVERSIDAD = 120

/** Cambia el modo de uso: Estudiante ↔ Tutor. */
class CambiarModoUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(rol: Rol) = repositorio.cambiarModo(rol)
}

/** Cierra la sesión actual. */
class CerrarSesionUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    operator fun invoke() = repositorio.cerrarSesion()
}

/** Elimina la cuenta propia después de confirmar la contraseña. */
class EliminarCuentaUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    suspend operator fun invoke(contrasena: String) = repositorio.eliminarCuenta(contrasena)

    suspend fun conGoogle(tokenGoogle: String) = repositorio.eliminarCuentaConGoogle(tokenGoogle)
}

/** Usuario en sesión (para personalizar la interfaz según su rol). */
class ObtenerUsuarioUseCase @Inject constructor(
    private val repositorio: AuthRepository
) {
    operator fun invoke(): kotlinx.coroutines.flow.StateFlow<Usuario> = repositorio.usuarioActual
}
