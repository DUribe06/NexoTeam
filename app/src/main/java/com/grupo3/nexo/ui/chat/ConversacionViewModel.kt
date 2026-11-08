package com.grupo3.nexo.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.Mensaje
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.usecase.AceptarAcuerdoUseCase
import com.grupo3.nexo.domain.usecase.CalificarSesionUseCase
import com.grupo3.nexo.domain.usecase.EnviarMensajeUseCase
import com.grupo3.nexo.domain.usecase.MarcarChatLeidoUseCase
import com.grupo3.nexo.domain.usecase.ObtenerChatsUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMensajesUseCase
import com.grupo3.nexo.domain.usecase.ObtenerSesionDeConversacionUseCase
import com.grupo3.nexo.domain.usecase.ObtenerSesionesDelModoUseCase
import com.grupo3.nexo.domain.usecase.ObtenerTutoresUseCase
import com.grupo3.nexo.domain.usecase.ProponerAcuerdoUseCase
import com.grupo3.nexo.domain.usecase.RechazarAcuerdoUseCase
import com.grupo3.nexo.ui.AccionesEnCurso
import com.grupo3.nexo.ui.Recarga
import com.grupo3.nexo.ui.falloDe
import com.grupo3.nexo.ui.intentar
import com.grupo3.nexo.ui.navigation.Rutas
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConversacionViewModel @Inject constructor(
    obtenerChats: ObtenerChatsUseCase,
    obtenerMensajes: ObtenerMensajesUseCase,
    obtenerTutores: ObtenerTutoresUseCase,
    obtenerSesiones: ObtenerSesionesDelModoUseCase,
    private val calificarSesion: CalificarSesionUseCase,
    private val sesionDeConversacion: ObtenerSesionDeConversacionUseCase,
    private val enviarMensaje: EnviarMensajeUseCase,
    private val marcarLeido: MarcarChatLeidoUseCase,
    private val proponerAcuerdo: ProponerAcuerdoUseCase,
    private val aceptarAcuerdo: AceptarAcuerdoUseCase,
    private val rechazarAcuerdo: RechazarAcuerdoUseCase,
    estadoGuardado: SavedStateHandle
) : ViewModel() {

    data class Estado(
        val cargando: Boolean = true,
        val nombre: String = "",
        val enLinea: Boolean = false,
        val soloLectura: Boolean = false,
        val mensajes: List<Mensaje> = emptyList(),
        /** Sesión de la conversación: la próxima si hay una agendada; si no, la más reciente (RF8, RF9). */
        val sesion: Sesion? = null,
        val esTutor: Boolean = false,
        /** Falló la lectura: la pantalla muestra el error con "Reintentar". */
        val error: Boolean = false
    )

    private val chatId = MutableStateFlow(estadoGuardado.get<String>(Rutas.Conversacion.ARG_CHAT).orEmpty())

    private val recarga = Recarga()

    fun reintentar() = recarga.reintentar()

    val estado = recarga.proteger(alCargar = { Estado() }, alFallar = { Estado(cargando = false, error = true) }) {
        combine(
            chatId,
            chatId.flatMapLatest { obtenerMensajes(it) },
            obtenerChats(),
            obtenerTutores(),
            obtenerSesiones()
        ) { id, mensajes, chats, tutores, sesiones ->
            val chat = chats.firstOrNull { it.id == id }
            // Toda navegación llega con el id real del chat (el repositorio lo crea antes de navegar)
            val contraparteId = chat?.contraparteId
            val tutor = tutores.firstOrNull { it.id == contraparteId }
            val sesion = contraparteId?.let { sesionDeConversacion(sesiones, it, chat.soloLectura) }
            Estado(
                cargando = id.isEmpty(),
                nombre = chat?.nombre ?: tutor?.nombre.orEmpty(),
                enLinea = chat?.enLinea ?: tutor?.enLinea ?: false,
                soloLectura = chat?.soloLectura ?: false,
                mensajes = mensajes,
                sesion = sesion,
                esTutor = chat?.comoTutor ?: false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    init {
        // Marcar como leído es secundario: si falla no se avisa, se reintenta al volver a abrir el chat
        chatId.value.takeIf { it.isNotEmpty() }?.let { id -> viewModelScope.launch { intentar { marcarLeido(id) } } }
    }

    /** Avisa con `alTerminar(null)` si se guardó y `alTerminar(fallo)` si no. */
    private val enCurso = AccionesEnCurso()

    fun calificar(sesionId: String, estrellas: Int, comentario: String?, alTerminar: (Throwable?) -> Unit) {
        enCurso.lanzar(viewModelScope, "calificar:$sesionId") { alTerminar(falloDe { calificarSesion(sesionId, estrellas, comentario) }) }
    }

    /** Un retardo de `minutos` sobre la hora original, o cancelar con null. */
    fun proponer(sesionId: String, minutos: Int?, alTerminar: (Throwable?) -> Unit) {
        enCurso.lanzar(viewModelScope, "acuerdo:$sesionId") { alTerminar(falloDe { proponerAcuerdo(sesionId, minutos) }) }
    }

    fun aceptar(sesionId: String, alTerminar: (Throwable?) -> Unit) {
        enCurso.lanzar(viewModelScope, "acuerdo:$sesionId") { alTerminar(falloDe { aceptarAcuerdo(sesionId) }) }
    }

    /** La otra persona la rechaza; quien la propuso la retira. */
    fun rechazar(sesionId: String, alTerminar: (Throwable?) -> Unit) {
        enCurso.lanzar(viewModelScope, "acuerdo:$sesionId") { alTerminar(falloDe { rechazarAcuerdo(sesionId) }) }
    }

    /** Si el mensaje no sale, `alTerminar(fallo)` permite devolver el texto a la caja para reintentar. */
    fun enviar(texto: String, alTerminar: (Throwable?) -> Unit) {
        val limpio = texto.trim()
        if (limpio.isEmpty() || chatId.value.isEmpty() || estado.value.soloLectura) return
        // Solo se ignora el mismo texto mientras sale; otro mensaje distinto sigue su curso
        enCurso.lanzar(viewModelScope, "enviar:$limpio") { alTerminar(falloDe { enviarMensaje(chatId.value, limpio) }) }
    }
}
