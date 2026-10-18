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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.HoraDisponible
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.usecase.DefinirHoraUseCase
import com.grupo3.nexo.domain.usecase.ObtenerDisponibilidadUseCase
import com.grupo3.nexo.ui.components.BarraSuperior
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.HojaNexo
import com.grupo3.nexo.ui.components.descripcion
import com.grupo3.nexo.ui.components.icono
import com.grupo3.nexo.ui.components.pulsable
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.theme.Bosque
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.Menta
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.TinteBosque
import com.grupo3.nexo.ui.theme.VerdeEstado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ==================== ViewModel ==================== */

@HiltViewModel
class DisponibilidadViewModel @Inject constructor(
    obtenerDisponibilidad: ObtenerDisponibilidadUseCase,
    private val definirHora: DefinirHoraUseCase
) : ViewModel() {

    val dias = obtenerDisponibilidad()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<DiaDisponibilidad>())

    fun definir(dia: String, hora: String, modalidades: Set<Modalidad>) {
        viewModelScope.launch { definirHora(dia, hora, modalidades) }
    }
}

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
    val dias by viewModel.dias.collectAsState()
    var enEdicion by rememberSaveable { mutableStateOf<HoraEnEdicion?>(null) }

    val horas = dias.flatMap { it.horas }
    val total = horas.count { it.seleccionada }
    val virtuales = horas.count { Modalidad.VIRTUAL in it.modalidades }
    val presenciales = horas.count { Modalidad.PRESENCIAL in it.modalidades }
    val diasActivos = dias.count { d -> d.horas.any { it.seleccionada } }

    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Mi disponibilidad", alVolver)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
        ) {
            item(key = "resumen") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BosqueMedio, RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$total", color = Color.White, style = MaterialTheme.typography.displaySmall.merge(CifrasTabulares))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (total == 1) "hora publicada" else "horas publicadas",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        "en $diasActivos ${if (diasActivos == 1) "día" else "días"} · $virtuales virtuales · $presenciales presenciales",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Toca una hora para elegir si la ofreces virtual, presencial o ambas. Los estudiantes solo pueden solicitarte tutorías en tus horas publicadas.",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            items(dias, key = { it.dia }) { dia ->
                val activas = dia.horas.count { it.seleccionada }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .superficie(RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(dia.dia, color = TextoPrimario, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(
                            when (activas) {
                                0 -> "Sin horas"
                                1 -> "1 hora"
                                else -> "$activas horas"
                            },
                            color = if (activas > 0) VerdeEstado else TextoTercero,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .superficie(RoundedCornerShape(12.dp))
                    .selectableGroup()
            ) {
                opcionesHora.forEachIndexed { i, (texto, modalidades) ->
                    if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 56.dp).height(1.dp).background(Divisor))
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

/** Hora de la semana: apagada (contorno) o publicada (bosque plano con íconos de modalidad). */
@Composable
private fun ChipHora(hora: HoraDisponible, alClicar: () -> Unit) {
    val activa = hora.seleccionada
    val colorTexto by animateColorAsState(if (activa) Color.White else BosqueMedio.copy(alpha = 0.8f), label = "textoHora")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(40.dp)
            .pulsable(rol = Role.Button, etiqueta = "Editar ${hora.hora}", alClicar = alClicar)
            .then(
                if (activa) Modifier.background(BosqueMedio, RoundedCornerShape(8.dp))
                else Modifier
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Bosque.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "${hora.hora}, ${if (activa) hora.modalidades.descripcion() else "no disponible"}"
            }
            .padding(start = 14.dp, end = if (activa) 10.dp else 14.dp)
    ) {
        Text(hora.hora, color = colorTexto, style = MaterialTheme.typography.titleSmall.merge(CifrasTabulares))
        if (activa) {
            Spacer(Modifier.width(6.dp))
            Modalidad.entries.filter { it in hora.modalidades }.forEach {
                Icon(it.icono, contentDescription = null, tint = Menta, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun OpcionHora(texto: String, modalidades: Set<Modalidad>, seleccionada: Boolean, alClicar: () -> Unit) {
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
                .border(if (seleccionada) 2.dp else 1.5.dp, if (seleccionada) BosqueMedio else Bosque.copy(alpha = 0.3f), CircleShape)
        ) {
            if (seleccionada) Box(Modifier.size(9.dp).background(Menta, CircleShape))
        }
        Spacer(Modifier.width(16.dp))
        Text(texto, color = TextoPrimario, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Modalidad.entries.filter { it in modalidades }.forEach {
            Icon(it.icono, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
        }
        if (modalidades.isEmpty()) {
            Text("Oculta en búsquedas", color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
        }
    }
}
