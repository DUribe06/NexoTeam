package com.grupo3.nexo.domain.usecase

import com.grupo3.nexo.domain.model.DiaSolicitable
import com.grupo3.nexo.domain.model.FiltroTutores
import com.grupo3.nexo.domain.model.FranjaTutor
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.repository.DisponibilidadRepository
import com.grupo3.nexo.domain.repository.TutorRepository
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

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

/**
 * Búsqueda con filtros de materia, modalidad y franja horaria.
 * Resultados ordenados por calificación (UC3, paso 4).
 */
class BuscarTutoresUseCase @Inject constructor(
    private val repositorio: TutorRepository
) {
    operator fun invoke(filtro: FiltroTutores = FiltroTutores()): Flow<List<Tutor>> =
        repositorio.buscarTutores(filtro).map { lista -> lista.sortedByDescending { it.rating } }
}

/** Reporta a un usuario (tutor) con un motivo. */
class ReportarTutorUseCase @Inject constructor(
    private val repositorio: TutorRepository
) {
    suspend operator fun invoke(tutorId: String, motivo: String) = repositorio.reportar(tutorId, motivo)
}

/* ==================== Reglas de la solicitud (UC5) ==================== */

/** Días hacia adelante en que se puede pedir una tutoría, contando desde mañana. */
const val DIAS_SOLICITABLES = 14L

/** Próximos días (desde mañana, dos semanas) con horario del tutor; la fecha nunca es anterior a hoy. */
fun diasSolicitables(horario: List<FranjaTutor>, hoy: LocalDate = LocalDate.now()): List<DiaSolicitable> =
    (1L..DIAS_SOLICITABLES).mapNotNull { desfase ->
        val fecha = hoy.plusDays(desfase)
        val franjas = horario.filter { it.dia.numero == fecha.dayOfWeek.value }.sortedBy { it.hora }
        if (franjas.isEmpty()) null else DiaSolicitable(fecha, franjas)
    }

/**
 * Días que la hoja Solicitar ofrece: los del servidor, que ya descartan las horas ocupadas. Si esa lectura falla se usan
 * los del horario del tutor y el servidor valida al enviar; sin servidor, siempre los del horario.
 */
class ObtenerDiasSolicitablesUseCase @Inject constructor(
    private val repositorio: DisponibilidadRepository
) {
    operator fun invoke(tutor: Tutor): Flow<List<DiaSolicitable>> {
        val delHorario = diasSolicitables(tutor.horario)
        return repositorio.diasSolicitablesDelServidor(tutor.id)?.catch { emit(delHorario) } ?: flowOf(delHorario)
    }
}

/** La franja pedida si existe ese día; si no, la primera disponible. */
fun franjaValida(dia: DiaSolicitable?, hora: LocalTime?): FranjaTutor? =
    dia?.franjas?.firstOrNull { it.hora == hora } ?: dia?.franjas?.firstOrNull()

/** La modalidad pedida si la franja la ofrece; si no, la primera que ofrezca. */
fun modalidadValida(franja: FranjaTutor?, modalidad: Modalidad?): Modalidad? =
    modalidad?.takeIf { franja != null && it in franja.modalidades }
        ?: franja?.modalidades?.let { m -> Modalidad.entries.firstOrNull { it in m } }
