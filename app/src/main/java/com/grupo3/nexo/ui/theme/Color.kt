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
/** Punto de presencia cuando la persona no está en línea. */
val PuntoDesconectado = Color(0xFFB9C6BE)
/** Trazo de los eslabones de la barra de navegación. */
val TrazoEslabon = Color(0xFF153A28)
/** Menta clara para el texto de la miga sobre fondo oscuro. */
val TextoMigaClaro = Color(0xFF8FE0B8)

/* Superficies */
val Fondo = Lienzo
val Superficie = Color.White
/** Relleno tenue para bloques secundarios dentro de una superficie blanca. */
val SuperficieClara = Color(0xFFEDF2EE)
/** Tinte del indicador de selección (pestaña activa, fila presionada). */
val TinteBosque = Color(0xFFE1ECE5)

/* Texto */
val TextoPrimario = BosqueProfundo
/** Salvia oscurecida para texto: 4.5:1 sobre blanco y sobre el lienzo (Salvia queda para formas e ilustración). */
val TextoSecundario = Color(0xFF587966)
/** Metadatos de menor jerarquía; cumple 4.5:1 sobre blanco y sobre el lienzo. */
val TextoTercero = Color(0xFF60776B)
val Marcador = Bosque.copy(alpha = 0.64f)

/* Borde fino de 1 dp (mismo que los campos del acceso). */
val BordeSutil = Bosque.copy(alpha = 0.10f)
val Divisor = Bosque.copy(alpha = 0.07f)
/** Borde de botones secundarios, campos y opciones en reposo. */
val BordeMarcado = Bosque.copy(alpha = 0.16f)
/** Borde verdoso de las tarjetas con acento (esqueleto, disponibilidad, sesión). */
val BordeAcento = BosqueAcento.copy(alpha = 0.14f)

/* Estados */
val Rojo = ErrorBosque
/** Rojo de las acciones destructivas en texto e íconos secundarios. */
val RojoSuave = Rojo.copy(alpha = 0.85f)
val Miel = Color(0xFFD69A1E)
/** Miel para las estrellas de calificación: 3:1 sobre blanco, sin perder la calidez de Miel. */
val MielFuerte = Color(0xFFBF8500)
/** Estrella de calificación sin llenar: 3,08:1 sobre blanco (mínimo 3:1 para componentes gráficos). */
val EstrellaVacia = Bosque.copy(alpha = 0.50f)
val MielOscura = Color(0xFF8A5A00)
val VerdeEstado = Color(0xFF0D6B43)
val GrisEstado = Color(0xFF66716B)

/* Alfas que se repiten entre pantallas, con nombre. */
/** Borde de la opción elegida (modalidad, día, materia, tarjeta de selección). */
val BordeSeleccion = BosqueAcento.copy(alpha = 0.55f)
/** Relleno tenue del botón deshabilitado y de los bloques sin acción. */
val RellenoTenue = Bosque.copy(alpha = 0.07f)
/** Ícono secundario dentro de un campo o tarjeta (lupa, campana, borrar texto): 3,08:1 sobre blanco. */
val IconoSecundario = Bosque.copy(alpha = 0.5f)
/** Contorno de radios y casillas sin marcar: 3:1 sobre el lienzo. */
val BordeRadio = Bosque.copy(alpha = 0.52f)
/** Fondo de la fila elegida en las listas de opciones. */
val TinteFilaElegida = TinteBosque.copy(alpha = 0.5f)
/** Punto de estado apagado (sin conexión, cancelada). */
val GrisEstadoSuave = GrisEstado.copy(alpha = 0.6f)
/** Sombra de las tarjetas elevadas: luz ambiente y luz puntual. */
val SombraAmbiente = BosqueProfundo.copy(alpha = 0.22f)
val SombraPuntual = BosqueProfundo.copy(alpha = 0.38f)

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

/* Logo animado (ilustración) */
val LogoAzulPower = Color(0xFF9CC4FF)
val LogoFondoOscuro = Color(0xFF020B07)
val LogoFondoMedio = Color(0xFF0B3324)
val LogoFondoClaro = Color(0xFF17573C)
val LogoLenguaBorde = Color(0xFF1F8A5B)
val LogoLengua1 = Color(0xFF3CCB8B)
val LogoLengua2 = Color(0xFF7FDDB0)
val LogoLengua3 = Color(0xFFA8EBCB)
val LogoLenguaSurco = Color(0xFF2A9E6A)
val LogoMigaSombra = Color(0xFFC9A46A)
val LogoMiga = Color(0xFFF3E3C3)

/* Escena dibujada de la barra de sesión del Inicio. */
val CalOscuro = Color(0xFF264435)
val CalSalvia = Color(0xFF898F78)
val CalDia = Color(0xFF8C9584)
val CalCuerpo = Color(0xFFE6EAE7)
val CalCelda = Color(0xFFF3F4F1)
val CalLinea = Color(0xFFA3A89F)
val VerdeMarca = Color(0xFF3FA874)
val SombraSuelo = Color(0xFFD5DBD4)

/** Texto pequeño sobre TinteBosque: Salvia y TextoSecundario no llegan a 4,5:1 ahí. */
val TextoSobreTinte = Color(0xFF4A6B59)
/** Texto e íconos sobre el verde y sobre la foto. */
val SobreOscuro = Color.White
/** Texto secundario sobre el verde y sobre la foto (subtítulos de cabecera). */
val SobreOscuroSuave = SobreOscuro.copy(alpha = 0.88f)
