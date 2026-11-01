package com.grupo3.nexo.ui.perfil

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.R
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.ResumenActividad
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.usecase.CambiarModoUseCase
import com.grupo3.nexo.domain.usecase.CerrarSesionUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMisResenasUseCase
import com.grupo3.nexo.domain.usecase.ObtenerResumenActividadUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import com.grupo3.nexo.ui.components.BotonTexto
import com.grupo3.nexo.ui.components.Carga
import com.grupo3.nexo.ui.components.EsqueletoLista
import com.grupo3.nexo.ui.components.ErrorDeLista
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.cargaDe
import com.grupo3.nexo.ui.theme.cambioContenido
import com.grupo3.nexo.ui.theme.resorteIndicador
import com.grupo3.nexo.ui.components.Avatar
import com.grupo3.nexo.ui.components.CabeceraBosque
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.ListaResenas
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.components.DialogoConfirmacion
import com.grupo3.nexo.ui.components.EncabezadoSeccion
import com.grupo3.nexo.ui.components.Estrellas
import com.grupo3.nexo.ui.components.IconosClarosEnBarraDeEstado
import com.grupo3.nexo.ui.components.Onda
import com.grupo3.nexo.ui.components.TextoLinea
import com.grupo3.nexo.ui.components.VeloBarraEstado
import com.grupo3.nexo.ui.components.aparicion
import com.grupo3.nexo.ui.components.formatoHoras
import com.grupo3.nexo.ui.components.formatoValoracion
import com.grupo3.nexo.ui.components.rememberPrimeraVez
import com.grupo3.nexo.ui.components.solaparHaciaArriba
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.components.universidadCorta
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.Fondo
import com.grupo3.nexo.ui.theme.Miel
import com.grupo3.nexo.ui.theme.Rojo
import com.grupo3.nexo.ui.theme.Salvia
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.TinteBosque
import com.grupo3.nexo.ui.theme.pincelBosque
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioAmplio
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioExtenso
import com.grupo3.nexo.ui.theme.EspacioFino
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioMicro
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.EspacioSuelto
import com.grupo3.nexo.ui.theme.FormaBoton
import com.grupo3.nexo.ui.theme.FormaCampoAlto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.grupo3.nexo.ui.components.IconosNexo

/* ==================== ViewModel ==================== */

@HiltViewModel
class PerfilViewModel @Inject constructor(
    obtenerUsuario: ObtenerUsuarioUseCase,
    obtenerResumen: ObtenerResumenActividadUseCase,
    obtenerMisResenas: ObtenerMisResenasUseCase,
    private val cambiarModo: CambiarModoUseCase,
    private val cerrarSesion: CerrarSesionUseCase
) : ViewModel() {

    data class Estado(
        val usuario: Usuario,
        /** null mientras llegan las sesiones: las cifras muestran guiones en vez de ceros falsos. */
        val resumen: ResumenActividad? = null,
        val resenas: List<Resena> = emptyList()
    ) {
        val esTutor get() = usuario.rol == Rol.TUTOR
    }

    private val usuario = obtenerUsuario()

    val estado = combine(usuario, obtenerResumen(), obtenerMisResenas()) { u, resumen, resenas ->
        Estado(u, resumen, resenas)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado(usuario.value))

    fun elegirModo(rol: Rol) = cambiarModo(rol)
    fun salir() = cerrarSesion()
}

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
    viewModel: PerfilViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsState()
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val altoPantalla = maxHeight
        // Con poco contenido la foto crece hasta llenar la pantalla, en vez de dejar un vacío blanco.
        CabeceraAjustable(
            altoPantalla = altoPantalla,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(desplazamiento),
            cabecera = { Cabecera(usuario, iniciado) }
        ) {
            Column(Modifier.padding(bottom = EspacioExtenso)) {
                // El selector ocupa el lugar del buscador: montado sobre la onda, con la única sombra
                SelectorModo(
                    actual = usuario.rol,
                    alElegir = viewModel::elegirModo,
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
                        // Lo accionable va primero; las cifras y materias quedan como contexto
                        if (esTutor) {
                            Column(Modifier.padding(start = EspacioMargen, end = EspacioMargen, top = EspacioSuelto)) {
                                TarjetaDisponibilidad(alAbrirDisponibilidad)
                            }
                            Cifras(estado.resumen, true, Modifier.padding(horizontal = EspacioMargen, vertical = EspacioSuelto))
                            BloqueTutor(usuario, estado.resenas, cargando = estado.resumen == null, alVerTodas = alAbrirHistorial, alPublicarHorario = alAbrirDisponibilidad)
                        } else {
                            val pendientes = estado.resumen?.porCalificar ?: 0
                            if (pendientes > 0) {
                                AvisoPorCalificar(pendientes, alAbrirHistorial, Modifier.padding(start = EspacioMargen, end = EspacioMargen, top = EspacioSuelto))
                            }
                            Cifras(estado.resumen, false, Modifier.padding(horizontal = EspacioMargen, vertical = EspacioSuelto))
                            estado.resumen?.materias?.takeIf { it.isNotEmpty() }?.let {
                                Materias("Has estudiado", it, Modifier.padding(horizontal = EspacioMargen))
                            }
                        }
                        Spacer(Modifier.height(EspacioAmplio))
                        Menu(alAbrirHistorial = alAbrirHistorial, alSalir = { confirmarSalida = true })
                    }
                }
            }
        }
        VeloBarraEstado(visible = !cabeceraVisible)
    }

    if (confirmarSalida) {
        DialogoConfirmacion(
            titulo = "¿Cerrar sesión?",
            mensaje = "Tendrás que volver a ingresar tu correo y contraseña.",
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
 * Pone la [cabecera] sobre el [contenido] y le da el alto que sobra en pantalla (entre 270 y 430 dp),
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
        val altoCabecera = (altoPantalla.roundToPx() - resto.height)
            .coerceIn(270.dp.roundToPx(), 430.dp.roundToPx())
        val cab = medCabecera.first().measure(Constraints.fixed(ancho, altoCabecera))
        layout(ancho, altoCabecera + resto.height) {
            cab.place(0, 0)
            resto.place(0, altoCabecera)
        }
    }
}

/* ==================== Piezas ==================== */

/** Esquinas amplias como la tarjeta de la próxima sesión del Inicio y los campos del acceso. */

/** Botón circular verde bosque con flecha, el mismo "ir" del buscador del Inicio. */
@Composable
private fun BotonIr(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .background(BosqueMedio, CircleShape)
    ) {
        Icon(painterResource(R.drawable.ph_flecha), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

/** Misma cabecera que el Inicio: foto, nombre grande como el saludo y la onda del lienzo. */
@Composable
private fun Cabecera(usuario: Usuario, iniciado: Boolean) {
    Box(Modifier.fillMaxSize()) {
        CabeceraBosque(conFoto = true, alturaOnda = 58.dp, modifier = Modifier.matchParentSize()) {
            // La cabecera compartida envuelve su contenido; esta columna lo estira para bajar el nombre
            Column(Modifier.fillMaxHeight()) {
            // Sin avatar: en el perfil propio repetiría el nombre que ya se lee en grande
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = EspacioAmplio, end = EspacioNormal, top = EspacioCorto)
                    .height(52.dp)
            ) {
                Text(
                    "Perfil",
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.weight(1f).heightIn(min = 40.dp))
            Column(
                Modifier
                    .padding(start = EspacioAmplio, end = EspacioAmplio, bottom = EspacioMargen)
                    .aparicion(0, iniciado)
            ) {
                Text(
                    usuario.nombre,
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    maxLines = 2,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(EspacioMicro))
                val detalle = listOf(usuario.carrera, universidadCorta(usuario.universidad)).filter { it.isNotBlank() }
                Text(
                    detalle.joinToString(" · ").ifBlank { usuario.correo },
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            }
        }
        Onda(
            Fondo,
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(48.dp)
        )
    }
}

/**
 * Tres cifras sin contenedor, separadas por líneas finas, con la tipografía de la hora del Inicio.
 * Estudiante: sesiones, horas, por calificar. Tutor: promedio recibido, sesiones dadas, estudiantes.
 */
@Composable
private fun Cifras(resumen: ResumenActividad?, esTutor: Boolean, modifier: Modifier = Modifier) {
    val cifras: List<Triple<String, String, Boolean>> = when {
        resumen == null -> listOf(Triple("–", "sesiones", false), Triple("–", "horas", false), Triple("–", "", false))
        esTutor -> listOf(
            Triple(resumen.promedioRecibido?.let(::formatoValoracion) ?: "–", "calificación", resumen.promedioRecibido != null),
            Triple("${resumen.sesiones}", if (resumen.sesiones == 1) "sesión dada" else "sesiones dadas", false),
            Triple("${resumen.personas}", if (resumen.personas == 1) "estudiante" else "estudiantes", false)
        )
        else -> listOf(
            Triple("${resumen.sesiones}", if (resumen.sesiones == 1) "sesión" else "sesiones", false),
            Triple(formatoHoras(resumen.horas), "de estudio", false),
            Triple("${resumen.personas}", if (resumen.personas == 1) "tutor" else "tutores", false)
        )
    }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        valor,
                        color = BosqueProfundo,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp).merge(CifrasTabulares)
                    )
                    if (conEstrella) {
                        Spacer(Modifier.width(3.dp))
                        Icon(IconosNexo.Estrella, contentDescription = null, tint = Miel, modifier = Modifier.size(18.dp))
                    }
                }
                Text(etiqueta, color = TextoTercero, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Modo tutor: verificación, materias que enseña y las últimas reseñas recibidas. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BloqueTutor(
    usuario: Usuario,
    resenas: List<Resena>,
    cargando: Boolean,
    alVerTodas: () -> Unit,
    alPublicarHorario: () -> Unit
) {
    val carga = cargaDe(cargando, resenas.isEmpty())
    Column(Modifier.padding(horizontal = EspacioMargen)) {
        if (usuario.materias.isNotEmpty()) {
            Materias("Enseñas", usuario.materias)
        } else {
            EncabezadoSeccion("Enseñas")
            Text("Aún no tienes materias publicadas.", color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
            BotonTexto("Publicar mi horario", alPublicarHorario)
        }
        if (usuario.bio.isNotBlank()) {
            Text(
                usuario.bio,
                color = TextoSecundario,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = EspacioMedio)
            )
        }

        EncabezadoSeccion(
            "Lo que dicen de ti",
            Modifier.padding(top = EspacioAjustado),
            accion = if (carga == Carga.LISTO) "Ver todas" else null,
            alAccion = alVerTodas
        )
        when (carga) {
            Carga.CARGANDO -> EsqueletoLista(filas = 2)
            Carga.ERROR -> ErrorDeLista(titulo = "No pudimos cargar tus reseñas")
            Carga.VACIO -> EstadoVacio(
                icono = IconosNexo.Resena,
                titulo = "Aún sin reseñas",
                mensaje = "Aparecerán aquí cuando los estudiantes califiquen tus tutorías.",
                accion = "Ver mi historial",
                alAccion = alVerTodas
            )
            Carga.LISTO -> ListaResenas(resenas.take(2))
        }
    }
}

/** Materias como texto con borde fino: píldoras sí, pero sin color de relleno ni más peso que los títulos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Materias(titulo: String, materias: List<String>, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            titulo,
            color = TextoPrimario,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(bottom = EspacioMedio)
                .semantics { heading() }
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(EspacioCorto),
            verticalArrangement = Arrangement.spacedBy(EspacioCorto)
        ) {
            materias.forEach { materia ->
                Text(
                    materia,
                    color = BosqueProfundo,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier
                        .border(1.dp, BosqueAcento.copy(alpha = 0.22f), CircleShape)
                        .padding(horizontal = EspacioMedio, vertical = 5.dp)
                )
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
            .border(1.dp, BosqueAcento.copy(alpha = 0.14f), FormaOrganica)
            .clip(FormaOrganica)
            .clickable(role = Role.Button, onClickLabel = "Calificar sesiones", onClick = alCalificar)
            .padding(start = EspacioNormal, end = EspacioMedio, top = EspacioCercano, bottom = EspacioCercano)
    ) {
        Icon(IconosNexo.Estrella, contentDescription = null, tint = Miel, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(EspacioMedio))
        Column(Modifier.weight(1f)) {
            Text(
                if (cantidad == 1) "1 sesión por calificar" else "$cantidad sesiones por calificar",
                color = BosqueProfundo,
                style = MaterialTheme.typography.titleSmall
            )
            Text("Tu opinión ayuda a otros a elegir tutor.", color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
        }
        BotonIr()
    }
}

@Composable
private fun Menu(alAbrirHistorial: () -> Unit, alSalir: () -> Unit) {
    // Tarjeta con los mismos márgenes que el resto del perfil, como la de reseñas
    Column(
        Modifier
            .padding(horizontal = EspacioMargen)
            .fillMaxWidth()
            .superficie(FormaOrganica)
    ) {
        FilaMenu(painterResource(R.drawable.ph_historial), "Historial y calificaciones", alAbrirHistorial)
    }
    // Cerrar sesión es raro: píldora de borde fino, como "Continuar con Google" del acceso
    val rojo = Rojo.copy(alpha = 0.85f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(start = EspacioMargen, end = EspacioMargen, top = EspacioCercano)
            .fillMaxWidth()
            .height(50.dp)
            .clip(CircleShape)
            .border(1.dp, rojo.copy(alpha = 0.28f), CircleShape)
            .clickable(role = Role.Button, onClick = alSalir)
    ) {
        Icon(painterResource(R.drawable.ph_salir), contentDescription = null, tint = rojo, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(EspacioCorto))
        Text("Cerrar sesión", color = rojo, style = MaterialTheme.typography.titleSmall)
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
        Icon(painterResource(R.drawable.ph_calendario), contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(EspacioMedio))
        Column(Modifier.weight(1f)) {
            Text("Mi disponibilidad horaria", color = BosqueProfundo, style = MaterialTheme.typography.titleSmall)
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
            .shadow(14.dp, forma, ambientColor = BosqueProfundo.copy(alpha = 0.22f), spotColor = BosqueProfundo.copy(alpha = 0.38f))
            .superficie(forma)
            .padding(7.dp)
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
                val color by animateColorAsState(if (activo) Color.White else TextoSecundario, label = "textoModo")
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
private fun FilaMenu(
    icono: Painter,
    titulo: String,
    alClicar: () -> Unit,
    peligro: Boolean = false,
    habilitada: Boolean = true,
    detalle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = habilitada, role = Role.Button, onClick = alClicar)
            .padding(horizontal = EspacioNormal, vertical = EspacioAjustado)
            .alpha(if (habilitada) 1f else 0.5f)
    ) {
        // Rojo atenuado: avisa sin gritar en una pantalla sobria
        val colorPeligro = Rojo.copy(alpha = 0.85f)
        Icon(icono, contentDescription = null, tint = if (peligro) colorPeligro else BosqueAcento, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(EspacioNormal))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                color = if (peligro) colorPeligro else TextoPrimario,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
            )
            detalle?.let { Text(it, color = TextoTercero, style = MaterialTheme.typography.bodySmall) }
        }
        if (!peligro && habilitada) {
            Icon(painterResource(R.drawable.ph_caret), contentDescription = null, tint = Salvia.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SeparadorMenu() {
    // Sangrado hasta el texto (16 de margen + 22 de ícono + 16 de espacio)
    Box(Modifier.fillMaxWidth().padding(start = 54.dp).height(1.dp).background(Divisor))
}
