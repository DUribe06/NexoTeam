package com.grupo3.nexo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.SuperficieClara
import com.grupo3.nexo.ui.theme.TinteBosque

/*
 * Esqueletos de carga: siluetas con la forma del contenido que viene, mientras llegan los datos.
 * Respiran con un pulso suave de opacidad (sin destellos que recorran la pantalla) y todas las piezas
 * de un mismo esqueleto laten a la vez. TalkBack los anuncia una sola vez como "Cargando…".
 */

private val LocalPulso = staticCompositionLocalOf<State<Float>> { mutableFloatStateOf(1f) }

/** Contenedor de un esqueleto: da el pulso compartido y lo presenta como un solo elemento accesible. */
@Composable
fun Esqueleto(
    modifier: Modifier = Modifier,
    descripcion: String = "Cargando…",
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val pulso = rememberInfiniteTransition(label = "esqueleto").animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulso"
    )
    CompositionLocalProvider(LocalPulso provides pulso) {
        Column(
            modifier = modifier.clearAndSetSemantics { contentDescription = descripcion },
            verticalArrangement = verticalArrangement,
            content = contenido
        )
    }
}

/** Una pieza del esqueleto: un bloque redondeado que late con el pulso del [Esqueleto] que la contiene. */
@Composable
fun Hueso(
    modifier: Modifier = Modifier,
    forma: Shape = RoundedCornerShape(6.dp),
    color: Color = SuperficieClara
) {
    val pulso = LocalPulso.current
    Box(
        modifier
            .graphicsLayer { alpha = pulso.value }
            .background(color, forma)
    )
}

/** Líneas de texto: alto de una línea de cuerpo y ancho relativo. */
@Composable
fun LineaHueso(fraccion: Float, alto: Dp = 12.dp, modifier: Modifier = Modifier, color: Color = SuperficieClara) =
    Hueso(modifier.fillMaxWidth(fraccion).height(alto), RoundedCornerShape(alto / 2), color)

/** Tarjeta de próxima sesión del Inicio: fecha, hora grande, materia, tutor y el botón de chat. */
@Composable
fun SesionEsqueleto(modifier: Modifier = Modifier, capas: Int = 2) {
    val forma = RoundedCornerShape(22.dp)
    Esqueleto(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(TinteBosque, forma)
                .border(1.dp, BosqueAcento.copy(alpha = 0.14f), forma)
                .padding(start = 16.dp, end = 14.dp, top = 16.dp, bottom = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineaHueso(0.42f, alto = 11.dp, modifier = Modifier.weight(1f), color = Color.White)
                Hueso(Modifier.size(width = 56.dp, height = 11.dp), color = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp, bottom = 16.dp)) {
                Hueso(Modifier.size(width = 112.dp, height = 38.dp), RoundedCornerShape(8.dp), Color.White)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LineaHueso(0.7f, alto = 14.dp, color = Color.White)
                    LineaHueso(0.85f, alto = 10.dp, color = Color.White)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Hueso(Modifier.size(28.dp), CircleShape, Color.White)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LineaHueso(0.45f, alto = 11.dp, color = Color.White)
                    LineaHueso(0.3f, alto = 9.dp, color = Color.White)
                }
                Hueso(Modifier.size(width = 112.dp, height = 40.dp), RoundedCornerShape(20.dp), Color.White)
            }
        }
        // Las sesiones siguientes, apiladas detrás como en la tarjeta real: así el esqueleto mide lo mismo
        // y nada salta cuando llegan los datos
        repeat(capas) { n ->
            Box(
                Modifier
                    .solaparHaciaArriba(12.dp)
                    .padding(horizontal = (8 * (n + 1)).dp)
                    .fillMaxWidth()
                    .height(if (n == 0) 46.dp else 44.dp)
                    .background(
                        TinteBosque.copy(alpha = if (n == 0) 0.62f else 0.34f),
                        RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
                    )
            )
        }
    }
}

/** Estado de unos datos que llegan del repositorio: aún cargando, vacíos o listos. */
enum class Carga { CARGANDO, VACIO, LISTO }
