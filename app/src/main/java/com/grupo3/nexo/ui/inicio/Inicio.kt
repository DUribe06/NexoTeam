package com.grupo3.nexo.ui.inicio

import com.grupo3.nexo.ui.components.SesionEsqueleto
import com.grupo3.nexo.ui.components.Carga
import androidx.compose.animation.Crossfade
import com.grupo3.nexo.ui.theme.BosqueMedio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.SizeTransform
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.usecase.ObtenerProximasSesionesUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMisSolicitudesUseCase
import com.grupo3.nexo.domain.usecase.ObtenerDisponibilidadUseCase
import com.grupo3.nexo.domain.usecase.ObtenerTutoresUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import com.grupo3.nexo.ui.busqueda.AreaEstudio
import com.grupo3.nexo.ui.busqueda.FilaTutor
import com.grupo3.nexo.ui.components.Avatar
import com.grupo3.nexo.ui.components.BotonIcono
import com.grupo3.nexo.ui.components.BotonPrimario
import com.grupo3.nexo.ui.components.BotonSecundario
import com.grupo3.nexo.ui.components.CabeceraBosque
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.EncabezadoSeccion
import com.grupo3.nexo.ui.components.IconosClarosEnBarraDeEstado
import com.grupo3.nexo.ui.components.InsigniaNexo
import com.grupo3.nexo.ui.components.RotuloGrupo
import com.grupo3.nexo.ui.components.TextoLinea
import com.grupo3.nexo.ui.components.VeloBarraEstado
import com.grupo3.nexo.ui.components.Valoracion
import com.grupo3.nexo.ui.components.aparicion
import com.grupo3.nexo.ui.components.formatoHoras
import com.grupo3.nexo.ui.components.formatoPrecio
import com.grupo3.nexo.ui.components.icono
import com.grupo3.nexo.ui.components.pulsable
import com.grupo3.nexo.ui.components.rememberPrimeraVez
import com.grupo3.nexo.ui.components.solaparHaciaArriba
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.components.superficiePulsable
import com.grupo3.nexo.ui.theme.Bosque
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.Marcador
import com.grupo3.nexo.ui.theme.Menta
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.TinteBosque
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.grupo3.nexo.ui.components.BotonTexto
import com.grupo3.nexo.ui.components.filaAgrupada
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.grupo3.nexo.R
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.ui.components.Onda
import com.grupo3.nexo.ui.components.formatoValoracion
import com.grupo3.nexo.ui.components.universidadCorta
import com.grupo3.nexo.ui.theme.Fondo
import com.grupo3.nexo.ui.theme.InstrumentSans
import com.grupo3.nexo.ui.theme.Salvia
import com.grupo3.nexo.ui.theme.SuperficieClara
import com.grupo3.nexo.ui.theme.pincelBosque
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

/* ==================== ViewModel ==================== */

@HiltViewModel
class InicioViewModel @Inject constructor(
    obtenerTutores: ObtenerTutoresUseCase,
    obtenerProximas: ObtenerProximasSesionesUseCase,
    obtenerUsuario: ObtenerUsuarioUseCase,
    obtenerMisSolicitudes: ObtenerMisSolicitudesUseCase,
    obtenerDisponibilidad: ObtenerDisponibilidadUseCase
) : ViewModel() {

    /** Sesión próxima resuelta con el id del tutor para abrir su chat. */
    data class SesionProxima(val sesion: Sesion, val tutorId: String?, val tutor: Tutor? = null)

    data class Estado(
        val cargando: Boolean = true,
        val usuario: Usuario? = null,
        val proximas: List<SesionProxima> = emptyList(),
        /** Modo tutor: solicitudes recibidas sin responder. Modo estudiante: enviadas sin respuesta. */
        val pendientes: Int = 0,
        /** Modo tutor: horas publicadas por semana y días con al menos una (RF7). */
        val horasPublicadas: Int = 0,
        val diasPublicados: List<String> = emptyList()
    ) {
        val esTutor get() = usuario?.rol == Rol.TUTOR
    }

    val estado = combine(
        obtenerTutores(),
        // Las sesiones llegan del servidor: mientras tanto se emite null y la tarjeta muestra su esqueleto,
        // sin retener el saludo ni la insignia de pendientes, que ya están disponibles
        obtenerProximas().map<List<Sesion>, List<Sesion>?> { it }.onStart { emit(null) },
        obtenerUsuario(),
        obtenerMisSolicitudes(),
        obtenerDisponibilidad()
    ) { tutores, proximas, usuario, solicitudes, dias ->
        Estado(
            horasPublicadas = dias.sumOf { d -> d.horas.count { it.seleccionada } },
            diasPublicados = dias.filter { d -> d.horas.any { it.seleccionada } }.map { it.dia.take(3) },
            cargando = proximas == null,
            usuario = usuario,
            proximas = proximas.orEmpty().map { sesion ->
                SesionProxima(sesion, sesion.contraparteId, tutores.find { !sesion.comoTutor && it.id == sesion.contraparteId })
            },
            pendientes = solicitudes.count { it.estado == EstadoSolicitud.PENDIENTE },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())
}

/* ==================== Pantalla ==================== */

/** Íconos Phosphor Regular, la misma familia de la campana, la lupa y la barra inferior. */
private val AreaEstudio.iconoPh: Int
    get() = when (this) {
        AreaEstudio.MATEMATICAS -> R.drawable.ph_sigma
        AreaEstudio.PROGRAMACION -> R.drawable.ph_codigo
        AreaEstudio.CIENCIAS -> R.drawable.ph_matraz
        AreaEstudio.IDIOMAS -> R.drawable.ph_traducir
        AreaEstudio.DATOS -> R.drawable.ph_base_datos
        AreaEstudio.DISENO -> R.drawable.ph_paleta
        AreaEstudio.NEGOCIOS -> R.drawable.ph_maletin
    }

/**
 * Inicio (estudiante primero): saludo sobre la cabecera bosque, buscador montado sobre la onda,
 * accesos por área y próximas sesiones. Los tutores se buscan en Buscar.
 */
@Composable
fun PantallaInicio(
    alAbrirChat: (String) -> Unit,
    alAbrirPerfil: () -> Unit,
    alVerSolicitudes: () -> Unit,
    alBuscar: (area: String?) -> Unit,
    alAbrirDisponibilidad: () -> Unit,
    viewModel: InicioViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val desplazamiento = rememberScrollState()
    val cabeceraVisible by remember { derivedStateOf { desplazamiento.value < 400 } }
    IconosClarosEnBarraDeEstado(activo = cabeceraVisible)
    val iniciado = rememberPrimeraVez()

    // El contenido termina en las sesiones; el alto que sobra en pantalla se reparte en vez de
    // quedar vacío abajo: más foto en la cabecera, más aire antes de las sesiones y un margen final.
    // En teléfonos bajos no sobra nada y la pantalla simplemente se desplaza.
    var sobrante by remember { mutableIntStateOf(0) }
    val aireCabecera = (sobrante * 0.55f).roundToInt().coerceAtMost(with(LocalDensity.current) { 130.dp.roundToPx() })
    val aireSesiones = ((sobrante - aireCabecera) * 0.45f).roundToInt()
    val densidad = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val altoVisible = constraints.maxHeight
        // Sin alto mínimo, para que onSizeChanged mida el contenido y no la pantalla
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(desplazamiento)
                .onSizeChanged { tamano ->
                    val natural = tamano.height - aireCabecera - aireSesiones
                    sobrante = (altoVisible - natural).coerceAtLeast(0)
                }
                .padding(bottom = 20.dp)
        ) {
            Cabecera(
                usuario = estado.usuario,
                pendientes = estado.pendientes,
                iniciado = iniciado,
                aire = with(densidad) { aireCabecera.toDp() },
                alVerSolicitudes = alVerSolicitudes,
                alAbrirPerfil = alAbrirPerfil
            )

            // Sobre la onda va lo primero que cada modo necesita: buscar tutor o responder solicitudes
            val montado = Modifier
                .solaparHaciaArriba(30.dp)
                .padding(horizontal = 20.dp)
                .aparicion(2, iniciado)
            if (estado.esTutor) {
                AccesoSolicitudes(estado.pendientes, alVerSolicitudes, montado)
            } else {
                Buscador(alClicar = { alBuscar(null) }, modifier = montado)
            }

            // Tira de materias sin contenedores: ícono y nombre, se desliza de lado (solo para buscar)
            val tira = rememberLazyListState()
            // Debajo del control flotante, una tira del mismo molde en ambos modos: materias para buscar
            // (estudiante) o los días de la semana con horario publicado (tutor).
            if (estado.esTutor) Column(Modifier.padding(top = 12.dp).aparicion(3, iniciado)) {
                TiraDisponibilidad(estado.horasPublicadas, estado.diasPublicados, alAbrirDisponibilidad)
                Box(Modifier.fillMaxWidth().padding(top = 2.dp).height(1.dp).background(Divisor))
            } else Column(Modifier.padding(top = 12.dp).aparicion(3, iniciado)) {
                LazyRow(
                    state = tira,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.desvanecerBordes(tira)
                ) {
                    items(AreaEstudio.entries, key = { it.name }) { area ->
                        AccesoArea(area, alClicar = { alBuscar(area.name) })
                    }
                }
                Box(Modifier.fillMaxWidth().padding(top = 2.dp).height(1.dp).background(Divisor))
            }

            BarraSesion(Modifier.padding(top = 6.dp)) // Provisional: doble toque cambia de fase
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp + with(densidad) { aireSesiones.toDp() })
                    .aparicion(4, iniciado)
            ) {
                EncabezadoSeccion(
                    titulo = if (estado.cargando || estado.proximas.size > 1) "Tus próximas sesiones" else "Tu próxima sesión",
                    accion = if (estado.proximas.isNotEmpty()) "Ver todas" else null,
                    alAccion = alVerSolicitudes
                )
                Spacer(Modifier.height(2.dp))
                // Mientras llegan los datos, la silueta de la tarjeta; luego, la tarjeta o el estado vacío.
                // Si la lista se vacía, la tarjeta que se desvanece conserva la última con sesiones
                // (si no, leería la lista vacía durante el fundido y cerraría la app).
                val ultimasProximas = remember { mutableStateOf(estado.proximas) }
                if (estado.proximas.isNotEmpty()) ultimasProximas.value = estado.proximas
                Crossfade(
                    targetState = when {
                        estado.cargando -> Carga.CARGANDO
                        estado.proximas.isEmpty() -> Carga.VACIO
                        else -> Carga.LISTO
                    },
                    animationSpec = tween(280),
                    label = "proximas"
                ) { carga ->
                    when (carga) {
                        Carga.CARGANDO -> SesionEsqueleto()
                        Carga.VACIO -> SinSesiones(alBuscar = { alBuscar(null) })
                        Carga.LISTO -> ultimasProximas.value.takeIf { it.isNotEmpty() }?.let { TarjetaProximas(it, alAbrirChat) }
                    }
                }
            }
        }
        VeloBarraEstado(visible = !cabeceraVisible)
    }
}

/**
 * Desvanece los bordes de una tira que se desliza de lado, solo donde hay más contenido:
 * así el último elemento no queda cortado a mitad de palabra sino que se pierde suavemente.
 */
private fun Modifier.desvanecerBordes(estado: LazyListState, ancho: Dp = 32.dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val borde = (ancho.toPx() / size.width).coerceAtMost(0.5f)
            val inicio = if (estado.canScrollBackward) borde else 0f
            val fin = if (estado.canScrollForward) 1f - borde else 1f
            drawRect(
                Brush.horizontalGradient(
                    0f to (if (inicio > 0f) Color.Transparent else Color.Black),
                    inicio to Color.Black,
                    fin to Color.Black,
                    1f to (if (fin < 1f) Color.Transparent else Color.Black)
                ),
                blendMode = BlendMode.DstIn
            )
        }

/* ==================== Piezas ==================== */

@Composable
private fun Cabecera(
    usuario: Usuario?,
    pendientes: Int,
    iniciado: Boolean,
    aire: Dp,
    alVerSolicitudes: () -> Unit,
    alAbrirPerfil: () -> Unit
) {
    val comelon = rememberEstadoComelon()
    val (modSaludo, layoutSaludo) = remember { saludoComelon(comelon, 0) }
    val (modPregunta, layoutPregunta) = remember { saludoComelon(comelon, 1) }
    Box(Modifier.cabeceraComelon(comelon)) {
        CabeceraBosque(conFoto = true, alturaOnda = 58.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 8.dp)
            ) {
                MarcaComelon(comelon)
                Spacer(Modifier.weight(1f))
                BotonIcono(
                    ImageVector.vectorResource(R.drawable.ph_campana),
                    descripcion = if (pendientes > 0) "Solicitudes, $pendientes pendientes" else "Solicitudes",
                    alClicar = alVerSolicitudes,
                    sobreOscuro = true,
                    insignia = pendientes
                )
                Spacer(Modifier.width(4.dp))
                // Anillo blanco fino: hace juego con la insignia blanca del logo
                Avatar(
                    nombre = usuario?.nombre ?: "Nexo",
                    tamano = 36.dp,
                    anillo = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .pulsable(etiqueta = "Abrir perfil", alClicar = alAbrirPerfil)
                )
            }

            // El alto sobrante de la pantalla se ve como más foto entre la marca y el saludo
            Spacer(Modifier.height(aire))
            val nombre = usuario?.nombre?.substringBefore(' ').orEmpty()
            Column(
                Modifier
                    .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 20.dp)
                    .aparicion(0, iniciado)
            ) {
                Text(
                    if (nombre.isBlank()) "Hola" else "Hola, $nombre",
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    onTextLayout = layoutSaludo,
                    modifier = modSaludo.semantics { heading() }
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (usuario?.rol == Rol.TUTOR) "Tus estudiantes te esperan." else "¿Qué quieres aprender hoy?",
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyLarge,
                    onTextLayout = layoutPregunta,
                    modifier = modPregunta
                )
            }
        }
        // La onda del lienzo que corta la foto, donde se apoya el buscador
        Onda(
            Fondo,
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(48.dp)
        )
        // Migas del logo comelón y puntos de bonificación
        CapaMigasComelon(comelon, Modifier.matchParentSize())
    }
}

/** Botón con forma de campo: abre Buscar (no se escribe aquí para no duplicar la pantalla). */
@Composable
private fun Buscador(alClicar: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(27.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(14.dp, forma, ambientColor = BosqueProfundo.copy(alpha = 0.22f), spotColor = BosqueProfundo.copy(alpha = 0.38f))
            .superficiePulsable(forma, etiqueta = "Buscar tutores", alClicar = alClicar)
            .padding(start = 20.dp, end = 7.dp)
    ) {
        Icon(painterResource(R.drawable.ph_lupa), contentDescription = null, tint = Bosque.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            "Busca un tutor o una materia",
            color = Marcador,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        // Botón de ir: verde bosque como los eslabones, con la flecha Phosphor de la misma familia
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(pincelBosque, CircleShape)
        ) {
            Icon(painterResource(R.drawable.ph_flecha), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/** Acceso por área: ícono y nombre, sin contenedor. */
@Composable
private fun AccesoArea(area: AreaEstudio, alClicar: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 62.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "Buscar en ${area.etiqueta}", role = Role.Button, onClick = alClicar)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Icon(painterResource(area.iconoPh), contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(5.dp))
        Text(
            area.etiqueta,
            color = BosqueProfundo,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

private val Modalidad.iconoPh: Int
    get() = if (this == Modalidad.VIRTUAL) R.drawable.ph_video else R.drawable.ph_ubicacion

/**
 * Próxima sesión con la hora en grande y, detrás, las siguientes como capas apiladas: cada una más
 * angosta y más translúcida que la de arriba, para que se lean como "lo que viene después".
 */

/** Salida suave con frenada larga: el movimiento arranca rápido y se asienta sin rebote. */
private val EnfaticaPila = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
private fun TarjetaProximas(proximas: List<InicioViewModel.SesionProxima>, alAbrirChat: (String) -> Unit) {
    // Tocar una capa de atrás la pasa al frente; las demás se reacomodan en orden (como un mazo de cartas)
    var inicio by remember(proximas) { mutableIntStateOf(0) }
    // Al salir de la pantalla (otra pestaña, otra pantalla o la app en segundo plano) vuelve la más próxima
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { inicio = 0 }
    val orden = remember(proximas, inicio) { proximas.drop(inicio) + proximas.take(inicio) }
    val primera = orden.first()
    val s = primera.sesion
    val formaFrente = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth()) {
        // Sin sombra (la única sombra de la pantalla es la del buscador): el borde fino la recorta del fondo
        Column(
            Modifier
                .zIndex(3f)
                .fillMaxWidth()
                // Toda la tarjeta se hunde al presionar y abre el chat, igual que su botón
                .then(
                    primera.tutorId?.let { id -> Modifier.pulsable(etiqueta = "Escribir a ${s.conQuien}") { alAbrirChat(id) } } ?: Modifier
                )
                .background(TinteBosque, formaFrente)
                .border(1.dp, BosqueAcento.copy(alpha = 0.14f), formaFrente)
                .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)
        ) {
          AnimatedContent(
            targetState = primera,
            // La elegida SUBE desde abajo y crece hasta su sitio; la que estaba al frente BAJA, se achica y se apaga
            transitionSpec = {
                (fadeIn(tween(240, delayMillis = 90)) +
                    slideInVertically(tween(460, easing = EnfaticaPila)) { it / 4 } +
                    scaleIn(tween(460, easing = EnfaticaPila), initialScale = .94f)) togetherWith
                    (fadeOut(tween(170)) +
                        slideOutVertically(tween(380, easing = EnfaticaPila)) { it / 5 } +
                        scaleOut(tween(380, easing = EnfaticaPila), targetScale = .94f)) using
                    SizeTransform(clip = false) { _, _ -> tween(420, easing = EnfaticaPila) }
            },
            label = "sesionFrente"
          ) { primera ->
            val s = primera.sesion
            Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    fechaLarga(s.fecha),
                    color = BosqueAcento,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                // Modalidad como texto con ícono, sin pastilla: el único botón blanco es "Abrir chat"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(24.dp)
                ) {
                    Icon(painterResource(s.modalidad.iconoPh), contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.modalidad.etiqueta, color = BosqueAcento, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold))
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 12.dp)
                    .height(IntrinsicSize.Min)
            ) {
                Text(
                    s.hora,
                    color = BosqueProfundo,
                    style = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 42.sp, lineHeight = 46.sp, letterSpacing = (-1.6).sp).merge(CifrasTabulares)
                )
                Spacer(Modifier.width(14.dp))
                Box(
                    Modifier
                        .padding(vertical = 4.dp)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(BosqueAcento.copy(alpha = 0.18f))
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    TextoLinea(s.materia, MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp), BosqueProfundo)
                    TextoLinea(
                        "${formatoHoras(s.duracionHoras)} · hasta las ${horaFin(s.hora, s.duracionHoras)}",
                        MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium),
                        Salvia
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(s.conQuien, tamano = 28.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    TextoLinea(s.conQuien, MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), BosqueProfundo)
                    primera.tutor?.let { t ->
                        TextoLinea(
                            "${universidadCorta(t.universidad)} · ${formatoValoracion(t.rating)}",
                            MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            Salvia
                        )
                    }
                }
                primera.tutorId?.let { id ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .pulsable(etiqueta = "Escribir a ${s.conQuien}") { alAbrirChat(id) }
                            .height(40.dp)
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .padding(horizontal = 15.dp)
                    ) {
                        Icon(painterResource(R.drawable.ph_chat), contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Abrir chat", color = BosqueProfundo, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
                    }
                }
            }
            }
          }
        }

        // Capas de las siguientes sesiones: más angostas y más translúcidas hacia abajo
        orden.drop(1).take(2).forEachIndexed { n, p ->
            val fondo = TinteBosque.copy(alpha = if (n == 0) 0.62f else 0.34f)
            val tinta = if (n == 0) 0.88f else 0.62f
            // Cascada: tras la tarjeta principal, cada capa sale de detrás de la anterior con un resorte suave
            var salio by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(260L + n * 110L); salio = true }
            val salida by animateFloatAsState(
                if (salio) 1f else 0f,
                spring(dampingRatio = 0.68f, stiffness = 320f),
                label = "capa$n"
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .solaparHaciaArriba(12.dp)
                    .zIndex(2f - n)
                    .padding(horizontal = (8 * (n + 1)).dp)
                    .fillMaxWidth()
                    .height(if (n == 0) 46.dp else 44.dp)
                    .pulsable(etiqueta = "Pasar al frente la sesión de ${p.sesion.materia}") { inicio = (inicio + 1 + n) % proximas.size }
                    .background(fondo, RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp)
                    .graphicsLayer {
                        // Invisible mientras sigue detrás de la capa anterior: su texto no se transparenta encima
                        alpha = tinta * ((salida - .45f) / .45f).coerceIn(0f, 1f)
                        translationY = -(1f - salida) * 46.dp.toPx()
                    }
            ) {
                AnimatedContent(
                    targetState = p,
                    // La que llega a la capa viene de arriba (bajó desde el frente); la que sube sale hacia arriba
                    transitionSpec = {
                        (fadeIn(tween(240, delayMillis = 80)) + slideInVertically(tween(420, easing = EnfaticaPila)) { -it / 2 }) togetherWith
                            (fadeOut(tween(140)) + slideOutVertically(tween(300, easing = EnfaticaPila)) { -it / 2 })
                    },
                    label = "capa${n}Contenido"
                ) { q ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${fechaCorta(q.sesion.fecha)} · ${q.sesion.hora}",
                    color = BosqueProfundo,
                    style = MaterialTheme.typography.titleSmall.merge(CifrasTabulares)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "${q.sesion.materia} · ${q.sesion.conQuien}",
                    color = BosqueProfundo,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Icon(painterResource(q.sesion.modalidad.iconoPh), contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/* Fechas de las sesiones: los datos vienen como "24 oct"; se muestran como "Sábado 24 de octubre" y "Lun 26". */
private val mesesCortos = mapOf(
    "ene" to 1, "feb" to 2, "mar" to 3, "abr" to 4, "may" to 5, "jun" to 6,
    "jul" to 7, "ago" to 8, "sep" to 9, "sept" to 9, "oct" to 10, "nov" to 11, "dic" to 12
)
private val espanol = Locale("es", "CO")

private fun fechaDe(texto: String): LocalDate? {
    val partes = texto.trim().split(" ")
    val dia = partes.getOrNull(0)?.toIntOrNull() ?: return null
    val mes = mesesCortos[partes.getOrNull(1)?.lowercase()?.trimEnd('.')] ?: return null
    val hoy = LocalDate.now()
    val fecha = runCatching { LocalDate.of(hoy.year, mes, dia) }.getOrNull() ?: return null
    return if (fecha.isBefore(hoy.minusMonths(6))) fecha.plusYears(1) else fecha
}

private fun String.mayusculaInicial() = replaceFirstChar { it.titlecase(espanol) }

private fun fechaLarga(texto: String): String = fechaDe(texto)?.let {
    "${it.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, espanol).mayusculaInicial()} ${it.dayOfMonth} de " +
        it.month.getDisplayName(java.time.format.TextStyle.FULL, espanol)
} ?: texto

private fun fechaCorta(texto: String): String = fechaDe(texto)?.let {
    "${it.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, espanol).trimEnd('.').mayusculaInicial()} ${it.dayOfMonth}"
} ?: texto

private fun horaFin(hora: String, horas: Double): String {
    val partes = hora.split(":").mapNotNull { it.toIntOrNull() }
    if (partes.size != 2) return hora
    val total = partes[0] * 60 + partes[1] + (horas * 60).roundToInt()
    return String.format(Locale.US, "%02d:%02d", (total / 60) % 24, total % 60)
}

@Composable
private fun SinSesiones(alBuscar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .superficie(RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(TinteBosque, RoundedCornerShape(10.dp))
        ) {
            Icon(Icons.Rounded.EventAvailable, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Sin sesiones agendadas", color = TextoPrimario, style = MaterialTheme.typography.titleMedium)
            Text("Elige un tutor y solicita tu primera sesión.", color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(8.dp))
        BotonSecundario("Buscar", alBuscar, compacto = true)
    }
}

/**
 * Modo tutor, en el lugar de la tira de materias: la misma forma (ícono arriba, nombre abajo, sin
 * contenedores) con los siete días. Los que tienen horario publicado se marcan; tocar la tira edita
 * la disponibilidad. Sin horas publicadas invita a publicarlas: sin ellas no aparece en las búsquedas.
 */
@Composable
private fun TiraDisponibilidad(horas: Int, dias: List<String>, alEditar: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Editar disponibilidad", role = Role.Button, onClick = alEditar)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 22.dp).heightIn(min = 32.dp)
        ) {
            Text(
                if (horas == 0) "Publica tus horarios para aparecer en las búsquedas"
                else "Tu disponibilidad · ${if (horas == 1) "1 hora" else "$horas horas"} a la semana",
                color = Salvia,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (horas == 0) "Publicar" else "Editar",
                color = BosqueAcento,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            )
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
            listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom").forEach { d ->
                val activo = d in dias
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .background(if (activo) TinteBosque else Color.Transparent, CircleShape)
                            .border(1.dp, if (activo) BosqueAcento.copy(alpha = 0.30f) else Divisor, CircleShape)
                    ) {
                        if (activo) Box(Modifier.size(8.dp).background(BosqueAcento, CircleShape))
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        d,
                        color = if (activo) BosqueProfundo else TextoTercero,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp, fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Medium),
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Modo tutor: en el lugar del buscador, con su misma forma, el acceso a las solicitudes por responder.
 * Sin pendientes se lee "Todo al día" y sigue llevando a la lista.
 */
@Composable
private fun AccesoSolicitudes(cantidad: Int, alRevisar: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(27.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(14.dp, forma, ambientColor = BosqueProfundo.copy(alpha = 0.22f), spotColor = BosqueProfundo.copy(alpha = 0.38f))
            .superficiePulsable(forma, etiqueta = "Ver solicitudes recibidas", alClicar = alRevisar)
            .padding(start = 20.dp, end = 7.dp)
    ) {
        Icon(painterResource(R.drawable.ph_campana), contentDescription = null, tint = Bosque.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            when (cantidad) {
                0 -> "Todo al día: sin solicitudes nuevas"
                1 -> "1 solicitud por responder"
                else -> "$cantidad solicitudes por responder"
            },
            color = if (cantidad > 0) BosqueProfundo else Marcador,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (cantidad > 0) FontWeight.SemiBold else FontWeight.Normal),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(pincelBosque, CircleShape)
        ) {
            Icon(painterResource(R.drawable.ph_flecha), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

