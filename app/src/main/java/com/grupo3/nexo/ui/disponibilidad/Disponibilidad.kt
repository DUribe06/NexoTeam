package com.grupo3.nexo.ui.disponibilidad

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.ui.components.AvisoNexo
import com.grupo3.nexo.ui.components.CabeceraSeccion
import com.grupo3.nexo.ui.components.Carga
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.ErrorDeLista
import com.grupo3.nexo.ui.components.Esqueleto
import com.grupo3.nexo.ui.components.Hueso
import com.grupo3.nexo.ui.components.LineaHueso
import com.grupo3.nexo.ui.components.cargaDe
import com.grupo3.nexo.ui.components.descripcion
import com.grupo3.nexo.ui.components.formatoHora
import com.grupo3.nexo.ui.components.icono
import com.grupo3.nexo.ui.components.plural
import com.grupo3.nexo.ui.components.pulsable
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.theme.AltoEsqueletoCuadricula
import com.grupo3.nexo.ui.theme.AltoSelectorPincel
import com.grupo3.nexo.ui.theme.AnchoMinimoEtiquetaHora
import com.grupo3.nexo.ui.theme.AreaTactilMinima
import com.grupo3.nexo.ui.theme.BordeAcento
import com.grupo3.nexo.ui.theme.BordeMarcado
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.ElevacionTarjeta
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioExtenso
import com.grupo3.nexo.ui.theme.EspacioFino
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.FormaBoton
import com.grupo3.nexo.ui.theme.FormaCampoAlto
import com.grupo3.nexo.ui.theme.FormaOpcion
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.theme.Icono15
import com.grupo3.nexo.ui.theme.Icono18
import com.grupo3.nexo.ui.theme.Menta
import com.grupo3.nexo.ui.theme.SobreOscuro
import com.grupo3.nexo.ui.theme.SombraAmbiente
import com.grupo3.nexo.ui.theme.SombraPuntual
import com.grupo3.nexo.ui.theme.Superficie
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoSobreTinte
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.TinteBosque
import com.grupo3.nexo.ui.theme.especColor
import com.grupo3.nexo.ui.theme.pincelBosque
import com.grupo3.nexo.ui.theme.resorteIndicador
import kotlin.math.abs

/* ==================== Pantalla ==================== */

/** Con qué se marca: se elige una vez y sirve para todas las horas que se toquen después. */
private val pinceles: List<Pair<String, Set<Modalidad>>> = listOf(
    "Virtual" to setOf(Modalidad.VIRTUAL),
    "Presencial" to setOf(Modalidad.PRESENCIAL),
    "Ambas" to setOf(Modalidad.VIRTUAL, Modalidad.PRESENCIAL)
)

/** Lo que ocupa la pantalla aparte de las siete filas de horas. */
private const val ALTO_SIN_CUADRICULA_DP = 472
private val SeparacionCeldas = 3.dp

/** Arranca con la modalidad que el tutor más usa: quien solo da clases virtuales no tiene que elegirla cada vez. */
private fun pincelInicial(dias: List<DiaDisponibilidad>): Int {
    val usadas = dias.flatMap { it.horas }.map { it.modalidades }.filter { it.isNotEmpty() }
    val masUsada = usadas.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    return pinceles.indexOfFirst { it.second == masUsada }.coerceAtLeast(0)
}

@Composable
fun PantallaDisponibilidad(
    alVolver: () -> Unit,
    viewModel: DisponibilidadViewModel = hiltViewModel()
) {
    val diasCargados by viewModel.dias.collectAsStateWithLifecycle()
    val dias = diasCargados.orEmpty()
    val fallo by viewModel.error.collectAsStateWithLifecycle()
    val carga = cargaDe(cargando = diasCargados == null, vacia = dias.isEmpty(), error = fallo)
    val resumen by viewModel.resumen.collectAsStateWithLifecycle()
    val guardado by viewModel.guardado.collectAsStateWithLifecycle()
    val puedeDeshacer by viewModel.puedeDeshacer.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    // Sin calendario se crea solo: el tutor llega directo a la cuadrícula, sin un paso previo que no decide nada
    var noSePudoCrear by rememberSaveable { mutableStateOf(false) }
    fun crear() {
        noSePudoCrear = false
        viewModel.empezar { error -> if (error != null) noSePudoCrear = true }
    }
    LaunchedEffect(carga) { if (carga == Carga.VACIO) crear() }

    // Se decide una sola vez: marcar o limpiar la semana no le cambia el pincel al tutor mientras trabaja
    var pincelElegido by rememberSaveable { mutableStateOf<Int?>(null) }
    val indicePincel = pincelElegido ?: pincelInicial(dias)
    LaunchedEffect(diasCargados != null) {
        if (pincelElegido == null && diasCargados != null) pincelElegido = pincelInicial(dias)
    }
    val pincel = pinceles[indicePincel].second

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CabeceraSeccion("Mi disponibilidad", alVolver = alVolver)
            Column(
                verticalArrangement = Arrangement.spacedBy(EspacioMedio),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = EspacioMargen, end = EspacioMargen, top = EspacioNormal, bottom = EspacioExtenso)
                    .navigationBarsPadding()
            ) {
                when {
                    carga == Carga.CARGANDO || (carga == Carga.VACIO && !noSePudoCrear) -> EsqueletoSemana()
                    carga == Carga.VACIO -> ErrorDeLista(
                        titulo = "No pudimos preparar tu calendario",
                        alReintentar = ::crear
                    )
                    carga != Carga.LISTO -> ErrorDeLista(
                        titulo = "No pudimos cargar tu disponibilidad",
                        alReintentar = viewModel::reintentar
                    )
                    else -> {
                        Resumen(resumen)
                        SelectorPincel(indicePincel, alElegir = { pincelElegido = it })
                        Text(
                            "Toca una hora o arrastra sobre varias. Para marcar un día o una franja completa, toca su título.",
                            color = TextoSecundario,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Cuadricula(
                            dias = dias,
                            pincel = pincel,
                            alTrazar = { trazo -> viewModel.editar { it.conTrazo(trazo) } },
                            alTocarDia = { d -> viewModel.editar { it.alternarDia(d, pincel) } },
                            alTocarHora = { h -> viewModel.editar { it.alternarHora(h, pincel) } }
                        )
                        Acciones(
                            guardado = guardado,
                            puedeDeshacer = puedeDeshacer,
                            hayHoras = resumen.total > 0,
                            alDeshacer = viewModel::deshacer,
                            alLimpiar = { viewModel.editar { it.sinHoras() } },
                            alReintentar = viewModel::reintentarGuardar
                        )
                    }
                }
            }
        }
        AvisoNexo(avisos, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = EspacioMedio))
    }
}

/* ==================== Resumen y pincel ==================== */

@Composable
private fun Resumen(resumen: ResumenDisponibilidad) {
    val (total, virtuales, presenciales, diasActivos) = resumen
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(TinteBosque, FormaOrganica)
            .border(1.dp, BordeAcento, FormaOrganica)
            .padding(horizontal = EspacioNormal, vertical = EspacioAjustado)
    ) {
        Text("$total", color = BosqueProfundo, style = MaterialTheme.typography.headlineMedium.merge(CifrasTabulares))
        Spacer(Modifier.width(EspacioMedio))
        Column {
            Text(
                plural(total, "hora publicada", "horas publicadas"),
                color = TextoSobreTinte,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                if (total == 0) "Los estudiantes aún no pueden solicitarte una hora"
                else "en $diasActivos ${plural(diasActivos, "día", "días")} · " +
                    "$virtuales ${plural(virtuales, "virtual", "virtuales")} · " +
                    "$presenciales ${plural(presenciales, "presencial", "presenciales")}",
                color = TextoSobreTinte,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SelectorPincel(indice: Int, alElegir: (Int) -> Unit) {
    val forma = FormaCampoAlto
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(AltoSelectorPincel)
            .shadow(ElevacionTarjeta, forma, ambientColor = SombraAmbiente, spotColor = SombraPuntual)
            .superficie(forma)
            .padding(EspacioFino)
    ) {
        val anchoOpcion = maxWidth / pinceles.size
        val desplazamiento by animateDpAsState(anchoOpcion * indice, resorteIndicador(), label = "indicadorPincel")
        Box(
            Modifier
                .offset(x = desplazamiento)
                .width(anchoOpcion)
                .fillMaxHeight()
                .background(pincelBosque, FormaBoton)
        )
        Row(Modifier.fillMaxSize().selectableGroup()) {
            pinceles.forEachIndexed { i, (texto, modalidades) ->
                val activo = i == indice
                val color by animateColorAsState(if (activo) SobreOscuro else TextoSecundario, especColor(), label = "textoPincel")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = activo,
                            role = Role.RadioButton,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { alElegir(i) }
                        )
                ) {
                    IconosModalidad(modalidades, color, Icono15)
                    Spacer(Modifier.width(EspacioFino))
                    Text(texto, color = color, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

/* ==================== Cuadrícula ==================== */

/**
 * Toda la semana en una pantalla, sin hojas de por medio. Toda la cuadrícula recibe el gesto y calcula la celda por
 * posición, así arrastrar no depende de cuál celda recibió el primer toque. Para quien no puede arrastrar, tocar celda
 * a celda, un día o una hora hace lo mismo, y cada celda se anuncia y se activa sola con un lector de pantalla.
 */
@Composable
private fun Cuadricula(
    dias: List<DiaDisponibilidad>,
    pincel: Set<Modalidad>,
    alTrazar: (Trazo) -> Unit,
    alTocarDia: (Int) -> Unit,
    alTocarHora: (Int) -> Unit
) {
    val horas = dias.firstOrNull()?.horas.orEmpty()
    if (horas.isEmpty()) return
    // Las celdas ceden un poco de alto para que en un teléfono pequeño la semana quepa sin desplazar
    // Con letra grande lo que rodea la cuadrícula crece, así que su parte fija se escala igual
    val configuracion = LocalConfiguration.current
    val reservado = ALTO_SIN_CUADRICULA_DP * configuracion.fontScale.coerceAtLeast(1f)
    val altoCelda = ((configuracion.screenHeightDp - reservado) / horas.size).dp.coerceIn(40.dp, 48.dp)
    var trazo by remember { mutableStateOf<Trazo?>(null) }
    val vibracion = LocalHapticFeedback.current
    val diasActuales by rememberUpdatedState(dias)
    val pincelActual by rememberUpdatedState(pincel)
    val nombrePincel = pinceles.firstOrNull { it.second == pincel }?.first.orEmpty().lowercase()
    val alTrazarActual by rememberUpdatedState(alTrazar)

    Column(
        Modifier
            .fillMaxWidth()
            .superficie(FormaOrganica)
            .padding(EspacioAjustado)
    ) {
        Row {
            // Las etiquetas ocupan lo que pida la letra más ancha; el espacio superior alinea con los títulos de día
            Column(
                verticalArrangement = Arrangement.spacedBy(SeparacionCeldas),
                modifier = Modifier.widthIn(min = AnchoMinimoEtiquetaHora).width(IntrinsicSize.Max)
            ) {
                Spacer(Modifier.height(AreaTactilMinima - SeparacionCeldas))
                horas.forEachIndexed { h, hora ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(altoCelda)
                            .pulsable(etiqueta = "marcar o quitar en toda la semana", alClicar = { alTocarHora(h) })
                    ) {
                        Text(
                            formatoHora(hora.hora),
                            color = TextoSecundario,
                            style = MaterialTheme.typography.labelMedium.merge(CifrasTabulares),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SeparacionCeldas)) {
                Row(horizontalArrangement = Arrangement.spacedBy(SeparacionCeldas)) {
                    dias.forEachIndexed { d, dia ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = AreaTactilMinima)
                                .pulsable(etiqueta = "marcar o quitar todo", alClicar = { alTocarDia(d) })
                                .semantics(mergeDescendants = true) { contentDescription = dia.dia }
                        ) {
                            Text(
                                dia.dia.take(3),
                                color = if (dia.horasActivas > 0) TextoPrimario else TextoSecundario,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(SeparacionCeldas),
                    modifier = Modifier
                        .pointerInput(dias.size, horas.size) {
                            fun celdaEn(p: Offset) = Celda(
                                dia = (p.x / size.width * dias.size).toInt().coerceIn(0, dias.size - 1),
                                hora = (p.y / size.height * horas.size).toInt().coerceIn(0, horas.size - 1)
                            )
                            awaitEachGesture {
                                // Nada se consume hasta saber qué quiere el dedo: un gesto vertical es para desplazar la pantalla
                                val toque = awaitFirstDown(requireUnconsumed = false)
                                val inicio = celdaEn(toque.position)
                                val actual = diasActuales.getOrNull(inicio.dia)?.horas?.getOrNull(inicio.hora)?.modalidades.orEmpty()
                                val destino = destinoDe(actual, pincelActual)
                                val arrastre = awaitTouchSlopOrCancellation(toque.id) { cambio, exceso ->
                                    if (abs(exceso.x) >= abs(exceso.y)) cambio.consume()
                                }
                                if (arrastre == null) {
                                    // Sin desplazamiento: si el dedo se levantó, fue un toque simple sobre una celda
                                    val ultimo = currentEvent.changes.firstOrNull { it.id == toque.id }
                                    if (ultimo != null && !ultimo.pressed && !ultimo.isConsumed) {
                                        vibracion.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        alTrazarActual(Trazo(inicio, inicio, destino))
                                    }
                                    return@awaitEachGesture
                                }
                                var enCurso = Trazo(inicio, celdaEn(arrastre.position), destino)
                                trazo = enCurso
                                vibracion.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                var cancelado = false
                                while (true) {
                                    val evento = awaitPointerEvent()
                                    val cambio = evento.changes.firstOrNull { it.id == toque.id }
                                    if (cambio == null) {
                                        cancelado = true
                                        break
                                    }
                                    if (!cambio.pressed) break
                                    cambio.consume()
                                    val fin = celdaEn(cambio.position)
                                    if (fin != enCurso.fin) {
                                        enCurso = enCurso.copy(fin = fin)
                                        trazo = enCurso
                                        vibracion.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                                trazo = null
                                if (!cancelado) alTrazarActual(enCurso)
                            }
                        }
                ) {
                        horas.indices.forEach { h ->
                            Row(horizontalArrangement = Arrangement.spacedBy(SeparacionCeldas)) {
                                dias.forEachIndexed { d, dia ->
                                    val celda = Celda(d, h)
                                    val guardadas = dia.horas.getOrNull(h)?.modalidades.orEmpty()
                                    val enTrazo = trazo?.takeIf { it.contiene(celda) }
                                    CeldaHora(
                                        nombre = "${dia.dia}, ${dia.horas.getOrNull(h)?.hora?.let(::formatoHora).orEmpty()}",
                                        modalidades = enTrazo?.valor ?: guardadas,
                                        accion = if (destinoDe(guardadas, pincel).isEmpty()) "quitar" else "marcar $nombrePincel",
                                    alActivar = { alTrazar(Trazo(celda, celda, destinoDe(guardadas, pincel))) },
                                        modifier = Modifier.weight(1f).height(altoCelda)
                                    )
                                }
                            }
                        }
                }
            }
        }
    }
}

/** Ni el color ni el ícono van solos: una hora marcada se distingue también sin percibir el verde. */
@Composable
private fun CeldaHora(
    nombre: String,
    modalidades: Set<Modalidad>,
    accion: String,
    alActivar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activa = modalidades.isNotEmpty()
    val fondo by animateColorAsState(if (activa) BosqueMedio else Superficie, especColor(), label = "fondoHora")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .background(fondo, FormaOpcion)
            .then(if (activa) Modifier else Modifier.border(1.dp, BordeMarcado, FormaOpcion))
            .semantics(mergeDescendants = true) {
                contentDescription = nombre
                stateDescription = if (activa) modalidades.descripcion() else "no disponible"
                role = Role.Button
                onClick(label = accion) {
                    alActivar()
                    true
                }
            }
    ) {
        if (activa) IconosModalidad(modalidades, Menta, if (modalidades.size > 1) Icono15 else Icono18)
    }
}

@Composable
private fun IconosModalidad(modalidades: Set<Modalidad>, color: androidx.compose.ui.graphics.Color, tamano: androidx.compose.ui.unit.Dp) {
    Modalidad.entries.filter { it in modalidades }.forEach {
        Icon(it.icono, contentDescription = null, tint = color, modifier = Modifier.size(tamano))
    }
}

/* ==================== Acciones ==================== */

@Composable
private fun Acciones(
    guardado: EstadoGuardado,
    puedeDeshacer: Boolean,
    hayHoras: Boolean,
    alDeshacer: () -> Unit,
    alLimpiar: () -> Unit,
    alReintentar: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }) {
            when (guardado) {
                EstadoGuardado.SIN_CAMBIOS -> Unit
                EstadoGuardado.GUARDANDO -> EstadoTexto("Guardando…", TextoSecundario)
                EstadoGuardado.GUARDADO -> EstadoTexto("Guardado", TextoSecundario)
                EstadoGuardado.FALLO -> AccionTexto("No se guardó · Reintentar", habilitada = true, alClicar = alReintentar)
            }
        }
        AccionTexto("Deshacer", habilitada = puedeDeshacer, alClicar = alDeshacer)
        Spacer(Modifier.width(EspacioMedio))
        AccionTexto("Limpiar", habilitada = hayHoras, alClicar = alLimpiar)
    }
}

@Composable
internal fun EstadoTexto(texto: String, color: androidx.compose.ui.graphics.Color) {
    Text(texto, color = color, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun AccionTexto(texto: String, habilitada: Boolean, alClicar: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = AreaTactilMinima)
            .pulsable(habilitado = habilitada, alClicar = alClicar)
            .padding(horizontal = EspacioCorto)
    ) {
        Text(
            texto,
            color = if (habilitada) BosqueAcento else TextoTercero,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** Silueta mientras llegan los datos: resumen, selector de pincel y cuadrícula, con la misma medida que lo real. */
@Composable
private fun EsqueletoSemana() {
    Esqueleto(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(EspacioMedio)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(EspacioCorto),
            modifier = Modifier
                .fillMaxWidth()
                .superficie(FormaOrganica)
                .padding(horizontal = EspacioNormal, vertical = EspacioAjustado)
        ) {
            LineaHueso(0.35f, alto = 18.dp)
            LineaHueso(0.7f, alto = 10.dp)
        }
        Hueso(Modifier.fillMaxWidth().height(AltoSelectorPincel), FormaCampoAlto)
        Hueso(Modifier.fillMaxWidth().height(AltoEsqueletoCuadricula), FormaOrganica)
    }
}
