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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewModelScope
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/* ==================== ViewModel ==================== */

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TutorPerfilViewModel @Inject constructor(
    private val obtenerTutor: ObtenerTutorUseCase,
    private val obtenerResenas: ObtenerResenasUseCase,
    private val enviarSolicitud: EnviarSolicitudUseCase,
    private val reportarTutor: ReportarTutorUseCase
) : ViewModel() {

    data class Estado(
        val tutor: Tutor? = null,
        val resenas: List<Resena> = emptyList(),
        val aviso: String? = null
    )

    private val tutorId = MutableStateFlow("")
    private val aviso = MutableStateFlow<String?>(null)

    private val tutor: Flow<Tutor?> = tutorId.flatMapLatest { id ->
        if (id.isEmpty()) flowOf<Tutor?>(null) else obtenerTutor(id)
    }
    private val resenas: Flow<List<Resena>> = tutorId.flatMapLatest { id ->
        if (id.isEmpty()) flowOf<List<Resena>>(emptyList()) else obtenerResenas(id)
    }

    val estado = combine(tutor, resenas, aviso) { t, r, a ->
        Estado(tutor = t, resenas = r, aviso = a)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun cargar(id: String) {
        tutorId.value = id
    }

    fun solicitar(datos: DatosSolicitud) {
        val t = estado.value.tutor ?: return
        viewModelScope.launch {
            enviarSolicitud(t.id, t.nombre, datos.materia, datos.tema, datos.fecha, datos.hora, datos.modalidad, datos.mensaje)
            aviso.value = "Solicitud enviada. ${t.nombre.substringBefore(' ')} tiene 24 h para responder."
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
    val estado by viewModel.estado.collectAsState()
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
                Box(Modifier.padding(start = 12.dp, top = 6.dp).height(140.dp)) {
                    BotonIcono(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", alVolver, sobreOscuro = true)
                }
            }
            CircularProgressIndicator(
                color = BosqueAcento,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = lista,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item(key = "portada") {
                        Portada(alVolver = alVolver, alReportar = { mostrarReporte = true })
                    }
                    item(key = "identidad") {
                        Identidad(tutor, Modifier.aparicion(0, iniciado))
                    }
                    item(key = "cifras") {
                        Cifras(
                            tutor,
                            Modifier
                                .padding(start = 20.dp, end = 20.dp, top = 22.dp)
                                .aparicion(1, iniciado)
                        )
                    }
                    item(key = "sobre") {
                        Column(
                            Modifier
                                .padding(horizontal = 20.dp)
                                .padding(top = 18.dp)
                                .aparicion(2, iniciado)
                        ) {
                            EncabezadoSeccion("Sobre ${tutor.nombre.substringBefore(' ')}")
                            Text(tutor.bio, color = TextoSecundario, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(18.dp))
                            RotuloGrupo("Enseña")
                            Spacer(Modifier.height(10.dp))
                            Materias(tutor.materias)
                        }
                    }
                    item(key = "horario") {
                        Column(
                            Modifier
                                .padding(horizontal = 20.dp)
                                .padding(top = 22.dp)
                        ) {
                            EncabezadoSeccion("Horarios disponibles")
                            HorarioSemanal(tutor.horario)
                        }
                    }
                    item(key = "resenasTitulo") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(top = 22.dp, bottom = 6.dp)
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
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${formatoValoracion(tutor.rating)} · ${tutor.totalResenas}",
                                color = TextoSecundario,
                                style = MaterialTheme.typography.bodySmall.merge(CifrasTabulares)
                            )
                        }
                    }
                    if (estado.resenas.isEmpty()) {
                        item(key = "sinResenas") {
                            EstadoVacio(
                                icono = Icons.Rounded.RateReview,
                                titulo = "Aún sin reseñas",
                                mensaje = "Tras tu sesión podrás calificar a ${tutor.nombre.substringBefore(' ')}."
                            )
                        }
                    } else {
                        items(estado.resenas, key = { it.autor + it.fecha }) { resena ->
                            TarjetaResena(
                                resena = resena,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 10.dp)
                            )
                        }
                    }
                }

                BarraAcciones(
                    tutor = tutor,
                    alSolicitar = { mostrarSolicitud = true }
                )
            }

            VeloBarraEstado(visible = !portadaVisible)

            AvisoNexo(
                avisos,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 92.dp)
            )
        }
    }

    if (mostrarSolicitud && tutor != null) {
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
    CabeceraBosque(alturaOnda = 52.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            BotonIcono(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", alVolver, sobreOscuro = true)
            Spacer(Modifier.weight(1f))
            BotonIcono(Icons.Outlined.Flag, "Reportar tutor", alReportar, sobreOscuro = true)
        }
        Spacer(Modifier.height(72.dp))
    }
}

/** Avatar en insignia blanca montado sobre la onda (como el logo del acceso), nombre y estado. */
@Composable
private fun Identidad(tutor: Tutor, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Box(
            Modifier
                .solaparHaciaArriba(64.dp)
                .background(Color.White, CircleShape)
                .padding(4.dp)
        ) {
            Avatar(tutor.nombre, tamano = 98.dp)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            tutor.nombre,
            color = TextoPrimario,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.School, contentDescription = null, tint = TextoTercero, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                tutor.universidad,
                color = TextoSecundario,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(12.dp))
        val enLinea = tutor.enLinea
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Box(Modifier.size(7.dp).background(if (enLinea) Menta else GrisEstado.copy(alpha = 0.6f), CircleShape))
            Spacer(Modifier.width(7.dp))
            Text(
                if (enLinea) "Disponible ahora" else "Desconectado",
                color = if (enLinea) VerdeEstado else GrisEstado,
                style = MaterialTheme.typography.labelMedium
            )
            Text("  ·  ${tutor.handle}", color = TextoTercero, style = MaterialTheme.typography.labelMedium)
        }
        if (tutor.modalidades.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            .height(78.dp)
            .superficie(RoundedCornerShape(12.dp))
    ) {
        Cifra(formatoValoracion(tutor.rating), "valoración", Modifier.weight(1f), conEstrella = true)
        Box(Modifier.fillMaxHeight().padding(vertical = 18.dp).width(1.dp).background(Divisor))
        Cifra("${tutor.sesionesTotales}", "sesiones", Modifier.weight(1f))
        Box(Modifier.fillMaxHeight().padding(vertical = 18.dp).width(1.dp).background(Divisor))
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
                Icon(Icons.Rounded.Star, contentDescription = null, tint = Miel, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
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
            .superficie(RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(resena.autor, tamano = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                TextoLinea(resena.autor, MaterialTheme.typography.titleSmall, TextoPrimario)
                TextoLinea(resena.fecha, MaterialTheme.typography.bodySmall, TextoTercero)
            }
            Estrellas(resena.estrellas, tamano = 13.dp)
        }
        Spacer(Modifier.height(10.dp))
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
            .background(Color.White)
            .drawBehind { drawLine(Divisor, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Column {
            Text(
                formatoPrecio(tutor.precioHora),
                color = TextoPrimario,
                style = MaterialTheme.typography.headlineSmall.merge(CifrasTabulares)
            )
            Text("por hora", color = TextoTercero, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(16.dp))
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

/** Una fecha concreta en la que el tutor tiene al menos una franja publicada. */
private data class DiaSolicitable(val fecha: LocalDate, val franjas: List<FranjaTutor>) {
    val abreviatura: String get() = DiaSemana.desde(fecha.dayOfWeek.value).abreviatura
    val etiqueta: String get() = fecha.format(formatoFecha).replace(".", "")
}

private val localeColombia = Locale("es", "CO")
private val formatoFecha = DateTimeFormatter.ofPattern("d MMM", localeColombia)

/** Próximos días (desde mañana, dos semanas) con horario del tutor; la fecha nunca es anterior a hoy (UC5). */
private fun diasSolicitables(horario: List<FranjaTutor>): List<DiaSolicitable> {
    val hoy = LocalDate.now()
    return (1L..14L).mapNotNull { desfase ->
        val fecha = hoy.plusDays(desfase)
        val franjas = horario.filter { it.dia.numero == fecha.dayOfWeek.value }.sortedBy { it.hora }
        if (franjas.isEmpty()) null else DiaSolicitable(fecha, franjas)
    }
}

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
    val franja = dia?.franjas?.firstOrNull { it.hora == hora } ?: dia?.franjas?.firstOrNull()
    // Hora y modalidad siempre válidas para el día elegido: si no, se toma la primera disponible.
    val horaElegida = franja?.hora
    val modalidadElegida = modalidad?.takeIf { franja != null && it in franja.modalidades }
        ?: franja?.modalidades?.let { m -> Modalidad.entries.first { it in m } }
    val temaFalta = tema.isBlank()

    HojaNexo(
        titulo = "Solicitar tutoría",
        subtitulo = "con ${tutor.nombre} · ${formatoPrecio(tutor.precioHora)} la hora",
        alCerrar = alCerrar
    ) {
        RotuloGrupo("Materia", Modifier.padding(top = 22.dp, bottom = 10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tutor.materias.forEach { FiltroOpcion(it, it == materia, { materia = it }) }
        }

        RotuloGrupo("Tema a reforzar", Modifier.padding(top = 20.dp, bottom = 10.dp))
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
                modifier = Modifier.padding(start = 20.dp, top = 6.dp)
            )
        }

        if (dias.isEmpty()) {
            Text(
                "${tutor.nombre.substringBefore(' ')} no tiene horarios publicados por ahora.",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp)
            )
        } else {
            RotuloGrupo("Día", Modifier.padding(top = 20.dp, bottom = 10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.selectableGroup()
            ) {
                items(dias.size) { i ->
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

            RotuloGrupo("Hora", Modifier.padding(top = 20.dp, bottom = 10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dia?.franjas?.forEach { f ->
                    FiltroOpcion(f.hora, f.hora == horaElegida, { hora = f.hora }, conMarca = false)
                }
            }

            RotuloGrupo("Modalidad", Modifier.padding(top = 20.dp, bottom = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        RotuloGrupo("Mensaje (opcional)", Modifier.padding(top = 20.dp, bottom = 10.dp))
        Campo(
            valor = mensaje,
            alCambiar = { mensaje = it },
            marcador = "Algo que deba saber antes de la sesión",
            lineasMaximas = 4,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        Spacer(Modifier.height(24.dp))
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
            "${tutor.nombre.substringBefore(' ')} tiene 24 horas para aceptar o rechazar. Si acepta, se habilita el chat.",
            color = TextoTercero,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
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
    val fondo by animateColorAsState(if (seleccionada) BosqueMedio else Color.White, label = "fondoModalidad")
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(52.dp)
            .background(fondo, RoundedCornerShape(12.dp))
            .border(1.dp, if (seleccionada) Color.Transparent else Bosque.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .selectable(selected = seleccionada, enabled = habilitada, role = Role.RadioButton, onClick = alClicar)
            .graphicsLayer { alpha = if (habilitada) 1f else 0.4f }
    ) {
        Icon(modalidad.icono, contentDescription = null, tint = if (seleccionada) Menta else BosqueAcento, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            modalidad.etiqueta,
            color = if (seleccionada) Color.White else TextoPrimario,
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
            .superficie(RoundedCornerShape(12.dp))
    ) {
        val porDia = horario.groupBy { it.dia }.toSortedMap(compareBy { it.numero })
        porDia.entries.forEachIndexed { i, (dia, franjas) ->
            if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Divisor))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(dia.nombre, color = TextoPrimario, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(92.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    franjas.sortedBy { it.hora }.forEach { f ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(SuperficieClara, RoundedCornerShape(6.dp))
                                .padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Modalidad.entries.forEach { m ->
                Icon(m.icono, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(m.etiqueta, color = TextoTercero, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(14.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("Sesiones de 1 h", color = TextoTercero, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Día en bloque vertical: abreviatura arriba, número grande. Seleccionado = bosque satinado. */
@Composable
private fun SelectorDia(dia: String, fecha: String, seleccionado: Boolean, alClicar: () -> Unit) {
    val (numero, mes) = fecha.split(" ").let { it[0] to it.getOrElse(1) { "" } }
    val fondo by animateColorAsState(if (seleccionado) BosqueMedio else Color.White, label = "fondoDia")
    val texto by animateColorAsState(if (seleccionado) Color.White else TextoPrimario, label = "textoDia")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = 58.dp, height = 72.dp)
            .background(fondo, RoundedCornerShape(12.dp))
            .border(1.dp, if (seleccionado) Color.Transparent else Bosque.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .selectable(selected = seleccionado, role = Role.RadioButton, onClick = alClicar)
            .semantics(mergeDescendants = true) { }
    ) {
        Text(dia, color = if (seleccionado) Menta else TextoTercero, style = MaterialTheme.typography.labelMedium)
        Text(numero, color = texto, style = MaterialTheme.typography.titleLarge.merge(CifrasTabulares))
        Text(mes, color = if (seleccionado) Color.White.copy(alpha = 0.7f) else TextoTercero, style = MaterialTheme.typography.bodySmall)
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
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .superficie(RoundedCornerShape(12.dp))
                .selectableGroup()
        ) {
            motivosReporte.forEachIndexed { i, opcion ->
                if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 52.dp).height(1.dp).background(Divisor))
                OpcionRadio(opcion, opcion == motivo) { motivo = opcion }
            }
        }
        Spacer(Modifier.height(22.dp))
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
            .padding(horizontal = 16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .border(if (seleccionada) 2.dp else 1.5.dp, borde, CircleShape)
        ) {
            if (seleccionada) Box(Modifier.size(9.dp).background(Menta, CircleShape))
        }
        Spacer(Modifier.width(16.dp))
        Text(texto, color = TextoPrimario, style = MaterialTheme.typography.bodyLarge)
    }
}
