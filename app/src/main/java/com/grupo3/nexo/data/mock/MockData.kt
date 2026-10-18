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
import com.grupo3.nexo.domain.model.EstadoVerificacion
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

/**
 * Fuente de datos simulada para el frontend (Entrega 2A).
 * Contexto: estudiantes universitarios de Bucaramanga (UIS y universidades vecinas),
 * tarifas en pesos colombianos. Cuando el backend esté listo se reemplaza por la
 * fuente remota sin tocar los repositorios ni la capa de dominio.
 */
object MockData {

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
        // Diego también enseña: su cuenta de tutor ya fue validada (RF12)
        verificacion = EstadoVerificacion.VERIFICADO,
        bio = "Estudiante de sistemas de sexto semestre. Explico cálculo y programación con ejercicios cortos y mucha práctica.",
        materias = listOf("Cálculo I", "Programación")
    )

    private fun franja(dia: DiaSemana, hora: String, vararg modalidades: Modalidad) =
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
                franja(LUNES, "10:00", VIRTUAL), franja(LUNES, "16:00", VIRTUAL, PRESENCIAL),
                franja(MIERCOLES, "10:00", VIRTUAL), franja(JUEVES, "18:00", VIRTUAL),
                franja(SABADO, "10:00", VIRTUAL, PRESENCIAL)
            ),
            colorPrimario = 0xFF1B5E3F, colorSecundario = 0xFF4E9C78
        ),
        Tutor(
            id = "t2", nombre = "Carlos Mendoza", handle = "@cmendoza",
            bio = "Desarrollador full-stack y monitor de programación. Aprendes programando desde la primera sesión: proyectos reales, no diapositivas.",
            universidad = UIS,
            materias = listOf("Programación", "Python", "Java"),
            rating = 4.8, totalResenas = 64, sesionesTotales = 175, estudiantes = 78,
            precioHora = 30_000.0, enLinea = true,
            horario = listOf(
                franja(MARTES, "18:00", VIRTUAL), franja(MIERCOLES, "18:00", VIRTUAL),
                franja(VIERNES, "14:00", PRESENCIAL), franja(SABADO, "08:00", VIRTUAL)
            ),
            colorPrimario = 0xFF155E63, colorSecundario = 0xFF4A9EA0
        ),
        Tutor(
            id = "t3", nombre = "Lucía Fernández", handle = "@lucfer",
            bio = "Física sin fórmulas mágicas: primero el concepto, después la matemática. Preparación para parciales y laboratorios.",
            universidad = UNAB,
            materias = listOf("Física I", "Estadística"),
            rating = 4.9, totalResenas = 52, sesionesTotales = 130, estudiantes = 61,
            precioHora = 28_000.0, enLinea = true,
            horario = listOf(
                franja(LUNES, "15:00", PRESENCIAL), franja(MARTES, "10:00", VIRTUAL, PRESENCIAL),
                franja(JUEVES, "15:00", PRESENCIAL), franja(VIERNES, "09:00", VIRTUAL)
            ),
            colorPrimario = 0xFF8A6A1F, colorSecundario = 0xFFC9A44C
        ),
        Tutor(
            id = "t4", nombre = "María López", handle = "@marlopez",
            bio = "Conversación en inglés de B2 a C1 y redacción académica. Corrijo tus textos en vivo durante la sesión.",
            universidad = UNAB,
            materias = listOf("Inglés", "Redacción"),
            rating = 4.7, totalResenas = 38, sesionesTotales = 98, estudiantes = 54,
            precioHora = 22_000.0, enLinea = false,
            horario = listOf(
                franja(LUNES, "07:00", VIRTUAL), franja(MIERCOLES, "07:00", VIRTUAL),
                franja(SABADO, "09:00", VIRTUAL)
            ),
            colorPrimario = 0xFF8C3B2E, colorSecundario = 0xFFC7714F
        ),
        Tutor(
            id = "t5", nombre = "Pablo Sánchez", handle = "@pablos",
            bio = "Diseño de productos digitales con Figma. De cero a portafolio en un ciclo de sesiones.",
            universidad = UPB,
            materias = listOf("Diseño UI/UX", "Figma"),
            rating = 4.8, totalResenas = 41, sesionesTotales = 112, estudiantes = 47,
            precioHora = 35_000.0, enLinea = true,
            horario = listOf(
                franja(MARTES, "19:00", VIRTUAL), franja(JUEVES, "19:00", VIRTUAL),
                franja(SABADO, "14:00", PRESENCIAL)
            ),
            colorPrimario = 0xFF2E4A62, colorSecundario = 0xFF6585A0
        ),
        Tutor(
            id = "t6", nombre = "Sofía García", handle = "@sofiag",
            bio = "Química general y biología con modelos y esquemas. Ideal para primer semestre.",
            universidad = USTA,
            materias = listOf("Química", "Biología"),
            rating = 4.6, totalResenas = 29, sesionesTotales = 76, estudiantes = 39,
            precioHora = 20_000.0, enLinea = false,
            horario = listOf(
                franja(LUNES, "14:00", PRESENCIAL), franja(MIERCOLES, "14:00", PRESENCIAL),
                franja(VIERNES, "16:00", VIRTUAL, PRESENCIAL)
            ),
            colorPrimario = 0xFF2F6B3A, colorSecundario = 0xFF7FAF6B
        ),
        Tutor(
            id = "t7", nombre = "Diego Ramírez", handle = "@dramirez",
            bio = "Bases de datos y SQL: desde el primer SELECT hasta modelar un sistema completo.",
            universidad = UIS,
            materias = listOf("Bases de Datos", "SQL"),
            rating = 4.8, totalResenas = 45, sesionesTotales = 103, estudiantes = 52,
            precioHora = 30_000.0, enLinea = true,
            horario = listOf(
                franja(MARTES, "16:00", VIRTUAL), franja(MIERCOLES, "16:00", VIRTUAL, PRESENCIAL),
                franja(JUEVES, "20:00", VIRTUAL), franja(DOMINGO, "10:00", VIRTUAL)
            ),
            colorPrimario = 0xFF1D4D5C, colorSecundario = 0xFF4C7F8C
        ),
        Tutor(
            id = "t8", nombre = "Elena Vargas", handle = "@elenav",
            bio = "Contabilidad y economía explicadas con casos reales. Aprendes cuadrando balances, no memorizando.",
            universidad = UTS,
            materias = listOf("Contabilidad", "Economía"),
            rating = 4.7, totalResenas = 33, sesionesTotales = 89, estudiantes = 44,
            precioHora = 26_000.0, enLinea = false,
            horario = listOf(
                franja(MARTES, "08:00", PRESENCIAL), franja(JUEVES, "08:00", PRESENCIAL),
                franja(SABADO, "15:00", VIRTUAL)
            ),
            colorPrimario = 0xFF6B3A55, colorSecundario = 0xFFA0688A
        )
    )

    /** Reseñas de cada tutor, de la más reciente a la más antigua (RF4). */
    val resenas = listOf(
        Resena("t1", "Valeria R.", "@vale.r", "Ana explica paso a paso y nunca te hace sentir tonto por preguntar. Pasé Cálculo I con 4,5.", 5, "hace 2 sem"),
        Resena("t1", "Jorge L.", "@jorgel", "Sus trucos para los parciales son oro. Muy puntual y preparada.", 5, "hace 1 mes"),
        Resena("t1", "Camila T.", "@camt", "Buena explicación, aunque a veces la sesión se pasa del tiempo (para bien).", 4, "hace 2 meses"),
        Resena("t2", "Sebastián M.", "@sebasm", "Armamos una API completa en tres sesiones. Aprendí más que en todo el semestre.", 5, "hace 3 sem"),
        Resena("t2", "Andrea P.", "@andrep", "Carlos se adapta a tu ritmo. Recomendadísimo para Python desde cero.", 5, "hace 1 mes"),
        Resena("t2", "Luis F.", "@luisf", "Muy bueno, aunque me gustarían más ejercicios para la casa.", 4, "hace 2 meses"),
        Resena("t3", "Renzo C.", "@renzc", "La física por fin tiene sentido. Los diagramas antes de las fórmulas ayudan muchísimo.", 5, "hace 1 sem"),
        Resena("t3", "Marta G.", "@martag", "Explica estadística con ejemplos del día a día. Excelente.", 5, "hace 3 sem"),
        Resena("t4", "Iván R.", "@ivanr", "Mi speaking mejoró muchísimo en un mes. Corrige con paciencia.", 5, "hace 2 sem"),
        Resena("t4", "Paola D.", "@paolad", "Buenas sesiones de redacción; ojalá más práctica de listening.", 4, "hace 1 mes"),
        Resena("t5", "Bruno S.", "@brunos", "Con el portafolio que armé en Figma conseguí la práctica. Gracias, Pablo.", 5, "hace 1 sem"),
        Resena("t5", "Nadia K.", "@nadiak", "Metodología clara y retroalimentación directa sobre tus diseños.", 5, "hace 2 meses"),
        Resena("t7", "Frank O.", "@franko", "SQL dejó de dar miedo. El proyecto final lo usé en toda la materia.", 5, "hace 2 sem"),
        // Reseñas que recibió Diego como tutor
        Resena("u1", "Renzo Cruz", "@renzc", "Me explicó funciones con ejemplos de mi propio taller. Muy claro y paciente.", 5, "hace 3 días"),
        Resena("u1", "Valeria Ríos", "@vale.r", "Repasamos límites antes del parcial y saqué 4,3. Lo recomiendo.", 5, "hace 1 sem"),
        Resena("u1", "Jorge Luna", "@jorgel", "Buena sesión de recursión, aunque empezamos un poco tarde.", 4, "hace 3 sem")
    )

    val solicitudes = listOf(
        // Recibidas por Diego en modo tutor (enseña Cálculo I y Programación).
        Solicitud("s1", "u1", "Diego", "e1", "Valeria Ríos", "Cálculo I", "Límites y continuidad", "24 oct", "10:00", VIRTUAL, EstadoSolicitud.PENDIENTE, "Hola, ¿podemos repasar antes del parcial del lunes?"),
        Solicitud("s2", "u1", "Diego", "e2", "Jorge Luna", "Programación", "Recursión en Python", "25 oct", "16:00", PRESENCIAL, EstadoSolicitud.PENDIENTE, "Necesito ayuda con el taller 3."),
        Solicitud("s3", "u1", "Diego", "e3", "Camila Torres", "Cálculo I", "Derivadas implícitas", "26 oct", "08:00", VIRTUAL, EstadoSolicitud.PENDIENTE, "¿Tienes espacio esta semana?"),
        Solicitud("s4", "u1", "Diego", "e4", "Andrea Prado", "Programación", "Listas y diccionarios", "20 oct", "18:00", VIRTUAL, EstadoSolicitud.ACEPTADA, "Gracias, nos vemos por Meet."),
        Solicitud("s5", "u1", "Diego", "e5", "Bruno Salas", "Cálculo I", "Integrales", "18 oct", "11:00", PRESENCIAL, EstadoSolicitud.RECHAZADA, "¿Podemos vernos en la biblioteca?", motivoRechazo = "Ese horario ya estaba reservado."),
        Solicitud("s6", "u1", "Diego", "e6", "Renzo Cruz", "Programación", "Funciones", "14 oct", "15:00", VIRTUAL, EstadoSolicitud.FINALIZADA, "Sesión completada. ¡Gracias!"),
        // Enviadas por Diego en modo estudiante.
        Solicitud("s7", "t2", "Carlos Mendoza", "u1", "Diego", "Python", "Funciones y listas", "28 oct", "18:00", VIRTUAL, EstadoSolicitud.PENDIENTE, "Hola Carlos, quiero repasar para el quiz."),
        Solicitud("s8", "t3", "Lucía Fernández", "u1", "Diego", "Física I", "Cinemática", "26 oct", "15:00", PRESENCIAL, EstadoSolicitud.ACEPTADA, "Confirmada, nos vemos en la biblioteca."),
        Solicitud("s9", "t4", "María López", "u1", "Diego", "Inglés", "Speaking para entrevista", "12 oct", "09:00", VIRTUAL, EstadoSolicitud.RECHAZADA, "¿Podemos practicar una entrevista?", motivoRechazo = "Esa semana estoy de viaje, ¿la siguiente?"),
        Solicitud("s10", "t1", "Ana Torres", "u1", "Diego", "Cálculo I", "Límites y continuidad", "24 oct", "10:00", VIRTUAL, EstadoSolicitud.ACEPTADA, "¿Podemos repasar los problemas de la guía 4?"),
        Solicitud("s11", "t7", "Diego Ramírez", "u1", "Diego", "SQL", "Modelo entidad–relación", "27 oct", "16:00", VIRTUAL, EstadoSolicitud.ACEPTADA, "Quiero revisar el modelo del proyecto.")
    )

    /** Solo hay chat con solicitudes aceptadas o sesiones ya terminadas (RF8). */
    val chats = listOf(
        Chat("t1", "Ana Torres", "Perfecto, nos vemos el sábado a las 10 👍", "10:24", 2, true, 0xFF1B5E3F, 0xFF4E9C78),
        Chat("t3", "Lucía Fernández", "Recuerda traer los problemas del parcial", "Ayer", 1, true, 0xFF8A6A1F, 0xFFC9A44C),
        Chat("t7", "Diego Ramírez", "¿Te parece si revisamos el modelo ER?", "Lun", 0, true, 0xFF1D4D5C, 0xFF4C7F8C),
        Chat("t5", "Pablo Sánchez", "Mi portafolio va avanzando 🎉", "7 oct", 0, true, 0xFF2E4A62, 0xFF6585A0, soloLectura = true),
        // Conversaciones con estudiantes, en modo tutor
        Chat("e4", "Andrea Prado", "Gracias, nos vemos por Meet.", "9:12", 1, true, 0xFF5B4A8A, 0xFF8E7FC0, comoTutor = true),
        Chat("e6", "Renzo Cruz", "¡Gracias por la sesión!", "14 oct", 0, false, 0xFF7A4B2A, 0xFFB98256, soloLectura = true, comoTutor = true)
    )

    val mensajes = listOf(
        Mensaje("m1", "t1", "Hola Ana, ¿tendrías disponibilidad esta semana?", "10:02", true),
        Mensaje("m2", "t1", "¡Hola Diego! Sí, el sábado a las 10 estoy libre.", "10:15", false),
        Mensaje("m3", "t1", "Genial, ¿podemos repasar límites y continuidad?", "10:18", true),
        Mensaje("m4", "t1", "Claro, prepara los problemas de la guía 4.", "10:21", false),
        Mensaje("m5", "t1", "Perfecto, nos vemos el sábado a las 10 👍", "10:24", false),
        Mensaje("m6", "t3", "Hola Lucía, ¿nos vemos en la biblioteca central?", "Ayer", true),
        Mensaje("m7", "t3", "Sí, en el segundo piso.", "Ayer", false),
        Mensaje("m8", "t3", "Recuerda traer los problemas del parcial", "Ayer", false),
        Mensaje("m9", "t7", "¿Te parece si revisamos el modelo ER?", "Lun", false),
        Mensaje("m10", "t5", "Gracias por la sesión, Pablo.", "7 oct", true),
        Mensaje("m11", "t5", "Mi portafolio va avanzando 🎉", "7 oct", false),
        Mensaje("m12", "e4", "Hola Diego, acepté la hora del lunes. ¿Usamos Meet?", "9:05", false),
        Mensaje("m13", "e4", "Sí, te envío el enlace un rato antes.", "9:10", true),
        Mensaje("m14", "e4", "Gracias, nos vemos por Meet.", "9:12", false),
        Mensaje("m15", "e6", "¡Gracias por la sesión!", "14 oct", false)
    )

    /** Sesiones de Diego en sus dos modos: futuras (PROGRAMADA) y pasadas (historial, RF10). */
    val sesiones = listOf(
        // Como estudiante
        Sesion("x1", "t1", "Ana Torres", "Cálculo I", "24 oct", "10:00", 1.5, VIRTUAL, PROGRAMADA, colorPrimario = 0xFF1B5E3F, colorSecundario = 0xFF4E9C78, solicitudId = "s10"),
        Sesion("x2", "t3", "Lucía Fernández", "Física I", "26 oct", "15:00", 1.0, PRESENCIAL, PROGRAMADA, colorPrimario = 0xFF8A6A1F, colorSecundario = 0xFFC9A44C, solicitudId = "s8"),
        Sesion("x3", "t7", "Diego Ramírez", "SQL", "27 oct", "16:00", 1.0, VIRTUAL, PROGRAMADA, colorPrimario = 0xFF1D4D5C, colorSecundario = 0xFF4C7F8C, solicitudId = "s11"),
        Sesion("h1", "t3", "Lucía Fernández", "Física I", "14 oct", "15:00", 1.5, PRESENCIAL, COMPLETADA, calificacion = 5, comentario = "Clarísima con los diagramas.", calificacionRecibida = 5, colorPrimario = 0xFF8A6A1F, colorSecundario = 0xFFC9A44C),
        Sesion("h2", "t5", "Pablo Sánchez", "Figma", "7 oct", "11:00", 2.0, VIRTUAL, COMPLETADA, calificacion = 5, calificacionRecibida = 5, colorPrimario = 0xFF2E4A62, colorSecundario = 0xFF6585A0),
        Sesion("h3", "t4", "María López", "Inglés", "30 sept", "09:00", 1.0, VIRTUAL, COMPLETADA, calificacionRecibida = 4, colorPrimario = 0xFF8C3B2E, colorSecundario = 0xFFC7714F),
        Sesion("h4", "t1", "Ana Torres", "Matemática Básica", "23 sept", "10:00", 1.5, VIRTUAL, COMPLETADA, calificacion = 4, colorPrimario = 0xFF1B5E3F, colorSecundario = 0xFF4E9C78),
        Sesion("h5", "t6", "Sofía García", "Química", "16 sept", "14:00", 1.0, PRESENCIAL, COMPLETADA, colorPrimario = 0xFF2F6B3A, colorSecundario = 0xFF7FAF6B),
        Sesion("h6", "t2", "Carlos Mendoza", "Python", "10 sept", "18:00", 1.0, VIRTUAL, CANCELADA, colorPrimario = 0xFF155E63, colorSecundario = 0xFF4A9EA0),
        // Como tutor
        Sesion("y1", "e4", "Andrea Prado", "Programación", "20 oct", "18:00", 1.0, VIRTUAL, PROGRAMADA, comoTutor = true, colorPrimario = 0xFF5B4A8A, colorSecundario = 0xFF8E7FC0, solicitudId = "s4"),
        Sesion("g1", "e6", "Renzo Cruz", "Programación", "14 oct", "15:00", 1.0, VIRTUAL, COMPLETADA, comoTutor = true, calificacion = 5, calificacionRecibida = 5, colorPrimario = 0xFF7A4B2A, colorSecundario = 0xFFB98256),
        Sesion("g2", "e1", "Valeria Ríos", "Cálculo I", "9 oct", "10:00", 1.5, VIRTUAL, COMPLETADA, comoTutor = true, calificacion = 5, calificacionRecibida = 5, colorPrimario = 0xFF2F5D7C, colorSecundario = 0xFF6A97B5),
        Sesion("g3", "e2", "Jorge Luna", "Programación", "25 sept", "16:00", 1.0, PRESENCIAL, COMPLETADA, comoTutor = true, calificacionRecibida = 4, colorPrimario = 0xFF4A6B2F, colorSecundario = 0xFF86A866),
        Sesion("g4", "e3", "Camila Torres", "Cálculo I", "18 sept", "08:00", 1.0, VIRTUAL, NO_ASISTIO, comoTutor = true, colorPrimario = 0xFF7C2F4E, colorSecundario = 0xFFB56A88)
    )

    private val horasDia = listOf("08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00")

    private fun dia(nombre: String, vararg activas: Pair<String, Set<Modalidad>>) = DiaDisponibilidad(
        nombre,
        horasDia.map { h -> HoraDisponible(h, activas.firstOrNull { it.first == h }?.second.orEmpty()) }
    )

    private val v = setOf(VIRTUAL)
    private val p = setOf(PRESENCIAL)
    private val vp = setOf(VIRTUAL, PRESENCIAL)

    /** Disponibilidad del usuario en modo tutor (RF7): hora → modalidades. */
    val disponibilidad = listOf(
        dia("Lunes", "10:00" to v, "16:00" to vp),
        dia("Martes", "12:00" to p),
        dia("Miércoles", "08:00" to v, "14:00" to vp),
        dia("Jueves", "10:00" to v, "18:00" to v),
        dia("Viernes", "12:00" to p, "20:00" to v),
        dia("Sábado", "08:00" to vp),
        dia("Domingo")
    )
}
