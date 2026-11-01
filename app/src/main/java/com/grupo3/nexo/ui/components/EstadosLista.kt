package com.grupo3.nexo.ui.components

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.grupo3.nexo.ui.theme.BordeSutil
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.FormaTarjeta
import com.grupo3.nexo.ui.theme.Superficie

/**
 * Interruptor de revisión: los datos son simulados y nunca fallan, así que esto permite ver
 * la carga y el error de las listas. Solo responde en compilaciones de depuración.
 */
object RevisionEstados {
    var forzado by mutableStateOf<Carga?>(null)
        private set

    /** Normal → cargando → error → normal. */
    fun siguiente() {
        forzado = when (forzado) {
            null -> Carga.CARGANDO
            Carga.CARGANDO -> Carga.ERROR
            else -> null
        }
    }

    fun limpiar() {
        forzado = null
    }
}

/** Estado de una lista, respetando lo que pida el interruptor de revisión. Llamar durante la composición. */
fun cargaDe(cargando: Boolean, vacia: Boolean): Carga =
    RevisionEstados.forzado ?: when {
        cargando -> Carga.CARGANDO
        vacia -> Carga.VACIO
        else -> Carga.LISTO
    }

/** Doble toque en el título de una pantalla: cambia de estado para revisarlo. No hace nada en la versión final. */
@Composable
fun Modifier.interruptorDeRevision(): Modifier {
    val contexto = LocalContext.current
    val depuracion = remember(contexto) {
        contexto.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
    return if (depuracion) {
        pointerInput(Unit) { detectTapGestures(onDoubleTap = { RevisionEstados.siguiente() }) }
    } else {
        this
    }
}

/** Filas de relleno para una lista que aún carga: avatar y tres líneas, sobre tarjeta con borde. */
@Composable
fun EsqueletoLista(modifier: Modifier = Modifier, filas: Int = 5) {
    Esqueleto(
        modifier
            .fillMaxWidth()
            .padding(horizontal = EspacioMargen, vertical = EspacioNormal),
        verticalArrangement = Arrangement.spacedBy(EspacioMedio)
    ) {
        repeat(filas) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Superficie, FormaTarjeta)
                    .border(1.dp, BordeSutil, FormaTarjeta)
                    .padding(EspacioNormal)
            ) {
                Hueso(Modifier.size(44.dp), CircleShape)
                Spacer(Modifier.width(EspacioMedio))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(EspacioCorto)) {
                    LineaHueso(0.55f, alto = 14.dp)
                    LineaHueso(0.85f, alto = 10.dp)
                    LineaHueso(0.4f, alto = 10.dp)
                }
            }
        }
    }
}

/** Error de una lista. "Reintentar" vuelve a pedir los datos; con datos simulados basta con apagar el interruptor. */
@Composable
fun ErrorDeLista(modifier: Modifier = Modifier, titulo: String = "No pudimos cargar esto") {
    EstadoError(modifier = modifier, titulo = titulo, alReintentar = RevisionEstados::limpiar)
}
