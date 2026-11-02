package com.grupo3.nexo.domain.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Modelos de dominio de Nexo.
 * Puros: sin dependencias de Android ni de framework.
 */

/** Horas que tiene el tutor para responder una solicitud antes de que expire (RF5, RF6). */
const val PLAZO_RESPUESTA_HORAS = 24

enum class Rol(val etiqueta: String) {
    ESTUDIANTE("Estudiante"),
    TUTOR("Tutor")
}

enum class EstadoSolicitud(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    ACEPTADA("Aceptada"),
    RECHAZADA("Rechazada"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada"),
    /** Pasó el plazo de respuesta sin que el tutor contestara (RF6). */
    EXPIRADA("Expirada")
}

/** Ciclo de vida de una sesión de tutoría (RF10). */
enum class EstadoSesion(val etiqueta: String) {
    PROGRAMADA("Programada"),
    COMPLETADA("Completada"),
    CANCELADA("Cancelada"),
    NO_ASISTIO("No asistió")
}

/** Forma en la que se presta la tutoría (glosario de la Entrega 1B). */
enum class Modalidad(val etiqueta: String) {
    VIRTUAL("Virtual"),
    PRESENCIAL("Presencial")
}

/** Franjas del día para filtrar por horario (HU-05). Horas en formato "HH:mm". */
enum class Franja(val etiqueta: String, val desde: Int, val hasta: Int) {
    MANANA("Mañana", 6, 11),
    TARDE("Tarde", 12, 17),
    NOCHE("Noche", 18, 23);

    fun incluye(hora: String): Boolean = hora.substringBefore(':').toIntOrNull()?.let { it in desde..hasta } ?: false
}

/** Días de la semana; `numero` coincide con java.time.DayOfWeek (1 = lunes). */
enum class DiaSemana(val numero: Int, val nombre: String, val abreviatura: String) {
    LUNES(1, "Lunes", "Lun"),
    MARTES(2, "Martes", "Mar"),
    MIERCOLES(3, "Miércoles", "Mié"),
    JUEVES(4, "Jueves", "Jue"),
    VIERNES(5, "Viernes", "Vie"),
    SABADO(6, "Sábado", "Sáb"),
    DOMINGO(7, "Domingo", "Dom");

    companion object {
        fun desde(numero: Int): DiaSemana = entries.first { it.numero == numero }
    }
}

/** Franja publicada por un tutor: día, hora de inicio (1 h) y modalidades que ofrece en ella (RF7). */
data class FranjaTutor(
    val dia: DiaSemana,
    val hora: String,
    val modalidades: Set<Modalidad>
)

data class Tutor(
    val id: String,
    val nombre: String,
    val handle: String,
    val bio: String,
    val universidad: String,
    val materias: List<String>,
    val rating: Double,
    val totalResenas: Int,
    val sesionesTotales: Int,
    val estudiantes: Int,
    /** Tarifa por hora en pesos colombianos (COP). */
    val precioHora: Double,
    val enLinea: Boolean,
    val horario: List<FranjaTutor> = emptyList()
) {
    /** Modalidades que el tutor ofrece en al menos una franja. */
    val modalidades: Set<Modalidad> get() = horario.flatMapTo(mutableSetOf()) { it.modalidades }
}

/** Primer nombre de un nombre completo ("Laura Gómez" -> "Laura"). */
val String.primerNombre: String get() = substringBefore(' ')

data class Resena(
    val id: String,
    val tutorId: String,
    val autor: String,
    val handleAutor: String,
    val texto: String,
    val estrellas: Int,
    val fecha: String
)

/** Qué puede hacer cada parte con una solicitud según su estado. */
enum class AccionesSolicitud { RESPONDER, ABRIR_CHAT, ABRIR_CHAT_O_CANCELAR, CANCELAR, VER_OTROS, NINGUNA }

data class Solicitud(
    val id: String,
    val tutorId: String,
    val nombreTutor: String,
    val estudianteId: String,
    val nombreEstudiante: String,
    val materia: String,
    /** Tema a reforzar (RF5). */
    val tema: String,
    val fecha: String,
    val hora: String,
    val modalidad: Modalidad,
    val estado: EstadoSolicitud,
    val mensaje: String,
    /** Motivo opcional que da el tutor al rechazar (RF6). */
    val motivoRechazo: String? = null,
    /** Momento en que el estudiante la envió; con él se cuenta el plazo de respuesta (RF6). */
    val enviada: LocalDateTime = LocalDateTime.now()
)

/** Horas que quedan para que el tutor responda; 0 o menos si el plazo ya venció. */
fun Solicitud.horasRestantes(ahora: LocalDateTime = LocalDateTime.now()): Double =
    PLAZO_RESPUESTA_HORAS - Duration.between(enviada, ahora).toMinutes() / 60.0

/** Una pendiente con el plazo vencido se trata como expirada; las demás quedan igual. */
fun Solicitud.conPlazoAplicado(ahora: LocalDateTime = LocalDateTime.now()): Solicitud =
    if (estado == EstadoSolicitud.PENDIENTE && horasRestantes(ahora) <= 0.0) copy(estado = EstadoSolicitud.EXPIRADA) else this

/** Sigue en curso: espera respuesta o ya se aceptó. Las demás están cerradas. */
val Solicitud.activa: Boolean get() = estado == EstadoSolicitud.PENDIENTE || estado == EstadoSolicitud.ACEPTADA

/** Cuántas solicitudes esperan respuesta. */
fun List<Solicitud>.contarPendientes(): Int = count { it.estado == EstadoSolicitud.PENDIENTE }

fun Solicitud.acciones(esTutor: Boolean): AccionesSolicitud = when {
    esTutor && estado == EstadoSolicitud.PENDIENTE -> AccionesSolicitud.RESPONDER
    esTutor && estado == EstadoSolicitud.ACEPTADA -> AccionesSolicitud.ABRIR_CHAT
    !esTutor && estado == EstadoSolicitud.ACEPTADA -> AccionesSolicitud.ABRIR_CHAT_O_CANCELAR
    !esTutor && estado == EstadoSolicitud.PENDIENTE -> AccionesSolicitud.CANCELAR
    !esTutor && (estado == EstadoSolicitud.RECHAZADA || estado == EstadoSolicitud.EXPIRADA) -> AccionesSolicitud.VER_OTROS
    else -> AccionesSolicitud.NINGUNA
}

val mesesCortos = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/**
 * Único lector de las fechas cortas de los datos ("24 oct", "30 sept."): devuelve día y mes (1–12),
 * o null si el texto no es una fecha.
 */
fun diaYMes(fecha: String): Pair<Int, Int>? {
    val partes = fecha.trim().split(" ")
    val dia = partes.getOrNull(0)?.toIntOrNull() ?: return null
    val mes = mesesCortos.indexOf(partes.getOrNull(1)?.lowercase()?.trimEnd('.')?.take(3)) + 1
    return if (mes > 0) dia to mes else null
}

/** Fecha corta llevada al calendario: los datos no traen año, así que se toma el más cercano a hoy. */
fun fechaCalendario(texto: String, hoy: LocalDate = LocalDate.now()): LocalDate? {
    val (dia, mes) = diaYMes(texto) ?: return null
    val fecha = runCatching { LocalDate.of(hoy.year, mes, dia) }.getOrNull() ?: return null
    return if (fecha.isBefore(hoy.minusMonths(6))) fecha.plusYears(1) else fecha
}

/**
 * Clave para ordenar fechas cortas como "24 oct" o "30 sept" y horas "HH:mm".
 */
fun claveFecha(fecha: String, hora: String = "00:00"): Int {
    val dia = fechaCalendario(fecha)?.toEpochDay()?.toInt() ?: 0
    val minutos = hora.split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (it.getOrNull(1)?.toIntOrNull() ?: 0) }
    return dia * 1440 + minutos
}

/** Hora en que termina una sesión: "15:00" y 1,5 h → "16:30". Si la hora no se entiende, se devuelve tal cual. */
fun horaFin(hora: String, horas: Double): String {
    val partes = hora.split(":").mapNotNull { it.toIntOrNull() }
    if (partes.size != 2) return hora
    val total = partes[0] * 60 + partes[1] + (horas * 60).roundToInt()
    return String.format(Locale("es", "CO"), "%02d:%02d", (total / 60) % 24, total % 60)
}

data class Sesion(
    val id: String,
    /** Id de la otra parte: el tutor si el usuario fue estudiante, el estudiante si fue tutor. */
    val contraparteId: String,
    val conQuien: String,
    val materia: String,
    val fecha: String,
    val hora: String,
    val duracionHoras: Double,
    val modalidad: Modalidad,
    val estado: EstadoSesion,
    /** true si el usuario dio la sesión como tutor. */
    val comoTutor: Boolean = false,
    /** Calificación que el usuario dio a la otra parte (1–5). null si aún no califica (RF9). */
    val calificacion: Int? = null,
    /** Comentario opcional de esa calificación. */
    val comentario: String? = null,
    /** Calificación que la otra parte le dio al usuario (RF9 es mutua). */
    val calificacionRecibida: Int? = null,
    /** Solicitud de la que nació la sesión al ser aceptada (RF6). */
    val solicitudId: String? = null
) {
    val proxima: Boolean get() = estado == EstadoSesion.PROGRAMADA
    /** La sesión ocurrió; las canceladas y las sin asistencia no se califican ni muestran nota. */
    val completada: Boolean get() = estado == EstadoSesion.COMPLETADA
    /** Solo se califica una sesión que ocurrió. */
    val porCalificar: Boolean get() = completada && calificacion == null
}

data class Chat(
    /** Coincide con el id de la otra parte para navegar directamente a la conversación. */
    val id: String,
    val nombre: String,
    val ultimoMensaje: String,
    val hora: String,
    val noLeidos: Int,
    val enLinea: Boolean,
    /** La sesión ya terminó: el historial se conserva pero no admite mensajes nuevos (RF8). */
    val soloLectura: Boolean = false,
    /** true si la conversación es con un estudiante (el usuario actúa como tutor). */
    val comoTutor: Boolean = false
)

data class Mensaje(
    val id: String,
    val chatId: String,
    val texto: String,
    val hora: String,
    /** true si lo envía el usuario actual. */
    val mio: Boolean
)

/** Hora de la disponibilidad del tutor en sesión. Sin modalidades = no disponible. */
data class HoraDisponible(
    val hora: String,
    val modalidades: Set<Modalidad> = emptySet()
) {
    val seleccionada: Boolean get() = modalidades.isNotEmpty()
}

data class DiaDisponibilidad(
    val dia: String,
    val horas: List<HoraDisponible>
)

/** Horas que el tutor tiene publicadas en la semana. */
val List<DiaDisponibilidad>.horasPublicadas: Int
    get() = sumOf { d -> d.horas.count { it.seleccionada } }

/** Días de la semana que tienen al menos una hora publicada, en orden de calendario. */
val List<DiaDisponibilidad>.diasPublicados: List<DiaSemana>
    get() = filter { d -> d.horas.any { it.seleccionada } }
        .mapNotNull { d -> DiaSemana.entries.firstOrNull { it.nombre == d.dia } }

data class Usuario(
    val id: String,
    val nombre: String,
    val handle: String,
    val correo: String,
    val rol: Rol,
    val universidad: String = "",
    val carrera: String = "",
    /** Perfil público como tutor. */
    val bio: String = "",
    val materias: List<String> = emptyList()
)

/** Cifras del usuario en el modo actual, para el Perfil y el Historial. */
data class ResumenActividad(
    val sesiones: Int,
    val horas: Double,
    val porCalificar: Int,
    /** Promedio de lo que la otra parte le ha calificado; null sin calificaciones. */
    val promedioRecibido: Double?,
    val totalResenas: Int,
    val personas: Int,
    /** Materias de las sesiones completadas, la más frecuente primero. */
    val materias: List<String> = emptyList()
)
