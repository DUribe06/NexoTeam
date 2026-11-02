package com.grupo3.nexo.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.ResultadoAcceso
import com.grupo3.nexo.domain.usecase.IniciarSesionGoogleUseCase
import com.grupo3.nexo.domain.usecase.IniciarSesionUseCase
import com.grupo3.nexo.domain.usecase.RegistrarseUseCase
import com.grupo3.nexo.domain.usecase.ValidarAccesoUseCase
import com.grupo3.nexo.ui.resultadoDe
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val iniciarSesion: IniciarSesionUseCase,
    private val registrarse: RegistrarseUseCase,
    private val iniciarSesionGoogle: IniciarSesionGoogleUseCase,
    private val validarAcceso: ValidarAccesoUseCase
) : ViewModel() {

    data class Estado(
        val nombre: String = "",
        val correo: String = "",
        val contrasena: String = "",
        val aceptaCondiciones: Boolean = false,
        val esRegistro: Boolean = false,
        val cargando: Boolean = false,
        val error: String? = null,
        /** Confirmación en verde, por ejemplo tras cambiar la contraseña. */
        val aviso: String? = null,
        val autenticado: Boolean = false,
        /** Si no es null, hay que ir a confirmar este correo. */
        val correoPorVerificar: String? = null,
        /** Al entrar con un correo sin confirmar no se acaba de enviar ningún código: hay que pedir uno. */
        val reenviarAlVerificar: Boolean = false
    )

    private val _estado = MutableStateFlow(Estado())
    val estado = _estado.asStateFlow()

    fun cambiarNombre(valor: String) = _estado.update { it.copy(nombre = valor, error = null, aviso = null) }
    fun cambiarCorreo(valor: String) = _estado.update { it.copy(correo = valor.trim(), error = null, aviso = null) }
    fun cambiarContrasena(valor: String) = _estado.update { it.copy(contrasena = valor, error = null, aviso = null) }
    fun cambiarCondiciones(valor: Boolean) = _estado.update { it.copy(aceptaCondiciones = valor, error = null, aviso = null) }

    fun alternarModo() = _estado.update { it.copy(esRegistro = !it.esRegistro, error = null, aviso = null) }

    fun contrasenaRestablecida(correo: String) = _estado.update {
        it.copy(
            correo = correo,
            contrasena = "",
            esRegistro = false,
            error = null,
            aviso = "Listo. Ya puedes iniciar sesión con tu contraseña nueva."
        )
    }

    fun entrar() {
        val e = _estado.value
        if (e.cargando) return
        val error = validarAcceso(e.esRegistro, e.nombre, e.correo, e.contrasena, e.aceptaCondiciones)
        if (error != null) {
            _estado.update { it.copy(error = error, aviso = null) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, aviso = null) }
            val resultado = resultadoDe {
                if (e.esRegistro) registrarse(e.nombre.trim(), e.correo, e.contrasena)
                else iniciarSesion(e.correo, e.contrasena)
            }
            val acceso = resultado.getOrNull()
            _estado.update {
                it.copy(
                    cargando = false,
                    autenticado = acceso == ResultadoAcceso.Entro,
                    correoPorVerificar = (acceso as? ResultadoAcceso.FaltaVerificarCorreo)?.correo,
                    reenviarAlVerificar = !e.esRegistro,
                    error = resultado.fold(::mensajeDeAcceso, ::mensajeDeError)
                )
            }
        }
    }

    fun entrarGoogle() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, aviso = null) }
            val resultado = resultadoDe { iniciarSesionGoogle() }
            val fallo = resultado.exceptionOrNull()
            _estado.update {
                if (fallo == null) it.copy(cargando = false, autenticado = true)
                else it.copy(cargando = false, error = mensajeDeError(fallo))
            }
        }
    }

    fun consumirNavegacion() = _estado.update { it.copy(autenticado = false, correoPorVerificar = null) }
}
