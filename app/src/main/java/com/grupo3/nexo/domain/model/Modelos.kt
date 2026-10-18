package com.grupo3.nexo.domain.model

/**
 * Modelos de dominio de Nexo.
 * Puros: sin dependencias de Android ni de framework.
 */

enum class Rol(val etiqueta: String) {
    ESTUDIANTE("Estudiante"),
    TUTOR("Tutor")
}

enum class EstadoSolicitud(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    ACEPTADA("Aceptada"),
    RECHAZADA("Rechazada"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada")
}

/** Ciclo de vida de una sesión de tutoría (RF10). */
enum class EstadoSesion(val etiqueta: String) {
    PROGRAMADA("Programada"),
    COMPLETADA("Completada"),
    CANCELADA("Cancelada"),
    NO_ASISTIO("No asistió")
}

/** Validación de la cuenta de tutor por el administrador (RF1, RF12). */
enum class EstadoVerificacion(val etiqueta: String) {
    PENDIENTE("En verificación"),
    VERIFICADO("Tutor verificado"),
    SUSPENDIDO("Cuenta suspendida")
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
    val horario: List<FranjaTutor> = emptyList(),
    /** Colores del gradiente de identidad visual del tutor (ARGB). */
    val colorPrimario: Long,
    val colorSecundario: Long
) {
    /** Modalidades que el tutor ofrece en al menos una franja. */
    val modalidades: Set<Modalidad> get() = horario.flatMapTo(mutableSetOf()) { it.modalidades }
}

data class Resena(
    val tutorId: String,
    val autor: String,
    val handleAutor: String,
    val texto: String,
    val estrellas: Int,
    val fecha: String
)

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
    val motivoRechazo: String? = null
)

private val mesesCortos = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/**
 * Clave para ordenar fechas cortas como "24 oct" o "30 sept" y horas "HH:mm" del mismo año académico.
 * Los meses anteriores a julio se cuentan como del año siguiente (el semestre cruza el año).
 */
fun claveFecha(fecha: String, hora: String = "00:00"): Int {
    val partes = fecha.trim().split(" ")
    val dia = partes.getOrNull(0)?.toIntOrNull() ?: 0
    val mes = mesesCortos.indexOfFirst { partes.getOrNull(1)?.lowercase()?.startsWith(it) == true } + 1
    val anio = if (mes in 1..6) 1 else 0
    val minutos = hora.split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (it.getOrNull(1)?.toIntOrNull() ?: 0) }
    return ((anio * 12 + mes) * 32 + dia) * 1440 + minutos
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
    val colorPrimario: Long,
    val colorSecundario: Long,
    /** Solicitud de la que nació la sesión al ser aceptada (RF6). */
    val solicitudId: String? = null
) {
    val proxima: Boolean get() = estado == EstadoSesion.PROGRAMADA
    /** Solo se califica una sesión que ocurrió. */
    val porCalificar: Boolean get() = estado == EstadoSesion.COMPLETADA && calificacion == null
}

data class Chat(
    /** Coincide con el id de la otra parte para navegar directamente a la conversación. */
    val id: String,
    val nombre: String,
    val ultimoMensaje: String,
    val hora: String,
    val noLeidos: Int,
    val enLinea: Boolean,
    val colorPrimario: Long,
    val colorSecundario: Long,
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

data class Usuario(
    val id: String,
    val nombre: String,
    val handle: String,
    val correo: String,
    val rol: Rol,
    val universidad: String = "",
    val carrera: String = "",
    /** null si nunca se registró como tutor; si no, el estado de su validación (RF12). */
    val verificacion: EstadoVerificacion? = null,
    /** Perfil público como tutor. */
    val bio: String = "",
    val materias: List<String> = emptyList(),
    val colorPrimario: Long = 0xFF0D3829,
    val colorSecundario: Long = 0xFF3F8F69
) {
    val puedeEnsenar: Boolean get() = verificacion == EstadoVerificacion.VERIFICADO
}

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
