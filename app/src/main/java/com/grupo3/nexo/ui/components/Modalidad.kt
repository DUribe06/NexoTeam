package com.grupo3.nexo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.vectorResource
import com.grupo3.nexo.R
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.TinteBosque

/** Ícono Phosphor de cada modalidad (cámara o ubicación), la familia de toda la app. */
val Modalidad.icono: ImageVector
    @Composable get() = ImageVector.vectorResource(
        if (this == Modalidad.VIRTUAL) R.drawable.ph_video else R.drawable.ph_ubicacion
    )

/** Texto para un conjunto de modalidades: "Virtual", "Presencial" o "Virtual y presencial". */
fun Set<Modalidad>.descripcion(): String = when {
    size >= 2 -> "Virtual y presencial"
    isEmpty() -> "Sin horarios publicados"
    else -> first().etiqueta
}

/** Modalidad como texto con su ícono, sin fondo. */
@Composable
fun EtiquetaModalidad(modalidad: Modalidad, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(modalidad.icono, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(modalidad.etiqueta, color = BosqueProfundo, style = MaterialTheme.typography.labelMedium)
    }
}

/** Íconos de las modalidades ofrecidas, en orden fijo (virtual, presencial). */
@Composable
fun IconosModalidad(
    modalidades: Set<Modalidad>,
    modifier: Modifier = Modifier,
    tamano: Dp = 14.dp,
    color: Color = BosqueAcento
) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = modifier) {
        Modalidad.entries.filter { it in modalidades }.forEach {
            Icon(it.icono, contentDescription = it.etiqueta, tint = color, modifier = Modifier.size(tamano))
        }
    }
}
