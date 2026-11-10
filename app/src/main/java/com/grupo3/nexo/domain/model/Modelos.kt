package com.grupo3.nexo.domain.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Modelos de dominio de Nexo.
 * Puros: sin dependencias de Android ni de framework.
 *
 * Fechas en tipos de java.time; una API REST las enviará en ISO-8601 (`2026-11-02`, `10:00`,
 * `2026-11-02T10:00:00`) y el formato para pantalla vive en ui/components/Formatos.kt.
 */

/** Horas que tiene el tutor para responder una solicitud antes de que expire (RF5, RF6). */
const val PLAZO_RESPUESTA_HORAS = 24

/** Con estas horas o menos para responder, el plazo se resalta como aviso. */
const val HORAS_AVISO_PLAZO = 3

private const val SEGUNDOS_POR_MINUTO = 60L
private const val MINUTOS_POR_HORA = 60.0

enum class Rol(val etiqueta: String) {
    ESTUDIANTE("Estudiante"),
    TUTOR("Tutor")
}

/**
 * Ciclo de vida de una solicitud (RF5, RF6). La sesión nace al pasar a [ACEPTADA] y tiene su propio
 * estado ([EstadoSesion]): FINALIZADA equivale a una sesión COMPLETADA.
 */
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

/** Franjas del día para filtrar por horario (HU-05).  */
enum class Franja(val etiqueta: String, val desde: Int, val hasta: Int) {
    MANANA("Mañana", 6, 11),
    TARDE("Tarde", 12, 17),
    NOCHE("Noche", 18, 23);

    /** Rango legible de la franja: "06:00–12:00". */
    val rango: String get() = String.format(Locale.ROOT, "%02d:00–%02d:00", desde, hasta + 1)

    fun incluye(hora: LocalTime): Boolean = hora.hour in desde..hasta
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
    val hora: LocalTime,
    val modalidades: Set<Modalidad>
)

/** Franjas agrupadas por día, en orden de calendario y con las horas de cada día ordenadas. */
val List<FranjaTutor>.porDia: List<Pair<DiaSemana, List<FranjaTutor>>>
    get() = groupBy { it.dia }.toSortedMap(compareBy { it.numero }).map { (dia, franjas) -> dia to franjas.sortedBy { it.hora } }

/**
 * Duraciones que se pueden pedir. El calendario va de dos en dos horas: una tutoría de 2 h termina justo cuando empieza
 * la siguiente hora publicada.
 */
val DURACIONES_SOLICITABLES = listOf(1, 2)

/** Tope de la tarifa y del precio propuesto, el mismo que valida el servidor. */
const val PRECIO_MAXIMO = 10_000_000.0

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
    val horario: List<FranjaTutor> = emptyList(),
    /** Recibidas como tutor en los últimos 90 días, de cualquier nivel. */
    val penalizaciones: Int = 0,
    /** Foto de perfil (Cloudinary o Google); null: se muestran las iniciales. */
    val fotoUrl: String? = null
) {
    /** Modalidades que el tutor ofrece en al menos una franja. */
    val modalidades: Set<Modalidad> get() = horario.flatMapTo(mutableSetOf()) { it.modalidades }
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

/** Si el tutor cumple los filtros, tal como los aplica el servidor (salvo las tildes del texto libre). */
fun Tutor.cumple(f: FiltroTutores): Boolean {
    val texto = f.consulta.trim()
    val coincideTexto = texto.isEmpty() || buildString {
        append(nombre); append(' ')
        append(materias.joinToString(" ")); append(' ')
        append(universidad)
    }.contains(texto, ignoreCase = true)
    val coincideMateria = f.materias.isEmpty() ||
        materias.any { materia -> f.materias.any { materia.contains(it, ignoreCase = true) } }
    // Modalidad y franja se evalúan sobre la misma franja del horario: "virtual en la noche"
    // exige una hora nocturna que sea virtual, no una virtual y otra nocturna.
    val coincideHorario = (f.modalidad == null && f.franja == null) || horario.any { franja ->
        (f.modalidad == null || f.modalidad in franja.modalidades) &&
            (f.franja == null || f.franja.incluye(franja.hora))
    }
    val coincideLinea = !f.soloEnLinea || enLinea
    return coincideTexto && coincideMateria && coincideHorario && coincideLinea
}

/** Una fecha concreta en la que el tutor tiene al menos una franja libre (RF5). */
data class DiaSolicitable(val fecha: LocalDate, val franjas: List<FranjaTutor>)

/** Primer nombre de un nombre completo ("Laura Gómez" -> "Laura"). */
val String.primerNombre: String get() = substringBefore(' ')

data class Resena(
    val id: String,
    val tutorId: String,
    val autor: String,
    val handleAutor: String,
    val texto: String,
    val estrellas: Int,
    /** Día en que se publicó; la pantalla lo muestra relativo ("hace 2 sem"). */
    val fecha: LocalDate
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
    val fecha: LocalDate,
    val hora: LocalTime,
    val modalidad: Modalidad,
    val estado: EstadoSolicitud,
    val mensaje: String,
    /** Motivo opcional que da el tutor al rechazar (RF6). */
    val motivoRechazo: String? = null,
    /** Momento en que el estudiante la envió. */
    val enviada: LocalDateTime,
    /** Límite para responder: 24 h tras el envío o el inicio de la tutoría, lo que llegue antes; lo fija el servidor (RF6). */
    val venceEn: LocalDateTime = enviada.plusHours(PLAZO_RESPUESTA_HORAS.toLong()),
    /** Del estudiante como estudiante, en los últimos 90 días; ayuda al tutor a decidir. */
    val penalizacionesEstudiante: Int = 0,
    /** Precio por hora que propuso el estudiante; null si aceptó la tarifa del tutor. Al aceptar, el tutor acepta este precio. */
    val precioPropuesto: Double? = null,
    /** Horas que durará la tutoría: 1 o 2. */
    val duracionHoras: Int = 1
)

/** Horas que quedan para que el tutor responda, contando minutos completos; 0 o menos si el plazo ya venció. */
fun Solicitud.horasRestantes(ahora: LocalDateTime = LocalDateTime.now()): Double =
    -Math.floorDiv(Duration.between(venceEn, ahora).seconds, SEGUNDOS_POR_MINUTO) / MINUTOS_POR_HORA

/** Una pendiente con el plazo vencido se trata como expirada; las demás quedan igual. */
fun Solicitud.conPlazoAplicado(ahora: LocalDateTime = LocalDateTime.now()): Solicitud =
    if (estado == EstadoSolicitud.PENDIENTE && horasRestantes(ahora) <= 0.0) copy(estado = EstadoSolicitud.EXPIRADA) else this

/** Sigue en curso: espera respuesta o ya se aceptó. Las demás están cerradas. */
val Solicitud.activa: Boolean get() = estado == EstadoSolicitud.PENDIENTE || estado == EstadoSolicitud.ACEPTADA

/** Cuántas solicitudes esperan respuesta. */
fun List<Solicitud>.contarPendientes(): Int = count { it.estado == EstadoSolicitud.PENDIENTE }

/** El servidor solo deja cancelar antes de la hora: una sesión que ya empezó solo conserva el chat. */
fun Solicitud.acciones(esTutor: Boolean, ahora: LocalDateTime = LocalDateTime.now()): AccionesSolicitud = when {
    esTutor && estado == EstadoSolicitud.PENDIENTE -> AccionesSolicitud.RESPONDER
    esTutor && estado == EstadoSolicitud.ACEPTADA -> AccionesSolicitud.ABRIR_CHAT
    !esTutor && estado == EstadoSolicitud.ACEPTADA && !LocalDateTime.of(fecha, hora).isAfter(ahora) -> AccionesSolicitud.ABRIR_CHAT
    !esTutor && estado == EstadoSolicitud.ACEPTADA -> AccionesSolicitud.ABRIR_CHAT_O_CANCELAR
    !esTutor && estado == EstadoSolicitud.PENDIENTE -> AccionesSolicitud.CANCELAR
    !esTutor && (estado == EstadoSolicitud.RECHAZADA || estado == EstadoSolicitud.EXPIRADA) -> AccionesSolicitud.VER_OTROS
    else -> AccionesSolicitud.NINGUNA
}

/** Hora en que termina una sesión: 15:00 y 1,5 h → 16:30 (pasada la medianoche vuelve a empezar el día). */
fun horaFin(hora: LocalTime, horas: Double): LocalTime = hora.plusMinutes((horas * 60).roundToInt().toLong())

/** Día y hora de la sesión como un solo momento. */
val Sesion.inicio: LocalDateTime get() = fecha.atTime(hora)

/** Orden cronológico de las sesiones, de la más antigua a la más reciente. */
val PorInicio: Comparator<Sesion> = compareBy { it.inicio }

data class Sesion(
    val id: String,
    /** Id de la otra parte: el tutor si el usuario fue estudiante, el estudiante si fue tutor. */
    val contraparteId: String,
    val conQuien: String,
    val materia: String,
    val fecha: LocalDate,
    val hora: LocalTime,
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
    val solicitudId: String? = null,
    /** Presencial: el tutor ya llegó y envió el PIN. */
    val pinEnviado: Boolean = false,
    /** Lo ve solo el estudiante, desde que el tutor lo envía hasta que empieza la sesión. */
    val pin: String? = null,
    /** Presencial: el tutor ya escribió el PIN correcto. */
    val iniciada: Boolean = false,
    /** Presencial sin iniciar: pasado este momento se cancela y alguien queda penalizado. */
    val limite: LocalDateTime? = null,
    /** Nueva hora, si se pactó un retardo por chat. La sesión sigue terminando a la misma hora. */
    val horaPactada: LocalTime? = null,
    /** Si el retardo pactado es del usuario. Null si no hubo retardo. */
    val retardoMio: Boolean? = null,
    /** Por qué se canceló, si no fue el estudiante quien lo hizo antes de la hora. */
    val cierre: CierreSesion? = null,
    /** La penalización que esta sesión le dejó al usuario. */
    val penalizacion: NivelPenalizacion? = null,
    /** Propuesta por el chat que falta responder. */
    val acuerdo: AcuerdoPendiente? = null,
    /** En qué va la revisión que pidió el usuario sobre su penalización. */
    val revision: EstadoRevision? = null,
    val puedePedirRevision: Boolean = false
) {
    val proxima: Boolean get() = estado == EstadoSesion.PROGRAMADA
    /** La sesión ocurrió; las canceladas y las sin asistencia no se califican ni muestran nota. */
    val completada: Boolean get() = estado == EstadoSesion.COMPLETADA
    /** Solo se califica una sesión que ocurrió. */
    val porCalificar: Boolean get() = completada && calificacion == null
}

enum class EstadoRevision { ABIERTA, ACEPTADA, RECHAZADA }

enum class CierreSesion { TUTOR_NO_LLEGO, ESTUDIANTE_NO_LLEGO, ACORDADA }

enum class NivelPenalizacion { NORMAL, GRAVE }

enum class TipoAcuerdo { RETARDO, CANCELACION }

/** Lo que una de las dos personas propuso por el chat. [nuevaHora] solo en un retardo. */
data class AcuerdoPendiente(val tipo: TipoAcuerdo, val nuevaHora: LocalTime?, val propuestoPorMi: Boolean)

data class Chat(
    /** Identifica la conversación; la navegación hacia ella va por este id. */
    val id: String,
    /** Id de la otra persona (tutor o estudiante), para abrir su perfil o reportarla. */
    val contraparteId: String,
    val nombre: String,
    val ultimoMensaje: String,
    /** Momento del último mensaje; la pantalla lo muestra relativo ("10:24", "Ayer", "Lun", "7 oct"). Null si aún no hay mensajes. */
    val hora: LocalDateTime?,
    val noLeidos: Int,
    val enLinea: Boolean,
    /** La sesión ya terminó: el historial se conserva pero no admite mensajes nuevos (RF8). */
    val soloLectura: Boolean = false,
    /** true si la conversación es con un estudiante (el usuario actúa como tutor). */
    val comoTutor: Boolean = false,
    val fotoUrl: String? = null
)

data class Mensaje(
    val id: String,
    val chatId: String,
    val texto: String,
    val hora: LocalDateTime,
    /** true si lo envía el usuario actual. */
    val mio: Boolean,
    /** Foto o imagen adjunta; el texto puede venir vacío. */
    val imagenUrl: String? = null
)

/** Horas que ofrece el calendario semanal del tutor. */
val HORAS_CALENDARIO = listOf(8, 10, 12, 14, 16, 18, 20).map { LocalTime.of(it, 0) }

/** Semana completa sin ninguna hora publicada: punto de partida para quien aún no tiene calendario. */
fun calendarioVacio(): List<DiaDisponibilidad> =
    DiaSemana.entries.map { d -> DiaDisponibilidad(d.nombre, HORAS_CALENDARIO.map { HoraDisponible(it) }) }

/** Hora de la disponibilidad del tutor en sesión. Sin modalidades = no disponible. */
data class HoraDisponible(
    val hora: LocalTime,
    val modalidades: Set<Modalidad> = emptySet()
) {
    val seleccionada: Boolean get() = modalidades.isNotEmpty()
}

data class DiaDisponibilidad(
    val dia: String,
    val horas: List<HoraDisponible>
) {
    /** Horas de este día que el tutor ya publicó. */
    val horasActivas: Int get() = horas.count { it.seleccionada }
}

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
    val materias: List<String> = emptyList(),
    /** Tarifa por hora como tutor, en pesos; null si aún no la fija. */
    val precioHora: Double? = null,
    /** False si la cuenta solo entra con Google. */
    val tieneContrasena: Boolean = true,
    /** La propia o la de Google; null: se muestran las iniciales. */
    val fotoUrl: String? = null
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
    val materias: List<String> = emptyList(),
    /** Recibidas en este modo en los últimos 90 días, de cualquier nivel. */
    val penalizaciones: Int = 0,
    /** Hasta cuándo dura la pausa por una penalización grave; vale para los dos modos. */
    val suspendidaHasta: LocalDateTime? = null
)
