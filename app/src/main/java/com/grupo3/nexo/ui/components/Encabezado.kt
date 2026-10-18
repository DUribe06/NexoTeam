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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grupo3.nexo.R
import com.grupo3.nexo.ui.theme.Fondo
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.BosqueMedio
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

/** Título de las pestañas principales: grande, como "Enlace directo", con subtítulo en salvia. */
@Composable
fun TituloPantalla(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 6.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                color = TextoPrimario,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitulo != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitulo, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
            }
        }
        acciones()
    }
}

/** Barra de las pantallas de detalle: volver + título, sobre el lienzo. */
@Composable
fun BarraSuperior(
    titulo: String,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        BotonIcono(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", alVolver)
        Spacer(Modifier.width(8.dp))
        Text(
            titulo,
            color = TextoPrimario,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        acciones()
    }
}
