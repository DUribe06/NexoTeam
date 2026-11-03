package com.grupo3.nexo.data.remote

import kotlinx.serialization.Serializable

/* Esquemas que comparten varios módulos del contrato. */

@Serializable(with = ModalidadDtoSerializador::class)
enum class ModalidadDto { PRESENCIAL, VIRTUAL, DESCONOCIDO }

object ModalidadDtoSerializador : EnumTolerante<ModalidadDto>("ModalidadDto", ModalidadDto.entries, ModalidadDto.DESCONOCIDO)

@Serializable(with = DiaSemanaDtoSerializador::class)
enum class DiaSemanaDto { LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO, DESCONOCIDO }

object DiaSemanaDtoSerializador : EnumTolerante<DiaSemanaDto>("DiaSemanaDto", DiaSemanaDto.entries, DiaSemanaDto.DESCONOCIDO)

/** `PaginaTutores` y `PaginaResenas` del contrato. */
@Serializable
data class PaginaDto<T>(
    val pagina: Int,
    val tamano: Int,
    val totalElementos: Int,
    val totalPaginas: Int,
    val elementos: List<T>
)
