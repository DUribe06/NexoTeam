package com.grupo3.nexo.ui.disponibilidad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.usecase.CrearCalendarioUseCase
import com.grupo3.nexo.domain.usecase.DefinirHoraUseCase
import com.grupo3.nexo.domain.usecase.ObtenerDisponibilidadUseCase
import com.grupo3.nexo.ui.Recarga
import com.grupo3.nexo.ui.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Conteos de la semana publicada, listos para mostrar. */
data class ResumenDisponibilidad(
    val total: Int = 0,
    val virtuales: Int = 0,
    val presenciales: Int = 0,
    val diasActivos: Int = 0
)

@HiltViewModel
class DisponibilidadViewModel @Inject constructor(
    obtenerDisponibilidad: ObtenerDisponibilidadUseCase,
    private val definirHora: DefinirHoraUseCase,
    private val crearCalendario: CrearCalendarioUseCase
) : ViewModel() {

    /** Lo que llegó de la semana: `dias` es null mientras cargan o si falló la lectura. */
    private data class Lectura(val dias: List<DiaDisponibilidad>? = null, val error: Boolean = false)

    private val recarga = Recarga()

    fun reintentar() = recarga.reintentar()

    private val lectura = recarga.proteger(alCargar = { Lectura() }, alFallar = { Lectura(error = true) }) {
        obtenerDisponibilidad().map { Lectura(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Lectura())

    /** null mientras llegan los datos: así no se muestra "0 horas publicadas" por error. */
    val dias = lectura.map { it.dias }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null as List<DiaDisponibilidad>?)

    /** Falló la lectura: la pantalla muestra el error con "Reintentar". */
    val error = lectura.map { it.error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val resumen = dias.map { lista ->
        val horas = lista.orEmpty().flatMap { it.horas }
        ResumenDisponibilidad(
            total = horas.count { it.seleccionada },
            virtuales = horas.count { Modalidad.VIRTUAL in it.modalidades },
            presenciales = horas.count { Modalidad.PRESENCIAL in it.modalidades },
            diasActivos = lista.orEmpty().count { it.horasActivas > 0 }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResumenDisponibilidad())

    /** Las dos acciones avisan con `alTerminar(true)` si se guardó y `alTerminar(false)` si falló. */
    fun empezar(alTerminar: (Boolean) -> Unit) {
        viewModelScope.launch { alTerminar(intentar { crearCalendario() }) }
    }

    fun definir(dia: String, hora: String, modalidades: Set<Modalidad>, alTerminar: (Boolean) -> Unit) {
        viewModelScope.launch { alTerminar(intentar { definirHora(dia, hora, modalidades) }) }
    }
}
