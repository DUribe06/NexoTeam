package com.grupo3.nexo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.grupo3.nexo.ui.theme.BordeMarcado
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioAmplio
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioMinimo
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.FormaControl
import com.grupo3.nexo.ui.theme.FormaHoja
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.theme.Menta
import com.grupo3.nexo.ui.theme.Rojo
import com.grupo3.nexo.ui.theme.SobreOscuro
import com.grupo3.nexo.ui.theme.Superficie
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario

/**
 * Hoja inferior de la app: blanca, esquinas de [FormaHoja], asa discreta,
 * título + subtítulo y contenido desplazable que respeta el teclado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaNexo(
    titulo: String,
    alCerrar: () -> Unit,
    subtitulo: String? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    // Los íconos del sistema siguen a la pantalla de abajo: claros sobre foto, oscuros sobre fondo claro
    val vistaApp = LocalView.current
    ModalBottomSheet(
        onDismissRequest = alCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Superficie,
        contentColor = TextoPrimario,
        scrimColor = BosqueProfundo.copy(alpha = 0.45f),
        shape = FormaHoja,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = EspacioMedio, bottom = EspacioCorto)
                    .size(width = 40.dp, height = 4.dp)
                    .background(BordeMarcado, CircleShape)
            )
        }
    ) {
        // La hoja es su propia ventana y no hereda ese ajuste
        val vista = LocalView.current
        SideEffect {
            (vista.parent as? DialogWindowProvider)?.window?.let { ventana ->
                WindowCompat.getInsetsController(ventana, vista).isAppearanceLightStatusBars =
                    ViewCompat.getWindowInsetsController(vistaApp)?.isAppearanceLightStatusBars ?: false
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EspacioAmplio)
                .padding(top = EspacioCorto, bottom = EspacioMargen)
        ) {
            Text(
                titulo,
                color = TextoPrimario,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitulo != null) {
                Spacer(Modifier.height(EspacioMinimo))
                Text(subtitulo, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
            }
            contenido()
        }
    }
}

/** Diálogo de confirmación para acciones que no se pueden deshacer. */
@Composable
fun DialogoConfirmacion(
    titulo: String,
    mensaje: String,
    confirmar: String,
    alConfirmar: () -> Unit,
    alCerrar: () -> Unit,
    cancelar: String = "Volver",
    peligro: Boolean = true
) {
    AlertDialog(
        onDismissRequest = alCerrar,
        containerColor = Superficie,
        shape = FormaOrganica,
        title = { Text(titulo, style = MaterialTheme.typography.headlineSmall, color = TextoPrimario) },
        text = { Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario) },
        confirmButton = {
            BotonTexto(confirmar, alClicar = alConfirmar, color = if (peligro) Rojo else com.grupo3.nexo.ui.theme.BosqueAcento)
        },
        dismissButton = {
            BotonTexto(cancelar, alClicar = alCerrar, color = TextoSecundario)
        }
    )
}

/** Aviso transitorio (snackbar): bloque bosque plano de esquinas suaves, con check menta. */
@Composable
fun AvisoNexo(estado: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(estado, modifier = modifier) { datos: SnackbarData ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = EspacioNormal, vertical = EspacioCorto)
                .fillMaxWidth()
                .background(BosqueProfundo, FormaControl)
                .padding(horizontal = EspacioNormal, vertical = EspacioCercano)
        ) {
            Icon(IconosNexo.MarcaCirculo, contentDescription = null, tint = Menta, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(EspacioAjustado))
            Text(datos.visuals.message, color = SobreOscuro, style = MaterialTheme.typography.titleSmall)
        }
    }
}
