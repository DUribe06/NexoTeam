package com.grupo3.nexo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val EsquemaBosque = lightColorScheme(
    primary = BosqueMedio,
    onPrimary = Color.White,
    primaryContainer = TinteBosque,
    onPrimaryContainer = BosqueProfundo,
    secondary = Salvia,
    onSecondary = Color.White,
    secondaryContainer = TinteBosque,
    onSecondaryContainer = BosqueProfundo,
    tertiary = Menta,
    background = Fondo,
    onBackground = TextoPrimario,
    surface = Superficie,
    onSurface = TextoPrimario,
    surfaceVariant = SuperficieClara,
    onSurfaceVariant = TextoSecundario,
    surfaceContainerLowest = Superficie,
    surfaceContainerLow = Superficie,
    surfaceContainer = Superficie,
    surfaceContainerHigh = Superficie,
    surfaceContainerHighest = SuperficieClara,
    inverseSurface = BosqueProfundo,
    inverseOnSurface = Color.White,
    outline = BordeSutil,
    outlineVariant = Divisor,
    scrim = BosqueProfundo,
    error = Rojo
)

/** Esquinas sobrias: 8 dp en opciones, 12 dp en tarjetas, 16 dp en hojas y diálogos. */
private val Formas = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

/**
 * Tema de la app: claro (como la pantalla de acceso), paleta "Bosque".
 */
@Composable
fun NexoTema(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaBosque,
        typography = Tipografia,
        shapes = Formas,
        content = contenido
    )
}
