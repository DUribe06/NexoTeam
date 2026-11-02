package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.Solicitud
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de datos de las sesiones y su calificación.
 *
 * Las lecturas (`Flow`) y las escrituras (`suspend`) de una implementación remota deben lanzar una excepción
 * ante fallos de red o del servidor: la UI la captura, muestra el error con "Reintentar" o un aviso breve.
 */
interface SesionRepository {
    /** Todas las sesiones del usuario, en sus dos modos, de la más reciente a la más antigua. */
    fun obtenerSesiones(): Flow<List<Sesion>>
    /** Agenda la sesión de una solicitud recién aceptada (RF6 → RF10). */
    suspend fun programar(solicitud: Solicitud, comoTutor: Boolean)
    /** Cancela la sesión que nació de una solicitud, si existe. */
    suspend fun cancelarDe(solicitudId: String)
    suspend fun calificar(sesionId: String, estrellas: Int, comentario: String?)
}
