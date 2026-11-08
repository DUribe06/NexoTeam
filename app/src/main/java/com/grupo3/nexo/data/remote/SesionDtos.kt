package com.grupo3.nexo.data.remote

import kotlinx.serialization.Serializable

/* Esquemas de /api/sesiones. No confundir con `SesionDto`, la sesión iniciada con sus tokens. */

@Serializable(with = EstadoSesionDtoSerializador::class)
enum class EstadoSesionDto { PROGRAMADA, COMPLETADA, CANCELADA, NO_ASISTIO, DESCONOCIDO }

object EstadoSesionDtoSerializador :
    EnumTolerante<EstadoSesionDto>("EstadoSesionDto", EstadoSesionDto.entries, EstadoSesionDto.DESCONOCIDO)

@Serializable
data class SesionTutoriaDto(
    val id: String,
    val solicitudId: String,
    val contraparteId: String,
    val conQuien: String,
    val materia: String,
    val fecha: String,
    val hora: String,
    val duracionHoras: Double,
    val modalidad: ModalidadDto,
    val estado: EstadoSesionDto,
    val comoTutor: Boolean,
    val calificacion: Int? = null,
    val comentario: String? = null,
    val calificacionRecibida: Int? = null,
    val iniciadaEn: String? = null,
    val pinEnviado: Boolean = false,
    val pin: String? = null,
    val limite: String? = null,
    val horaPactada: String? = null,
    val retardoMio: Boolean? = null,
    val cierre: String? = null,
    val penalizacion: String? = null,
    val acuerdo: AcuerdoPendienteDto? = null,
    val revision: String? = null,
    val puedePedirRevision: Boolean = false
)

@Serializable
data class AcuerdoPendienteDto(val tipo: String, val nuevaHora: String? = null, val propuestoPorMi: Boolean)

@Serializable
data class SolicitudIniciarSesion(val pin: String)

@Serializable
data class SolicitudAcuerdo(val tipo: String, val minutos: Int? = null)

@Serializable
data class SolicitudRevision(val motivo: String)

@Serializable
data class SolicitudCalificar(val estrellas: Int, val comentario: String? = null)
