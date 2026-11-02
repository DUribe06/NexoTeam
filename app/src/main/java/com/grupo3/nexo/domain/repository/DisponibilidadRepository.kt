package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.Modalidad
import kotlinx.coroutines.flow.Flow

interface DisponibilidadRepository {
    fun obtener(): Flow<List<DiaDisponibilidad>>
    /** Define en qué modalidades se ofrece una hora; un conjunto vacío la desactiva (RF7). */
    /** Si el tutor no tiene calendario, crea la semana con todas las horas sin publicar. */
    suspend fun crearCalendario()
    suspend fun definirHora(dia: String, hora: String, modalidades: Set<Modalidad>)
}
