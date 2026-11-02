package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.mock.MockData
import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.model.conPlazoAplicado
import com.grupo3.nexo.domain.repository.AuthRepository
import com.grupo3.nexo.domain.repository.SolicitudRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Singleton
class SolicitudRepositoryImpl @Inject constructor(
    private val authRepository: AuthRepository
) : SolicitudRepository {

    private val solicitudes = MutableStateFlow(MockData.solicitudes)
    private var contador = solicitudes.value.size

    override fun obtenerSolicitudes(): Flow<List<Solicitud>> = solicitudes.map { lista -> lista.map { it.conPlazoAplicado() } }

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
        fecha: LocalDate,
        hora: LocalTime,
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
                mensaje = mensaje,
                enviada = LocalDateTime.now()
            )
        }
    }
}
