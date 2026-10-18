package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.Tutor
import kotlinx.coroutines.flow.Flow

interface TutorRepository {
    fun obtenerTutores(): Flow<List<Tutor>>
    fun obtenerTutor(id: String): Flow<Tutor?>
    fun obtenerResenas(tutorId: String): Flow<List<Resena>>
    suspend fun reportar(tutorId: String, motivo: String)
    /** Publica una reseña y recalcula el promedio y el total del tutor (RF9). */
    suspend fun agregarResena(resena: Resena)
}
