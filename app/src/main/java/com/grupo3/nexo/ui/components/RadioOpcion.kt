package com.grupo3.nexo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grupo3.nexo.ui.theme.BordeRadio
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.Icono20
import com.grupo3.nexo.ui.theme.Menta
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TinteFilaElegida
import com.grupo3.nexo.ui.theme.especColor

/** Sangría de los divisores entre opciones: alinea con el texto, tras el margen, el círculo y la separación. */
val SangriaDivisorRadio: Dp = EspacioNormal * 2 + Icono20

/**
 * Opción de radio del sistema de diseño: anillo bosque con punto menta (DESIGN.md).
 * `extremo` va a la derecha del texto (íconos, notas); úsala dentro de un `selectableGroup`.
 */
@Composable
fun RadioOpcion(
    texto: String,
    seleccionada: Boolean,
    alClicar: () -> Unit,
    modifier: Modifier = Modifier,
    extremo: @Composable RowScope.() -> Unit = {}
) {
    val borde by animateColorAsState(if (seleccionada) BosqueMedio else BordeRadio, especColor(), label = "radio")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = seleccionada, role = Role.RadioButton, onClick = alClicar)
            .background(if (seleccionada) TinteFilaElegida else Color.Transparent)
            .padding(horizontal = EspacioNormal)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Icono20)
                .border(if (seleccionada) 2.dp else 1.5.dp, borde, CircleShape)
        ) {
            if (seleccionada) Box(Modifier.size(9.dp).background(Menta, CircleShape))
        }
        Spacer(Modifier.width(EspacioNormal))
        Text(texto, color = TextoPrimario, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        extremo()
    }
}
