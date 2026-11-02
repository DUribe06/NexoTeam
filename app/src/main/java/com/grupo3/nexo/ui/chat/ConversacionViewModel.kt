package com.grupo3.nexo.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.Mensaje
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.usecase.CalificarSesionUseCase
import com.grupo3.nexo.domain.usecase.EnviarMensajeUseCase
import com.grupo3.nexo.domain.usecase.MarcarChatLeidoUseCase
import com.grupo3.nexo.domain.usecase.ObtenerChatsUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMensajesUseCase
import com.grupo3.nexo.domain.usecase.ObtenerSesionDeConversacionUseCase
import com.grupo3.nexo.domain.usecase.ObtenerSesionesDelModoUseCase
import com.grupo3.nexo.domain.usecase.ObtenerTutoresUseCase
import com.grupo3.nexo.ui.Recarga
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
            val tutor = tutores.firstOrNull { it.id == id }
            val sesion = sesionDeConversacion(sesiones, id, chat?.soloLectura ?: false)
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

    /** Avisa con `alTerminar(true)` si se guardó y `alTerminar(false)` si falló. */
    fun calificar(sesionId: String, estrellas: Int, comentario: String?, alTerminar: (Boolean) -> Unit) {
        viewModelScope.launch { alTerminar(intentar { calificarSesion(sesionId, estrellas, comentario) }) }
    }

    /** Si el mensaje no sale, `alTerminar(false)` permite devolver el texto a la caja para reintentar. */
    fun enviar(texto: String, alTerminar: (Boolean) -> Unit) {
        val limpio = texto.trim()
        if (limpio.isEmpty() || chatId.value.isEmpty() || estado.value.soloLectura) return
        viewModelScope.launch { alTerminar(intentar { enviarMensaje(chatId.value, limpio) }) }
    }
}
