package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.mock.MockData
import com.grupo3.nexo.domain.model.EstadoSesion
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.repository.AuthRepository
import com.grupo3.nexo.domain.repository.SesionRepository
import com.grupo3.nexo.domain.repository.TutorRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SesionRepositoryImpl @Inject constructor(
    private val tutorRepository: TutorRepository,
    private val authRepository: AuthRepository
) : SesionRepository {

    private val sesiones = MutableStateFlow(MockData.sesiones)
    private val carga = PrimeraCarga(1000)

    override fun obtenerSesiones(): Flow<List<Sesion>> = carga.sobre(sesiones.asStateFlow())

    override suspend fun programar(solicitud: Solicitud, comoTutor: Boolean) {
        if (sesiones.value.any { it.solicitudId == solicitud.id }) return
        val (id, nombre) = if (comoTutor) solicitud.estudianteId to solicitud.nombreEstudiante else solicitud.tutorId to solicitud.nombreTutor
        val tutor = if (comoTutor) null else MockData.tutores.firstOrNull { it.id == id }
        val nueva = Sesion(
            id = "p${solicitud.id}",
            contraparteId = id,
            conQuien = nombre,
            materia = solicitud.materia,
            fecha = solicitud.fecha,
            hora = solicitud.hora,
            duracionHoras = 1.0,
            modalidad = solicitud.modalidad,
            estado = EstadoSesion.PROGRAMADA,
            comoTutor = comoTutor,
            colorPrimario = tutor?.colorPrimario ?: 0xFF2F5D7C,
            colorSecundario = tutor?.colorSecundario ?: 0xFF6A97B5,
            solicitudId = solicitud.id
        )
        sesiones.update { listOf(nueva) + it }
    }

    override suspend fun cancelarDe(solicitudId: String) {
        sesiones.update { actual ->
            actual.map { if (it.solicitudId == solicitudId && it.proxima) it.copy(estado = EstadoSesion.CANCELADA) else it }
        }
    }

    override suspend fun calificar(sesionId: String, estrellas: Int, comentario: String?) {
        delay(600)
        val texto = comentario?.takeIf { it.isNotBlank() }
        val sesion = sesiones.value.firstOrNull { it.id == sesionId } ?: return
        sesiones.update { actual -> actual.map { if (it.id == sesionId) it.copy(calificacion = estrellas, comentario = texto) else it } }
        // Calificar a un tutor publica la reseña en su perfil y recalcula su promedio (RF9)
        if (!sesion.comoTutor) {
            val autor = authRepository.usuarioActual.value
            tutorRepository.agregarResena(
                Resena(sesion.contraparteId, autor.nombre, autor.handle, texto.orEmpty(), estrellas, "ahora")
            )
        }
    }
}
