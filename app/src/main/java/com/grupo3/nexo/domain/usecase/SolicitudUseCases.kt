package com.grupo3.nexo.domain.usecase

import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.repository.SolicitudRepository
import kotlinx.coroutines.flow.Flow
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.repository.AuthRepository
import kotlinx.coroutines.flow.combine
import com.grupo3.nexo.domain.repository.SesionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Devuelve todas las solicitudes (enviadas y recibidas). */
class ObtenerSolicitudesUseCase @Inject constructor(
    private val repositorio: SolicitudRepository
) {
    operator fun invoke(): Flow<List<Solicitud>> = repositorio.obtenerSolicitudes()
}

/**
 * Solicitudes del modo actual: como tutor, las que recibe; como estudiante, las que envió.
 * Ordenadas con las pendientes primero y, dentro de cada estado, la más reciente arriba.
 */
class ObtenerMisSolicitudesUseCase @Inject constructor(
    private val repositorio: SolicitudRepository,
    private val auth: AuthRepository
) {
    private val orden = listOf(
        EstadoSolicitud.PENDIENTE, EstadoSolicitud.ACEPTADA, EstadoSolicitud.FINALIZADA,
        EstadoSolicitud.RECHAZADA, EstadoSolicitud.CANCELADA
    )

    operator fun invoke(): Flow<List<Solicitud>> =
        combine(repositorio.obtenerSolicitudes(), auth.usuarioActual) { todas, usuario ->
            val esTutor = usuario.rol == Rol.TUTOR
            todas.filter { if (esTutor) it.tutorId == usuario.id else it.estudianteId == usuario.id }
                .reversed()
                .sortedBy { orden.indexOf(it.estado) }
        }
}

/** El tutor acepta o rechaza (con motivo opcional, RF6); el estudiante cancela. */
class ActualizarSolicitudUseCase @Inject constructor(
    private val repositorio: SolicitudRepository,
    private val sesiones: SesionRepository,
    private val auth: AuthRepository
) {
    /** Aceptar agenda la sesión; cancelar una aceptada la cancela también (RF6 → RF10). */
    suspend operator fun invoke(id: String, estado: EstadoSolicitud, motivo: String? = null) {
        val anterior = repositorio.obtenerSolicitudes().first().firstOrNull { it.id == id } ?: return
        repositorio.actualizarEstado(id, estado, motivo?.trim())
        when (estado) {
            EstadoSolicitud.ACEPTADA -> sesiones.programar(anterior, comoTutor = anterior.tutorId == auth.usuarioActual.value.id)
            EstadoSolicitud.CANCELADA, EstadoSolicitud.RECHAZADA -> sesiones.cancelarDe(id)
            else -> Unit
        }
    }
}

/** El estudiante envía una solicitud de tutoría (RF5 / UC5). */
class EnviarSolicitudUseCase @Inject constructor(
    private val repositorio: SolicitudRepository
) {
    suspend operator fun invoke(
        tutorId: String,
        nombreTutor: String,
        materia: String,
        tema: String,
        fecha: String,
        hora: String,
        modalidad: Modalidad,
        mensaje: String
    ) {
        require(tema.isNotBlank()) { "Indica el tema a reforzar" }
        repositorio.enviar(tutorId, nombreTutor, materia, tema.trim(), fecha, hora, modalidad, mensaje.trim())
    }
}
