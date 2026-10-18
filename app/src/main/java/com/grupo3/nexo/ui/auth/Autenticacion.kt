package com.grupo3.nexo.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.R
import com.grupo3.nexo.domain.usecase.IniciarSesionGoogleUseCase
import com.grupo3.nexo.domain.usecase.IniciarSesionUseCase
import com.grupo3.nexo.domain.usecase.RegistrarseUseCase
import com.grupo3.nexo.ui.components.IconosClarosEnBarraDeEstado
import com.grupo3.nexo.ui.components.Onda
import com.grupo3.nexo.ui.theme.Bosque
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.ErrorBosque
import com.grupo3.nexo.ui.theme.InstrumentSans
import com.grupo3.nexo.ui.theme.Lienzo
import com.grupo3.nexo.ui.theme.Salvia
import com.grupo3.nexo.ui.theme.pincelBosque
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.grupo3.nexo.domain.model.Rol
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import javax.inject.Inject

/* ==================== ViewModel ==================== */

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val iniciarSesion: IniciarSesionUseCase,
    private val registrarse: RegistrarseUseCase,
    private val iniciarSesionGoogle: IniciarSesionGoogleUseCase
) : ViewModel() {

    data class Estado(
        val nombre: String = "",
        val correo: String = "",
        val contrasena: String = "",
        val aceptaCondiciones: Boolean = false,
        /** Rol con el que se registra; un tutor queda pendiente de verificación (RF1). */
        val rol: Rol = Rol.ESTUDIANTE,
        val esRegistro: Boolean = false,
        val cargando: Boolean = false,
        val error: String? = null,
        val autenticado: Boolean = false
    )

    private val _estado = MutableStateFlow(Estado())
    val estado = _estado.asStateFlow()

    fun cambiarNombre(valor: String) = _estado.update { it.copy(nombre = valor, error = null) }
    fun cambiarCorreo(valor: String) = _estado.update { it.copy(correo = valor.trim(), error = null) }
    fun cambiarContrasena(valor: String) = _estado.update { it.copy(contrasena = valor, error = null) }
    fun cambiarCondiciones(valor: Boolean) = _estado.update { it.copy(aceptaCondiciones = valor, error = null) }

    fun cambiarRol(valor: Rol) = _estado.update { it.copy(rol = valor, error = null) }

    fun alternarModo() = _estado.update { it.copy(esRegistro = !it.esRegistro, error = null) }

    fun entrar() {
        val e = _estado.value
        if (e.cargando) return
        val error = when {
            e.esRegistro && e.nombre.isBlank() -> "Escribe tu nombre o un alias"
            e.correo.isBlank() || e.contrasena.isBlank() -> "Completa todos los campos para continuar"
            !e.correo.contains("@") -> "Escribe un correo válido, por ejemplo correo@ejemplo.com"
            e.contrasena.length < 4 -> "La contraseña debe tener al menos 4 caracteres"
            e.esRegistro && !e.aceptaCondiciones -> "Acepta las condiciones de uso para vincularte"
            else -> null
        }
        if (error != null) {
            _estado.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val exito = if (e.esRegistro) registrarse(e.nombre.trim(), e.correo, e.contrasena, e.rol)
                        else iniciarSesion(e.correo, e.contrasena)
            _estado.update {
                it.copy(
                    cargando = false,
                    autenticado = exito,
                    error = if (exito) null else "No pudimos validar tus datos. Revisa el correo y la contraseña."
                )
            }
        }
    }

    fun entrarGoogle() {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            iniciarSesionGoogle()
            _estado.update { it.copy(cargando = false, autenticado = true) }
        }
    }

    fun consumirNavegacion() = _estado.update { it.copy(autenticado = false) }
}

/* ==================== Pantalla ==================== */

/**
 * Acceso a Nexo: registro con nombre, correo y contraseña,
 * inicio de sesión y acceso con Google (HU-01/02).
 * Diseño de referencia: docs/diseno/stitch-login.
 */
@Composable
fun PantallaLogin(
    alEntrar: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(estado.autenticado) {
        if (estado.autenticado) {
            alEntrar()
            viewModel.consumirNavegacion()
        }
    }

    IconosClarosEnBarraDeEstado()

    ContenidoAcceso(
        estado = estado,
        alCambiarNombre = viewModel::cambiarNombre,
        alCambiarCorreo = viewModel::cambiarCorreo,
        alCambiarContrasena = viewModel::cambiarContrasena,
        alCambiarCondiciones = viewModel::cambiarCondiciones,
        alEnviar = viewModel::entrar,
        alEntrarConGoogle = viewModel::entrarGoogle,
        alAlternarModo = viewModel::alternarModo
    )
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
    alAlternarModo: () -> Unit
) {
    val focos = LocalFocusManager.current
    val enviar = {
        focos.clearFocus()
        alEnviar()
    }

    // Entrada escalonada: solo la primera vez (no se repite al rotar la pantalla).
    var iniciado by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { iniciado = true }

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
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
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
                            .padding(horizontal = 28.dp)
                    ) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Nexo",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.6).sp,
                            modifier = Modifier
                                .semantics { heading() }
                                .entrada(1, iniciado)
                        )
                        Spacer(Modifier.height(4.dp))
                        AnimatedContent(
                            targetState = estado.esRegistro,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                            label = "subtitulo",
                            modifier = Modifier.entrada(1, iniciado)
                        ) { esRegistro ->
                            Text(
                                if (esRegistro) "Acceso a la red colaborativa de tutorías."
                                else "Qué bueno verte de nuevo. Inicia sesión.",
                                color = Salvia,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(max = 300.dp)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

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
                                icono = Icons.Outlined.Person,
                                teclado = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .entrada(2, iniciado)
                            )
                        }
                        CampoPildora(
                            valor = estado.correo,
                            alCambiar = alCambiarCorreo,
                            marcador = "correo@ejemplo.com",
                            icono = Icons.Outlined.Email,
                            teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            modifier = Modifier.entrada(3, iniciado)
                        )
                        Spacer(Modifier.height(12.dp))
                        CampoPildora(
                            valor = estado.contrasena,
                            alCambiar = alCambiarContrasena,
                            marcador = if (estado.esRegistro) "Contraseña segura" else "Contraseña",
                            icono = Icons.Outlined.Lock,
                            esContrasena = true,
                            teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            alPulsarListo = enviar,
                            modifier = Modifier.entrada(4, iniciado)
                        )

                        AnimatedVisibility(
                            visible = estado.esRegistro,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(Modifier.entrada(5, iniciado)) {
                                CasillaCondiciones(
                                    aceptado = estado.aceptaCondiciones,
                                    alCambiar = alCambiarCondiciones,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = estado.error != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Text(
                                estado.error.orEmpty(),
                                color = ErrorBosque,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        BotonVincularse(
                            texto = if (estado.esRegistro) "Vincularse" else "Iniciar sesión",
                            cargando = estado.cargando,
                            alClicar = enviar,
                            modifier = Modifier.entrada(5, iniciado)
                        )

                        Spacer(Modifier.height(26.dp))
                        SeparadorSocial(Modifier.entrada(6, iniciado))
                        Spacer(Modifier.height(14.dp))
                        BotonGoogle(
                            habilitado = !estado.cargando,
                            alClicar = alEntrarConGoogle,
                            modifier = Modifier.entrada(6, iniciado)
                        )

                        Spacer(Modifier.height(10.dp))
                        EnlaceCambioModo(
                            esRegistro = estado.esRegistro,
                            alClicar = alAlternarModo,
                            modifier = Modifier.entrada(7, iniciado)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
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
    val zoom by rememberInfiniteTransition(label = "portada").animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "zoom"
    )
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
                            BosqueProfundo.copy(alpha = 0.60f),
                            BosqueProfundo.copy(alpha = 0.25f),
                            BosqueProfundo.copy(alpha = 0.55f)
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
    val forma = RoundedCornerShape(24.dp)
    val flotacion by rememberInfiniteTransition(label = "logo").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flotacion"
    )
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
                    rebote.animateTo(0.82f, tween(90, easing = FastOutSlowInEasing))
                    rebote.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 500f))
                }
            }
            .size(84.dp)
            .shadow(20.dp, forma, ambientColor = BosqueProfundo, spotColor = BosqueProfundo.copy(alpha = 0.35f))
            .background(Color.White, forma)
            .border(1.dp, Color.White.copy(alpha = 0.95f), forma)
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
private fun CampoPildora(
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
        if (enfocado) BosqueAcento.copy(alpha = 0.55f) else Bosque.copy(alpha = 0.10f), label = "borde"
    )
    val grosorBorde by animateDpAsState(if (enfocado) 1.5.dp else 1.dp, label = "grosor")
    val elevacion by animateDpAsState(if (enfocado) 10.dp else 3.dp, label = "elevacion")
    val colorIcono by animateColorAsState(
        if (enfocado) BosqueAcento else Bosque.copy(alpha = 0.55f), label = "icono"
    )

    BasicTextField(
        value = valor,
        onValueChange = alCambiar,
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = InstrumentSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = BosqueProfundo
        ),
        cursorBrush = SolidColor(BosqueAcento),
        visualTransformation = if (esContrasena && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = teclado,
        keyboardActions = KeyboardActions(onDone = { alPulsarListo() }),
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevacion, CircleShape, ambientColor = BosqueProfundo.copy(alpha = 0.4f), spotColor = BosqueProfundo.copy(alpha = 0.18f))
            .background(Color.White, CircleShape)
            .border(grosorBorde, colorBorde, CircleShape)
            .onFocusChanged { enfocado = it.isFocused },
        decorationBox = { campo ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 54.dp)
                    .padding(start = 20.dp, end = if (esContrasena) 6.dp else 20.dp)
            ) {
                Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (valor.isEmpty()) {
                        Text(marcador, color = Bosque.copy(alpha = 0.50f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    campo()
                }
                if (esContrasena) {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = Bosque.copy(alpha = 0.55f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    )
}

/** Casilla redonda de aceptación de condiciones (toda la fila es pulsable). */
@Composable
private fun CasillaCondiciones(aceptado: Boolean, alCambiar: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val relleno by animateColorAsState(if (aceptado) BosqueMedio else Color.White, label = "relleno")
    val borde by animateColorAsState(if (aceptado) BosqueMedio else Bosque.copy(alpha = 0.3f), label = "borde")
    val escalaMarca by animateFloatAsState(
        if (aceptado) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "marca"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .toggleable(value = aceptado, role = Role.Checkbox, onValueChange = alCambiar)
            .heightIn(min = 40.dp)
            .padding(horizontal = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(18.dp)
                .background(relleno, CircleShape)
                .border(1.5.dp, borde, CircleShape)
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = escalaMarca
                        scaleY = escalaMarca
                    }
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            buildAnnotatedString {
                append("Acepto las ")
                withStyle(
                    SpanStyle(color = Bosque, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)
                ) { append("condiciones de uso") }
            },
            color = Salvia,
            fontSize = 13.sp
        )
    }
}

/** Botón principal: píldora verde satinada. */
@Composable
private fun BotonVincularse(texto: String, cargando: Boolean, alClicar: () -> Unit, modifier: Modifier = Modifier) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(if (presionado) 0.97f else 1f, label = "escala")
    val desplazamientoFlecha by animateDpAsState(if (presionado) 4.dp else 0.dp, label = "flecha")

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .shadow(16.dp, CircleShape, ambientColor = BosqueProfundo, spotColor = BosqueProfundo.copy(alpha = 0.6f))
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
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "contenidoBoton"
        ) { estaCargando ->
            if (estaCargando) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(texto, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.3.sp)
                    Spacer(Modifier.width(10.dp + desplazamientoFlecha))
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
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
        Box(Modifier.width(52.dp).height(1.dp).background(Bosque.copy(alpha = 0.15f)))
        Text(
            "O CONÉCTATE CON",
            color = Bosque.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(Modifier.width(52.dp).height(1.dp).background(Bosque.copy(alpha = 0.15f)))
    }
}

@Composable
private fun BotonGoogle(habilitado: Boolean, alClicar: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            // Solo contorno, sin relleno ni sombra: así no se confunde con los campos
            .border(1.dp, Bosque.copy(alpha = 0.18f), CircleShape)
            .clip(CircleShape)
            .clickable(enabled = habilitado, role = Role.Button, onClick = alClicar)
    ) {
        Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text("Continuar con Google", fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EnlaceCambioModo(esRegistro: Boolean, alClicar: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            append(if (esRegistro) "¿Ya perteneces a Nexo? " else "¿Aún no tienes cuenta? ")
            withStyle(SpanStyle(color = Bosque, fontWeight = FontWeight.SemiBold)) {
                append(if (esRegistro) "Iniciar sesión" else "Vincúlate")
            }
        },
        color = Salvia,
        fontSize = 13.sp,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = alClicar
            )
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

/* ==================== Utilidades ==================== */

/** Aparición escalonada (desvanecido + ligero ascenso) según el orden del elemento. */
@Composable
private fun Modifier.entrada(orden: Int, iniciado: Boolean): Modifier {
    val progreso by animateFloatAsState(
        targetValue = if (iniciado) 1f else 0f,
        animationSpec = tween(520, delayMillis = 80 + orden * 70, easing = EaseOutCubic),
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
        alAlternarModo = {}
    )
}
