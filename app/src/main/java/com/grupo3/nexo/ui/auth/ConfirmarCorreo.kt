package com.grupo3.nexo.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo3.nexo.domain.usecase.LARGO_CODIGO
import com.grupo3.nexo.ui.components.IconosClarosEnBarraDeEstado
import com.grupo3.nexo.ui.components.areaTactilSinCambiarMedida
import com.grupo3.nexo.ui.theme.AreaTactilMinima
import com.grupo3.nexo.ui.theme.BordeSeleccion
import com.grupo3.nexo.ui.theme.BordeSutil
import com.grupo3.nexo.ui.theme.Bosque
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.CuerpoEnlace
import com.grupo3.nexo.ui.theme.DigitoCodigo
import com.grupo3.nexo.ui.theme.ElevacionCampoReposo
import com.grupo3.nexo.ui.theme.ErrorBosque
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.FormaTarjeta
import com.grupo3.nexo.ui.theme.SombraAmbienteCampoAcceso
import com.grupo3.nexo.ui.theme.SombraPuntualCampoAcceso
import com.grupo3.nexo.ui.theme.Superficie
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.resorteSuave
import com.grupo3.nexo.ui.theme.resorteSuaveDp

/** Alto máximo de la foto en las pantallas que abren el teclado al entrar. */
internal val AltoPortadaConTeclado = 200.dp

/** Paso obligatorio tras registrarse, o al entrar con un correo sin confirmar: el código de 6 dígitos. */
@Composable
fun PantallaConfirmarCorreo(
    alEntrar: () -> Unit,
    alVolver: () -> Unit,
    viewModel: ConfirmarCorreoViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.entro) {
        if (estado.entro) alEntrar()
    }

    IconosClarosEnBarraDeEstado()

    ContenidoConfirmarCorreo(
        estado = estado,
        alCambiarCodigo = viewModel::cambiarCodigo,
        alConfirmar = viewModel::confirmar,
        alReenviar = viewModel::reenviar,
        alVolver = alVolver
    )
}

@Composable
private fun ContenidoConfirmarCorreo(
    estado: ConfirmarCorreoViewModel.Estado,
    alCambiarCodigo: (String) -> Unit,
    alConfirmar: () -> Unit,
    alReenviar: () -> Unit,
    alVolver: () -> Unit
) {
    val focos = LocalFocusManager.current
    val confirmar = {
        focos.clearFocus()
        alConfirmar()
    }
    val iniciado = recordarEntradaIniciada()

    MarcoAcceso(iniciado = iniciado, altoMaximoPortada = AltoPortadaConTeclado, desplazable = true) {
        EncabezadoAcceso(
            titulo = "Confirma tu correo",
            subtitulo = "Escribe el código de $LARGO_CODIGO dígitos que enviamos a ${estado.correo}.",
            iniciado = iniciado
        )
        CampoCodigo(
            valor = estado.codigo,
            alCambiar = alCambiarCodigo,
            conError = estado.codigoRechazado,
            alPulsarListo = confirmar,
            modifier = Modifier.entrada(2, iniciado)
        )
        MensajeAcceso(error = estado.error, aviso = estado.aviso)
        Spacer(Modifier.height(EspacioNormal))
        BotonVincularse(
            texto = "Confirmar",
            cargando = estado.cargando,
            alClicar = confirmar,
            modifier = Modifier.entrada(3, iniciado)
        )
        Spacer(Modifier.height(EspacioAjustado))
        EnlaceReenviar(
            espera = estado.espera,
            reenviando = estado.reenviando,
            alClicar = alReenviar,
            modifier = Modifier.entrada(4, iniciado)
        )
        EnlaceAcceso(
            pregunta = "¿No es tu correo? ",
            accion = "Volver",
            alClicar = alVolver,
            modifier = Modifier.entrada(5, iniciado)
        )
        Spacer(Modifier.height(EspacioNormal))
    }
}

/**
 * Una sola entrada de texto dibujada como seis casillas: así funcionan el teclado numérico, borrar y pegar el
 * código completo. El texto real queda invisible encima, para que mantener pulsado siga ofreciendo "Pegar".
 */
@Composable
internal fun CampoCodigo(
    valor: String,
    alCambiar: (String) -> Unit,
    conError: Boolean,
    alPulsarListo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val foco = remember { FocusRequester() }
    var enfocado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { foco.requestFocus() }

    CompositionLocalProvider(LocalTextSelectionColors provides TextSelectionColors(Color.Transparent, Color.Transparent)) {
        BasicTextField(
            value = valor,
            onValueChange = alCambiar,
            singleLine = true,
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { alPulsarListo() }),
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(foco)
                .onFocusChanged { enfocado = it.isFocused }
                .semantics { contentDescription = "Código de $LARGO_CODIGO dígitos" },
            decorationBox = { campo ->
                Box {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(EspacioCorto),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(LARGO_CODIGO) { indice ->
                            CasillaCodigo(
                                digito = valor.getOrNull(indice),
                                activa = enfocado && indice == valor.length.coerceAtMost(LARGO_CODIGO - 1),
                                conError = conError,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Box(Modifier.matchParentSize()) { campo() }
                }
            }
        )
    }
}

/** Misma piel de los campos del acceso (fondo, borde y realce al enfocarse), en tamaño de un dígito. */
@Composable
private fun CasillaCodigo(digito: Char?, activa: Boolean, conError: Boolean, modifier: Modifier = Modifier) {
    val colorBorde by animateColorAsState(
        when {
            conError -> ErrorBosque
            activa -> BordeSeleccion
            else -> BordeSutil
        },
        resorteSuave(),
        label = "borde"
    )
    val grosorBorde by animateDpAsState(if (activa || conError) 1.5.dp else 1.dp, resorteSuaveDp(), label = "grosor")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(54.dp)
            .shadow(ElevacionCampoReposo, FormaTarjeta, ambientColor = SombraAmbienteCampoAcceso, spotColor = SombraPuntualCampoAcceso)
            .background(Superficie, FormaTarjeta)
            .border(grosorBorde, colorBorde, FormaTarjeta)
    ) {
        Text(digito?.toString().orEmpty(), color = BosqueProfundo, style = DigitoCodigo)
    }
}

/** "Reenviar código", con la cuenta atrás mientras el servidor no permite otro envío. */
@Composable
internal fun EnlaceReenviar(espera: Int, reenviando: Boolean, alClicar: () -> Unit, modifier: Modifier = Modifier) {
    val libre = espera == 0 && !reenviando
    val texto = when {
        reenviando -> "Enviando código…"
        espera > 0 -> "Reenviar código en ${espera / 60}:${(espera % 60).toString().padStart(2, '0')}"
        else -> "Reenviar código"
    }
    Text(
        texto,
        color = if (libre) Bosque else TextoSecundario,
        fontWeight = if (libre) FontWeight.SemiBold else FontWeight.Normal,
        style = LocalTextStyle.current.merge(CuerpoEnlace),
        modifier = modifier
            .areaTactilSinCambiarMedida()
            .clickable(enabled = libre, role = Role.Button, onClick = alClicar)
            .heightIn(min = AreaTactilMinima)
            .wrapContentHeight()
            .padding(horizontal = EspacioCercano)
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 884)
@Composable
private fun VistaPreviaConfirmarCorreo() {
    ContenidoConfirmarCorreo(
        estado = ConfirmarCorreoViewModel.Estado(correo = "correo@ejemplo.com", codigo = "482", espera = 42),
        alCambiarCodigo = {},
        alConfirmar = {},
        alReenviar = {},
        alVolver = {}
    )
}
