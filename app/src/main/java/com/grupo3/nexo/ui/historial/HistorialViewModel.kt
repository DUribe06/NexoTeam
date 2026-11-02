package com.grupo3.nexo.ui.historial

import com.grupo3.nexo.ui.AccionesEnCurso
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.ResumenActividad
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.usecase.CalificarSesionUseCase
import com.grupo3.nexo.domain.usecase.ObtenerHistorialUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMisResenasUseCase
import com.grupo3.nexo.domain.usecase.ObtenerResumenActividadUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import com.grupo3.nexo.ui.Recarga
import com.grupo3.nexo.ui.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistorialViewModel @Inject constructor(
    obtenerHistorial: ObtenerHistorialUseCase,
    obtenerUsuario: ObtenerUsuarioUseCase,
    obtenerResumen: ObtenerResumenActividadUseCase,
    obtenerResenas: ObtenerMisResenasUseCase,
    private val calificarSesion: CalificarSesionUseCase
) : ViewModel() {

    data class Estado(
        val cargando: Boolean = true,
        val todas: List<Sesion> = emptyList(),
        val materia: String? = null,
        /** En modo tutor el historial son las sesiones dadas y las reseñas recibidas (RF9, RF10). */
        val esTutor: Boolean = false,
        val resumen: ResumenActividad? = null,
        val resenas: List<Resena> = emptyList(),
        /** Falló la lectura: la pantalla muestra el error con "Reintentar". */
        val error: Boolean = false
    ) {
        /** Sesiones del filtro actual, en orden cronológico (la más reciente arriba). */
        val sesiones = todas.filter { materia == null || it.materia == materia }
        val materias = todas.map { it.materia }.distinct()
    }

    private val materia = MutableStateFlow<String?>(null)

    private val recarga = Recarga()

    fun reintentar() = recarga.reintentar()

    val estado = recarga.proteger(alCargar = { Estado() }, alFallar = { Estado(cargando = false, error = true) }) {
        combine(obtenerHistorial(), materia, obtenerUsuario(), obtenerResumen(), obtenerResenas()) { lista, m, usuario, resumen, resenas ->
            Estado(
                cargando = false,
                todas = lista,
                materia = m,
                esTutor = usuario.rol == Rol.TUTOR,
                resumen = resumen,
                resenas = if (usuario.rol == Rol.TUTOR) resenas else emptyList()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun filtrar(valor: String?) {
        materia.value = if (materia.value == valor) null else valor
    }

    /** Avisa con `alTerminar(true)` si se guardó y `alTerminar(false)` si falló. */
    private val enCurso = AccionesEnCurso()

    fun calificar(sesionId: String, estrellas: Int, comentario: String?, alTerminar: (Boolean) -> Unit) {
        enCurso.lanzar(viewModelScope, "calificar:$sesionId") { alTerminar(intentar { calificarSesion(sesionId, estrellas, comentario) }) }
    }
}
