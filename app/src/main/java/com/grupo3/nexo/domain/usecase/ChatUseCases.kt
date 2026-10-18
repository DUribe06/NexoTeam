package com.grupo3.nexo.domain.usecase

import com.grupo3.nexo.domain.model.Chat
import com.grupo3.nexo.domain.model.Mensaje
import com.grupo3.nexo.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.repository.AuthRepository
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Devuelve la bandeja de conversaciones. */
class ObtenerChatsUseCase @Inject constructor(
    private val repositorio: ChatRepository,
    private val auth: AuthRepository
) {
    /** Solo las conversaciones del modo actual: con tutores o con estudiantes. */
    operator fun invoke(): Flow<List<Chat>> =
        combine(repositorio.obtenerChats(), auth.usuarioActual) { chats, usuario ->
            chats.filter { it.comoTutor == (usuario.rol == Rol.TUTOR) }
        }
}

/** Devuelve los mensajes de una conversación. */
class ObtenerMensajesUseCase @Inject constructor(
    private val repositorio: ChatRepository
) {
    operator fun invoke(chatId: String): Flow<List<Mensaje>> = repositorio.obtenerMensajes(chatId)
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
    suspend operator fun invoke(tutor: com.grupo3.nexo.domain.model.Tutor) =
        repositorio.asegurarChat(tutor.id, tutor.nombre, tutor.colorPrimario, tutor.colorSecundario, tutor.enLinea, comoTutor = false)

    /** Conversación del tutor con un estudiante de una solicitud aceptada. */
    suspend fun conEstudiante(id: String, nombre: String) =
        repositorio.asegurarChat(id, nombre, 0xFF2F5D7C, 0xFF6A97B5, enLinea = false, comoTutor = true)
}
