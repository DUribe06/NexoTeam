package com.grupo3.nexo.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.grupo3.nexo.ui.disponibilidad.EstadoGuardado
import com.grupo3.nexo.ui.disponibilidad.EstadoTexto
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo3.nexo.ui.components.AvisoNexo
import com.grupo3.nexo.ui.components.BotonSecundario
import com.grupo3.nexo.ui.components.CabeceraSeccion
import com.grupo3.nexo.ui.components.Campo
import com.grupo3.nexo.ui.components.EncabezadoSeccion
import com.grupo3.nexo.ui.components.IconosNexo
import com.grupo3.nexo.ui.components.avisoFallo
import com.grupo3.nexo.ui.components.mostrarError
import com.grupo3.nexo.ui.components.pulsable
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.theme.AreaTactilMinima
import com.grupo3.nexo.ui.theme.BordeMarcado
import com.grupo3.nexo.ui.theme.BordeSutil
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.ErrorBosque
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioExtenso
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.FormaControl
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.theme.Icono18
import com.grupo3.nexo.ui.theme.IconoSecundario
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario

/** Las materias por las que los estudiantes encuentran al tutor (RF4). */
@Composable
fun PantallaMisMaterias(alVolver: () -> Unit, viewModel: MisMateriasViewModel = hiltViewModel()) {
    val materias by viewModel.materias.collectAsStateWithLifecycle()
    var escrito by rememberSaveable { mutableStateOf("") }
    val avisos = remember { SnackbarHostState() }
    val guardado by viewModel.guardado.collectAsStateWithLifecycle()
    // Lo último que se quitó, con el lugar que ocupaba, para poder deshacerlo un rato
    var quitada by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var errorEscrito by rememberSaveable { mutableStateOf<String?>(null) }
    val sugerencias = remember(materias, escrito) { sugerenciasDeMaterias(materias, escrito) }

    LaunchedEffect(viewModel) {
        viewModel.fallos.collect { avisos.mostrarError(avisoFallo("guardar tus materias", it)) }
    }
    fun agregar(texto: String) {
        val aviso = viewModel.agregar(texto)
        errorEscrito = aviso
        if (aviso == null) escrito = ""
    }
    LaunchedEffect(quitada) {
        if (quitada != null) {
            delay(6_000)
            quitada = null
        }
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            CabeceraSeccion("Mis materias", alVolver = alVolver)
            Column(
                verticalArrangement = Arrangement.spacedBy(EspacioNormal),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = EspacioMargen, end = EspacioMargen, top = EspacioNormal, bottom = EspacioExtenso)
                    .navigationBarsPadding()
            ) {
                Text(
                    "Los estudiantes te encuentran por estas materias. Los cambios se guardan solos.",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(EspacioAjustado)) {
                    Campo(
                        valor = escrito,
                        alCambiar = {
                            escrito = it.take(MAXIMO_LETRAS_MATERIA)
                            errorEscrito = null
                        },
                        marcador = "Escribe una materia",
                        etiqueta = "Agregar materia",
                        limpiable = true,
                        teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        acciones = KeyboardActions(onDone = { agregar(escrito) }),
                        modifier = Modifier.weight(1f)
                    )
                    BotonSecundario("Agregar", { agregar(escrito) }, habilitado = escrito.isNotBlank())
                }
                errorEscrito?.let {
                    Text(
                        it,
                        color = ErrorBosque,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = EspacioNormal).semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                // Mientras escribe van pegadas al campo, para que el teclado no las tape
                if (escrito.isNotBlank()) Sugerencias(sugerencias, ::agregar)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EncabezadoSeccion("Enseñas", Modifier.weight(1f))
                    Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                        when (guardado) {
                            EstadoGuardado.GUARDANDO -> EstadoTexto("Guardando…", TextoSecundario)
                            EstadoGuardado.GUARDADO -> EstadoTexto("Guardado", TextoSecundario)
                            else -> Unit
                        }
                    }
                }
                if (materias.isEmpty()) {
                    Text(
                        "Aún no tienes materias. Agrega la primera para aparecer en las búsquedas por materia.",
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Column(Modifier.fillMaxWidth().superficie(FormaOrganica)) {
                        materias.forEachIndexed { i, materia ->
                            key(materia) {
                                if (i > 0) {
                                    Box(
                                        Modifier
                                            .padding(horizontal = EspacioNormal)
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(BordeSutil)
                                    )
                                }
                                FilaMateria(materia, puedeQuitarse = materias.size > 1, alQuitar = {
                                    quitada = materia to i
                                    viewModel.quitar(materia)
                                })
                            }
                        }
                    }
                    quitada?.let { (nombre, posicion) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
                        ) {
                            Text(
                                "Quitaste $nombre",
                                color = TextoSecundario,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .heightIn(min = AreaTactilMinima)
                                    .pulsable(etiqueta = "Deshacer: volver a agregar $nombre", alClicar = {
                                        viewModel.restaurar(nombre, posicion)
                                        quitada = null
                                    })
                                    .padding(horizontal = EspacioCorto)
                            ) {
                                Text("Deshacer", color = BosqueAcento, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    if (materias.size == 1) {
                        Text(
                            "Debes enseñar al menos una materia: agrega otra para poder quitar esta.",
                            color = TextoSecundario,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (escrito.isBlank() && sugerencias.isNotEmpty()) {
                    EncabezadoSeccion("Sugerencias")
                    Sugerencias(sugerencias, ::agregar)
                }
            }
        }
        AvisoNexo(avisos, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = EspacioMedio))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Sugerencias(sugerencias: List<String>, alElegir: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(EspacioCorto),
        verticalArrangement = Arrangement.spacedBy(EspacioCorto)
    ) {
        sugerencias.forEach { sugerida ->
            key(sugerida) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .heightIn(min = AreaTactilMinima)
                        .pulsable(etiqueta = "Agregar $sugerida", alClicar = { alElegir(sugerida) })
                ) {
                    Text(
                        sugerida,
                        color = BosqueProfundo,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .border(1.dp, BordeMarcado, FormaControl)
                            .padding(horizontal = EspacioAjustado, vertical = EspacioCorto)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaMateria(materia: String, puedeQuitarse: Boolean, alQuitar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = AreaTactilMinima).padding(start = EspacioNormal)
    ) {
        Text(
            materia,
            color = TextoPrimario,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(vertical = EspacioAjustado)
        )
        if (puedeQuitarse) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(AreaTactilMinima).pulsable(etiqueta = "Quitar $materia", alClicar = alQuitar)
            ) {
                Icon(IconosNexo.Cerrar, contentDescription = null, tint = IconoSecundario, modifier = Modifier.size(Icono18))
            }
        }
    }
}
