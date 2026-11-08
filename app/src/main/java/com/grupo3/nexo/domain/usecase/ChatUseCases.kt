package com.grupo3.nexo.domain.usecase

import com.grupo3.nexo.domain.model.Chat
import com.grupo3.nexo.domain.model.Mensaje
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.repository.AuthRepository
import com.grupo3.nexo.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Devuelve la bandeja de conversaciones. */
class ObtenerChatsUseCase @Inject constructor(
    private val repositorio: ChatRepository,
    private val auth: AuthRepository
) {
    /** Solo las conversaciones del modo actual: con tutores o con estudiantes. */
    operator fun invoke(): Flow<List<Chat>> =
        combine(repositorio.obtenerChats(), auth.usuarioActual) { chats, usuario ->
            chats.filter { it.comoTutor == (usuario.rol == Rol.TUTOR) }
                // Los chats nuevos, aún sin mensajes, arriba; después el último mensaje más reciente primero
                .sortedWith(compareByDescending<Chat> { it.hora == null }.thenByDescending { it.hora })
        }
}

/** Devuelve los mensajes de una conversación. */
class ObtenerMensajesUseCase @Inject constructor(
    private val repositorio: ChatRepository
) {
    /** Del más antiguo al más reciente. */
    operator fun invoke(chatId: String): Flow<List<Mensaje>> =
        repositorio.obtenerMensajes(chatId).map { lista -> lista.sortedBy { it.hora } }
}

/** Envía un mensaje en una conversación. */
class EnviarMensajeUseCase @Inject constructor(
    private val repositorio: ChatRepository
) {
    suspend operator fun invoke(chatId: String, texto: String) =
        repositorio.enviarMensaje(chatId, texto)
}

/** Marca la conversación como leída al abrirla. */
class MarcarChatLeidoUseCase @Inject constructor(
    private val repositorio: ChatRepository
) {
    suspend operator fun invoke(chatId: String) = repositorio.marcarLeido(chatId)
}

/** Garantiza que exista la conversación con un tutor antes de abrirla. */
class AbrirChatUseCase @Inject constructor(
    private val repositorio: ChatRepository
) {
    /** Devuelve el id del chat con el tutor. */
    suspend operator fun invoke(tutor: com.grupo3.nexo.domain.model.Tutor): String =
        repositorio.asegurarChat(tutor.id, tutor.nombre, tutor.enLinea, comoTutor = false)

    /** Conversación del tutor con un estudiante de una solicitud aceptada. */
    suspend fun conEstudiante(id: String, nombre: String): String =
        repositorio.asegurarChat(id, nombre, enLinea = false, comoTutor = true)
}

/**
 * Abre el chat de una solicitud aceptada (RF8): el tutor lo abre con el estudiante y el estudiante con el tutor.
 * Devuelve el id de la conversación, o null si el tutor ya no existe.
 */
class AbrirChatDeSolicitudUseCase @Inject constructor(
    private val abrirChat: AbrirChatUseCase,
    private val auth: AuthRepository,
    private val tutor: ObtenerTutorUseCase
) {
    suspend operator fun invoke(solicitud: Solicitud): String? =
        if (auth.usuarioActual.value.rol == Rol.TUTOR) {
            abrirChat.conEstudiante(solicitud.estudianteId, solicitud.nombreEstudiante)
        } else {
            // Por id y no en el catálogo: un tutor sin horas publicadas ya no aparece en la búsqueda, pero el chat sigue
            tutor(solicitud.tutorId).first()?.let { abrirChat(it) }
        }
}

/** Abre el chat con la contraparte de una sesión próxima y devuelve el id de la conversación. */
class AbrirChatDeSesionUseCase @Inject constructor(
    private val repositorio: ChatRepository
) {
    suspend operator fun invoke(sesion: Sesion): String =
        repositorio.asegurarChat(sesion.contraparteId, sesion.conQuien, enLinea = false, comoTutor = sesion.comoTutor)
}
