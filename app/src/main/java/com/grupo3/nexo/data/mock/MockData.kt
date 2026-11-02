package com.grupo3.nexo.data.mock

import com.grupo3.nexo.domain.model.Chat
import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.DiaSemana
import com.grupo3.nexo.domain.model.DiaSemana.DOMINGO
import com.grupo3.nexo.domain.model.DiaSemana.JUEVES
import com.grupo3.nexo.domain.model.DiaSemana.LUNES
import com.grupo3.nexo.domain.model.DiaSemana.MARTES
import com.grupo3.nexo.domain.model.DiaSemana.MIERCOLES
import com.grupo3.nexo.domain.model.DiaSemana.SABADO
import com.grupo3.nexo.domain.model.DiaSemana.VIERNES
import com.grupo3.nexo.domain.model.EstadoSesion.CANCELADA
import com.grupo3.nexo.domain.model.EstadoSesion.COMPLETADA
import com.grupo3.nexo.domain.model.EstadoSesion.NO_ASISTIO
import com.grupo3.nexo.domain.model.EstadoSesion.PROGRAMADA
import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.FranjaTutor
import com.grupo3.nexo.domain.model.HoraDisponible
import com.grupo3.nexo.domain.model.Mensaje
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Modalidad.PRESENCIAL
import com.grupo3.nexo.domain.model.Modalidad.VIRTUAL
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.model.Usuario
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Fuente de datos simulada para el frontend (Entrega 2A).
 * Contexto: estudiantes universitarios de Bucaramanga (UIS y universidades vecinas),
 * tarifas en pesos colombianos. Cuando el backend esté listo se reemplaza por la
 * fuente remota sin tocar los repositorios ni la capa de dominio.
 */
object MockData {

    /** Fecha a tantos días de hoy (negativo hacia atrás), para que lo agendado siempre quede por delante. */
    private fun enDias(dias: Long): LocalDate = LocalDate.now().plusDays(dias)

    /** Fecha ya pasada del día y mes dados: la más reciente que no sea posterior a hoy. */
    private fun pasada(dia: Int, mes: Int): LocalDate =
        LocalDate.of(LocalDate.now().year, mes, dia).let { if (it.isAfter(LocalDate.now())) it.minusYears(1) else it }

    /** Hora del día a partir de "HH:mm". */
    private fun h(texto: String): LocalTime = LocalTime.parse(texto)

    /** Hoy a esa hora "HH:mm", para los mensajes del día. */
    private fun hoyA(texto: String): LocalDateTime = LocalDate.now().atTime(h(texto))

    /** Ayer por la tarde. */
    private val ayer: LocalDateTime get() = LocalDate.now().minusDays(1).atTime(18, 0)

    /** El lunes más reciente que no sea hoy ni ayer, para que se muestre como día de la semana. */
    private val ultimoLunes: LocalDateTime
        get() = LocalDate.now().minusDays(2).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atTime(18, 0)

    /** Nombre del día de la semana ("martes") a tantos días de hoy, para los mensajes que lo mencionan. */
    private fun diaEn(dias: Long): String =
        LocalDate.now().plusDays(dias).dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "CO"))

    /** Momento de envío de una solicitud, a tantas horas de ahora, para que el plazo de respuesta sea creíble. */
    private fun haceHoras(horas: Long): LocalDateTime = LocalDateTime.now().minusHours(horas)

    private const val UIS = "Universidad Industrial de Santander"
    private const val UNAB = "Universidad Autónoma de Bucaramanga"
    private const val UTS = "Unidades Tecnológicas de Santander"
    private const val UPB = "Universidad Pontificia Bolivariana, Bucaramanga"
    private const val USTA = "Universidad Santo Tomás, Bucaramanga"

    val usuario = Usuario(
        id = "u1",
        nombre = "Diego",
        handle = "@diego",
        correo = "diego@correo.uis.edu.co",
        rol = Rol.ESTUDIANTE,
        universidad = UIS,
        carrera = "Ingeniería de Sistemas",
        // Diego también enseña: su perfil de tutor se muestra con bio y materias
        bio = "Estudiante de sistemas de sexto semestre. Explico cálculo y programación con ejercicios cortos y mucha práctica.",
        materias = listOf("Cálculo I", "Programación")
    )

    private fun franja(dia: DiaSemana, hora: LocalTime, vararg modalidades: Modalidad) =
        FranjaTutor(dia, hora, modalidades.toSet())

    val tutores = listOf(
        Tutor(
            id = "t1", nombre = "Ana Torres", handle = "@anatorres",
            bio = "Ayudo a desaprender el miedo a los números. Sesiones prácticas con ejercicios de parcial y trucos para resolver rápido.",
            universidad = UIS,
            materias = listOf("Cálculo I", "Álgebra", "Matemática Básica"),
            rating = 4.9, totalResenas = 87, sesionesTotales = 210, estudiantes = 96,
            precioHora = 25_000.0, enLinea = true,
            horario = listOf(
                franja(LUNES, h("10:00"), VIRTUAL), franja(LUNES, h("16:00"), VIRTUAL, PRESENCIAL),
                franja(MIERCOLES, h("10:00"), VIRTUAL), franja(JUEVES, h("18:00"), VIRTUAL),
                franja(SABADO, h("10:00"), VIRTUAL, PRESENCIAL)
            )
        ),
        Tutor(
            id = "t2", nombre = "Carlos Mendoza", handle = "@cmendoza",
            bio = "Desarrollador full-stack y monitor de programación. Aprendes programando desde la primera sesión: proyectos reales, no diapositivas.",
            universidad = UIS,
            materias = listOf("Programación", "Python", "Java"),
            rating = 4.8, totalResenas = 64, sesionesTotales = 175, estudiantes = 78,
            precioHora = 30_000.0, enLinea = true,
            horario = listOf(
                franja(MARTES, h("18:00"), VIRTUAL), franja(MIERCOLES, h("18:00"), VIRTUAL),
                franja(VIERNES, h("14:00"), PRESENCIAL), franja(SABADO, h("08:00"), VIRTUAL)
            )
        ),
        Tutor(
            id = "t3", nombre = "Lucía Fernández", handle = "@lucfer",
            bio = "Física sin fórmulas mágicas: primero el concepto, después la matemática. Preparación para parciales y laboratorios.",
            universidad = UNAB,
            materias = listOf("Física I", "Estadística"),
            rating = 4.9, totalResenas = 52, sesionesTotales = 130, estudiantes = 61,
            precioHora = 28_000.0, enLinea = true,
            horario = listOf(
                franja(LUNES, h("15:00"), PRESENCIAL), franja(MARTES, h("10:00"), VIRTUAL, PRESENCIAL),
                franja(JUEVES, h("15:00"), PRESENCIAL), franja(VIERNES, h("09:00"), VIRTUAL)
            )
        ),
        Tutor(
            id = "t4", nombre = "María López", handle = "@marlopez",
            bio = "Conversación en inglés de B2 a C1 y redacción académica. Corrijo tus textos en vivo durante la sesión.",
            universidad = UNAB,
            materias = listOf("Inglés", "Redacción"),
            rating = 4.7, totalResenas = 38, sesionesTotales = 98, estudiantes = 54,
            precioHora = 22_000.0, enLinea = false,
            horario = listOf(
                franja(LUNES, h("07:00"), VIRTUAL), franja(MIERCOLES, h("07:00"), VIRTUAL),
                franja(SABADO, h("09:00"), VIRTUAL)
            )
        ),
        Tutor(
            id = "t5", nombre = "Pablo Sánchez", handle = "@pablos",
            bio = "Diseño de productos digitales con Figma. De cero a portafolio en un ciclo de sesiones.",
            universidad = UPB,
            materias = listOf("Diseño UI/UX", "Figma"),
            rating = 4.8, totalResenas = 41, sesionesTotales = 112, estudiantes = 47,
            precioHora = 35_000.0, enLinea = true,
            horario = listOf(
                franja(MARTES, h("19:00"), VIRTUAL), franja(JUEVES, h("19:00"), VIRTUAL),
                franja(SABADO, h("14:00"), PRESENCIAL)
            )
        ),
        Tutor(
            id = "t6", nombre = "Sofía García", handle = "@sofiag",
            bio = "Química general y biología con modelos y esquemas. Ideal para primer semestre.",
            universidad = USTA,
            materias = listOf("Química", "Biología"),
            rating = 4.6, totalResenas = 29, sesionesTotales = 76, estudiantes = 39,
            precioHora = 20_000.0, enLinea = false,
            horario = listOf(
                franja(LUNES, h("14:00"), PRESENCIAL), franja(MIERCOLES, h("14:00"), PRESENCIAL),
                franja(VIERNES, h("16:00"), VIRTUAL, PRESENCIAL)
            )
        ),
        Tutor(
            id = "t7", nombre = "Diego Ramírez", handle = "@dramirez",
            bio = "Bases de datos y SQL: desde el primer SELECT hasta modelar un sistema completo.",
            universidad = UIS,
            materias = listOf("Bases de Datos", "SQL"),
            rating = 4.8, totalResenas = 45, sesionesTotales = 103, estudiantes = 52,
            precioHora = 30_000.0, enLinea = true,
            horario = listOf(
                franja(MARTES, h("16:00"), VIRTUAL), franja(MIERCOLES, h("16:00"), VIRTUAL, PRESENCIAL),
                franja(JUEVES, h("20:00"), VIRTUAL), franja(DOMINGO, h("10:00"), VIRTUAL)
            )
        ),
        Tutor(
            id = "t8", nombre = "Elena Vargas", handle = "@elenav",
            bio = "Contabilidad y economía explicadas con casos reales. Aprendes cuadrando balances, no memorizando.",
            universidad = UTS,
            materias = listOf("Contabilidad", "Economía"),
            rating = 4.7, totalResenas = 33, sesionesTotales = 89, estudiantes = 44,
            precioHora = 26_000.0, enLinea = false,
            horario = listOf(
                franja(MARTES, h("08:00"), PRESENCIAL), franja(JUEVES, h("08:00"), PRESENCIAL),
                franja(SABADO, h("15:00"), VIRTUAL)
            )
        )
    )

    /** Reseñas de cada tutor, de la más reciente a la más antigua (RF4). */
    val resenas = listOf(
        Resena("r1", "t1", "Valeria R.", "@vale.r", "Ana explica paso a paso y nunca te hace sentir tonto por preguntar. Pasé Cálculo I con 4,5.", 5, enDias(-14)),
        Resena("r2", "t1", "Jorge L.", "@jorgel", "Sus trucos para los parciales son oro. Muy puntual y preparada.", 5, enDias(-30)),
        Resena("r3", "t1", "Camila T.", "@camt", "Buena explicación, aunque a veces la sesión se pasa del tiempo (para bien).", 4, enDias(-60)),
        Resena("r4", "t2", "Sebastián M.", "@sebasm", "Armamos una API completa en tres sesiones. Aprendí más que en todo el semestre.", 5, enDias(-21)),
        Resena("r5", "t2", "Andrea P.", "@andrep", "Carlos se adapta a tu ritmo. Recomendadísimo para Python desde cero.", 5, enDias(-30)),
        Resena("r6", "t2", "Luis F.", "@luisf", "Muy bueno, aunque me gustarían más ejercicios para la casa.", 4, enDias(-60)),
        Resena("r7", "t3", "Renzo C.", "@renzc", "La física por fin tiene sentido. Los diagramas antes de las fórmulas ayudan muchísimo.", 5, enDias(-7)),
        Resena("r8", "t3", "Marta G.", "@martag", "Explica estadística con ejemplos del día a día. Excelente.", 5, enDias(-21)),
        Resena("r9", "t4", "Iván R.", "@ivanr", "Mi speaking mejoró muchísimo en un mes. Corrige con paciencia.", 5, enDias(-14)),
        Resena("r10", "t4", "Paola D.", "@paolad", "Buenas sesiones de redacción; ojalá más práctica de listening.", 4, enDias(-30)),
        Resena("r11", "t5", "Bruno S.", "@brunos", "Con el portafolio que armé en Figma conseguí la práctica. Gracias, Pablo.", 5, enDias(-7)),
        Resena("r12", "t5", "Nadia K.", "@nadiak", "Metodología clara y retroalimentación directa sobre tus diseños.", 5, enDias(-60)),
        Resena("r13", "t7", "Frank O.", "@franko", "SQL dejó de dar miedo. El proyecto final lo usé en toda la materia.", 5, enDias(-14)),
        // Reseñas que recibió Diego como tutor
        Resena("r14", "u1", "Renzo Cruz", "@renzc", "Me explicó funciones con ejemplos de mi propio taller. Muy claro y paciente.", 5, enDias(-3)),
        Resena("r15", "u1", "Valeria Ríos", "@vale.r", "Repasamos límites antes del parcial y saqué 4,3. Lo recomiendo.", 5, enDias(-7)),
        Resena("r16", "u1", "Jorge Luna", "@jorgel", "Buena sesión de recursión, aunque empezamos un poco tarde.", 4, enDias(-21))
    )

    val solicitudes = listOf(
        // Recibidas por Diego en modo tutor (enseña Cálculo I y Programación).
        Solicitud("s1", "u1", "Diego", "e1", "Valeria Ríos", "Cálculo I", "Límites y continuidad", enDias(2), h("10:00"), VIRTUAL, EstadoSolicitud.PENDIENTE, "Hola, ¿podemos repasar antes del parcial?", enviada = haceHoras(2)),
        Solicitud("s2", "u1", "Diego", "e2", "Jorge Luna", "Programación", "Recursión en Python", enDias(3), h("16:00"), PRESENCIAL, EstadoSolicitud.PENDIENTE, "Necesito ayuda con el taller 3.", enviada = haceHoras(6)),
        Solicitud("s3", "u1", "Diego", "e3", "Camila Torres", "Cálculo I", "Derivadas implícitas", enDias(4), h("08:00"), VIRTUAL, EstadoSolicitud.PENDIENTE, "¿Tienes espacio esta semana?", enviada = haceHoras(22)),
        Solicitud("s4", "u1", "Diego", "e4", "Andrea Prado", "Programación", "Listas y diccionarios", enDias(2), h("18:00"), VIRTUAL, EstadoSolicitud.ACEPTADA, "Gracias, nos vemos por Meet."),
        Solicitud("s5", "u1", "Diego", "e5", "Bruno Salas", "Cálculo I", "Integrales", pasada(18, 10), h("11:00"), PRESENCIAL, EstadoSolicitud.RECHAZADA, "¿Podemos vernos en la biblioteca?", motivoRechazo = "Ese horario ya estaba reservado."),
        Solicitud("s6", "u1", "Diego", "e6", "Renzo Cruz", "Programación", "Funciones", pasada(14, 10), h("15:00"), VIRTUAL, EstadoSolicitud.FINALIZADA, "Sesión completada. ¡Gracias!"),
        // Enviadas por Diego en modo estudiante.
        Solicitud("s7", "t2", "Carlos Mendoza", "u1", "Diego", "Python", "Funciones y listas", enDias(6), h("18:00"), VIRTUAL, EstadoSolicitud.PENDIENTE, "Hola Carlos, quiero repasar para el quiz.", enviada = haceHoras(21)),
        Solicitud("s8", "t3", "Lucía Fernández", "u1", "Diego", "Física I", "Cinemática", enDias(3), h("15:00"), PRESENCIAL, EstadoSolicitud.ACEPTADA, "Confirmada, nos vemos en la biblioteca."),
        Solicitud("s9", "t4", "María López", "u1", "Diego", "Inglés", "Speaking para entrevista", pasada(12, 10), h("09:00"), VIRTUAL, EstadoSolicitud.RECHAZADA, "¿Podemos practicar una entrevista?", motivoRechazo = "Esa semana estoy de viaje, ¿la siguiente?"),
        Solicitud("s10", "t1", "Ana Torres", "u1", "Diego", "Cálculo I", "Límites y continuidad", enDias(1), h("10:00"), VIRTUAL, EstadoSolicitud.ACEPTADA, "¿Podemos repasar los problemas de la guía 4?"),
        Solicitud("s11", "t7", "Diego Ramírez", "u1", "Diego", "SQL", "Modelo entidad–relación", enDias(5), h("16:00"), VIRTUAL, EstadoSolicitud.ACEPTADA, "Quiero revisar el modelo del proyecto."),
        // Sin respuesta dentro del plazo: expiradas.
        Solicitud("s12", "u1", "Diego", "e7", "Sara Gómez", "Programación", "Arreglos y ciclos", enDias(1), h("09:00"), VIRTUAL, EstadoSolicitud.EXPIRADA, "Hola, ¿tienes espacio para repasar ciclos?", enviada = haceHoras(30)),
        Solicitud("s13", "t5", "Pablo Sánchez", "u1", "Diego", "Diseño UI/UX", "Prototipo en Figma", enDias(2), h("14:00"), VIRTUAL, EstadoSolicitud.EXPIRADA, "Quisiera una revisión de mi prototipo.", enviada = haceHoras(28))
    )

    /** Solo hay chat con solicitudes aceptadas o sesiones ya terminadas (RF8). */
    val chats = listOf(
        Chat("t1", "Ana Torres", "Perfecto, nos vemos el ${diaEn(1)} a las 10 👍", hoyA("10:24"), 2, true),
        Chat("t3", "Lucía Fernández", "Recuerda traer los problemas del parcial", ayer, 1, true),
        Chat("t7", "Diego Ramírez", "¿Te parece si revisamos el modelo ER?", ultimoLunes, 0, true),
        Chat("t5", "Pablo Sánchez", "Mi portafolio va avanzando 🎉", pasada(7, 10).atTime(18, 0), 0, true, soloLectura = true),
        // Conversaciones con estudiantes, en modo tutor
        Chat("e4", "Andrea Prado", "Gracias, nos vemos por Meet.", hoyA("09:12"), 1, true, comoTutor = true),
        Chat("e6", "Renzo Cruz", "¡Gracias por la sesión!", pasada(14, 10).atTime(18, 0), 0, false, soloLectura = true, comoTutor = true),
        // Sesiones terminadas sin calificar: desde aquí se puede calificar a la otra parte (RF9)
        Chat("e2", "Jorge Luna", "Ya entendí la recursión, gracias.", pasada(25, 9).atTime(18, 0), 0, false, soloLectura = true, comoTutor = true),
        Chat("t4", "María López", "Gracias por la clase, ¡nos vemos!", pasada(30, 9).atTime(18, 0), 0, false, soloLectura = true)
    )

    val mensajes = listOf(
        Mensaje("m1", "t1", "Hola Ana, ¿tendrías disponibilidad esta semana?", hoyA("10:02"), true),
        Mensaje("m2", "t1", "¡Hola Diego! Sí, el ${diaEn(1)} a las 10 estoy libre.", hoyA("10:15"), false),
        Mensaje("m3", "t1", "Genial, ¿podemos repasar límites y continuidad?", hoyA("10:18"), true),
        Mensaje("m4", "t1", "Claro, prepara los problemas de la guía 4.", hoyA("10:21"), false),
        Mensaje("m5", "t1", "Perfecto, nos vemos el ${diaEn(1)} a las 10 👍", hoyA("10:24"), false),
        Mensaje("m6", "t3", "Hola Lucía, ¿nos vemos en la biblioteca central?", ayer, true),
        Mensaje("m7", "t3", "Sí, en el segundo piso.", ayer, false),
        Mensaje("m8", "t3", "Recuerda traer los problemas del parcial", ayer, false),
        Mensaje("m9", "t7", "¿Te parece si revisamos el modelo ER?", ultimoLunes, false),
        Mensaje("m10", "t5", "Gracias por la sesión, Pablo.", pasada(7, 10).atTime(18, 0), true),
        Mensaje("m11", "t5", "Mi portafolio va avanzando 🎉", pasada(7, 10).atTime(18, 0), false),
        Mensaje("m12", "e4", "Hola Diego, gracias por aceptar la hora. ¿Usamos Meet?", hoyA("09:05"), false),
        Mensaje("m13", "e4", "Sí, te envío el enlace un rato antes.", hoyA("09:10"), true),
        Mensaje("m14", "e4", "Gracias, nos vemos por Meet.", hoyA("09:12"), false),
        Mensaje("m15", "e6", "¡Gracias por la sesión!", pasada(14, 10).atTime(18, 0), false),
        Mensaje("m16", "e2", "Ya entendí la recursión, gracias.", pasada(25, 9).atTime(18, 0), false),
        Mensaje("m17", "t4", "Gracias por la clase, ¡nos vemos!", pasada(30, 9).atTime(18, 0), true)
    )

    /** Sesiones de Diego en sus dos modos: futuras (PROGRAMADA) y pasadas (historial, RF10). */
    val sesiones = listOf(
        // Como estudiante
        Sesion("x1", "t1", "Ana Torres", "Cálculo I", enDias(1), h("10:00"), 1.5, VIRTUAL, PROGRAMADA, solicitudId = "s10"),
        Sesion("x2", "t3", "Lucía Fernández", "Física I", enDias(3), h("15:00"), 1.0, PRESENCIAL, PROGRAMADA, solicitudId = "s8"),
        Sesion("x3", "t7", "Diego Ramírez", "SQL", enDias(5), h("16:00"), 1.0, VIRTUAL, PROGRAMADA, solicitudId = "s11"),
        Sesion("h1", "t3", "Lucía Fernández", "Física I", pasada(14, 10), h("15:00"), 1.5, PRESENCIAL, COMPLETADA, calificacion = 5, comentario = "Clarísima con los diagramas.", calificacionRecibida = 5),
        Sesion("h2", "t5", "Pablo Sánchez", "Figma", pasada(7, 10), h("11:00"), 2.0, VIRTUAL, COMPLETADA, calificacion = 5, calificacionRecibida = 5),
        Sesion("h3", "t4", "María López", "Inglés", pasada(30, 9), h("09:00"), 1.0, VIRTUAL, COMPLETADA, calificacionRecibida = 4),
        Sesion("h4", "t1", "Ana Torres", "Matemática Básica", pasada(23, 9), h("10:00"), 1.5, VIRTUAL, COMPLETADA, calificacion = 4),
        Sesion("h5", "t6", "Sofía García", "Química", pasada(16, 9), h("14:00"), 1.0, PRESENCIAL, COMPLETADA),
        Sesion("h6", "t2", "Carlos Mendoza", "Python", pasada(10, 9), h("18:00"), 1.0, VIRTUAL, CANCELADA),
        // Como tutor
        Sesion("y1", "e4", "Andrea Prado", "Programación", enDias(2), h("18:00"), 1.0, VIRTUAL, PROGRAMADA, comoTutor = true, solicitudId = "s4"),
        Sesion("g1", "e6", "Renzo Cruz", "Programación", pasada(14, 10), h("15:00"), 1.0, VIRTUAL, COMPLETADA, comoTutor = true, calificacion = 5, calificacionRecibida = 5),
        Sesion("g2", "e1", "Valeria Ríos", "Cálculo I", pasada(9, 10), h("10:00"), 1.5, VIRTUAL, COMPLETADA, comoTutor = true, calificacion = 5, calificacionRecibida = 5),
        Sesion("g3", "e2", "Jorge Luna", "Programación", pasada(25, 9), h("16:00"), 1.0, PRESENCIAL, COMPLETADA, comoTutor = true, calificacionRecibida = 4),
        Sesion("g4", "e3", "Camila Torres", "Cálculo I", pasada(18, 9), h("08:00"), 1.0, VIRTUAL, NO_ASISTIO, comoTutor = true)
    )

    private val horasDia = listOf(h("08:00"), h("10:00"), h("12:00"), h("14:00"), h("16:00"), h("18:00"), h("20:00"))

    private fun dia(nombre: String, vararg activas: Pair<LocalTime, Set<Modalidad>>) = DiaDisponibilidad(
        nombre,
        horasDia.map { h -> HoraDisponible(h, activas.firstOrNull { it.first == h }?.second.orEmpty()) }
    )

    private val v = setOf(VIRTUAL)
    private val p = setOf(PRESENCIAL)
    private val vp = setOf(VIRTUAL, PRESENCIAL)

    /** Disponibilidad del usuario en modo tutor (RF7): hora → modalidades. */
    val disponibilidad = listOf(
        dia("Lunes", h("10:00") to v, h("16:00") to vp),
        dia("Martes", h("12:00") to p),
        dia("Miércoles", h("08:00") to v, h("14:00") to vp),
        dia("Jueves", h("10:00") to v, h("18:00") to v),
        dia("Viernes", h("12:00") to p, h("20:00") to v),
        dia("Sábado", h("08:00") to vp),
        dia("Domingo")
    )
}
