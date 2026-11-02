package com.grupo3.nexo.ui.historial

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.ui.components.Avatar
import com.grupo3.nexo.ui.components.AvisoNexo
import com.grupo3.nexo.ui.components.BotonSecundario
import com.grupo3.nexo.ui.components.CabeceraSeccion
import com.grupo3.nexo.ui.components.Carga
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.EncabezadoSeccion
import com.grupo3.nexo.ui.components.ErrorDeLista
import com.grupo3.nexo.ui.components.EsqueletoLista
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.Estrellas
import com.grupo3.nexo.ui.components.FiltroOpcion
import com.grupo3.nexo.ui.components.HojaCalificacion
import com.grupo3.nexo.ui.components.IconosNexo
import com.grupo3.nexo.ui.components.ListaResenas
import com.grupo3.nexo.ui.components.RotuloGrupo
import com.grupo3.nexo.ui.components.TextoLinea
import com.grupo3.nexo.ui.components.cargaDe
import com.grupo3.nexo.ui.components.formatoFecha
import com.grupo3.nexo.ui.components.formatoHoras
import com.grupo3.nexo.ui.components.formatoValoracion
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioAmplio
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioExtenso
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioMinimo
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.theme.IconoDiminuto
import com.grupo3.nexo.ui.theme.MielFuerte
import com.grupo3.nexo.ui.theme.MielOscura
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.cambioContenido
import kotlinx.coroutines.launch


/* ==================== Pantalla ==================== */

@Composable
fun PantallaHistorial(
    alVolver: () -> Unit,
    alBuscar: () -> Unit,
    alAbrirDisponibilidad: (() -> Unit)? = null,
    viewModel: HistorialViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var porCalificarId by rememberSaveable { mutableStateOf<String?>(null) }
    val porCalificar = estado.todas.firstOrNull { it.id == porCalificarId }
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CabeceraSeccion(if (estado.esTutor) "Tutorías dadas" else "Tus tutorías", alVolver = alVolver)

            val carga = cargaDe(estado.cargando, estado.todas.isEmpty())
            AnimatedContent(
                targetState = carga,
                transitionSpec = { cambioContenido() },
                label = "historial",
                modifier = Modifier.weight(1f)
            ) { fase ->
                when (fase) {
                    Carga.CARGANDO -> EsqueletoLista()
                    Carga.ERROR -> ErrorDeLista(titulo = "No pudimos cargar tu historial")
                    Carga.VACIO -> EstadoVacio(
                        icono = IconosNexo.Historial,
                        titulo = "Aún no hay sesiones",
                        mensaje = if (estado.esTutor) "Las sesiones que des aparecerán aquí para que califiques a tus estudiantes."
                                  else "Tus sesiones completadas aparecerán aquí para que puedas calificarlas.",
                        accion = if (estado.esTutor) alAbrirDisponibilidad?.let { "Revisar mi disponibilidad" } else "Buscar tutor",
                        alAccion = if (estado.esTutor) alAbrirDisponibilidad else alBuscar
                    )
                    Carga.LISTO -> {
                    val margenSistema = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(EspacioMedio),
                        contentPadding = PaddingValues(start = EspacioMargen, end = EspacioMargen, top = EspacioNormal, bottom = EspacioExtenso + margenSistema)
                    ) {
                        item(key = "resumen") { Resumen(estado) }
                        item(key = "rotulo") {
                            RotuloGrupo("Más recientes primero", Modifier.padding(start = EspacioMinimo, top = EspacioMedio))
                        }
                        if (estado.materias.size > 1) {
                            item(key = "filtros") {
                                FiltrosMateria(estado.materias, estado.materia, viewModel::filtrar)
                            }
                        }
                        items(estado.sesiones, key = { it.id }) { sesion ->
                            TarjetaHistorial(sesion, estado.esTutor, alCalificar = { porCalificarId = sesion.id }, modifier = Modifier.animateItem())
                        }
                        if (estado.esTutor) {
                            item(key = "resenas") {
                                Column(Modifier.padding(top = EspacioCercano)) {
                                    EncabezadoSeccion("Lo que dicen de ti")
                                    if (estado.resenas.isEmpty()) {
                                        Text(
                                            "Aún no tienes reseñas. Aparecerán aquí cuando tus estudiantes te califiquen.",
                                            color = TextoTercero,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = EspacioMinimo)
                                        )
                                    } else {
                                        ListaResenas(estado.resenas)
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
        AvisoNexo(avisos, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = EspacioAmplio))
    }

    porCalificar?.let { sesion ->
        HojaCalificacion(
            sesion = sesion,
            esTutor = estado.esTutor,
            alCerrar = { porCalificarId = null },
            alEnviar = { estrellas, comentario ->
                viewModel.calificar(sesion.id, estrellas, comentario)
                porCalificarId = null
                alcance.launch { avisos.showSnackbar(if (estado.esTutor) "Gracias, tu calificación quedó guardada" else "Gracias, tu calificación ayuda a otros estudiantes") }
            }
        )
    }
}

/* ==================== Componentes privados ==================== */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltrosMateria(materias: List<String>, actual: String?, alElegir: (String?) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(EspacioCorto),
        verticalArrangement = Arrangement.spacedBy(EspacioCorto),
        modifier = Modifier.selectableGroup().padding(vertical = EspacioMinimo)
    ) {
        FiltroOpcion("Todas", actual == null, { alElegir(null) }, conMarca = false)
        materias.forEach { m -> FiltroOpcion(m, actual == m, { alElegir(m) }, conMarca = false) }
    }
}

@Composable
private fun Resumen(estado: HistorialViewModel.Estado) {
    val resumen = estado.resumen ?: return
    val unaSesion = resumen.sesiones == 1
    val etiquetaSesiones = when {
        estado.esTutor -> if (unaSesion) "sesión dada" else "sesiones dadas"
        else -> if (unaSesion) "sesión" else "sesiones"
    }
    val etiquetaHoras = if (estado.esTutor) "dadas" else "de estudio"
    // Con letra muy grande las tres cifras no caben en fila: se apilan, valor y etiqueta en línea
    val apilar = LocalDensity.current.fontScale > 1.4f
    if (apilar) {
        Column(Modifier.fillMaxWidth()) {
            CifraResumen("${resumen.sesiones}", etiquetaSesiones, enLinea = true)
            CifraResumen(formatoHoras(resumen.horas), etiquetaHoras, enLinea = true)
            if (estado.esTutor) {
                CifraResumen(resumen.promedioRecibido?.let(::formatoValoracion) ?: "–", "calificación", enLinea = true)
            } else {
                CifraResumen("${resumen.porCalificar}", "por calificar", enLinea = true, resaltar = resumen.porCalificar > 0)
            }
        }
        return
    }
    // Sin contenedor, como las cifras del Perfil: solo líneas finas entre ellas
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .height(IntrinsicSize.Min)
    ) {
        CifraResumen("${resumen.sesiones}", etiquetaSesiones, Modifier.weight(1f))
        Box(Modifier.fillMaxHeight().padding(vertical = EspacioAjustado).width(1.dp).background(Divisor))
        CifraResumen(formatoHoras(resumen.horas), etiquetaHoras, Modifier.weight(1f))
        Box(Modifier.fillMaxHeight().padding(vertical = EspacioAjustado).width(1.dp).background(Divisor))
        if (estado.esTutor) {
            CifraResumen(resumen.promedioRecibido?.let(::formatoValoracion) ?: "–", "calificación", Modifier.weight(1f))
        } else {
            CifraResumen("${resumen.porCalificar}", "por calificar", Modifier.weight(1f), resaltar = resumen.porCalificar > 0)
        }
    }
}

@Composable
private fun CifraResumen(
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier,
    resaltar: Boolean = false,
    enLinea: Boolean = false
) {
    val textoValor: @Composable () -> Unit = {
        Text(
            valor,
            color = if (resaltar) MielOscura else BosqueProfundo,
            style = MaterialTheme.typography.headlineSmall.merge(CifrasTabulares)
        )
    }
    if (enLinea) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics(mergeDescendants = true) { }
        ) {
            textoValor()
            Spacer(Modifier.width(EspacioCorto))
            Text(etiqueta, color = TextoTercero, style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .semantics(mergeDescendants = true) { }
    ) {
        textoValor()
        Text(etiqueta, color = TextoTercero, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TarjetaHistorial(sesion: Sesion, esTutor: Boolean, alCalificar: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .superficie(FormaOrganica)
            .padding(EspacioNormal)
    ) {
        // Solo el avatar se atenúa en sesiones sin completar: el texto conserva su contraste
        Avatar(
            sesion.conQuien,
            tamano = 40.dp,
            modifier = Modifier.graphicsLayer { alpha = if (sesion.completada) 1f else 0.6f }
        )
        Spacer(Modifier.width(EspacioMedio))
        Column(Modifier.weight(1f)) {
            TextoLinea(sesion.materia, MaterialTheme.typography.titleMedium, TextoPrimario)
            TextoLinea(sesion.conQuien, MaterialTheme.typography.bodyMedium, TextoSecundario)
            Text(
                "${formatoFecha(sesion.fecha)} · ${sesion.hora} · ${formatoHoras(sesion.duracionHoras)} · ${sesion.modalidad.etiqueta}",
                color = TextoTercero,
                style = MaterialTheme.typography.bodySmall.merge(CifrasTabulares)
            )
            sesion.calificacionRecibida?.takeIf { sesion.completada }?.let { nota ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = EspacioMinimo)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Te calificó con $nota ${if (nota == 1) "estrella" else "estrellas"}"
                        }
                ) {
                    Text("Te calificó", color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(EspacioMinimo))
                    Icon(IconosNexo.Estrella, contentDescription = null, tint = MielFuerte, modifier = Modifier.size(IconoDiminuto))
                    Text("$nota", color = TextoSecundario, style = MaterialTheme.typography.bodySmall.merge(CifrasTabulares))
                }
            }
            sesion.comentario?.let {
                Text(
                    "“$it”",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    modifier = Modifier.padding(top = EspacioMinimo)
                )
            }
        }
        Spacer(Modifier.width(EspacioCorto))
        if (!sesion.completada) {
            // Cancelada o sin asistencia: no se califica; el estado se lee como texto discreto
            Text(sesion.estado.etiqueta, color = TextoTercero, style = MaterialTheme.typography.labelMedium)
            return@Row
        }
        AnimatedContent(
            targetState = sesion.calificacion,
            transitionSpec = { cambioContenido() },
            label = "calificacion"
        ) { nota ->
            if (nota != null) {
                Estrellas(nota, tamano = 13.dp)
            } else {
                BotonSecundario(
                    "Calificar", alCalificar,
                    icono = IconosNexo.Estrella, compacto = true, colorIcono = MielFuerte, alto = 48.dp,
                    descripcion = "Calificar a ${sesion.conQuien}, ${sesion.materia}"
                )
            }
        }
    }
}
