package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.Chat
import com.grupo3.nexo.domain.model.Mensaje
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun obtenerChats(): Flow<List<Chat>>
    fun obtenerMensajes(chatId: String): Flow<List<Mensaje>>
    suspend fun enviarMensaje(chatId: String, texto: String)
    suspend fun marcarLeido(chatId: String)
    /** Crea la conversación si todavía no existe (primer mensaje a un tutor). */
    suspend fun asegurarChat(chatId: String, nombre: String, colorPrimario: Long, colorSecundario: Long, enLinea: Boolean, comoTutor: Boolean)
}
