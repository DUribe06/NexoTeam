package com.grupo3.nexo.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo3.nexo.domain.usecase.LARGO_CODIGO
import com.grupo3.nexo.ui.auth.RecuperarContrasenaViewModel.Paso
import com.grupo3.nexo.ui.components.IconosClarosEnBarraDeEstado
import com.grupo3.nexo.ui.components.IconosNexo
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioNormal

/** "¿Olvidaste tu contraseña?": primero se pide el código al correo y luego se escribe la contraseña nueva. */
@Composable
fun PantallaRecuperarContrasena(
    alTerminar: (correo: String) -> Unit,
    alVolver: () -> Unit,
    viewModel: RecuperarContrasenaViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.listo) {
        if (estado.listo) alTerminar(estado.correo)
    }
    BackHandler(enabled = estado.paso == Paso.CAMBIAR_CONTRASENA, onBack = viewModel::volverAPedir)

    IconosClarosEnBarraDeEstado()

    ContenidoRecuperar(
        estado = estado,
        alCambiarCorreo = viewModel::cambiarCorreo,
        alCambiarCodigo = viewModel::cambiarCodigo,
        alCambiarNueva = viewModel::cambiarNueva,
        alPedirCodigo = viewModel::pedirCodigo,
        alCambiarContrasena = viewModel::cambiarContrasena,
        alReenviar = viewModel::reenviar,
        alVolverAPedir = viewModel::volverAPedir,
        alVolver = alVolver
    )
}

@Composable
private fun ContenidoRecuperar(
    estado: RecuperarContrasenaViewModel.Estado,
    alCambiarCorreo: (String) -> Unit,
    alCambiarCodigo: (String) -> Unit,
    alCambiarNueva: (String) -> Unit,
    alPedirCodigo: () -> Unit,
    alCambiarContrasena: () -> Unit,
    alReenviar: () -> Unit,
    alVolverAPedir: () -> Unit,
    alVolver: () -> Unit
) {
    val focos = LocalFocusManager.current
    val iniciado = recordarEntradaIniciada()

    MarcoAcceso(iniciado = iniciado, altoMaximoPortada = AltoPortadaConTeclado, desplazable = true) {
        when (estado.paso) {
            Paso.PEDIR_CODIGO -> {
                val pedir = {
                    focos.clearFocus()
                    alPedirCodigo()
                }
                EncabezadoAcceso(
                    titulo = "Recupera tu contraseña",
                    subtitulo = "Escribe tu correo y te enviamos un código para crear una nueva.",
                    iniciado = iniciado
                )
                CampoPildora(
                    valor = estado.correo,
                    alCambiar = alCambiarCorreo,
                    marcador = "correo@ejemplo.com",
                    icono = IconosNexo.Correo,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    alPulsarListo = pedir,
                    modifier = Modifier.entrada(2, iniciado)
                )
                MensajeAcceso(error = estado.error)
                Spacer(Modifier.height(EspacioNormal))
                BotonVincularse(
                    texto = "Enviar código",
                    cargando = estado.cargando,
                    alClicar = pedir,
                    modifier = Modifier.entrada(3, iniciado)
                )
                Spacer(Modifier.height(EspacioAjustado))
                EnlaceAcceso(
                    pregunta = "¿Ya la recordaste? ",
                    accion = "Iniciar sesión",
                    alClicar = alVolver,
                    modifier = Modifier.entrada(4, iniciado)
                )
            }
            Paso.CAMBIAR_CONTRASENA -> {
                val cambiar = {
                    focos.clearFocus()
                    alCambiarContrasena()
                }
                EncabezadoAcceso(
                    titulo = "Crea tu contraseña nueva",
                    // El servidor no revela si el correo tiene cuenta: el texto tampoco lo afirma
                    subtitulo = "Si ${estado.correo} tiene una cuenta, le llegó un código de $LARGO_CODIGO dígitos.",
                    iniciado = iniciado
                )
                CampoCodigo(
                    valor = estado.codigo,
                    alCambiar = alCambiarCodigo,
                    conError = estado.codigoRechazado,
                    alPulsarListo = {},
                    modifier = Modifier.entrada(2, iniciado)
                )
                Spacer(Modifier.height(EspacioMedio))
                CampoPildora(
                    valor = estado.nueva,
                    alCambiar = alCambiarNueva,
                    marcador = "Contraseña nueva",
                    icono = IconosNexo.Candado,
                    esContrasena = true,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    alPulsarListo = cambiar,
                    modifier = Modifier.entrada(3, iniciado)
                )
                MensajeAcceso(error = estado.error, aviso = estado.aviso)
                Spacer(Modifier.height(EspacioNormal))
                BotonVincularse(
                    texto = "Cambiar contraseña",
                    cargando = estado.cargando,
                    alClicar = cambiar,
                    modifier = Modifier.entrada(4, iniciado)
                )
                Spacer(Modifier.height(EspacioAjustado))
                EnlaceReenviar(
                    espera = estado.espera,
                    reenviando = estado.reenviando,
                    alClicar = alReenviar,
                    modifier = Modifier.entrada(5, iniciado)
                )
                EnlaceAcceso(
                    pregunta = "¿No es tu correo? ",
                    accion = "Volver",
                    alClicar = alVolverAPedir,
                    modifier = Modifier.entrada(6, iniciado)
                )
            }
        }
        Spacer(Modifier.height(EspacioNormal))
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 884)
@Composable
private fun VistaPreviaRecuperar() {
    ContenidoRecuperar(
        estado = RecuperarContrasenaViewModel.Estado(
            paso = Paso.CAMBIAR_CONTRASENA,
            correo = "correo@ejemplo.com",
            espera = 42
        ),
        alCambiarCorreo = {},
        alCambiarCodigo = {},
        alCambiarNueva = {},
        alPedirCodigo = {},
        alCambiarContrasena = {},
        alReenviar = {},
        alVolverAPedir = {},
        alVolver = {}
    )
}
