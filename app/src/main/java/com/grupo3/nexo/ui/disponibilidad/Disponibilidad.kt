package com.grupo3.nexo.ui.disponibilidad

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo3.nexo.domain.model.HoraDisponible
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.ui.components.CabeceraSeccion
import com.grupo3.nexo.ui.components.Carga
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.ErrorDeLista
import com.grupo3.nexo.ui.components.Esqueleto
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.HojaNexo
import com.grupo3.nexo.ui.components.IconosNexo
import com.grupo3.nexo.ui.components.LineaHueso
import com.grupo3.nexo.ui.components.RadioOpcion
import com.grupo3.nexo.ui.components.SangriaDivisorRadio
import com.grupo3.nexo.ui.components.areaTactilSinCambiarMedida
import com.grupo3.nexo.ui.components.cargaDe
import com.grupo3.nexo.ui.components.descripcion
import com.grupo3.nexo.ui.components.formatoHoras
import com.grupo3.nexo.ui.components.icono
import com.grupo3.nexo.ui.components.pulsable
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.theme.BordeAcento
import com.grupo3.nexo.ui.theme.BordeMarcado
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioExtenso
import com.grupo3.nexo.ui.theme.EspacioFino
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioMinimo
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.FormaOpcion
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.theme.Icono15
import com.grupo3.nexo.ui.theme.Icono18
import com.grupo3.nexo.ui.theme.Menta
import com.grupo3.nexo.ui.theme.SobreOscuro
import com.grupo3.nexo.ui.theme.Superficie
import com.grupo3.nexo.ui.theme.TextoHoraInactiva
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoSobreTinte
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.TinteBosque
import com.grupo3.nexo.ui.theme.VerdeEstado
import com.grupo3.nexo.ui.theme.especColor


/* ==================== Pantalla ==================== */

/** Hora que se está editando (día + hora), guardable en el estado de la pantalla. */
private data class HoraEnEdicion(val dia: String, val hora: String) : java.io.Serializable

/** Las cuatro opciones de una hora, en el orden en que se muestran. */
private val opcionesHora: List<Pair<String, Set<Modalidad>>> = listOf(
    "No disponible" to emptySet(),
    "Virtual" to setOf(Modalidad.VIRTUAL),
    "Presencial" to setOf(Modalidad.PRESENCIAL),
    "Virtual y presencial" to setOf(Modalidad.VIRTUAL, Modalidad.PRESENCIAL)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaDisponibilidad(
    alVolver: () -> Unit,
    viewModel: DisponibilidadViewModel = hiltViewModel()
) {
    val diasCargados by viewModel.dias.collectAsStateWithLifecycle()
    val dias = diasCargados.orEmpty()
    val carga = cargaDe(cargando = diasCargados == null, vacia = dias.isEmpty())
    var enEdicion by rememberSaveable { mutableStateOf<HoraEnEdicion?>(null) }

    val resumen by viewModel.resumen.collectAsStateWithLifecycle()
    val (total, virtuales, presenciales, diasActivos) = resumen

    Column(Modifier.fillMaxSize()) {
        CabeceraSeccion("Mi disponibilidad", alVolver = alVolver)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(EspacioMedio),
            contentPadding = PaddingValues(start = EspacioMargen, end = EspacioMargen, top = EspacioNormal, bottom = EspacioExtenso + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            if (carga == Carga.CARGANDO) item(key = "cargando") { EsqueletoDias() }
            else if (carga == Carga.VACIO) item(key = "vacio") {
                EstadoVacio(
                    icono = IconosNexo.Calendario,
                    titulo = "Aún no hay horas para publicar",
                    mensaje = "Crea tu semana y elige cuáles horas ofreces, en virtual, presencial o ambas.",
                    accion = "Agregar horario",
                    alAccion = viewModel::empezar
                )
            }
            else if (carga != Carga.LISTO) item(key = "error") { ErrorDeLista(titulo = "No pudimos cargar tu disponibilidad") }
            else item(key = "resumen") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TinteBosque, FormaOrganica)
                        .border(1.dp, BordeAcento, FormaOrganica)
                        .padding(EspacioNormal)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$total", color = BosqueProfundo, style = MaterialTheme.typography.displaySmall.merge(CifrasTabulares))
                        Spacer(Modifier.width(EspacioCorto))
                        Text(
                            if (total == 1) "hora publicada" else "horas publicadas",
                            color = TextoSobreTinte,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = EspacioMinimo)
                        )
                    }
                    Text(
                        "en $diasActivos ${if (diasActivos == 1) "día" else "días"} · $virtuales ${if (virtuales == 1) "virtual" else "virtuales"} · $presenciales ${if (presenciales == 1) "presencial" else "presenciales"}",
                        color = TextoSobreTinte,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(EspacioMedio))
                    Text(
                        "Toca una hora para elegir si la ofreces virtual, presencial o ambas. Los estudiantes solo pueden solicitarte tutorías en tus horas publicadas.",
                        color = TextoSobreTinte,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (carga == Carga.LISTO) items(dias, key = { it.dia }) { dia ->
                val activas = dia.horasActivas
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .superficie(FormaOrganica)
                        .padding(EspacioNormal)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(dia.dia, color = TextoPrimario, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(
                            when (activas) {
                                0 -> "Sin horas"
                                else -> formatoHoras(activas.toDouble())
                            },
                            color = if (activas > 0) VerdeEstado else TextoTercero,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(Modifier.height(EspacioMedio))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(EspacioCorto),
                        verticalArrangement = Arrangement.spacedBy(EspacioCorto)
                    ) {
                        dia.horas.forEach { hora ->
                            ChipHora(hora, alClicar = { enEdicion = HoraEnEdicion(dia.dia, hora.hora) })
                        }
                    }
                }
            }
        }
    }

    enEdicion?.let { edicion ->
        val actual = dias.firstOrNull { it.dia == edicion.dia }?.horas?.firstOrNull { it.hora == edicion.hora }
        HojaNexo(
            titulo = "${edicion.dia} · ${edicion.hora}",
            subtitulo = "¿Cómo ofreces esta hora?",
            alCerrar = { enEdicion = null }
        ) {
            Spacer(Modifier.height(EspacioNormal))
            Column(
                Modifier
                    .fillMaxWidth()
                    .superficie(FormaOrganica)
                    .selectableGroup()
            ) {
                opcionesHora.forEachIndexed { i, (texto, modalidades) ->
                    if (i > 0) Box(Modifier.fillMaxWidth().padding(start = SangriaDivisorRadio).height(1.dp).background(Divisor))
                    OpcionHora(
                        texto = texto,
                        modalidades = modalidades,
                        seleccionada = (actual?.modalidades ?: emptySet<Modalidad>()) == modalidades,
                        alClicar = {
                            viewModel.definir(edicion.dia, edicion.hora, modalidades)
                            enEdicion = null
                        }
                    )
                }
            }
        }
    }
}

/** Silueta del resumen y los días mientras llegan los datos; la lista ya trae su margen lateral. */
@Composable
private fun EsqueletoDias() {
    Esqueleto(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(EspacioMedio)) {
        repeat(3) {
            Column(
                verticalArrangement = Arrangement.spacedBy(EspacioCorto),
                modifier = Modifier
                    .fillMaxWidth()
                    .superficie(FormaOrganica)
                    .padding(EspacioNormal)
            ) {
                LineaHueso(0.4f, alto = 14.dp)
                LineaHueso(0.85f, alto = 10.dp)
                LineaHueso(0.6f, alto = 10.dp)
            }
        }
    }
}

/** Hora de la semana: apagada (contorno) o publicada (bosque plano con íconos de modalidad). */
@Composable
private fun ChipHora(hora: HoraDisponible, alClicar: () -> Unit) {
    val activa = hora.seleccionada
    val colorTexto by animateColorAsState(if (activa) SobreOscuro else TextoHoraInactiva, especColor(), label = "textoHora")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .areaTactilSinCambiarMedida()
            .heightIn(min = 48.dp)
            // La hora y su estado los anuncia solo la descripción de abajo; la acción dice únicamente "Editar"
            .pulsable(rol = Role.Button, etiqueta = "Editar", alClicar = alClicar)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 40.dp)
                .then(
                    if (activa) Modifier.background(BosqueMedio, FormaOpcion)
                    else Modifier
                        .background(Superficie, FormaOpcion)
                        .border(1.dp, BordeMarcado, FormaOpcion)
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = "${hora.hora}, ${if (activa) hora.modalidades.descripcion() else "no disponible"}"
                }
                .padding(start = EspacioCercano, end = if (activa) EspacioAjustado else EspacioCercano)
        ) {
            Text(hora.hora, color = colorTexto, style = MaterialTheme.typography.titleSmall.merge(CifrasTabulares))
            if (activa) {
                Spacer(Modifier.width(EspacioFino))
                Modalidad.entries.filter { it in hora.modalidades }.forEach {
                    Icon(it.icono, contentDescription = null, tint = Menta, modifier = Modifier.size(Icono15))
                }
            }
        }
    }
}

@Composable
private fun OpcionHora(texto: String, modalidades: Set<Modalidad>, seleccionada: Boolean, alClicar: () -> Unit) {
    RadioOpcion(texto, seleccionada, alClicar) {
        Modalidad.entries.filter { it in modalidades }.forEach {
            Icon(it.icono, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(Icono18))
            Spacer(Modifier.width(EspacioMinimo))
        }
        if (modalidades.isEmpty()) {
            Text("Oculta en búsquedas", color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
        }
    }
}
