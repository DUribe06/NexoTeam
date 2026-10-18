package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.mock.MockData
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.repository.TutorRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TutorRepositoryImpl @Inject constructor() : TutorRepository {

    private val tutores = MutableStateFlow(MockData.tutores)
    private val resenas = MutableStateFlow(MockData.resenas)

    override fun obtenerTutores(): Flow<List<Tutor>> = tutores.asStateFlow()

    override fun obtenerTutor(id: String): Flow<Tutor?> =
        tutores.asStateFlow().map { lista -> lista.firstOrNull { it.id == id } }

    override fun obtenerResenas(tutorId: String): Flow<List<Resena>> =
        resenas.asStateFlow().map { lista -> lista.filter { it.tutorId == tutorId } }

    override suspend fun reportar(tutorId: String, motivo: String) {
        delay(800) // simula la llamada al backend
    }

    override suspend fun agregarResena(resena: Resena) {
        if (resena.texto.isNotBlank()) resenas.update { listOf(resena) + it }
        tutores.update { lista ->
            lista.map { t ->
                if (t.id != resena.tutorId) t
                else {
                    val total = t.totalResenas + 1
                    t.copy(rating = (t.rating * t.totalResenas + resena.estrellas) / total, totalResenas = total)
                }
            }
        }
    }
}
