package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.mock.MockData
import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.repository.AuthRepository
import com.grupo3.nexo.domain.repository.SolicitudRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolicitudRepositoryImpl @Inject constructor(
    private val authRepository: AuthRepository
) : SolicitudRepository {

    private val solicitudes = MutableStateFlow(MockData.solicitudes)
    private var contador = solicitudes.value.size

    override fun obtenerSolicitudes(): Flow<List<Solicitud>> = solicitudes.asStateFlow()

    override suspend fun actualizarEstado(id: String, estado: EstadoSolicitud, motivo: String?) {
        delay(500)
        solicitudes.update { actual ->
            actual.map {
                if (it.id == id) it.copy(estado = estado, motivoRechazo = motivo?.takeIf { m -> m.isNotBlank() } ?: it.motivoRechazo)
                else it
            }
        }
    }

    override suspend fun enviar(
        tutorId: String,
        nombreTutor: String,
        materia: String,
        tema: String,
        fecha: String,
        hora: String,
        modalidad: Modalidad,
        mensaje: String
    ) {
        delay(700)
        val usuario = authRepository.usuarioActual.value
        solicitudes.update { actual ->
            actual + Solicitud(
                id = "s${++contador}",
                tutorId = tutorId,
                nombreTutor = nombreTutor,
                estudianteId = usuario.id,
                nombreEstudiante = usuario.nombre,
                materia = materia,
                tema = tema,
                fecha = fecha,
                hora = hora,
                modalidad = modalidad,
                estado = EstadoSolicitud.PENDIENTE,
                mensaje = mensaje
            )
        }
    }
}
