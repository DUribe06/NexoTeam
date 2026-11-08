package com.grupo3.nexo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grupo3.nexo.ui.theme.AltoPildora
import com.grupo3.nexo.ui.theme.AreaTactilMinima
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.EspacioFino
import com.grupo3.nexo.ui.theme.EspacioPildora
import com.grupo3.nexo.ui.theme.EtiquetaBoton
import com.grupo3.nexo.ui.theme.FormaBoton
import com.grupo3.nexo.ui.theme.Icono17
import com.grupo3.nexo.ui.theme.TinteBosque

/**
 * "Abrir chat" como píldora (PRODUCT.md): se ve de [AltoPildora] y se toca en [AreaTactilMinima].
 * [fondo] cambia según la superficie donde se apoya (blanco sobre tinte, tinte sobre blanco).
 */
@Composable
fun PildoraAbrirChat(conQuien: String, alClicar: () -> Unit, fondo: Color = TinteBosque) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .areaTactilSinCambiarMedida()
            .heightIn(min = AreaTactilMinima)
            .pulsable(etiqueta = "abrir chat con $conQuien") { alClicar() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = AltoPildora)
                .background(fondo, FormaBoton)
                .padding(horizontal = EspacioPildora)
        ) {
            Icon(IconosNexo.Chat, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(Icono17))
            Spacer(Modifier.width(EspacioFino))
            Text("Abrir chat", color = BosqueProfundo, style = EtiquetaBoton)
        }
    }
}
