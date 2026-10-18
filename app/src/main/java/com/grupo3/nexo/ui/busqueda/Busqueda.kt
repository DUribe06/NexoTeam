package com.grupo3.nexo.ui.busqueda

import androidx.compose.ui.text.font.FontWeight
import com.grupo3.nexo.ui.theme.BosqueAcento
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.Franja
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.usecase.BuscarTutoresUseCase
import com.grupo3.nexo.domain.usecase.FiltroTutores
import com.grupo3.nexo.ui.components.Avatar
import com.grupo3.nexo.ui.components.BotonIcono
import com.grupo3.nexo.ui.components.Pestanas
import com.grupo3.nexo.ui.components.descripcion
import com.grupo3.nexo.ui.components.filaAgrupada
import com.grupo3.nexo.ui.components.BotonPrimario
import com.grupo3.nexo.ui.components.BotonTexto
import com.grupo3.nexo.ui.components.Campo
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.FiltroOpcion
import com.grupo3.nexo.ui.components.HojaNexo
import com.grupo3.nexo.ui.components.IconosModalidad
import com.grupo3.nexo.ui.components.InsigniaConteo
import com.grupo3.nexo.ui.components.RotuloGrupo
import com.grupo3.nexo.ui.components.TextoLinea
import com.grupo3.nexo.ui.components.BarraSuperior
import com.grupo3.nexo.ui.components.Valoracion
import com.grupo3.nexo.ui.components.formatoPrecio
import com.grupo3.nexo.ui.components.icono
import com.grupo3.nexo.ui.components.pulsable
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.components.superficiePulsable
import com.grupo3.nexo.ui.components.universidadCorta
import com.grupo3.nexo.ui.navigation.Rutas
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.Menta
import com.grupo3.nexo.ui.theme.Salvia
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.TinteBosque
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/* ==================== Áreas de estudio ==================== */

/**
 * Agrupa las materias en áreas reconocibles para filtrar con un toque
 * ("Matemáticas" encuentra Cálculo I, Álgebra y Matemática Básica).
 */
enum class AreaEstudio(val etiqueta: String, val claves: List<String>) {
    MATEMATICAS("Matemáticas", listOf("Cálculo", "Álgebra", "Matemática")),
    PROGRAMACION("Programación", listOf("Programación", "Python", "Java")),
    CIENCIAS("Ciencias", listOf("Física", "Química", "Biología", "Estadística")),
    IDIOMAS("Idiomas", listOf("Inglés", "Redacción")),
    DATOS("Bases de datos", listOf("Bases de Datos", "SQL")),
    DISENO("Diseño", listOf("Diseño", "Figma")),
    NEGOCIOS("Negocios", listOf("Contabilidad", "Economía"));

    companion object {
        fun desde(nombre: String?): AreaEstudio? = entries.firstOrNull { it.name == nombre }

        /** Área a la que pertenece una materia (para sugerir tutores alternativos, RF6). */
        fun deMateria(materia: String): AreaEstudio? =
            entries.firstOrNull { area -> area.claves.any { materia.contains(it, ignoreCase = true) } }
    }
}

/* ==================== ViewModel ==================== */

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BusquedaViewModel @Inject constructor(
    buscarTutores: BuscarTutoresUseCase,
    estadoGuardado: SavedStateHandle
) : ViewModel() {

    data class Estado(
        val consulta: String = "",
        val area: AreaEstudio? = null,
        val modalidad: Modalidad? = null,
        val franja: Franja? = null,
        val soloEnLinea: Boolean = false,
        val resultados: List<Tutor> = emptyList(),
        val cargando: Boolean = true
    ) {
        /** Filtros de la hoja (no cuenta texto ni área, que están a la vista). */
        val filtrosHoja get() = listOf(modalidad != null, franja != null, soloEnLinea).count { it }
        val hayFiltros get() = consulta.isNotBlank() || area != null || filtrosHoja > 0
    }

    private val consulta = MutableStateFlow("")
    private val area = MutableStateFlow(AreaEstudio.desde(estadoGuardado.get<String>(Rutas.Busqueda.ARG_AREA)))
    private val modalidad = MutableStateFlow<Modalidad?>(null)
    private val franja = MutableStateFlow<Franja?>(null)
    private val soloEnLinea = MutableStateFlow(false)

    private val filtro = combine(consulta, area, modalidad, franja, soloEnLinea) { q, a, m, f, l ->
        FiltroTutores(consulta = q, materias = a?.claves.orEmpty(), modalidad = m, franja = f, soloEnLinea = l)
    }

    val estado = combine(
        combine(consulta, area, modalidad, franja, soloEnLinea) { q, a, m, f, l -> Estado(q, a, m, f, l) },
        filtro.flatMapLatest { buscarTutores(it) }
    ) { base, resultados -> base.copy(resultados = resultados, cargando = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun cambiarConsulta(valor: String) { consulta.value = valor }
    fun alternarArea(valor: AreaEstudio) { area.update { if (it == valor) null else valor } }
    fun elegirModalidad(valor: Modalidad?) { modalidad.value = valor }
    fun elegirFranja(valor: Franja?) { franja.value = valor }
    fun cambiarEnLinea(valor: Boolean) { soloEnLinea.value = valor }
    fun limpiarHoja() {
        modalidad.value = null
        franja.value = null
        soloEnLinea.value = false
    }
    fun limpiarTodo() {
        consulta.value = ""
        area.value = null
        limpiarHoja()
    }
}

/* ==================== Pantalla ==================== */

@Composable
fun PantallaBusqueda(
    alAbrirTutor: (String) -> Unit,
    alVolver: () -> Unit,
    viewModel: BusquedaViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val focos = LocalFocusManager.current
    var mostrarFiltros by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Buscar tutores", alVolver) {
            BotonIcono(
                Icons.Rounded.Tune,
                descripcion = if (estado.filtrosHoja > 0) "Filtros, ${estado.filtrosHoja} activos" else "Filtros",
                alClicar = { mostrarFiltros = true },
                insignia = estado.filtrosHoja
            )
        }

        Campo(
            valor = estado.consulta,
            alCambiar = viewModel::cambiarConsulta,
            marcador = "Materia, tutor o universidad",
            icono = Icons.Rounded.Search,
            limpiable = true,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Search),
            acciones = KeyboardActions(onSearch = { focos.clearFocus() }),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)
        )

        Pestanas(
            opciones = listOf("Todas") + AreaEstudio.entries.map { it.etiqueta },
            seleccionada = estado.area?.let { it.ordinal + 1 } ?: 0,
            alElegir = { i -> if (i == 0) estado.area?.let(viewModel::alternarArea) else viewModel.alternarArea(AreaEstudio.entries[i - 1]) },
            modifier = Modifier.padding(top = 8.dp)
        )

        AnimatedContent(
            targetState = estado.resultados.isEmpty() && !estado.cargando,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
            label = "resultados",
            modifier = Modifier.weight(1f)
        ) { vacio ->
            if (vacio) {
                EstadoVacio(
                    icono = Icons.Rounded.SearchOff,
                    titulo = "Ningún tutor coincide",
                    mensaje = "Amplía la búsqueda: prueba otra franja horaria, la otra modalidad o quita el área.",
                    accion = if (estado.hayFiltros) "Quitar todos los filtros" else null,
                    alAccion = viewModel::limpiarTodo
                )
            } else {
                // Sin barra inferior en esta pantalla: el final de la lista respeta la barra del sistema
                val margenSistema = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp + margenSistema)) {
                    item(key = "conteo") {
                        Text(
                            resumenResultados(estado),
                            color = TextoTercero,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                        )
                    }
                    itemsIndexed(estado.resultados, key = { _, t -> t.id }) { i, tutor ->
                        FilaTutor(
                            tutor,
                            alAbrir = { alAbrirTutor(tutor.id) },
                            modifier = Modifier
                                .animateItem()
                                .filaAgrupada(i, estado.resultados.size, sangria = 82.dp)
                        )
                    }
                }
            }
        }
    }

    if (mostrarFiltros) {
        HojaFiltros(
            estado = estado,
            alElegirModalidad = viewModel::elegirModalidad,
            alElegirFranja = viewModel::elegirFranja,
            alCambiarEnLinea = viewModel::cambiarEnLinea,
            alLimpiar = viewModel::limpiarHoja,
            alCerrar = { mostrarFiltros = false }
        )
    }
}

private fun resumenResultados(e: BusquedaViewModel.Estado): String = buildString {
    append(if (e.resultados.size == 1) "1 tutor" else "${e.resultados.size} tutores")
    val detalles = listOfNotNull(
        e.modalidad?.etiqueta?.lowercase(),
        e.franja?.etiqueta?.lowercase()?.let { "en la $it" },
        if (e.soloEnLinea) "en línea" else null
    )
    if (detalles.isNotEmpty()) append(" · " + detalles.joinToString(" · "))
    append(" · por calificación")
}

/** Filtros de horario y modalidad (HU-05). Se aplican al instante; el botón muestra el resultado. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HojaFiltros(
    estado: BusquedaViewModel.Estado,
    alElegirModalidad: (Modalidad?) -> Unit,
    alElegirFranja: (Franja?) -> Unit,
    alCambiarEnLinea: (Boolean) -> Unit,
    alLimpiar: () -> Unit,
    alCerrar: () -> Unit
) {
    HojaNexo(titulo = "Filtros", subtitulo = "Encuentra a quien pueda cuando tú puedes.", alCerrar = alCerrar) {
        RotuloGrupo("Modalidad", Modifier.padding(top = 22.dp, bottom = 10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FiltroOpcion("Cualquiera", estado.modalidad == null, { alElegirModalidad(null) }, conMarca = false)
            Modalidad.entries.forEach { m ->
                FiltroIcono(m.etiqueta, m.icono, estado.modalidad == m) { alElegirModalidad(m) }
            }
        }

        RotuloGrupo("Horario", Modifier.padding(top = 20.dp, bottom = 10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FiltroOpcion("Cualquiera", estado.franja == null, { alElegirFranja(null) }, conMarca = false)
            Franja.entries.forEach { f ->
                FiltroOpcion("${f.etiqueta} · ${"%02d".format(f.desde)}–${f.hasta + 1}h", estado.franja == f, { alElegirFranja(f) }, conMarca = false)
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .superficie()
                .toggleable(value = estado.soloEnLinea, role = Role.Switch, onValueChange = alCambiarEnLinea)
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Solo en línea ahora", color = TextoPrimario, style = MaterialTheme.typography.titleMedium)
                Text("Tutores que pueden responder de inmediato", color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = estado.soloEnLinea,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = BosqueMedio,
                    checkedThumbColor = Color.White,
                    checkedIconColor = BosqueMedio,
                    uncheckedTrackColor = TinteBosque,
                    uncheckedBorderColor = Salvia.copy(alpha = 0.6f),
                    uncheckedThumbColor = Salvia
                )
            )
        }

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BotonTexto("Limpiar", alLimpiar, color = TextoSecundario)
            Spacer(Modifier.width(8.dp))
            BotonPrimario(
                texto = when (estado.resultados.size) {
                    0 -> "Sin resultados"
                    1 -> "Ver 1 tutor"
                    else -> "Ver ${estado.resultados.size} tutores"
                },
                habilitado = estado.resultados.isNotEmpty(),
                alClicar = alCerrar,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Opción de filtro con ícono (modalidad), mismo estilo que [FiltroOpcion]. */
@Composable
private fun FiltroIcono(texto: String, icono: ImageVector, seleccionado: Boolean, alClicar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(40.dp)
            .pulsable(rol = Role.RadioButton, alClicar = alClicar)
            .then(
                if (seleccionado) Modifier.background(TinteBosque, RoundedCornerShape(8.dp)).border(1.dp, BosqueAcento.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                else Modifier.superficie(RoundedCornerShape(8.dp))
            )
            .padding(horizontal = 14.dp)
    ) {
        Icon(icono, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, color = TextoPrimario, style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Medium))
    }
}

/**
 * Fila de tutor para listas agrupadas: avatar con estado, nombre, materias y una línea
 * de contexto (universidad · modalidad); a la derecha, valoración y tarifa.
 * No dibuja fondo propio: lo pone la lista (ver [filaAgrupada]).
 */
@Composable
fun FilaTutor(tutor: Tutor, alAbrir: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Ver perfil de ${tutor.nombre}", role = Role.Button, onClick = alAbrir)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Avatar(tutor.nombre, tamano = 50.dp, enLinea = tutor.enLinea)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            TextoLinea(tutor.nombre, MaterialTheme.typography.titleMedium, TextoPrimario)
            TextoLinea(tutor.materias.joinToString(" · "), MaterialTheme.typography.bodyMedium, TextoSecundario)
            TextoLinea(
                listOf(universidadCorta(tutor.universidad), tutor.modalidades.descripcion()).joinToString(" · "),
                MaterialTheme.typography.bodySmall,
                TextoTercero
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Valoracion(tutor.rating)
            Spacer(Modifier.height(4.dp))
            Text(
                formatoPrecio(tutor.precioHora),
                color = TextoPrimario,
                style = MaterialTheme.typography.titleSmall.merge(CifrasTabulares)
            )
        }
    }
}
