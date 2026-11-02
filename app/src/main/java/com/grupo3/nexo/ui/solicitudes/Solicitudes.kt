package com.grupo3.nexo.ui.solicitudes

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grupo3.nexo.R
import com.grupo3.nexo.domain.model.AccionesSolicitud
import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.HORAS_AVISO_PLAZO
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.model.acciones
import com.grupo3.nexo.domain.model.activa
import com.grupo3.nexo.domain.model.horasRestantes
import com.grupo3.nexo.domain.model.primerNombre
import com.grupo3.nexo.ui.busqueda.AreaEstudio
import com.grupo3.nexo.ui.components.Avatar
import com.grupo3.nexo.ui.components.AvisoNexo
import com.grupo3.nexo.ui.components.BotonPrimario
import com.grupo3.nexo.ui.components.BotonTexto
import com.grupo3.nexo.ui.components.BotonTonal
import com.grupo3.nexo.ui.components.CabeceraSeccion
import com.grupo3.nexo.ui.components.Campo
import com.grupo3.nexo.ui.components.Carga
import com.grupo3.nexo.ui.components.CifrasTabulares
import com.grupo3.nexo.ui.components.DialogoConfirmacion
import com.grupo3.nexo.ui.components.ErrorDeLista
import com.grupo3.nexo.ui.components.EsqueletoLista
import com.grupo3.nexo.ui.components.EstadoVacio
import com.grupo3.nexo.ui.components.FiltroOpcion
import com.grupo3.nexo.ui.components.HojaNexo
import com.grupo3.nexo.ui.components.IconosNexo
import com.grupo3.nexo.ui.components.InsigniaEstado
import com.grupo3.nexo.ui.components.Pestanas
import com.grupo3.nexo.ui.components.RotuloGrupo
import com.grupo3.nexo.ui.components.TextoLinea
import com.grupo3.nexo.ui.components.areaTactilSinCambiarMedida
import com.grupo3.nexo.ui.components.cargaDe
import com.grupo3.nexo.ui.components.formatoFecha
import com.grupo3.nexo.ui.components.formatoHoras
import com.grupo3.nexo.ui.components.icono
import com.grupo3.nexo.ui.components.pulsable
import com.grupo3.nexo.ui.components.superficie
import com.grupo3.nexo.ui.theme.BosqueAcento
import com.grupo3.nexo.ui.theme.BosqueProfundo
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
import com.grupo3.nexo.ui.theme.EspacioPildora
import com.grupo3.nexo.ui.theme.EtiquetaBoton
import com.grupo3.nexo.ui.theme.FormaBoton
import com.grupo3.nexo.ui.theme.FormaOrganica
import com.grupo3.nexo.ui.theme.Icono15
import com.grupo3.nexo.ui.theme.Icono17
import com.grupo3.nexo.ui.theme.MielOscura
import com.grupo3.nexo.ui.theme.NotaCursiva
import com.grupo3.nexo.ui.theme.Rojo
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoSecundario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.TinteBosque
import com.grupo3.nexo.ui.theme.cambioContenido
import kotlinx.coroutines.launch


/* ==================== Pantalla ==================== */

@Composable
fun PantallaSolicitudes(
    alBuscar: (area: String?) -> Unit,
    alAbrirChat: (String) -> Unit,
    alAbrirDisponibilidad: () -> Unit,
    viewModel: SolicitudesViewModel = hiltViewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
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
            CabeceraSeccion(
                titulo = if (estado.esTutor) "Solicitudes recibidas" else "Mis solicitudes",
                subtitulo = when {
                    estado.total == 0 -> null
                    estado.pendientes == 0 -> "Todo al día"
                    estado.esTutor -> if (estado.pendientes == 1) "1 espera tu respuesta" else "${estado.pendientes} esperan tu respuesta"
                    else -> if (estado.pendientes == 1) "1 esperando respuesta del tutor" else "${estado.pendientes} esperando respuesta del tutor"
                }
            )

            if (estado.total > 0) {
                val estados = remember(estado.conteos) { EstadoSolicitud.entries.filter { (estado.conteos[it] ?: 0) > 0 } }
                val opciones = remember(estados, estado.conteos) {
                    listOf("Todas") + estados.map { "${plural(it)} ${estado.conteos[it]}" }
                }
                Pestanas(
                    opciones = opciones,
                    seleccionada = estado.filtro?.let { estados.indexOf(it) + 1 } ?: 0,
                    alElegir = { i -> viewModel.filtrar(if (i == 0) null else estados[i - 1]) },
                    modifier = Modifier.padding(top = EspacioFino)
                )
            }

            val carga = cargaDe(estado.cargando, estado.solicitudes.isEmpty())
            AnimatedContent(
                targetState = carga,
                transitionSpec = { cambioContenido() },
                label = "lista",
                modifier = Modifier.weight(1f)
            ) { fase ->
                if (fase == Carga.CARGANDO) {
                    EsqueletoLista()
                } else if (fase == Carga.ERROR) {
                    ErrorDeLista(titulo = "No pudimos cargar tus solicitudes")
                } else if (fase == Carga.VACIO) {
                    EstadoVacio(
                        icono = IconosNexo.Calendario,
                        titulo = if (estado.esTutor) "Aún no recibes solicitudes" else "Aún no has pedido tutorías",
                        mensaje = if (estado.esTutor) "Publica tus horarios para aparecer en las búsquedas de los estudiantes."
                        else "Busca un tutor por materia, horario y modalidad, y envíale tu primera solicitud.",
                        accion = if (estado.esTutor) "Mi disponibilidad" else "Buscar tutor",
                        alAccion = if (estado.esTutor) alAbrirDisponibilidad else ({ alBuscar(null) })
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(EspacioMedio),
                        contentPadding = PaddingValues(start = EspacioMargen, end = EspacioMargen, top = EspacioNormal, bottom = EspacioAmplio)
                    ) {
                        items(estado.solicitudes, key = { it.id }) { solicitud ->
                            TarjetaSolicitud(
                                solicitud = solicitud,
                                esTutor = estado.esTutor,
                                horas = estado.horasRestantes[solicitud.id] ?: 0.0,
                                horasEnteras = estado.horasEnteras[solicitud.id] ?: 0.0,
                                alAceptar = {
                                    viewModel.aceptar(solicitud.id)
                                    avisar("Sesión con ${solicitud.nombreEstudiante.primerNombre} confirmada.")
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
                .padding(bottom = EspacioMedio)
        )
    }

    porRechazar?.let { s ->
        HojaRechazo(
            solicitud = s,
            alCerrar = { porRechazar = null },
            alConfirmar = { motivo ->
                viewModel.rechazar(s.id, motivo)
                porRechazar = null
                avisar("Solicitud rechazada. Le avisaremos a ${s.nombreEstudiante.primerNombre}.")
            }
        )
    }

    porCancelar?.let { s ->
        val aceptada = s.estado == EstadoSolicitud.ACEPTADA
        DialogoConfirmacion(
            titulo = if (aceptada) "¿Cancelar la sesión?" else "¿Cancelar la solicitud?",
            mensaje = if (aceptada) "Le avisaremos a ${s.nombreTutor} que no asistirás el ${formatoFecha(s.fecha)} a las ${s.hora}."
            else "${s.nombreTutor} ya no verá esta solicitud.",
            confirmar = if (aceptada) "Cancelar sesión" else "Cancelar solicitud",
            alConfirmar = {
                viewModel.cancelar(s.id)
                porCancelar = null
                avisar(if (aceptada) "Sesión cancelada." else "Solicitud cancelada.")
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
    EstadoSolicitud.EXPIRADA -> "Expiradas"
}

@Composable
private fun TarjetaSolicitud(
    solicitud: Solicitud,
    esTutor: Boolean,
    horas: Double,
    horasEnteras: Double,
    alAceptar: () -> Unit,
    alRechazar: () -> Unit,
    alCancelar: () -> Unit,
    alEscribir: () -> Unit,
    alVerOtros: () -> Unit,
    modifier: Modifier = Modifier
) {
    val otraParte = if (esTutor) solicitud.nombreEstudiante else solicitud.nombreTutor
    // Las cerradas (rechazadas, finalizadas, canceladas) apagan el nombre con color, no con transparencia: el contraste se mantiene
    val activa = solicitud.activa

    Column(
        modifier = modifier
            .fillMaxWidth()
            .superficie(FormaOrganica)
            .padding(start = EspacioNormal, end = EspacioNormal, top = EspacioNormal, bottom = EspacioCorto)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(otraParte, tamano = 40.dp)
            Spacer(Modifier.width(EspacioMedio))
            Column(Modifier.weight(1f)) {
                TextoLinea(otraParte, MaterialTheme.typography.titleMedium, if (activa) TextoPrimario else TextoSecundario)
                TextoLinea(solicitud.materia, MaterialTheme.typography.bodyMedium, TextoSecundario)
            }
            Spacer(Modifier.width(EspacioCorto))
            InsigniaEstado(solicitud.estado)
        }

        Spacer(Modifier.height(EspacioMedio))
        Text(solicitud.tema, color = TextoPrimario, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(EspacioFino))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dato(ImageVector.vectorResource(R.drawable.ph_calendario), formatoFecha(solicitud.fecha))
            Spacer(Modifier.width(EspacioCercano))
            Dato(ImageVector.vectorResource(R.drawable.ph_reloj), solicitud.hora)
            Spacer(Modifier.width(EspacioCercano))
            Dato(solicitud.modalidad.icono, solicitud.modalidad.etiqueta)
        }

        if (solicitud.mensaje.isNotBlank()) {
            Spacer(Modifier.height(EspacioAjustado))
            Text(
                "“${solicitud.mensaje}”",
                color = TextoTercero,
                style = MaterialTheme.typography.bodyMedium.merge(NotaCursiva)
            )
        }

        val motivo = solicitud.motivoRechazo
        if (solicitud.estado == EstadoSolicitud.RECHAZADA && !motivo.isNullOrBlank()) {
            Spacer(Modifier.height(EspacioAjustado))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Rojo, fontWeight = FontWeight.SemiBold)) { append("Motivo: ") }
                    append(motivo)
                },
                color = TextoPrimario,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        when (solicitud.acciones(esTutor)) {
            AccionesSolicitud.RESPONDER -> {
                Spacer(Modifier.height(EspacioMedio))
                AvisoPlazo(
                    if (horas < 1) "Queda menos de ${formatoHoras(1.0)} para responder" else if (horasEnteras < 2) "Queda ${formatoHoras(horasEnteras)} para responder" else "Quedan ${formatoHoras(horasEnteras)} para responder",
                    urgente = horas <= HORAS_AVISO_PLAZO
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BotonTonal(
                        "Aceptar",
                        alAceptar,
                        icono = IconosNexo.Marca,
                        accion = "aceptar la solicitud de $otraParte"
                    )
                    Spacer(Modifier.weight(1f))
                    BotonTexto(
                        "Rechazar",
                        alRechazar,
                        color = TextoSecundario,
                        accion = "rechazar la solicitud de $otraParte"
                    )
                }
                Spacer(Modifier.height(EspacioMinimo))
            }
            AccionesSolicitud.ABRIR_CHAT -> {
                Spacer(Modifier.height(EspacioMedio))
                BotonAbrirChat(otraParte, alEscribir)
                Spacer(Modifier.height(EspacioMinimo))
            }
            AccionesSolicitud.ABRIR_CHAT_O_CANCELAR -> {
                Spacer(Modifier.height(EspacioMedio))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BotonAbrirChat(otraParte, alEscribir)
                    Spacer(Modifier.weight(1f))
                    BotonTexto(
                        "Cancelar",
                        alCancelar,
                        color = Rojo,
                        accion = "cancelar con $otraParte"
                    )
                }
                Spacer(Modifier.height(EspacioMinimo))
            }
            AccionesSolicitud.CANCELAR -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvisoPlazo(
                        if (horas < 1) "Expira en menos de ${formatoHoras(1.0)}" else "Expira en ${formatoHoras(horasEnteras)} si no responde",
                        Modifier.weight(1f),
                        conAire = false,
                        urgente = horas <= HORAS_AVISO_PLAZO
                    )
                    BotonTexto(
                        "Cancelar",
                        alCancelar,
                        color = Rojo,
                        accion = "cancelar con $otraParte"
                    )
                }
            }
            AccionesSolicitud.VER_OTROS -> {
                BotonTexto("Ver otros tutores de ${solicitud.materia}", alVerOtros)
            }
            AccionesSolicitud.NINGUNA -> Spacer(Modifier.height(EspacioCorto))
        }
    }
}

/** "Abrir chat" como píldora, igual que en las tarjetas del Inicio (PRODUCT.md); se ve de 40 dp y se toca en 48. */
@Composable
private fun BotonAbrirChat(conQuien: String, alClicar: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .areaTactilSinCambiarMedida()
            .heightIn(min = 48.dp)
            .pulsable(etiqueta = "abrir chat con $conQuien") { alClicar() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 40.dp)
                .background(TinteBosque, FormaBoton)
                .padding(horizontal = EspacioPildora)
        ) {
            Icon(IconosNexo.Chat, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(Icono17))
            Spacer(Modifier.width(EspacioFino))
            Text("Abrir chat", color = BosqueProfundo, style = EtiquetaBoton)
        }
    }
}

/** Plazo de respuesta (RF6) en una línea discreta, igual para estudiante y tutor. */
@Composable
private fun AvisoPlazo(texto: String, modifier: Modifier = Modifier, conAire: Boolean = true, urgente: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = if (conAire) EspacioMedio else 0.dp)
    ) {
        Icon(ImageVector.vectorResource(R.drawable.ph_reloj), contentDescription = null, tint = MielOscura, modifier = Modifier.size(Icono15))
        Spacer(Modifier.width(EspacioFino))
        Text(texto, color = if (urgente) MielOscura else TextoTercero, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Dato(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = BosqueAcento, modifier = Modifier.size(Icono15))
        Spacer(Modifier.width(EspacioIcono))
        val estilo = MaterialTheme.typography.bodyMedium
        Text(texto, color = BosqueProfundo, style = remember(estilo) { estilo.copy(fontWeight = FontWeight.Medium).merge(CifrasTabulares) })
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
        subtitulo = "${solicitud.nombreEstudiante} · ${solicitud.materia} · ${formatoFecha(solicitud.fecha)}, ${solicitud.hora}. A ${solicitud.nombreEstudiante.primerNombre} le sugeriremos otros tutores de ${solicitud.materia}.",
        alCerrar = alCerrar
    ) {
        RotuloGrupo("Motivo (opcional)", Modifier.padding(top = EspacioMargen, bottom = EspacioAjustado))
        FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(EspacioCorto), verticalArrangement = Arrangement.spacedBy(EspacioCorto)) {
            motivosRapidos.forEach { m ->
                FiltroOpcion(m, motivo == m, { motivo = if (motivo == m) "" else m }, conMarca = false)
            }
        }
        Spacer(Modifier.height(EspacioMedio))
        Campo(
            valor = motivo,
            alCambiar = { motivo = it },
            marcador = "O escribe tu motivo",
            lineasMaximas = 3,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        Spacer(Modifier.height(EspacioAmplio))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BotonTexto("Volver", alCerrar, color = TextoSecundario)
            Spacer(Modifier.width(EspacioCorto))
            BotonPrimario(
                "Rechazar solicitud",
                alClicar = { alConfirmar(motivo.ifBlank { null }) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
