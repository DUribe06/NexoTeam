package com.grupo3.nexo.data.remote

import com.grupo3.nexo.domain.model.AcuerdoPendiente
import com.grupo3.nexo.domain.model.CierreSesion
import com.grupo3.nexo.domain.model.NivelPenalizacion
import com.grupo3.nexo.domain.model.TipoAcuerdo
import com.grupo3.nexo.domain.model.Chat
import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.DiaSemana
import com.grupo3.nexo.domain.model.DiaSolicitable
import com.grupo3.nexo.domain.model.EstadoRevision
import com.grupo3.nexo.domain.model.EstadoSesion
import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.FranjaTutor
import com.grupo3.nexo.domain.model.HoraDisponible
import com.grupo3.nexo.domain.model.Mensaje
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.ResumenActividad
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.model.Tutor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * Del contrato al dominio. Un valor de enum que la app no conoce no tumba la lectura: el mapeador devuelve null y esa
 * fila no se muestra, porque la app no sabría qué acciones ofrecerle. Una fecha mal formada sí es un fallo del
 * servidor: lanza una excepción y la pantalla muestra el error con "Reintentar".
 */

/** Los momentos llegan con zona y el dominio los guarda sin ella, siempre en hora de Colombia. */
val ZONA_COLOMBIA: ZoneId = ZoneId.of("America/Bogota")

private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")

/** Lo que muestra la bandeja en un chat nuevo: el servidor lo envía vacío. */
private const val CHAT_SIN_MENSAJES = "Inicia la conversación"

fun momentoEnColombia(texto: String): LocalDateTime =
    OffsetDateTime.parse(texto).atZoneSameInstant(ZONA_COLOMBIA).toLocalDateTime()

fun LocalTime.aTextoApi(): String = format(FORMATO_HORA)

fun RolDto.aDominio(): Rol? = when (this) {
    RolDto.ESTUDIANTE -> Rol.ESTUDIANTE
    RolDto.TUTOR -> Rol.TUTOR
    RolDto.ADMIN, RolDto.DESCONOCIDO -> null
}

fun Rol.aDto(): RolDto = when (this) {
    Rol.ESTUDIANTE -> RolDto.ESTUDIANTE
    Rol.TUTOR -> RolDto.TUTOR
}

fun Modalidad.aDto(): ModalidadDto = when (this) {
    Modalidad.PRESENCIAL -> ModalidadDto.PRESENCIAL
    Modalidad.VIRTUAL -> ModalidadDto.VIRTUAL
}

fun DiaSemana.aDto(): DiaSemanaDto = when (this) {
    DiaSemana.LUNES -> DiaSemanaDto.LUNES
    DiaSemana.MARTES -> DiaSemanaDto.MARTES
    DiaSemana.MIERCOLES -> DiaSemanaDto.MIERCOLES
    DiaSemana.JUEVES -> DiaSemanaDto.JUEVES
    DiaSemana.VIERNES -> DiaSemanaDto.VIERNES
    DiaSemana.SABADO -> DiaSemanaDto.SABADO
    DiaSemana.DOMINGO -> DiaSemanaDto.DOMINGO
}

private fun ModalidadDto.aDominio(): Modalidad? = when (this) {
    ModalidadDto.PRESENCIAL -> Modalidad.PRESENCIAL
    ModalidadDto.VIRTUAL -> Modalidad.VIRTUAL
    ModalidadDto.DESCONOCIDO -> null
}

private fun DiaSemanaDto.aDominio(): DiaSemana? = when (this) {
    DiaSemanaDto.LUNES -> DiaSemana.LUNES
    DiaSemanaDto.MARTES -> DiaSemana.MARTES
    DiaSemanaDto.MIERCOLES -> DiaSemana.MIERCOLES
    DiaSemanaDto.JUEVES -> DiaSemana.JUEVES
    DiaSemanaDto.VIERNES -> DiaSemana.VIERNES
    DiaSemanaDto.SABADO -> DiaSemana.SABADO
    DiaSemanaDto.DOMINGO -> DiaSemana.DOMINGO
    DiaSemanaDto.DESCONOCIDO -> null
}

private fun EstadoSolicitudDto.aDominio(): EstadoSolicitud? = when (this) {
    EstadoSolicitudDto.PENDIENTE -> EstadoSolicitud.PENDIENTE
    EstadoSolicitudDto.ACEPTADA -> EstadoSolicitud.ACEPTADA
    EstadoSolicitudDto.RECHAZADA -> EstadoSolicitud.RECHAZADA
    EstadoSolicitudDto.FINALIZADA -> EstadoSolicitud.FINALIZADA
    EstadoSolicitudDto.CANCELADA -> EstadoSolicitud.CANCELADA
    EstadoSolicitudDto.EXPIRADA -> EstadoSolicitud.EXPIRADA
    EstadoSolicitudDto.DESCONOCIDO -> null
}

private fun EstadoSesionDto.aDominio(): EstadoSesion? = when (this) {
    EstadoSesionDto.PROGRAMADA -> EstadoSesion.PROGRAMADA
    EstadoSesionDto.COMPLETADA -> EstadoSesion.COMPLETADA
    EstadoSesionDto.CANCELADA -> EstadoSesion.CANCELADA
    EstadoSesionDto.NO_ASISTIO -> EstadoSesion.NO_ASISTIO
    EstadoSesionDto.DESCONOCIDO -> null
}

private fun List<ModalidadDto>.aDominio(): Set<Modalidad> = mapNotNullTo(mutableSetOf()) { it.aDominio() }

private fun FranjaTutorDto.aDominio(): FranjaTutor? {
    val modalidadesConocidas = modalidades.aDominio().takeIf { it.isNotEmpty() } ?: return null
    return FranjaTutor(dia.aDominio() ?: return null, LocalTime.parse(hora), modalidadesConocidas)
}

fun TutorDto.aDominio(): Tutor = Tutor(
    id = id,
    nombre = nombre,
    handle = handle,
    bio = bio,
    universidad = universidad,
    materias = materias,
    rating = calificacionPromedio,
    totalResenas = totalResenas,
    sesionesTotales = sesionesTotales,
    estudiantes = estudiantes,
    precioHora = precioHora,
    enLinea = enLinea,
    horario = horario.mapNotNull { it.aDominio() },
    penalizaciones = penalizaciones,
    fotoUrl = fotoUrl
)

fun ResenaDto.aDominio(): Resena = Resena(id, tutorId, autor, handleAutor, texto, estrellas, LocalDate.parse(fecha))

fun SemanaDisponibilidadDto.aDominio(): List<DiaDisponibilidad> =
    if (!creado) emptyList()
    else dias.mapNotNull { d ->
        val dia = d.dia.aDominio() ?: return@mapNotNull null
        DiaDisponibilidad(dia.nombre, d.horas.map { HoraDisponible(LocalTime.parse(it.hora), it.modalidades.aDominio()) })
    }

/** null si no queda ninguna hora con una modalidad conocida. */
fun DiaSolicitableDto.aDominio(): DiaSolicitable? {
    val dia = LocalDate.parse(fecha)
    val diaSemana = DiaSemana.desde(dia.dayOfWeek.value)
    val horas = franjas.mapNotNull { f ->
        f.modalidades.aDominio().takeIf { it.isNotEmpty() }?.let { FranjaTutor(diaSemana, LocalTime.parse(f.hora), it) }
    }
    return horas.takeIf { it.isNotEmpty() }?.let { DiaSolicitable(dia, it) }
}

fun SolicitudDto.aDominio(): Solicitud? {
    val modalidadConocida = modalidad.aDominio() ?: return null
    val estadoConocido = estado.aDominio() ?: return null
    return Solicitud(
        id = id,
        tutorId = tutorId,
        nombreTutor = nombreTutor,
        estudianteId = estudianteId,
        nombreEstudiante = nombreEstudiante,
        materia = materia,
        tema = tema,
        fecha = LocalDate.parse(fecha),
        hora = LocalTime.parse(hora),
        modalidad = modalidadConocida,
        estado = estadoConocido,
        mensaje = mensaje,
        motivoRechazo = motivoRechazo,
        enviada = momentoEnColombia(enviada),
        venceEn = momentoEnColombia(venceEn),
        penalizacionesEstudiante = penalizacionesEstudiante,
        precioPropuesto = precioPropuesto
    )
}

fun SesionTutoriaDto.aDominio(): Sesion? {
    val modalidadConocida = modalidad.aDominio() ?: return null
    val estadoConocido = estado.aDominio() ?: return null
    return Sesion(
        id = id,
        contraparteId = contraparteId,
        conQuien = conQuien,
        materia = materia,
        fecha = LocalDate.parse(fecha),
        hora = LocalTime.parse(hora),
        duracionHoras = duracionHoras,
        modalidad = modalidadConocida,
        estado = estadoConocido,
        comoTutor = comoTutor,
        calificacion = calificacion,
        comentario = comentario,
        calificacionRecibida = calificacionRecibida,
        solicitudId = solicitudId,
        pinEnviado = pinEnviado,
        pin = pin,
        iniciada = iniciadaEn != null,
        limite = limite?.let(::momentoEnColombia),
        horaPactada = horaPactada?.let(LocalTime::parse),
        retardoMio = retardoMio,
        // Un valor que esta versión no conoce no debe tumbar la lista: se lee como si no viniera
        cierre = CierreSesion.entries.firstOrNull { it.name == cierre },
        penalizacion = NivelPenalizacion.entries.firstOrNull { it.name == penalizacion },
        acuerdo = acuerdo?.aDominio(),
        revision = EstadoRevision.entries.firstOrNull { it.name == revision },
        puedePedirRevision = puedePedirRevision
    )
}

private fun AcuerdoPendienteDto.aDominio(): AcuerdoPendiente? =
    TipoAcuerdo.entries.firstOrNull { it.name == tipo }?.let { AcuerdoPendiente(it, nuevaHora?.let(LocalTime::parse), propuestoPorMi) }

fun ResumenActividadDto.aDominio(): ResumenActividad =
    ResumenActividad(
        sesiones, horas, porCalificar, promedioRecibido, totalResenas, personas, materias, penalizaciones,
        suspendidaHasta?.let(::momentoEnColombia)
    )

fun ChatDto.aDominio(): Chat = Chat(
    id = id,
    contraparteId = contraparteId,
    nombre = nombre,
    ultimoMensaje = ultimoMensaje.ifEmpty { CHAT_SIN_MENSAJES },
    hora = ultimoMensajeEn?.let(::momentoEnColombia),
    noLeidos = noLeidos,
    enLinea = enLinea,
    soloLectura = soloLectura,
    comoTutor = comoTutor,
    fotoUrl = fotoUrl
)

fun MensajeDto.aDominio(): Mensaje = Mensaje(id, chatId, texto, momentoEnColombia(enviadoEn), mio, imagenUrl)
