package com.grupo3.nexo.ui.busqueda

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo3.nexo.domain.model.AreaEstudio
import com.grupo3.nexo.domain.model.Franja
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.ui.components.Avatar
import com.grupo3.nexo.ui.components.BotonIcono
import com.grupo3.nexo.ui.components.BotonPrimario
import com.grupo3.nexo.ui.components.BotonTexto
import com.grupo3.nexo.ui.components.CabeceraSeccion
import com.grupo3.nexo.ui.components.Campo
import com.grupo3.nexo.ui.components.Carga
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.ErrorDeLista
import com.grupo3.nexo.ui.components.EsqueletoLista
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.FiltroOpcion
import com.grupo3.nexo.ui.components.HojaNexo
import com.grupo3.nexo.ui.components.IconosNexo
import com.grupo3.nexo.ui.components.Pestanas
import com.grupo3.nexo.ui.components.RotuloGrupo
import com.grupo3.nexo.ui.components.TextoLinea
import com.grupo3.nexo.ui.components.Valoracion
import com.grupo3.nexo.ui.components.areaTactilSinCambiarMedida
import com.grupo3.nexo.ui.components.cargaDe
import com.grupo3.nexo.ui.components.descripcion
import com.grupo3.nexo.ui.components.filaAgrupada
import com.grupo3.nexo.ui.components.formatoPrecio
import com.grupo3.nexo.ui.components.icono
import com.grupo3.nexo.ui.components.pulsable
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.components.universidadCorta
import com.grupo3.nexo.ui.theme.BordeRadio
import com.grupo3.nexo.ui.theme.BordeSeleccion
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueMedio
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
import com.grupo3.nexo.ui.theme.TamanoAvatarLista
import com.grupo3.nexo.ui.theme.FormaOpcion
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.theme.Icono17
import com.grupo3.nexo.ui.theme.Salvia
import com.grupo3.nexo.ui.theme.SobreOscuro
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.TinteBosque
import com.grupo3.nexo.ui.theme.cambioContenido
import com.grupo3.nexo.ui.theme.enfasisSeleccion

/**
 * Alias temporal: [AreaEstudio] pasó a `domain.model`. Solicitudes.kt aún lo importa desde aquí;
 * al cambiar su import, se borra esta línea.
 */
typealias AreaEstudio = com.grupo3.nexo.domain.model.AreaEstudio


/* ==================== Pantalla ==================== */

@Composable
fun PantallaBusqueda(
    alAbrirTutor: (String) -> Unit,
    alVolver: () -> Unit,
    viewModel: BusquedaViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val focos = LocalFocusManager.current
    val opcionesArea = remember { listOf("Todas") + AreaEstudio.entries.map { it.etiqueta } }
    var mostrarFiltros by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        CabeceraSeccion("Buscar tutores", alVolver = alVolver) {
            BotonIcono(
                IconosNexo.Filtros,
                descripcion = if (estado.filtrosHoja > 0) "Filtros, ${estado.filtrosHoja} ${if (estado.filtrosHoja == 1) "activo" else "activos"}" else "Filtros",
                alClicar = { mostrarFiltros = true },
                sobreOscuro = true,
                insignia = estado.filtrosHoja
            )
        }

        Campo(
            valor = estado.consulta,
            alCambiar = viewModel::cambiarConsulta,
            marcador = "Materia, tutor o universidad",
            icono = IconosNexo.Lupa,
            limpiable = true,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Search),
            acciones = KeyboardActions(onSearch = { focos.clearFocus() }),
            modifier = Modifier.padding(start = EspacioMargen, end = EspacioMargen, top = EspacioCercano)
        )

        Pestanas(
            opciones = opcionesArea,
            seleccionada = estado.area?.let { it.ordinal + 1 } ?: 0,
            alElegir = { i -> if (i == 0) estado.area?.let(viewModel::alternarArea) else viewModel.alternarArea(AreaEstudio.entries[i - 1]) },
            modifier = Modifier.padding(top = EspacioCorto)
        )

        AnimatedContent(
            targetState = cargaDe(estado.cargando, estado.resultados.isEmpty()),
            transitionSpec = { cambioContenido() },
            label = "resultados",
            modifier = Modifier.weight(1f)
        ) { carga ->
            if (carga == Carga.CARGANDO) {
                EsqueletoLista()
            } else if (carga == Carga.ERROR) {
                ErrorDeLista(titulo = "No pudimos cargar los tutores")
            } else if (carga == Carga.VACIO) {
                EstadoVacio(
                    icono = IconosNexo.SinResultados,
                    titulo = "Ningún tutor coincide",
                    mensaje = "Amplía la búsqueda: prueba otra franja horaria, la otra modalidad o quita el área.",
                    accion = if (estado.hayFiltros) "Quitar todos los filtros" else "Ver todas las áreas",
                    alAccion = viewModel::limpiarTodo
                )
            } else {
                // Sin barra inferior en esta pantalla: el final de la lista respeta la barra del sistema
                val margenSistema = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyColumn(contentPadding = PaddingValues(start = EspacioMargen, end = EspacioMargen, top = EspacioNormal, bottom = EspacioAmplio + margenSistema)) {
                    item(key = "conteo") {
                        Text(
                            resumenResultados(estado),
                            color = TextoTercero,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = EspacioMinimo, bottom = EspacioAjustado)
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
    append(" · ordenados por calificación")
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
        RotuloGrupo("Modalidad", Modifier.padding(top = EspacioHolgado, bottom = EspacioAjustado))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(EspacioCorto),
            verticalArrangement = Arrangement.spacedBy(EspacioCorto),
            modifier = Modifier.selectableGroup()
        ) {
            FiltroOpcion("Cualquiera", estado.modalidad == null, { alElegirModalidad(null) }, conMarca = false)
            Modalidad.entries.forEach { m ->
                FiltroIcono(m.etiqueta, m.icono, estado.modalidad == m) { alElegirModalidad(m) }
            }
        }

        RotuloGrupo("Horario", Modifier.padding(top = EspacioMargen, bottom = EspacioAjustado))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(EspacioCorto),
            verticalArrangement = Arrangement.spacedBy(EspacioCorto),
            modifier = Modifier.selectableGroup()
        ) {
            FiltroOpcion("Cualquiera", estado.franja == null, { alElegirFranja(null) }, conMarca = false)
            Franja.entries.forEach { f ->
                FiltroOpcion("${f.etiqueta} · ${f.rango}", estado.franja == f, { alElegirFranja(f) }, conMarca = false)
            }
        }

        Spacer(Modifier.height(EspacioMargen))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .superficie(FormaOrganica)
                .toggleable(value = estado.soloEnLinea, role = Role.Switch, onValueChange = alCambiarEnLinea)
                .heightIn(min = 60.dp)
                .padding(horizontal = EspacioNormal)
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
                    checkedThumbColor = SobreOscuro,
                    checkedIconColor = BosqueMedio,
                    uncheckedTrackColor = TinteBosque,
                    uncheckedBorderColor = BordeRadio,
                    uncheckedThumbColor = Salvia
                )
            )
        }

        Spacer(Modifier.height(EspacioAmplio))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BotonTexto("Limpiar", alLimpiar, color = TextoSecundario)
            Spacer(Modifier.width(EspacioCorto))
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .areaTactilSinCambiarMedida()
            .heightIn(min = 48.dp)
            .pulsable(rol = Role.RadioButton, alClicar = alClicar)
            .semantics { selected = seleccionado }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 40.dp)
                .then(
                    if (seleccionado) Modifier.background(TinteBosque, FormaOpcion).border(1.dp, BordeSeleccion, FormaOpcion)
                    else Modifier.superficie(FormaOpcion)
                )
                .padding(horizontal = EspacioCercano)
        ) {
            Icon(icono, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(Icono17))
            Spacer(Modifier.width(EspacioFino))
            Text(texto, color = TextoPrimario, style = MaterialTheme.typography.titleSmall.merge(enfasisSeleccion(seleccionado)))
        }
    }
}

/**
 * Fila de tutor para listas agrupadas: avatar con estado, nombre, materias y una línea
 * de contexto (universidad · modalidad); a la derecha, calificación y tarifa.
 * No dibuja fondo propio: lo pone la lista (ver [filaAgrupada]).
 */
@Composable
fun FilaTutor(tutor: Tutor, alAbrir: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Ver perfil de ${tutor.nombre}", role = Role.Button, onClick = alAbrir)
            .semantics { stateDescription = if (tutor.enLinea) "En línea" else "Desconectado" }
            .padding(horizontal = EspacioNormal, vertical = EspacioCercano)
    ) {
        Avatar(tutor.nombre, tamano = TamanoAvatarLista, enLinea = tutor.enLinea, anunciarEstado = false)
        Spacer(Modifier.width(EspacioNormal))
        Column(Modifier.weight(1f)) {
            TextoLinea(tutor.nombre, MaterialTheme.typography.titleMedium, TextoPrimario)
            TextoLinea(tutor.materias.joinToString(" · "), MaterialTheme.typography.bodyMedium, TextoSecundario)
            Text(
                listOf(universidadCorta(tutor.universidad), tutor.modalidades.descripcion()).joinToString(" · "),
                color = TextoTercero,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(EspacioMedio))
        Column(horizontalAlignment = Alignment.End) {
            Valoracion(tutor.rating)
            Spacer(Modifier.height(EspacioMinimo))
            Text(
                formatoPrecio(tutor.precioHora),
                color = TextoPrimario,
                style = MaterialTheme.typography.titleSmall.merge(CifrasTabulares)
            )
        }
    }
}
