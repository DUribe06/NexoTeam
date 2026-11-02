package com.grupo3.nexo.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.usecase.IniciarSesionGoogleUseCase
import com.grupo3.nexo.domain.usecase.IniciarSesionUseCase
import com.grupo3.nexo.domain.usecase.RegistrarseUseCase
import com.grupo3.nexo.domain.usecase.ValidarAccesoUseCase
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
        val autenticado: Boolean = false
    )

    private val _estado = MutableStateFlow(Estado())
    val estado = _estado.asStateFlow()

    fun cambiarNombre(valor: String) = _estado.update { it.copy(nombre = valor, error = null) }
    fun cambiarCorreo(valor: String) = _estado.update { it.copy(correo = valor.trim(), error = null) }
    fun cambiarContrasena(valor: String) = _estado.update { it.copy(contrasena = valor, error = null) }
    fun cambiarCondiciones(valor: Boolean) = _estado.update { it.copy(aceptaCondiciones = valor, error = null) }

    fun alternarModo() = _estado.update { it.copy(esRegistro = !it.esRegistro, error = null) }

    fun entrar() {
        val e = _estado.value
        if (e.cargando) return
        val error = validarAcceso(e.esRegistro, e.nombre, e.correo, e.contrasena, e.aceptaCondiciones)
        if (error != null) {
            _estado.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val exito = if (e.esRegistro) registrarse(e.nombre.trim(), e.correo, e.contrasena)
                        else iniciarSesion(e.correo, e.contrasena)
            _estado.update {
                it.copy(
                    cargando = false,
                    autenticado = exito,
                    error = if (exito) null else "No pudimos validar tus datos. Revisa el correo y la contraseña."
                )
            }
        }
    }

    fun entrarGoogle() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            iniciarSesionGoogle()
            _estado.update { it.copy(cargando = false, autenticado = true) }
        }
    }

    fun consumirNavegacion() = _estado.update { it.copy(autenticado = false) }
}
