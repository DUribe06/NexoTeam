package com.grupo3.nexo.data.remote

import kotlinx.serialization.Serializable

/* Esquemas de /api/disponibilidad y /api/tutores/{tutorId}/disponibilidad. */

@Serializable
data class HoraDisponibleDto(val hora: String, val modalidades: List<ModalidadDto>)

@Serializable
data class DiaDisponibilidadDto(val dia: DiaSemanaDto, val horas: List<HoraDisponibleDto>)

@Serializable
data class SemanaDisponibilidadDto(val creado: Boolean, val dias: List<DiaDisponibilidadDto>)

@Serializable
data class SolicitudDefinirHora(val dia: DiaSemanaDto, val hora: String, val modalidades: List<ModalidadDto>)

@Serializable
data class SolicitudDefinirSemana(val horas: List<SolicitudDefinirHora>)

@Serializable
data class DiaSolicitableDto(val fecha: String, val franjas: List<HoraDisponibleDto>)
