package com.grupo3.nexo.ui.chat

import com.grupo3.nexo.ui.theme.BosqueMedio
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.Chat
import com.grupo3.nexo.domain.model.Mensaje
import com.grupo3.nexo.domain.usecase.EnviarMensajeUseCase
import com.grupo3.nexo.domain.usecase.MarcarChatLeidoUseCase
import com.grupo3.nexo.domain.usecase.ObtenerChatsUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMensajesUseCase
import com.grupo3.nexo.domain.usecase.ObtenerTutoresUseCase
import com.grupo3.nexo.ui.components.Avatar
import com.grupo3.nexo.ui.components.BotonIcono
import com.grupo3.nexo.ui.components.Campo
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.InsigniaConteo
import com.grupo3.nexo.ui.components.TextoLinea
import com.grupo3.nexo.ui.components.TituloPantalla
import com.grupo3.nexo.ui.components.pulsable
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.components.superficiePulsable
import com.grupo3.nexo.ui.theme.Bosque
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.Fondo
import com.grupo3.nexo.ui.theme.Menta
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.VerdeEstado
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import com.grupo3.nexo.ui.components.filaAgrupada
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ==================== ViewModels ==================== */

@HiltViewModel
class ChatsViewModel @Inject constructor(
    obtenerChats: ObtenerChatsUseCase
) : ViewModel() {
    data class Estado(val cargando: Boolean = true, val chats: List<Chat> = emptyList())

    val estado = obtenerChats()
        .map { Estado(cargando = false, chats = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConversacionViewModel @Inject constructor(
    obtenerChats: ObtenerChatsUseCase,
    obtenerMensajes: ObtenerMensajesUseCase,
    obtenerTutores: ObtenerTutoresUseCase,
    private val enviarMensaje: EnviarMensajeUseCase,
    private val marcarLeido: MarcarChatLeidoUseCase
) : ViewModel() {

    data class Estado(
        val nombre: String = "",
        val enLinea: Boolean = false,
        val soloLectura: Boolean = false,
        val mensajes: List<Mensaje> = emptyList()
    )

    private val chatId = MutableStateFlow("")

    val estado = combine(
        chatId,
        chatId.flatMapLatest { obtenerMensajes(it) },
        obtenerChats(),
        obtenerTutores()
    ) { id, mensajes, chats, tutores ->
        val chat = chats.firstOrNull { it.id == id }
        val tutor = tutores.firstOrNull { it.id == id }
        Estado(
            nombre = chat?.nombre ?: tutor?.nombre.orEmpty(),
            enLinea = chat?.enLinea ?: tutor?.enLinea ?: false,
            soloLectura = chat?.soloLectura ?: false,
            mensajes = mensajes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun cargar(id: String) {
        chatId.value = id
        viewModelScope.launch { marcarLeido(id) }
    }

    fun enviar(texto: String) {
        val limpio = texto.trim()
        if (limpio.isEmpty() || chatId.value.isEmpty() || estado.value.soloLectura) return
        viewModelScope.launch { enviarMensaje(chatId.value, limpio) }
    }
}

/* ==================== Bandeja de chats ==================== */

@Composable
fun PantallaChats(
    alAbrirChat: (String) -> Unit,
    alBuscar: () -> Unit,
    viewModel: ChatsViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val sinLeer = estado.chats.sumOf { it.noLeidos }

    Column(Modifier.fillMaxSize()) {
        TituloPantalla(
            titulo = "Chats",
            subtitulo = when (sinLeer) {
                0 -> "Coordina los detalles de tus sesiones"
                1 -> "1 mensaje sin leer"
                else -> "$sinLeer mensajes sin leer"
            }
        )

        if (estado.chats.isEmpty() && !estado.cargando) {
            EstadoVacio(
                icono = Icons.Rounded.ChatBubbleOutline,
                titulo = "Aún no tienes chats",
                mensaje = "El chat con un tutor se habilita cuando acepta tu solicitud de tutoría.",
                accion = "Buscar tutor",
                alAccion = alBuscar
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp)) {
                itemsIndexed(estado.chats, key = { _, c -> c.id }) { i, chat ->
                    FilaChat(
                        chat,
                        alAbrir = { alAbrirChat(chat.id) },
                        modifier = Modifier
                            .animateItem()
                            .filaAgrupada(i, estado.chats.size, sangria = 82.dp)
                    )
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
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Avatar(chat.nombre, tamano = 50.dp, enLinea = chat.enLinea)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextoLinea(chat.nombre, MaterialTheme.typography.titleMedium, TextoPrimario, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(
                    chat.hora,
                    color = if (sinLeer) VerdeEstado else TextoTercero,
                    style = MaterialTheme.typography.bodySmall.merge(CifrasTabulares).copy(
                        fontWeight = if (sinLeer) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
            Spacer(Modifier.height(3.dp))
            if (chat.soloLectura) {
                Text("Sesión finalizada · solo lectura", color = TextoTercero, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(1.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextoLinea(
                    chat.ultimoMensaje,
                    MaterialTheme.typography.bodyMedium.copy(fontWeight = if (sinLeer) FontWeight.Medium else FontWeight.Normal),
                    if (sinLeer) TextoPrimario else TextoSecundario,
                    Modifier.weight(1f)
                )
                if (sinLeer) {
                    Spacer(Modifier.width(8.dp))
                    InsigniaConteo(chat.noLeidos, bordeColor = Color.White)
                }
            }
        }
    }
}

/* ==================== Conversación ==================== */

private val respuestasRapidas = listOf(
    "Hola, ¿tienes disponibilidad esta semana?",
    "¿Cuánto dura una sesión?",
    "¿Podemos repasar para el parcial?"
)

@Composable
fun PantallaConversacion(
    chatId: String,
    alVolver: () -> Unit,
    viewModel: ConversacionViewModel = hiltViewModel()
) {
    LaunchedEffect(chatId) { viewModel.cargar(chatId) }
    val estado by viewModel.estado.collectAsState()
    val listaEstado = rememberLazyListState()
    var texto by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(estado.mensajes.size) {
        if (estado.mensajes.isNotEmpty()) listaEstado.animateScrollToItem(estado.mensajes.lastIndex)
    }

    fun enviar(valor: String) {
        viewModel.enviar(valor)
        texto = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .imePadding()
    ) {
        // Cabecera
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .drawBehind {
                    drawLine(Divisor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                }
                .statusBarsPadding()
                .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 10.dp)
        ) {
            BotonIcono(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", alVolver)
            Spacer(Modifier.width(6.dp))
            Avatar(estado.nombre.ifBlank { "Nexo" }, tamano = 42.dp, enLinea = estado.enLinea)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                TextoLinea(estado.nombre, MaterialTheme.typography.titleMedium, TextoPrimario)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (estado.enLinea) {
                        Box(Modifier.size(7.dp).background(Menta, CircleShape))
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        if (estado.enLinea) "En línea" else "Desconectado",
                        color = if (estado.enLinea) VerdeEstado else TextoTercero,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Mensajes
        if (estado.mensajes.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EstadoVacio(
                    icono = Icons.Rounded.ChatBubbleOutline,
                    titulo = "Empieza la conversación",
                    mensaje = "Coordina el tema, la fecha y el enlace de la sesión."
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(respuestasRapidas) { sugerencia ->
                    Text(
                        sugerencia,
                        color = BosqueProfundo,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .pulsable { enviar(sugerencia) }
                            .superficie(RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listaEstado,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(estado.mensajes, key = { _, m -> m.id }) { indice, mensaje ->
                    val anterior = estado.mensajes.getOrNull(indice - 1)
                    val siguiente = estado.mensajes.getOrNull(indice + 1)
                    Burbuja(
                        mensaje = mensaje,
                        iniciaGrupo = anterior?.mio != mensaje.mio,
                        cierraGrupo = siguiente?.mio != mensaje.mio,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        if (estado.soloLectura) {
            AvisoSoloLectura()
            return@Column
        }

        // Entrada de texto
        val puedeEnviar = texto.isNotBlank()
        val escalaEnviar by animateFloatAsState(if (puedeEnviar) 1f else 0.9f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "enviar")
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
        ) {
            Campo(
                valor = texto,
                alCambiar = { texto = it },
                marcador = "Escribe un mensaje",
                lineasMaximas = 4,
                teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = escalaEnviar
                        scaleY = escalaEnviar
                    }
                    .pulsable(habilitado = puedeEnviar) { enviar(texto) }
                    .then(
                        if (puedeEnviar) Modifier.background(BosqueMedio, RoundedCornerShape(10.dp))
                        else Modifier.background(Bosque.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
                    )
                    .semantics { contentDescription = "Enviar mensaje" }
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null,
                    tint = if (puedeEnviar) Color.White else TextoTercero,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Burbuja de mensaje. Los mensajes seguidos del mismo autor se agrupan: menos aire entre ellos,
 * esquina "de cola" solo en el último y la hora solo al cierre del grupo.
 */
@Composable
private fun Burbuja(mensaje: Mensaje, iniciaGrupo: Boolean, cierraGrupo: Boolean, modifier: Modifier = Modifier) {
    val mio = mensaje.mio
    val grande = 16.dp
    val chica = 4.dp
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
            .padding(top = if (iniciaGrupo) 10.dp else 3.dp)
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .then(
                    if (mio) Modifier.background(BosqueMedio, forma)
                    else Modifier.superficie(forma)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                mensaje.texto,
                color = if (mio) Color.White else TextoPrimario,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        if (cierraGrupo) {
            Text(
                mensaje.hora,
                color = TextoTercero,
                style = MaterialTheme.typography.bodySmall.merge(CifrasTabulares),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

/** Pie de una conversación cuya sesión terminó: el historial se conserva, sin mensajes nuevos (RF8). */
@Composable
private fun AvisoSoloLectura() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .drawBehind { drawLine(Divisor, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = TextoTercero, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "La sesión terminó. Puedes consultar la conversación, pero ya no enviar mensajes.",
            color = TextoSecundario,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
