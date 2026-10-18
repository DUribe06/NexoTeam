package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.mock.MockData
import com.grupo3.nexo.domain.model.Chat
import com.grupo3.nexo.domain.model.Mensaje
import com.grupo3.nexo.domain.repository.ChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor() : ChatRepository {

    private val chats = MutableStateFlow(MockData.chats)
    private val mensajes = MutableStateFlow(MockData.mensajes)
    private var contador = mensajes.value.size

    override fun obtenerChats(): Flow<List<Chat>> = chats.asStateFlow()

    override fun obtenerMensajes(chatId: String): Flow<List<Mensaje>> =
        mensajes.asStateFlow().map { lista -> lista.filter { it.chatId == chatId } }

    override suspend fun enviarMensaje(chatId: String, texto: String) {
        delay(300)
        val hora = "ahora"
        mensajes.update { actual ->
            actual + Mensaje("m${++contador}", chatId, texto, hora, mio = true)
        }
        chats.update { actual ->
            actual.map { if (it.id == chatId) it.copy(ultimoMensaje = texto, hora = hora) else it }
        }
    }

    override suspend fun marcarLeido(chatId: String) {
        chats.update { actual ->
            actual.map { if (it.id == chatId) it.copy(noLeidos = 0) else it }
        }
    }

    override suspend fun asegurarChat(
        chatId: String,
        nombre: String,
        colorPrimario: Long,
        colorSecundario: Long,
        enLinea: Boolean,
        comoTutor: Boolean
    ) {
        chats.update { actual ->
            if (actual.any { it.id == chatId }) actual
            else listOf(
                Chat(chatId, nombre, "Inicia la conversación", "", 0, enLinea, colorPrimario, colorSecundario, comoTutor = comoTutor)
            ) + actual
        }
    }
}
