package com.grupo3.nexo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/*
 * Escala de esquinas de la app (el acceso conserva la suya):
 * mínima 6 dp, opciones pequeñas 8 dp, controles (botones, campos) 10 dp,
 * tarjetas 12 dp y paneles 16 dp.
 * Nada de píldoras salvo avatares, puntos de estado y contadores.
 */
val FormaMinima = RoundedCornerShape(6.dp)
val FormaOpcion = RoundedCornerShape(8.dp)
val FormaControl = RoundedCornerShape(10.dp)
val FormaTarjeta = RoundedCornerShape(12.dp)
val FormaPanel = RoundedCornerShape(16.dp)
/** Tarjetas grandes del Perfil y el Historial, hermanas de la tarjeta de sesión del Inicio. */
val FormaOrganica = RoundedCornerShape(22.dp)
