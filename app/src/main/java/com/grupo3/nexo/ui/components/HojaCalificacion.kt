package com.grupo3.nexo.ui.components

import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioAmplio
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioFino
import com.grupo3.nexo.ui.theme.EspacioHolgado
import com.grupo3.nexo.ui.theme.EspacioMargen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.ui.theme.Bosque
import com.grupo3.nexo.ui.theme.LocalAnimacionesReducidas
import com.grupo3.nexo.ui.theme.Miel
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.VerdeEstado
import com.grupo3.nexo.ui.theme.cambioContenido
import com.grupo3.nexo.ui.theme.resorteRebote

private val etiquetasNota = listOf("Toca una estrella", "Mala", "Regular", "Buena", "Muy buena", "Excelente")

@Composable
fun HojaCalificacion(sesion: Sesion, esTutor: Boolean, alCerrar: () -> Unit, alEnviar: (Int, String?) -> Unit) {
    val reducidas = LocalAnimacionesReducidas.current
    var estrellas by rememberSaveable { mutableIntStateOf(0) }
    var comentario by rememberSaveable { mutableStateOf("") }

    HojaNexo(
        titulo = "¿Cómo te fue con ${sesion.conQuien.substringBefore(' ')}?",
        subtitulo = "${sesion.materia} · ${formatoFecha(sesion.fecha)}",
        alCerrar = alCerrar
    ) {
        Spacer(Modifier.height(EspacioAmplio))
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
                    if (reducidas) snap() else resorteRebote(),
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
            transitionSpec = { if (reducidas) EnterTransition.None togetherWith ExitTransition.None else cambioContenido() },
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
                    .padding(top = EspacioCorto)
            )
        }
        RotuloGrupo("Reseña (opcional)", Modifier.padding(top = EspacioHolgado, bottom = EspacioAjustado))
        Campo(
            valor = comentario,
            alCambiar = { comentario = it },
            marcador = if (esTutor) "¿Cómo participó en la sesión?" else "¿Qué le dirías a otro estudiante?",
            lineasMaximas = 4,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        Text(
            "Solo puedes calificar una vez cada sesión.",
            color = TextoTercero,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = EspacioMargen, top = EspacioFino)
        )
        Spacer(Modifier.height(EspacioMargen))
        BotonPrimario(
            texto = "Enviar calificación",
            habilitado = estrellas > 0,
            alClicar = { alEnviar(estrellas, comentario.ifBlank { null }) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
