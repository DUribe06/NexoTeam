package com.grupo3.nexo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero

/** Reseñas recibidas en una sola tarjeta con filas separadas por una línea fina (Perfil e Historial del tutor). */
@Composable
fun ListaResenas(resenas: List<Resena>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().superficie(FormaOrganica)) {
        resenas.forEachIndexed { i, resena ->
            key(resena.id) {
                if (i > 0) Box(Modifier.fillMaxWidth().padding(horizontal = EspacioNormal).height(1.dp).background(Divisor))
                FilaResena(resena)
            }
        }
    }
}

@Composable
fun FilaResena(resena: Resena) {
    Column(Modifier.padding(EspacioNormal)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(resena.autor, tamano = 32.dp)
            Spacer(Modifier.width(EspacioAjustado))
            Column(Modifier.weight(1f)) {
                TextoLinea(resena.autor, MaterialTheme.typography.titleSmall, TextoPrimario)
                TextoLinea(resena.fecha, MaterialTheme.typography.bodySmall, TextoTercero)
            }
            Estrellas(resena.estrellas, tamano = 12.dp)
        }
        Text(
            resena.texto,
            color = TextoSecundario,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            modifier = Modifier.padding(top = EspacioCorto)
        )
    }
}
