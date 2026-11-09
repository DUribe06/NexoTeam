package com.grupo3.nexo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Escala de esquinas de la app (el acceso conserva la suya):
 * mínima 6 dp, opciones pequeñas 8 dp, controles (botones, campos) 10 dp, tarjetas pequeñas 12 dp,
 * botones de ícono 20 dp, superficies orgánicas 22 dp (tarjetas, listas y diálogos) y paneles amplios 24 dp.
 * Nada de píldoras salvo avatares, puntos de estado y contadores.
 */
val FormaMinima = RoundedCornerShape(6.dp)
val FormaOpcion = RoundedCornerShape(8.dp)
val FormaControl = RoundedCornerShape(10.dp)
val FormaTarjeta = RoundedCornerShape(12.dp)
/** Radio de las tarjetas y listas: el de la tarjeta de sesión del Inicio. */
val RadioOrganico = 22.dp
/** Forma de toda tarjeta o lista agrupada, hermana de la tarjeta de sesión del Inicio. */
val FormaOrganica = RoundedCornerShape(RadioOrganico)

/** Botones de ícono y flechas redondeadas del Perfil. */
val FormaBoton = RoundedCornerShape(20.dp)
/** Paneles grandes. */
val FormaAmplia = RoundedCornerShape(24.dp)
/** Campos y botones de 54 dp de alto: extremos totalmente redondos. */
val FormaCampoAlto = RoundedCornerShape(27.dp)
/** Hojas que suben desde abajo: más redondas que las tarjetas (22 dp) que contienen. */
val FormaHoja = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
/** Capas apiladas bajo la tarjeta de sesión. */
val FormaCapaInferior = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)

/** Elevación de las tarjetas principales (sesión del Inicio, avatar del Perfil). */
val ElevacionTarjeta = 14.dp
/** Sombras propias del acceso: el logo flotante y el botón principal (excepción documentada en PRODUCT.md). */
val ElevacionLogoAcceso = 20.dp
val ElevacionBotonAcceso = 16.dp
/** Esquinas de la burbuja de chat: redondeada en general y casi recta en la "cola" y en los mensajes agrupados. */
val RadioBurbuja = 16.dp
val RadioBurbujaCola = 4.dp

/**
 * Forma de la burbuja de chat: la esquina "de cola" va en el borde inferior del autor y también arriba
 * en los mensajes agrupados. Con `margen` se obtiene la forma concéntrica de lo que va dentro (la foto).
 */
fun formaBurbuja(mio: Boolean, iniciaGrupo: Boolean, margen: Dp = 0.dp): RoundedCornerShape {
    val grande = (RadioBurbuja - margen).coerceAtLeast(0.dp)
    val chica = (RadioBurbujaCola - margen).coerceAtLeast(0.dp)
    return RoundedCornerShape(
        topStart = if (!mio && !iniciaGrupo) chica else grande,
        topEnd = if (mio && !iniciaGrupo) chica else grande,
        bottomStart = if (!mio) chica else grande,
        bottomEnd = if (mio) chica else grande
    )
}
