package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Solicitud
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de datos de las solicitudes de tutoría.
 *
 * Las lecturas (`Flow`) y las escrituras (`suspend`) de una implementación remota deben lanzar una excepción
 * ante fallos de red o del servidor: la UI la captura, muestra el error con "Reintentar" o un aviso breve.
 */
interface SolicitudRepository {
    fun obtenerSolicitudes(): Flow<List<Solicitud>>
    /** `motivo` solo aplica al rechazar (RF6). */
    suspend fun actualizarEstado(id: String, estado: EstadoSolicitud, motivo: String? = null)
    suspend fun enviar(
        tutorId: String,
        nombreTutor: String,
        materia: String,
        tema: String,
        fecha: String,
        hora: String,
        modalidad: Modalidad,
        mensaje: String
    )
}
