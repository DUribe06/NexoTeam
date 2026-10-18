package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Solicitud
import kotlinx.coroutines.flow.Flow

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
