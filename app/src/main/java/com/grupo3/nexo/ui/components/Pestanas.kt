package com.grupo3.nexo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.TextoSecundario

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
    margen: Dp = 20.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(Divisor, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx())
            }
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = margen - 12.dp)
            .selectableGroup()
    ) {
        opciones.forEachIndexed { i, texto ->
            val activa = i == seleccionada
            val color by animateColorAsState(if (activa) BosqueProfundo else TextoSecundario, tween(160), label = "pestana")
            val progreso by animateFloatAsState(if (activa) 1f else 0f, tween(200), label = "filete")
            Text(
                texto,
                color = color,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (activa) FontWeight.SemiBold else FontWeight.Medium),
                maxLines = 1,
                modifier = Modifier
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
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            )
        }
    }
}
