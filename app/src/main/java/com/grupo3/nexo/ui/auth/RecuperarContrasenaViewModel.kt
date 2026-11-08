package com.grupo3.nexo.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.usecase.RecuperarContrasenaUseCase
import com.grupo3.nexo.domain.usecase.RestablecerContrasenaUseCase
import com.grupo3.nexo.domain.usecase.LARGO_CODIGO
import com.grupo3.nexo.domain.usecase.ValidarRestablecerUseCase
import com.grupo3.nexo.ui.navigation.Rutas
import com.grupo3.nexo.ui.resultadoDe
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecuperarContrasenaViewModel @Inject constructor(
    argumentos: SavedStateHandle,
    private val recuperar: RecuperarContrasenaUseCase,
    private val restablecer: RestablecerContrasenaUseCase,
    private val validar: ValidarRestablecerUseCase
) : ViewModel() {

    enum class Paso { PEDIR_CODIGO, CAMBIAR_CONTRASENA }

    data class Estado(
        val paso: Paso = Paso.PEDIR_CODIGO,
        val correo: String = "",
        val codigo: String = "",
        val nueva: String = "",
        val cargando: Boolean = false,
        val reenviando: Boolean = false,
        val espera: Int = 0,
        val error: String? = null,
        val aviso: String? = null,
        val codigoRechazado: Boolean = false,
        val listo: Boolean = false
    )

    private val _estado = MutableStateFlow(Estado(correo = argumentos[Rutas.RecuperarContrasena.ARG_CORREO] ?: ""))
    val estado = _estado.asStateFlow()

    private val esperaReenvio = EsperaReenvio(viewModelScope) { falta -> _estado.update { it.copy(espera = falta) } }

    fun cambiarCorreo(valor: String) = _estado.update { it.copy(correo = valor.trim(), error = null, aviso = null) }
    fun cambiarNueva(valor: String) = _estado.update { it.copy(nueva = valor, error = null, aviso = null) }
    fun cambiarCodigo(valor: String) = _estado.update {
        it.copy(codigo = valor.filter(Char::isDigit).take(LARGO_CODIGO), error = null, aviso = null, codigoRechazado = false)
    }

    /** Vuelve del segundo paso al primero, por si el correo estaba mal escrito. */
    fun volverAPedir() = _estado.update { it.copy(paso = Paso.PEDIR_CODIGO, codigo = "", nueva = "", error = null, aviso = null) }

    fun pedirCodigo() {
        val e = _estado.value
        if (e.cargando) return
        if (!e.correo.contains("@")) {
            _estado.update { it.copy(error = "Escribe un correo válido, por ejemplo correo@ejemplo.com") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val fallo = resultadoDe { recuperar(e.correo) }.exceptionOrNull()
            if (fallo == null) esperaReenvio.iniciar()
            _estado.update {
                it.copy(
                    cargando = false,
                    paso = if (fallo == null) Paso.CAMBIAR_CONTRASENA else it.paso,
                    error = fallo?.let(::mensajeDeError)
                )
            }
        }
    }

    fun reenviar() {
        val e = _estado.value
        if (e.reenviando || e.espera > 0) return
        viewModelScope.launch {
            _estado.update { it.copy(reenviando = true, error = null, aviso = null) }
            val fallo = resultadoDe { recuperar(e.correo) }.exceptionOrNull()
            val espera = (fallo as? ErrorApi)?.reintentarEnSegundos
            when {
                fallo == null -> esperaReenvio.iniciar()
                espera != null -> esperaReenvio.iniciar(espera)
            }
            _estado.update {
                it.copy(
                    reenviando = false,
                    codigo = if (fallo == null) "" else it.codigo,
                    codigoRechazado = if (fallo == null) false else it.codigoRechazado,
                    aviso = if (fallo == null) "Te enviamos un código nuevo." else null,
                    error = fallo?.let(::mensajeDeError)
                )
            }
        }
    }

    fun cambiarContrasena() {
        val e = _estado.value
        if (e.cargando) return
        val error = validar(e.codigo, e.nueva)
        if (error != null) {
            _estado.update { it.copy(error = error, aviso = null) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, aviso = null) }
            val fallo = resultadoDe { restablecer(e.correo, e.codigo, e.nueva) }.exceptionOrNull()
            _estado.update {
                it.copy(
                    cargando = false,
                    listo = fallo == null,
                    error = fallo?.let(::mensajeDeError),
                    codigoRechazado = (fallo as? ErrorApi)?.codigo in CODIGOS_RECHAZADOS
                )
            }
        }
    }

    private companion object {
        val CODIGOS_RECHAZADOS = setOf(
            CodigoError.CODIGO_INVALIDO,
            CodigoError.CODIGO_VENCIDO,
            CodigoError.CODIGO_INTENTOS_AGOTADOS
        )
    }
}
