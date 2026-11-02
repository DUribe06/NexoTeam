package com.grupo3.nexo.domain.usecase

import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.repository.DisponibilidadRepository
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Devuelve la disponibilidad horaria semanal del tutor. */
class ObtenerDisponibilidadUseCase @Inject constructor(
    private val repositorio: DisponibilidadRepository
) {
    operator fun invoke(): Flow<List<DiaDisponibilidad>> = repositorio.obtener()
}

/** Define la modalidad de una hora; sin modalidades, la hora deja de estar disponible (RF7). */
class DefinirHoraUseCase @Inject constructor(
    private val repositorio: DisponibilidadRepository
) {
    suspend operator fun invoke(dia: String, hora: LocalTime, modalidades: Set<Modalidad>) =
        repositorio.definirHora(dia, hora, modalidades)
}

/** Crea la semana editable cuando el tutor aún no tiene calendario, para que pueda empezar a publicar horas (RF7). */
class CrearCalendarioUseCase @Inject constructor(
    private val repositorio: DisponibilidadRepository
) {
    suspend operator fun invoke() = repositorio.crearCalendario()
}
