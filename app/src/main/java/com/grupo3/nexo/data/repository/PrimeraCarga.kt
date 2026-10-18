package com.grupo3.nexo.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

/**
 * Simula la latencia del backend solo la primera vez que se piden unos datos; después quedan en caché
 * y llegan al instante. Así la app muestra sus esqueletos de carga como lo hará con el servidor real.
 * Quitar cuando los repositorios hablen con el backend.
 */
internal class PrimeraCarga(private val milisegundos: Long) {
    @Volatile
    private var lista = false

    fun <T> sobre(flujo: Flow<T>): Flow<T> = flujo.onStart {
        if (!lista) {
            delay(milisegundos)
            lista = true
        }
    }
}
