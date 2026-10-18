package com.grupo3.nexo.domain.usecase

import com.grupo3.nexo.domain.model.Franja
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.repository.TutorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Devuelve el catálogo completo de tutores. */
class ObtenerTutoresUseCase @Inject constructor(
    private val repositorio: TutorRepository
) {
    operator fun invoke(): Flow<List<Tutor>> = repositorio.obtenerTutores()
}

/** Devuelve el detalle de un tutor por id. */
class ObtenerTutorUseCase @Inject constructor(
    private val repositorio: TutorRepository
) {
    operator fun invoke(id: String): Flow<Tutor?> = repositorio.obtenerTutor(id)
}

/** Devuelve las reseñas de un tutor, la más reciente primero (RF4). */
class ObtenerResenasUseCase @Inject constructor(
    private val repositorio: TutorRepository
) {
    operator fun invoke(tutorId: String): Flow<List<Resena>> = repositorio.obtenerResenas(tutorId)
}

/** Criterios de búsqueda de tutores (RF3 / HU-05). Todo es opcional. */
data class FiltroTutores(
    /** Texto libre: nombre, materia o universidad. */
    val consulta: String = "",
    /** Palabras clave de materia (un área agrupa varias, p. ej. Cálculo, Álgebra). */
    val materias: List<String> = emptyList(),
    val modalidad: Modalidad? = null,
    val franja: Franja? = null,
    val soloEnLinea: Boolean = false
) {
    val activo: Boolean get() = consulta.isNotBlank() || materias.isNotEmpty() || modalidad != null || franja != null || soloEnLinea
}

/**
 * Búsqueda con filtros de materia, modalidad y franja horaria.
 * Resultados ordenados por calificación (UC3, paso 4).
 */
class BuscarTutoresUseCase @Inject constructor(
    private val repositorio: TutorRepository
) {
    operator fun invoke(filtro: FiltroTutores = FiltroTutores()): Flow<List<Tutor>> =
        repositorio.obtenerTutores().map { lista -> lista.filter { cumple(it, filtro) }.sortedByDescending { it.rating } }

    private fun cumple(tutor: Tutor, f: FiltroTutores): Boolean {
        val texto = f.consulta.trim()
        val coincideTexto = texto.isEmpty() || buildString {
            append(tutor.nombre); append(' ')
            append(tutor.materias.joinToString(" ")); append(' ')
            append(tutor.universidad)
        }.contains(texto, ignoreCase = true)
        val coincideMateria = f.materias.isEmpty() ||
            tutor.materias.any { materia -> f.materias.any { materia.contains(it, ignoreCase = true) } }
        // Modalidad y franja se evalúan sobre la misma franja del horario: "virtual en la noche"
        // exige una hora nocturna que sea virtual, no una virtual y otra nocturna.
        val coincideHorario = (f.modalidad == null && f.franja == null) || tutor.horario.any { franja ->
            (f.modalidad == null || f.modalidad in franja.modalidades) &&
                (f.franja == null || f.franja.incluye(franja.hora))
        }
        val coincideLinea = !f.soloEnLinea || tutor.enLinea
        return coincideTexto && coincideMateria && coincideHorario && coincideLinea
    }
}

/** Reporta a un usuario (tutor) con un motivo. */
class ReportarTutorUseCase @Inject constructor(
    private val repositorio: TutorRepository
) {
    suspend operator fun invoke(tutorId: String, motivo: String) = repositorio.reportar(tutorId, motivo)
}
