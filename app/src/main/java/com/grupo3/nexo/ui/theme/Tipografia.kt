package com.grupo3.nexo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    tamano: Number,
    alto: Int,
    peso: FontWeight = FontWeight.Normal,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = InstrumentSans,
    fontWeight = peso,
    fontSize = tamano.toFloat().sp,
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

/*
 * Estilos puntuales de las pantallas aprobadas (acceso e Inicio) y de los componentes compartidos.
 * Los que vienen de un Text sin estilo propio solo llevan los atributos que se escribían en el sitio
 * de uso y se aplican con `LocalTextStyle.current.merge(...)`, para heredar lo demás como antes.
 */

// Acceso
val TituloMarca = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp)
val CuerpoAviso = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
val CuerpoEnlace = TextStyle(fontSize = 13.sp)
val MarcadorCampo = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
val TextoBotonPrincipal = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
val EtiquetaSeparador = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.6.sp)
val TextoBotonSecundario = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)

/** Texto escrito en los campos del acceso (sin interlineado propio). */
val CampoAcceso = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 15.sp)
val DigitoCodigo = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)

// Componentes compartidos
val CampoEntrada = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp)
val NumeroInsignia = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp)

/** Iniciales del avatar: el tamaño sigue al del círculo (36 %). */
fun inicialesAvatar(tamano: Dp) = TextStyle(
    fontFamily = InstrumentSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = (tamano.value * 0.36f).sp,
    letterSpacing = (-0.2).sp
)

// Inicio
val EtiquetaArea = estilo(11.5, 16, FontWeight.SemiBold)
val CifraAcceso = estilo(11.5, 15, FontWeight.Medium)
val EtiquetaFecha = estilo(12.5, 16, FontWeight.SemiBold)
val CuerpoSesion = estilo(12.5, 17, FontWeight.Medium)
val TituloMateria = estilo(16, 21, FontWeight.SemiBold, -0.15)
val CuerpoNombre = estilo(13, 21, FontWeight.Medium)
val EtiquetaBoton = estilo(13, 16, FontWeight.SemiBold)
val CifraHora = estilo(42, 46, FontWeight.SemiBold, -1.6)

/** Cifras de resumen (Perfil, Historial): headlineSmall con cifras de ancho fijo. */
val TextoCifraResumen = estilo(22, 28, FontWeight.SemiBold, -0.5).merge(TextStyle(fontFeatureSettings = "tnum"))

// Logo animado
val TituloLogo = estilo(19, 24, FontWeight.SemiBold, -0.4)
val EtiquetaMiga = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)

// Énfasis y variantes puntuales: se aplican con `estiloBase.merge(...)` sobre un estilo de la escala.
/** Peso fuerte para el elemento activo o el estado resaltado. */
val EnfasisFuerte = TextStyle(fontWeight = FontWeight.SemiBold)
/** Peso medio para lo inactivo o lo que no está seleccionado. */
val EnfasisMedio = TextStyle(fontWeight = FontWeight.Medium)
/** Peso de lectura normal, para lo que no pide énfasis. */
val EnfasisNormal = TextStyle(fontWeight = FontWeight.Normal)
/** Cursiva para notas y citas del usuario. */
val NotaCursiva = TextStyle(fontStyle = FontStyle.Italic)
/** Código de verificación: dígitos espaciados para leerlos de uno en uno. */
val CodigoEspaciado = TextStyle(letterSpacing = 2.sp)

/** Fuerte si está activo, medio si no (pestañas, chips, filas elegibles). */
fun enfasisSeleccion(activo: Boolean) = if (activo) EnfasisFuerte else EnfasisMedio
