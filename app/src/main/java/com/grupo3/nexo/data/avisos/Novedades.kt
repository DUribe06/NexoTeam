package com.grupo3.nexo.data.avisos

import com.grupo3.nexo.data.remote.ChatDto
import com.grupo3.nexo.data.remote.EstadoSesionDto
import com.grupo3.nexo.data.remote.EstadoSolicitudDto
import com.grupo3.nexo.data.remote.ModalidadDto
import com.grupo3.nexo.data.remote.SesionTutoriaDto
import com.grupo3.nexo.data.remote.SolicitudDto
import com.grupo3.nexo.ui.components.diaYMes
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.serialization.Serializable

/** Lo que había en la revisión anterior: solo se avisa de lo que cambió desde entonces. */
@Serializable
data class FotoNovedades(
    val recibidas: Set<String> = emptySet(),
    val enviadas: Map<String, EstadoSolicitudDto> = emptyMap(),
    val noLeidos: Map<String, Int> = emptyMap(),
    val recordadas: Set<String> = emptySet()
)

/** Una notificación por mostrar. [clave] es estable: si ya estaba en pantalla, se reemplaza en vez de repetirse. */
data class Novedad(val clave: String, val titulo: String, val texto: String)

data class DatosNovedades(
    val recibidas: List<SolicitudDto>,
    val enviadas: List<SolicitudDto>,
    val chats: List<ChatDto>,
    val sesiones: List<SesionTutoriaDto>
)

private val ZONA_COLOMBIA: ZoneId = ZoneId.of("America/Bogota")
private val ANTICIPO_RECORDATORIO: Duration = Duration.ofHours(1)

/**
 * Compara con la revisión anterior y dice qué avisar. Sin [antes] (la primera revisión de la sesión) no avisa nada: lo que
 * ya estaba ahí no es una novedad y la persona no debe recibir de golpe todo su historial.
 */
fun calcularNovedades(antes: FotoNovedades?, datos: DatosNovedades, ahora: Instant): Pair<List<Novedad>, FotoNovedades> {
    val pendientes = datos.recibidas.filter { it.estado == EstadoSolicitudDto.PENDIENTE }
    val porRecordar = datos.sesiones.filter { porEmpezar(it, ahora) }
    val foto = FotoNovedades(
        recibidas = pendientes.map { it.id }.toSet(),
        enviadas = datos.enviadas.associate { it.id to it.estado },
        noLeidos = datos.chats.associate { it.id to it.noLeidos },
        recordadas = porRecordar.map { it.id }.toSet()
    )
    if (antes == null) return emptyList<Novedad>() to foto

    val novedades = buildList {
        pendientes.filter { it.id !in antes.recibidas }.forEach {
            add(Novedad("solicitud-${it.id}", "Nueva solicitud de ${it.nombreEstudiante}", it.cuando()))
        }
        datos.enviadas.filter { antes.enviadas[it.id] == EstadoSolicitudDto.PENDIENTE }.forEach { s ->
            val titulo = when (s.estado) {
                EstadoSolicitudDto.ACEPTADA -> "${s.nombreTutor} aceptó tu solicitud"
                EstadoSolicitudDto.RECHAZADA -> "${s.nombreTutor} no aceptó tu solicitud"
                EstadoSolicitudDto.EXPIRADA -> "${s.nombreTutor} no respondió a tiempo"
                else -> null
            }
            if (titulo != null) add(Novedad("solicitud-${s.id}", titulo, s.cuando()))
        }
        datos.chats.filter { it.noLeidos > (antes.noLeidos[it.id] ?: 0) }.forEach {
            add(Novedad("chat-${it.id}", "Mensaje de ${it.nombre}", it.ultimoMensaje))
        }
        porRecordar.filter { it.id !in antes.recordadas }.forEach {
            add(Novedad("sesion-${it.id}", "Tu tutoría de ${it.materia} es a las ${it.hora}", textoRecordatorio(it)))
        }
    }
    return novedades to foto
}

private fun SolicitudDto.cuando(): String {
    val dia = runCatching { diaYMes(LocalDate.parse(fecha)) }.getOrDefault(fecha)
    return "$materia · $dia, $hora"
}

private fun porEmpezar(sesion: SesionTutoriaDto, ahora: Instant): Boolean {
    if (sesion.estado != EstadoSesionDto.PROGRAMADA || sesion.iniciadaEn != null) return false
    val inicio = runCatching {
        LocalDate.parse(sesion.fecha).atTime(LocalTime.parse(sesion.hora)).atZone(ZONA_COLOMBIA).toInstant()
    }.getOrNull() ?: return false
    return ahora.isBefore(inicio) && !ahora.isBefore(inicio.minus(ANTICIPO_RECORDATORIO))
}

private fun textoRecordatorio(sesion: SesionTutoriaDto): String = when {
    sesion.comoTutor && sesion.modalidad == ModalidadDto.PRESENCIAL ->
        "Con ${sesion.conQuien}. Envíale el PIN cuando llegues."
    else -> "Con ${sesion.conQuien}."
}
