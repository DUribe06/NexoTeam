package com.grupo3.nexo.ui.tutor

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.usecase.DiaSolicitable
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import com.grupo3.nexo.domain.usecase.diasSolicitables
import com.grupo3.nexo.domain.usecase.franjaValida
import com.grupo3.nexo.domain.usecase.modalidadValida
import com.grupo3.nexo.ui.components.formatoHoras
import com.grupo3.nexo.ui.theme.SobreOscuro
import com.grupo3.nexo.ui.theme.Superficie
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.usecase.EnviarSolicitudUseCase
import com.grupo3.nexo.domain.usecase.ObtenerResenasUseCase
import com.grupo3.nexo.domain.usecase.ObtenerTutorUseCase
import com.grupo3.nexo.domain.usecase.ReportarTutorUseCase
import com.grupo3.nexo.ui.components.Avatar
import com.grupo3.nexo.ui.components.AvisoNexo
import com.grupo3.nexo.ui.components.BotonIcono
import com.grupo3.nexo.ui.components.BotonPrimario
import com.grupo3.nexo.ui.components.CabeceraBosque
import com.grupo3.nexo.ui.components.Campo
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.EncabezadoSeccion
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.Estrellas
import com.grupo3.nexo.ui.components.FiltroOpcion
import com.grupo3.nexo.ui.components.HojaNexo
import com.grupo3.nexo.ui.components.IconosClarosEnBarraDeEstado
import com.grupo3.nexo.ui.components.enumerar
import com.grupo3.nexo.ui.components.RotuloGrupo
import com.grupo3.nexo.ui.components.TextoLinea
import com.grupo3.nexo.ui.components.VeloBarraEstado
import com.grupo3.nexo.ui.components.aparicion
import com.grupo3.nexo.ui.components.PLAZO_RESPUESTA
import com.grupo3.nexo.ui.components.formatoPrecio
import com.grupo3.nexo.ui.components.formatoValoracion
import com.grupo3.nexo.ui.components.rememberPrimeraVez
import com.grupo3.nexo.ui.components.solaparHaciaArriba
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.theme.Bosque
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.Fondo
import com.grupo3.nexo.ui.theme.GrisEstado
import com.grupo3.nexo.ui.theme.GrisFondo
import com.grupo3.nexo.ui.theme.Menta
import com.grupo3.nexo.ui.theme.Miel
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.TinteBosque
import com.grupo3.nexo.ui.theme.VerdeEstado
import com.grupo3.nexo.ui.theme.VerdeFondo
import com.grupo3.nexo.domain.model.DiaSemana
import com.grupo3.nexo.domain.model.FranjaTutor
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.ui.components.EtiquetaModalidad
import com.grupo3.nexo.ui.components.IconosModalidad
import com.grupo3.nexo.ui.components.icono
import com.grupo3.nexo.ui.theme.Rojo
import com.grupo3.nexo.ui.theme.SuperficieClara
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.graphicsLayer
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioAmplio
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioFino
import com.grupo3.nexo.ui.theme.EspacioHolgado
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioMinimo
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.EspacioSuelto
import com.grupo3.nexo.ui.theme.FormaMinima
import com.grupo3.nexo.ui.theme.FormaTarjeta
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import com.grupo3.nexo.ui.components.BotonTexto
import com.grupo3.nexo.ui.components.Carga
import com.grupo3.nexo.ui.components.ErrorDeLista
import com.grupo3.nexo.ui.components.EsqueletoLista
import com.grupo3.nexo.ui.components.cargaDe
import com.grupo3.nexo.ui.theme.especColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import com.grupo3.nexo.ui.components.IconosNexo
import com.grupo3.nexo.ui.components.Onda
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.components.LetraAcotada

/* ==================== ViewModel ==================== */

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TutorPerfilViewModel @Inject constructor(
    private val obtenerTutor: ObtenerTutorUseCase,
    private val obtenerResenas: ObtenerResenasUseCase,
    private val enviarSolicitud: EnviarSolicitudUseCase,
    private val reportarTutor: ReportarTutorUseCase,
    obtenerUsuario: ObtenerUsuarioUseCase
) : ViewModel() {

    data class Estado(
        val tutor: Tutor? = null,
        val resenas: List<Resena> = emptyList(),
        val aviso: String? = null,
        /** true hasta que llega la primera respuesta del tutor. */
        val cargando: Boolean = true,
        val noEncontrado: Boolean = false,
        /** Un tutor viendo a otro tutor no puede solicitarle tutorías. */
        val esTutor: Boolean = false
    )

    private val tutorId = MutableStateFlow("")
    private val aviso = MutableStateFlow<String?>(null)

    private val tutor: Flow<Tutor?> = tutorId.flatMapLatest { id ->
        if (id.isEmpty()) emptyFlow() else obtenerTutor(id)
    }
    private val resenas: Flow<List<Resena>> = tutorId.flatMapLatest { id ->
        if (id.isEmpty()) emptyFlow() else obtenerResenas(id)
    }

    val estado = combine(tutor, resenas, aviso, obtenerUsuario()) { t, r, a, u ->
        Estado(tutor = t, resenas = r, aviso = a, cargando = false, noEncontrado = t == null, esTutor = u.rol == Rol.TUTOR)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun cargar(id: String) {
        tutorId.value = id
    }

    fun solicitar(datos: DatosSolicitud) {
        val t = estado.value.tutor ?: return
        viewModelScope.launch {
            enviarSolicitud(t, datos.materia, datos.tema, datos.fecha, datos.hora, datos.modalidad, datos.mensaje)
            aviso.value = "Solicitud enviada. ${t.nombre.substringBefore(' ')} tiene $PLAZO_RESPUESTA para responder."
        }
    }

    fun reportar(motivo: String) {
        val t = estado.value.tutor ?: return
        viewModelScope.launch {
            reportarTutor(t.id, motivo)
            aviso.value = "Reporte enviado. Gracias por cuidar la comunidad."
        }
    }

    fun descartarAviso() {
        aviso.value = null
    }
}

/* ==================== Pantalla ==================== */

private val motivosReporte = listOf(
    "Comportamiento inapropiado",
    "Información falsa en el perfil",
    "No se presentó a la sesión",
    "Spam o publicidad",
    "Otro motivo"
)

@Composable
fun PantallaTutorPerfil(
    tutorId: String,
    alVolver: () -> Unit,
    viewModel: TutorPerfilViewModel = hiltViewModel()
) {
    LaunchedEffect(tutorId) { viewModel.cargar(tutorId) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val tutor = estado.tutor

    var mostrarSolicitud by rememberSaveable { mutableStateOf(false) }
    var mostrarReporte by rememberSaveable { mutableStateOf(false) }
    val avisos = remember { SnackbarHostState() }

    LaunchedEffect(estado.aviso) {
        val texto = estado.aviso ?: return@LaunchedEffect
        avisos.showSnackbar(texto)
        viewModel.descartarAviso()
    }

    // La portada es oscura: íconos claros solo mientras está a la vista.
    val lista = rememberLazyListState()
    val portadaVisible by remember {
        derivedStateOf { lista.firstVisibleItemIndex == 0 && lista.firstVisibleItemScrollOffset < 300 }
    }
    IconosClarosEnBarraDeEstado(activo = portadaVisible)
    val iniciado = rememberPrimeraVez()

    Box(Modifier.fillMaxSize().background(Fondo)) {
        if (tutor == null) {
            CabeceraBosque(alturaOnda = 44.dp) {
                Box(Modifier.padding(start = EspacioMedio, top = EspacioFino).height(140.dp)) {
                    BotonIcono(IconosNexo.Atras, "Volver", alVolver, sobreOscuro = true)
                }
            }
            // Bajo la cabecera: la silueta del perfil mientras llegan los datos, o el error
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.62f)) {
                when (cargaDe(cargando = estado.cargando, vacia = estado.noEncontrado)) {
                    Carga.ERROR -> ErrorDeLista(titulo = "No pudimos cargar este perfil")
                    Carga.VACIO -> ErrorDeLista(titulo = "No encontramos a este tutor")
                    else -> EsqueletoLista(filas = 3)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = lista,
                    // Sin la barra de solicitud (modo tutor), el contenido debe despejar la barra de navegación
                    contentPadding = PaddingValues(
                        bottom = EspacioAmplio + if (estado.esTutor) WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() else 0.dp
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    item(key = "portada") {
                        Portada(alVolver = alVolver, alReportar = { mostrarReporte = true })
                    }
                    item(key = "identidad") {
                        Identidad(tutor, Modifier.aparicion(0, iniciado))
                    }
                    item(key = "cifras") {
                        LetraAcotada {
                        Cifras(
                            tutor,
                            Modifier
                                .padding(start = EspacioMargen, end = EspacioMargen, top = EspacioHolgado)
                                .aparicion(1, iniciado)
                        )
                        }
                    }
                    item(key = "sobre") {
                        Column(
                            Modifier
                                .padding(horizontal = EspacioMargen)
                                .padding(top = EspacioSuelto)
                                .aparicion(2, iniciado)
                        ) {
                            EncabezadoSeccion("Sobre ${tutor.nombre.substringBefore(' ')}")
                            Text(tutor.bio, color = TextoSecundario, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(EspacioSuelto))
                            RotuloGrupo("Enseña")
                            Spacer(Modifier.height(EspacioAjustado))
                            if (tutor.materias.isEmpty()) {
                                Text("Aún no publica materias.", color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
                                if (tutor.horario.isNotEmpty() && !estado.esTutor) BotonTexto("Solicitar tutoría", { mostrarSolicitud = true })
                            } else {
                                Materias(tutor.materias)
                            }
                        }
                    }
                    item(key = "horario") {
                        Column(
                            Modifier
                                .padding(horizontal = EspacioMargen)
                                .padding(top = EspacioHolgado)
                        ) {
                            EncabezadoSeccion("Horarios disponibles")
                            HorarioSemanal(tutor.horario)
                        }
                    }
                    item(key = "resenasTitulo") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = EspacioMargen)
                                .padding(top = EspacioHolgado, bottom = EspacioFino)
                        ) {
                            Text(
                                "Reseñas",
                                color = TextoPrimario,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { heading() }
                            )
                            Estrellas(Math.round(tutor.rating).toInt(), tamano = 14.dp)
                            Spacer(Modifier.width(EspacioFino))
                            Text(
                                "${formatoValoracion(tutor.rating)} · ${tutor.totalResenas}",
                                color = TextoSecundario,
                                style = MaterialTheme.typography.bodySmall.merge(CifrasTabulares)
                            )
                        }
                    }
                    when (cargaDe(cargando = false, vacia = estado.resenas.isEmpty())) {
                        Carga.CARGANDO -> item(key = "cargandoResenas") { EsqueletoLista(filas = 2) }
                        Carga.ERROR -> item(key = "errorResenas") {
                            ErrorDeLista(titulo = "No pudimos cargar las reseñas")
                        }
                        Carga.VACIO -> item(key = "sinResenas") {
                            EstadoVacio(
                                icono = IconosNexo.Resena,
                                titulo = "Aún sin reseñas",
                                mensaje = "Tras tu sesión podrás calificar a ${tutor.nombre.substringBefore(' ')}.",
                                accion = if (tutor.horario.isNotEmpty() && !estado.esTutor) "Solicitar tutoría" else null,
                                alAccion = { mostrarSolicitud = true }
                            )
                        }
                        Carga.LISTO -> items(estado.resenas, key = { it.autor + it.fecha }) { resena ->
                            TarjetaResena(
                                resena = resena,
                                modifier = Modifier
                                    .padding(horizontal = EspacioMargen)
                                    .padding(bottom = EspacioAjustado)
                            )
                        }
                    }
                }

                if (!estado.esTutor) {
                    BarraAcciones(
                        tutor = tutor,
                        alSolicitar = { mostrarSolicitud = true }
                    )
                }
            }

            // El velo del sistema es translúcido: detrás, un fondo sólido para que el texto no se asome al desplazar
            val fondoBarra by animateFloatAsState(if (portadaVisible) 0f else 1f, especColor(), label = "fondoBarra")
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .graphicsLayer { alpha = fondoBarra }
                    .background(Fondo)
            )
            VeloBarraEstado(visible = !portadaVisible)

            AvisoNexo(
                avisos,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (estado.esTutor) 0.dp else 92.dp)
            )
        }
    }

    if (mostrarSolicitud && tutor != null && !estado.esTutor) {
        HojaSolicitud(
            tutor = tutor,
            alCerrar = { mostrarSolicitud = false },
            alEnviar = { datos ->
                viewModel.solicitar(datos)
                mostrarSolicitud = false
            }
        )
    }
    if (mostrarReporte && tutor != null) {
        HojaReporte(
            nombre = tutor.nombre,
            alCerrar = { mostrarReporte = false },
            alEnviar = { motivo ->
                viewModel.reportar(motivo)
                mostrarReporte = false
            }
        )
    }
}

/* ==================== Piezas ==================== */

@Composable
private fun Portada(alVolver: () -> Unit, alReportar: () -> Unit) {
    Box {
        CabeceraBosque(conFoto = true, alturaOnda = 52.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EspacioMedio, vertical = EspacioFino)
            ) {
                BotonIcono(IconosNexo.Atras, "Volver", alVolver, sobreOscuro = true)
                Spacer(Modifier.weight(1f))
                BotonIcono(IconosNexo.Bandera, "Reportar tutor", alReportar, sobreOscuro = true)
            }
            Spacer(Modifier.height(72.dp))
        }
        Onda(
            Fondo,
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(32.dp)
        )
    }
}

/** Avatar en insignia blanca montado sobre la onda (como el logo del acceso), nombre y estado. */
@Composable
private fun Identidad(tutor: Tutor, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EspacioAmplio)
    ) {
        Box(
            Modifier
                .solaparHaciaArriba(64.dp)
                .background(Superficie, CircleShape)
                .padding(EspacioMinimo)
        ) {
            Avatar(tutor.nombre, tamano = 98.dp)
        }
        Spacer(Modifier.height(EspacioCercano))
        Text(
            tutor.nombre,
            color = TextoPrimario,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(EspacioMinimo))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(IconosNexo.Graduacion, contentDescription = null, tint = TextoTercero, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(EspacioFino))
            Text(
                tutor.universidad,
                color = TextoSecundario,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(EspacioMedio))
        val enLinea = tutor.enLinea
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = EspacioMinimo)
        ) {
            Box(Modifier.size(7.dp).background(if (enLinea) Menta else GrisEstado.copy(alpha = 0.6f), CircleShape))
            Spacer(Modifier.width(7.dp))
            Text(
                if (enLinea) "En línea" else "Desconectado",
                color = if (enLinea) VerdeEstado else GrisEstado,
                style = MaterialTheme.typography.labelMedium
            )
            Text("  ·  ${tutor.handle}", color = TextoTercero, style = MaterialTheme.typography.labelMedium)
        }
        if (tutor.modalidades.isNotEmpty()) {
            Spacer(Modifier.height(EspacioCorto))
            Row(horizontalArrangement = Arrangement.spacedBy(EspacioCorto)) {
                Modalidad.entries.filter { it in tutor.modalidades }.forEach { EtiquetaModalidad(it) }
            }
        }
    }
}

/** Tres cifras en una sola superficie, separadas por filetes. */
@Composable
private fun Cifras(tutor: Tutor, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 78.dp)
            .height(IntrinsicSize.Min)
            .superficie(FormaOrganica)
    ) {
        Cifra(formatoValoracion(tutor.rating), "calificación", Modifier.weight(1f), conEstrella = true)
        Box(Modifier.fillMaxHeight().padding(vertical = EspacioSuelto).width(1.dp).background(Divisor))
        Cifra("${tutor.sesionesTotales}", "sesiones", Modifier.weight(1f))
        Box(Modifier.fillMaxHeight().padding(vertical = EspacioSuelto).width(1.dp).background(Divisor))
        Cifra("${tutor.estudiantes}", "estudiantes", Modifier.weight(1f))
    }
}

@Composable
private fun Cifra(valor: String, etiqueta: String, modifier: Modifier = Modifier, conEstrella: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .semantics(mergeDescendants = true) { }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (conEstrella) {
                Icon(IconosNexo.Estrella, contentDescription = null, tint = Miel, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(EspacioMinimo))
            }
            Text(valor, color = TextoPrimario, style = MaterialTheme.typography.titleLarge.merge(CifrasTabulares))
        }
        Text(etiqueta, color = TextoTercero, style = MaterialTheme.typography.bodySmall)
    }
}

/** Materias como texto corrido, sin etiquetas: se leen de un vistazo y no recargan la pantalla. */
@Composable
private fun Materias(materias: List<String>) {
    Text(enumerar(materias), color = TextoPrimario, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun TarjetaResena(resena: Resena, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .superficie(FormaOrganica)
            .padding(EspacioNormal)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(resena.autor, tamano = 38.dp)
            Spacer(Modifier.width(EspacioMedio))
            Column(Modifier.weight(1f)) {
                TextoLinea(resena.autor, MaterialTheme.typography.titleSmall, TextoPrimario)
                TextoLinea(resena.fecha, MaterialTheme.typography.bodySmall, TextoTercero)
            }
            Estrellas(resena.estrellas, tamano = 13.dp)
        }
        Spacer(Modifier.height(EspacioAjustado))
        Text(resena.texto, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Barra fija inferior: tarifa a la izquierda, mensaje y la acción principal. */
@Composable
private fun BarraAcciones(tutor: Tutor, alSolicitar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Superficie)
            .drawBehind { drawLine(Divisor, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
            .padding(start = EspacioMargen, end = EspacioNormal, top = EspacioMedio, bottom = EspacioMedio)
    ) {
        Column {
            Text(
                formatoPrecio(tutor.precioHora),
                color = TextoPrimario,
                style = MaterialTheme.typography.headlineSmall.merge(CifrasTabulares)
            )
            Text("por hora", color = TextoTercero, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(EspacioNormal))
        BotonPrimario(
            "Solicitar tutoría",
            alSolicitar,
            conFlecha = true,
            habilitado = tutor.horario.isNotEmpty(),
            modifier = Modifier.weight(1f)
        )
    }
}

/* ==================== Hojas ==================== */

/** Lo que el estudiante elige en la hoja de solicitud (RF5). */
data class DatosSolicitud(
    val materia: String,
    val tema: String,
    val fecha: String,
    val hora: String,
    val modalidad: Modalidad,
    val mensaje: String
)

private val DiaSolicitable.abreviatura: String get() = DiaSemana.desde(fecha.dayOfWeek.value).abreviatura
private val DiaSolicitable.etiqueta: String get() = fecha.format(formatoDiaCorto).replace(".", "")

private val localeColombia = Locale("es", "CO")
private val formatoDiaCorto = DateTimeFormatter.ofPattern("d MMM", localeColombia)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HojaSolicitud(
    tutor: Tutor,
    alCerrar: () -> Unit,
    alEnviar: (DatosSolicitud) -> Unit
) {
    val dias = remember(tutor.horario) { diasSolicitables(tutor.horario) }
    var materia by rememberSaveable { mutableStateOf(tutor.materias.firstOrNull().orEmpty()) }
    var tema by rememberSaveable { mutableStateOf("") }
    var indiceDia by rememberSaveable { mutableIntStateOf(0) }
    var hora by rememberSaveable { mutableStateOf<String?>(null) }
    var modalidad by rememberSaveable { mutableStateOf<Modalidad?>(null) }
    var mensaje by rememberSaveable { mutableStateOf("") }
    var intentoEnviar by rememberSaveable { mutableStateOf(false) }

    val dia = dias.getOrNull(indiceDia)
    val franja = franjaValida(dia, hora)
    // Hora y modalidad siempre válidas para el día elegido: si no, se toma la primera disponible.
    val horaElegida = franja?.hora
    val modalidadElegida = modalidadValida(franja, modalidad)
    val temaFalta = tema.isBlank()

    HojaNexo(
        titulo = "Solicitar tutoría",
        subtitulo = "con ${tutor.nombre} · ${formatoPrecio(tutor.precioHora)} la hora",
        alCerrar = alCerrar
    ) {
        RotuloGrupo("Materia", Modifier.padding(top = EspacioHolgado, bottom = EspacioAjustado))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(EspacioCorto), verticalArrangement = Arrangement.spacedBy(EspacioCorto)) {
            tutor.materias.forEach { FiltroOpcion(it, it == materia, { materia = it }) }
        }

        RotuloGrupo("Tema a reforzar", Modifier.padding(top = EspacioMargen, bottom = EspacioAjustado))
        Campo(
            valor = tema,
            alCambiar = { tema = it },
            marcador = "Ej.: límites y continuidad",
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        if (intentoEnviar && temaFalta) {
            Text(
                "Cuéntale al tutor qué tema quieres reforzar.",
                color = Rojo,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = EspacioMargen, top = EspacioFino)
            )
        }

        if (dias.isEmpty()) {
            Text(
                "${tutor.nombre.substringBefore(' ')} no tiene horarios publicados por ahora.",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = EspacioMargen)
            )
        } else {
            RotuloGrupo("Día", Modifier.padding(top = EspacioMargen, bottom = EspacioAjustado))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(EspacioCorto),
                modifier = Modifier.selectableGroup()
            ) {
                items(dias.size, key = { dias[it].fecha.toString() }) { i ->
                    SelectorDia(
                        dia = dias[i].abreviatura,
                        fecha = dias[i].etiqueta,
                        seleccionado = i == indiceDia,
                        alClicar = {
                            indiceDia = i
                            hora = null
                        }
                    )
                }
            }

            RotuloGrupo("Hora", Modifier.padding(top = EspacioMargen, bottom = EspacioAjustado))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(EspacioCorto), verticalArrangement = Arrangement.spacedBy(EspacioCorto)) {
                dia?.franjas?.forEach { f ->
                    FiltroOpcion(f.hora, f.hora == horaElegida, { hora = f.hora }, conMarca = false)
                }
            }

            RotuloGrupo("Modalidad", Modifier.padding(top = EspacioMargen, bottom = EspacioAjustado))
            Row(horizontalArrangement = Arrangement.spacedBy(EspacioCorto)) {
                Modalidad.entries.forEach { m ->
                    val disponible = franja != null && m in franja.modalidades
                    OpcionModalidad(
                        modalidad = m,
                        seleccionada = m == modalidadElegida,
                        habilitada = disponible,
                        alClicar = { modalidad = m },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        RotuloGrupo("Mensaje (opcional)", Modifier.padding(top = EspacioMargen, bottom = EspacioAjustado))
        Campo(
            valor = mensaje,
            alCambiar = { mensaje = it },
            marcador = "Algo que deba saber antes de la sesión",
            lineasMaximas = 4,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        Spacer(Modifier.height(EspacioAmplio))
        BotonPrimario(
            texto = "Enviar solicitud",
            conFlecha = true,
            habilitado = dia != null && horaElegida != null && modalidadElegida != null,
            alClicar = {
                intentoEnviar = true
                if (!temaFalta && dia != null && horaElegida != null && modalidadElegida != null) {
                    alEnviar(DatosSolicitud(materia, tema.trim(), dia.etiqueta, horaElegida, modalidadElegida, mensaje.trim()))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "${tutor.nombre.substringBefore(' ')} tiene $PLAZO_RESPUESTA para aceptar o rechazar. Si acepta, se habilita el chat.",
            color = TextoTercero,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = EspacioMedio)
        )
    }
}

/** Opción de modalidad en la solicitud; las que el tutor no ofrece en esa hora quedan deshabilitadas. */
@Composable
private fun OpcionModalidad(
    modalidad: Modalidad,
    seleccionada: Boolean,
    habilitada: Boolean,
    alClicar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fondo by animateColorAsState(if (seleccionada) BosqueMedio else Superficie, label = "fondoModalidad")
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 52.dp)
            .background(fondo, FormaTarjeta)
            .border(1.dp, if (seleccionada) Color.Transparent else Bosque.copy(alpha = 0.10f), FormaTarjeta)
            .selectable(selected = seleccionada, enabled = habilitada, role = Role.RadioButton, onClick = alClicar)
            .graphicsLayer { alpha = if (habilitada) 1f else 0.4f }
    ) {
        Icon(modalidad.icono, contentDescription = null, tint = if (seleccionada) Menta else BosqueAcento, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(EspacioCorto))
        Text(
            modalidad.etiqueta,
            color = if (seleccionada) SobreOscuro else TextoPrimario,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

/** Semana del tutor (RF4): un renglón por día con sus horas y la modalidad de cada una. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HorarioSemanal(horario: List<FranjaTutor>) {
    if (horario.isEmpty()) {
        Text("Aún no publica horarios.", color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .superficie(FormaOrganica)
    ) {
        val porDia = horario.groupBy { it.dia }.toSortedMap(compareBy { it.numero })
        porDia.entries.forEachIndexed { i, (dia, franjas) ->
            if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = EspacioNormal).height(1.dp).background(Divisor))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EspacioNormal, vertical = EspacioMedio)
            ) {
                Text(dia.nombre, color = TextoPrimario, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(92.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(EspacioFino),
                    verticalArrangement = Arrangement.spacedBy(EspacioFino),
                    modifier = Modifier.weight(1f)
                ) {
                    franjas.sortedBy { it.hora }.forEach { f ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(SuperficieClara, FormaMinima)
                                .padding(start = EspacioCorto, end = EspacioFino, top = EspacioMinimo, bottom = EspacioMinimo)
                        ) {
                            Text(f.hora, color = TextoPrimario, style = MaterialTheme.typography.labelMedium.merge(CifrasTabulares))
                            Spacer(Modifier.width(5.dp))
                            IconosModalidad(f.modalidades, tamano = 13.dp)
                        }
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(SuperficieClara.copy(alpha = 0.5f))
                .padding(horizontal = EspacioNormal, vertical = EspacioAjustado)
        ) {
            Modalidad.entries.forEach { m ->
                Icon(m.icono, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(EspacioMinimo))
                Text(m.etiqueta, color = TextoTercero, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(EspacioCercano))
            }
            Spacer(Modifier.weight(1f))
            Text("Sesiones de ${formatoHoras(1.0)}", color = TextoTercero, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Día en bloque vertical: abreviatura arriba, número grande. Seleccionado = bosque satinado. */
@Composable
private fun SelectorDia(dia: String, fecha: String, seleccionado: Boolean, alClicar: () -> Unit) {
    val (numero, mes) = fecha.split(" ").let { it[0] to it.getOrElse(1) { "" } }
    val fondo by animateColorAsState(if (seleccionado) BosqueMedio else Superficie, label = "fondoDia")
    val texto by animateColorAsState(if (seleccionado) SobreOscuro else TextoPrimario, label = "textoDia")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .sizeIn(minWidth = 58.dp, minHeight = 72.dp)
            .background(fondo, FormaTarjeta)
            .border(1.dp, if (seleccionado) Color.Transparent else Bosque.copy(alpha = 0.10f), FormaTarjeta)
            .selectable(selected = seleccionado, role = Role.RadioButton, onClick = alClicar)
            .semantics(mergeDescendants = true) { }
    ) {
        Text(dia, color = if (seleccionado) Menta else TextoTercero, style = MaterialTheme.typography.labelMedium)
        Text(numero, color = texto, style = MaterialTheme.typography.titleLarge.merge(CifrasTabulares))
        Text(mes, color = if (seleccionado) SobreOscuro.copy(alpha = 0.7f) else TextoTercero, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HojaReporte(nombre: String, alCerrar: () -> Unit, alEnviar: (String) -> Unit) {
    var motivo by rememberSaveable { mutableStateOf<String?>(null) }

    HojaNexo(
        titulo = "Reportar a ${nombre.substringBefore(' ')}",
        subtitulo = "Cuéntanos qué ocurrió. Un administrador revisará el caso; ${nombre.substringBefore(' ')} no sabrá quién lo reportó.",
        alCerrar = alCerrar
    ) {
        Spacer(Modifier.height(EspacioNormal))
        Column(
            Modifier
                .fillMaxWidth()
                .superficie(FormaOrganica)
                .selectableGroup()
        ) {
            motivosReporte.forEachIndexed { i, opcion ->
                if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 52.dp).height(1.dp).background(Divisor))
                OpcionRadio(opcion, opcion == motivo) { motivo = opcion }
            }
        }
        Spacer(Modifier.height(EspacioHolgado))
        BotonPrimario(
            texto = "Enviar reporte",
            habilitado = motivo != null,
            alClicar = { motivo?.let(alEnviar) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Opción de radio del sistema de diseño: anillo bosque con punto menta (DESIGN.md). */
@Composable
private fun OpcionRadio(texto: String, seleccionada: Boolean, alClicar: () -> Unit) {
    val borde by animateColorAsState(if (seleccionada) BosqueMedio else Bosque.copy(alpha = 0.3f), label = "radio")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = seleccionada, role = Role.RadioButton, onClick = alClicar)
            .background(if (seleccionada) TinteBosque.copy(alpha = 0.5f) else Color.Transparent)
            .padding(horizontal = EspacioNormal)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .border(if (seleccionada) 2.dp else 1.5.dp, borde, CircleShape)
        ) {
            if (seleccionada) Box(Modifier.size(9.dp).background(Menta, CircleShape))
        }
        Spacer(Modifier.width(EspacioNormal))
        Text(texto, color = TextoPrimario, style = MaterialTheme.typography.bodyLarge)
    }
}
