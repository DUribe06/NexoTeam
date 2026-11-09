package com.grupo3.nexo.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.usecase.LARGO_CODIGO
import com.grupo3.nexo.domain.usecase.ReenviarCodigoUseCase
import com.grupo3.nexo.domain.usecase.VerificarCorreoUseCase
import com.grupo3.nexo.ui.navigation.Rutas
import com.grupo3.nexo.ui.resultadoDe
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ConfirmarCorreoViewModel @Inject constructor(
    argumentos: SavedStateHandle,
    private val verificarCorreo: VerificarCorreoUseCase,
    private val reenviarCodigo: ReenviarCodigoUseCase
) : ViewModel() {

    data class Estado(
        val correo: String = "",
        val codigo: String = "",
        val cargando: Boolean = false,
        val reenviando: Boolean = false,
        /** Segundos que faltan para poder pedir otro código. */
        val espera: Int = 0,
        val error: String? = null,
        val aviso: String? = null,
        /** El servidor rechazó el código (no fue un fallo de conexión): las casillas se marcan en rojo. */
        val codigoRechazado: Boolean = false,
        val entro: Boolean = false
    )

    private val _estado = MutableStateFlow(Estado(correo = argumentos[Rutas.ConfirmarCorreo.ARG_CORREO] ?: ""))
    val estado = _estado.asStateFlow()

    private val esperaReenvio = EsperaReenvio(viewModelScope) { falta -> _estado.update { it.copy(espera = falta) } }

    init {
        // Tras registrarse el código acaba de salir; al llegar desde el login hay que pedir uno
        if (argumentos[Rutas.ConfirmarCorreo.ARG_REENVIAR] ?: false) enviarCodigo(automatico = true)
        else esperaReenvio.iniciar()
    }

    /** Solo dígitos, para que pegar "482 913" funcione; al completar los 6 se envía solo. */
    fun cambiarCodigo(valor: String) {
        val anterior = _estado.value.codigo
        val codigo = valor.filter(Char::isDigit).take(LARGO_CODIGO)
        if (codigo == anterior) return
        _estado.update { it.copy(codigo = codigo, error = null, aviso = null, codigoRechazado = false) }
        if (codigo.length == LARGO_CODIGO) confirmar()
    }

    fun confirmar() {
        val e = _estado.value
        if (e.cargando) return
        if (e.codigo.length != LARGO_CODIGO) {
            _estado.update { it.copy(error = "Escribe los $LARGO_CODIGO dígitos del código.", aviso = null) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, aviso = null) }
            val fallo = resultadoDe { verificarCorreo(e.correo, e.codigo) }.exceptionOrNull()
            val rechazado = (fallo as? ErrorApi)?.codigo in CODIGOS_RECHAZADOS
            _estado.update {
                it.copy(
                    cargando = false,
                    entro = fallo == null,
                    error = fallo?.let(::mensajeDeError),
                    codigoRechazado = rechazado,
                    // Un código rechazado ya no sirve: se vacía para escribir otro
                    codigo = if (rechazado) "" else it.codigo
                )
            }
        }
    }

    fun reenviar() = enviarCodigo(automatico = false)

    private fun enviarCodigo(automatico: Boolean) {
        val e = _estado.value
        if (e.reenviando || e.espera > 0) return
        viewModelScope.launch {
            _estado.update { it.copy(reenviando = true, error = null, aviso = null) }
            val fallo = resultadoDe { reenviarCodigo(e.correo) }.exceptionOrNull()
            val espera = (fallo as? ErrorApi)?.reintentarEnSegundos
            when {
                fallo == null -> esperaReenvio.iniciar()
                espera != null -> esperaReenvio.iniciar(espera)
            }
            // Un 429 al abrir la pantalla solo significa que ya hay un código reciente: no es un error para la persona
            val silenciar = automatico && espera != null
            _estado.update {
                it.copy(
                    reenviando = false,
                    // El código anterior deja de servir
                    codigo = if (fallo == null) "" else it.codigo,
                    codigoRechazado = if (fallo == null) false else it.codigoRechazado,
                    aviso = if (fallo == null) "Te enviamos un código nuevo." else null,
                    error = if (fallo == null || silenciar) null else mensajeDeError(fallo)
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
