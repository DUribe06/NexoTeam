package com.grupo3.nexo.ui.inicio

import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.Solicitud
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.DiaSemana
import com.grupo3.nexo.domain.model.ResumenActividad
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.model.contarPendientes
import com.grupo3.nexo.domain.model.diasPublicados
import com.grupo3.nexo.domain.model.horasPublicadas
import com.grupo3.nexo.domain.usecase.AbrirChatDeSesionUseCase
import com.grupo3.nexo.domain.usecase.EnviarPinUseCase
import com.grupo3.nexo.domain.usecase.IniciarSesionTutoriaUseCase
import com.grupo3.nexo.domain.usecase.ObtenerDisponibilidadUseCase
import com.grupo3.nexo.domain.usecase.ObtenerHistorialUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMisSolicitudesUseCase
import com.grupo3.nexo.domain.usecase.ObtenerProximasSesionesUseCase
import com.grupo3.nexo.domain.usecase.ObtenerResumenActividadUseCase
import com.grupo3.nexo.domain.usecase.ObtenerTutoresUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import com.grupo3.nexo.ui.AccionesEnCurso
import com.grupo3.nexo.ui.Recarga
import com.grupo3.nexo.ui.falloDe
import com.grupo3.nexo.ui.resultadoDe
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InicioViewModel @Inject constructor(
    obtenerTutores: ObtenerTutoresUseCase,
    obtenerProximas: ObtenerProximasSesionesUseCase,
    obtenerHistorial: ObtenerHistorialUseCase,
    obtenerUsuario: ObtenerUsuarioUseCase,
    obtenerMisSolicitudes: ObtenerMisSolicitudesUseCase,
    obtenerDisponibilidad: ObtenerDisponibilidadUseCase,
    obtenerResumen: ObtenerResumenActividadUseCase,
    private val abrirChat: AbrirChatDeSesionUseCase,
    private val enviarPinDeSesion: EnviarPinUseCase,
    private val iniciarSesion: IniciarSesionTutoriaUseCase
) : ViewModel() {

    /**
     * Sesión próxima con el perfil del tutor, si el usuario es el estudiante.
     * `tema` sale de la solicitud de esa misma sesión: cada tarjeta del mazo lleva el suyo.
     */
    data class SesionProxima(val sesion: Sesion, val tutor: Tutor? = null, val tema: String? = null)

    data class Estado(
        val cargando: Boolean = true,
        val usuario: Usuario? = null,
        val proximas: List<SesionProxima> = emptyList(),
        /** Sesiones que ya no están programadas: de aquí sale si hay una por calificar. */
        val historial: List<Sesion> = emptyList(),
        /** Modo tutor: solicitudes recibidas sin responder. Modo estudiante: enviadas sin respuesta. */
        val pendientes: Int = 0,
        /** Modo tutor: horas publicadas por semana y días con al menos una (RF7). */
        val horasPublicadas: Int = 0,
        val diasPublicados: List<DiaSemana> = emptyList(),
        /** Modo tutor: cifras de los accesos. Null mientras llegan o si fallaron: un acceso no tumba el Inicio. */
        val resumen: ResumenActividad? = null,
        /** Modo estudiante: solicitudes rechazadas cuya fecha aún no pasa, para ofrecer otros tutores. */
        val rechazadas: List<Solicitud> = emptyList(),
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
    fun abrirChat(sesion: Sesion, alListo: (String) -> Unit, alFallar: (Throwable) -> Unit) {
        viewModelScope.launch {
            resultadoDe { abrirChat(sesion) }.onSuccess(alListo).onFailure(alFallar)
        }
    }

    private val enCurso = AccionesEnCurso()

    /** `alTerminar(null)` si salió bien. Tocar dos veces la barra no lo envía dos veces. */
    fun enviarPin(sesion: Sesion, alTerminar: (Throwable?) -> Unit) {
        enCurso.lanzar(viewModelScope, "pin:${sesion.id}") { alTerminar(falloDe { enviarPinDeSesion(sesion.id) }) }
    }

    fun iniciar(sesion: Sesion, pin: String, alTerminar: (Throwable?) -> Unit) {
        enCurso.lanzar(viewModelScope, "iniciar:${sesion.id}") { alTerminar(falloDe { iniciarSesion(sesion.id, pin) }) }
    }

    val estado = recarga.proteger(alCargar = { Estado(usuario = usuario.value) }, alFallar = { Estado(cargando = false, usuario = usuario.value, error = true) }) {
        combine(
            obtenerTutores(),
            // Las sesiones llegan del servidor: mientras tanto se emite null y la tarjeta muestra su esqueleto,
            // sin retener el saludo ni la insignia de pendientes, que ya están disponibles
            combine(obtenerProximas(), obtenerHistorial()) { proximas, historial -> proximas to historial }
                .map<Pair<List<Sesion>, List<Sesion>>, Pair<List<Sesion>, List<Sesion>>?> { it }
                .onStart { emit(null) },
            usuario,
            obtenerMisSolicitudes(),
            combine(
                obtenerDisponibilidad(),
                usuario.map { it.rol == Rol.TUTOR }.distinctUntilChanged().flatMapLatest { esTutor ->
                    if (!esTutor) flowOf(null)
                    else obtenerResumen().map<ResumenActividad, ResumenActividad?> { it }.onStart { emit(null) }.catch { emit(null) }
                }
            ) { dias, resumen -> dias to resumen }
        ) { tutores, sesiones, usuario, solicitudes, (dias, resumen) ->
            val proximas = sesiones?.first
            Estado(
                resumen = resumen,
                historial = sesiones?.second.orEmpty(),
                horasPublicadas = dias.horasPublicadas,
                diasPublicados = dias.diasPublicados,
                cargando = proximas == null,
                usuario = usuario,
                proximas = proximas.orEmpty().map { sesion ->
                    SesionProxima(
                        sesion,
                        tutores.find { !sesion.comoTutor && it.id == sesion.contraparteId },
                        sesion.solicitudId?.let { id -> solicitudes.find { it.id == id }?.tema }
                    )
                },
                pendientes = solicitudes.contarPendientes(),
                rechazadas = if (usuario.rol == Rol.TUTOR) emptyList() else solicitudes
                    .filter { it.estado == EstadoSolicitud.RECHAZADA && it.estudianteId == usuario.id && !it.fecha.isBefore(LocalDate.now()) }
                    .sortedByDescending { it.enviada },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())
}
