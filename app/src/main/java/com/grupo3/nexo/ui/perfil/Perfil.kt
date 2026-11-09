package com.grupo3.nexo.ui.perfil

import com.grupo3.nexo.ui.theme.IconoPequenoCamara
import com.grupo3.nexo.ui.theme.TamanoInsigniaCamara
import com.grupo3.nexo.ui.theme.TamanoAvatarPropio
import com.grupo3.nexo.ui.components.RotuloGrupo
import com.grupo3.nexo.ui.theme.LocalAnimacionesReducidas
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateFloatAsState
import com.grupo3.nexo.ui.theme.Bosque
import com.grupo3.nexo.ui.components.Avatar
import androidx.compose.material3.CircularProgressIndicator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import com.grupo3.nexo.ui.theme.AreaTactilMinima
import com.grupo3.nexo.ui.auth.HojaCondiciones
import com.grupo3.nexo.ui.components.plural
import com.grupo3.nexo.ui.components.textoDePenalizacionesRecientes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import com.grupo3.nexo.ui.theme.FormaOpcion
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo3.nexo.R
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.ResumenActividad
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.ui.auth.MENSAJE_GOOGLE_NO_DISPONIBLE
import com.grupo3.nexo.ui.auth.MENSAJE_GOOGLE_SIN_CUENTAS
import com.grupo3.nexo.ui.auth.ResultadoGoogle
import com.grupo3.nexo.ui.auth.pedirTokenGoogle
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.ui.components.AvisoNexo
import com.grupo3.nexo.ui.components.BotonSecundario
import com.grupo3.nexo.ui.theme.Rojo
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.ui.components.BotonTexto
import com.grupo3.nexo.ui.components.Campo
import com.grupo3.nexo.ui.components.HojaNexo
import com.grupo3.nexo.ui.components.URL_PRIVACIDAD
import com.grupo3.nexo.ui.components.CabeceraBosque
import com.grupo3.nexo.ui.components.Carga
import com.grupo3.nexo.ui.components.DialogoConfirmacion
import com.grupo3.nexo.ui.components.EncabezadoSeccion
import com.grupo3.nexo.ui.components.EsqueletoLista
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.grupo3.nexo.ui.theme.AltoBarraAvance
import com.grupo3.nexo.ui.theme.AltoFilaMenu
import com.grupo3.nexo.ui.theme.TamanoBotonIr
import com.grupo3.nexo.ui.components.ErrorDeLista
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.IconosClarosEnBarraDeEstado
import com.grupo3.nexo.ui.components.IconosNexo
import com.grupo3.nexo.ui.components.ListaResenas
import com.grupo3.nexo.ui.components.Onda
import com.grupo3.nexo.ui.components.VeloBarraEstado
import com.grupo3.nexo.ui.components.aparicion
import com.grupo3.nexo.ui.components.avisoFallo
import com.grupo3.nexo.ui.components.cargaDe
import com.grupo3.nexo.ui.components.enumerar
import com.grupo3.nexo.ui.components.diaYMes
import com.grupo3.nexo.ui.components.formatoHora
import com.grupo3.nexo.ui.components.formatoHoras
import com.grupo3.nexo.ui.components.formatoValoracion
import com.grupo3.nexo.ui.components.mostrarError
import com.grupo3.nexo.ui.components.rememberPrimeraVez
import com.grupo3.nexo.ui.components.solaparHaciaArriba
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.components.formatoPrecio
import com.grupo3.nexo.ui.theme.BordeAcento
import com.grupo3.nexo.ui.theme.BordeRojoSalir
import com.grupo3.nexo.ui.theme.BordeSutil
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.BordeRojoPeligro
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.EnfasisMedio
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioAmplio
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioExtenso
import com.grupo3.nexo.ui.theme.EspacioHolgado
import com.grupo3.nexo.ui.theme.EspacioFino
import com.grupo3.nexo.ui.theme.EspacioMargen
import kotlinx.coroutines.launch
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioMicro
import com.grupo3.nexo.ui.theme.EspacioMinimo
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.EspacioSuelto
import com.grupo3.nexo.ui.theme.Fondo
import com.grupo3.nexo.ui.theme.FormaBoton
import com.grupo3.nexo.ui.theme.FormaCampoAlto
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.theme.IconoCuerpo
import com.grupo3.nexo.ui.theme.IconoMediano
import com.grupo3.nexo.ui.theme.IconoNormal
import com.grupo3.nexo.ui.theme.IconoSecundario
import com.grupo3.nexo.ui.theme.MielFuerte
import com.grupo3.nexo.ui.theme.SobreOscuro
import com.grupo3.nexo.ui.theme.SobreOscuroSuave
import com.grupo3.nexo.ui.theme.SombraAmbiente
import com.grupo3.nexo.ui.theme.SombraPuntual
import com.grupo3.nexo.ui.theme.Superficie
import com.grupo3.nexo.ui.theme.TextoCifraResumen
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoSobreTinte
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.TinteBosque
import com.grupo3.nexo.ui.theme.cambioContenido
import com.grupo3.nexo.ui.theme.especColor
import com.grupo3.nexo.ui.theme.pincelBosque
import com.grupo3.nexo.ui.theme.resorteIndicador
import com.grupo3.nexo.ui.theme.ElevacionTarjeta


/* ==================== Pantalla ==================== */

/** Alto aproximado del nombre y la carrera sobre el selector, con dos líneas de nombre. */
private val ALTO_NOMBRE_SOBRE_SELECTOR = 130.dp

/**
 * Perfil propio, derivado del Inicio: foto con el nombre como saludo, el selector de modo montado
 * sobre la onda (en el lugar del buscador) y, debajo, lo que define al usuario en el modo actual.
 * No repite lo que ya está en la barra (solicitudes, chats).
 */
@Composable
fun PantallaPerfil(
    alCerrarSesion: () -> Unit,
    alAbrirHistorial: () -> Unit,
    alAbrirDisponibilidad: () -> Unit,
    alAbrirResenas: () -> Unit,
    alAbrirMaterias: () -> Unit,
    alEditarPerfilTutor: () -> Unit,
    alVerPerfilPublico: (String) -> Unit,
    viewModel: PerfilViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val usuario = estado.usuario
    val desplazamiento = rememberScrollState()
    // El velo aparece cuando el selector llega a la barra de estado, sea cual sea el alto de la foto
    val altoBarra = WindowInsets.statusBars.getTop(LocalDensity.current)
    var arribaSelector by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    // El nombre queda por encima del selector: el velo debe cubrirlo antes de que llegue a la barra
    val altoNombre = with(LocalDensity.current) { ALTO_NOMBRE_SOBRE_SELECTOR.roundToPx() }
    val cabeceraVisible by remember { derivedStateOf { arribaSelector > altoBarra + altoNombre } }
    IconosClarosEnBarraDeEstado(activo = cabeceraVisible)
    val iniciado = rememberPrimeraVez()
    var confirmarSalida by remember { mutableStateOf(false) }
    var eliminando by remember { mutableStateOf(false) }
    var viendoCondiciones by remember { mutableStateOf(false) }
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val subiendoFoto by viewModel.subiendoFoto.collectAsStateWithLifecycle()
    // El selector de fotos del sistema no pide permisos: solo entrega la imagen elegida
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.cambiarFoto(uri.toString()) { fallo ->
            alcance.launch { avisos.mostrarError(avisoFallo("cambiar la foto", fallo)) }
        }
    }

    val pedirFoto: () -> Unit = { elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val altoPantalla = maxHeight
        // Con poco contenido la foto crece hasta llenar la pantalla, en vez de dejar un vacío blanco.
        CabeceraAjustable(
            altoPantalla = altoPantalla,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(desplazamiento),
            cabecera = { Cabecera(usuario, iniciado, subiendoFoto, alCambiarFoto = pedirFoto) }
        ) {
            Column(Modifier.padding(bottom = EspacioExtenso)) {
                // El selector ocupa el lugar del buscador: montado sobre la onda, con la única sombra
                SelectorModo(
                    actual = usuario.rol,
                    alElegir = { rol ->
                        viewModel.elegirModo(rol) { fallo ->
                            alcance.launch { avisos.mostrarError(avisoFallo("cambiar de modo", fallo)) }
                        }
                    },
                    modifier = Modifier
                        .solaparHaciaArriba(30.dp)
                        .onGloballyPositioned { arribaSelector = it.positionInRoot().y }
                        .padding(horizontal = EspacioMargen)
                        .aparicion(1, iniciado)
                )

                // Lo propio de cada modo se funde al cambiar, sin saltos de la cabecera ni del selector
                AnimatedContent(
                    targetState = estado.esTutor,
                    transitionSpec = { cambioContenido() },
                    label = "contenidoModo",
                    modifier = Modifier.aparicion(2, iniciado)
                ) { esTutor ->
                    Column(Modifier.fillMaxWidth()) {
                        estado.resumen?.let {
                            AvisoPenalizaciones(it, esTutor, Modifier.padding(start = EspacioMargen, end = EspacioMargen, top = EspacioSuelto))
                        }
                        // Lo accionable va primero; las cifras y materias quedan como contexto
                        if (esTutor) {
                            Column(
                                Modifier.padding(start = EspacioMargen, end = EspacioMargen, top = EspacioSuelto),
                                verticalArrangement = Arrangement.spacedBy(EspacioSuelto)
                            ) {
                                TarjetaCompletaPerfil(usuario, estado.pasosListos, subiendoFoto, pedirFoto, alEditarPerfilTutor, alAbrirMaterias)
                                TarjetaDisponibilidad(alAbrirDisponibilidad)
                            }
                            Cifras(estado.resumen, true, Modifier.padding(horizontal = EspacioMargen, vertical = EspacioSuelto))
                            BloqueTutor(usuario, estado.resenas, cargando = estado.resumen == null, error = estado.error, alReintentar = viewModel::reintentar, alVerTodas = alAbrirResenas, alVerHistorial = alAbrirHistorial, alEditarMaterias = alAbrirMaterias, alEditarPerfil = alEditarPerfilTutor)
                        } else {
                            val pendientes = estado.resumen?.porCalificar ?: 0
                            if (pendientes > 0) {
                                AvisoPorCalificar(pendientes, alAbrirHistorial, Modifier.padding(start = EspacioMargen, end = EspacioMargen, top = EspacioSuelto))
                            }
                            Cifras(estado.resumen, false, Modifier.padding(horizontal = EspacioMargen, vertical = EspacioSuelto))
                            if (estado.error) {
                                ErrorDeLista(titulo = "No pudimos cargar tu actividad", alReintentar = viewModel::reintentar)
                            }
                            estado.resumen?.materias?.takeIf { it.isNotEmpty() }?.let {
                                SeccionMaterias("Has estudiado", it, Modifier.padding(horizontal = EspacioMargen))
                            }
                        }
                        Spacer(Modifier.height(EspacioMargen))
                        Menu(
                            esTutor = esTutor,
                            alAbrirHistorial = alAbrirHistorial,
                            alSalir = { confirmarSalida = true },
                            alEliminar = { eliminando = true },
                            alVerCondiciones = { viendoCondiciones = true },
                            alVerPerfilPublico = { alVerPerfilPublico(usuario.id) }
                        )
                    }
                }
            }
        }
        VeloBarraEstado(visible = !cabeceraVisible)
        AvisoNexo(avisos, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = EspacioMedio))
    }

    if (viendoCondiciones) HojaCondiciones(alCerrar = { viendoCondiciones = false })

    if (eliminando) {
        val eliminandoCuenta by viewModel.eliminandoCuenta.collectAsStateWithLifecycle()
        var errorContrasena by remember { mutableStateOf(false) }
        // La hoja se cierra solo si salió bien (la navegación ya cierra la sesión); si no, se queda para corregir
        val alTerminar: (Throwable?) -> Unit = { fallo ->
            if (fallo == null) {
                eliminando = false
                alCerrarSesion()
            } else if ((fallo as? ErrorApi)?.codigo == CodigoError.CONTRASENA_INCORRECTA) {
                errorContrasena = true
            } else {
                alcance.launch { avisos.mostrarError(avisoFallo("eliminar tu cuenta", fallo)) }
            }
        }
        val contexto = LocalContext.current
        HojaEliminarCuenta(
            conContrasena = usuario.tieneContrasena,
            eliminando = eliminandoCuenta,
            errorContrasena = errorContrasena,
            alCambiarContrasena = { errorContrasena = false },
            alCerrar = { if (!eliminandoCuenta) eliminando = false },
            alConfirmar = { contrasena ->
                errorContrasena = false
                viewModel.eliminarCuenta(contrasena, alTerminar)
            },
            alConfirmarConGoogle = {
                alcance.launch {
                    when (val cuenta = pedirTokenGoogle(contexto)) {
                        is ResultadoGoogle.Token -> viewModel.eliminarCuentaConGoogle(cuenta.valor, alTerminar)
                        ResultadoGoogle.Cancelado -> Unit
                        ResultadoGoogle.SinCuentas -> avisos.mostrarError(MENSAJE_GOOGLE_SIN_CUENTAS)
                        ResultadoGoogle.NoDisponible -> avisos.mostrarError(MENSAJE_GOOGLE_NO_DISPONIBLE)
                    }
                }
            }
        )
    }

    if (confirmarSalida) {
        DialogoConfirmacion(
            titulo = "¿Cerrar sesión?",
            mensaje = "Tendrás que volver a iniciar sesión para entrar.",
            confirmar = "Cerrar sesión",
            alConfirmar = {
                confirmarSalida = false
                viewModel.salir()
                alCerrarSesion()
            },
            alCerrar = { confirmarSalida = false }
        )
    }
}

/**
 * Pone la [cabecera] sobre el [contenido] y le da el alto que sobra en pantalla (entre 270 y 430 dp; más con letra grande),
 * así el perfil siempre llena la pantalla y solo se desplaza cuando el contenido lo exige.
 */
@Composable
private fun CabeceraAjustable(
    altoPantalla: Dp,
    modifier: Modifier = Modifier,
    cabecera: @Composable () -> Unit,
    contenido: @Composable () -> Unit
) {
    Layout(contents = listOf(cabecera, contenido), modifier = modifier) { (medCabecera, medContenido), restricciones ->
        val ancho = restricciones.maxWidth
        val resto = medContenido.first().measure(Constraints(maxWidth = ancho))
        // Con letra grande el nombre y el correo necesitan más alto: el mínimo crece con la letra
        val minimo = (270.dp.roundToPx() * fontScale.coerceIn(1f, 1.5f)).toInt()
        val altoCabecera = (altoPantalla.roundToPx() - resto.height)
            .coerceIn(minimo, maxOf(minimo, 430.dp.roundToPx()))
        val cab = medCabecera.first().measure(Constraints.fixed(ancho, altoCabecera))
        layout(ancho, altoCabecera + resto.height) {
            cab.place(0, 0)
            resto.place(0, altoCabecera)
        }
    }
}

/* ==================== Piezas ==================== */

/** Botón circular verde bosque con flecha, parecido al "ir" del buscador del Inicio, pero de 36 dp y en verde liso (el del Inicio mide 40 dp y lleva degradado). */
@Composable
private fun BotonIr(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(TamanoBotonIr)
            .background(BosqueMedio, CircleShape)
    ) {
        Icon(IconosNexo.Flecha, contentDescription = null, tint = SobreOscuro, modifier = Modifier.size(IconoCuerpo))
    }
}

/** Misma cabecera que el Inicio: foto, nombre grande como el saludo y la onda del lienzo. */
@Composable
private fun Cabecera(usuario: Usuario, iniciado: Boolean, subiendoFoto: Boolean, alCambiarFoto: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        CabeceraBosque(conFoto = true, alturaOnda = 58.dp, modifier = Modifier.matchParentSize()) {
            // La cabecera compartida envuelve su contenido; esta columna lo estira para bajar el nombre
            Column(Modifier.fillMaxHeight()) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = EspacioAmplio, end = EspacioNormal, top = EspacioCorto)
                    .height(52.dp)
            ) {
                Text(
                    "Perfil",
                    color = SobreOscuroSuave,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.weight(1f).heightIn(min = 40.dp))
            Column(
                Modifier
                    .padding(start = EspacioAmplio, end = EspacioAmplio, bottom = EspacioMargen)
                    .aparicion(0, iniciado)
            ) {
                // Foto y nombre lado a lado (como Instagram o LinkedIn): la cabecera no crece al poner foto
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FotoEditable(usuario, subiendoFoto, alCambiarFoto)
                    Spacer(Modifier.width(EspacioNormal))
                    Column(Modifier.weight(1f)) {
                        Text(
                            usuario.nombre,
                            color = SobreOscuro,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(Modifier.height(EspacioMicro))
                        // Usuario y correo en líneas propias: en una columna angosta no caben juntos
                        Text(usuario.handle, color = SobreOscuroSuave, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(usuario.correo, color = SobreOscuroSuave, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            }
        }
        Onda(
            Fondo,
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(AreaTactilMinima)
        )
    }
}

/** Avatar grande con un botón de cámara: tocarlo abre el selector de fotos del teléfono. */
@Composable
private fun FotoEditable(usuario: Usuario, subiendo: Boolean, alCambiar: () -> Unit) {
    // Sin recorte en el contenedor: la insignia de la cámara asoma sobre el borde del círculo
    Box(
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = !subiendo,
            onClickLabel = "Cambiar foto de perfil",
            role = Role.Button,
            onClick = alCambiar
        )
    ) {
        Avatar(usuario.nombre, tamano = TamanoAvatarPropio, anillo = SobreOscuro, fotoUrl = usuario.fotoUrl)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = EspacioMinimo, y = EspacioMinimo)
                .size(TamanoInsigniaCamara)
                .border(2.dp, SobreOscuro, CircleShape)
                .padding(2.dp)
                .background(BosqueMedio, CircleShape)
        ) {
            if (subiendo) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = SobreOscuro, modifier = Modifier.size(IconoPequenoCamara))
            } else {
                Icon(IconosNexo.Camara, contentDescription = null, tint = SobreOscuro, modifier = Modifier.size(IconoPequenoCamara))
            }
        }
    }
}

/**
 * Tres cifras sin contenedor, separadas por líneas finas, con la tipografía de la hora del Inicio.
 * Estudiante: sesiones, horas, por calificar. Tutor: promedio recibido, sesiones dadas, horas dadas (mapa, sección 3).
 */
@Composable
private fun Cifras(resumen: ResumenActividad?, esTutor: Boolean, modifier: Modifier = Modifier) {
    val cifras: List<Triple<String, String, Boolean>> = when {
        resumen == null && esTutor -> listOf(Triple("–", "calificación", false), Triple("–", "sesiones dadas", false), Triple("–", "dadas", false))
        resumen == null -> listOf(Triple("–", "sesiones", false), Triple("–", "de estudio", false), Triple("–", "tutores", false))
        esTutor -> listOf(
            Triple(resumen.promedioRecibido?.let(::formatoValoracion) ?: "–", "calificación", resumen.promedioRecibido != null),
            Triple("${resumen.sesiones}", plural(resumen.sesiones, "sesión dada", "sesiones dadas"), false),
            Triple(formatoHoras(resumen.horas), "dadas", false)
        )
        else -> listOf(
            Triple("${resumen.sesiones}", plural(resumen.sesiones, "sesión", "sesiones"), false),
            Triple(formatoHoras(resumen.horas), "de estudio", false),
            Triple("${resumen.personas}", plural(resumen.personas, "tutor", "tutores"), false)
        )
    }
    // Con letra muy grande las etiquetas se partirían a media palabra: una cifra por fila, sin divisores
    if (LocalDensity.current.fontScale > 1.4f) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(EspacioMedio)) {
            cifras.forEach { (valor, etiqueta, conEstrella) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) { }
                ) {
                    ValorCifra(valor, conEstrella)
                    Spacer(Modifier.width(EspacioCorto))
                    Text(etiqueta, color = TextoTercero, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            cifras.forEachIndexed { i, (valor, etiqueta, conEstrella) ->
                if (i > 0) Box(Modifier.padding(vertical = EspacioFino).width(1.dp).fillMaxHeight().background(Divisor))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) { }
                ) {
                    ValorCifra(valor, conEstrella)
                    Text(etiqueta, color = TextoTercero, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ValorCifra(valor: String, conEstrella: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            valor,
            color = BosqueProfundo,
            style = TextoCifraResumen
        )
        if (conEstrella) {
            Spacer(Modifier.width(EspacioMinimo))
            Icon(IconosNexo.Estrella, contentDescription = null, tint = MielFuerte, modifier = Modifier.size(IconoNormal))
        }
    }
}

/** Tarjeta blanca con el mismo interior en todas las secciones; el título de la sección va fuera, como en el Inicio. */
@Composable
private fun TarjetaSeccion(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .superficie(FormaOrganica)
            .padding(EspacioNormal)
    ) { contenido() }
}

/** Título de sección con su acción, fuera de la tarjeta, y el contenido justo debajo (patrón del Inicio). */
@Composable
private fun Seccion(
    titulo: String,
    accion: String? = null,
    alAccion: (() -> Unit)? = null,
    contenido: @Composable () -> Unit
) {
    Column {
        EncabezadoSeccion(titulo, accion = accion, alAccion = alAccion)
        Spacer(Modifier.height(EspacioMicro))
        contenido()
    }
}

/** Lo que falta para que el perfil de tutor se vea completo; no aparece cuando ya está todo. */
@Composable
private fun TarjetaCompletaPerfil(
    usuario: Usuario,
    listosPasos: Set<PasoPerfil>,
    subiendoFoto: Boolean,
    alPonerFoto: () -> Unit,
    alEditarPerfil: () -> Unit,
    alEditarMaterias: () -> Unit
) {
    val puntos = listOf(
        Triple(PasoPerfil.FOTO, "Agrega tu foto", alPonerFoto),
        Triple(PasoPerfil.TARIFA, "Fija tu tarifa", alEditarPerfil),
        Triple(PasoPerfil.MATERIAS, "Agrega tus materias", alEditarMaterias),
        Triple(PasoPerfil.PRESENTACION, "Escribe tu presentación", alEditarPerfil)
    )
    val listos = listosPasos.size
    if (listos == puntos.size) return
    TarjetaSeccion {
        Column(Modifier.semantics(mergeDescendants = true) { heading() }) {
            Text("Completa tu perfil", color = TextoPrimario, style = MaterialTheme.typography.titleMedium)
            Text("$listos de ${puntos.size} listos", color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
        }
        val avance by animateFloatAsState(
            listos / puntos.size.toFloat(),
            animationSpec = if (LocalAnimacionesReducidas.current) snap() else spring(),
            label = "avancePerfil"
        )
        Box(
            Modifier
                .padding(vertical = EspacioMedio)
                .fillMaxWidth()
                .height(AltoBarraAvance)
                .clip(CircleShape)
                .background(TinteBosque)
        ) {
            // Con 0 listos se ve un poco de avance: la tarea se lee como empezada, no como vacía
            Box(Modifier.fillMaxHeight().fillMaxWidth(avance.coerceAtLeast(0.04f)).clip(CircleShape).background(BosqueMedio))
        }
        puntos.filter { it.first !in listosPasos }.forEach { (paso, texto, alClicar) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AreaTactilMinima)
                    .clip(FormaOpcion)
                    .clickable(enabled = !(paso == PasoPerfil.FOTO && subiendoFoto), role = Role.Button, onClick = alClicar)
            ) {
                Text(texto, color = TextoPrimario, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Icon(IconosNexo.Caret, contentDescription = null, tint = IconoSecundario, modifier = Modifier.size(IconoNormal))
            }
        }
        Text(
            "Con el perfil completo apareces mejor en las búsquedas.",
            color = TextoTercero,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = EspacioFino)
        )
    }
}

/** Modo tutor: tarifa, materias que enseña y reseñas recibidas. */
@Composable
private fun BloqueTutor(
    usuario: Usuario,
    resenas: List<Resena>,
    cargando: Boolean,
    error: Boolean,
    alReintentar: () -> Unit,
    alVerTodas: () -> Unit,
    alVerHistorial: () -> Unit,
    alEditarMaterias: () -> Unit,
    alEditarPerfil: () -> Unit
) {
    val carga = cargaDe(cargando, resenas.isEmpty(), error)
    val precio = usuario.tarifaFijada
    val datos = listOf(usuario.universidad, usuario.carrera).filter { it.isNotBlank() }.joinToString(" · ")
    Column(Modifier.padding(horizontal = EspacioMargen), verticalArrangement = Arrangement.spacedBy(EspacioSuelto)) {
        Seccion("Tu tarifa", accion = if (precio != null) "Editar" else "Fijar", alAccion = alEditarPerfil) {
            TarjetaSeccion {
                if (precio != null) {
                    val cifra = MaterialTheme.typography.headlineSmall.toSpanStyle().copy(color = BosqueProfundo)
                    Text(
                        buildAnnotatedString {
                            withStyle(cifra) { append(formatoPrecio(precio)) }
                            append(" la hora")
                        },
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text("Aún no has fijado tu tarifa: los estudiantes verán «A convenir».", color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
                }
                if (datos.isNotEmpty()) {
                    Text(datos, color = TextoPrimario, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = EspacioMedio))
                }
                if (usuario.bio.isNotBlank()) {
                    Text(
                        usuario.bio,
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = EspacioFino)
                    )
                }
            }
        }

        SeccionMaterias("Enseñas", usuario.materias, accion = "Editar", alAccion = alEditarMaterias, accionVacia = "Agregar")

        Seccion(
            "Lo que dicen de ti",
            accion = when (carga) { Carga.LISTO -> "Ver todas"; Carga.VACIO -> "Ver tutorías"; else -> null },
            alAccion = if (carga == Carga.VACIO) alVerHistorial else alVerTodas
        ) {
            when (carga) {
                Carga.CARGANDO -> EsqueletoLista(filas = 2, conMargen = false)
                Carga.ERROR -> TarjetaSeccion { ErrorDeLista(titulo = "No pudimos cargar tus reseñas", alReintentar = alReintentar) }
                Carga.VACIO -> TarjetaSeccion {
                    Text(
                        "Aún sin reseñas. Aparecerán aquí cuando los estudiantes califiquen tus sesiones.",
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Carga.LISTO -> ListaResenas(resenas.take(2))
            }
        }
    }
}

/** Materias como píldoras en su tarjeta; si no hay, texto corto y la acción [accionVacia] en el encabezado. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionMaterias(
    titulo: String,
    materias: List<String>,
    modifier: Modifier = Modifier,
    accion: String? = null,
    alAccion: () -> Unit = {},
    accionVacia: String? = null
) {
    Column(modifier) {
        Seccion(titulo, accion = if (materias.isEmpty()) accionVacia else accion, alAccion = alAccion) {
            TarjetaSeccion {
                if (materias.isEmpty()) {
                    Text(
                        "Aún no publicas materias: elige las que puedes enseñar.",
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(EspacioCorto),
                        verticalArrangement = Arrangement.spacedBy(EspacioCorto)
                    ) {
                        materias.forEach {
                            Text(
                                it,
                                color = BosqueProfundo,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .background(TinteBosque, FormaBoton)
                                    .padding(horizontal = EspacioNormal, vertical = EspacioAjustado)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Modo estudiante: recordatorio de sesiones terminadas sin calificar (RF9). */
@Composable
private fun AvisoPorCalificar(cantidad: Int, alCalificar: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(TinteBosque, FormaOrganica)
            .border(1.dp, BordeAcento, FormaOrganica)
            .clip(FormaOrganica)
            .clickable(role = Role.Button, onClickLabel = "Calificar sesiones", onClick = alCalificar)
            .padding(start = EspacioNormal, end = EspacioMedio, top = EspacioCercano, bottom = EspacioCercano)
    ) {
        Icon(IconosNexo.Estrella, contentDescription = null, tint = MielFuerte, modifier = Modifier.size(IconoMediano))
        Spacer(Modifier.width(EspacioMedio))
        Column(Modifier.weight(1f)) {
            Text(
                plural(cantidad, "1 sesión por calificar", "$cantidad sesiones por calificar"),
                color = BosqueProfundo,
                style = MaterialTheme.typography.titleSmall
            )
            Text("Tu opinión ayuda a otros a elegir tutor.", color = TextoSobreTinte, style = MaterialTheme.typography.bodySmall)
        }
        BotonIr()
    }
}

/** Solo aparece si hay algo que contar: la pausa manda y el contador la acompaña. */
@Composable
private fun AvisoPenalizaciones(resumen: ResumenActividad, esTutor: Boolean, modifier: Modifier = Modifier) {
    val hasta = resumen.suspendidaHasta
    if (hasta == null && resumen.penalizaciones == 0) return
    Column(
        modifier
            .fillMaxWidth()
            .background(TinteBosque, FormaOrganica)
            .border(1.dp, BordeRojoPeligro, FormaOrganica)
            .padding(horizontal = EspacioNormal, vertical = EspacioCercano)
    ) {
        if (hasta != null) {
            Text(
                "Tu cuenta está en pausa hasta el ${diaYMes(hasta.toLocalDate())}, ${formatoHora(hasta.toLocalTime())}",
                color = BosqueProfundo,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                (if (esTutor) "No apareces en la búsqueda ni puedes aceptar solicitudes." else "No puedes enviar solicitudes.") +
                    " Tus sesiones agendadas siguen en pie. ¿Fue un error? Pide revisión en " + (if (esTutor) "Tutorías dadas." else "Tus tutorías."),
                color = TextoSobreTinte,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (resumen.penalizaciones > 0) {
            Text(
                textoDePenalizacionesRecientes(resumen.penalizaciones) + if (esTutor) ", como tutor" else ", como estudiante",
                color = if (hasta == null) BosqueProfundo else TextoSobreTinte,
                style = if (hasta == null) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun Menu(
    esTutor: Boolean,
    alAbrirHistorial: () -> Unit,
    alSalir: () -> Unit,
    alEliminar: () -> Unit,
    alVerCondiciones: () -> Unit,
    alVerPerfilPublico: () -> Unit
) {
    val enlaces = LocalUriHandler.current
    Column(Modifier.padding(horizontal = EspacioMargen), verticalArrangement = Arrangement.spacedBy(EspacioSuelto)) {
        TarjetaFilas {
            if (esTutor) FilaMenu(IconosNexo.Usuario, "Ver mi perfil público", alVerPerfilPublico)
            FilaMenu(IconosNexo.Historial, if (esTutor) "Tutorías dadas" else "Tus tutorías", alAbrirHistorial)
        }
        Column(verticalArrangement = Arrangement.spacedBy(EspacioCorto)) {
            RotuloGrupo("Cuenta", Modifier.padding(start = EspacioNormal).semantics { heading() })
            TarjetaFilas {
                FilaMenu(IconosNexo.Documento, "Condiciones de uso", alVerCondiciones)
                FilaMenu(IconosNexo.Candado, "Política de privacidad", { enlaces.openUri(URL_PRIVACIDAD) })
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            BotonTexto("Cerrar sesión", alSalir, color = TextoSecundario)
            BotonTexto("Eliminar mi cuenta", alEliminar, color = TextoTercero)
        }
    }
}

/** Tarjeta sin relleno propio para filas que ya traen el suyo. */
@Composable
private fun TarjetaFilas(contenido: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().superficie(FormaOrganica)) { contenido() }
}

/**
 * Pide la contraseña, o la cuenta de Google si no tiene: es lo único que distingue a la persona de quien encuentre
 * el teléfono desbloqueado.
 */
@Composable
private fun HojaEliminarCuenta(
    conContrasena: Boolean,
    eliminando: Boolean,
    errorContrasena: Boolean,
    alCambiarContrasena: () -> Unit,
    alCerrar: () -> Unit,
    alConfirmar: (String) -> Unit,
    alConfirmarConGoogle: () -> Unit
) {
    var contrasena by remember { mutableStateOf("") }
    HojaNexo(
        titulo = "Eliminar tu cuenta",
        subtitulo = "Se borran tu nombre, tu correo y tu perfil, y no se puede deshacer. Las tutorías pasadas, " +
            "las reseñas y los mensajes se conservan sin tu nombre, porque también son de la otra persona.",
        alCerrar = alCerrar
    ) {
        if (conContrasena) {
            Spacer(Modifier.height(EspacioNormal))
            Campo(
                valor = contrasena,
                alCambiar = { contrasena = it; alCambiarContrasena() },
                marcador = "Tu contraseña",
                oculto = true,
                teclado = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            if (errorContrasena) {
                Text(
                    "La contraseña no es correcta.",
                    color = Rojo,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = EspacioMinimo)
                )
            }
        }
        Spacer(Modifier.height(EspacioHolgado))
        BotonSecundario(
            texto = if (conContrasena) "Eliminar mi cuenta" else "Confirmar con Google y eliminar",
            peligro = true,
            cargando = eliminando,
            habilitado = !eliminando && (!conContrasena || contrasena.isNotEmpty()),
            alClicar = { if (conContrasena) alConfirmar(contrasena) else alConfirmarConGoogle() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Modo tutor: acceso directo a la disponibilidad, lo que más usa quien enseña. */
@Composable
private fun TarjetaDisponibilidad(alAbrir: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .superficie(FormaOrganica)
            .clickable(role = Role.Button, onClick = alAbrir)
            .padding(start = EspacioNormal, end = EspacioMedio, top = EspacioCercano, bottom = EspacioCercano)
    ) {
        Icon(IconosNexo.Calendario, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(IconoMediano))
        Spacer(Modifier.width(EspacioMedio))
        Column(Modifier.weight(1f)) {
            Text("Mi disponibilidad", color = BosqueProfundo, style = MaterialTheme.typography.titleSmall)
            Text("Define cuándo puedes dar tutorías.", color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
        }
        BotonIr()
    }
}

/**
 * Control segmentado Estudiante / Tutor con la forma del buscador del Inicio: el indicador
 * verde bosque se desliza con un resorte entre las dos opciones.
 */
@Composable
private fun SelectorModo(actual: Rol, alElegir: (Rol) -> Unit, modifier: Modifier = Modifier) {
    val opciones = Rol.entries
    val indice = opciones.indexOf(actual).coerceAtLeast(0)
    val forma = FormaCampoAlto
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(ElevacionTarjeta, forma, ambientColor = SombraAmbiente, spotColor = SombraPuntual)
            .superficie(forma)
            .padding(EspacioFino)
    ) {
        val anchoOpcion = maxWidth / opciones.size
        val desplazamiento by animateDpAsState(
            anchoOpcion * indice,
            resorteIndicador(),
            label = "indicadorModo"
        )
        Box(
            Modifier
                .offset(x = desplazamiento)
                .width(anchoOpcion)
                .fillMaxHeight()
                .background(pincelBosque, FormaBoton)
        )
        Row(Modifier.fillMaxSize().selectableGroup()) {
            opciones.forEach { rol ->
                val activo = rol == actual
                val color by animateColorAsState(if (activo) SobreOscuro else TextoSecundario, especColor(), label = "textoModo")
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = activo,
                            role = Role.RadioButton,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { alElegir(rol) }
                        )
                ) {
                    Text(rol.etiqueta, color = color, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun FilaMenu(icono: ImageVector, titulo: String, alClicar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AltoFilaMenu)
            .clickable(role = Role.Button, onClick = alClicar)
            .padding(horizontal = EspacioNormal, vertical = EspacioAjustado)
    ) {
        Icon(icono, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(IconoMediano))
        Spacer(Modifier.width(EspacioNormal))
        Text(
            titulo,
            color = TextoPrimario,
            style = MaterialTheme.typography.titleMedium.merge(EnfasisMedio),
            modifier = Modifier.weight(1f)
        )
        Icon(IconosNexo.Caret, contentDescription = null, tint = IconoSecundario, modifier = Modifier.size(IconoNormal))
    }
}
