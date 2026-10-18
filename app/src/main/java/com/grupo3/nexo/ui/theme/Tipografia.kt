package com.grupo3.nexo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.grupo3.nexo.R

/** Instrument Sans (fuente variable, licencia OFL en assets/fuentes/). */
@OptIn(ExperimentalTextApi::class)
private fun instrumentSans(peso: FontWeight) = Font(
    R.font.instrument_sans,
    weight = peso,
    variationSettings = FontVariation.Settings(FontVariation.weight(peso.weight))
)

val InstrumentSans = FontFamily(
    instrumentSans(FontWeight.Normal),
    instrumentSans(FontWeight.Medium),
    instrumentSans(FontWeight.SemiBold),
    instrumentSans(FontWeight.Bold)
)

private fun estilo(
    tamano: Int,
    alto: Int,
    peso: FontWeight = FontWeight.Normal,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = InstrumentSans,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = alto.sp,
    letterSpacing = tracking.sp
)

/**
 * Escala tipográfica única de la app, calibrada con la pantalla de acceso:
 * títulos SemiBold con tracking negativo ("Enlace directo" = headlineMedium),
 * cuerpo 14/21 en salvia y etiquetas en versalitas espaciadas ("O CONÉCTATE CON" = labelSmall).
 */
val Tipografia = Typography(
    displaySmall = estilo(30, 36, FontWeight.SemiBold, -0.8),
    headlineMedium = estilo(26, 32, FontWeight.SemiBold, -0.6),
    headlineSmall = estilo(22, 28, FontWeight.SemiBold, -0.5),
    titleLarge = estilo(18, 24, FontWeight.SemiBold, -0.3),
    titleMedium = estilo(15, 21, FontWeight.SemiBold, -0.15),
    titleSmall = estilo(13, 18, FontWeight.SemiBold),
    bodyLarge = estilo(15, 22),
    bodyMedium = estilo(14, 21),
    bodySmall = estilo(12, 17),
    labelLarge = estilo(15, 20, FontWeight.SemiBold, 0.3),
    labelMedium = estilo(12, 16, FontWeight.Medium),
    labelSmall = estilo(11, 14, FontWeight.Medium, 1.6)
)
