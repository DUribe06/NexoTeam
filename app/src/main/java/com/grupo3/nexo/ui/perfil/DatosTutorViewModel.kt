package com.grupo3.nexo.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.usecase.EditarDatosTutorUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import com.grupo3.nexo.ui.falloDe
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Lo que el tutor escribe en "Mi perfil de tutor"; la tarifa va como cifras sin puntos. */
data class DatosTutor(val tarifa: String, val universidad: String, val carrera: String, val descripcion: String)

sealed interface ResultadoGuardar {
    data object Guardado : ResultadoGuardar
    data class Fallo(val causa: Throwable) : ResultadoGuardar
}

/** Edita universidad, carrera, descripción y tarifa del tutor (RF4). Guarda solo cuando el tutor lo pide. */
@HiltViewModel
class DatosTutorViewModel @Inject constructor(
    obtenerUsuario: ObtenerUsuarioUseCase,
    private val editarDatosTutor: EditarDatosTutorUseCase
) : ViewModel() {

    private val usuario = obtenerUsuario()

    /** Lo guardado en el servidor, como punto de partida y para saber si hay cambios. */
    val iniciales: DatosTutor = usuario.value.let {
        DatosTutor(
            tarifa = it.precioHora?.takeIf { p -> p > 0 }?.toLong()?.toString().orEmpty(),
            universidad = it.universidad,
            carrera = it.carrera,
            descripcion = it.bio
        )
    }

    private val _guardando = MutableStateFlow(false)
    val guardando: StateFlow<Boolean> = _guardando

    private val _resultados = MutableSharedFlow<ResultadoGuardar>(extraBufferCapacity = 1)
    val resultados: SharedFlow<ResultadoGuardar> = _resultados

    fun guardar(datos: DatosTutor) {
        if (_guardando.value) return
        _guardando.value = true
        viewModelScope.launch {
            // Vaciar la tarifa la deja "a convenir" (0); si nunca tuvo, no se envía
            val precio = datos.tarifa.toDoubleOrNull() ?: if (usuario.value.precioHora != null) 0.0 else null
            val fallo = falloDe { editarDatosTutor(datos.universidad, datos.carrera, datos.descripcion, precio) }
            _guardando.value = false
            _resultados.tryEmit(if (fallo == null) ResultadoGuardar.Guardado else ResultadoGuardar.Fallo(fallo))
        }
    }
}
