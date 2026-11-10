package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.remote.AlmacenEnMemoria
import com.grupo3.nexo.data.remote.ColaConPerfil
import com.grupo3.nexo.data.remote.ErroresApi
import com.grupo3.nexo.data.remote.RolDto
import com.grupo3.nexo.data.remote.RutasFalsas
import com.grupo3.nexo.data.remote.apiDePrueba
import com.grupo3.nexo.data.remote.cuerpoPerfil
import com.grupo3.nexo.data.remote.cuerpoTexto
import com.grupo3.nexo.data.remote.enPrueba
import com.grupo3.nexo.data.remote.jsonDePrueba
import com.grupo3.nexo.data.remote.sesionDePrueba
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.horasRestantes
import com.grupo3.nexo.domain.usecase.ActualizarSolicitudUseCase
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class SolicitudRepositoryRemotoTest {
    private val servidor = MockWebServer()
    private val rutas = RutasFalsas()
    private val almacen = AlmacenEnMemoria(sesionDePrueba())
    private lateinit var tutores: TutorRepositoryRemoto
    private lateinit var sesiones: SesionRepositoryRemoto
    private lateinit var chats: ChatRepositoryRemoto
    private lateinit var repositorio: SolicitudRepositoryRemoto

    @Before
    fun preparar() {
        servidor.dispatcher = rutas
        servidor.start()
        val api = apiDePrueba(servidor.url("/"))
        val errores = ErroresApi(jsonDePrueba)
        val contexto = ContextoRemoto(almacen)
        tutores = TutorRepositoryRemoto(api, errores, contexto)
        chats = ChatRepositoryRemoto(api, errores, contexto, 60_000)
        sesiones = SesionRepositoryRemoto(api, errores, tutores, chats, { repositorio }, contexto)
        repositorio = SolicitudRepositoryRemoto(api, errores, sesiones, chats, contexto)
        rutas.responder("GET", "/api/solicitudes", "[${solicitud()}]")
        rutas.responder("GET", "/api/sesiones", "[]")
        rutas.responder("GET", "/api/usuarios/yo/resumen", RESUMEN)
        rutas.responder("GET", "/api/chats", "[]")
    }

    @After
    fun cerrar() = servidor.shutdown()

    private fun solicitud(estado: String = "PENDIENTE", id: String = "s-1") =
        """{"id":"$id","tutorId":"t-1","nombreTutor":"Ana Torres","estudianteId":"u-1","nombreEstudiante":"Diego",
            "materia":"Cálculo I","tema":"Integrales","fecha":"2026-11-03","hora":"08:00","modalidad":"VIRTUAL",
            "estado":"$estado","mensaje":"","enviada":"2026-11-02T14:30:00Z","venceEn":"2026-11-03T08:00:00-05:00"}"""

    private suspend fun cargarTodo() {
        repositorio.obtenerSolicitudes().first()
        sesiones.obtenerSesiones().first()
        sesiones.resumenDelServidor().first()
        chats.obtenerChats().first()
    }

    @Test
    fun `lee las solicitudes del modo actual y convierte las fechas a hora de Colombia`() = enPrueba {
        val s = repositorio.obtenerSolicitudes().first().single()

        assertEquals("/api/solicitudes?modo=ESTUDIANTE", rutas.peticiones.single().path)
        assertEquals(LocalDate.of(2026, 11, 3), s.fecha)
        assertEquals(LocalTime.of(8, 0), s.hora)
        assertEquals(Modalidad.VIRTUAL, s.modalidad)
        // 14:30 en UTC son las 09:30 en Colombia
        assertEquals(LocalDateTime.of(2026, 11, 2, 9, 30), s.enviada)
        assertEquals(LocalDateTime.of(2026, 11, 3, 8, 0), s.venceEn)
    }

    @Test
    fun `el plazo es venceEn del servidor, no enviada mas 24 h`() = enPrueba {
        val s = repositorio.obtenerSolicitudes().first().single()
        // Enviada a las 09:30 y la tutoría empieza a las 08:00 del día siguiente: 22,5 h, no 24
        assertEquals(22.5, s.horasRestantes(s.enviada), 1e-9)
    }

    @Test
    fun `una solicitud con un estado que la app no conoce no tumba la lista`() = enPrueba {
        rutas.responder("GET", "/api/solicitudes", "[${solicitud("EN_DISPUTA", "s-9")},${solicitud()}]")
        assertEquals(listOf("s-1"), repositorio.obtenerSolicitudes().first().map { it.id })
    }

    @Test
    fun `un fallo de lectura se propaga y reintentar vuelve a pedir`() = enPrueba {
        rutas.cortar("GET", "/api/solicitudes")
        try {
            repositorio.obtenerSolicitudes().first()
            fail("Debía fallar")
        } catch (e: IOException) {
            // Es lo que la pantalla muestra como error con "Reintentar"
        }
        rutas.responder("GET", "/api/solicitudes", "[${solicitud()}]")
        assertEquals(1, repositorio.obtenerSolicitudes().first().size)
    }

    @Test
    fun `aceptar refresca solicitudes, sesiones y chats, y todas las pantallas lo ven`() = enPrueba {
        cargarTodo()
        rutas.peticiones.clear()
        rutas.responder("POST", "/api/solicitudes/s-1/aceptar", ACEPTADA)
        rutas.responder("GET", "/api/solicitudes", "[${solicitud("ACEPTADA")}]")
        rutas.responder("GET", "/api/sesiones", "[$SESION]")
        rutas.responder("GET", "/api/chats", "[$CHAT]")

        repositorio.actualizarEstado("s-1", EstadoSolicitud.ACEPTADA)

        assertEquals(1, rutas.pedidas("GET", "/api/solicitudes").size)
        assertEquals(1, rutas.pedidas("GET", "/api/sesiones").size)
        assertEquals(1, rutas.pedidas("GET", "/api/chats").size)
        assertEquals(EstadoSolicitud.ACEPTADA, repositorio.obtenerSolicitudes().first().single().estado)
        assertEquals("se-1", sesiones.obtenerSesiones().first().single().id)
        assertEquals("c-1", chats.obtenerChats().first().single().id)
    }

    @Test
    fun `la orquestacion del caso de uso no hace otras llamadas porque el servidor ya creo la sesion y el chat`() = enPrueba {
        val auth = RutasConPerfil()
        cargarTodo()
        rutas.peticiones.clear()
        rutas.responder("POST", "/api/solicitudes/s-1/aceptar", ACEPTADA)
        rutas.responder("GET", "/api/solicitudes", "[${solicitud("ACEPTADA")}]")

        ActualizarSolicitudUseCase(repositorio, sesiones, chats, auth.repositorio)("s-1", EstadoSolicitud.ACEPTADA)

        assertEquals(1, rutas.peticiones.count { it.method == "POST" })
        rutas.responder("POST", "/api/solicitudes/s-1/cancelar", solicitud("CANCELADA"))
        ActualizarSolicitudUseCase(repositorio, sesiones, chats, auth.repositorio)("s-1", EstadoSolicitud.CANCELADA)
        assertEquals(2, rutas.peticiones.count { it.method == "POST" })
    }

    private inner class RutasConPerfil {
        val repositorio: AuthRepositoryRemoto
        init {
            rutas.responder("GET", ColaConPerfil.RUTA_PERFIL, cuerpoPerfil())
            repositorio = AuthRepositoryRemoto(apiDePrueba(servidor.url("/")), almacen, ErroresApi(jsonDePrueba))
        }
    }

    @Test
    fun `rechazar envia el motivo y sin motivo no envia null`() = enPrueba {
        rutas.responder("POST", "/api/solicitudes/s-1/rechazar", solicitud("RECHAZADA"))

        repositorio.actualizarEstado("s-1", EstadoSolicitud.RECHAZADA, "No tengo cupo")
        repositorio.actualizarEstado("s-1", EstadoSolicitud.RECHAZADA, " ")

        val cuerpos = rutas.pedidas("POST", "/api/solicitudes/s-1/rechazar").map { it.cuerpoTexto() }
        assertEquals(listOf("""{"motivo":"No tengo cupo"}""", "{}"), cuerpos)
    }

    @Test
    fun `un rechazo con codigo llega a la pantalla y la lista se pone al dia`() = enPrueba {
        repositorio.obtenerSolicitudes().first()
        rutas.fallar("POST", "/api/solicitudes/s-1/aceptar", 409, CodigoError.SOLICITUD_NO_PENDIENTE)
        rutas.responder("GET", "/api/solicitudes", "[${solicitud("CANCELADA")}]")
        try {
            repositorio.actualizarEstado("s-1", EstadoSolicitud.ACEPTADA)
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.SOLICITUD_NO_PENDIENTE, e.codigo)
        }
        assertEquals(EstadoSolicitud.CANCELADA, repositorio.obtenerSolicitudes().first().single().estado)
    }

    @Test
    fun `la app no pide un cambio de estado sin endpoint`() = enPrueba {
        try {
            repositorio.actualizarEstado("s-1", EstadoSolicitud.FINALIZADA)
            fail("Debía fallar")
        } catch (e: IllegalArgumentException) {
            assertTrue(rutas.pedidas("POST", "/api/solicitudes/s-1/finalizar").isEmpty())
        }
    }

    @Test
    fun `enviar manda el contrato, sin nombres ni plazos, y la solicitud aparece en la lista`() = enPrueba {
        rutas.responder("GET", "/api/solicitudes", "[]")
        repositorio.obtenerSolicitudes().first()
        rutas.responder("POST", "/api/solicitudes", solicitud(id = "s-2"), 201)
        rutas.responder("GET", "/api/solicitudes", "[${solicitud(id = "s-2")}]")

        repositorio.enviar("t-1", "Cálculo I", "Integrales", LocalDate.of(2026, 11, 3), LocalTime.of(8, 0), Modalidad.VIRTUAL, "")

        val cuerpo = rutas.pedidas("POST", "/api/solicitudes").single().cuerpoTexto()
        assertEquals(
            """{"tutorId":"t-1","materia":"Cálculo I","tema":"Integrales","fecha":"2026-11-03","hora":"08:00","modalidad":"VIRTUAL"}""",
            cuerpo
        )
        assertFalse(cuerpo.contains("null"))
        assertEquals("s-2", repositorio.obtenerSolicitudes().first().single().id)
    }

    @Test
    fun `enviar con precio propuesto lo manda en el cuerpo`() = enPrueba {
        rutas.responder("POST", "/api/solicitudes", solicitud(id = "s-3"), 201)
        rutas.responder("GET", "/api/solicitudes", "[${solicitud(id = "s-3")}]")

        repositorio.enviar("t-1", "Cálculo I", "Integrales", LocalDate.of(2026, 11, 3), LocalTime.of(8, 0), Modalidad.VIRTUAL, "", 20000.0)

        val cuerpo = rutas.pedidas("POST", "/api/solicitudes").single().cuerpoTexto()
        assertTrue(cuerpo.contains(""""precioPropuesto":20000.0"""))
    }

    @Test
    fun `una solicitud rechazada al enviar dice por que`() = enPrueba {
        rutas.fallar("POST", "/api/solicitudes", 422, CodigoError.HORA_NO_DISPONIBLE)
        try {
            repositorio.enviar("t-1", "Cálculo I", "Integrales", LocalDate.of(2026, 11, 3), LocalTime.of(8, 0), Modalidad.VIRTUAL, "")
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.HORA_NO_DISPONIBLE, e.codigo)
        }
    }

    @Test
    fun `al cambiar de modo se vuelve a pedir con el modo nuevo`() = enPrueba {
        repositorio.obtenerSolicitudes().first()
        almacen.guardar(sesionDePrueba(rol = RolDto.TUTOR))
        repositorio.obtenerSolicitudes().first()
        assertEquals(
            listOf("/api/solicitudes?modo=ESTUDIANTE", "/api/solicitudes?modo=TUTOR"),
            rutas.pedidas("GET", "/api/solicitudes").map { it.path }
        )
    }

    @Test
    fun `motivo vacio de rechazo se omite del dominio`() = enPrueba {
        assertNull(repositorio.obtenerSolicitudes().first().single().motivoRechazo)
    }

    private companion object {
        const val SESION = """{"id":"se-1","solicitudId":"s-1","contraparteId":"t-1","conQuien":"Ana","materia":"Cálculo I",
            "fecha":"2026-11-03","hora":"08:00","duracionHoras":1,"modalidad":"VIRTUAL","estado":"PROGRAMADA","comoTutor":false}"""
        const val CHAT = """{"id":"c-1","contraparteId":"t-1","nombre":"Ana","ultimoMensaje":"","noLeidos":0,"enLinea":false,
            "soloLectura":false,"comoTutor":false}"""
        const val RESUMEN = """{"sesiones":0,"horas":0,"porCalificar":0,"totalResenas":0,"personas":0,"materias":[]}"""
        val ACEPTADA = """{"solicitud":{"id":"s-1","tutorId":"t-1","nombreTutor":"Ana Torres","estudianteId":"u-1",
            "nombreEstudiante":"Diego","materia":"Cálculo I","tema":"Integrales","fecha":"2026-11-03","hora":"08:00",
            "modalidad":"VIRTUAL","estado":"ACEPTADA","mensaje":"","enviada":"2026-11-02T09:30:00-05:00",
            "venceEn":"2026-11-03T08:00:00-05:00"},"sesion":$SESION,"chat":$CHAT}"""
    }
}
