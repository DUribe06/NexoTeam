package com.grupo3.nexo.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.ResumenActividad
import com.grupo3.nexo.domain.usecase.ObtenerMisResenasUseCase
import com.grupo3.nexo.domain.usecase.ObtenerResumenActividadUseCase
import com.grupo3.nexo.ui.Recarga
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MisResenasViewModel @Inject constructor(
    obtenerResenas: ObtenerMisResenasUseCase,
    obtenerResumen: ObtenerResumenActividadUseCase
) : ViewModel() {

    data class Estado(
        val cargando: Boolean = true,
        val resenas: List<Resena> = emptyList(),
        val resumen: ResumenActividad? = null,
        val error: Boolean = false
    )

    private val recarga = Recarga()

    fun reintentar() = recarga.reintentar()

    val estado = recarga.proteger(alCargar = { Estado() }, alFallar = { Estado(cargando = false, error = true) }) {
        combine(obtenerResenas(), obtenerResumen()) { resenas, resumen ->
            Estado(cargando = false, resenas = resenas, resumen = resumen)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())
}
