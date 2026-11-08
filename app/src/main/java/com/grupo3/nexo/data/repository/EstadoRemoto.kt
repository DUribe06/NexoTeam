package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.remote.PaginaDto
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Una suscripción nueva no vuelve a pedir datos más recientes que esto (Inicio y la barra se suscriben a la vez). */
private const val FRESCURA_MS = 3_000L

private const val NANOS_POR_MS = 1_000_000L

/**
 * Datos del servidor en memoria, compartidos por todas las pantallas.
 *
 * Se llena con la primera suscripción; si esa lectura falla la excepción llega a la pantalla, y al reintentar (volver a
 * suscribirse) se pide otra vez. Con datos en memoria, suscribirse los entrega de inmediato y los repasa por detrás, y
 * un fallo de ese repaso o de un [refrescar] no borra lo que ya se ve. Cada dato queda marcado con su [ClaveDatos]: lo
 * que llegue de otra cuenta o de otro modo nunca se muestra.
 *
 * @param traer pide los datos; `actual` es lo que ya hay en memoria, para quien solo pide lo nuevo.
 */
internal class EstadoRemoto<T>(
    private val contexto: ContextoRemoto,
    private val traer: suspend (clave: ClaveDatos, actual: T?) -> T
) {
    private class Guardado<T>(val clave: ClaveDatos, val dato: T, val pedidoEnMs: Long?)

    private val guardado = MutableStateFlow<Guardado<T>?>(null)

    /** Una sola petición a la vez: un cambio local nunca queda pisado por una respuesta que ya venía en camino. */
    private val cerrojo = Mutex()

    init {
        contexto.alCambiar { guardado.value = null }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun flujo(): Flow<T> = contexto.clave.flatMapLatest { clave ->
        if (clave == null) emptyFlow() else lecturas(clave)
    }

    private fun lecturas(clave: ClaveDatos): Flow<T> = flow {
        if (guardado.value?.clave != clave) cargar(clave, forzar = false)
        coroutineScope {
            launch { cargarSinFallar(clave, forzar = false) }
            emitAll(
                guardado.filterNotNull()
                    .filter { it.clave == clave }
                    .map { it.dato }
                    .distinctUntilChanged()
            )
        }
    }

    /** Vuelve a pedir lo que ya está en memoria tras una escritura. Si falla no avisa: la escritura ya se hizo. */
    suspend fun refrescar() {
        val clave = contexto.clave.value ?: return
        if (guardado.value?.clave == clave) cargarSinFallar(clave, forzar = true)
    }

    /** Aplica ya lo que respondió una escritura, sin esperar al refresco. Sin datos en memoria no hay nada que cambiar. */
    suspend fun actualizar(cambio: (T) -> T) = cerrojo.withLock {
        guardado.value?.let { guardado.value = Guardado(it.clave, cambio(it.dato), it.pedidoEnMs) }
    }

    /** Refresca cada [intervaloMs] mientras haya alguien suscrito (chats y mensajes). */
    fun sondear(alcance: CoroutineScope, intervaloMs: Long): Job = alcance.launch {
        guardado.subscriptionCount.map { it > 0 }.distinctUntilChanged().collectLatest { haySuscriptores ->
            while (haySuscriptores) {
                delay(intervaloMs)
                refrescar()
            }
        }
    }

    private suspend fun cargar(clave: ClaveDatos, forzar: Boolean) = cerrojo.withLock {
        val previo = guardado.value?.takeIf { it.clave == clave }
        if (forzar || previo == null || !previo.esFresco()) {
            val dato = traer(clave, previo?.dato)
            // Mientras venía la respuesta pudo cambiar la cuenta o el modo: entonces ya no sirve
            if (contexto.clave.value == clave) guardado.value = Guardado(clave, dato, ahoraMs())
        }
    }

    private suspend fun cargarSinFallar(clave: ClaveDatos, forzar: Boolean) {
        try {
            cargar(clave, forzar)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Se conserva lo que hay, marcado como viejo para que la próxima suscripción lo vuelva a pedir
            guardado.value?.takeIf { it.clave == clave }?.let { guardado.value = Guardado(it.clave, it.dato, null) }
        }
    }

    private fun Guardado<T>.esFresco(): Boolean = pedidoEnMs != null && ahoraMs() - pedidoEnMs < FRESCURA_MS

    private fun ahoraMs(): Long = System.nanoTime() / NANOS_POR_MS
}

/** Un [EstadoRemoto] por id (un tutor, las reseñas de un tutor). Se limpian con el contexto, no se descartan. */
internal class EstadosPorId<T>(
    private val contexto: ContextoRemoto,
    private val traer: suspend (id: String, clave: ClaveDatos, actual: T?) -> T
) {
    private val estados = ConcurrentHashMap<String, EstadoRemoto<T>>()

    fun de(id: String): EstadoRemoto<T> =
        estados.computeIfAbsent(id) { EstadoRemoto(contexto) { clave, actual -> traer(id, clave, actual) } }

    /** Solo el que ya se pidió: lo demás no hace falta refrescarlo. */
    fun yaPedido(id: String): EstadoRemoto<T>? = estados[id]
}

/** Elementos por página al recorrer una lista completa; es el máximo del contrato. */
internal const val TAMANO_PAGINA = 50

/** El dominio trabaja con listas completas: recorre todas las páginas de una lista paginada. */
internal suspend fun <T> todasLasPaginas(pedir: suspend (pagina: Int) -> PaginaDto<T>): List<T> {
    val primera = pedir(0)
    val resto = (1 until primera.totalPaginas).flatMap { pedir(it).elementos }
    return primera.elementos + resto
}
