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

/** Botones de ícono y flechas redondeadas del Perfil. */
val FormaBoton = RoundedCornerShape(20.dp)
/** Paneles grandes. */
val FormaAmplia = RoundedCornerShape(24.dp)
/** Campos y botones de 54 dp de alto: extremos totalmente redondos. */
val FormaCampoAlto = RoundedCornerShape(27.dp)
/** Hojas que suben desde abajo. */
val FormaHoja = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
/** Capas apiladas bajo la tarjeta de sesión. */
val FormaCapaInferior = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
