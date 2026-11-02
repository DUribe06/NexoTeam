package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.Modalidad
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de datos de la disponibilidad del tutor.
 *
 * Las lecturas (`Flow`) y las escrituras (`suspend`) de una implementación remota deben lanzar una excepción
 * ante fallos de red o del servidor: la UI la captura, muestra el error con "Reintentar" o un aviso breve.
 */
interface DisponibilidadRepository {
    fun obtener(): Flow<List<DiaDisponibilidad>>
    /** Define en qué modalidades se ofrece una hora; un conjunto vacío la desactiva (RF7). */
    /** Si el tutor no tiene calendario, crea la semana con todas las horas sin publicar. */
    suspend fun crearCalendario()
    suspend fun definirHora(dia: String, hora: LocalTime, modalidades: Set<Modalidad>)
}
