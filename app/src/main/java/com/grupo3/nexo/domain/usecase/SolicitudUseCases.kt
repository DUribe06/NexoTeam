package com.grupo3.nexo.domain.usecase

import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import com.grupo3.nexo.domain.model.PRECIO_MAXIMO
import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.repository.AuthRepository
import com.grupo3.nexo.domain.repository.ChatRepository
import com.grupo3.nexo.domain.repository.SesionRepository
import com.grupo3.nexo.domain.repository.SolicitudRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

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
        EstadoSolicitud.RECHAZADA, EstadoSolicitud.CANCELADA, EstadoSolicitud.EXPIRADA
    )

    operator fun invoke(): Flow<List<Solicitud>> =
        combine(repositorio.obtenerSolicitudes(), auth.usuarioActual) { todas, usuario ->
            val esTutor = usuario.rol == Rol.TUTOR
            todas.filter { if (esTutor) it.tutorId == usuario.id else it.estudianteId == usuario.id }
                // Pendientes: primero la que vence antes; el resto, la más reciente arriba
                .sortedWith(
                    compareBy<Solicitud> { orden.indexOf(it.estado) }
                        .thenBy { if (it.estado == EstadoSolicitud.PENDIENTE) it.venceEn else null }
                        .thenByDescending { it.enviada }
                )
        }
}

/** El tutor acepta o rechaza (con motivo opcional, RF6); el estudiante cancela. */
class ActualizarSolicitudUseCase @Inject constructor(
    private val repositorio: SolicitudRepository,
    private val sesiones: SesionRepository,
    private val chats: ChatRepository,
    private val auth: AuthRepository
) {
    /**
     * Aceptar agenda la sesión y deja activo el chat con esa persona; cancelar una aceptada la cancela
     * también y, si no quedan sesiones por venir con ella, el chat pasa a solo lectura (RF6 → RF8, RF10).
     */
    // Con el backend real esta orquestación ya la hizo el servidor en actualizarEstado: las secundarias no hacen nada
    suspend operator fun invoke(id: String, estado: EstadoSolicitud, motivo: String? = null) {
        val anterior = repositorio.obtenerSolicitudes().first().firstOrNull { it.id == id } ?: return
        // La escritura principal decide el resultado: si falla, la excepción llega a quien llamó
        repositorio.actualizarEstado(id, estado, motivo?.trim())
        // Las secundarias se intentan sin tumbar el resultado: la solicitud ya cambió de estado
        when (estado) {
            EstadoSolicitud.ACEPTADA -> {
                val comoTutor = anterior.tutorId == auth.usuarioActual.value.id
                secundaria { sesiones.programar(anterior, comoTutor) }
                secundaria {
                    if (comoTutor) chats.habilitarChat(anterior.estudianteId, anterior.nombreEstudiante, enLinea = false, comoTutor = true)
                    else chats.habilitarChat(anterior.tutorId, anterior.nombreTutor, enLinea = false, comoTutor = false)
                }
            }
            EstadoSolicitud.CANCELADA -> secundaria {
                sesiones.cancelarDe(id)
                val comoTutor = anterior.tutorId == auth.usuarioActual.value.id
                val contraparte = if (comoTutor) anterior.estudianteId else anterior.tutorId
                val quedan = sesiones.obtenerSesiones().first().any { it.proxima && it.contraparteId == contraparte && it.comoTutor == comoTutor }
                if (!quedan) chats.cerrarChat(contraparte)
            }
            EstadoSolicitud.RECHAZADA -> secundaria { sesiones.cancelarDe(id) }
            else -> Unit
        }
    }

    /** Ejecuta un paso secundario ignorando su fallo; la cancelación de la corrutina sí se propaga. */
    private suspend fun secundaria(paso: suspend () -> Unit) {
        try {
            paso()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin reintento propio: no debe presentarse como fallo de la acción principal
        }
    }
}

/** El estudiante envía una solicitud de tutoría (RF5 / UC5). */
class EnviarSolicitudUseCase @Inject constructor(
    private val repositorio: SolicitudRepository
) {
    suspend operator fun invoke(
        tutor: Tutor,
        materia: String,
        tema: String,
        fecha: LocalDate,
        hora: LocalTime,
        modalidad: Modalidad,
        mensaje: String,
        precioPropuesto: Double? = null
    ) {
        require(tema.isNotBlank()) { "Indica el tema a reforzar" }
        require(precioPropuesto == null || precioPropuesto in 1.0..PRECIO_MAXIMO) { "El precio propuesto no es válido" }
        // Proponer la misma tarifa del tutor no es una propuesta
        val propuesta = precioPropuesto?.takeIf { it != tutor.precioHora }
        repositorio.enviar(tutor.id, materia, tema.trim(), fecha, hora, modalidad, mensaje.trim(), propuesta)
    }
}
