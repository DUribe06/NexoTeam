package com.grupo3.nexo.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.DiaSemana
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.model.contarPendientes
import com.grupo3.nexo.domain.model.diasPublicados
import com.grupo3.nexo.domain.model.horasPublicadas
import com.grupo3.nexo.domain.usecase.ObtenerDisponibilidadUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMisSolicitudesUseCase
import com.grupo3.nexo.domain.usecase.ObtenerProximasSesionesUseCase
import com.grupo3.nexo.domain.usecase.ObtenerTutoresUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class InicioViewModel @Inject constructor(
    obtenerTutores: ObtenerTutoresUseCase,
    obtenerProximas: ObtenerProximasSesionesUseCase,
    obtenerUsuario: ObtenerUsuarioUseCase,
    obtenerMisSolicitudes: ObtenerMisSolicitudesUseCase,
    obtenerDisponibilidad: ObtenerDisponibilidadUseCase
) : ViewModel() {

    /** Sesión próxima resuelta con el id del tutor para abrir su chat. */
    data class SesionProxima(val sesion: Sesion, val tutorId: String?, val tutor: Tutor? = null)

    data class Estado(
        val cargando: Boolean = true,
        val usuario: Usuario? = null,
        val proximas: List<SesionProxima> = emptyList(),
        /** Modo tutor: solicitudes recibidas sin responder. Modo estudiante: enviadas sin respuesta. */
        val pendientes: Int = 0,
        /** Modo tutor: horas publicadas por semana y días con al menos una (RF7). */
        val horasPublicadas: Int = 0,
        val diasPublicados: List<DiaSemana> = emptyList(),
        /** Modo tutor: tema que pidió el estudiante para la próxima sesión (RF5). */
        val temaProximo: String? = null
    ) {
        val esTutor get() = usuario?.rol == Rol.TUTOR
    }

    val estado = combine(
        obtenerTutores(),
        // Las sesiones llegan del servidor: mientras tanto se emite null y la tarjeta muestra su esqueleto,
        // sin retener el saludo ni la insignia de pendientes, que ya están disponibles
        obtenerProximas().map<List<Sesion>, List<Sesion>?> { it }.onStart { emit(null) },
        obtenerUsuario(),
        obtenerMisSolicitudes(),
        obtenerDisponibilidad()
    ) { tutores, proximas, usuario, solicitudes, dias ->
        Estado(
            horasPublicadas = dias.horasPublicadas,
            diasPublicados = dias.diasPublicados,
            cargando = proximas == null,
            usuario = usuario,
            proximas = proximas.orEmpty().map { sesion ->
                SesionProxima(sesion, sesion.contraparteId, tutores.find { !sesion.comoTutor && it.id == sesion.contraparteId })
            },
            pendientes = solicitudes.contarPendientes(),
            temaProximo = proximas?.firstOrNull()?.solicitudId?.let { id -> solicitudes.find { it.id == id }?.tema },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())
}
