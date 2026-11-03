package com.grupo3.nexo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grupo3.nexo.ui.theme.TamanoAvatarNormal
import com.grupo3.nexo.ui.theme.BordeSutil
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.theme.Superficie

/** Estado de una lista. Llamar durante la composición. */
fun cargaDe(cargando: Boolean, vacia: Boolean, error: Boolean = false): Carga = when {
    error -> Carga.ERROR
    cargando -> Carga.CARGANDO
    vacia -> Carga.VACIO
    else -> Carga.LISTO
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
                    .background(Superficie, FormaOrganica)
                    .border(1.dp, BordeSutil, FormaOrganica)
                    .padding(EspacioNormal)
            ) {
                Hueso(Modifier.size(TamanoAvatarNormal), CircleShape)
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

/** Error de una lista. "Reintentar" llama a `alReintentar` (normalmente `viewModel.reintentar()`). */
@Composable
fun ErrorDeLista(modifier: Modifier = Modifier, titulo: String = "No pudimos cargar esto", alReintentar: () -> Unit = {}) {
    EstadoError(
        modifier = modifier,
        titulo = titulo,
        alReintentar = alReintentar
    )
}
