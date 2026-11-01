package com.grupo3.nexo.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.grupo3.nexo.R

/** Íconos Phosphor de la interfaz (Regular; la estrella, rellena), la misma familia de la campana y la barra. */
object IconosNexo {
    val Adelante: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_flecha_regular)
    val Atras: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_atras)
    val Bandera: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_bandera)
    val Calendario: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_calendario)
    val CalendarioMarcado: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_calendario_check)
    val Candado: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_candado)
    val Cerrar: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_cerrar)
    val Chat: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_chat)
    val Correo: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_correo)
    val Enviar: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_enviar)
    val Estrella: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_estrella)
    val Filtros: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_filtros)
    val Graduacion: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_graduacion)
    val Historial: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_historial)
    val Lupa: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_lupa)
    val Marca: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_check)
    val MarcaCirculo: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_check_circulo)
    val Ojo: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_ojo)
    val OjoTachado: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_ojo_tachado)
    val Resena: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_resena)
    val SinResultados: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_sin_resultados)
    val Usuario: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ph_usuario)
}
