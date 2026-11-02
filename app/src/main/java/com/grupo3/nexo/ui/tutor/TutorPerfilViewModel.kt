package com.grupo3.nexo.ui.tutor

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
        val esTutor: Boolean = false
    )

    private val tutorId = MutableStateFlow(estadoGuardado.get<String>(Rutas.TutorPerfil.ARG_TUTOR).orEmpty())
    private val aviso = MutableStateFlow<AvisoPerfil?>(null)

    private val tutor: Flow<Tutor?> = tutorId.flatMapLatest { id ->
        if (id.isEmpty()) emptyFlow() else obtenerTutor(id)
    }
    private val resenas: Flow<List<Resena>> = tutorId.flatMapLatest { id ->
        if (id.isEmpty()) emptyFlow() else obtenerResenas(id)
    }

    val estado = combine(tutor, resenas, aviso, obtenerUsuario()) { t, r, a, u ->
        Estado(tutor = t, resenas = r, aviso = a, cargando = false, noEncontrado = t == null, esTutor = u.rol == Rol.TUTOR)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun solicitar(datos: DatosSolicitud) {
        val t = estado.value.tutor ?: return
        viewModelScope.launch {
            enviarSolicitud(t, datos.materia, datos.tema, datos.fecha, datos.hora, datos.modalidad, datos.mensaje)
            aviso.value = AvisoPerfil.SolicitudEnviada(t.nombre.primerNombre)
        }
    }

    fun reportar(motivo: String) {
        val t = estado.value.tutor ?: return
        viewModelScope.launch {
            reportarTutor(t.id, motivo)
            aviso.value = AvisoPerfil.ReporteEnviado
        }
    }

    fun descartarAviso() {
        aviso.value = null
    }
}
