package com.grupo3.nexo.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo3.nexo.domain.model.Chat
import com.grupo3.nexo.domain.model.EstadoSesion
import com.grupo3.nexo.domain.model.Mensaje
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.ui.components.Avatar
import com.grupo3.nexo.ui.components.AvisoNexo
import com.grupo3.nexo.ui.components.BotonIcono
import com.grupo3.nexo.ui.components.BotonSecundario
import com.grupo3.nexo.ui.components.CabeceraSeccion
import com.grupo3.nexo.ui.components.Campo
import com.grupo3.nexo.ui.components.Carga
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.ErrorDeLista
import com.grupo3.nexo.ui.components.Esqueleto
import com.grupo3.nexo.ui.components.EsqueletoLista
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.HojaCalificacion
import com.grupo3.nexo.ui.components.Hueso
import com.grupo3.nexo.ui.components.IconosNexo
import com.grupo3.nexo.ui.components.InsigniaConteo
import com.grupo3.nexo.ui.components.TextoLinea
import com.grupo3.nexo.ui.components.areaTactilSinCambiarMedida
import com.grupo3.nexo.ui.components.avisoFallo
import com.grupo3.nexo.ui.components.cargaDe
import com.grupo3.nexo.ui.components.filaAgrupada
import com.grupo3.nexo.ui.components.formatoDia
import com.grupo3.nexo.ui.components.formatoFecha
import com.grupo3.nexo.ui.components.formatoHora
import com.grupo3.nexo.ui.components.formatoMomento
import com.grupo3.nexo.ui.components.mostrarError
import com.grupo3.nexo.ui.components.motivoDeCierre
import com.grupo3.nexo.ui.components.plural
import com.grupo3.nexo.ui.components.pulsable
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.components.textoDePenalizacion
import com.grupo3.nexo.ui.theme.AreaTactilMinima
import com.grupo3.nexo.ui.theme.BosqueMedio
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.EnfasisMedio
import com.grupo3.nexo.ui.theme.EnfasisNormal
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioAmplio
import com.grupo3.nexo.ui.theme.EspacioCercano
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioFino
import com.grupo3.nexo.ui.theme.EspacioIcono
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioMinimo
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.EspacioRozante
import com.grupo3.nexo.ui.theme.Fondo
import com.grupo3.nexo.ui.theme.FormaControl
import com.grupo3.nexo.ui.theme.FormaOpcion
import com.grupo3.nexo.ui.theme.Icono14
import com.grupo3.nexo.ui.theme.Icono18
import com.grupo3.nexo.ui.theme.Icono20
import com.grupo3.nexo.ui.theme.LocalAnimacionesReducidas
import com.grupo3.nexo.ui.theme.Menta
import com.grupo3.nexo.ui.theme.MielFuerte
import com.grupo3.nexo.ui.theme.RadioBurbuja
import com.grupo3.nexo.ui.theme.RadioBurbujaCola
import com.grupo3.nexo.ui.theme.RellenoTenue
import com.grupo3.nexo.ui.theme.SangriaFilaAvatar
import com.grupo3.nexo.ui.theme.SobreOscuro
import com.grupo3.nexo.ui.theme.Superficie
import com.grupo3.nexo.ui.theme.TamanoAvatarCabecera
import com.grupo3.nexo.ui.theme.TamanoAvatarLista
import com.grupo3.nexo.ui.theme.TamanoBotonEnviar
import com.grupo3.nexo.ui.theme.TamanoPuntoEstado
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.VerdeEstado
import com.grupo3.nexo.ui.theme.cambioContenido
import com.grupo3.nexo.ui.theme.resorteRebote
import java.time.LocalDate
import kotlinx.coroutines.launch



/* ==================== Bandeja de chats ==================== */

@Composable
fun PantallaChats(
    alAbrirChat: (String) -> Unit,
    alBuscar: () -> Unit,
    alVerSolicitudes: () -> Unit,
    viewModel: ChatsViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val sinLeer = remember(estado.chats) { estado.chats.sumOf { it.noLeidos } }

    Column(Modifier.fillMaxSize()) {
        CabeceraSeccion(
            titulo = "Chats",
            subtitulo = when (sinLeer) {
                0 -> "Coordina los detalles de tus sesiones"
                1 -> "1 mensaje sin leer"
                else -> "$sinLeer mensajes sin leer"
            }
        )

        AnimatedContent(
            targetState = cargaDe(estado.cargando, estado.chats.isEmpty(), estado.error),
            transitionSpec = { cambioContenido() },
            label = "chats",
            modifier = Modifier.weight(1f)
        ) { carga ->
            if (carga == Carga.CARGANDO) {
                EsqueletoLista()
            } else if (carga == Carga.ERROR) {
                ErrorDeLista(titulo = "No pudimos cargar tus chats", alReintentar = viewModel::reintentar)
            } else if (carga == Carga.VACIO) {
                if (estado.esTutor) {
                    EstadoVacio(
                        icono = IconosNexo.Chat,
                        titulo = "Aún no tienes chats",
                        mensaje = "El chat con un estudiante se habilita cuando aceptas su solicitud de tutoría.",
                        accion = "Ver solicitudes",
                        alAccion = alVerSolicitudes
                    )
                } else {
                    EstadoVacio(
                        icono = IconosNexo.Chat,
                        titulo = "Aún no tienes chats",
                        mensaje = "El chat con un tutor se habilita cuando acepta tu solicitud de tutoría.",
                        accion = "Buscar tutor",
                        alAccion = alBuscar
                    )
                }
            } else {
            LazyColumn(contentPadding = PaddingValues(start = EspacioMargen, end = EspacioMargen, top = EspacioNormal, bottom = EspacioAmplio)) {
                itemsIndexed(estado.chats, key = { _, c -> c.id }) { i, chat ->
                    FilaChat(
                        chat,
                        alAbrir = { alAbrirChat(chat.id) },
                        modifier = Modifier
                            .animateItem()
                            .filaAgrupada(i, estado.chats.size, sangria = SangriaFilaAvatar)
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun FilaChat(chat: Chat, alAbrir: () -> Unit, modifier: Modifier = Modifier) {
    val sinLeer = chat.noLeidos > 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Abrir chat con ${chat.nombre}", role = Role.Button, onClick = alAbrir)
            .semantics { stateDescription = if (chat.enLinea) "En línea" else "Desconectado" }
            .padding(horizontal = EspacioNormal, vertical = EspacioCercano)
    ) {
        // TalkBack dice primero el nombre y luego si está en línea
        Avatar(chat.nombre, tamano = TamanoAvatarLista, enLinea = chat.enLinea, anunciarEstado = false)
        Spacer(Modifier.width(EspacioNormal))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextoLinea(
                    chat.nombre, MaterialTheme.typography.titleMedium, TextoPrimario,
                    Modifier.weight(1f)
                )
                Spacer(Modifier.width(EspacioCorto))
                Text(
                    chat.hora?.let { formatoMomento(it) }.orEmpty(),
                    color = if (sinLeer) VerdeEstado else TextoTercero,
                    style = MaterialTheme.typography.bodySmall.merge(CifrasTabulares).copy(
                        fontWeight = if (sinLeer) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
            Spacer(Modifier.height(EspacioRozante))
            if (chat.soloLectura) {
                Text("Conversación cerrada · solo lectura", color = TextoTercero, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(1.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextoLinea(
                    chat.ultimoMensaje,
                    MaterialTheme.typography.bodyMedium.merge(if (sinLeer) EnfasisMedio else EnfasisNormal),
                    if (sinLeer) TextoPrimario else TextoSecundario,
                    Modifier.weight(1f)
                )
                if (sinLeer) {
                    Spacer(Modifier.width(EspacioCorto))
                    InsigniaConteo(chat.noLeidos, bordeColor = Superficie)
                }
            }
        }
    }
}

/* ==================== Conversación ==================== */

private val respuestasRapidasTutor = listOf(
    "Hola, ya acepté tu solicitud",
    "¿Qué tema quieres repasar?",
    "¿Tienes algún ejercicio que traer?"
)

private val respuestasRapidas = listOf(
    "Hola, ¿tienes disponibilidad esta semana?",
    "¿Cuánto dura una sesión?",
    "¿Podemos repasar para el parcial?"
)

/** Anchos relativos de las burbujas del esqueleto; alternan izquierda (recibido) y derecha (enviado). */
private val anchosEsqueletoBurbujas = listOf(0.6f, 0.45f, 0.7f, 0.5f)

/** Esqueleto de la conversación: burbujas grises que alternan de lado mientras llegan los mensajes. */
@Composable
private fun EsqueletoBurbujas(modifier: Modifier = Modifier) {
    Esqueleto(
        descripcion = "Cargando mensajes…",
        verticalArrangement = Arrangement.spacedBy(EspacioMedio),
        modifier = modifier.fillMaxWidth().padding(horizontal = EspacioNormal, vertical = EspacioNormal)
    ) {
        anchosEsqueletoBurbujas.forEachIndexed { i, ancho ->
            val mio = i % 2 == 1
            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mio) Arrangement.End else Arrangement.Start) {
                Hueso(
                    Modifier.fillMaxWidth(ancho).height(44.dp),
                    RoundedCornerShape(
                        topStart = RadioBurbuja, topEnd = RadioBurbuja,
                        bottomStart = if (mio) RadioBurbuja else RadioBurbujaCola,
                        bottomEnd = if (mio) RadioBurbujaCola else RadioBurbuja
                    )
                )
            }
        }
    }
}

@Composable
fun PantallaConversacion(
    alVolver: () -> Unit,
    viewModel: ConversacionViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val listaEstado = rememberLazyListState()
    var texto by rememberSaveable { mutableStateOf("") }
    var calificando by rememberSaveable { mutableStateOf(false) }
    var acordando by rememberSaveable { mutableStateOf(false) }

    // Al abrir, el chat ya aparece en el último mensaje; solo se anima lo que llega después
    var yaPosicionado by remember { mutableStateOf(false) }
    // La lista lleva un separador por cada día, además de los mensajes
    val elementos = remember(estado.mensajes) { estado.mensajes.size + estado.mensajes.map { it.hora.toLocalDate() }.distinct().size }
    LaunchedEffect(elementos) {
        if (estado.mensajes.isEmpty()) return@LaunchedEffect
        if (yaPosicionado) {
            listaEstado.animateScrollToItem(elementos - 1)
        } else {
            listaEstado.scrollToItem(elementos - 1)
            yaPosicionado = true
        }
    }

    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    fun enviar(valor: String) {
        viewModel.enviar(valor) { fallo ->
            // Si el mensaje no salió, el texto vuelve a la caja para reintentar (salvo que ya se haya escrito otra cosa)
            if (fallo != null) {
                if (texto.isEmpty()) texto = valor
                alcance.launch { avisos.mostrarError(avisoFallo("enviar el mensaje", fallo)) }
            }
        }
        texto = ""
    }

    Box(Modifier.fillMaxSize().background(Fondo).imePadding()) {
        Column(Modifier.fillMaxSize()) {
            // Cabecera
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Superficie)
                    .drawBehind {
                        drawLine(Divisor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                    }
                    .statusBarsPadding()
                    .padding(start = EspacioCorto, end = EspacioNormal, top = EspacioFino, bottom = EspacioAjustado)
            ) {
                BotonIcono(IconosNexo.Atras, "Volver", alVolver)
                Spacer(Modifier.width(EspacioFino))
                Avatar(estado.nombre.ifBlank { "Nexo" }, tamano = TamanoAvatarCabecera, enLinea = estado.enLinea, anunciarEstado = false)
                Spacer(Modifier.width(EspacioMedio))
                Column(Modifier.weight(1f)) {
                    TextoLinea(estado.nombre, MaterialTheme.typography.titleMedium, TextoPrimario, Modifier.semantics { heading() })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (estado.enLinea) {
                            Box(Modifier.size(TamanoPuntoEstado).background(Menta, CircleShape))
                            Spacer(Modifier.width(EspacioIcono))
                        }
                        Text(
                            if (estado.enLinea) "En línea" else "Desconectado",
                            color = if (estado.enLinea) VerdeEstado else TextoTercero,
                            style = MaterialTheme.typography.bodySmall
                        )
                        estado.sesion?.let { sesion ->
                            Text(
                                " · ${sesion.materia} · ${formatoFecha(sesion.fecha)}",
                                color = TextoTercero,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Mensajes
            val cargaMensajes = cargaDe(estado.cargando, estado.mensajes.isEmpty(), estado.error)
            if (cargaMensajes == Carga.CARGANDO) {
                Box(Modifier.weight(1f).fillMaxWidth()) { EsqueletoBurbujas() }
            } else if (cargaMensajes == Carga.ERROR) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ErrorDeLista(titulo = "No pudimos cargar los mensajes", alReintentar = viewModel::reintentar)
                }
            } else if (estado.mensajes.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EstadoVacio(
                        icono = IconosNexo.Chat,
                        titulo = "Empieza la conversación",
                        mensaje = "Coordina el tema, la fecha y el enlace de la sesión."
                    )
                }
                if (!estado.soloLectura) LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(EspacioCorto),
                    contentPadding = PaddingValues(horizontal = EspacioNormal)
                ) {
                    items(if (estado.esTutor) respuestasRapidasTutor else respuestasRapidas, key = { it }) { sugerencia ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .areaTactilSinCambiarMedida()
                                .heightIn(min = AreaTactilMinima)
                                .pulsable { enviar(sugerencia) }
                        ) {
                        Text(
                            sugerencia,
                            color = BosqueProfundo,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .superficie(FormaOpcion)
                                .padding(horizontal = EspacioCercano, vertical = EspacioAjustado)
                        )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listaEstado,
                    contentPadding = PaddingValues(horizontal = EspacioNormal, vertical = EspacioNormal),
                    modifier = Modifier.weight(1f)
                ) {
                    estado.mensajes.forEachIndexed { indice, mensaje ->
                        val dia = mensaje.hora.toLocalDate()
                        val anterior = estado.mensajes.getOrNull(indice - 1)
                        val siguiente = estado.mensajes.getOrNull(indice + 1)
                        val iniciaDia = anterior?.hora?.toLocalDate() != dia
                        val cierraDia = siguiente?.hora?.toLocalDate() != dia
                        if (iniciaDia) item(key = "dia$dia") { SeparadorDia(dia, Modifier.animateItem()) }
                        item(key = mensaje.id) {
                            Burbuja(
                                mensaje = mensaje,
                                iniciaGrupo = iniciaDia || anterior?.mio != mensaje.mio,
                                cierraGrupo = cierraDia || siguiente?.mio != mensaje.mio,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }

            if (estado.soloLectura) {
                PieSesionTerminada(estado.sesion, alCalificar = { calificando = true })
                return@Column
            }
            // Mientras carga o falla no hay a quién escribir: sin campo de texto
            if (cargaMensajes == Carga.CARGANDO || cargaMensajes == Carga.ERROR) return@Column

            // Quien va tarde lo acuerda aquí, antes de que se venza la hora
            estado.sesion?.takeIf { it.admiteAcuerdo(LocalDate.now()) }?.let { sesion ->
                TiraAcuerdo(
                    sesion = sesion,
                    alAcordar = { acordando = true },
                    alAceptar = { viewModel.aceptar(sesion.id) { it?.let { fallo -> alcance.launch { avisos.mostrarError(avisoFallo("aceptar la propuesta", fallo)) } } } },
                    alRechazar = { viewModel.rechazar(sesion.id) { it?.let { fallo -> alcance.launch { avisos.mostrarError(avisoFallo("responder la propuesta", fallo)) } } } }
                )
            }

            // Entrada de texto
            val puedeEnviar = texto.isNotBlank()
            val escalaEnviar by animateFloatAsState(
                if (puedeEnviar) 1f else 0.9f,
                if (LocalAnimacionesReducidas.current) snap() else resorteRebote(),
                label = "enviar"
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = EspacioNormal, end = EspacioMedio, top = EspacioAjustado, bottom = EspacioAjustado)
            ) {
                Campo(
                    valor = texto,
                    alCambiar = { texto = it },
                    marcador = "Escribe un mensaje",
                    lineasMaximas = 4,
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(EspacioAjustado))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(TamanoBotonEnviar)
                        .graphicsLayer {
                            scaleX = escalaEnviar
                            scaleY = escalaEnviar
                        }
                        .pulsable(habilitado = puedeEnviar) { enviar(texto) }
                        .then(
                            if (puedeEnviar) Modifier.background(BosqueMedio, FormaControl)
                            else Modifier.background(RellenoTenue, FormaControl)
                        )
                        .semantics { contentDescription = "Enviar mensaje" }
                ) {
                    Icon(
                        IconosNexo.Enviar,
                        contentDescription = null,
                        tint = if (puedeEnviar) SobreOscuro else TextoTercero,
                        modifier = Modifier.size(Icono20)
                    )
                }
            }
        }

        AvisoNexo(avisos, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = EspacioAmplio))
    }

    val sesionPorAcordar = estado.sesion?.takeIf { it.admiteAcuerdo(LocalDate.now()) }
    if (acordando && sesionPorAcordar != null) {
        HojaAcuerdo(
            sesion = sesionPorAcordar,
            alCerrar = { acordando = false },
            alElegir = { minutos ->
                acordando = false
                viewModel.proponer(sesionPorAcordar.id, minutos) { it?.let { fallo -> alcance.launch { avisos.mostrarError(avisoFallo("enviar la propuesta", fallo)) } } }
            }
        )
    }

    val sesionPorCalificar = estado.sesion
    if (calificando && sesionPorCalificar != null) {
        HojaCalificacion(
            sesion = sesionPorCalificar,
            esTutor = estado.esTutor,
            alCerrar = { calificando = false },
            alEnviar = { estrellas, comentario ->
                viewModel.calificar(sesionPorCalificar.id, estrellas, comentario) { fallo ->
                    alcance.launch {
                        if (fallo == null) avisos.showSnackbar(if (estado.esTutor) "Gracias, tu calificación quedó guardada" else "Gracias, tu calificación ayuda a otros estudiantes")
                        else avisos.mostrarError(avisoFallo("guardar tu calificación", fallo))
                    }
                }
                calificando = false
            }
        )
    }
}

/** Separador discreto entre mensajes de días distintos: "Hoy", "Ayer" o "Lunes 26 de octubre". */
@Composable
private fun SeparadorDia(dia: LocalDate, modifier: Modifier = Modifier) {
    Text(
        formatoDia(dia),
        color = TextoTercero,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = EspacioNormal, bottom = EspacioFino)
            // Con TalkBack se puede saltar de un día a otro como entre encabezados
            .semantics { heading() }
    )
}

/**
 * Burbuja de mensaje. Los mensajes seguidos del mismo autor se agrupan: menos aire entre ellos,
 * esquina "de cola" solo en el último y la hora solo al cierre del grupo.
 */
@Composable
private fun Burbuja(mensaje: Mensaje, iniciaGrupo: Boolean, cierraGrupo: Boolean, modifier: Modifier = Modifier) {
    val mio = mensaje.mio
    val grande = RadioBurbuja
    val chica = RadioBurbujaCola
    val forma = RoundedCornerShape(
        topStart = if (!mio && !iniciaGrupo) chica else grande,
        topEnd = if (mio && !iniciaGrupo) chica else grande,
        bottomStart = if (!mio) chica else grande,
        bottomEnd = if (mio) chica else grande
    )
    Column(
        horizontalAlignment = if (mio) Alignment.End else Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (iniciaGrupo) EspacioAjustado else EspacioRozante)
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .then(
                    if (mio) Modifier.background(BosqueMedio, forma)
                    else Modifier.superficie(forma)
                )
                .padding(horizontal = EspacioCercano, vertical = EspacioAjustado)
        ) {
            Text(
                mensaje.texto,
                color = if (mio) SobreOscuro else TextoPrimario,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        if (cierraGrupo) {
            Text(
                formatoHora(mensaje.hora.toLocalTime()),
                color = TextoTercero,
                style = MaterialTheme.typography.bodySmall.merge(CifrasTabulares),
                modifier = Modifier.padding(horizontal = EspacioFino, vertical = EspacioMinimo)
            )
        }
    }
}

/** Pie de una conversación cuya sesión terminó: el historial se conserva, sin mensajes nuevos (RF8). */
@Composable
private fun AvisoSoloLectura(nota: Int? = null, motivo: String = "La sesión terminó.") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Superficie)
            .drawBehind { drawLine(Divisor, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
            .padding(horizontal = EspacioMargen, vertical = EspacioNormal)
    ) {
        Icon(IconosNexo.Candado, contentDescription = null, tint = TextoTercero, modifier = Modifier.size(Icono18))
        Spacer(Modifier.width(EspacioAjustado))
        Column {
            Text(
                "$motivo Puedes consultar la conversación, pero ya no enviar mensajes.",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodySmall
            )
            if (nota != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = EspacioMinimo)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Calificaste con $nota ${plural(nota, "estrella", "estrellas")}"
                        }
                ) {
                    Text("Calificaste", color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(EspacioMinimo))
                    Icon(IconosNexo.Estrella, contentDescription = null, tint = MielFuerte, modifier = Modifier.size(Icono14))
                    Text("$nota", color = TextoSecundario, style = MaterialTheme.typography.bodySmall.merge(CifrasTabulares))
                }
            }
        }
    }
}

/** Pie de una conversación terminada: si la sesión quedó sin calificar, invita a hacerlo (RF9). */
@Composable
private fun PieSesionTerminada(sesion: Sesion?, alCalificar: () -> Unit) {
    if (sesion == null) {
        AvisoSoloLectura()
        return
    }
    if (!sesion.completada) {
        AvisoSoloLectura(
            motivo = when {
                sesion.cierre != null -> listOfNotNull("La sesión se canceló.", sesion.motivoDeCierre(), sesion.textoDePenalizacion()).joinToString(" ")
                sesion.estado == EstadoSesion.CANCELADA -> "La sesión se canceló."
                else -> "La sesión se cerró sin asistencia."
            }
        )
        return
    }
    if (!sesion.porCalificar) {
        AvisoSoloLectura(sesion.calificacion)
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Superficie)
            .drawBehind { drawLine(Divisor, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
            .padding(start = EspacioMargen, end = EspacioNormal, top = EspacioMedio, bottom = EspacioMedio)
    ) {
        Column(Modifier.weight(1f)) {
            Text("La sesión terminó", color = TextoPrimario, style = MaterialTheme.typography.titleSmall)
            Text("Ya no se pueden enviar mensajes.", color = TextoTercero, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(EspacioMedio))
        BotonSecundario(
            "Calificar", alCalificar,
            icono = IconosNexo.Estrella, compacto = true, colorIcono = MielFuerte, alto = AreaTactilMinima,
            descripcion = "Calificar a ${sesion.conQuien}, ${sesion.materia}"
        )
    }
}
