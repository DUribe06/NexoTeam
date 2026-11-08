package com.grupo3.nexo.ui.components

import com.grupo3.nexo.BuildConfig
import com.grupo3.nexo.ui.theme.TamanoAvatarNormal
import com.grupo3.nexo.ui.theme.TamanoEstrella
import com.grupo3.nexo.ui.theme.TamanoMarcoIcono
import com.grupo3.nexo.ui.theme.TamanoPuntoEstado
import com.grupo3.nexo.ui.theme.AreaTactilMinima
import com.grupo3.nexo.ui.theme.AltoPildora
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavBackStackEntry
import com.grupo3.nexo.R
import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.ui.theme.BordeMarcado
import com.grupo3.nexo.ui.theme.BordeRojoPeligro
import com.grupo3.nexo.ui.theme.BordeSeleccion
import com.grupo3.nexo.ui.theme.BordeSutil
import com.grupo3.nexo.ui.theme.Bosque
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.CampoEntrada
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.DuracionCorta
import com.grupo3.nexo.ui.theme.DuracionMedia
import com.grupo3.nexo.ui.theme.EnfasisFuerte
import com.grupo3.nexo.ui.theme.EscalaPresionBoton
import com.grupo3.nexo.ui.theme.EscalaPresionBotonCompacto
import com.grupo3.nexo.ui.theme.EscalaPresionTarjeta
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioAmplio
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioFino
import com.grupo3.nexo.ui.theme.EspacioGrande
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioMicro
import com.grupo3.nexo.ui.theme.EspacioMinimo
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.EspacioSuelto
import com.grupo3.nexo.ui.theme.EstrellaVacia
import com.grupo3.nexo.ui.theme.FormaControl
import com.grupo3.nexo.ui.theme.FormaOpcion
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.theme.FormaTarjeta
import com.grupo3.nexo.ui.theme.GrisEstado
import com.grupo3.nexo.ui.theme.GrisEstadoSuave
import com.grupo3.nexo.ui.theme.Icono17
import com.grupo3.nexo.ui.theme.Icono19
import com.grupo3.nexo.ui.theme.IconoAccion
import com.grupo3.nexo.ui.theme.IconoCampoReposo
import com.grupo3.nexo.ui.theme.IconoCuerpo
import com.grupo3.nexo.ui.theme.IconoGrande
import com.grupo3.nexo.ui.theme.IconoNormal
import com.grupo3.nexo.ui.theme.IconoPequeno
import com.grupo3.nexo.ui.theme.IconoSecundario
import com.grupo3.nexo.ui.theme.Lienzo
import com.grupo3.nexo.ui.theme.LocalAnimacionesReducidas
import com.grupo3.nexo.ui.theme.Marcador
import com.grupo3.nexo.ui.theme.MarcadorCampo
import com.grupo3.nexo.ui.theme.Menta
import com.grupo3.nexo.ui.theme.Miel
import com.grupo3.nexo.ui.theme.MielFuerte
import com.grupo3.nexo.ui.theme.MielOscura
import com.grupo3.nexo.ui.theme.NumeroInsignia
import com.grupo3.nexo.ui.theme.PuntoDesconectado
import com.grupo3.nexo.ui.theme.RadioOrganico
import com.grupo3.nexo.ui.theme.RellenoTenue
import com.grupo3.nexo.ui.theme.Rojo
import com.grupo3.nexo.ui.theme.SobreOscuro
import com.grupo3.nexo.ui.theme.Superficie
import com.grupo3.nexo.ui.theme.TextoOpcionReposo
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.TinteBosque
import com.grupo3.nexo.ui.theme.VeloBarraEstado
import com.grupo3.nexo.ui.theme.VerdeEstado
import com.grupo3.nexo.ui.theme.enfasisSeleccion
import com.grupo3.nexo.ui.theme.especAparicion
import com.grupo3.nexo.ui.theme.especColor
import com.grupo3.nexo.ui.theme.especFundidoSalida
import com.grupo3.nexo.ui.theme.inicialesAvatar
import com.grupo3.nexo.ui.theme.resortePresion
import com.grupo3.nexo.ui.theme.tonosAvatar
import kotlin.math.roundToInt

/* ==================== Superficies ==================== */

/** Cifras con ancho fijo (precios, horas, calificaciones) para que no "bailen" al cambiar. */
val CifrasTabulares = TextStyle(fontFeatureSettings = "tnum")

/**
 * Superficie base: blanco sobre el lienzo con un borde fino. Plana a propósito:
 * sin sombras ni degradados, para que la vista descanse y el contenido mande.
 */
fun Modifier.superficie(forma: Shape = FormaOrganica): Modifier =
    this
        .background(Superficie, forma)
        .border(1.dp, BordeSutil, forma)
        .clip(forma)

/**
 * Superficie que responde al toque: se hunde levemente (escala 0.98) y muestra la onda
 * de Material recortada a su forma.
 */
@Composable
fun Modifier.superficiePulsable(
    forma: Shape,
    etiqueta: String? = null,
    alClicar: () -> Unit
): Modifier {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val reducidas = LocalAnimacionesReducidas.current
    val escala by animateFloatAsState(
        if (presionado && !reducidas) EscalaPresionTarjeta else 1f,
        resortePresion(),
        label = "escalaTarjeta"
    )
    return this
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

/**
 * Área táctil de 48 dp para una fila visible de [alto]: el padre sigue viendo `alto`
 * y el pulsable (de 48 dp) sobresale por igual arriba y abajo, sin desplazar nada.
 * Va antes de `.height(48.dp)` y `.pulsable(...)`.
 */
fun Modifier.areaTactilSinCambiarMedida(alto: Dp = AltoPildora): Modifier = layout { medible, restricciones ->
    val p = medible.measure(restricciones)
    // Con letra grande el contenido supera los 48 dp: se reporta entero para no cortar el texto
    val reportado = if (p.height > maxOf(alto, AreaTactilMinima).roundToPx()) p.height else alto.roundToPx().coerceAtMost(p.height)
    // Mismo redondeo que el centrado del contenido (mitad hacia arriba), para no mover nada 1 px
    val sobra = ((p.height - reportado) / 2f).roundToInt()
    layout(p.width, reportado) { p.place(0, -sobra) }
}

/** Solo escala al presionar; para elementos con fondo propio (avatares, píldoras). */
@Composable
fun Modifier.pulsable(
    rol: Role = Role.Button,
    etiqueta: String? = null,
    habilitado: Boolean = true,
    alClicar: () -> Unit
): Modifier {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val reducidas = LocalAnimacionesReducidas.current
    val escala by animateFloatAsState(if (presionado && !reducidas) EscalaPresionBotonCompacto else 1f, resortePresion(), label = "escala")
    // Sin animaciones no hay escala: la onda deja la respuesta al toque
    val respuesta = if (reducidas) ripple() else null
    return this
        .graphicsLayer {
            scaleX = escala
            scaleY = escala
        }
        .clickable(
            interactionSource = interaccion,
            indication = respuesta,
            enabled = habilitado,
            role = rol,
            onClickLabel = etiqueta,
            onClick = alClicar
        )
}

/* ==================== Botones ==================== */

/**
 * Botón principal: verde bosque plano con la forma de los controles; se hunde levemente al presionar.
 * Es la acción principal de la pantalla. `conFlecha` marca acciones de avance.
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
    val reducidas = LocalAnimacionesReducidas.current
    val escala by animateFloatAsState(if (presionado && !reducidas) EscalaPresionBoton else 1f, resortePresion(), label = "escala")
    val desplazamientoFlecha by animateDpAsState(
        if (presionado && !reducidas) EspacioMinimo else 0.dp,
        resortePresion(),
        label = "flecha"
    )
    val activo = habilitado && !cargando

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = if (compacto) 44.dp else 52.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .background(if (habilitado) BosqueMedio else RellenoTenue, FormaControl)
            .clip(FormaControl)
            .clickable(
                interactionSource = interaccion,
                indication = ripple(color = SobreOscuro),
                enabled = activo,
                role = Role.Button,
                onClick = alClicar
            )
            .padding(horizontal = if (compacto) EspacioSuelto else EspacioAmplio)
    ) {
        AnimatedContent(
            targetState = cargando,
            transitionSpec = { fadeIn(tween(DuracionMedia)) togetherWith fadeOut(especFundidoSalida()) },
            label = "contenidoBoton"
        ) { estaCargando ->
            if (estaCargando) {
                CircularProgressIndicator(color = SobreOscuro, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                val color = if (habilitado) Superficie else TextoTercero
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icono != null) {
                        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(IconoNormal))
                        Spacer(Modifier.width(EspacioCorto))
                    }
                    Text(
                        texto,
                        color = color,
                        style = if (compacto) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (conFlecha) {
                        Spacer(Modifier.width(EspacioAjustado + desplazamientoFlecha))
                        Icon(
                            IconosNexo.Adelante,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(Icono17)
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
    colorIcono: Color = BosqueAcento,
    /** Alto del botón; 48 dp en pantallas nuevas para cumplir el área táctil mínima. */
    alto: Dp = if (compacto) 44.dp else 52.dp,
    /** Lo que lee TalkBack cuando el texto solo no basta (p. ej. "Calificar a Ana, Cálculo I"). */
    descripcion: String? = null
) {
    val colorTexto = if (peligro) Rojo else TextoPrimario
    // El botón se ve de `alto`, pero el área de toque mide al menos 48 dp
    Box(
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
        modifier = modifier
            .areaTactilSinCambiarMedida(alto)
            .heightIn(min = maxOf(alto, AreaTactilMinima))
            .pulsable(habilitado = habilitado, alClicar = alClicar)
            .semantics { if (descripcion != null) contentDescription = descripcion }
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = alto)
                .background(Superficie, FormaControl)
                .border(1.dp, if (peligro) BordeRojoPeligro else BordeMarcado, FormaControl)
                .padding(horizontal = if (compacto) EspacioNormal else EspacioMargen)
        ) {
            if (icono != null) {
                Icon(icono, contentDescription = null, tint = if (peligro) Rojo else colorIcono, modifier = Modifier.size(IconoNormal))
                Spacer(Modifier.width(EspacioCorto))
            }
            Text(
                texto,
                color = colorTexto,
                style = if (compacto) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

/** La política de privacidad la sirve el mismo servidor de la API. */
val URL_PRIVACIDAD: String get() = BuildConfig.API_URL + "privacidad.html"

/** Acción de texto (enlaces como "Ver todo"), con área táctil de 48 dp. */
@Composable
fun BotonTexto(texto: String, alClicar: () -> Unit, modifier: Modifier = Modifier, color: Color = BosqueAcento, accion: String? = null) {
    Text(
        texto,
        color = color,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier
            .clip(FormaOpcion)
            // [accion] completa lo que TalkBack dice tras "doble toque para…" ("rechazar la solicitud de Ana")
            .clickable(role = Role.Button, onClickLabel = accion, onClick = alClicar)
            .heightIn(min = AreaTactilMinima)
            .padding(horizontal = EspacioMedio, vertical = EspacioCercano)
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
    colorIcono: Color = BosqueAcento,
    accion: String? = null
) {
    // Se ve de 40 dp pero se toca en 48: el área extra sobresale sin mover el diseño
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .areaTactilSinCambiarMedida()
            .heightIn(min = AreaTactilMinima)
            .clip(FormaControl)
            .clickable(role = Role.Button, onClickLabel = accion, onClick = alClicar)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = AltoPildora)
                .clip(FormaControl)
                .background(TinteBosque)
                .padding(horizontal = EspacioCercano, vertical = EspacioAjustado)
        ) {
            if (icono != null) {
                Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(Icono17))
                Spacer(Modifier.width(EspacioFino))
            }
            Text(
                texto,
                color = BosqueProfundo,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

/**
 * Fila de una lista agrupada dentro de una LazyColumn: fondo blanco, esquinas redondeadas solo
 * en la primera y la última, y filete superior con sangría (desde la segunda fila).
 */
fun Modifier.filaAgrupada(indice: Int, total: Int, sangria: Dp = 16.dp, radio: Dp = RadioOrganico): Modifier {
    val primera = indice == 0
    val ultima = indice == total - 1
    val forma = RoundedCornerShape(
        topStart = if (primera) radio else 0.dp,
        topEnd = if (primera) radio else 0.dp,
        bottomStart = if (ultima) radio else 0.dp,
        bottomEnd = if (ultima) radio else 0.dp
    )
    return this
        .background(Superficie, forma)
        .clip(forma)
        .drawBehind {
            // Borde fino solo en el contorno de la lista: el trazo se prolonga fuera de las filas
            // contiguas para que sus bordes internos queden recortados
            val trazo = 2.dp.toPx()
            val arriba = if (primera) 0f else trazo
            val abajo = if (ultima) 0f else trazo
            translate(top = -arriba) {
                drawOutline(
                    forma.createOutline(Size(size.width, size.height + arriba + abajo), layoutDirection, this),
                    BordeSutil,
                    style = Stroke(trazo)
                )
            }
            if (!primera) drawLine(Divisor, Offset(sangria.toPx(), 0f), Offset(size.width, 0f), 1.dp.toPx())
        }
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
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(
                role = Role.Button,
                onClick = alClicar
            )
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(FormaControl)
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = if (sobreOscuro) SobreOscuro else TextoPrimario,
                modifier = Modifier.size(IconoAccion)
            )
        }
        if (insignia > 0) {
            InsigniaConteo(
                insignia,
                bordeColor = if (sobreOscuro) BosqueProfundo else Superficie,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = EspacioMicro, end = 0.dp)
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
    /** Nombre accesible del campo; si falta se usa el marcador. Se lee siempre, también con texto escrito. */
    etiqueta: String? = null,
    limpiable: Boolean = false,
    teclado: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    acciones: KeyboardActions = KeyboardActions.Default,
    lineasMaximas: Int = 1,
    /** Para contraseñas: muestra puntos en lugar del texto. */
    oculto: Boolean = false
) {
    var enfocado by remember { mutableStateOf(false) }
    val colorBorde by animateColorAsState(
        if (enfocado) BosqueAcento else BordeMarcado, especColor(), label = "borde"
    )
    val grosorBorde by animateDpAsState(if (enfocado) 1.5.dp else 1.dp, especColor(), label = "grosor")
    val colorIcono by animateColorAsState(
        if (enfocado) BosqueAcento else IconoCampoReposo, especColor(), label = "icono"
    )
    val multilinea = lineasMaximas > 1
    val forma = FormaControl

    BasicTextField(
        value = valor,
        onValueChange = alCambiar,
        singleLine = !multilinea,
        maxLines = lineasMaximas,
        textStyle = CampoEntrada.copy(color = BosqueProfundo),
        cursorBrush = SolidColor(BosqueAcento),
        keyboardOptions = teclado,
        visualTransformation = if (oculto) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardActions = acciones,
        modifier = modifier
            .fillMaxWidth()
            .background(Superficie, forma)
            .border(grosorBorde, colorBorde, forma)
            .semantics {
                // Con texto escrito, la descripción taparía lo escrito: el nombre del campo pasa a ser su estado
                val nombre = etiqueta ?: marcador
                if (valor.isEmpty()) contentDescription = nombre else stateDescription = nombre
            }
            .onFocusChanged { enfocado = it.isFocused },
        decorationBox = { campo ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .padding(
                        start = EspacioNormal,
                        end = if (limpiable && valor.isNotEmpty()) EspacioMinimo else EspacioNormal,
                        top = if (multilinea) EspacioCorto else 0.dp,
                        bottom = if (multilinea) EspacioCorto else 0.dp
                    )
            ) {
                if (icono != null) {
                    Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(Icono19))
                    Spacer(Modifier.width(EspacioMedio))
                }
                Box(Modifier.weight(1f)) {
                    if (valor.isEmpty()) {
                        Text(marcador, modifier = Modifier.clearAndSetSemantics { }, color = Marcador, style = LocalTextStyle.current.merge(MarcadorCampo), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    campo()
                }
                if (limpiable && valor.isNotEmpty()) {
                    IconButton(onClick = { alCambiar("") }) {
                        Icon(IconosNexo.Cerrar, contentDescription = "Borrar texto", tint = IconoSecundario, modifier = Modifier.size(Icono19))
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
    val fondo by animateColorAsState(if (seleccionado) TinteBosque else Superficie, especColor(), label = "fondoOpcion")
    val borde by animateColorAsState(if (seleccionado) BordeSeleccion else BordeSutil, especColor(), label = "bordeOpcion")
    val interaccion = remember { MutableInteractionSource() }
    // La zona de toque sobresale 4 dp arriba y abajo (48 dp) sin que la opción ocupe más sitio ni cambie de tamaño,
    // también con letra grande
    Box(
        modifier
            .layout { medible, restricciones ->
                val p = medible.measure(restricciones)
                val sobra = 4.dp.roundToPx()
                layout(p.width, p.height - sobra * 2) { p.place(0, -sobra) }
            }
            .clickable(
                interactionSource = interaccion,
                indication = null,
                enabled = habilitado,
                role = Role.RadioButton,
                onClick = alClicar
            )
            .semantics { selected = seleccionado }
            .padding(vertical = EspacioMinimo)
    ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = AltoPildora)
            .clip(FormaOpcion)
            .background(fondo)
            .border(1.dp, borde, FormaOpcion)
            .indication(interaccion, LocalIndication.current)
            .graphicsLayer { alpha = if (habilitado) 1f else 0.4f }
            .padding(start = if (seleccionado && conMarca) EspacioAjustado else EspacioCercano, end = EspacioCercano, top = EspacioAjustado, bottom = EspacioAjustado)
    ) {
        if (seleccionado && conMarca) {
            Icon(IconosNexo.Marca, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(IconoCuerpo))
            Spacer(Modifier.width(EspacioMinimo))
        }
        Text(
            texto,
            color = if (seleccionado) BosqueProfundo else TextoOpcionReposo,
            style = MaterialTheme.typography.titleSmall.merge(enfasisSeleccion(seleccionado)),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
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
fun InsigniaConteo(
    cantidad: Int,
    modifier: Modifier = Modifier,
    bordeColor: Color = Superficie,
    /** Lo que anuncia TalkBack (p. ej. "3 sin leer"); sin él la insignia queda muda, como en la barra. */
    descripcion: String? = null
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 18.dp)
            .widthIn(min = 18.dp)
            .border(2.dp, bordeColor, CircleShape)
            .padding(EspacioMicro)
            .background(Menta, CircleShape)
            .padding(horizontal = EspacioMinimo)
            .clearAndSetSemantics { if (descripcion != null) contentDescription = descripcion }
    ) {
        Text(
            if (cantidad > 9) "9+" else cantidad.toString(),
            color = BosqueProfundo,
            style = LocalTextStyle.current.merge(NumeroInsignia)
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
        EstadoSolicitud.CANCELADA, EstadoSolicitud.EXPIRADA -> GrisEstado to GrisEstadoSuave
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(Modifier.size(TamanoPuntoEstado).background(punto, CircleShape))
        Spacer(Modifier.width(EspacioFino))
        Text(estado.etiqueta, color = texto, style = MaterialTheme.typography.labelMedium.merge(EnfasisFuerte))
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
    tamano: Dp = TamanoAvatarNormal,
    modifier: Modifier = Modifier,
    enLinea: Boolean? = null,
    anillo: Color? = null,
    /** Anuncia "En línea" o "Desconectado" cuando hay punto de estado (no solo el color). */
    anunciarEstado: Boolean = true
) {
    val (oscuro, _) = tonoAvatar(nombre)
    Box(
        modifier.semantics {
            if (enLinea != null && anunciarEstado) contentDescription = if (enLinea) "En línea" else "Desconectado"
        }
    ) {
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
                color = SobreOscuro,
                style = LocalTextStyle.current.merge(inicialesAvatar(tamano))
            )
        }
        if (enLinea != null) {
            val tamanoPunto = (tamano * 0.27f).coerceAtLeast(10.dp)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(tamanoPunto)
                    .border(2.dp, anillo ?: Superficie, CircleShape)
                    .padding(EspacioMicro)
                    .background(if (enLinea) Menta else PuntoDesconectado, CircleShape)
            )
        }
    }
}

/** Fila de 5 estrellas (llenas según `cantidad`). */
@Composable
fun Estrellas(cantidad: Int, tamano: Dp = TamanoEstrella, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(EspacioMicro),
        modifier = modifier.semantics { contentDescription = "$cantidad de 5 estrellas" }
    ) {
        repeat(5) { indice ->
            Icon(
                IconosNexo.Estrella,
                contentDescription = null,
                tint = if (indice < cantidad) MielFuerte else EstrellaVacia,
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
        Icon(IconosNexo.Estrella, contentDescription = "Calificación", tint = MielFuerte, modifier = Modifier.size(IconoPequeno))
        Spacer(Modifier.width(EspacioMinimo))
        Text(
            formatoValoracion(valor),
            color = color,
            style = MaterialTheme.typography.titleSmall.merge(CifrasTabulares)
        )
        if (resenas != null) {
            Text(
                " ($resenas)",
                color = TextoTercero,
                style = MaterialTheme.typography.bodySmall.merge(CifrasTabulares)
            )
        }
    }
}

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
            .heightIn(min = AreaTactilMinima)
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
        modifier = modifier.semantics { heading() }
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
            .padding(horizontal = EspacioGrande, vertical = EspacioGrande)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(TamanoMarcoIcono)
                .background(TinteBosque, FormaTarjeta)
        ) {
            Icon(icono, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(IconoGrande))
        }
        Spacer(Modifier.height(EspacioNormal))
        Text(
            titulo,
            color = TextoPrimario,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(EspacioFino))
        Text(
            mensaje,
            color = TextoSecundario,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp)
        )
        if (accion != null && alAccion != null) {
            Spacer(Modifier.height(EspacioMargen))
            BotonSecundario(accion, alAccion, compacto = true, alto = AreaTactilMinima)
        }
    }
}

/** Estado de error de carga: insignia tenue con aviso, título, mensaje y reintento opcional. */
@Composable
fun EstadoError(
    modifier: Modifier = Modifier,
    titulo: String = "No pudimos cargar esto",
    mensaje: String = "Revisa tu conexión e inténtalo de nuevo en un momento.",
    alReintentar: (() -> Unit)? = null,
) {
    val reducidas = LocalAnimacionesReducidas.current
    val aparicion = remember { Animatable(if (reducidas) 1f else 0f) }
    LaunchedEffect(reducidas) {
        if (reducidas) aparicion.snapTo(1f) else aparicion.animateTo(1f, especAparicion(0))
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = aparicion.value }
            .padding(horizontal = EspacioGrande, vertical = EspacioGrande)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(TamanoMarcoIcono)
                .background(TinteBosque, FormaTarjeta)
        ) {
            Icon(
                IconosNexo.Aviso,
                contentDescription = null,
                tint = TextoSecundario,
                modifier = Modifier.size(IconoGrande)
            )
        }
        Spacer(Modifier.height(EspacioNormal))
        Text(
            titulo,
            color = TextoPrimario,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(EspacioFino))
        Text(
            mensaje,
            color = TextoSecundario,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp)
        )
        if (alReintentar != null) {
            Spacer(Modifier.height(EspacioMargen))
            // El botón compacto mide 44 dp; la caja le da los 48 dp de área táctil sin cambiarlo
            Box(contentAlignment = Alignment.Center, modifier = Modifier.heightIn(min = AreaTactilMinima)) {
                BotonSecundario("Reintentar", alReintentar, compacto = true, alto = AreaTactilMinima)
            }
        }
    }
}

/** Texto de una línea con elipsis, atajo usado en filas densas. */
@Composable
fun TextoLinea(texto: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    Text(texto, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

/* ==================== Marca ==================== */

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
    val reducidas = LocalAnimacionesReducidas.current
    val progreso by animateFloatAsState(
        targetValue = if (iniciado) 1f else 0f,
        animationSpec = if (reducidas) snap() else especAparicion(orden),
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
    val opacidad by animateFloatAsState(if (visible) 1f else 0f, tween(DuracionCorta), label = "velo")
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .graphicsLayer { alpha = opacidad }
            .background(VeloBarraEstado)
    )
}

private tailrec fun Context.buscarActividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActividad()
    else -> null
}

