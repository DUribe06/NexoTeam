package com.grupo3.nexo.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavBackStackEntry
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import com.grupo3.nexo.ui.theme.TinteBosque
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.BosqueMedio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.grupo3.nexo.R
import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.ui.theme.Bosque
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.BordeSutil
import com.grupo3.nexo.ui.theme.GrisEstado
import com.grupo3.nexo.ui.theme.GrisFondo
import com.grupo3.nexo.ui.theme.InstrumentSans
import com.grupo3.nexo.ui.theme.Lienzo
import com.grupo3.nexo.ui.theme.Marcador
import com.grupo3.nexo.ui.theme.Menta
import com.grupo3.nexo.ui.theme.Miel
import com.grupo3.nexo.ui.theme.MielFondo
import com.grupo3.nexo.ui.theme.MielOscura
import com.grupo3.nexo.ui.theme.Rojo
import com.grupo3.nexo.ui.theme.RojoFondo
import com.grupo3.nexo.ui.theme.SuperficieClara
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.VerdeEstado
import com.grupo3.nexo.ui.theme.VerdeFondo
import com.grupo3.nexo.ui.theme.pincelBosque
import com.grupo3.nexo.ui.theme.tonosAvatar
import java.util.Locale

/* ==================== Superficies ==================== */

/** Cifras con ancho fijo (precios, horas, calificaciones) para que no "bailen" al cambiar. */
val CifrasTabulares = TextStyle(fontFeatureSettings = "tnum")

/*
 * Escala de esquinas de la app (el acceso conserva la suya):
 * tarjetas 12 dp, controles (botones, campos) 10 dp, opciones pequeñas 8 dp.
 * Nada de píldoras salvo avatares, puntos de estado y contadores.
 */
val FormaTarjeta = RoundedCornerShape(12.dp)
val FormaControl = RoundedCornerShape(10.dp)
val FormaOpcion = RoundedCornerShape(8.dp)

/**
 * Superficie base: blanco sobre el lienzo con un borde fino. Plana a propósito:
 * sin sombras ni degradados, para que la vista descanse y el contenido mande.
 */
fun Modifier.superficie(forma: Shape = FormaTarjeta): Modifier =
    this
        .background(Color.White, forma)
        .border(1.dp, BordeSutil, forma)
        .clip(forma)

/**
 * Superficie que responde al toque: se hunde levemente (escala 0.98) y muestra la onda
 * de Material recortada a su forma.
 */
fun Modifier.superficiePulsable(
    forma: Shape = FormaTarjeta,
    etiqueta: String? = null,
    alClicar: () -> Unit
): Modifier = composed {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        if (presionado) 0.98f else 1f,
        spring(stiffness = 900f),
        label = "escalaTarjeta"
    )
    this
        .graphicsLayer {
            scaleX = escala
            scaleY = escala
        }
        .superficie(forma)
        .clickable(
            interactionSource = interaccion,
            indication = ripple(color = Bosque),
            role = Role.Button,
            onClickLabel = etiqueta,
            onClick = alClicar
        )
}

/** Solo escala al presionar; para elementos con fondo propio (avatares, píldoras). */
fun Modifier.pulsable(
    rol: Role = Role.Button,
    etiqueta: String? = null,
    habilitado: Boolean = true,
    alClicar: () -> Unit
): Modifier = composed {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(if (presionado) 0.96f else 1f, spring(stiffness = 900f), label = "escala")
    this
        .graphicsLayer {
            scaleX = escala
            scaleY = escala
        }
        .clickable(
            interactionSource = interaccion,
            indication = null,
            enabled = habilitado,
            role = rol,
            onClickLabel = etiqueta,
            onClick = alClicar
        )
}

/* ==================== Botones ==================== */

/**
 * Botón principal: verde bosque plano con esquinas de 10 dp; se hunde levemente al presionar.
 * Es el único elemento oscuro de cada pantalla. `conFlecha` marca acciones de avance.
 */
@Composable
fun BotonPrimario(
    texto: String,
    alClicar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    conFlecha: Boolean = false,
    icono: ImageVector? = null,
    compacto: Boolean = false
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(if (presionado) 0.97f else 1f, spring(stiffness = 900f), label = "escala")
    val desplazamientoFlecha by animateDpAsState(if (presionado) 4.dp else 0.dp, label = "flecha")
    val activo = habilitado && !cargando

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(if (compacto) 44.dp else 52.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .background(if (habilitado) BosqueMedio else Bosque.copy(alpha = 0.07f), FormaControl)
            .clip(FormaControl)
            .clickable(
                interactionSource = interaccion,
                indication = ripple(color = Color.White),
                enabled = activo,
                role = Role.Button,
                onClick = alClicar
            )
            .padding(horizontal = if (compacto) 18.dp else 24.dp)
    ) {
        AnimatedContent(
            targetState = cargando,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "contenidoBoton"
        ) { estaCargando ->
            if (estaCargando) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                val color = if (habilitado) Color.White else TextoTercero
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icono != null) {
                        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        texto,
                        color = color,
                        style = if (compacto) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                    if (conFlecha) {
                        Spacer(Modifier.width(10.dp + desplazamientoFlecha))
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Botón secundario: blanco con borde fino, misma forma que el principal. */
@Composable
fun BotonSecundario(
    texto: String,
    alClicar: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    compacto: Boolean = false,
    peligro: Boolean = false,
    habilitado: Boolean = true,
    colorIcono: Color = BosqueAcento
) {
    val colorTexto = if (peligro) Rojo else TextoPrimario
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(if (compacto) 44.dp else 52.dp)
            .pulsable(habilitado = habilitado, alClicar = alClicar)
            .background(Color.White, FormaControl)
            .border(1.dp, if (peligro) Rojo.copy(alpha = 0.30f) else Bosque.copy(alpha = 0.16f), FormaControl)
            .padding(horizontal = if (compacto) 16.dp else 20.dp)
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = if (peligro) Rojo else colorIcono, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            texto,
            color = colorTexto,
            style = if (compacto) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

/** Acción de texto (enlaces como "Ver todo"), con área táctil de 48 dp. */
@Composable
fun BotonTexto(texto: String, alClicar: () -> Unit, modifier: Modifier = Modifier, color: Color = BosqueAcento) {
    Text(
        texto,
        color = color,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier
            .clip(FormaOpcion)
            .clickable(role = Role.Button, onClick = alClicar)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    )
}

/**
 * Botón tonal: verde muy suave con texto bosque. Para acciones secundarias dentro de
 * listas y tarjetas (Aceptar, Abrir chat, Calificar), sin competir con el botón principal.
 */
@Composable
fun BotonTonal(
    texto: String,
    alClicar: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    colorIcono: Color = BosqueAcento
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(FormaControl)
            .background(TinteBosque)
            .clickable(role = Role.Button, onClick = alClicar)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(texto, color = BosqueProfundo, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}

/**
 * Lista agrupada: una sola superficie blanca con filas separadas por filetes,
 * en lugar de una tarjeta flotante por elemento.
 */
@Composable
fun <T> ListaAgrupada(
    elementos: List<T>,
    modifier: Modifier = Modifier,
    sangriaDivisor: Dp = 16.dp,
    fila: @Composable (T) -> Unit
) {
    Column(modifier.fillMaxWidth().superficie()) {
        elementos.forEachIndexed { i, e ->
            if (i > 0) DivisorFila(sangriaDivisor)
            fila(e)
        }
    }
}

/**
 * Fila de una lista agrupada dentro de una LazyColumn: fondo blanco, esquinas redondeadas solo
 * en la primera y la última, y filete superior con sangría (desde la segunda fila).
 */
fun Modifier.filaAgrupada(indice: Int, total: Int, sangria: Dp = 16.dp, radio: Dp = 12.dp): Modifier {
    val forma = RoundedCornerShape(
        topStart = if (indice == 0) radio else 0.dp,
        topEnd = if (indice == 0) radio else 0.dp,
        bottomStart = if (indice == total - 1) radio else 0.dp,
        bottomEnd = if (indice == total - 1) radio else 0.dp
    )
    return this
        .background(Color.White, forma)
        .clip(forma)
        .then(
            if (indice > 0) Modifier.drawBehind {
                drawLine(Divisor, Offset(sangria.toPx(), 0f), Offset(size.width, 0f), 1.dp.toPx())
            } else Modifier
        )
}

/** Filete entre filas, con sangría para alinearse con el texto (no con el avatar). */
@Composable
fun DivisorFila(sangria: Dp = 16.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = sangria)
            .height(1.dp)
            .background(Divisor)
    )
}

/**
 * Botón de icono sin contenedor (48 dp táctiles): solo el ícono y la onda al tocar.
 * `sobreOscuro` lo pinta en blanco para las cabeceras bosque.
 */
@Composable
fun BotonIcono(
    icono: ImageVector,
    descripcion: String,
    alClicar: () -> Unit,
    modifier: Modifier = Modifier,
    sobreOscuro: Boolean = false,
    insignia: Int = 0
) {
    Box(modifier = modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(FormaControl)
                .clickable(
                    role = Role.Button,
                    onClickLabel = descripcion,
                    onClick = alClicar
                )
                .semantics { contentDescription = descripcion }
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = if (sobreOscuro) Color.White else TextoPrimario,
                modifier = Modifier.size(22.dp)
            )
        }
        if (insignia > 0) {
            InsigniaConteo(
                insignia,
                bordeColor = if (sobreOscuro) BosqueProfundo else Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 0.dp)
            )
        }
    }
}

/* ==================== Campos ==================== */

/**
 * Campo de texto blanco con borde fino que se oscurece al enfocarse (sin sombra).
 * Si `limpiable`, muestra un botón para borrar el texto.
 */
@Composable
fun Campo(
    valor: String,
    alCambiar: (String) -> Unit,
    marcador: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    limpiable: Boolean = false,
    teclado: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    acciones: KeyboardActions = KeyboardActions.Default,
    lineasMaximas: Int = 1
) {
    var enfocado by remember { mutableStateOf(false) }
    val colorBorde by animateColorAsState(
        if (enfocado) BosqueAcento else Bosque.copy(alpha = 0.16f), tween(160), label = "borde"
    )
    val grosorBorde by animateDpAsState(if (enfocado) 1.5.dp else 1.dp, tween(160), label = "grosor")
    val colorIcono by animateColorAsState(
        if (enfocado) BosqueAcento else Bosque.copy(alpha = 0.45f), tween(160), label = "icono"
    )
    val multilinea = lineasMaximas > 1
    val forma = FormaControl

    BasicTextField(
        value = valor,
        onValueChange = alCambiar,
        singleLine = !multilinea,
        maxLines = lineasMaximas,
        textStyle = TextStyle(
            fontFamily = InstrumentSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = BosqueProfundo
        ),
        cursorBrush = SolidColor(BosqueAcento),
        keyboardOptions = teclado,
        keyboardActions = acciones,
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, forma)
            .border(grosorBorde, colorBorde, forma)
            .onFocusChanged { enfocado = it.isFocused },
        decorationBox = { campo ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .padding(
                        start = 16.dp,
                        end = if (limpiable && valor.isNotEmpty()) 4.dp else 16.dp,
                        top = if (multilinea) 8.dp else 0.dp,
                        bottom = if (multilinea) 8.dp else 0.dp
                    )
            ) {
                if (icono != null) {
                    Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(12.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (valor.isEmpty()) {
                        Text(marcador, color = Marcador, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                    campo()
                }
                if (limpiable && valor.isNotEmpty()) {
                    IconButton(onClick = { alCambiar("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Borrar texto", tint = Bosque.copy(alpha = 0.5f), modifier = Modifier.size(19.dp))
                    }
                }
            }
        }
    )
}

/* ==================== Opciones e insignias ==================== */

/**
 * Opción elegible (filtros, materia, hora), con esquinas de 8 dp. En reposo: contorno fino.
 * Elegida: verde muy suave con borde bosque y marca de verificación,
 * para que el verde oscuro quede reservado a la acción principal.
 */
@Composable
fun FiltroOpcion(
    texto: String,
    seleccionado: Boolean,
    alClicar: () -> Unit,
    modifier: Modifier = Modifier,
    conMarca: Boolean = true,
    habilitado: Boolean = true
) {
    val fondo by animateColorAsState(if (seleccionado) TinteBosque else Color.White, tween(160), label = "fondoOpcion")
    val borde by animateColorAsState(if (seleccionado) BosqueAcento.copy(alpha = 0.55f) else BordeSutil, tween(160), label = "bordeOpcion")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(FormaOpcion)
            .background(fondo)
            .border(1.dp, borde, FormaOpcion)
            .clickable(enabled = habilitado, role = Role.Checkbox, onClick = alClicar)
            .semantics { selected = seleccionado }
            .graphicsLayer { alpha = if (habilitado) 1f else 0.4f }
            .padding(start = if (seleccionado && conMarca) 10.dp else 14.dp, end = 14.dp, top = 9.dp, bottom = 9.dp)
    ) {
        if (seleccionado && conMarca) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            texto,
            color = if (seleccionado) BosqueProfundo else BosqueProfundo.copy(alpha = 0.78f),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Medium),
            maxLines = 1
        )
    }
}

/** Lista de elementos como texto corrido: "Cálculo I, Álgebra y Matemática Básica". */
fun enumerar(elementos: List<String>): String = when (elementos.size) {
    0 -> ""
    1 -> elementos[0]
    else -> elementos.dropLast(1).joinToString(", ") + " y " + elementos.last()
}

/** Contador de no leídos / pendientes: menta con cifra bosque (contraste AA). */
@Composable
fun InsigniaConteo(cantidad: Int, modifier: Modifier = Modifier, bordeColor: Color = Color.White) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 18.dp)
            .widthIn(min = 18.dp)
            .border(2.dp, bordeColor, CircleShape)
            .padding(2.dp)
            .background(Menta, CircleShape)
            .padding(horizontal = 4.dp)
            .clearAndSetSemantics { }
    ) {
        Text(
            if (cantidad > 9) "9+" else cantidad.toString(),
            color = BosqueProfundo,
            fontFamily = InstrumentSans,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            lineHeight = 12.sp
        )
    }
}

/** Estado de una solicitud: punto de color y texto, sin fondo. */
@Composable
fun InsigniaEstado(estado: EstadoSolicitud, modifier: Modifier = Modifier) {
    val (texto, punto) = when (estado) {
        EstadoSolicitud.PENDIENTE -> MielOscura to Miel
        EstadoSolicitud.ACEPTADA -> VerdeEstado to Menta
        EstadoSolicitud.RECHAZADA -> Rojo to Rojo
        EstadoSolicitud.FINALIZADA -> TextoSecundario to TextoSecundario
        EstadoSolicitud.CANCELADA -> GrisEstado to GrisEstado.copy(alpha = 0.6f)
        EstadoSolicitud.EXPIRADA -> GrisEstado to GrisEstado.copy(alpha = 0.6f)
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(Modifier.size(7.dp).background(punto, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(estado.etiqueta, color = texto, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}
/* ==================== Personas ==================== */

/** Tono de avatar estable para un nombre (siempre el mismo par de la paleta bosque). */
fun tonoAvatar(nombre: String): Pair<Color, Color> =
    tonosAvatar[Math.floorMod(nombre.trim().lowercase().hashCode(), tonosAvatar.size)]

fun iniciales(nombre: String): String = nombre.split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .mapNotNull { it.firstOrNull() }
    .joinToString("") { it.uppercase() }
    .ifEmpty { "N" }

/**
 * Avatar tipográfico: iniciales blancas sobre un tono del bosque.
 * `enLinea` (si no es null) añade el punto de estado; `anillo` lo separa de fondos oscuros.
 */
@Composable
fun Avatar(
    nombre: String,
    tamano: Dp = 44.dp,
    modifier: Modifier = Modifier,
    enLinea: Boolean? = null,
    anillo: Color? = null
) {
    val (oscuro, _) = tonoAvatar(nombre)
    Box(modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .then(if (anillo != null) Modifier.background(anillo, CircleShape).padding(tamano * 0.05f) else Modifier)
                .size(tamano)
                .clip(CircleShape)
                .background(oscuro)
                .clearAndSetSemantics { }
        ) {
            Text(
                iniciales(nombre),
                color = Color.White,
                fontFamily = InstrumentSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = (tamano.value * 0.36f).sp,
                letterSpacing = (-0.2).sp
            )
        }
        if (enLinea != null) {
            val tamanoPunto = (tamano * 0.27f).coerceAtLeast(10.dp)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(tamanoPunto)
                    .border(2.dp, anillo ?: Color.White, CircleShape)
                    .padding(2.dp)
                    .background(if (enLinea) Menta else Color(0xFFB9C6BE), CircleShape)
            )
        }
    }
}

/** Fila de 5 estrellas (llenas según `cantidad`). */
@Composable
fun Estrellas(cantidad: Int, tamano: Dp = 14.dp, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.semantics { contentDescription = "$cantidad de 5 estrellas" }
    ) {
        repeat(5) { indice ->
            Icon(
                Icons.Rounded.Star,
                contentDescription = null,
                tint = if (indice < cantidad) Miel else Bosque.copy(alpha = 0.12f),
                modifier = Modifier.size(tamano)
            )
        }
    }
}

/** Calificación compacta: estrella + valor con cifras tabulares. */
@Composable
fun Valoracion(valor: Double, modifier: Modifier = Modifier, color: Color = TextoPrimario, resenas: Int? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics(mergeDescendants = true) { }
    ) {
        Icon(Icons.Rounded.Star, contentDescription = "Valoración", tint = Miel, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            formatoValoracion(valor),
            color = color,
            style = MaterialTheme.typography.titleSmall.merge(CifrasTabulares)
        )
        if (resenas != null) {
            Text(
                " ($resenas)",
                color = if (color == TextoPrimario) TextoTercero else color.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall.merge(CifrasTabulares)
            )
        }
    }
}

/** Decimales con coma, como se escriben en Colombia: 4,9 y 1,5 h. */
private val localeColombia = Locale("es", "CO")

fun formatoValoracion(valor: Double): String = String.format(localeColombia, "%.1f", valor)
/** Siglas de las universidades conocidas, para filas densas donde el nombre completo se corta. */
private val siglasUniversidad = mapOf(
    "Universidad Industrial de Santander" to "UIS",
    "Universidad Autónoma de Bucaramanga" to "UNAB",
    "Unidades Tecnológicas de Santander" to "UTS",
    "Universidad Pontificia Bolivariana, Bucaramanga" to "UPB Bucaramanga",
    "Universidad Santo Tomás, Bucaramanga" to "USTA Bucaramanga"
)

fun universidadCorta(nombre: String): String = siglasUniversidad[nombre] ?: nombre

/** Pesos colombianos con punto de miles: 25000.0 → "$25.000". */
fun formatoPrecio(valor: Double): String =
    "$" + valor.toLong().toString().reversed().chunked(3).joinToString(".").reversed()
fun formatoHoras(horas: Double): String =
    if (horas % 1.0 == 0.0) "${horas.toInt()} h" else String.format(localeColombia, "%.1f h", horas)

/* ==================== Textos estructurales ==================== */

/** Encabezado de sección con acción opcional ("Ver todo"). */
@Composable
fun EncabezadoSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    alAccion: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text(
            titulo,
            color = TextoPrimario,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (accion != null && alAccion != null) {
            BotonTexto(accion, alAccion, modifier = Modifier.padding(end = 0.dp))
        }
    }
}

/** Rótulo de grupo en versalitas espaciadas, como "O CONÉCTATE CON" del acceso. */
@Composable
fun RotuloGrupo(texto: String, modifier: Modifier = Modifier, color: Color = TextoTercero) {
    Text(
        texto.uppercase(),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
    )
}

/** Estado vacío: insignia tenue con icono, título, mensaje y acción opcional. */
@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    alAccion: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .background(TinteBosque, FormaTarjeta)
        ) {
            Icon(icono, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(titulo, color = TextoPrimario, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            mensaje,
            color = TextoSecundario,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp)
        )
        if (accion != null && alAccion != null) {
            Spacer(Modifier.height(20.dp))
            BotonSecundario(accion, alAccion, compacto = true)
        }
    }
}

/** Texto de una línea con elipsis, atajo usado en filas densas. */
@Composable
fun TextoLinea(texto: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    Text(texto, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

/* ==================== Marca ==================== */

/** Logo de Nexo en su insignia blanca (la del acceso, en tamaño reducido). */
@Composable
fun InsigniaNexo(tamano: Dp = 36.dp, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(tamano * 0.3f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamano)
            .background(Color.White, forma)
    ) {
        Image(
            painter = painterResource(R.drawable.logo_nexo),
            contentDescription = "Nexo",
            modifier = Modifier.size(tamano * 0.64f)
        )
    }
}

/** Curva orgánica del diseño (viewBox 414×56), escalada al ancho real. */
@Composable
fun Onda(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sx = size.width / 414f
        val sy = size.height / 56f
        val trazo = Path().apply {
            moveTo(0f, 28f * sy)
            cubicTo(115f * sx, 54f * sy, 299f * sx, 6f * sy, size.width, 32f * sy)
            lineTo(size.width, size.height + 1f)
            lineTo(0f, size.height + 1f)
            close()
        }
        drawPath(trazo, color)
    }
}

/* ==================== Movimiento y utilidades ==================== */

/**
 * Aparición escalonada (desvanecido + ascenso de 12 dp) de la pantalla de acceso.
 * Se reproduce solo la primera vez que se muestra la pantalla, no al volver a ella.
 */
@Composable
fun rememberPrimeraVez(): Boolean {
    var iniciado by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { iniciado = true }
    return iniciado
}

@Composable
fun Modifier.aparicion(orden: Int, iniciado: Boolean): Modifier {
    val progreso by animateFloatAsState(
        targetValue = if (iniciado) 1f else 0f,
        animationSpec = tween(480, delayMillis = 40 + orden * 60, easing = EaseOutCubic),
        label = "aparicion$orden"
    )
    return graphicsLayer {
        alpha = progreso
        translationY = (1f - progreso) * 12.dp.toPx()
    }
}

/** Sube el elemento [cantidad] sobre el anterior y descuenta ese espacio del layout. */
fun Modifier.solaparHaciaArriba(cantidad: Dp): Modifier = layout { medible, restricciones ->
    val colocable = medible.measure(restricciones)
    val desplazamiento = cantidad.roundToPx()
    layout(colocable.width, (colocable.height - desplazamiento).coerceAtLeast(0)) {
        colocable.place(0, -desplazamiento)
    }
}

/**
 * Cuántas pantallas visibles piden íconos claros. Con transiciones superpuestas dos
 * pantallas conviven un instante; contar (en vez de "restaurar el valor previo")
 * evita que la que sale deje la barra con el color equivocado.
 */
private var solicitudesIconosClaros = 0

/** Id de la entrada de navegación de destino (la que se está mostrando o hacia la que se va). */
val LocalEntradaActual = staticCompositionLocalOf<String?> { null }

/**
 * Para pantallas con cabecera oscura: íconos claros en la barra de estado
 * mientras la pantalla está visible y `activo` es verdadero. El resto de la app es clara.
 */
@Composable
fun IconosClarosEnBarraDeEstado(activo: Boolean = true) {
    val vista = LocalView.current
    if (vista.isInEditMode) return
    // Durante una transición la pantalla que se va sigue montada hasta terminar su salida; solo la de
    // destino decide el color, así los íconos cambian al instante y no quedan blancos sobre fondo claro
    val propia = (LocalLifecycleOwner.current as? NavBackStackEntry)?.id
    val actual = LocalEntradaActual.current
    val esDestino = propia == null || actual == null || propia == actual
    val activo = activo && esDestino
    DisposableEffect(vista, activo) {
        val ventana = vista.context.buscarActividad()?.window
        if (ventana == null || !activo) {
            onDispose { }
        } else {
            val controlador = WindowCompat.getInsetsController(ventana, vista)
            solicitudesIconosClaros++
            controlador.isAppearanceLightStatusBars = false
            onDispose {
                solicitudesIconosClaros = (solicitudesIconosClaros - 1).coerceAtLeast(0)
                controlador.isAppearanceLightStatusBars = solicitudesIconosClaros == 0
            }
        }
    }
}

/**
 * Velo del color del lienzo detrás de la barra de estado; aparece cuando el contenido
 * se desplaza bajo ella (pantallas sin barra superior fija).
 */
@Composable
fun VeloBarraEstado(visible: Boolean, modifier: Modifier = Modifier) {
    val opacidad by animateFloatAsState(if (visible) 1f else 0f, tween(180), label = "velo")
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .graphicsLayer { alpha = opacidad }
            .background(Lienzo.copy(alpha = 0.96f))
    )
}

private tailrec fun Context.buscarActividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActividad()
    else -> null
}
