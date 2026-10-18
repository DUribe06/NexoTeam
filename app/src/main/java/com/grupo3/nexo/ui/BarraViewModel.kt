package com.grupo3.nexo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.EstadoSolicitud
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.grupo3.nexo.domain.usecase.ObtenerChatsUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMisSolicitudesUseCase
import javax.inject.Inject

/**
 * Suministra los contadores de insignias de la barra de navegación
 * (solicitudes pendientes y mensajes sin leer).
 */
@HiltViewModel
class BarraViewModel @Inject constructor(
    obtenerMisSolicitudes: ObtenerMisSolicitudesUseCase,
    obtenerChats: ObtenerChatsUseCase
) : ViewModel() {

    /** Tutor: pendientes recibidas. Estudiante: pendientes enviadas por él. */
    val pendientes = obtenerMisSolicitudes()
        .map { lista -> lista.count { it.estado == EstadoSolicitud.PENDIENTE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Mensajes sin leer de las conversaciones del modo actual. */
    val noLeidos = obtenerChats()
        .map { lista -> lista.sumOf { it.noLeidos } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
