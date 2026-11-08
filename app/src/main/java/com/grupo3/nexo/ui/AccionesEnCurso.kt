package com.grupo3.nexo.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Evita lanzar dos veces la misma acción mientras la primera sigue en curso (doble toque).
 * Cada acción se identifica con una clave (por ejemplo "aceptar:id"); una repetición con la misma clave se ignora
 * y la clave queda libre al terminar, con éxito o con fallo. Se usa desde el hilo principal, como el `viewModelScope`.
 */
class AccionesEnCurso {
    private val enCurso = mutableSetOf<String>()

    fun lanzar(alcance: CoroutineScope, clave: String, accion: suspend () -> Unit) {
        if (!enCurso.add(clave)) return
        alcance.launch {
            try {
                accion()
            } finally {
                enCurso.remove(clave)
            }
        }
    }
}
