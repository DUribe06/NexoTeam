package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.Solicitud
import kotlinx.coroutines.flow.Flow

interface SesionRepository {
    /** Todas las sesiones del usuario, en sus dos modos, de la más reciente a la más antigua. */
    fun obtenerSesiones(): Flow<List<Sesion>>
    /** Agenda la sesión de una solicitud recién aceptada (RF6 → RF10). */
    suspend fun programar(solicitud: Solicitud, comoTutor: Boolean)
    /** Cancela la sesión que nació de una solicitud, si existe. */
    suspend fun cancelarDe(solicitudId: String)
    suspend fun calificar(sesionId: String, estrellas: Int, comentario: String?)
}
