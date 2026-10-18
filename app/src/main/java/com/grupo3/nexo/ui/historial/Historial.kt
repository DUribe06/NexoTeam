package com.grupo3.nexo.ui.historial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.usecase.CalificarSesionUseCase
import com.grupo3.nexo.domain.usecase.ObtenerHistorialUseCase
import com.grupo3.nexo.ui.components.Avatar
import com.grupo3.nexo.ui.components.AvisoNexo
import com.grupo3.nexo.ui.components.BarraSuperior
import com.grupo3.nexo.ui.components.BotonPrimario
import com.grupo3.nexo.ui.components.BotonSecundario
import com.grupo3.nexo.ui.components.Campo
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.FiltroOpcion
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.Estrellas
import com.grupo3.nexo.ui.components.HojaNexo
import com.grupo3.nexo.ui.components.RotuloGrupo
import com.grupo3.nexo.ui.components.TextoLinea
import com.grupo3.nexo.ui.components.formatoHoras
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.theme.Bosque
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.Miel
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.VerdeEstado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.grupo3.nexo.domain.model.EstadoSesion
import com.grupo3.nexo.ui.theme.MielOscura
import com.grupo3.nexo.ui.theme.BosqueProfundo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import javax.inject.Inject

/* ==================== ViewModel ==================== */

@HiltViewModel
class HistorialViewModel @Inject constructor(
    obtenerHistorial: ObtenerHistorialUseCase,
    private val calificarSesion: CalificarSesionUseCase
) : ViewModel() {

    data class Estado(
        val cargando: Boolean = true,
        val todas: List<Sesion> = emptyList(),
        val materia: String? = null
    ) {
        /** Sesiones del filtro actual, en orden cronológico (la más reciente arriba). */
        val sesiones get() = todas.filter { materia == null || it.materia == materia }
        val materias get() = todas.map { it.materia }.distinct()
        val porCalificar get() = todas.count { it.porCalificar }
        val completadas get() = todas.count { it.estado == EstadoSesion.COMPLETADA }
        val horas get() = todas.filter { it.estado == EstadoSesion.COMPLETADA }.sumOf { it.duracionHoras }
    }

    private val materia = MutableStateFlow<String?>(null)

    val estado = combine(obtenerHistorial(), materia) { lista, m ->
        Estado(cargando = false, todas = lista, materia = m)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun filtrar(valor: String?) {
        materia.value = if (materia.value == valor) null else valor
    }

    fun calificar(sesionId: String, estrellas: Int, comentario: String?) {
        viewModelScope.launch { calificarSesion(sesionId, estrellas, comentario) }
    }
}

/* ==================== Pantalla ==================== */

@Composable
fun PantallaHistorial(
    alVolver: () -> Unit,
    alBuscar: () -> Unit,
    viewModel: HistorialViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsState()
    var porCalificarId by rememberSaveable { mutableStateOf<String?>(null) }
    val porCalificar = estado.todas.firstOrNull { it.id == porCalificarId }
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BarraSuperior("Historial", alVolver)

            if (estado.todas.isEmpty() && !estado.cargando) {
                EstadoVacio(
                    icono = Icons.Rounded.History,
                    titulo = "Aún no hay sesiones",
                    mensaje = "Tus tutorías completadas aparecerán aquí para que puedas calificarlas.",
                    accion = "Buscar tutor",
                    alAccion = alBuscar
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
                ) {
                    item(key = "resumen") { Resumen(estado) }
                    item(key = "rotulo") {
                        RotuloGrupo("Tus sesiones", Modifier.padding(start = 4.dp, top = 12.dp))
                    }
                    if (estado.materias.size > 1) {
                        item(key = "filtros") {
                            FiltrosMateria(estado.materias, estado.materia, viewModel::filtrar)
                        }
                    }
                    items(estado.sesiones, key = { it.id }) { sesion ->
                        TarjetaHistorial(sesion, alCalificar = { porCalificarId = sesion.id }, modifier = Modifier.animateItem())
                    }
                }
            }
        }
        AvisoNexo(avisos, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }

    porCalificar?.let { sesion ->
        HojaCalificacion(
            sesion = sesion,
            alCerrar = { porCalificarId = null },
            alEnviar = { estrellas, comentario ->
                viewModel.calificar(sesion.id, estrellas, comentario)
                porCalificarId = null
                alcance.launch { avisos.showSnackbar("Gracias, tu calificación ayuda a otros estudiantes") }
            }
        )
    }
}

/* ==================== Componentes privados ==================== */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltrosMateria(materias: List<String>, actual: String?, alElegir: (String?) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        FiltroOpcion("Todas", actual == null, { alElegir(null) }, conMarca = false)
        materias.forEach { m -> FiltroOpcion(m, actual == m, { alElegir(m) }, conMarca = false) }
    }
}

@Composable
private fun Resumen(estado: HistorialViewModel.Estado) {
    // Sin contenedor, como las cifras del Perfil: solo líneas finas entre ellas
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        CifraResumen("${estado.completadas}", if (estado.completadas == 1) "sesión" else "sesiones", Modifier.weight(1f))
        Box(Modifier.fillMaxHeight().padding(vertical = 10.dp).width(1.dp).background(Divisor))
        CifraResumen(formatoHoras(estado.horas), "de estudio", Modifier.weight(1f))
        Box(Modifier.fillMaxHeight().padding(vertical = 10.dp).width(1.dp).background(Divisor))
        CifraResumen("${estado.porCalificar}", "por calificar", Modifier.weight(1f), resaltar = estado.porCalificar > 0)
    }
}

@Composable
private fun CifraResumen(valor: String, etiqueta: String, modifier: Modifier = Modifier, resaltar: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .semantics(mergeDescendants = true) { }
    ) {
        Text(
            valor,
            color = if (resaltar) MielOscura else BosqueProfundo,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp).merge(CifrasTabulares)
        )
        Text(etiqueta, color = TextoTercero, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TarjetaHistorial(sesion: Sesion, alCalificar: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .superficie(RoundedCornerShape(20.dp))
            .graphicsLayer { alpha = if (sesion.estado == EstadoSesion.COMPLETADA) 1f else 0.82f }
            .padding(14.dp)
    ) {
        Avatar(sesion.conQuien, tamano = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            TextoLinea(sesion.materia, MaterialTheme.typography.titleMedium, TextoPrimario)
            TextoLinea(sesion.conQuien, MaterialTheme.typography.bodyMedium, TextoSecundario)
            Text(
                "${sesion.fecha} · ${sesion.hora} · ${formatoHoras(sesion.duracionHoras)} · ${sesion.modalidad.etiqueta}",
                color = TextoTercero,
                style = MaterialTheme.typography.bodySmall.merge(CifrasTabulares)
            )
            sesion.comentario?.let {
                Text(
                    "“$it”",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (sesion.estado != EstadoSesion.COMPLETADA) {
            // Cancelada o sin asistencia: no se califica; el estado se lee como texto discreto
            Text(sesion.estado.etiqueta, color = TextoTercero, style = MaterialTheme.typography.labelMedium)
            return@Row
        }
        AnimatedContent(
            targetState = sesion.calificacion,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "calificacion"
        ) { nota ->
            if (nota != null) {
                Estrellas(nota, tamano = 13.dp)
            } else {
                BotonSecundario("Calificar", alCalificar, icono = Icons.Rounded.Star, compacto = true, colorIcono = Miel)
            }
        }
    }
}

private val etiquetasNota = listOf("Toca una estrella", "Mala", "Regular", "Buena", "Muy buena", "Excelente")

@Composable
private fun HojaCalificacion(sesion: Sesion, alCerrar: () -> Unit, alEnviar: (Int, String?) -> Unit) {
    var estrellas by rememberSaveable { mutableIntStateOf(0) }
    var comentario by rememberSaveable { mutableStateOf("") }

    HojaNexo(
        titulo = "¿Cómo te fue con ${sesion.conQuien.substringBefore(' ')}?",
        subtitulo = "${sesion.materia} · ${sesion.fecha}",
        alCerrar = alCerrar
    ) {
        Spacer(Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            repeat(5) { indice ->
                val valor = indice + 1
                val llena = valor <= estrellas
                // Cada estrella "salta" al llenarse, en cascada desde la primera.
                val escala by animateFloatAsState(
                    if (llena) 1f else 0.86f,
                    spring(dampingRatio = 0.45f, stiffness = 700f - indice * 60f),
                    label = "estrella$indice"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .selectable(
                            selected = valor == estrellas,
                            role = Role.RadioButton,
                            onClick = { estrellas = valor }
                        )
                        .semantics { contentDescription = "$valor ${if (valor == 1) "estrella" else "estrellas"}" }
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = if (llena) Miel else Bosque.copy(alpha = 0.12f),
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer {
                                scaleX = escala
                                scaleY = escala
                            }
                    )
                }
            }
        }
        AnimatedContent(
            targetState = estrellas,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(100)) },
            label = "etiquetaNota",
            modifier = Modifier.fillMaxWidth()
        ) { n ->
            Text(
                etiquetasNota[n],
                color = if (n == 0) TextoTercero else VerdeEstado,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
        RotuloGrupo("Reseña (opcional)", Modifier.padding(top = 22.dp, bottom = 10.dp))
        Campo(
            valor = comentario,
            alCambiar = { comentario = it },
            marcador = "¿Qué le dirías a otro estudiante?",
            lineasMaximas = 4,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        Text(
            "Solo puedes calificar una vez cada sesión.",
            color = TextoTercero,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 20.dp, top = 6.dp)
        )
        Spacer(Modifier.height(20.dp))
        BotonPrimario(
            texto = "Enviar calificación",
            habilitado = estrellas > 0,
            alClicar = { alEnviar(estrellas, comentario.ifBlank { null }) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
