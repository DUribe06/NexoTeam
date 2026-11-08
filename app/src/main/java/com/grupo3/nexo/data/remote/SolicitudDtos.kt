package com.grupo3.nexo.data.remote

import kotlinx.serialization.Serializable

/* Esquemas de /api/solicitudes. */

@Serializable(with = EstadoSolicitudDtoSerializador::class)
enum class EstadoSolicitudDto { PENDIENTE, ACEPTADA, RECHAZADA, FINALIZADA, CANCELADA, EXPIRADA, DESCONOCIDO }

object EstadoSolicitudDtoSerializador :
    EnumTolerante<EstadoSolicitudDto>("EstadoSolicitudDto", EstadoSolicitudDto.entries, EstadoSolicitudDto.DESCONOCIDO)

@Serializable
data class SolicitudDto(
    val id: String,
    val tutorId: String,
    val nombreTutor: String,
    val estudianteId: String,
    val nombreEstudiante: String,
    val materia: String,
    val tema: String,
    val fecha: String,
    val hora: String,
    val modalidad: ModalidadDto,
    val estado: EstadoSolicitudDto,
    val mensaje: String,
    val motivoRechazo: String? = null,
    val enviada: String,
    val venceEn: String,
    val penalizacionesEstudiante: Int = 0
)

@Serializable
data class SolicitudNueva(
    val tutorId: String,
    val materia: String,
    val tema: String,
    val fecha: String,
    val hora: String,
    val modalidad: ModalidadDto,
    val mensaje: String? = null
)

@Serializable
data class SolicitudRechazar(val motivo: String? = null)

@Serializable
data class ResultadoAceptarSolicitudDto(
    val solicitud: SolicitudDto,
    val sesion: SesionTutoriaDto,
    val chat: ChatDto
)
