package com.grupo3.nexo.data.remote

import kotlinx.serialization.Serializable

/* Esquemas de /api/chats. */

@Serializable
data class ChatDto(
    val id: String,
    val contraparteId: String,
    val nombre: String,
    val ultimoMensaje: String,
    val ultimoMensajeEn: String? = null,
    val noLeidos: Int,
    val enLinea: Boolean,
    val soloLectura: Boolean,
    val comoTutor: Boolean,
    val fotoUrl: String? = null
)

@Serializable
data class MensajeDto(
    val id: String,
    val chatId: String,
    val texto: String,
    val imagenUrl: String? = null,
    val enviadoEn: String,
    val mio: Boolean
)

@Serializable
data class PaginaMensajesDto(val mensajes: List<MensajeDto>, val hayMas: Boolean)

@Serializable
data class SolicitudMensaje(val texto: String, val idCliente: String, val imagenUrl: String? = null)
