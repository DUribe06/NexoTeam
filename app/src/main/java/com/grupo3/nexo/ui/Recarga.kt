package com.grupo3.nexo.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

/**
 * Lectura de datos que puede fallar y reintentarse. Cada ViewModel con listas tiene una:
 * `proteger` suscribe los flujos y, si alguno lanza una excepción (red o servidor), emite el estado de error
 * en vez de dejar caer la app; `reintentar` los vuelve a suscribir.
 */
class Recarga {
    private val _intentos = MutableStateFlow(0)

    /** Cuenta los reintentos; sirve de clave para volver a suscribir un flujo propio. */
    val intentos: StateFlow<Int> = _intentos

    fun reintentar() = _intentos.update { it + 1 }

    /**
     * `origen` se pide de nuevo en cada reintento. Si falla emite `alFallar()`; al reintentar emite antes `alCargar()`
     * para que la pantalla vuelva a mostrar la carga.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> proteger(alCargar: () -> T, alFallar: () -> T, origen: () -> Flow<T>): Flow<T> =
        _intentos.flatMapLatest { n ->
            val flujo = origen().catch { emit(alFallar()) }
            if (n > 0) flujo.onStart { emit(alCargar()) } else flujo
        }
}

/**
 * Ejecuta una acción que escribe y dice si salió bien. Cualquier excepción (red, servidor, validación)
 * cuenta como fallo; la cancelación de la corrutina sí se propaga.
 */
suspend fun intentar(accion: suspend () -> Unit): Boolean =
    try {
        accion()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

/** Como [intentar], pero conserva el resultado o la excepción para decidir qué mostrar. */
suspend fun <T> resultadoDe(accion: suspend () -> T): Result<T> =
    try {
        Result.success(accion())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
