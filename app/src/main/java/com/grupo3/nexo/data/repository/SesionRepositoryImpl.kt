package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.mock.MockData
import com.grupo3.nexo.domain.model.AcuerdoPendiente
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.EstadoRevision
import com.grupo3.nexo.domain.model.EstadoSesion
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.model.TipoAcuerdo
import com.grupo3.nexo.domain.repository.AuthRepository
import com.grupo3.nexo.domain.repository.SesionRepository
import com.grupo3.nexo.domain.repository.TutorRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

private const val PIN_DE_MUESTRA = "4821"

@Singleton
class SesionRepositoryImpl @Inject constructor(
    private val tutorRepository: TutorRepository,
    private val authRepository: AuthRepository
) : SesionRepository {

    private val sesiones = MutableStateFlow(MockData.sesiones)
    private val carga = PrimeraCarga(1000)

    override fun obtenerSesiones(): Flow<List<Sesion>> = carga.sobre(sesiones.map { lista -> lista.map(::conLimite) })

    private fun conLimite(s: Sesion): Sesion =
        if (s.proxima && s.modalidad == Modalidad.PRESENCIAL && !s.iniciada) {
            s.copy(limite = s.fecha.atTime(s.horaPactada ?: s.hora).plusMinutes(10))
        } else {
            s
        }

    /** Sin servidor no hay otra persona: el PIN de muestra y los acuerdos solo dejan ver cómo se comporta la app. */
    private fun cambiar(sesionId: String, cambio: (Sesion) -> Sesion) {
        sesiones.update { actual -> actual.map { if (it.id == sesionId) cambio(it) else it } }
    }

    override suspend fun enviarPin(sesionId: String) {
        delay(300)
        cambiar(sesionId) { it.copy(pinEnviado = true, pin = PIN_DE_MUESTRA.takeUnless { _ -> it.comoTutor }) }
    }

    override suspend fun iniciar(sesionId: String, pin: String) {
        delay(300)
        if (pin != PIN_DE_MUESTRA) throw ErrorApi(CodigoError.PIN_INCORRECTO, "El PIN no coincide.")
        cambiar(sesionId) { it.copy(iniciada = true, pin = null) }
    }

    override suspend fun proponerAcuerdo(sesionId: String, tipo: TipoAcuerdo, minutos: Int?) {
        delay(300)
        cambiar(sesionId) { it.copy(acuerdo = AcuerdoPendiente(tipo, minutos?.let { m -> it.hora.plusMinutes(m.toLong()) }, propuestoPorMi = true)) }
    }

    override suspend fun aceptarAcuerdo(sesionId: String) {
        delay(300)
        cambiar(sesionId) { it.copy(horaPactada = it.acuerdo?.nuevaHora, retardoMio = false, acuerdo = null) }
    }

    override suspend fun rechazarAcuerdo(sesionId: String) {
        delay(300)
        cambiar(sesionId) { it.copy(acuerdo = null) }
    }

    override suspend fun pedirRevision(sesionId: String, motivo: String) {
        delay(300)
        cambiar(sesionId) { it.copy(revision = EstadoRevision.ABIERTA, puedePedirRevision = false) }
    }

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
                Resena(UUID.randomUUID().toString(), sesion.contraparteId, autor.nombre, autor.handle, texto.orEmpty(), estrellas, java.time.LocalDate.now())
            )
        }
    }
}
