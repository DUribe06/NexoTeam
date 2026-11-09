package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.remote.ErroresApi
import com.grupo3.nexo.data.remote.NexoApi
import com.grupo3.nexo.data.remote.RolDto
import com.grupo3.nexo.data.remote.SolicitudMensaje
import com.grupo3.nexo.data.remote.aDominio
import com.grupo3.nexo.data.remote.aDto
import com.grupo3.nexo.domain.model.Chat
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.Mensaje
import com.grupo3.nexo.domain.repository.ChatRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private const val INTERVALO_SONDEO_MS = 5_000L

/** Mensajes por petición; es el máximo del contrato. */
private const val LIMITE_MENSAJES = 100

/**
 * Chats contra /api/chats.
 * Mientras alguien observa la bandeja o una conversación se consulta al servidor cada [intervaloSondeoMs]; un fallo de
 * esas consultas no se muestra (lo que ya se ve sigue valiendo) y se reintenta en la siguiente.
 */
@Singleton
class ChatRepositoryRemoto internal constructor(
    private val api: NexoApi,
    private val errores: ErroresApi,
    private val contexto: ContextoRemoto,
    private val intervaloSondeoMs: Long
) : ChatRepository {

    @Inject
    constructor(api: NexoApi, errores: ErroresApi, contexto: ContextoRemoto) :
        this(api, errores, contexto, INTERVALO_SONDEO_MS)

    /**
     * [cursor] es el último mensaje recibido al consultar, no al enviar: si se avanzara con un envío propio, lo que la
     * otra persona escribió justo antes nunca llegaría.
     */
    private data class Hilo(val mensajes: List<Mensaje>, val cursor: String?)

    private val sondeos = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val chats = EstadoRemoto<List<Chat>>(contexto) { clave, _ ->
        errores.llamar { api.chats(clave.modo.aDto()) }.map { it.aDominio() }
    }.also { it.sondear(sondeos, intervaloSondeoMs) }

    private val hilos = ConcurrentHashMap<String, EstadoRemoto<Hilo>>()

    /** idCliente de cada envío que no se ha confirmado: reintentar el mismo texto reutiliza el suyo y no lo duplica. */
    private val sinConfirmar = ConcurrentHashMap<Pair<String, String>, String>()

    init {
        contexto.alCambiar { sinConfirmar.clear() }
    }

    override fun obtenerChats(): Flow<List<Chat>> = chats.flujo()

    override fun obtenerMensajes(chatId: String): Flow<List<Mensaje>> =
        if (chatId.isEmpty()) flowOf(emptyList())
        else hiloDe(chatId).flujo().map { it.mensajes }.distinctUntilChanged()

    override suspend fun enviarMensaje(chatId: String, texto: String, imagenUrl: String?) {
        val envio = chatId to (texto + imagenUrl.orEmpty())
        val idCliente = sinConfirmar.computeIfAbsent(envio) { UUID.randomUUID().toString() }
        val enviado = try {
            errores.llamar { api.enviarMensaje(chatId, SolicitudMensaje(texto, idCliente, imagenUrl)) }.aDominio()
        } catch (e: ErrorApi) {
            // La sesión terminó mientras se escribía: la pantalla debe pasar a solo lectura
            if (e.codigo == CodigoError.CHAT_SOLO_LECTURA) chats.refrescar()
            throw e
        }
        sinConfirmar.remove(envio)
        hiloDe(chatId).actualizar { it.copy(mensajes = it.mensajes.conNuevos(listOf(enviado))) }
        chats.actualizar { lista ->
            lista.map { if (it.id == chatId) it.copy(ultimoMensaje = enviado.texto.ifEmpty { "Foto" }, hora = enviado.hora) else it }
        }
        chats.refrescar()
    }

    override suspend fun marcarLeido(chatId: String) {
        errores.sinCuerpo { api.marcarChatLeido(chatId) }
        chats.actualizar { lista -> lista.map { if (it.id == chatId) it.copy(noLeidos = 0) else it } }
    }

    /** El servidor no crea chats aquí, solo los encuentra: nacen al aceptar una solicitud (`CHAT_NO_HABILITADO`). */
    override suspend fun asegurarChat(personaId: String, nombre: String, enLinea: Boolean, comoTutor: Boolean): String {
        val modo = if (comoTutor) RolDto.TUTOR else RolDto.ESTUDIANTE
        val chat = errores.llamar { api.chatCon(personaId, modo) }.aDominio()
        chats.actualizar { lista ->
            if (lista.any { it.id == chat.id }) lista.map { if (it.id == chat.id) chat else it } else listOf(chat) + lista
        }
        return chat.id
    }

    /** No-op: el servidor crea o reabre el chat al aceptar la solicitud. */
    override suspend fun habilitarChat(personaId: String, nombre: String, enLinea: Boolean, comoTutor: Boolean) = Unit

    /** No-op: el servidor pasa el chat a solo lectura al cancelar, si no quedan sesiones por venir. */
    override suspend fun cerrarChat(personaId: String) = Unit

    internal suspend fun refrescarChats() = chats.refrescar()

    private fun hiloDe(chatId: String): EstadoRemoto<Hilo> = hilos.computeIfAbsent(chatId) {
        EstadoRemoto<Hilo>(contexto) { _, actual ->
            if (actual == null) historial(chatId) else conLoNuevo(chatId, actual)
        }.also { it.sondear(sondeos, intervaloSondeoMs) }
    }

    /** Sin cursor llegan los últimos; la pantalla no pagina, así que se recorre todo hacia atrás. */
    private suspend fun historial(chatId: String): Hilo {
        var pagina = errores.llamar { api.mensajes(chatId, limite = LIMITE_MENSAJES) }
        val cursor = pagina.mensajes.lastOrNull()?.id
        var mensajes = pagina.mensajes
        while (pagina.hayMas && pagina.mensajes.isNotEmpty()) {
            val anterioresA = pagina.mensajes.first().id
            pagina = errores.llamar { api.mensajes(chatId, antes = anterioresA, limite = LIMITE_MENSAJES) }
            mensajes = pagina.mensajes + mensajes
        }
        return Hilo(mensajes.map { it.aDominio() }, cursor)
    }

    private suspend fun conLoNuevo(chatId: String, actual: Hilo): Hilo {
        var cursor = actual.cursor
        val nuevos = mutableListOf<Mensaje>()
        do {
            val pagina = errores.llamar { api.mensajes(chatId, desde = cursor, limite = LIMITE_MENSAJES) }
            nuevos += pagina.mensajes.map { it.aDominio() }
            cursor = pagina.mensajes.lastOrNull()?.id ?: cursor
            // Sin cursor la página trae los últimos y `hayMas` habla de los anteriores: no hay nada más que pedir
        } while (pagina.hayMas && actual.cursor != null && pagina.mensajes.isNotEmpty())
        // Consultar los mensajes solo ocurre con la conversación abierta: lo que llega ya se está leyendo
        if (nuevos.any { !it.mio }) marcarLeidoSinFallar(chatId)
        return Hilo(actual.mensajes.conNuevos(nuevos), cursor)
    }

    private suspend fun marcarLeidoSinFallar(chatId: String) {
        try {
            marcarLeido(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Secundario: se reintenta con el siguiente mensaje o al volver a abrir el chat
        }
    }

    /** Un mensaje puede llegar por el envío y otra vez por la consulta: cuenta una sola vez. */
    private fun List<Mensaje>.conNuevos(nuevos: List<Mensaje>): List<Mensaje> = (this + nuevos).distinctBy { it.id }
}
