package com.grupo3.nexo.ui.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Espera del servidor entre dos envíos de código. */
const val SEGUNDOS_ENTRE_ENVIOS = 60

/** Cuenta atrás para volver a pedir un código: avisa cada segundo cuánto falta y termina en 0. */
class EsperaReenvio(private val alcance: CoroutineScope, private val alCambiar: (Int) -> Unit) {
    private var cuenta: Job? = null

    fun iniciar(segundos: Int = SEGUNDOS_ENTRE_ENVIOS) {
        cuenta?.cancel()
        cuenta = alcance.launch {
            for (falta in segundos downTo 1) {
                alCambiar(falta)
                delay(1_000)
            }
            alCambiar(0)
        }
    }
}
