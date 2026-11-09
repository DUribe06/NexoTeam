package com.grupo3.nexo.ui.auth

import com.grupo3.nexo.ui.theme.AreaTactilMinima
import com.grupo3.nexo.ui.theme.DesplazamientoFlechaBoton
import com.grupo3.nexo.ui.theme.ElevacionCampoEnfocado
import com.grupo3.nexo.ui.theme.ElevacionCampoReposo
import com.grupo3.nexo.ui.theme.resorteSuave
import com.grupo3.nexo.ui.theme.resorteSuaveDp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo3.nexo.R
import com.grupo3.nexo.ui.components.BotonPrimario
import com.grupo3.nexo.ui.components.BotonTexto
import com.grupo3.nexo.ui.components.URL_PRIVACIDAD
import com.grupo3.nexo.ui.components.HojaNexo
import com.grupo3.nexo.ui.components.IconosClarosEnBarraDeEstado
import com.grupo3.nexo.ui.components.IconosNexo
import com.grupo3.nexo.ui.components.Onda
import com.grupo3.nexo.ui.components.areaTactilSinCambiarMedida
import com.grupo3.nexo.ui.theme.BordeBotonGoogle
import com.grupo3.nexo.ui.theme.BordeLogoAcceso
import com.grupo3.nexo.ui.theme.BordeRadio
import com.grupo3.nexo.ui.theme.BordeSeleccion
import com.grupo3.nexo.ui.theme.BordeSutil
import com.grupo3.nexo.ui.theme.Bosque
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.CampoAcceso
import com.grupo3.nexo.ui.theme.CuerpoAviso
import com.grupo3.nexo.ui.theme.CuerpoEnlace
import com.grupo3.nexo.ui.theme.CurvaAparicion
import com.grupo3.nexo.ui.theme.CurvaEstandar
import com.grupo3.nexo.ui.theme.DuracionAplasteLogo
import com.grupo3.nexo.ui.theme.DuracionCorta
import com.grupo3.nexo.ui.theme.DuracionEntraBotonAcceso
import com.grupo3.nexo.ui.theme.DuracionEntradaAcceso
import com.grupo3.nexo.ui.theme.DuracionFlotacionLogo
import com.grupo3.nexo.ui.theme.DuracionInstante
import com.grupo3.nexo.ui.theme.DuracionMedia
import com.grupo3.nexo.ui.theme.DuracionZoomPortada
import com.grupo3.nexo.ui.theme.ElevacionBotonAcceso
import com.grupo3.nexo.ui.theme.ElevacionLogoAcceso
import com.grupo3.nexo.ui.theme.ErrorBosque
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioAmplio
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioExtenso
import com.grupo3.nexo.ui.theme.EspacioFino
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioMinimo
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.EspacioSeparadorAcceso
import com.grupo3.nexo.ui.theme.EtiquetaSeparador
import com.grupo3.nexo.ui.theme.FormaAmplia
import com.grupo3.nexo.ui.theme.Icono19
import com.grupo3.nexo.ui.theme.IconoCampoAcceso
import com.grupo3.nexo.ui.theme.InstrumentSans
import com.grupo3.nexo.ui.theme.Lienzo
import com.grupo3.nexo.ui.theme.LineaSeparadorAcceso
import com.grupo3.nexo.ui.theme.LocalAnimacionesReducidas
import com.grupo3.nexo.ui.theme.Marcador
import com.grupo3.nexo.ui.theme.MarcadorCampo
import com.grupo3.nexo.ui.theme.PasoEntradaAcceso
import com.grupo3.nexo.ui.theme.RetrasoEntradaAcceso
import com.grupo3.nexo.ui.theme.SobreOscuro
import com.grupo3.nexo.ui.theme.SombraAmbienteCampoAcceso
import com.grupo3.nexo.ui.theme.SombraPuntualBotonAcceso
import com.grupo3.nexo.ui.theme.SombraPuntualCampoAcceso
import com.grupo3.nexo.ui.theme.SombraPuntualLogoAcceso
import com.grupo3.nexo.ui.theme.Superficie
import com.grupo3.nexo.ui.theme.TextoBotonPrincipal
import com.grupo3.nexo.ui.theme.TextoBotonSecundario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TituloMarca
import com.grupo3.nexo.ui.theme.VeloAccesoAbajo
import com.grupo3.nexo.ui.theme.VeloAccesoArriba
import com.grupo3.nexo.ui.theme.VeloAccesoCentro
import com.grupo3.nexo.ui.theme.pincelBosque
import com.grupo3.nexo.ui.theme.resorteLogoAcceso
import com.grupo3.nexo.ui.theme.resorteRebote
import kotlinx.coroutines.launch

/* ==================== Pantalla ==================== */

/**
 * Acceso a Nexo: registro con nombre, correo y contraseña,
 * inicio de sesión y acceso con Google (HU-01/02).
 * Diseño de referencia: docs/diseno/stitch-login.
 */
@Composable
fun PantallaLogin(
    alEntrar: () -> Unit,
    alVerificarCorreo: (correo: String, reenviarAlAbrir: Boolean) -> Unit,
    alOlvidarContrasena: (correo: String) -> Unit,
    /** Correo con la contraseña recién cambiada, al volver de restablecerla; null en cualquier otro caso. */
    correoRestablecido: String? = null,
    alConsumirRestablecido: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.autenticado) {
        if (estado.autenticado) {
            alEntrar()
            viewModel.consumirNavegacion()
        }
    }

    LaunchedEffect(estado.correoPorVerificar) {
        estado.correoPorVerificar?.let { correo ->
            alVerificarCorreo(correo, estado.reenviarAlVerificar)
            viewModel.consumirNavegacion()
        }
    }

    LaunchedEffect(correoRestablecido) {
        correoRestablecido?.let { correo ->
            viewModel.contrasenaRestablecida(correo)
            alConsumirRestablecido()
        }
    }

    IconosClarosEnBarraDeEstado()

    val contexto = LocalContext.current
    ContenidoAcceso(
        estado = estado,
        alCambiarNombre = viewModel::cambiarNombre,
        alCambiarCorreo = viewModel::cambiarCorreo,
        alCambiarContrasena = viewModel::cambiarContrasena,
        alCambiarCondiciones = viewModel::cambiarCondiciones,
        alEnviar = viewModel::entrar,
        alEntrarConGoogle = { viewModel.entrarGoogle { pedirTokenGoogle(contexto) } },
        alAlternarModo = viewModel::alternarModo,
        alOlvidarContrasena = { alOlvidarContrasena(estado.correo) }
    )

    if (estado.pedirCondicionesGoogle) {
        HojaCondiciones(
            alCerrar = viewModel::cancelarCondicionesGoogle,
            textoBoton = "Acepto y continúo",
            alConfirmar = viewModel::aceptarCondicionesGoogle
        )
    }
}

@Composable
private fun ContenidoAcceso(
    estado: LoginViewModel.Estado,
    alCambiarNombre: (String) -> Unit,
    alCambiarCorreo: (String) -> Unit,
    alCambiarContrasena: (String) -> Unit,
    alCambiarCondiciones: (Boolean) -> Unit,
    alEnviar: () -> Unit,
    alEntrarConGoogle: () -> Unit,
    alAlternarModo: () -> Unit,
    alOlvidarContrasena: () -> Unit
) {
    val focos = LocalFocusManager.current
    val enviar = {
        focos.clearFocus()
        alEnviar()
    }

    val iniciado = recordarEntradaIniciada()

    MarcoAcceso(iniciado = iniciado) {
        Spacer(Modifier.height(EspacioCercano))
        Text(
            "Nexo",
            style = LocalTextStyle.current.merge(TituloMarca),
            modifier = Modifier
                .semantics { heading() }
                .entrada(1, iniciado)
        )
        Spacer(Modifier.height(EspacioMinimo))
        AnimatedContent(
            targetState = estado.esRegistro,
            transitionSpec = { fadeIn(tween(DuracionMedia)) togetherWith fadeOut(tween(DuracionCorta)) },
            label = "subtitulo",
            modifier = Modifier.entrada(1, iniciado)
        ) { esRegistro ->
            Text(
                if (esRegistro) "Acceso a la red colaborativa de tutorías."
                else "Inicia sesión para encontrar o dar tutorías.",
                color = TextoSecundario,
                style = LocalTextStyle.current.merge(MaterialTheme.typography.bodyMedium),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }

        Spacer(Modifier.height(EspacioAmplio))

        // Formulario
        AnimatedVisibility(
            visible = estado.esRegistro,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            CampoPildora(
                valor = estado.nombre,
                alCambiar = alCambiarNombre,
                marcador = "Nombre o alias",
                icono = IconosNexo.Usuario,
                teclado = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .padding(bottom = EspacioMedio)
                    .entrada(2, iniciado)
            )
        }
        CampoPildora(
            valor = estado.correo,
            alCambiar = alCambiarCorreo,
            marcador = "correo@ejemplo.com",
            icono = IconosNexo.Correo,
            teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.entrada(3, iniciado)
        )
        Spacer(Modifier.height(EspacioMedio))
        CampoPildora(
            valor = estado.contrasena,
            alCambiar = alCambiarContrasena,
            marcador = if (estado.esRegistro) "Contraseña segura" else "Contraseña",
            icono = IconosNexo.Candado,
            esContrasena = true,
            teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            alPulsarListo = enviar,
            modifier = Modifier.entrada(4, iniciado)
        )

        AnimatedVisibility(
            visible = !estado.esRegistro,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .entrada(4, iniciado)
            ) {
                Text(
                    "¿Olvidaste tu contraseña?",
                    color = Bosque,
                    fontWeight = FontWeight.SemiBold,
                    style = LocalTextStyle.current.merge(CuerpoEnlace),
                    modifier = Modifier
                        .padding(end = EspacioMedio)
                        .areaTactilSinCambiarMedida()
                        .clickable(role = Role.Button, onClick = alOlvidarContrasena)
                        .heightIn(min = AreaTactilMinima)
                        .wrapContentHeight()
                )
            }
        }

        AnimatedVisibility(
            visible = estado.esRegistro,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(Modifier.entrada(5, iniciado)) {
                CasillaCondiciones(
                    aceptado = estado.aceptaCondiciones,
                    alCambiar = alCambiarCondiciones,
                    modifier = Modifier.padding(top = EspacioFino)
                )
            }
        }

        MensajeAcceso(error = estado.error, aviso = estado.aviso)

        Spacer(Modifier.height(EspacioNormal))
        BotonVincularse(
            texto = if (estado.esRegistro) "Vincularse" else "Iniciar sesión",
            cargando = estado.cargando,
            alClicar = enviar,
            modifier = Modifier.entrada(5, iniciado)
        )

        Spacer(Modifier.height(EspacioSeparadorAcceso))
        SeparadorSocial(Modifier.entrada(6, iniciado))
        Spacer(Modifier.height(EspacioCercano))
        BotonGoogle(
            habilitado = !estado.cargando,
            alClicar = alEntrarConGoogle,
            modifier = Modifier.entrada(6, iniciado)
        )

        Spacer(Modifier.height(EspacioAjustado))
        EnlaceAcceso(
            pregunta = if (estado.esRegistro) "¿Ya perteneces a Nexo? " else "¿Aún no tienes cuenta? ",
            accion = if (estado.esRegistro) "Iniciar sesión" else "Vincúlate",
            alClicar = alAlternarModo,
            modifier = Modifier.entrada(7, iniciado)
        )
        Spacer(Modifier.height(EspacioNormal))
    }
}

/* ==================== Marco común de las pantallas de cuenta ==================== */

/** Entrada escalonada: solo la primera vez (no se repite al rotar la pantalla). */
@Composable
internal fun recordarEntradaIniciada(): Boolean {
    var iniciado by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { iniciado = true }
    return iniciado
}

/**
 * Lo que comparten las pantallas de cuenta: foto con onda, insignia del logo y columna centrada.
 * Las que abren el teclado al entrar piden [desplazable] y una portada más baja, para que el formulario
 * se pueda alcanzar con el teclado abierto.
 */
@Composable
internal fun MarcoAcceso(
    iniciado: Boolean,
    altoMaximoPortada: Dp = 360.dp,
    desplazable: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit
) {
    ProvideTextStyle(TextStyle(fontFamily = InstrumentSans, color = BosqueProfundo)) {
        // El alto se mide sin teclado: así la portada no se encoge al escribir y el
        // scroll solo aparece cuando el teclado (o un teléfono muy bajo) lo exige.
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Lienzo)
        ) {
            val altoPantalla = maxHeight
            PortadaAjustable(
                altoPantalla = altoPantalla,
                altoMaximoPortada = altoMaximoPortada,
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .then(if (desplazable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                portada = { Portada() }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    InsigniaLogo(
                        Modifier
                            .solaparHaciaArriba(40.dp)
                            .entrada(0, iniciado)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .widthIn(max = 440.dp)
                            .fillMaxWidth()
                            .padding(horizontal = EspacioExtenso),
                        content = contenido
                    )
                }
            }
        }
    }
}

/** Título y texto de apoyo de las pantallas de cuenta. */
@Composable
internal fun EncabezadoAcceso(titulo: String, subtitulo: String, iniciado: Boolean) {
    Spacer(Modifier.height(EspacioCercano))
    Text(
        titulo,
        style = LocalTextStyle.current.merge(TituloMarca),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .semantics { heading() }
            .entrada(1, iniciado)
    )
    Spacer(Modifier.height(EspacioMinimo))
    Text(
        subtitulo,
        color = TextoSecundario,
        style = LocalTextStyle.current.merge(MaterialTheme.typography.bodyMedium),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .widthIn(max = 300.dp)
            .entrada(1, iniciado)
    )
    Spacer(Modifier.height(EspacioAmplio))
}

/** Línea de error, o de confirmación en verde si no hay error, justo encima del botón principal. */
@Composable
internal fun MensajeAcceso(error: String?, aviso: String? = null) {
    AnimatedVisibility(
        visible = error != null || aviso != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Text(
            error ?: aviso.orEmpty(),
            color = if (error != null) ErrorBosque else Bosque,
            style = LocalTextStyle.current.merge(CuerpoAviso),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = EspacioAjustado)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

/**
 * Coloca la [portada] sobre el [contenido] y le da a la portada solo el alto que sobra
 * en pantalla (entre [altoMinimoPortada] y [altoMaximoPortada]), para que el formulario
 * quepa completo sin desplazarse en cualquier teléfono.
 */
@Composable
private fun PortadaAjustable(
    altoPantalla: Dp,
    modifier: Modifier = Modifier,
    altoMinimoPortada: Dp = 130.dp,
    altoMaximoPortada: Dp = 360.dp,
    portada: @Composable () -> Unit,
    contenido: @Composable () -> Unit
) {
    Layout(contents = listOf(portada, contenido), modifier = modifier) { (medPortada, medContenido), restricciones ->
        val ancho = restricciones.maxWidth
        val contenidoColocable = medContenido.first().measure(Constraints(maxWidth = ancho))
        val altoPortada = (altoPantalla.roundToPx() - contenidoColocable.height)
            .coerceIn(altoMinimoPortada.roundToPx(), altoMaximoPortada.roundToPx())
        val portadaColocable = medPortada.first().measure(Constraints.fixed(ancho, altoPortada))
        val altoTotal = maxOf(altoPortada + contenidoColocable.height, restricciones.minHeight)
        layout(ancho, altoTotal) {
            portadaColocable.place(0, 0)
            contenidoColocable.place(0, altoPortada)
        }
    }
}

/* ==================== Piezas de la pantalla ==================== */

/** Foto de cabecera con velo verde y onda de transición hacia el lienzo. */
@Composable
private fun Portada() {
    // Zoom muy lento (efecto Ken Burns) para dar vida a la foto sin distraer.
    // Con las animaciones del sistema apagadas la foto queda quieta.
    val zoom = if (LocalAnimacionesReducidas.current) 1f else rememberInfiniteTransition(label = "portada").animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(DuracionZoomPortada, easing = LinearEasing), RepeatMode.Reverse),
        label = "zoom"
    ).value
    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        Image(
            painter = painterResource(R.drawable.fondo_acceso),
            contentDescription = "Dos estudiantes trabajando juntos en una biblioteca",
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(0f, -0.3f),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                }
        )
        // Velo: da contraste a los íconos de la barra de estado y unifica la foto con la paleta.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            VeloAccesoArriba,
                            VeloAccesoCentro,
                            VeloAccesoAbajo
                        )
                    )
                )
        )
        Onda(
            color = Lienzo,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp)
        )
    }
}

/** Logo de Nexo en una insignia blanca que flota suavemente sobre la onda. */
@Composable
private fun InsigniaLogo(modifier: Modifier = Modifier) {
    val forma = FormaAmplia
    val flotacion = if (LocalAnimacionesReducidas.current) 0f else rememberInfiniteTransition(label = "logo").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(DuracionFlotacionLogo, easing = CurvaEstandar), RepeatMode.Reverse),
        label = "flotacion"
    ).value
    // Rebote al tocarlo: se aplasta un poco y vuelve con un resorte elástico.
    val rebote = remember { Animatable(1f) }
    val alcance = rememberCoroutineScope()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                translationY = -flotacion * 4.dp.toPx()
                scaleX = rebote.value
                scaleY = rebote.value
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Animar logo"
            ) {
                alcance.launch {
                    rebote.animateTo(0.82f, tween(DuracionAplasteLogo, easing = CurvaEstandar))
                    rebote.animateTo(1f, resorteLogoAcceso())
                }
            }
            .size(84.dp)
            .shadow(ElevacionLogoAcceso, forma, ambientColor = BosqueProfundo, spotColor = SombraPuntualLogoAcceso)
            .background(Superficie, forma)
            .border(1.dp, BordeLogoAcceso, forma)
    ) {
        Image(
            painter = painterResource(R.drawable.logo_nexo),
            contentDescription = "Logo de Nexo",
            modifier = Modifier.size(54.dp)
        )
    }
}

/** Campo en forma de píldora blanca, con realce animado al enfocarse. */
@Composable
internal fun CampoPildora(
    valor: String,
    alCambiar: (String) -> Unit,
    marcador: String,
    icono: ImageVector,
    teclado: KeyboardOptions,
    modifier: Modifier = Modifier,
    esContrasena: Boolean = false,
    alPulsarListo: () -> Unit = {}
) {
    var enfocado by remember { mutableStateOf(false) }
    var visible by rememberSaveable { mutableStateOf(false) }

    val colorBorde by animateColorAsState(
        if (enfocado) BordeSeleccion else BordeSutil, resorteSuave(), label = "borde"
    )
    val grosorBorde by animateDpAsState(if (enfocado) 1.5.dp else 1.dp, resorteSuaveDp(), label = "grosor")
    val elevacion by animateDpAsState(if (enfocado) ElevacionCampoEnfocado else ElevacionCampoReposo, resorteSuaveDp(), label = "elevacion")
    val colorIcono by animateColorAsState(
        if (enfocado) BosqueAcento else IconoCampoAcceso, resorteSuave(), label = "icono"
    )

    BasicTextField(
        value = valor,
        onValueChange = alCambiar,
        singleLine = true,
        textStyle = CampoAcceso.copy(color = BosqueProfundo),
        cursorBrush = SolidColor(BosqueAcento),
        visualTransformation = if (esContrasena && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = teclado,
        keyboardActions = KeyboardActions(onDone = { alPulsarListo() }),
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevacion, CircleShape, ambientColor = SombraAmbienteCampoAcceso, spotColor = SombraPuntualCampoAcceso)
            .background(Superficie, CircleShape)
            .border(grosorBorde, colorBorde, CircleShape)
            .onFocusChanged { enfocado = it.isFocused },
        decorationBox = { campo ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 54.dp)
                    .padding(start = EspacioMargen, end = if (esContrasena) EspacioFino else EspacioMargen)
            ) {
                Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(Icono19))
                Spacer(Modifier.width(EspacioMedio))
                Box(Modifier.weight(1f)) {
                    if (valor.isEmpty()) {
                        Text(marcador, color = Marcador, style = LocalTextStyle.current.merge(MarcadorCampo))
                    }
                    campo()
                }
                if (esContrasena) {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) IconosNexo.OjoTachado else IconosNexo.Ojo,
                            contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = IconoCampoAcceso,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    )
}

/** Casilla redonda de aceptación; "condiciones de uso" abre su propia hoja sin marcar la casilla. */
@Composable
private fun CasillaCondiciones(aceptado: Boolean, alCambiar: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val relleno by animateColorAsState(if (aceptado) BosqueMedio else Superficie, resorteSuave(), label = "relleno")
    val borde by animateColorAsState(if (aceptado) BosqueMedio else BordeRadio, resorteSuave(), label = "borde")
    val escalaMarca by animateFloatAsState(
        if (aceptado) 1f else 0f, resorteRebote(), label = "marca"
    )
    var verCondiciones by rememberSaveable { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            // Se ve de 40 dp pero se toca en 48
            .areaTactilSinCambiarMedida()
            .clip(CircleShape)
            .toggleable(value = aceptado, role = Role.Checkbox, onValueChange = alCambiar)
            .semantics { contentDescription = "Acepto las condiciones de uso" }
            .heightIn(min = AreaTactilMinima)
            .padding(start = EspacioMedio)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(18.dp)
                .background(relleno, CircleShape)
                .border(1.5.dp, borde, CircleShape)
        ) {
            Icon(
                IconosNexo.Marca,
                contentDescription = null,
                tint = SobreOscuro,
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = escalaMarca
                        scaleY = escalaMarca
                    }
            )
        }
        Spacer(Modifier.width(EspacioCorto))
        Text(
            "Acepto las ",
            color = TextoSecundario,
            style = LocalTextStyle.current.merge(CuerpoEnlace)
        )
    }
    Text(
        "condiciones de uso",
        color = Bosque,
        fontWeight = FontWeight.SemiBold,
        style = LocalTextStyle.current.merge(CuerpoEnlace),
        modifier = Modifier
            .padding(end = EspacioMedio)
            .areaTactilSinCambiarMedida()
            .clickable(onClickLabel = "Leer las condiciones de uso", role = Role.Button) { verCondiciones = true }
            .heightIn(min = AreaTactilMinima)
            .wrapContentHeight()
    )
    }
    if (verCondiciones) HojaCondiciones(alCerrar = { verCondiciones = false })
}

/**
 * Si cambia este texto, cambia también VERSION_TERMINOS en AuthRepositoryRemoto.
 * También se abre desde el Perfil: ahí la persona repasa las penalizaciones sin cerrar sesión.
 */
@Composable
internal fun HojaCondiciones(alCerrar: () -> Unit, textoBoton: String = "Entendido", alConfirmar: () -> Unit = alCerrar) {
    HojaNexo(titulo = "Condiciones de uso", alCerrar = alCerrar) {
        Spacer(Modifier.height(EspacioNormal))
        listOf(
            "Nexo conecta a estudiantes con tutores universitarios para coordinar tutorías. No procesa pagos ni responde por " +
                "lo que se enseñe o se acuerde en una tutoría: eso queda entre las dos personas.",
            "Usa datos verídicos en tu cuenta y trata con respeto a los demás. Podemos suspender una cuenta que incumpla esto.",
            "Si no puedes asistir a una sesión, cancélala con tiempo o acuerda un retardo por el chat. No presentarse deja una " +
                "penalización visible en tu perfil.",
            "Tres penalizaciones normales en 90 días pausan tu cuenta 7 días. Una grave la pausa 7 días, la segunda 14 y la tercera " +
                "30. En pausa no puedes enviar ni aceptar solicitudes. Tienes 7 días para pedir la revisión de una penalización."
        ).forEach { parrafo ->
            Text(parrafo, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(EspacioMedio))
        }
        val enlaces = LocalUriHandler.current
        BotonTexto("Leer la política de privacidad", { enlaces.openUri(URL_PRIVACIDAD) })
        Spacer(Modifier.height(EspacioCorto))
        BotonPrimario(textoBoton, alConfirmar)
    }
}

/** Botón principal: píldora verde satinada. */
@Composable
internal fun BotonVincularse(texto: String, cargando: Boolean, alClicar: () -> Unit, modifier: Modifier = Modifier) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(if (presionado) 0.97f else 1f, resorteSuave(), label = "escala")
    val desplazamientoFlecha by animateDpAsState(if (presionado) DesplazamientoFlechaBoton else 0.dp, resorteSuaveDp(), label = "flecha")

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .shadow(ElevacionBotonAcceso, CircleShape, ambientColor = BosqueProfundo, spotColor = SombraPuntualBotonAcceso)
            .background(pincelBosque, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaccion,
                indication = null,
                enabled = !cargando,
                role = Role.Button,
                onClick = alClicar
            )
    ) {
        AnimatedContent(
            targetState = cargando,
            transitionSpec = { fadeIn(tween(DuracionEntraBotonAcceso)) togetherWith fadeOut(tween(DuracionInstante)) },
            label = "contenidoBoton"
        ) { estaCargando ->
            if (estaCargando) {
                CircularProgressIndicator(color = SobreOscuro, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(texto, color = SobreOscuro, style = LocalTextStyle.current.merge(TextoBotonPrincipal))
                    Spacer(Modifier.width(EspacioAjustado + desplazamientoFlecha))
                    Icon(
                        IconosNexo.Adelante,
                        contentDescription = null,
                        tint = SobreOscuro,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SeparadorSocial(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(Modifier.width(52.dp).height(1.dp).background(LineaSeparadorAcceso))
        Text(
            "O CONÉCTATE CON",
            color = Marcador,
            style = LocalTextStyle.current.merge(EtiquetaSeparador),
            modifier = Modifier.padding(horizontal = EspacioMedio)
        )
        Box(Modifier.width(52.dp).height(1.dp).background(LineaSeparadorAcceso))
    }
}

@Composable
private fun BotonGoogle(habilitado: Boolean, alClicar: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            // Solo contorno, sin relleno ni sombra: así no se confunde con los campos
            .border(1.dp, BordeBotonGoogle, CircleShape)
            .clip(CircleShape)
            .clickable(enabled = habilitado, role = Role.Button, onClick = alClicar)
    ) {
        Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(EspacioAjustado))
        Text("Continuar con Google", style = LocalTextStyle.current.merge(TextoBotonSecundario))
    }
}

/** Enlace al pie: una pregunta y la acción en verde. */
@Composable
internal fun EnlaceAcceso(pregunta: String, accion: String, alClicar: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            append(pregunta)
            withStyle(SpanStyle(color = Bosque, fontWeight = FontWeight.SemiBold)) {
                append(accion)
            }
        },
        color = TextoSecundario,
        style = LocalTextStyle.current.merge(CuerpoEnlace),
        modifier = modifier
            // Área de toque de 48 dp: el enlace sigue midiendo 44 dp para el resto del diseño
            .layout { medible, restricciones ->
                val p = medible.measure(restricciones)
                val minimo = 44.dp.roundToPx()
                layout(p.width, if (p.height <= AreaTactilMinima.roundToPx()) minimo else p.height) { p.place(0, 0) }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = alClicar
            )
            .heightIn(min = AreaTactilMinima)
            .padding(horizontal = EspacioCercano, vertical = EspacioMedio)
    )
}

/* ==================== Utilidades ==================== */

/** Aparición escalonada (desvanecido + ligero ascenso) según el orden del elemento. */
@Composable
internal fun Modifier.entrada(orden: Int, iniciado: Boolean): Modifier {
    val progreso by animateFloatAsState(
        targetValue = if (iniciado) 1f else 0f,
        animationSpec = tween(DuracionEntradaAcceso, delayMillis = RetrasoEntradaAcceso + orden * PasoEntradaAcceso, easing = CurvaAparicion),
        label = "entrada$orden"
    )
    return graphicsLayer {
        alpha = progreso
        translationY = (1f - progreso) * 16.dp.toPx()
    }
}

/** Sube el elemento [cantidad] sobre el anterior y descuenta ese espacio del layout. */
private fun Modifier.solaparHaciaArriba(cantidad: Dp): Modifier = layout { medible, restricciones ->
    val colocable = medible.measure(restricciones)
    val desplazamiento = cantidad.roundToPx()
    layout(colocable.width, colocable.height - desplazamiento) {
        colocable.place(0, -desplazamiento)
    }
}

/* ==================== Vista previa ==================== */

@Preview(showBackground = true, widthDp = 390, heightDp = 884)
@Composable
private fun VistaPreviaAcceso() {
    ContenidoAcceso(
        estado = LoginViewModel.Estado(),
        alCambiarNombre = {},
        alCambiarCorreo = {},
        alCambiarContrasena = {},
        alCambiarCondiciones = {},
        alEnviar = {},
        alEntrarConGoogle = {},
        alAlternarModo = {},
        alOlvidarContrasena = {}
    )
}
