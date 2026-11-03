package com.grupo3.nexo.ui.disponibilidad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.usecase.CrearCalendarioUseCase
import com.grupo3.nexo.domain.usecase.DefinirSemanaUseCase
import com.grupo3.nexo.domain.usecase.ObtenerDisponibilidadUseCase
import com.grupo3.nexo.ui.AccionesEnCurso
import com.grupo3.nexo.ui.Recarga
import com.grupo3.nexo.ui.falloDe
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Conteos de la semana publicada, listos para mostrar. */
data class ResumenDisponibilidad(
    val total: Int = 0,
    val virtuales: Int = 0,
    val presenciales: Int = 0,
    val diasActivos: Int = 0
)

/** Cómo van los cambios de la semana. */
enum class EstadoGuardado { SIN_CAMBIOS, GUARDANDO, GUARDADO, FALLO }

/** Espera tras el último cambio: varios gestos seguidos se guardan en una sola petición. */
private const val ESPERA_GUARDAR_MS = 600L

private const val MAXIMO_DESHACER = 30

/**
 * La semana se edita en memoria y se guarda sola y completa. Lo que se ve es el borrador mientras haya cambios sin
 * confirmar, así una respuesta del servidor nunca pisa lo que el tutor acaba de marcar.
 */
@HiltViewModel
class DisponibilidadViewModel @Inject constructor(
    obtenerDisponibilidad: ObtenerDisponibilidadUseCase,
    private val definirSemana: DefinirSemanaUseCase,
    private val crearCalendario: CrearCalendarioUseCase
) : ViewModel() {

    /** Lo que llegó de la semana: `dias` es null mientras cargan o si falló la lectura. */
    private data class Lectura(val dias: List<DiaDisponibilidad>? = null, val error: Boolean = false)

    private val recarga = Recarga()

    fun reintentar() = recarga.reintentar()

    private val lectura = recarga.proteger(alCargar = { Lectura() }, alFallar = { Lectura(error = true) }) {
        obtenerDisponibilidad().map { Lectura(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Lectura())

    private val borrador = MutableStateFlow<List<DiaDisponibilidad>?>(null)

    /** null mientras llegan los datos: así no se muestra "0 horas publicadas" por error. */
    val dias: StateFlow<List<DiaDisponibilidad>?> = combine(lectura, borrador) { l, b -> b ?: l.dias }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    private val _guardado = MutableStateFlow(EstadoGuardado.SIN_CAMBIOS)
    val guardado: StateFlow<EstadoGuardado> = _guardado.asStateFlow()

    private val historial = ArrayDeque<List<DiaDisponibilidad>>()
    private val _puedeDeshacer = MutableStateFlow(false)
    val puedeDeshacer: StateFlow<Boolean> = _puedeDeshacer.asStateFlow()

    /** Sobrevive a la pantalla: al volver atrás justo después de marcar, lo último también se guarda. */
    private val guardados = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var espera: Job? = null

    /** Una petición a la vez: una semana vieja nunca llega después de una más nueva. */
    private val turno = Mutex()

    private val enCurso = AccionesEnCurso()

    fun empezar(alTerminar: (Throwable?) -> Unit) {
        enCurso.lanzar(viewModelScope, "empezar") { alTerminar(falloDe { crearCalendario() }) }
    }

    /** Aplica un gesto: se ve de inmediato y se puede deshacer. */
    fun editar(cambio: (List<DiaDisponibilidad>) -> List<DiaDisponibilidad>) {
        val actual = dias.value ?: return
        val nueva = cambio(actual)
        if (nueva == actual) return
        historial.addLast(actual)
        if (historial.size > MAXIMO_DESHACER) historial.removeFirst()
        _puedeDeshacer.value = true
        cambiar(nueva)
    }

    fun deshacer() {
        val anterior = historial.removeLastOrNull() ?: return
        _puedeDeshacer.value = historial.isNotEmpty()
        cambiar(anterior)
    }

    /** Vuelve a intentar enviar lo que no se pudo guardar. */
    fun reintentarGuardar() {
        espera?.cancel()
        guardados.launch { enviar() }
    }

    private fun cambiar(nueva: List<DiaDisponibilidad>) {
        borrador.value = nueva
        _guardado.value = EstadoGuardado.GUARDANDO
        espera?.cancel()
        espera = guardados.launch {
            delay(ESPERA_GUARDAR_MS)
            enviar()
        }
    }

    private suspend fun enviar() = turno.withLock {
        val foto = borrador.value ?: return@withLock
        val fallo = falloDe { definirSemana(foto) }
        // Si hubo otro gesto mientras tanto, ese ya programó su propio envío
        if (borrador.value !== foto) return@withLock
        if (fallo == null) {
            borrador.value = null
            _guardado.value = EstadoGuardado.GUARDADO
        } else {
            // Se conserva el borrador: lo marcado no se pierde por un fallo de red
            _guardado.value = EstadoGuardado.FALLO
        }
    }

    override fun onCleared() {
        if (borrador.value != null) {
            espera?.cancel()
            guardados.launch { enviar() }
        }
    }
}
