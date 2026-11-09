package com.grupo3.nexo.ui.inicio

import com.grupo3.nexo.domain.model.Solicitud
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.IconButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.grupo3.nexo.R
import com.grupo3.nexo.domain.model.AreaEstudio
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.model.diasPublicados
import com.grupo3.nexo.domain.model.horaFin
import com.grupo3.nexo.domain.model.horasPublicadas
import com.grupo3.nexo.domain.model.primerNombre
import com.grupo3.nexo.ui.components.Avatar
import com.grupo3.nexo.ui.components.AvisoNexo
import com.grupo3.nexo.ui.components.BotonIcono
import com.grupo3.nexo.ui.components.BotonSecundario
import com.grupo3.nexo.ui.components.CabeceraBosque
import com.grupo3.nexo.ui.components.Carga
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.EncabezadoSeccion
import com.grupo3.nexo.ui.components.ErrorDeLista
import com.grupo3.nexo.ui.components.IconosClarosEnBarraDeEstado
import com.grupo3.nexo.ui.components.IconosNexo
import com.grupo3.nexo.ui.components.Onda
import com.grupo3.nexo.ui.components.PildoraAbrirChat
import com.grupo3.nexo.ui.components.SesionEsqueleto
import com.grupo3.nexo.ui.components.TextoLinea
import com.grupo3.nexo.ui.components.VeloBarraEstado
import com.grupo3.nexo.ui.components.aparicion
import com.grupo3.nexo.ui.components.areaTactilSinCambiarMedida
import com.grupo3.nexo.ui.components.avisoFallo
import com.grupo3.nexo.ui.components.cargaDe
import com.grupo3.nexo.ui.components.fechaCorta
import com.grupo3.nexo.ui.components.fechaLarga
import com.grupo3.nexo.ui.components.formatoHora
import com.grupo3.nexo.ui.components.formatoHoras
import com.grupo3.nexo.ui.components.formatoValoracion
import com.grupo3.nexo.ui.components.mostrarError
import com.grupo3.nexo.ui.components.plural
import com.grupo3.nexo.ui.components.pulsable
import com.grupo3.nexo.ui.components.rememberPrimeraVez
import com.grupo3.nexo.ui.components.solaparHaciaArriba
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.components.superficiePulsable
import com.grupo3.nexo.ui.components.universidadCorta
import com.grupo3.nexo.ui.theme.IconoNormal
import com.grupo3.nexo.ui.theme.EspacioSuelto
import com.grupo3.nexo.ui.theme.AltoBuscador
import com.grupo3.nexo.ui.theme.AltoPildora
import com.grupo3.nexo.ui.theme.AnilloSobreOscuro
import com.grupo3.nexo.ui.theme.AreaTactilMinima
import com.grupo3.nexo.ui.theme.BordeAcento
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.CifraAcceso
import com.grupo3.nexo.ui.theme.CifraHora
import com.grupo3.nexo.ui.theme.CuerpoNombre
import com.grupo3.nexo.ui.theme.CuerpoSesion
import com.grupo3.nexo.ui.theme.CurvaEnfatizada
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.DuracionCapaSeApaga
import com.grupo3.nexo.ui.theme.DuracionLarga
import com.grupo3.nexo.ui.theme.DuracionNormal
import com.grupo3.nexo.ui.theme.DuracionPilaAparece
import com.grupo3.nexo.ui.theme.DuracionPilaEntra
import com.grupo3.nexo.ui.theme.DuracionPilaSeApaga
import com.grupo3.nexo.ui.theme.DuracionPilaTamano
import com.grupo3.nexo.ui.theme.ElevacionTarjeta
import com.grupo3.nexo.ui.theme.EnfasisFuerte
import com.grupo3.nexo.ui.theme.EnfasisMedio
import com.grupo3.nexo.ui.theme.EnfasisNormal
import com.grupo3.nexo.ui.theme.EscalaPilaSaliente
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioAmplio
import com.grupo3.nexo.ui.theme.EspacioBotonLateral
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioFino
import com.grupo3.nexo.ui.theme.EspacioHolgado
import com.grupo3.nexo.ui.theme.EspacioIcono
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioMicro
import com.grupo3.nexo.ui.theme.EspacioMinimo
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.EspacioPildora
import com.grupo3.nexo.ui.theme.EtiquetaArea
import com.grupo3.nexo.ui.theme.EtiquetaBoton
import com.grupo3.nexo.ui.theme.EtiquetaFecha
import com.grupo3.nexo.ui.theme.Fondo
import com.grupo3.nexo.ui.theme.FondoAcentoTenue
import com.grupo3.nexo.ui.theme.FormaBoton
import com.grupo3.nexo.ui.theme.FormaCampoAlto
import com.grupo3.nexo.ui.theme.FormaCapaInferior
import com.grupo3.nexo.ui.theme.FormaControl
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.theme.Icono14
import com.grupo3.nexo.ui.theme.Icono16
import com.grupo3.nexo.ui.theme.Icono17
import com.grupo3.nexo.ui.theme.Icono18
import com.grupo3.nexo.ui.theme.Icono20
import com.grupo3.nexo.ui.theme.Icono22
import com.grupo3.nexo.ui.theme.Icono26
import com.grupo3.nexo.ui.theme.IconoSecundario
import com.grupo3.nexo.ui.theme.LocalAnimacionesReducidas
import com.grupo3.nexo.ui.theme.Marcador
import com.grupo3.nexo.ui.theme.PasoCascadaPila
import com.grupo3.nexo.ui.theme.RetrasoCapaPila
import com.grupo3.nexo.ui.theme.RetrasoCascadaPila
import com.grupo3.nexo.ui.theme.RetrasoPilaAparece
import com.grupo3.nexo.ui.theme.SobreOscuro
import com.grupo3.nexo.ui.theme.SobreOscuroSuave
import com.grupo3.nexo.ui.theme.SombraAmbiente
import com.grupo3.nexo.ui.theme.SombraPuntual
import com.grupo3.nexo.ui.theme.Superficie
import com.grupo3.nexo.ui.theme.TamanoAvatarBarra
import com.grupo3.nexo.ui.theme.TamanoAvatarMini
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoSobreTinte
import com.grupo3.nexo.ui.theme.TinteBosque
import com.grupo3.nexo.ui.theme.TintePilaCerca
import com.grupo3.nexo.ui.theme.TintePilaLejos
import com.grupo3.nexo.ui.theme.TituloMateria
import com.grupo3.nexo.ui.theme.pincelBosque
import com.grupo3.nexo.ui.theme.resorteCascada
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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
    alAbrirHistorial: () -> Unit,
    alAbrirResenas: () -> Unit,
    alAbrirMaterias: () -> Unit,
    viewModel: InicioViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    // La conversación se resuelve con la contraparte de la sesión y se navega con el id del chat
    val abrirChat: (Sesion) -> Unit = { sesion ->
        viewModel.abrirChat(sesion, alAbrirChat) { fallo -> alcance.launch { avisos.mostrarError(avisoFallo("abrir el chat", fallo)) } }
    }
    var escribiendoPin by rememberSaveable { mutableStateOf(false) }
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
                .padding(bottom = EspacioMargen)
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
                .padding(horizontal = EspacioMargen)
                .aparicion(2, iniciado)
            if (estado.esTutor) {
                AccesoSolicitudes(estado.pendientes, alVerSolicitudes, montado)
            } else {
                Buscador(alClicar = { alBuscar(null) }, modifier = montado)
            }

            // Tira de materias sin contenedores: ícono y nombre, se desliza de lado (solo para buscar)
            val tira = rememberLazyListState()
            // Debajo del control flotante, una tira del mismo molde en ambos modos: materias para buscar
            // (estudiante) o sus cuatro accesos, cada uno con su cifra (tutor).
            if (estado.esTutor) Column(Modifier.padding(top = EspacioMedio).aparicion(3, iniciado)) {
                AccesosTutor(estado, alAbrirDisponibilidad, alAbrirHistorial, alAbrirResenas, alAbrirMaterias)
                Box(Modifier.fillMaxWidth().padding(top = EspacioMicro).height(1.dp).background(Divisor))
            } else Column(Modifier.padding(top = EspacioMedio).aparicion(3, iniciado)) {
                LazyRow(
                    state = tira,
                    horizontalArrangement = Arrangement.spacedBy(EspacioMicro),
                    contentPadding = PaddingValues(horizontal = EspacioCercano),
                    modifier = Modifier.desvanecerBordes(tira)
                ) {
                    items(AreaEstudio.entries, key = { it.name }) { area ->
                        AccesoArea(area, alClicar = { alBuscar(area.name) })
                    }
                }
                Box(Modifier.fillMaxWidth().padding(top = EspacioMicro).height(1.dp).background(Divisor))
            }

            // Mientras cargan los datos (o si fallaron) no debe decir "Sin sesiones": arranca en la fase agendada
            val ahora = relojDeMinutos()
            val sesiones = estado.proximas.map { it.sesion }
            val fase = if (estado.cargando || estado.error) FaseSesion.AGENDADA
            else faseDe(sesiones, estado.historial, estado.pendientes, ahora)
            val vigente = sesiones.vigente(ahora)
            val aviso = if (estado.cargando || estado.error) null
            else vigente?.let { avisoDePin(it, ahora) } ?: avisoDeCierre(estado.historial, ahora)
            BarraSesion(
                faseActual = fase,
                cargando = estado.cargando,
                minutosParaEmpezar = vigente?.minutosPara(ahora),
                esTutor = estado.esTutor,
                aviso = aviso,
                alTocarAviso = {
                    if (vigente != null) when (aviso?.accion) {
                        AccionPin.ENVIAR -> viewModel.enviarPin(vigente) { fallo ->
                            alcance.launch {
                                if (fallo == null) avisos.showSnackbar("Le enviamos el PIN a ${vigente.conQuien}")
                                else avisos.mostrarError(avisoFallo("enviar el PIN", fallo))
                            }
                        }
                        AccionPin.ESCRIBIR -> escribiendoPin = true
                        null -> Unit
                    }
                },
                modifier = Modifier.padding(top = EspacioFino)
            )
            if (escribiendoPin && vigente != null && vigente.pinEnviado && !vigente.iniciada) {
                HojaPin(
                    sesion = vigente,
                    alCerrar = { escribiendoPin = false },
                    alEnviar = { pin, alFallar ->
                        viewModel.iniciar(vigente, pin) { fallo ->
                            if (fallo == null) {
                                escribiendoPin = false
                                alcance.launch { avisos.showSnackbar("Empezó tu sesión con ${vigente.conQuien}") }
                            } else {
                                alFallar(avisoFallo("iniciar la sesión", fallo))
                            }
                        }
                    }
                )
            }
            Column(
                Modifier
                    .padding(horizontal = EspacioMargen)
                    .padding(top = EspacioCercano + with(densidad) { aireSesiones.toDp() })
                    .aparicion(4, iniciado)
            ) {
                var descartadas by rememberSaveable { mutableStateOf(emptyList<String>()) }
                val rechazada = estado.rechazadas.firstOrNull { it.id !in descartadas }
                AnimatedVisibility(visible = rechazada != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    val ultima = remember { mutableStateOf(rechazada) }
                    if (rechazada != null) ultima.value = rechazada
                    ultima.value?.let { r ->
                        AvisoRechazo(
                            r,
                            alVerOtros = { alBuscar(AreaEstudio.deMateria(r.materia)?.name) },
                            alDescartar = { descartadas = descartadas + r.id },
                            modifier = Modifier.padding(bottom = EspacioSuelto)
                        )
                    }
                }
                EncabezadoSeccion(
                    titulo = if (estado.cargando || estado.error || estado.proximas.size > 1) "Tus próximas sesiones" else "Tu próxima sesión",
                    accion = if (estado.proximas.isNotEmpty()) "Ver solicitudes" else null,
                    alAccion = alVerSolicitudes
                )
                Spacer(Modifier.height(EspacioMicro))
                // Mientras llegan los datos, la silueta de la tarjeta; luego, la tarjeta o el estado vacío.
                // Si la lista se vacía, la tarjeta que se desvanece conserva la última con sesiones
                // (si no, leería la lista vacía durante el fundido y cerraría la app).
                val ultimasProximas = remember { mutableStateOf(estado.proximas) }
                SideEffect { if (estado.proximas.isNotEmpty()) ultimasProximas.value = estado.proximas }
                val proximasVisibles = estado.proximas.ifEmpty { ultimasProximas.value }
                Crossfade(
                    targetState = cargaDe(estado.cargando, estado.proximas.isEmpty(), estado.error),
                    animationSpec = tween(DuracionNormal),
                    label = "proximas"
                ) { carga ->
                    when (carga) {
                        Carga.CARGANDO -> SesionEsqueleto()
                        Carga.VACIO -> SinSesiones(esTutor = estado.esTutor, primeraVez = estado.historial.isEmpty(), alBuscar = { alBuscar(null) }, alVerSolicitudes = alVerSolicitudes)
                        Carga.ERROR -> ErrorDeLista(titulo = "No pudimos cargar tus sesiones", alReintentar = viewModel::reintentar)
                        Carga.LISTO -> proximasVisibles.takeIf { it.isNotEmpty() }?.let { TarjetaProximas(it, mostrarTema = estado.esTutor, abrirChat) }
                    }
                }
            }
        }
        VeloBarraEstado(visible = !cabeceraVisible)
        AvisoNexo(avisos, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = EspacioAmplio))
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
                    .padding(start = EspacioMargen, end = EspacioMedio, top = EspacioCorto)
            ) {
                // Para TalkBack el logo es "Nexo", no la palabra letra por letra
                MarcaComelon(comelon, Modifier.clearAndSetSemantics { contentDescription = "Nexo" })
                Spacer(Modifier.weight(1f))
                BotonIcono(
                    IconosNexo.Campana,
                    descripcion = if (pendientes > 0) "Solicitudes, $pendientes ${plural(pendientes, "pendiente", "pendientes")}" else "Solicitudes",
                    alClicar = alVerSolicitudes,
                    sobreOscuro = true,
                    insignia = pendientes
                )
                Spacer(Modifier.width(EspacioMinimo))
                // Anillo blanco fino: hace juego con la insignia blanca del logo
                // Área de toque de 48 dp; el avatar y lo que lo rodea miden y se ven igual (TamanoAvatarBarra)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(end = EspacioMinimo)
                        .layout { medible, _ ->
                            val lado = AreaTactilMinima.roundToPx()
                            val reducido = TamanoAvatarBarra.roundToPx()
                            val p = medible.measure(Constraints.fixed(lado, lado))
                            layout(reducido, reducido) { p.place(-(lado - reducido) / 2, -(lado - reducido) / 2) }
                        }
                        // TalkBack: "Tu perfil, doble toque para abrir"
                        .pulsable(etiqueta = "abrir", alClicar = alAbrirPerfil)
                        .semantics { contentDescription = "Tu perfil" }
                ) {
                    Avatar(
                        nombre = usuario?.nombre ?: "Nexo",
                        tamano = TamanoAvatarBarra,
                        anillo = AnilloSobreOscuro,
                        fotoUrl = usuario?.fotoUrl
                    )
                }
            }

            // El alto sobrante de la pantalla se ve como más foto entre la marca y el saludo
            Spacer(Modifier.height(aire))
            val nombre = usuario?.nombre?.primerNombre.orEmpty()
            Column(
                Modifier
                    .padding(start = EspacioAmplio, end = EspacioAmplio, top = EspacioHolgado, bottom = EspacioMargen)
                    .aparicion(0, iniciado)
            ) {
                Text(
                    if (nombre.isBlank()) "Hola" else "Hola, $nombre",
                    color = SobreOscuro,
                    style = MaterialTheme.typography.displaySmall,
                    onTextLayout = layoutSaludo,
                    modifier = modSaludo.semantics { heading() }
                )
                Spacer(Modifier.height(EspacioMicro))
                Text(
                    if (usuario?.rol == Rol.TUTOR) "Tus estudiantes te esperan." else "¿Qué quieres aprender hoy?",
                    color = SobreOscuroSuave,
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
                .height(AreaTactilMinima)
        )
        // Migas del logo comelón y puntos de bonificación
        CapaMigasComelon(comelon, Modifier.matchParentSize())
    }
}

/** Botón con forma de campo: abre Buscar (no se escribe aquí para no duplicar la pantalla). */
@Composable
private fun Buscador(alClicar: () -> Unit, modifier: Modifier = Modifier) {
    val forma = FormaCampoAlto
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AltoBuscador)
            .shadow(ElevacionTarjeta, forma, ambientColor = SombraAmbiente, spotColor = SombraPuntual)
            .superficiePulsable(forma, etiqueta = "Buscar tutores", alClicar = alClicar)
            .padding(start = EspacioMargen, end = EspacioBotonLateral)
    ) {
        Icon(IconosNexo.Lupa, contentDescription = null, tint = IconoSecundario, modifier = Modifier.size(Icono20))
        Spacer(Modifier.width(EspacioMedio))
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
                .size(AltoPildora)
                .background(pincelBosque, CircleShape)
        ) {
            Icon(IconosNexo.Flecha, contentDescription = null, tint = SobreOscuro, modifier = Modifier.size(Icono18))
        }
    }
}

/** Acceso por área: ícono y nombre, sin contenedor. */
@Composable
private fun AccesoArea(area: AreaEstudio, alClicar: () -> Unit) {
    AccesoTira(area.iconoPh, area.etiqueta, "Buscar en ${area.etiqueta}", alClicar = alClicar)
}

/** Un elemento de la tira bajo el control flotante: ícono arriba, nombre abajo, sin contenedores. */
@Composable
private fun AccesoTira(icono: Int, etiqueta: String, accion: String, descripcion: String? = null, alClicar: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 62.dp, max = 168.dp)
            .clip(FormaControl)
            .clickable(onClickLabel = accion, role = Role.Button, onClick = alClicar)
            .then(if (descripcion != null) Modifier.clearAndSetSemantics { contentDescription = descripcion } else Modifier)
            .padding(horizontal = EspacioCorto, vertical = EspacioCorto)
    ) {
        Icon(painterResource(icono), contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(Icono26))
        Spacer(Modifier.height(EspacioIcono))
        Text(
            etiqueta,
            color = BosqueProfundo,
            style = EtiquetaArea,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

@Composable
private fun TarjetaProximas(proximas: List<InicioViewModel.SesionProxima>, mostrarTema: Boolean, alAbrirChat: (Sesion) -> Unit) {
    // Tocar una capa de atrás la pasa al frente; las demás se reacomodan en orden (como un mazo de cartas)
    var inicio by remember(proximas) { mutableIntStateOf(0) }
    // Al salir de la pantalla (otra pestaña, otra pantalla o la app en segundo plano) vuelve la más próxima
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { inicio = 0 }
    val orden = remember(proximas, inicio) { proximas.drop(inicio) + proximas.take(inicio) }
    val primera = orden.first()
    val s = primera.sesion
    val formaFrente = FormaOrganica
    // Las capas se montan 12 dp bajo la tarjeta con espaciado negativo (no recortando su alto): así cada
    // una conserva su área de toque completa y, donde se solapan, gana la de arriba (zIndex).
    // TalkBack lee primero la tarjeta del frente y después las capas de atrás
    Column(Modifier.fillMaxWidth().semantics { isTraversalGroup = true }, verticalArrangement = Arrangement.spacedBy((-12).dp)) {
        // Sin sombra (la única sombra de la pantalla es la del buscador): el borde fino la recorta del fondo
        Column(
            Modifier
                .zIndex(3f)
                .semantics { traversalIndex = -1f }
                .fillMaxWidth()
                // Toda la tarjeta se hunde al presionar y abre el chat, igual que su botón
                .then(
                    Modifier.pulsable(etiqueta = "Escribir a ${s.conQuien}") { alAbrirChat(s) }
                )
                .background(TinteBosque, formaFrente)
                .border(1.dp, BordeAcento, formaFrente)
                .padding(start = EspacioNormal, end = EspacioCercano, top = EspacioCercano, bottom = EspacioMedio)
        ) {
          AnimatedContent(
            targetState = primera,
            // La elegida SUBE desde abajo y crece hasta su sitio; la que estaba al frente BAJA, se achica y se apaga
            transitionSpec = {
                (fadeIn(tween(DuracionPilaAparece, delayMillis = RetrasoPilaAparece)) +
                    slideInVertically(tween(DuracionPilaEntra, easing = CurvaEnfatizada)) { it / 4 } +
                    scaleIn(tween(DuracionPilaEntra, easing = CurvaEnfatizada), initialScale = EscalaPilaSaliente)) togetherWith
                    (fadeOut(tween(DuracionPilaSeApaga)) +
                        slideOutVertically(tween(DuracionLarga, easing = CurvaEnfatizada)) { it / 5 } +
                        scaleOut(tween(DuracionLarga, easing = CurvaEnfatizada), targetScale = EscalaPilaSaliente)) using
                    SizeTransform(clip = false) { _, _ -> tween(DuracionPilaTamano, easing = CurvaEnfatizada) }
            },
            label = "sesionFrente"
          ) { primera ->
            val s = primera.sesion
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        fechaLarga(s.fecha),
                        color = BosqueAcento,
                        style = EtiquetaFecha,
                        modifier = Modifier.weight(1f)
                    )
                    // Modalidad como texto con ícono, sin pastilla: el único botón blanco es "Abrir chat"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.heightIn(min = 24.dp)
                    ) {
                        Icon(painterResource(s.modalidad.iconoPh), contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(Icono14))
                        Spacer(Modifier.width(EspacioMinimo))
                        Text(s.modalidad.etiqueta, color = BosqueAcento, style = EtiquetaArea)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = EspacioFino, bottom = EspacioMedio)
                        .height(IntrinsicSize.Min)
                ) {
                    Text(
                        formatoHora(s.horaPactada ?: s.hora),
                        color = BosqueProfundo,
                        style = CifraHora.merge(CifrasTabulares)
                    )
                    Spacer(Modifier.width(EspacioCercano))
                    Box(
                        Modifier
                            .padding(vertical = EspacioMinimo)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(FondoAcentoTenue)
                    )
                    Spacer(Modifier.width(EspacioCercano))
                    Column(Modifier.weight(1f)) {
                        TextoLinea(s.materia, TituloMateria, BosqueProfundo)
                        Row {
                            // Si el tema no cabe se recorta él, nunca la hora
                            primera.tema?.takeIf { mostrarTema }?.let {
                                Text(
                                    it,
                                    color = TextoSobreTinte,
                                    style = CuerpoSesion,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(" · ", color = TextoSobreTinte, style = CuerpoSesion, maxLines = 1)
                            }
                            Text(
                                duracionVisible(s),
                                color = TextoSobreTinte,
                                style = CuerpoSesion,
                                maxLines = 1
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(s.conQuien, tamano = TamanoAvatarMini, fotoUrl = primera.tutor?.fotoUrl)
                    Spacer(Modifier.width(EspacioAjustado))
                    Column(Modifier.weight(1f)) {
                        TextoLinea(s.conQuien, CuerpoNombre, BosqueProfundo)
                        primera.tutor?.let { t ->
                            TextoLinea(
                                listOf(universidadCorta(t.universidad), if (t.rating > 0) formatoValoracion(t.rating) else "Tutor nuevo").filter { it.isNotBlank() }.joinToString(" · "),
                                MaterialTheme.typography.bodySmall.merge(EnfasisMedio),
                                TextoSobreTinte
                            )
                        }
                    }
                    BotonCalendario(s, primera.tema)
                    PildoraAbrirChat(s.conQuien, { alAbrirChat(s) }, fondo = Superficie)
                }
            }
          }
        }

        // Capas de las siguientes sesiones: más angostas y más translúcidas hacia abajo
        orden.drop(1).take(2).forEachIndexed { n, p ->
            val fondo = if (n == 0) TintePilaCerca else TintePilaLejos
            val tinta = if (n == 0) 0.88f else 0.62f
            // Cascada: tras la tarjeta principal, cada capa sale de detrás de la anterior con un resorte suave
            var salio by remember { mutableStateOf(false) }
            val reducidas = LocalAnimacionesReducidas.current
            LaunchedEffect(Unit) { if (!reducidas) delay(RetrasoCascadaPila + n * PasoCascadaPila); salio = true }
            val salida by animateFloatAsState(
                if (salio) 1f else 0f,
                resorteCascada(),
                label = "capa$n"
            )
            // El área pulsable llega a 48 dp, pero la capa conserva su alto visible (46 o 44 dp)
            val altoCapa = if (n == 0) 46.dp else 44.dp
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .zIndex(2f - n)
                    .padding(horizontal = (8 * (n + 1)).dp)
                    .fillMaxWidth()
                    .areaTactilSinCambiarMedida(altoCapa)
                    .heightIn(min = AreaTactilMinima)
                    .pulsable(etiqueta = "Pasar al frente la sesión de ${p.sesion.materia}") { inicio = (inicio + 1 + n) % proximas.size }
            ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = altoCapa)
                    .background(fondo, FormaCapaInferior)
                    .padding(start = EspacioCercano, end = EspacioCercano, top = EspacioCercano)
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
                        (fadeIn(tween(DuracionPilaAparece, delayMillis = RetrasoCapaPila)) + slideInVertically(tween(DuracionPilaTamano, easing = CurvaEnfatizada)) { -it / 2 }) togetherWith
                            (fadeOut(tween(DuracionCapaSeApaga)) + slideOutVertically(tween(DuracionNormal, easing = CurvaEnfatizada)) { -it / 2 })
                    },
                    label = "capa${n}Contenido"
                ) { q ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${fechaCorta(q.sesion.fecha)} · ${formatoHora(q.sesion.hora)}",
                    color = BosqueProfundo,
                    style = MaterialTheme.typography.titleSmall.merge(CifrasTabulares)
                )
                Spacer(Modifier.width(EspacioAjustado))
                Text(
                    "${q.sesion.materia} · ${q.sesion.conQuien}",
                    color = BosqueProfundo,
                    style = CuerpoSesion,
                    // Con letra grande el texto baja a una segunda línea en vez de cortarse
                    maxLines = if (LocalDensity.current.fontScale > 1f) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(EspacioAjustado))
                Icon(painterResource(q.sesion.modalidad.iconoPh), contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(Icono16))
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun SinSesiones(esTutor: Boolean, primeraVez: Boolean, alBuscar: () -> Unit, alVerSolicitudes: () -> Unit) {
    val texto = when {
        esTutor -> "Cuando aceptes una solicitud, aparecerá aquí."
        primeraVez -> "Elige un tutor y solicita tu primera sesión."
        else -> "Busca un tutor y agenda tu próxima sesión."
    }
    val boton: @Composable (Modifier) -> Unit = { m ->
        BotonSecundario(if (esTutor) "Solicitudes" else "Buscar", if (esTutor) alVerSolicitudes else alBuscar, compacto = true, modifier = m)
    }
    // Con letra grande el texto no cabe junto al botón: el botón baja a su propia línea
    if (LocalDensity.current.fontScale > 1.3f) {
        Column(
            Modifier.fillMaxWidth().superficie(FormaOrganica).padding(EspacioNormal),
            verticalArrangement = Arrangement.spacedBy(EspacioCercano)
        ) {
            Text("Sin sesiones agendadas", color = TextoPrimario, style = MaterialTheme.typography.titleMedium)
            Text(texto, color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
            boton(Modifier.fillMaxWidth())
        }
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .superficie(FormaOrganica)
            .padding(EspacioNormal)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AreaTactilMinima)
                .background(TinteBosque, FormaControl)
        ) {
            Icon(IconosNexo.CalendarioMarcado, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(Icono22))
        }
        Spacer(Modifier.width(EspacioCercano))
        Column(Modifier.weight(1f)) {
            Text("Sin sesiones agendadas", color = TextoPrimario, style = MaterialTheme.typography.titleMedium)
            Text(texto, color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(EspacioCorto))
        boton(Modifier)
    }
}

/** Los accesos del tutor, con la misma forma que la tira del estudiante: cada uno lleva a su pantalla y da una cifra. */
@Composable
private fun AccesosTutor(
    estado: InicioViewModel.Estado,
    alDisponibilidad: () -> Unit,
    alTutorias: () -> Unit,
    alResenas: () -> Unit,
    alMaterias: () -> Unit
) {
    val horas = estado.horasPublicadas
    val resumen = estado.resumen
    val dadas = resumen?.sesiones
    val promedio = resumen?.promedioRecibido
    val materias = estado.usuario?.materias.orEmpty().size
    Row(Modifier.fillMaxWidth().padding(horizontal = EspacioCercano)) {
        AccesoConCifra(
            R.drawable.ph_calendario_check, "Disponibilidad",
            if (estado.cargando) "–" else if (horas == 0) "Sin publicar" else "${formatoHoras(horas.toDouble())}/semana",
            alerta = horas == 0 && !estado.cargando,
            alClicar = alDisponibilidad
        )
        AccesoConCifra(
            R.drawable.ph_historial, "Tutorías",
            when (dadas) {
                null -> "–"
                0 -> "Ninguna aún"
                else -> "$dadas ${plural(dadas, "dada", "dadas")}"
            },
            alClicar = alTutorias
        )
        AccesoConCifra(
            R.drawable.ph_resena, "Reseñas",
            when {
                resumen == null -> "–"
                promedio == null -> "Sin reseñas"
                else -> "${formatoValoracion(promedio)} de 5"
            },
            alClicar = alResenas
        )
        AccesoConCifra(
            R.drawable.ph_graduacion, "Materias",
            if (estado.cargando) "–" else if (materias == 0) "Agregar" else "$materias ${plural(materias, "publicada", "publicadas")}",
            alerta = materias == 0 && !estado.cargando,
            alClicar = alMaterias
        )
    }
}

/** `alerta`: falta algo para que los estudiantes puedan encontrarlo o agendarle. */
@Composable
private fun RowScope.AccesoConCifra(icono: Int, nombre: String, cifra: String, alerta: Boolean = false, alClicar: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clip(FormaControl)
            .clickable(onClickLabel = "Abrir $nombre", role = Role.Button, onClick = alClicar)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = EspacioCorto)
    ) {
        Icon(painterResource(icono), contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(Icono26))
        Spacer(Modifier.height(EspacioIcono))
        Text(nombre, color = BosqueProfundo, style = EtiquetaArea, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            cifra,
            color = if (alerta) BosqueAcento else TextoSecundario,
            style = CifraAcceso.merge(CifrasTabulares),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * La hora actual, al minuto: mueve las fases de la sesión sin que nadie toque nada. Solo corre con la app a la vista y
 * se pone al día en cuanto vuelve.
 */
@Composable
private fun relojDeMinutos(): LocalDateTime {
    val ciclo = LocalLifecycleOwner.current.lifecycle
    val ahora by produceState(LocalDateTime.now(), ciclo) {
        ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val actual = LocalDateTime.now()
                value = actual
                delay((60 - actual.second) * 1_000L)
            }
        }
    }
    return ahora
}

/**
 * Modo tutor: en el lugar del buscador, con su misma forma, el acceso a las solicitudes por responder.
 * Sin pendientes se lee "Todo al día" y sigue llevando a la lista.
 */
@Composable
private fun AccesoSolicitudes(cantidad: Int, alRevisar: () -> Unit, modifier: Modifier = Modifier) {
    val forma = FormaCampoAlto
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AltoBuscador)
            .shadow(ElevacionTarjeta, forma, ambientColor = SombraAmbiente, spotColor = SombraPuntual)
            .superficiePulsable(forma, etiqueta = "Ver solicitudes recibidas", alClicar = alRevisar)
            .padding(start = EspacioMargen, end = EspacioBotonLateral)
    ) {
        Icon(IconosNexo.Campana, contentDescription = null, tint = IconoSecundario, modifier = Modifier.size(Icono20))
        Spacer(Modifier.width(EspacioMedio))
        Text(
            when (cantidad) {
                0 -> "Todo al día: sin solicitudes nuevas"
                1 -> "1 solicitud por responder"
                else -> "$cantidad solicitudes por responder"
            },
            color = if (cantidad > 0) BosqueProfundo else Marcador,
            style = MaterialTheme.typography.bodyLarge.merge(if (cantidad > 0) EnfasisFuerte else EnfasisNormal),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AltoPildora)
                .background(pincelBosque, CircleShape)
        ) {
            Icon(IconosNexo.Flecha, contentDescription = null, tint = SobreOscuro, modifier = Modifier.size(Icono18))
        }
    }
}


/**
 * Con un retardo pactado la sesión empieza más tarde pero termina a la misma hora: lo que queda es menos de lo
 * agendado ("22 min · hasta las 21:00", no "1 h").
 */
private fun duracionVisible(s: Sesion): String {
    val fin = horaFin(s.hora, s.duracionHoras)
    val desde = s.horaPactada ?: return "${formatoHoras(s.duracionHoras)} · hasta las ${formatoHora(fin)}"
    val minutos = java.time.Duration.between(desde, fin).toMinutes().coerceAtLeast(0)
    val cuanto = if (minutos % 60 == 0L) formatoHoras(minutos / 60.0) else "$minutos min"
    return "$cuanto · hasta las ${formatoHora(fin)}"
}

/** Pasa la sesión al calendario del teléfono, con la hora de Bogotá: así el recordatorio llega aunque Nexo esté cerrada. */
@Composable
private fun BotonCalendario(s: Sesion, tema: String?) {
    val contexto = LocalContext.current
    var sinApp by remember { mutableStateOf(false) }
    IconButton(
        onClick = {
            val zona = java.time.ZoneId.of("America/Bogota")
            val inicio = s.fecha.atTime(s.horaPactada ?: s.hora).atZone(zona)
            val fin = inicio.plusMinutes((s.duracionHoras * 60).toLong())
            val intento = android.content.Intent(android.content.Intent.ACTION_INSERT, android.provider.CalendarContract.Events.CONTENT_URI)
                .putExtra(android.provider.CalendarContract.Events.TITLE, "Tutoría de ${tema?.takeIf { it.isNotBlank() } ?: s.materia} con ${s.conQuien}")
                .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, inicio.toInstant().toEpochMilli())
                .putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, fin.toInstant().toEpochMilli())
                .putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, if (s.modalidad == Modalidad.VIRTUAL) "Virtual (Nexo)" else "Presencial")
                .putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Sesión agendada en Nexo.")
            try { contexto.startActivity(intento) } catch (e: android.content.ActivityNotFoundException) { sinApp = true }
        },
        modifier = Modifier.semantics { contentDescription = "Agregar al calendario" }
    ) {
        Icon(IconosNexo.Calendario, contentDescription = null, tint = BosqueProfundo, modifier = Modifier.size(Icono22))
    }
    if (sinApp) {
        LaunchedEffect(Unit) {
            android.widget.Toast.makeText(contexto, "No encontramos una app de calendario.", android.widget.Toast.LENGTH_SHORT).show()
            sinApp = false
        }
    }
}

/** Modo estudiante: un tutor no pudo aceptar; se ofrece buscar otro sin tener que ir a Solicitudes. */
@Composable
private fun AvisoRechazo(solicitud: Solicitud, alVerOtros: () -> Unit, alDescartar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .superficie(FormaOrganica)
            .padding(start = EspacioNormal, end = EspacioMinimo, top = EspacioMinimo, bottom = EspacioCercano)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${solicitud.nombreTutor.substringBefore(' ')} no pudo aceptar tu solicitud de ${solicitud.materia}",
                color = TextoPrimario,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).padding(top = EspacioCorto)
            )
            IconButton(onClick = alDescartar) {
                Icon(IconosNexo.Cerrar, contentDescription = "Descartar aviso", tint = IconoSecundario, modifier = Modifier.size(IconoNormal))
            }
        }
        solicitud.motivoRechazo?.takeIf { it.isNotBlank() }?.let {
            Text("«$it»", color = TextoSecundario, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = EspacioNormal))
        }
        Spacer(Modifier.height(EspacioCorto))
        BotonSecundario("Ver otros tutores", alVerOtros, compacto = true)
    }
}
