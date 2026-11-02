package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.Chat
import com.grupo3.nexo.domain.model.Mensaje
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de datos de las conversaciones y los mensajes.
 *
 * Las lecturas (`Flow`) y las escrituras (`suspend`) de una implementación remota deben lanzar una excepción
 * ante fallos de red o del servidor: la UI la captura, muestra el error con "Reintentar" o un aviso breve.
 */
interface ChatRepository {
    fun obtenerChats(): Flow<List<Chat>>
    fun obtenerMensajes(chatId: String): Flow<List<Mensaje>>
    suspend fun enviarMensaje(chatId: String, texto: String)
    suspend fun marcarLeido(chatId: String)
    /** Crea la conversación si todavía no existe (primer mensaje a un tutor). */
    suspend fun asegurarChat(chatId: String, nombre: String, enLinea: Boolean, comoTutor: Boolean)
    /** Deja la conversación activa: la crea si falta y reabre una que estaba en solo lectura (sesión nueva aceptada). */
    suspend fun habilitarChat(chatId: String, nombre: String, enLinea: Boolean, comoTutor: Boolean)
    /** Pasa la conversación a solo lectura cuando ya no quedan sesiones por venir con esa persona. */
    suspend fun cerrarChat(chatId: String)
}
