package com.grupo3.nexo.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * Paleta "Bosque", tomada de la pantalla de acceso (docs/diseno/stitch-login).
 * Lienzo claro, verdes profundos para la marca y salvia para los textos secundarios.
 * Toda la app usa solo estos tokens: nada de hex sueltos en las pantallas.
 */
val BosqueProfundo = Color(0xFF06241A)
val Bosque = Color(0xFF0F2E20)
val BosqueMedio = Color(0xFF0D3829)
val BosqueAcento = Color(0xFF1B5E3F)
val Salvia = Color(0xFF5C7F6B)
val Lienzo = Color(0xFFF6F8F6)
val ErrorBosque = Color(0xFFBA1A1A)

/** Menta: solo micro-indicadores (punto "en línea", insignias de no leídos, confirmaciones). */
val Menta = Color(0xFF00B872)

/* Superficies */
val Fondo = Lienzo
val Superficie = Color.White
/** Relleno tenue para bloques secundarios dentro de una superficie blanca. */
val SuperficieClara = Color(0xFFEDF2EE)
/** Tinte del indicador de selección (pestaña activa, fila presionada). */
val TinteBosque = Color(0xFFE1ECE5)

/* Texto */
val TextoPrimario = BosqueProfundo
val TextoSecundario = Salvia
/** Metadatos de menor jerarquía; cumple 4.5:1 sobre blanco y sobre el lienzo. */
val TextoTercero = Color(0xFF6B8577)
val Marcador = Bosque.copy(alpha = 0.42f)

val Acento = BosqueAcento

/* Borde fino de 1 dp (mismo que los campos del acceso). */
val BordeSutil = Bosque.copy(alpha = 0.10f)
val Divisor = Bosque.copy(alpha = 0.07f)

/* Estados */
val Rojo = ErrorBosque
val Miel = Color(0xFFD69A1E)
val MielOscura = Color(0xFF8A5A00)
val MielFondo = Color(0xFFF8EDD5)
val VerdeEstado = Color(0xFF0D6B43)
val VerdeFondo = Color(0xFFDDF2E6)
val RojoFondo = Color(0xFFFBE4E1)
val GrisEstado = Color(0xFF6E7A73)
val GrisFondo = Color(0xFFEDF0EE)

/** Gradiente satinado del botón "Vincularse": botones primarios y estados activos. */
val pincelBosque: Brush
    get() = Brush.linearGradient(listOf(Color(0xFF134631), Color(0xFF0A2D20), Color(0xFF051A12)))

/**
 * Velo de la cabecera con foto: deja ver la foto en el centro, como el acceso, y se cierra
 * arriba (barra de estado y marca) y abajo (saludo en blanco) para mantener el contraste.
 */
val veloCabecera: Brush
    get() = Brush.verticalGradient(
        0f to BosqueProfundo.copy(alpha = 0.74f),
        0.38f to BosqueProfundo.copy(alpha = 0.42f),
        0.66f to BosqueProfundo.copy(alpha = 0.66f),
        1f to BosqueMedio.copy(alpha = 0.90f)
    )

/**
 * Tonos de avatar: variaciones del bosque (pino, musgo, laguna, oliva, corteza, helecho).
 * Todos sostienen iniciales blancas con contraste AA.
 */
val tonosAvatar = listOf(
    Color(0xFF1B5E3F) to Color(0xFF2F7A57),
    Color(0xFF1F4E5A) to Color(0xFF336B78),
    Color(0xFF4B5E2E) to Color(0xFF66794A),
    Color(0xFF6A4E2C) to Color(0xFF86683F),
    Color(0xFF2E4B3F) to Color(0xFF4A6B5D),
    Color(0xFF0F3D33) to Color(0xFF22594C)
)
