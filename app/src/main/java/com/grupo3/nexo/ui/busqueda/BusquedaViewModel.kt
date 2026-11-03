package com.grupo3.nexo.ui.busqueda

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.AreaEstudio
import com.grupo3.nexo.domain.model.FiltroTutores
import com.grupo3.nexo.domain.model.Franja
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.usecase.BuscarTutoresUseCase
import com.grupo3.nexo.ui.Recarga
import com.grupo3.nexo.ui.navigation.Rutas
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BusquedaViewModel @Inject constructor(
    buscarTutores: BuscarTutoresUseCase,
    estadoGuardado: SavedStateHandle
) : ViewModel() {

    data class Estado(
        val consulta: String = "",
        val area: AreaEstudio? = null,
        val modalidad: Modalidad? = null,
        val franja: Franja? = null,
        val soloEnLinea: Boolean = false,
        val resultados: List<Tutor> = emptyList(),
        val cargando: Boolean = true,
        /** Falló la búsqueda: la pantalla muestra el error con "Reintentar". */
        val error: Boolean = false
    ) {
        /** Filtros de la hoja (no cuenta texto ni área, que están a la vista). */
        val filtrosHoja get() = listOf(modalidad != null, franja != null, soloEnLinea).count { it }
        val hayFiltros get() = consulta.isNotBlank() || area != null || filtrosHoja > 0
    }

    private val consulta = MutableStateFlow("")
    private val area = MutableStateFlow(AreaEstudio.desde(estadoGuardado.get<String>(Rutas.Busqueda.ARG_AREA)))
    private val modalidad = MutableStateFlow<Modalidad?>(null)
    private val franja = MutableStateFlow<Franja?>(null)
    private val soloEnLinea = MutableStateFlow(false)

    // El texto espera a que se deje de escribir; vacío (primera carga o borrar todo) pasa sin demora
    @OptIn(FlowPreview::class)
    private val consultaRetrasada = consulta.debounce { if (it.isEmpty()) 0L else RETRASO_CONSULTA_MS }

    private val filtro = combine(consultaRetrasada, area, modalidad, franja, soloEnLinea) { q, a, m, f, l ->
        FiltroTutores(consulta = q, materias = a?.claves.orEmpty(), modalidad = m, franja = f, soloEnLinea = l)
    }

    private val recarga = Recarga()

    fun reintentar() = recarga.reintentar()

    private sealed interface Resultado {
        data object Cargando : Resultado
        data object Fallo : Resultado
        data class Lista(val tutores: List<Tutor>) : Resultado
    }

    // Los filtros se conservan aunque falle la búsqueda: solo los resultados pasan a error
    private val busqueda = filtro.flatMapLatest { f ->
        recarga.proteger(alCargar = { Resultado.Cargando }, alFallar = { Resultado.Fallo }) {
            buscarTutores(f).map<List<Tutor>, Resultado> { Resultado.Lista(it) }
        }
    }

    val estado = combine(
        combine(consulta, area, modalidad, franja, soloEnLinea) { q, a, m, f, l -> Estado(q, a, m, f, l) },
        busqueda
    ) { base, resultado ->
        when (resultado) {
            Resultado.Cargando -> base.copy(resultados = emptyList(), cargando = true, error = false)
            Resultado.Fallo -> base.copy(resultados = emptyList(), cargando = false, error = true)
            is Resultado.Lista -> base.copy(resultados = resultado.tutores, cargando = false, error = false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun cambiarConsulta(valor: String) { consulta.value = valor }
    fun alternarArea(valor: AreaEstudio) { area.update { if (it == valor) null else valor } }
    fun elegirModalidad(valor: Modalidad?) { modalidad.value = valor }
    fun elegirFranja(valor: Franja?) { franja.value = valor }
    fun cambiarEnLinea(valor: Boolean) { soloEnLinea.value = valor }
    fun limpiarHoja() {
        modalidad.value = null
        franja.value = null
        soloEnLinea.value = false
    }
    fun limpiarTodo() {
        consulta.value = ""
        area.value = null
        limpiarHoja()
    }

    private companion object {
        const val RETRASO_CONSULTA_MS = 300L
    }
}
