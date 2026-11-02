package com.grupo3.nexo.ui.disponibilidad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.usecase.CrearCalendarioUseCase
import com.grupo3.nexo.domain.usecase.DefinirHoraUseCase
import com.grupo3.nexo.domain.usecase.ObtenerDisponibilidadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DisponibilidadViewModel @Inject constructor(
    obtenerDisponibilidad: ObtenerDisponibilidadUseCase,
    private val definirHora: DefinirHoraUseCase,
    private val crearCalendario: CrearCalendarioUseCase
) : ViewModel() {

    /** null mientras llegan los datos: así no se muestra "0 horas publicadas" por error. */
    val dias = obtenerDisponibilidad()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null as List<DiaDisponibilidad>?)

    fun empezar() {
        viewModelScope.launch { crearCalendario() }
    }

    fun definir(dia: String, hora: String, modalidades: Set<Modalidad>) {
        viewModelScope.launch { definirHora(dia, hora, modalidades) }
    }
}
