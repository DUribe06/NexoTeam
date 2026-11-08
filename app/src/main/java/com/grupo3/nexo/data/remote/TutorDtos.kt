package com.grupo3.nexo.data.remote

import kotlinx.serialization.Serializable

/* Esquemas de /api/tutores. */

@Serializable
data class FranjaTutorDto(val dia: DiaSemanaDto, val hora: String, val modalidades: List<ModalidadDto>)

@Serializable
data class TutorDto(
    val id: String,
    val nombre: String,
    val handle: String,
    val bio: String,
    val universidad: String,
    val materias: List<String>,
    val calificacionPromedio: Double,
    val totalResenas: Int,
    val sesionesTotales: Int,
    val estudiantes: Int,
    val precioHora: Double,
    val enLinea: Boolean,
    val horario: List<FranjaTutorDto>,
    val penalizaciones: Int = 0
)

@Serializable
data class ResenaDto(
    val id: String,
    val tutorId: String,
    val autor: String,
    val handleAutor: String,
    val texto: String,
    val estrellas: Int,
    val fecha: String
)

@Serializable
data class SolicitudReporte(val motivo: String)

@Serializable
data class ReporteDto(val id: String, val creadoEn: String)
