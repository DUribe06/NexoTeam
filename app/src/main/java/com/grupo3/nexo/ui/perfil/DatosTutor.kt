package com.grupo3.nexo.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo3.nexo.domain.usecase.LARGO_MAXIMO_DESCRIPCION
import com.grupo3.nexo.domain.usecase.LARGO_MAXIMO_UNIVERSIDAD
import com.grupo3.nexo.ui.components.AvisoNexo
import com.grupo3.nexo.ui.components.BotonPrimario
import com.grupo3.nexo.ui.components.CabeceraSeccion
import com.grupo3.nexo.ui.components.Campo
import com.grupo3.nexo.ui.components.RotuloGrupo
import com.grupo3.nexo.ui.components.avisoFallo
import com.grupo3.nexo.ui.components.formatoPrecio
import com.grupo3.nexo.ui.components.mostrarError
import com.grupo3.nexo.ui.components.soloCifras
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioAmplio
import com.grupo3.nexo.ui.theme.EspacioExtenso
import com.grupo3.nexo.ui.theme.EspacioFino
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero

/** Lo que los estudiantes ven del tutor en su perfil: tarifa, universidad, carrera y descripción (RF4). */
@Composable
fun PantallaDatosTutor(alVolver: () -> Unit, viewModel: DatosTutorViewModel = hiltViewModel()) {
    val iniciales = viewModel.iniciales
    var tarifa by rememberSaveable { mutableStateOf(iniciales.tarifa) }
    var universidad by rememberSaveable { mutableStateOf(iniciales.universidad) }
    var carrera by rememberSaveable { mutableStateOf(iniciales.carrera) }
    var descripcion by rememberSaveable { mutableStateOf(iniciales.descripcion) }
    val guardando by viewModel.guardando.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val datos = DatosTutor(tarifa, universidad.trim(), carrera.trim(), descripcion.trim())
    val hayCambios = datos != iniciales.copy(
        universidad = iniciales.universidad.trim(),
        carrera = iniciales.carrera.trim(),
        descripcion = iniciales.descripcion.trim()
    )

    LaunchedEffect(viewModel) {
        viewModel.resultados.collect { r ->
            when (r) {
                ResultadoGuardar.Guardado -> alVolver()
                is ResultadoGuardar.Fallo -> avisos.mostrarError(avisoFallo("guardar tu perfil", r.causa))
            }
        }
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            CabeceraSeccion("Mi perfil de tutor", alVolver = alVolver)
            Column(
                verticalArrangement = Arrangement.spacedBy(EspacioAjustado),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = EspacioMargen, end = EspacioMargen, top = EspacioNormal, bottom = EspacioExtenso)
                    .navigationBarsPadding()
            ) {
                Text(
                    "Esto es lo que ven los estudiantes en tu perfil y en las búsquedas.",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium
                )

                RotuloGrupo("Tarifa por hora", Modifier.padding(top = EspacioMedio))
                Campo(
                    valor = tarifa,
                    alCambiar = { tarifa = soloCifras(it) },
                    marcador = "Ej.: 25000",
                    etiqueta = "Tarifa por hora en pesos",
                    limpiable = true,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )
                Ayuda(
                    tarifa.toDoubleOrNull()?.let { "Los estudiantes verán ${formatoPrecio(it)} la hora. Pueden proponerte otro precio al solicitar." }
                        ?: "Sin tarifa apareces con precio a convenir."
                )

                RotuloGrupo("Universidad", Modifier.padding(top = EspacioMedio))
                Campo(
                    valor = universidad,
                    alCambiar = { universidad = it.take(LARGO_MAXIMO_UNIVERSIDAD) },
                    marcador = "Ej.: Universidad Industrial de Santander",
                    etiqueta = "Universidad",
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                )

                RotuloGrupo("Carrera", Modifier.padding(top = EspacioMedio))
                Campo(
                    valor = carrera,
                    alCambiar = { carrera = it.take(LARGO_MAXIMO_UNIVERSIDAD) },
                    marcador = "Ej.: Ingeniería de Sistemas",
                    etiqueta = "Carrera",
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
                )

                RotuloGrupo("Sobre ti", Modifier.padding(top = EspacioMedio))
                Campo(
                    valor = descripcion,
                    alCambiar = { descripcion = it.take(LARGO_MAXIMO_DESCRIPCION) },
                    marcador = "Cómo explicas, qué semestre cursas, en qué te destacas",
                    etiqueta = "Descripción",
                    lineasMaximas = 6,
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Text(
                    "${descripcion.length}/$LARGO_MAXIMO_DESCRIPCION",
                    color = TextoTercero,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(EspacioAmplio))
                BotonPrimario(
                    texto = "Guardar cambios",
                    alClicar = { viewModel.guardar(datos) },
                    habilitado = hayCambios,
                    cargando = guardando,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        AvisoNexo(avisos, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = EspacioMedio))
    }
}

@Composable
private fun Ayuda(texto: String) {
    Text(
        texto,
        color = TextoSecundario,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = EspacioMargen, top = EspacioFino)
    )
}
