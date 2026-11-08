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
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.theme.TamanoAvatarResena
import com.grupo3.nexo.ui.theme.TamanoEstrellaCompacta
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

/** Una reseña en su propia tarjeta, para listas largas como la del Perfil del tutor. */
@Composable
fun TarjetaResena(resena: Resena, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().superficie(FormaOrganica)) { FilaResena(resena) }
}

/** Contenido de una reseña: autor, antigüedad, estrellas y texto. */
@Composable
fun FilaResena(resena: Resena) {
    Column(Modifier.padding(EspacioNormal)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(resena.autor, tamano = TamanoAvatarResena)
            Spacer(Modifier.width(EspacioMedio))
            Column(Modifier.weight(1f)) {
                TextoLinea(resena.autor, MaterialTheme.typography.titleSmall, TextoPrimario)
                TextoLinea(formatoHace(resena.fecha), MaterialTheme.typography.bodySmall, TextoTercero)
            }
            Estrellas(resena.estrellas, tamano = TamanoEstrellaCompacta)
        }
        Spacer(Modifier.height(EspacioAjustado))
        Text(resena.texto, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
    }
}
