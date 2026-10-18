package com.grupo3.nexo.ui.solicitudes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.usecase.AbrirChatUseCase
import com.grupo3.nexo.domain.usecase.ActualizarSolicitudUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMisSolicitudesUseCase
import com.grupo3.nexo.domain.usecase.ObtenerTutoresUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import com.grupo3.nexo.ui.busqueda.AreaEstudio
import com.grupo3.nexo.ui.components.Avatar
import com.grupo3.nexo.ui.components.AvisoNexo
import com.grupo3.nexo.ui.components.BotonPrimario
import com.grupo3.nexo.ui.components.BotonSecundario
import com.grupo3.nexo.ui.components.BotonTexto
import com.grupo3.nexo.ui.components.Campo
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.DialogoConfirmacion
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.FiltroOpcion
import com.grupo3.nexo.ui.components.HojaNexo
import com.grupo3.nexo.ui.components.InsigniaEstado
import com.grupo3.nexo.ui.components.RotuloGrupo
import com.grupo3.nexo.ui.components.TextoLinea
import com.grupo3.nexo.ui.components.TituloPantalla
import com.grupo3.nexo.ui.components.icono
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.theme.MielOscura
import com.grupo3.nexo.ui.theme.Rojo
import com.grupo3.nexo.ui.theme.RojoFondo
import com.grupo3.nexo.ui.theme.SuperficieClara
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.grupo3.nexo.ui.components.BotonTonal
import com.grupo3.nexo.ui.components.Pestanas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.graphicsLayer
import com.grupo3.nexo.R
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueProfundo
import javax.inject.Inject

/* ==================== ViewModel ==================== */

@HiltViewModel
class SolicitudesViewModel @Inject constructor(
    obtenerMisSolicitudes: ObtenerMisSolicitudesUseCase,
    obtenerUsuario: ObtenerUsuarioUseCase,
    obtenerTutores: ObtenerTutoresUseCase,
    private val actualizarSolicitud: ActualizarSolicitudUseCase,
    private val abrirChat: AbrirChatUseCase
) : ViewModel() {

    data class Estado(
        val esTutor: Boolean = false,
        val filtro: EstadoSolicitud? = null,
        val solicitudes: List<Solicitud> = emptyList(),
        val conteos: Map<EstadoSolicitud, Int> = emptyMap(),
        val total: Int = 0,
        val tutores: List<Tutor> = emptyList()
    ) {
        val pendientes get() = conteos[EstadoSolicitud.PENDIENTE] ?: 0
    }

    /** El filtro recuerda el modo en que se eligió: al cambiar de estudiante a tutor se descarta. */
    private val filtro = MutableStateFlow<Pair<Rol, EstadoSolicitud>?>(null)

    val estado = combine(obtenerMisSolicitudes(), obtenerUsuario(), obtenerTutores(), filtro) { propias, usuario, tutores, elegido ->
        val f = elegido?.takeIf { it.first == usuario.rol }?.second
        Estado(
            esTutor = usuario.rol == Rol.TUTOR,
            filtro = f,
            solicitudes = propias.filter { f == null || it.estado == f },
            conteos = propias.groupingBy { it.estado }.eachCount(),
            total = propias.size,
            tutores = tutores
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())

    fun filtrar(valor: EstadoSolicitud?) {
        val rol = if (estado.value.esTutor) Rol.TUTOR else Rol.ESTUDIANTE
        filtro.value = if (valor == null || estado.value.filtro == valor) null else rol to valor
    }

    fun aceptar(id: String) = cambiar(id, EstadoSolicitud.ACEPTADA)
    fun rechazar(id: String, motivo: String?) = cambiar(id, EstadoSolicitud.RECHAZADA, motivo)
    fun cancelar(id: String) = cambiar(id, EstadoSolicitud.CANCELADA)

    /** El chat existe solo para solicitudes aceptadas (RF8): se crea si aún no existe y se abre. */
    fun abrirConversacion(solicitud: Solicitud, alListo: (String) -> Unit) {
        viewModelScope.launch {
            if (estado.value.esTutor) {
                abrirChat.conEstudiante(solicitud.estudianteId, solicitud.nombreEstudiante)
                alListo(solicitud.estudianteId)
            } else {
                val tutor = estado.value.tutores.firstOrNull { it.id == solicitud.tutorId } ?: return@launch
                abrirChat(tutor)
                alListo(tutor.id)
            }
        }
    }

    private fun cambiar(id: String, nuevo: EstadoSolicitud, motivo: String? = null) {
        viewModelScope.launch { actualizarSolicitud(id, nuevo, motivo) }
    }
}

/* ==================== Pantalla ==================== */

@Composable
fun PantallaSolicitudes(
    alBuscar: (area: String?) -> Unit,
    alAbrirChat: (String) -> Unit,
    viewModel: SolicitudesViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val avisos = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    var porCancelar by remember { mutableStateOf<Solicitud?>(null) }
    var porRechazar by remember { mutableStateOf<Solicitud?>(null) }
    fun avisar(texto: String) = alcance.launch {
        avisos.currentSnackbarData?.dismiss()
        avisos.showSnackbar(texto)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TituloPantalla(
                titulo = if (estado.esTutor) "Solicitudes recibidas" else "Mis solicitudes",
                subtitulo = when {
                    estado.total == 0 -> null
                    estado.pendientes == 0 -> "Todo al día"
                    estado.esTutor -> if (estado.pendientes == 1) "1 espera tu respuesta" else "${estado.pendientes} esperan tu respuesta"
                    else -> if (estado.pendientes == 1) "1 esperando respuesta del tutor" else "${estado.pendientes} esperando respuesta del tutor"
                }
            )

            if (estado.total > 0) {
                val estados = EstadoSolicitud.entries.filter { (estado.conteos[it] ?: 0) > 0 }
                Pestanas(
                    opciones = listOf("Todas") + estados.map { "${plural(it)} ${estado.conteos[it]}" },
                    seleccionada = estado.filtro?.let { estados.indexOf(it) + 1 } ?: 0,
                    alElegir = { i -> viewModel.filtrar(if (i == 0) null else estados[i - 1]) },
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            AnimatedContent(
                targetState = estado.solicitudes.isEmpty(),
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "lista",
                modifier = Modifier.weight(1f)
            ) { vacia ->
                if (vacia) {
                    EstadoVacio(
                        icono = Icons.AutoMirrored.Rounded.EventNote,
                        titulo = if (estado.esTutor) "Aún no recibes solicitudes" else "Aún no has pedido tutorías",
                        mensaje = if (estado.esTutor) "Publica tus horarios en Perfil › Mi disponibilidad para aparecer en las búsquedas."
                        else "Busca un tutor por materia, horario y modalidad, y envíale tu primera solicitud.",
                        accion = if (estado.esTutor) null else "Buscar tutor",
                        alAccion = { alBuscar(null) }
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
                    ) {
                        items(estado.solicitudes, key = { it.id }) { solicitud ->
                            TarjetaSolicitud(
                                solicitud = solicitud,
                                esTutor = estado.esTutor,
                                alAceptar = {
                                    viewModel.aceptar(solicitud.id)
                                    avisar("Tutoría con ${solicitud.nombreEstudiante.substringBefore(' ')} confirmada")
                                },
                                alRechazar = { porRechazar = solicitud },
                                alCancelar = { porCancelar = solicitud },
                                alEscribir = { viewModel.abrirConversacion(solicitud, alAbrirChat) },
                                alVerOtros = { alBuscar(AreaEstudio.deMateria(solicitud.materia)?.name) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }

        AvisoNexo(
            avisos,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }

    porRechazar?.let { s ->
        HojaRechazo(
            solicitud = s,
            alCerrar = { porRechazar = null },
            alConfirmar = { motivo ->
                viewModel.rechazar(s.id, motivo)
                porRechazar = null
                avisar("Solicitud rechazada. Le avisaremos a ${s.nombreEstudiante.substringBefore(' ')}")
            }
        )
    }

    porCancelar?.let { s ->
        val aceptada = s.estado == EstadoSolicitud.ACEPTADA
        DialogoConfirmacion(
            titulo = if (aceptada) "¿Cancelar la tutoría?" else "¿Cancelar la solicitud?",
            mensaje = if (aceptada) "Le avisaremos a ${s.nombreTutor} que no asistirás el ${s.fecha} a las ${s.hora}."
            else "${s.nombreTutor} ya no verá esta solicitud.",
            confirmar = if (aceptada) "Cancelar tutoría" else "Cancelar solicitud",
            alConfirmar = {
                viewModel.cancelar(s.id)
                porCancelar = null
                avisar(if (aceptada) "Tutoría cancelada" else "Solicitud cancelada")
            },
            alCerrar = { porCancelar = null }
        )
    }
}

/* ==================== Componentes privados ==================== */

private fun plural(e: EstadoSolicitud): String = when (e) {
    EstadoSolicitud.PENDIENTE -> "Pendientes"
    EstadoSolicitud.ACEPTADA -> "Aceptadas"
    EstadoSolicitud.RECHAZADA -> "Rechazadas"
    EstadoSolicitud.FINALIZADA -> "Finalizadas"
    EstadoSolicitud.CANCELADA -> "Canceladas"
}

@Composable
private fun TarjetaSolicitud(
    solicitud: Solicitud,
    esTutor: Boolean,
    alAceptar: () -> Unit,
    alRechazar: () -> Unit,
    alCancelar: () -> Unit,
    alEscribir: () -> Unit,
    alVerOtros: () -> Unit,
    modifier: Modifier = Modifier
) {
    val otraParte = if (esTutor) solicitud.nombreEstudiante else solicitud.nombreTutor
    // Las cerradas (rechazadas, finalizadas, canceladas) se apagan un poco: lo activo manda
    val activa = solicitud.estado == EstadoSolicitud.PENDIENTE || solicitud.estado == EstadoSolicitud.ACEPTADA

    Column(
        modifier = modifier
            .fillMaxWidth()
            .superficie(RoundedCornerShape(20.dp))
            .graphicsLayer { alpha = if (activa) 1f else 0.82f }
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(otraParte, tamano = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                TextoLinea(otraParte, MaterialTheme.typography.titleMedium, TextoPrimario)
                TextoLinea(solicitud.materia, MaterialTheme.typography.bodyMedium, TextoSecundario)
            }
            Spacer(Modifier.width(8.dp))
            InsigniaEstado(solicitud.estado)
        }

        Spacer(Modifier.height(12.dp))
        Text(solicitud.tema, color = TextoPrimario, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dato(ImageVector.vectorResource(R.drawable.ph_calendario), solicitud.fecha)
            Spacer(Modifier.width(14.dp))
            Dato(ImageVector.vectorResource(R.drawable.ph_reloj), solicitud.hora)
            Spacer(Modifier.width(14.dp))
            Dato(solicitud.modalidad.icono, solicitud.modalidad.etiqueta)
        }

        if (solicitud.mensaje.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "“${solicitud.mensaje}”",
                color = TextoTercero,
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
            )
        }

        val motivo = solicitud.motivoRechazo
        if (solicitud.estado == EstadoSolicitud.RECHAZADA && !motivo.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Rojo, fontWeight = FontWeight.SemiBold)) { append("Motivo: ") }
                    append(motivo)
                },
                color = TextoPrimario,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        when {
            esTutor && solicitud.estado == EstadoSolicitud.PENDIENTE -> {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BotonTonal("Aceptar", alAceptar, icono = Icons.Rounded.Check)
                    Spacer(Modifier.width(4.dp))
                    BotonTexto("Rechazar", alRechazar, color = TextoSecundario)
                }
                Spacer(Modifier.height(4.dp))
            }
            esTutor && solicitud.estado == EstadoSolicitud.ACEPTADA -> {
                Spacer(Modifier.height(12.dp))
                BotonTonal("Abrir chat", alEscribir, icono = Icons.Rounded.ChatBubbleOutline)
                Spacer(Modifier.height(4.dp))
            }
            !esTutor && solicitud.estado == EstadoSolicitud.ACEPTADA -> {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BotonTonal("Abrir chat", alEscribir, icono = Icons.Rounded.ChatBubbleOutline)
                    Spacer(Modifier.weight(1f))
                    BotonTexto("Cancelar", alCancelar, color = Rojo)
                }
                Spacer(Modifier.height(4.dp))
            }
            !esTutor && solicitud.estado == EstadoSolicitud.PENDIENTE -> {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = MielOscura, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Expira si no responde en 24 h",
                        color = TextoTercero,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    BotonTexto("Cancelar", alCancelar, color = Rojo)
                }
            }
            !esTutor && solicitud.estado == EstadoSolicitud.RECHAZADA -> {
                BotonTexto("Ver otros tutores de ${solicitud.materia}", alVerOtros, modifier = Modifier.offset(x = (-12).dp))
            }
            else -> Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Dato(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(texto, color = BosqueProfundo, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium).merge(CifrasTabulares))
    }
}

private val motivosRapidos = listOf(
    "Ese horario ya no está libre",
    "No domino ese tema",
    "Estaré de viaje esos días"
)

/** Rechazo con motivo opcional (RF6 / UC6): atajos frecuentes o texto libre. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HojaRechazo(solicitud: Solicitud, alCerrar: () -> Unit, alConfirmar: (String?) -> Unit) {
    var motivo by rememberSaveable { mutableStateOf("") }
    HojaNexo(
        titulo = "Rechazar solicitud",
        subtitulo = "${solicitud.nombreEstudiante} · ${solicitud.materia} · ${solicitud.fecha}, ${solicitud.hora}. A ${solicitud.nombreEstudiante.substringBefore(' ')} le sugeriremos otros tutores de ${solicitud.materia}.",
        alCerrar = alCerrar
    ) {
        RotuloGrupo("Motivo (opcional)", Modifier.padding(top = 20.dp, bottom = 10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            motivosRapidos.forEach { m ->
                FiltroOpcion(m, motivo == m, { motivo = if (motivo == m) "" else m }, conMarca = false)
            }
        }
        Spacer(Modifier.height(12.dp))
        Campo(
            valor = motivo,
            alCambiar = { motivo = it },
            marcador = "O escribe tu motivo",
            lineasMaximas = 3,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BotonTexto("Volver", alCerrar, color = TextoSecundario)
            Spacer(Modifier.width(8.dp))
            BotonPrimario(
                "Rechazar solicitud",
                alClicar = { alConfirmar(motivo.ifBlank { null }) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
