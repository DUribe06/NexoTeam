package com.grupo3.nexo.ui.busqueda

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.AreaEstudio
import com.grupo3.nexo.domain.model.Franja
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.usecase.BuscarTutoresUseCase
import com.grupo3.nexo.domain.usecase.FiltroTutores
import com.grupo3.nexo.ui.Recarga
import com.grupo3.nexo.ui.navigation.Rutas
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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

    private val filtro = combine(consulta, area, modalidad, franja, soloEnLinea) { q, a, m, f, l ->
        FiltroTutores(consulta = q, materias = a?.claves.orEmpty(), modalidad = m, franja = f, soloEnLinea = l)
    }

    private val recarga = Recarga()

    fun reintentar() = recarga.reintentar()

    // Los filtros se conservan aunque falle la búsqueda: solo los resultados pasan a null (error)
    private val busqueda = combine(filtro, recarga.intentos) { f, _ -> f }
        .flatMapLatest { f -> buscarTutores(f).map<List<Tutor>, List<Tutor>?> { it }.catch { emit(null) } }

    val estado = combine(
        combine(consulta, area, modalidad, franja, soloEnLinea) { q, a, m, f, l -> Estado(q, a, m, f, l) },
        busqueda
    ) { base, resultados -> base.copy(resultados = resultados.orEmpty(), cargando = false, error = resultados == null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

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
}
