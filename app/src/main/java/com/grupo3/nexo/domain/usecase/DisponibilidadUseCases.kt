package com.grupo3.nexo.domain.usecase

import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.repository.DisponibilidadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

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
    suspend operator fun invoke(dia: String, hora: String, modalidades: Set<Modalidad>) =
        repositorio.definirHora(dia, hora, modalidades)
}
