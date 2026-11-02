package com.grupo3.nexo.domain.usecase

import com.grupo3.nexo.domain.model.EstadoSesion
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.ResumenActividad
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.PorInicio
import com.grupo3.nexo.domain.repository.AuthRepository
import com.grupo3.nexo.domain.repository.SesionRepository
import com.grupo3.nexo.domain.repository.TutorRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** Sesiones del modo actual: como estudiante, las tomadas; como tutor, las dadas. */
class ObtenerSesionesDelModoUseCase @Inject constructor(
    private val sesiones: SesionRepository,
    private val auth: AuthRepository
) {
    operator fun invoke(): Flow<List<Sesion>> =
        combine(sesiones.obtenerSesiones(), auth.usuarioActual) { lista, usuario ->
            val comoTutor = usuario.rol == Rol.TUTOR
            lista.filter { it.comoTutor == comoTutor }
        }
}

/** Próximas sesiones agendadas del modo actual. */
class ObtenerProximasSesionesUseCase @Inject constructor(
    private val delModo: ObtenerSesionesDelModoUseCase
) {
    operator fun invoke(): Flow<List<Sesion>> = delModo().map { lista ->
        lista.filter { it.proxima }.sortedWith(PorInicio)
    }
}

/**
 * Sesión a la que se refiere una conversación (RF8, RF9): la próxima agendada con esa persona; si la
 * conversación ya terminó, la última sesión que ocurrió, aunque haya otra futura.
 */
class ObtenerSesionDeConversacionUseCase @Inject constructor() {
    operator fun invoke(sesiones: List<Sesion>, contraparteId: String, soloLectura: Boolean): Sesion? {
        val delChat = sesiones.filter { it.contraparteId == contraparteId }
        fun masReciente(lista: List<Sesion>) = lista.maxWithOrNull(PorInicio)
        return if (soloLectura) {
            masReciente(delChat.filterNot { it.proxima })
        } else {
            delChat.filter { it.proxima }.minWithOrNull(PorInicio) ?: masReciente(delChat)
        }
    }
}

/** Historial del modo actual: sesiones que ya ocurrieron o se cancelaron (RF10). */
class ObtenerHistorialUseCase @Inject constructor(
    private val delModo: ObtenerSesionesDelModoUseCase
) {
    operator fun invoke(): Flow<List<Sesion>> = delModo().map { lista ->
        lista.filterNot { it.proxima }.sortedWith(PorInicio.reversed())
    }
}

/** Cifras del usuario en el modo actual: sesiones, horas, pendientes de calificar y reputación recibida. */
class ObtenerResumenActividadUseCase @Inject constructor(
    private val historial: ObtenerHistorialUseCase
) {
    operator fun invoke(): Flow<ResumenActividad> = historial().map { lista ->
        val completadas = lista.filter { it.estado == EstadoSesion.COMPLETADA }
        val recibidas = completadas.mapNotNull { it.calificacionRecibida }
        ResumenActividad(
            sesiones = completadas.size,
            horas = completadas.sumOf { it.duracionHoras },
            porCalificar = lista.count { it.porCalificar },
            promedioRecibido = recibidas.takeIf { it.isNotEmpty() }?.average(),
            totalResenas = recibidas.size,
            personas = completadas.map { it.contraparteId }.distinct().size,
            materias = completadas.groupingBy { it.materia }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
        )
    }
}

/** Reseñas que otros han escrito sobre el usuario como tutor, la más reciente primero. */
class ObtenerMisResenasUseCase @Inject constructor(
    private val tutores: TutorRepository,
    private val auth: AuthRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Resena>> = auth.usuarioActual.flatMapLatest { tutores.obtenerResenas(it.id) }
}

/** Califica una sesión completada: 1–5 estrellas y comentario opcional (RF9, en ambos sentidos). */
class CalificarSesionUseCase @Inject constructor(
    private val repositorio: SesionRepository
) {
    suspend operator fun invoke(sesionId: String, estrellas: Int, comentario: String? = null) {
        require(estrellas in 1..5) { "La calificación debe estar entre 1 y 5" }
        repositorio.calificar(sesionId, estrellas, comentario?.trim())
    }
}
