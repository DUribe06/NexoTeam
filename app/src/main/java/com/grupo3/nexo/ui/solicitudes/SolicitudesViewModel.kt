package com.grupo3.nexo.ui.solicitudes

import com.grupo3.nexo.ui.AccionesEnCurso
import androidx.compose.foundation.layout.size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.model.conPlazoAplicado
import com.grupo3.nexo.domain.model.contarPendientes
import com.grupo3.nexo.domain.model.horasRestantes
import com.grupo3.nexo.domain.usecase.AbrirChatDeSolicitudUseCase
import com.grupo3.nexo.domain.usecase.ActualizarSolicitudUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMisSolicitudesUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import com.grupo3.nexo.ui.Recarga
import com.grupo3.nexo.ui.falloDe
import com.grupo3.nexo.ui.resultadoDe
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.floor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SolicitudesViewModel @Inject constructor(
    obtenerMisSolicitudes: ObtenerMisSolicitudesUseCase,
    obtenerUsuario: ObtenerUsuarioUseCase,
    private val actualizarSolicitud: ActualizarSolicitudUseCase,
    private val abrirChat: AbrirChatDeSolicitudUseCase
) : ViewModel() {

    data class Estado(
        val cargando: Boolean = true,
        val esTutor: Boolean = false,
        val filtro: EstadoSolicitud? = null,
        val solicitudes: List<Solicitud> = emptyList(),
        val conteos: Map<EstadoSolicitud, Int> = emptyMap(),
        val total: Int = 0,
        val pendientes: Int = 0,
        /** Horas que quedan de plazo por id de solicitud; se recalculan cada minuto. */
        val horasRestantes: Map<String, Double> = emptyMap(),
        /** Las mismas horas redondeadas hacia abajo, tal como se muestran en el aviso. */
        val horasEnteras: Map<String, Double> = emptyMap(),
        /** Falló la lectura: la pantalla muestra el error con "Reintentar". */
        val error: Boolean = false
    )

    private val recarga = Recarga()

    fun reintentar() = recarga.reintentar()

    /** El filtro recuerda el modo en que se eligió: al cambiar de estudiante a tutor se descarta. */
    private val filtro = MutableStateFlow<Pair<Rol, EstadoSolicitud>?>(null)

    /** Reloj que avanza cada minuto mientras la pantalla está a la vista (deja de correr sin suscriptores). */
    private val ahora = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000)
        }
    }

    val estado = recarga.proteger(alCargar = { Estado() }, alFallar = { Estado(cargando = false, error = true) }) {
        combine(obtenerMisSolicitudes(), obtenerUsuario(), filtro, ahora) { recibidas, usuario, elegido, reloj ->
            val propias = recibidas.map { it.conPlazoAplicado(reloj) }
            // Si el estado elegido se queda sin solicitudes (p. ej. se aceptó la última pendiente), vuelve a "Todas"
            val f = elegido?.takeIf { it.first == usuario.rol }?.second
                ?.takeIf { filtrado -> propias.any { it.estado == filtrado } }
            Estado(
                cargando = false,
                esTutor = usuario.rol == Rol.TUTOR,
                filtro = f,
                solicitudes = propias.filter { f == null || it.estado == f },
                conteos = propias.groupingBy { it.estado }.eachCount(),
                total = propias.size,
                pendientes = propias.contarPendientes(),
                horasRestantes = propias.associate { it.id to it.horasRestantes(reloj) },
                horasEnteras = propias.associate { it.id to floor(it.horasRestantes(reloj)) }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun filtrar(valor: EstadoSolicitud?) {
        val rol = if (estado.value.esTutor) Rol.TUTOR else Rol.ESTUDIANTE
        filtro.value = if (valor == null || estado.value.filtro == valor) null else rol to valor
    }

    /** Las tres acciones avisan con `alTerminar(null)` si se guardó y `alTerminar(fallo)` si no. */
    fun aceptar(id: String, alTerminar: (Throwable?) -> Unit) = cambiar(id, EstadoSolicitud.ACEPTADA, null, alTerminar)
    fun rechazar(id: String, motivo: String?, alTerminar: (Throwable?) -> Unit) = cambiar(id, EstadoSolicitud.RECHAZADA, motivo, alTerminar)
    fun cancelar(id: String, alTerminar: (Throwable?) -> Unit) = cambiar(id, EstadoSolicitud.CANCELADA, null, alTerminar)

    /** El chat existe solo para solicitudes aceptadas (RF8): se abre (o se crea) y, si falla, `alFallar` muestra el aviso. */
    fun abrirConversacion(solicitud: Solicitud, alListo: (String) -> Unit, alFallar: (Throwable) -> Unit) {
        viewModelScope.launch {
            resultadoDe { abrirChat(solicitud) }.onSuccess { id -> id?.let(alListo) }.onFailure(alFallar)
        }
    }

    private val enCurso = AccionesEnCurso()

    private fun cambiar(id: String, nuevo: EstadoSolicitud, motivo: String?, alTerminar: (Throwable?) -> Unit) {
        enCurso.lanzar(viewModelScope, "cambiar:$id") { alTerminar(falloDe { actualizarSolicitud(id, nuevo, motivo) }) }
    }
}
