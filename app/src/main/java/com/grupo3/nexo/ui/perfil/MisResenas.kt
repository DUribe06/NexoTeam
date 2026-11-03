package com.grupo3.nexo.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo3.nexo.ui.components.CabeceraSeccion
import com.grupo3.nexo.ui.components.Carga
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.ErrorDeLista
import com.grupo3.nexo.ui.components.EsqueletoLista
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.IconosNexo
import com.grupo3.nexo.ui.components.TarjetaResena
import com.grupo3.nexo.ui.components.cargaDe
import com.grupo3.nexo.ui.components.formatoValoracion
import com.grupo3.nexo.ui.components.plural
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.EspacioExtenso
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.TextoSecundario

/** Todo lo que los estudiantes han dicho del tutor (RF9). */
@Composable
fun PantallaMisResenas(alVolver: () -> Unit, viewModel: MisResenasViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        CabeceraSeccion("Reseñas", alVolver = alVolver)
        when (cargaDe(estado.cargando, estado.resenas.isEmpty(), estado.error)) {
            Carga.CARGANDO -> EsqueletoLista()
            Carga.ERROR -> ErrorDeLista(titulo = "No pudimos cargar tus reseñas", alReintentar = viewModel::reintentar)
            Carga.VACIO -> EstadoVacio(
                icono = IconosNexo.Resena,
                titulo = "Aún sin reseñas",
                mensaje = "Aparecerán aquí cuando los estudiantes califiquen tus sesiones."
            )
            Carga.LISTO -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(EspacioMedio),
                contentPadding = PaddingValues(start = EspacioMargen, end = EspacioMargen, top = EspacioNormal, bottom = EspacioExtenso),
                modifier = Modifier.fillMaxSize().navigationBarsPadding()
            ) {
                item(key = "resumen") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        estado.resumen?.promedioRecibido?.let { promedio ->
                            Text(
                                formatoValoracion(promedio),
                                color = BosqueProfundo,
                                style = MaterialTheme.typography.headlineMedium.merge(CifrasTabulares)
                            )
                            Spacer(Modifier.width(EspacioMedio))
                        }
                        val total = estado.resumen?.totalResenas ?: estado.resenas.size
                        val cuantas = "$total ${plural(total, "reseña", "reseñas")}"
                        Text(
                            if (estado.resumen?.promedioRecibido == null) cuantas else "de 5 · $cuantas",
                            color = TextoSecundario,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                items(estado.resenas, key = { it.id }) { TarjetaResena(it) }
            }
        }
    }
}
