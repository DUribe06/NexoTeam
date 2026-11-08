package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.FiltroTutores
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.Tutor
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de datos de los tutores, sus reseñas y reportes.
 *
 * Las lecturas (`Flow`) y las escrituras (`suspend`) de una implementación remota deben lanzar una excepción
 * ante fallos de red o del servidor: la UI la captura, muestra el error con "Reintentar" o un aviso breve.
 */
interface TutorRepository {
    fun obtenerTutores(): Flow<List<Tutor>>
    fun obtenerTutor(id: String): Flow<Tutor?>
    /** Tutores que cumplen el filtro (RF3), en cualquier orden. */
    fun buscarTutores(filtro: FiltroTutores): Flow<List<Tutor>>
    fun obtenerResenas(tutorId: String): Flow<List<Resena>>
    suspend fun reportar(tutorId: String, motivo: String)
    /** Publica una reseña y recalcula el promedio y el total del tutor (RF9). Con el servidor ya ocurrió al calificar. */
    suspend fun agregarResena(resena: Resena)
}
