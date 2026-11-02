package com.grupo3.nexo.ui.tutor

import com.grupo3.nexo.ui.AccionesEnCurso
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.model.primerNombre
import com.grupo3.nexo.domain.usecase.EnviarSolicitudUseCase
import com.grupo3.nexo.domain.usecase.ObtenerResenasUseCase
import com.grupo3.nexo.domain.usecase.ObtenerTutorUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import com.grupo3.nexo.domain.usecase.ReportarTutorUseCase
import com.grupo3.nexo.ui.Recarga
import com.grupo3.nexo.ui.intentar
import com.grupo3.nexo.ui.navigation.Rutas
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Confirmaciones que el ViewModel anuncia; el texto lo pone la pantalla. */
sealed interface AvisoPerfil {
    data class SolicitudEnviada(val tutor: String) : AvisoPerfil
    data object ReporteEnviado : AvisoPerfil
    data object SolicitudFallida : AvisoPerfil
    data object ReporteFallido : AvisoPerfil
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TutorPerfilViewModel @Inject constructor(
    private val obtenerTutor: ObtenerTutorUseCase,
    private val obtenerResenas: ObtenerResenasUseCase,
    private val enviarSolicitud: EnviarSolicitudUseCase,
    private val reportarTutor: ReportarTutorUseCase,
    obtenerUsuario: ObtenerUsuarioUseCase,
    estadoGuardado: SavedStateHandle
) : ViewModel() {

    data class Estado(
        val tutor: Tutor? = null,
        val resenas: List<Resena> = emptyList(),
        val aviso: AvisoPerfil? = null,
        /** true hasta que llega la primera respuesta del tutor. */
        val cargando: Boolean = true,
        val noEncontrado: Boolean = false,
        /** Un tutor viendo a otro tutor no puede solicitarle tutorías. */
        val esTutor: Boolean = false,
        /** Falló la lectura: la pantalla muestra el error con "Reintentar". */
        val error: Boolean = false
    )

    private val tutorId = MutableStateFlow(estadoGuardado.get<String>(Rutas.TutorPerfil.ARG_TUTOR).orEmpty())
    private val aviso = MutableStateFlow<AvisoPerfil?>(null)

    private val tutor: Flow<Tutor?> = tutorId.flatMapLatest { id ->
        if (id.isEmpty()) emptyFlow() else obtenerTutor(id)
    }
    private val resenas: Flow<List<Resena>> = tutorId.flatMapLatest { id ->
        if (id.isEmpty()) emptyFlow() else obtenerResenas(id)
    }

    private val recarga = Recarga()

    fun reintentar() = recarga.reintentar()

    val estado = recarga.proteger(alCargar = { Estado() }, alFallar = { Estado(cargando = false, error = true) }) {
        combine(tutor, resenas, aviso, obtenerUsuario()) { t, r, a, u ->
            Estado(tutor = t, resenas = r, aviso = a, cargando = false, noEncontrado = t == null, esTutor = u.rol == Rol.TUTOR)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    private val enCurso = AccionesEnCurso()

    fun solicitar(datos: DatosSolicitud) {
        val t = estado.value.tutor ?: return
        enCurso.lanzar(viewModelScope, "solicitar") {
            val exito = intentar { enviarSolicitud(t, datos.materia, datos.tema, datos.fecha, datos.hora, datos.modalidad, datos.mensaje) }
            aviso.value = if (exito) AvisoPerfil.SolicitudEnviada(t.nombre.primerNombre) else AvisoPerfil.SolicitudFallida
        }
    }

    fun reportar(motivo: String) {
        val t = estado.value.tutor ?: return
        enCurso.lanzar(viewModelScope, "reportar") {
            aviso.value = if (intentar { reportarTutor(t.id, motivo) }) AvisoPerfil.ReporteEnviado else AvisoPerfil.ReporteFallido
        }
    }

    fun descartarAviso() {
        aviso.value = null
    }
}
