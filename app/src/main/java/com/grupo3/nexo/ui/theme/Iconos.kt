package com.grupo3.nexo.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Tamaños de ícono, sacados de los usos reales: el número es el lado en dp.
 * Los íconos que van con texto toman el del cuerpo (14 a 18); los de acción y de tarjeta, los mayores.
 */
val Icono12: Dp = 12.dp
val Icono13: Dp = 13.dp
val Icono14: Dp = 14.dp
val Icono15: Dp = 15.dp
val Icono16: Dp = 16.dp
val Icono17: Dp = 17.dp
val Icono18: Dp = 18.dp
val Icono19: Dp = 19.dp
val Icono20: Dp = 20.dp
val Icono22: Dp = 22.dp
val Icono26: Dp = 26.dp
val Icono28: Dp = 28.dp

/*
 * Escala semántica: cada nombre apunta a un token numérico y dice para qué sirve.
 * En las pantallas se usa esta; los numéricos quedan como su respaldo.
 */
/** Detalle junto a texto pequeño o dentro de una etiqueta (flecha de enlace, marca de metadato). */
val IconoDiminuto: Dp = Icono13
/** Ícono en línea con texto de apoyo (estrella de valoración, viñeta de dato). */
val IconoPequeno: Dp = Icono14
/** Ícono que acompaña texto de cuerpo (chips, metadatos, marca de selección). */
val IconoCuerpo: Dp = Icono16
/** Ícono de campo, botón o fila (el tamaño habitual). */
val IconoNormal: Dp = Icono18
/** Ícono de fila destacada o de encabezado de tarjeta. */
val IconoMediano: Dp = Icono22
/** Ícono de botón de acción solo-ícono. */
val IconoAccion: Dp = Icono22
/** Ícono protagonista de una tarjeta o de un estado vacío. */
val IconoGrande: Dp = Icono28
