package com.grupo3.nexo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grupo3.nexo.R
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioMicro
import com.grupo3.nexo.ui.theme.EspacioMinimo
import com.grupo3.nexo.ui.theme.Fondo
import com.grupo3.nexo.ui.theme.SobreOscuro
import com.grupo3.nexo.ui.theme.SobreOscuroSuave
import com.grupo3.nexo.ui.theme.veloCabecera

/**
 * Cabecera bosque: bloque de color plano (o la foto de la biblioteca bajo un velo denso)
 * con borde inferior recto. `alturaOnda` reserva el espacio inferior donde se apoya
 * el elemento que se solapa (buscador, avatar).
 */
@Composable
fun CabeceraBosque(
    modifier: Modifier = Modifier,
    conFoto: Boolean = false,
    alturaOnda: Dp = 44.dp,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
    ) {
        if (conFoto) {
            Image(
                painter = painterResource(R.drawable.fondo_acceso),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0f, -0.2f),
                modifier = Modifier.matchParentSize()
            )
            Box(Modifier.matchParentSize().background(veloCabecera))
        } else {
            Box(Modifier.matchParentSize().background(BosqueMedio))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(bottom = alturaOnda),
            content = contenido
        )
    }
}

/**
 * Cabecera baja de las pantallas secundarias: la foto del Inicio bajo el velo, el título en blanco
 * y la misma onda que corta la foto. Con `alVolver` lleva la flecha a la izquierda.
 */
@Composable
fun CabeceraSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    alVolver: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    // La foto es oscura: íconos claros en la barra de estado mientras la cabecera está a la vista
    IconosClarosEnBarraDeEstado()
    Box(modifier) {
        CabeceraBosque(conFoto = true, alturaOnda = 24.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (alVolver != null) EspacioCorto else EspacioMargen, end = EspacioMedio, top = EspacioCercano, bottom = EspacioMinimo)
            ) {
                if (alVolver != null) {
                    BotonIcono(IconosNexo.Atras, "Volver", alVolver, sobreOscuro = true)
                    Spacer(Modifier.width(EspacioMinimo))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        titulo,
                        color = SobreOscuro,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .semantics { heading() }
                            .interruptorDeRevision()
                    )
                    if (subtitulo != null) {
                        Spacer(Modifier.height(EspacioMicro))
                        Text(subtitulo, color = SobreOscuroSuave, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                acciones()
            }
        }
        Onda(
            Fondo,
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(32.dp)
        )
    }
}

