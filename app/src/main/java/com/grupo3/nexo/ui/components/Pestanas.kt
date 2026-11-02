package com.grupo3.nexo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.DuracionMedia
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.LocalAnimacionesReducidas
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.especColor
import kotlin.math.roundToInt

/**
 * Pestañas subrayadas y desplazables: reemplazan las filas de píldoras para elegir una vista
 * (área, estado, materia). La activa lleva texto bosque y un filete de 2 dp que crece al elegirse.
 */
@Composable
fun Pestanas(
    opciones: List<String>,
    seleccionada: Int,
    alElegir: (Int) -> Unit,
    modifier: Modifier = Modifier,
    margen: Dp = EspacioMargen
) {
    val desplazamiento = rememberScrollState()
    val reducidas = LocalAnimacionesReducidas.current
    // Posición y ancho de cada pestaña, para traer la elegida a la vista
    val limites = remember { mutableMapOf<Int, Pair<Int, Int>>() }
    var visible by remember { mutableIntStateOf(0) }
    LaunchedEffect(seleccionada, opciones, visible) {
        val (inicio, ancho) = limites[seleccionada] ?: return@LaunchedEffect
        val destino = (inicio - (visible - ancho) / 2).coerceIn(0, desplazamiento.maxValue)
        if (reducidas) desplazamiento.scrollTo(destino) else desplazamiento.animateScrollTo(destino)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(Divisor, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx())
            }
            .onSizeChanged { visible = it.width }
            .desvanecerBordes(desplazamiento)
            .horizontalScroll(desplazamiento)
            .padding(horizontal = margen - EspacioMedio)
            .selectableGroup()
    ) {
        opciones.forEachIndexed { i, texto ->
            val activa = i == seleccionada
            val color by animateColorAsState(if (activa) BosqueProfundo else TextoSecundario, especColor(), label = "pestana")
            val progreso by animateFloatAsState(if (activa) 1f else 0f, if (reducidas) snap() else tween(DuracionMedia), label = "filete")
            Text(
                texto,
                color = color,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (activa) FontWeight.SemiBold else FontWeight.Medium),
                maxLines = 1,
                modifier = Modifier
                    .onGloballyPositioned { limites[i] = it.positionInParent().x.roundToInt() to it.size.width }
                    .selectable(selected = activa, role = Role.Tab, onClick = { alElegir(i) })
                    .heightIn(min = 48.dp)
                    .drawBehind {
                        if (progreso > 0f) {
                            val ancho = (size.width - 24.dp.toPx()) * progreso
                            val alto = 2.dp.toPx()
                            drawRoundRect(
                                BosqueMedio,
                                topLeft = Offset((size.width - ancho) / 2f, size.height - alto),
                                size = Size(ancho, alto),
                                cornerRadius = CornerRadius(alto, alto)
                            )
                        }
                    }
                    .padding(horizontal = EspacioMedio, vertical = EspacioCercano)
            )
        }
    }
}

/**
 * Desvanece los bordes de una tira con desplazamiento, solo donde hay más contenido:
 * la última pestaña no queda cortada a media palabra sino que se pierde suavemente.
 */
private fun Modifier.desvanecerBordes(estado: ScrollState, ancho: Dp = 32.dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val borde = (ancho.toPx() / size.width).coerceAtMost(0.5f)
            val inicio = if (estado.canScrollBackward) borde else 0f
            val fin = if (estado.canScrollForward) 1f - borde else 1f
            drawRect(
                Brush.horizontalGradient(
                    0f to (if (inicio > 0f) Color.Transparent else Color.Black),
                    inicio to Color.Black,
                    fin to Color.Black,
                    1f to (if (fin < 1f) Color.Transparent else Color.Black)
                ),
                blendMode = BlendMode.DstIn
            )
        }
