package com.grupo3.nexo.ui.perfil

import com.grupo3.nexo.ui.theme.AreaTactilMinima
import com.grupo3.nexo.ui.components.plural
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.ui.components.BotonTexto
import com.grupo3.nexo.ui.components.CabeceraBosque
import com.grupo3.nexo.ui.components.Carga
import com.grupo3.nexo.ui.components.DialogoConfirmacion
import com.grupo3.nexo.ui.components.EncabezadoSeccion
import com.grupo3.nexo.ui.components.ErrorDeLista
import com.grupo3.nexo.ui.components.Esqueleto
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.Hueso
import com.grupo3.nexo.ui.components.IconosClarosEnBarraDeEstado
import com.grupo3.nexo.ui.components.IconosNexo
import com.grupo3.nexo.ui.components.LineaHueso
import com.grupo3.nexo.ui.components.ListaResenas
import com.grupo3.nexo.ui.components.Onda
import com.grupo3.nexo.ui.components.VeloBarraEstado
import com.grupo3.nexo.ui.components.aparicion
import com.grupo3.nexo.ui.components.cargaDe
import com.grupo3.nexo.ui.components.enumerar
import com.grupo3.nexo.ui.components.formatoHoras
import com.grupo3.nexo.ui.components.formatoValoracion
import com.grupo3.nexo.ui.components.rememberPrimeraVez
import com.grupo3.nexo.ui.components.solaparHaciaArriba
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.theme.BordeAcento
import com.grupo3.nexo.ui.theme.BordeRojoSalir
import com.grupo3.nexo.ui.theme.BordeSutil
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.EnfasisMedio
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioAmplio
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioExtenso
import com.grupo3.nexo.ui.theme.EspacioFino
import com.grupo3.nexo.ui.theme.EspacioMargen
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
import com.grupo3.nexo.ui.theme.RojoSuave
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
                            BloqueTutor(usuario, estado.resenas, cargando = estado.resumen == null, error = estado.error, alReintentar = viewModel::reintentar, alVerTodas = alAbrirHistorial, alPublicarHorario = alAbrirDisponibilidad)
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
                                Materias("Has estudiado", it, Modifier.padding(horizontal = EspacioMargen))
                            }
                        }
                        Spacer(Modifier.height(EspacioAmplio))
                        Menu(esTutor = esTutor, alAbrirHistorial = alAbrirHistorial, alSalir = { confirmarSalida = true })
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
            .size(36.dp)
            .background(BosqueMedio, CircleShape)
    ) {
        Icon(IconosNexo.Flecha, contentDescription = null, tint = SobreOscuro, modifier = Modifier.size(IconoCuerpo))
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
                Text(
                    usuario.nombre,
                    color = SobreOscuro,
                    style = MaterialTheme.typography.displaySmall,
                    maxLines = 2,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(EspacioMicro))
                // Con letra grande no caben en una línea: el correo baja a la suya en vez de cortarse
                val apilado = LocalDensity.current.fontScale > 1.3f
                Text(
                    if (apilado) "${usuario.handle}\n${usuario.correo}" else "${usuario.handle} · ${usuario.correo}",
                    color = SobreOscuroSuave,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (apilado) 3 else 2,
                    overflow = TextOverflow.Ellipsis
                )
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

/** Modo tutor: verificación, materias que enseña y las últimas reseñas recibidas. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BloqueTutor(
    usuario: Usuario,
    resenas: List<Resena>,
    cargando: Boolean,
    error: Boolean,
    alReintentar: () -> Unit,
    alVerTodas: () -> Unit,
    alPublicarHorario: () -> Unit
) {
    val carga = cargaDe(cargando, resenas.isEmpty(), error)
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
            Carga.CARGANDO -> EsqueletoSinMargen(filas = 2)
            Carga.ERROR -> ErrorDeLista(titulo = "No pudimos cargar tus reseñas", alReintentar = alReintentar)
            Carga.VACIO -> EstadoVacio(
                icono = IconosNexo.Resena,
                titulo = "Aún sin reseñas",
                mensaje = "Aparecerán aquí cuando los estudiantes califiquen tus sesiones.",
                accion = "Ver mi historial",
                alAccion = alVerTodas
            )
            Carga.LISTO -> ListaResenas(resenas.take(2))
        }
    }
}

/** [EsqueletoLista] ya trae margen lateral; dentro de un bloque con margen propio se pintan los huesos sin él. */
@Composable
private fun EsqueletoSinMargen(filas: Int) {
    Esqueleto(
        Modifier.fillMaxWidth().padding(vertical = EspacioNormal),
        verticalArrangement = Arrangement.spacedBy(EspacioMedio)
    ) {
        repeat(filas) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Superficie, FormaOrganica)
                    .border(1.dp, BordeSutil, FormaOrganica)
                    .padding(EspacioNormal)
            ) {
                Hueso(Modifier.size(44.dp), CircleShape)
                Spacer(Modifier.width(EspacioMedio))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(EspacioCorto)) {
                    LineaHueso(0.55f, alto = 14.dp)
                    LineaHueso(0.85f, alto = 10.dp)
                    LineaHueso(0.4f, alto = 10.dp)
                }
            }
        }
    }
}

/** Materias como texto corrido, igual que en el perfil público: se leen de un vistazo y no recargan la pantalla. */
@Composable
private fun Materias(titulo: String, materias: List<String>, modifier: Modifier = Modifier) {
    Column(modifier) {
        EncabezadoSeccion(titulo)
        Text(enumerar(materias), color = TextoPrimario, style = MaterialTheme.typography.bodyLarge)
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

@Composable
private fun Menu(esTutor: Boolean, alAbrirHistorial: () -> Unit, alSalir: () -> Unit) {
    // Tarjeta con los mismos márgenes que el resto del perfil, como la de reseñas
    Column(
        Modifier
            .padding(horizontal = EspacioMargen)
            .fillMaxWidth()
            .superficie(FormaOrganica)
    ) {
        FilaMenu(IconosNexo.Historial, if (esTutor) "Tutorías dadas" else "Tus tutorías", alAbrirHistorial)
    }
    // Cerrar sesión es raro: píldora de borde fino, como "Continuar con Google" del acceso
    val rojo = RojoSuave
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(start = EspacioMargen, end = EspacioMargen, top = EspacioCercano)
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(CircleShape)
            .border(1.dp, BordeRojoSalir, CircleShape)
            .clickable(role = Role.Button, onClick = alSalir)
    ) {
        Icon(IconosNexo.Salir, contentDescription = null, tint = rojo, modifier = Modifier.size(IconoNormal))
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
            .heightIn(min = 56.dp)
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
