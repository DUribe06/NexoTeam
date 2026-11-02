package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.mock.MockData
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.ResultadoAcceso
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Cuentas simuladas. El registro entra directo, sin pasar por el código. Para revisar los demás casos:
 * entrar con `sinverificar@…` lleva a confirmar el correo, con `bloqueada@…` muestra el bloqueo,
 * y el código `000000` se rechaza como incorrecto.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    private val _autenticado = MutableStateFlow(false)
    private val _usuario = MutableStateFlow(MockData.usuario)

    override val autenticado: StateFlow<Boolean> = _autenticado.asStateFlow()
    override val usuarioActual: StateFlow<Usuario> = _usuario.asStateFlow()

    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoAcceso {
        delay(800)
        val valido = correo.contains("@") && contrasena.length >= 4
        return when {
            !valido -> ResultadoAcceso.CredencialesIncorrectas
            correo.startsWith("sinverificar@") -> ResultadoAcceso.FaltaVerificarCorreo(correo)
            correo.startsWith("bloqueada@") -> ResultadoAcceso.CuentaBloqueada(MINUTOS_BLOQUEO)
            else -> {
                _usuario.update { it.copy(correo = correo) }
                _autenticado.value = true
                ResultadoAcceso.Entro
            }
        }
    }

    override suspend fun registrarse(nombre: String, correo: String, contrasena: String): ResultadoAcceso {
        delay(900)
        val valido = nombre.isNotBlank() && correo.contains("@") && contrasena.length >= 4
        if (!valido) return ResultadoAcceso.CredencialesIncorrectas
        _usuario.update {
            // Cuenta nueva: sin historial; empieza como estudiante y cambia de modo desde el Perfil
            Usuario(
                id = it.id,
                nombre = nombre,
                correo = correo,
                handle = "@" + nombre.lowercase().replace(" ", "."),
                rol = Rol.ESTUDIANTE
            )
        }
        _autenticado.value = true
        return ResultadoAcceso.Entro
    }

    override suspend fun verificarCorreo(correo: String, codigo: String) {
        delay(800)
        exigirCodigoValido(codigo)
        _usuario.update { it.copy(correo = correo) }
        _autenticado.value = true
    }

    override suspend fun reenviarCodigo(correo: String) = delay(600)

    override suspend fun recuperarContrasena(correo: String) = delay(700)

    override suspend fun restablecerContrasena(correo: String, codigo: String, nueva: String) {
        delay(800)
        exigirCodigoValido(codigo)
    }

    override suspend fun iniciarSesionGoogle(): Boolean {
        delay(700)
        _autenticado.value = true
        return true
    }

    override fun cambiarModo(rol: Rol) {
        _usuario.update { it.copy(rol = rol) }
    }

    override fun cerrarSesion() {
        _autenticado.value = false
        _usuario.value = MockData.usuario
    }

    private fun exigirCodigoValido(codigo: String) {
        if (codigo == CODIGO_RECHAZADO) throw ErrorApi(CodigoError.CODIGO_INVALIDO, "El código es incorrecto")
    }

    private companion object {
        const val CODIGO_RECHAZADO = "000000"
        const val MINUTOS_BLOQUEO = 15
    }
}
