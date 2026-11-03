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
    /**
     * Devuelve el id del chat con esa persona y lo crea si todavía no existe (primer mensaje a un tutor).
     * El servidor no lo crea: si no hay una solicitud aceptada, lanza `ErrorApi` con `CHAT_NO_HABILITADO`.
     */
    suspend fun asegurarChat(personaId: String, nombre: String, enLinea: Boolean, comoTutor: Boolean): String
    /** Deja activo el chat con esa persona: lo crea si falta y reabre uno en solo lectura (sesión nueva aceptada). Con el servidor ya ocurrió al aceptar. */
    suspend fun habilitarChat(personaId: String, nombre: String, enLinea: Boolean, comoTutor: Boolean)
    /** Pasa el chat con esa persona a solo lectura cuando ya no quedan sesiones por venir con ella. Con el servidor ya ocurrió al cancelar. */
    suspend fun cerrarChat(personaId: String)
}
