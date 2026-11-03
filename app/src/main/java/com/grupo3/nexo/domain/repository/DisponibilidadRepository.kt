package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.DiaSolicitable
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de datos de la disponibilidad del tutor.
 *
 * Las lecturas (`Flow`) y las escrituras (`suspend`) de una implementación remota deben lanzar una excepción
 * ante fallos de red o del servidor: la UI la captura, muestra el error con "Reintentar" o un aviso breve.
 */
interface DisponibilidadRepository {
    fun obtener(): Flow<List<DiaDisponibilidad>>
    /** Si el tutor no tiene calendario, crea la semana con todas las horas sin publicar. */
    suspend fun crearCalendario()
    /** Reemplaza la semana completa de una vez: una hora sin modalidades deja de estar publicada (RF7). */
    suspend fun definirSemana(dias: List<DiaDisponibilidad>)
    /** Días que se le pueden pedir a un tutor sin las horas ya ocupadas; con null, se calculan de su horario (RF5). */
    fun diasSolicitablesDelServidor(tutorId: String): Flow<List<DiaSolicitable>>? = null
}
