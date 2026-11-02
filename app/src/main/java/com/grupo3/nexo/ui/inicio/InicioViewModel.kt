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
import com.grupo3.nexo.domain.usecase.AbrirChatDeSesionUseCase
import com.grupo3.nexo.domain.usecase.ObtenerDisponibilidadUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMisSolicitudesUseCase
import com.grupo3.nexo.domain.usecase.ObtenerProximasSesionesUseCase
import com.grupo3.nexo.domain.usecase.ObtenerTutoresUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import com.grupo3.nexo.ui.Recarga
import com.grupo3.nexo.ui.intentar
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class InicioViewModel @Inject constructor(
    obtenerTutores: ObtenerTutoresUseCase,
    obtenerProximas: ObtenerProximasSesionesUseCase,
    obtenerUsuario: ObtenerUsuarioUseCase,
    obtenerMisSolicitudes: ObtenerMisSolicitudesUseCase,
    obtenerDisponibilidad: ObtenerDisponibilidadUseCase,
    private val abrirChat: AbrirChatDeSesionUseCase
) : ViewModel() {

    /** Sesión próxima con el perfil del tutor, si el usuario es el estudiante. */
    data class SesionProxima(val sesion: Sesion, val tutor: Tutor? = null)

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
        val temaProximo: String? = null,
        /** Días que faltan para la próxima sesión (0 = hoy); negativo si ya pasó la fecha. Null sin sesiones. */
        val diasParaProxima: Long? = null,
        /** Falló la lectura: la pantalla muestra el error con "Reintentar". */
        val error: Boolean = false
    ) {
        val esTutor get() = usuario?.rol == Rol.TUTOR
    }

    private val recarga = Recarga()
    /** El usuario no depende del servidor de sesiones: si esa lectura falla, el saludo y el avatar se conservan. */
    private val usuario = obtenerUsuario()

    fun reintentar() = recarga.reintentar()

    /** Resuelve el chat con la contraparte de la sesión (lo crea si falta) y, si falla, `alFallar` muestra el aviso. */
    fun abrirChat(sesion: Sesion, alListo: (String) -> Unit, alFallar: () -> Unit) {
        viewModelScope.launch {
            var id: String? = null
            if (intentar { id = abrirChat(sesion) }) id?.let(alListo) else alFallar()
        }
    }

    val estado = recarga.proteger(alCargar = { Estado(usuario = usuario.value) }, alFallar = { Estado(cargando = false, usuario = usuario.value, error = true) }) {
        combine(
            obtenerTutores(),
            // Las sesiones llegan del servidor: mientras tanto se emite null y la tarjeta muestra su esqueleto,
            // sin retener el saludo ni la insignia de pendientes, que ya están disponibles
            obtenerProximas().map<List<Sesion>, List<Sesion>?> { it }.onStart { emit(null) },
            usuario,
            obtenerMisSolicitudes(),
            obtenerDisponibilidad()
        ) { tutores, proximas, usuario, solicitudes, dias ->
            Estado(
                horasPublicadas = dias.horasPublicadas,
                diasPublicados = dias.diasPublicados,
                cargando = proximas == null,
                usuario = usuario,
                proximas = proximas.orEmpty().map { sesion ->
                    SesionProxima(sesion, tutores.find { !sesion.comoTutor && it.id == sesion.contraparteId })
                },
                pendientes = solicitudes.contarPendientes(),
                temaProximo = proximas?.firstOrNull()?.solicitudId?.let { id -> solicitudes.find { it.id == id }?.tema },
                diasParaProxima = proximas?.firstOrNull()?.let { ChronoUnit.DAYS.between(LocalDate.now(), it.fecha) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())
}
