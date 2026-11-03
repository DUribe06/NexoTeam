package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.remote.AlmacenEnMemoria
import com.grupo3.nexo.data.remote.ErroresApi
import com.grupo3.nexo.data.remote.RolDto
import com.grupo3.nexo.data.remote.RutasFalsas
import com.grupo3.nexo.data.remote.apiDePrueba
import com.grupo3.nexo.data.remote.cuerpoTexto
import com.grupo3.nexo.data.remote.enPrueba
import com.grupo3.nexo.data.remote.jsonDePrueba
import com.grupo3.nexo.data.remote.sesionDePrueba
import com.grupo3.nexo.domain.model.AcuerdoPendiente
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.EstadoSesion
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.ResumenActividad
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.TipoAcuerdo
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.repository.AuthRepository
import com.grupo3.nexo.domain.usecase.ObtenerHistorialUseCase
import com.grupo3.nexo.domain.usecase.ObtenerResumenActividadUseCase
import com.grupo3.nexo.domain.usecase.ObtenerSesionesDelModoUseCase
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class SesionRepositoryRemotoTest {
    private val servidor = MockWebServer()
    private val rutas = RutasFalsas()
    private val almacen = AlmacenEnMemoria(sesionDePrueba())
    private lateinit var tutores: TutorRepositoryRemoto
    private lateinit var repositorio: SesionRepositoryRemoto

    @Before
    fun preparar() {
        servidor.dispatcher = rutas
        servidor.start()
        val api = apiDePrueba(servidor.url("/"))
        val errores = ErroresApi(jsonDePrueba)
        val contexto = ContextoRemoto(almacen)
        tutores = TutorRepositoryRemoto(api, errores, contexto)
        repositorio = SesionRepositoryRemoto(api, errores, tutores, contexto)
        rutas.responder("GET", "/api/sesiones", "[${sesion()}]")
        rutas.responder("GET", "/api/usuarios/yo/resumen", resumen(1))
    }

    @After
    fun cerrar() = servidor.shutdown()

    private fun sesion(estado: String = "COMPLETADA", extra: String = "", comoTutor: Boolean = false, id: String = "se-1") =
        """{"id":"$id","solicitudId":"s-1","contraparteId":"t-1","conQuien":"Ana Torres","materia":"Cálculo I",
            "fecha":"2026-11-03","hora":"14:00","duracionHoras":1.5,"modalidad":"PRESENCIAL","estado":"$estado",
            "comoTutor":$comoTutor$extra}"""

    private fun resumen(sesiones: Int) =
        """{"sesiones":$sesiones,"horas":13.5,"porCalificar":1,"promedioRecibido":4.7,"totalResenas":9,"personas":5,"materias":["Cálculo I"]}"""

    @Test
    fun `lee las sesiones del modo y convierte fecha, hora y calificacion`() = enPrueba {
        rutas.responder("GET", "/api/sesiones", "[${sesion(extra = ""","calificacion":4,"calificacionRecibida":5""")}]")

        val s = repositorio.obtenerSesiones().first().single()

        assertEquals("/api/sesiones?modo=ESTUDIANTE", rutas.peticiones.single().path)
        assertEquals(LocalDate.of(2026, 11, 3), s.fecha)
        assertEquals(LocalTime.of(14, 0), s.hora)
        assertEquals(1.5, s.duracionHoras, 0.0)
        assertEquals(Modalidad.PRESENCIAL, s.modalidad)
        assertEquals(EstadoSesion.COMPLETADA, s.estado)
        assertEquals("s-1", s.solicitudId)
        assertEquals(4, s.calificacion)
        assertEquals(5, s.calificacionRecibida)
        assertNull(s.comentario)
    }

    @Test
    fun `lee el PIN, el limite, el retardo y la propuesta en hora de Colombia`() = enPrueba {
        rutas.responder("GET", "/api/sesiones", "[${sesion("PROGRAMADA", extra = """,
            "pinEnviado":true,"pin":"4821","limite":"2026-11-03T19:10:00Z","horaPactada":"14:30","retardoMio":true,
            "acuerdo":{"tipo":"CANCELACION","propuestoPorMi":false}""")}]")

        val s = repositorio.obtenerSesiones().first().single()

        assertTrue(s.pinEnviado)
        assertEquals("4821", s.pin)
        assertEquals(java.time.LocalDateTime.of(2026, 11, 3, 14, 10), s.limite)
        assertEquals(LocalTime.of(14, 30), s.horaPactada)
        assertEquals(true, s.retardoMio)
        assertEquals(AcuerdoPendiente(TipoAcuerdo.CANCELACION, null, false), s.acuerdo)
    }

    @Test
    fun `un cierre o una penalizacion desconocidos no tumban la lista`() = enPrueba {
        rutas.responder("GET", "/api/sesiones", "[${sesion("CANCELADA", extra = ""","cierre":"OTRO","penalizacion":"ALTA"""")}]")
        val s = repositorio.obtenerSesiones().first().single()
        assertNull(s.cierre)
        assertNull(s.penalizacion)
    }

    @Test
    fun `enviar el PIN se ve sin esperar al siguiente sondeo`() = enPrueba {
        rutas.responder("GET", "/api/sesiones", "[${sesion("PROGRAMADA")}]")
        repositorio.obtenerSesiones().first()
        rutas.responder("POST", "/api/sesiones/se-1/pin", sesion("PROGRAMADA", extra = ""","pinEnviado":true"""))

        repositorio.enviarPin("se-1")

        assertTrue(repositorio.obtenerSesiones().first().single().pinEnviado)
    }

    @Test
    fun `un PIN rechazado deja pasar el error y vuelve a leer`() = enPrueba {
        rutas.responder("GET", "/api/sesiones", "[${sesion("PROGRAMADA")}]")
        repositorio.obtenerSesiones().first()
        rutas.fallar("POST", "/api/sesiones/se-1/iniciar", 422, CodigoError.PIN_INCORRECTO)
        val antes = rutas.pedidas("GET", "/api/sesiones").size

        try {
            repositorio.iniciar("se-1", "0000")
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.PIN_INCORRECTO, e.codigo)
        }

        assertTrue(rutas.pedidas("GET", "/api/sesiones").size > antes)
    }

    @Test
    fun `un retardo manda el tipo y los minutos`() = enPrueba {
        rutas.responder("POST", "/api/sesiones/se-1/acuerdo", sesion("PROGRAMADA"), 201)
        repositorio.proponerAcuerdo("se-1", TipoAcuerdo.RETARDO, 30)
        val cuerpo = rutas.pedidas("POST", "/api/sesiones/se-1/acuerdo").single().cuerpoTexto()
        assertTrue(cuerpo, cuerpo.contains("\"tipo\":\"RETARDO\"") && cuerpo.contains("\"minutos\":30"))
    }

    @Test
    fun `una sesion con un estado desconocido se omite sin tumbar la lista`() = enPrueba {
        rutas.responder("GET", "/api/sesiones", "[${sesion("REPROGRAMADA", id = "se-9")},${sesion()}]")
        assertEquals(listOf("se-1"), repositorio.obtenerSesiones().first().map { it.id })
    }

    @Test
    fun `el resumen trae las penalizaciones y hasta cuando dura la pausa, en hora de Colombia`() = enPrueba {
        rutas.responder(
            "GET",
            "/api/usuarios/yo/resumen",
            """{"sesiones":0,"horas":0,"porCalificar":0,"totalResenas":0,"personas":0,"materias":[],
                "penalizaciones":2,"suspendidaHasta":"2026-11-10T01:30:00Z"}"""
        )

        val visto = repositorio.resumenDelServidor().first()

        assertEquals(2, visto.penalizaciones)
        assertEquals(LocalDate.of(2026, 11, 9).atTime(20, 30), visto.suspendidaHasta)
    }

    @Test
    fun `el resumen viene del servidor en el modo actual`() = enPrueba {
        almacen.guardar(sesionDePrueba(rol = RolDto.TUTOR))

        val visto = repositorio.resumenDelServidor().first()

        assertEquals("/api/usuarios/yo/resumen?modo=TUTOR", rutas.peticiones.single().path)
        assertEquals(ResumenActividad(1, 13.5, 1, 4.7, 9, 5, listOf("Cálculo I")), visto)
    }

    @Test
    fun `el caso de uso del resumen no calcula si el servidor lo entrega`() = enPrueba {
        val delModo = ObtenerSesionesDelModoUseCase(repositorio, CuentaFija)
        val caso = ObtenerResumenActividadUseCase(ObtenerHistorialUseCase(delModo), repositorio)

        assertEquals(9, caso().first().totalResenas)
        assertTrue(rutas.pedidas("GET", "/api/sesiones").isEmpty())
    }

    @Test
    fun `calificar a un tutor refresca sesiones, resumen y su perfil publico`() = enPrueba {
        val tutor = "/api/tutores/t-1"
        rutas.responder("GET", tutor, TUTOR)
        rutas.responder("GET", "$tutor/resenas", """{"pagina":0,"tamano":50,"totalElementos":0,"totalPaginas":0,"elementos":[]}""")
        rutas.responder("GET", "/api/tutores", """{"pagina":0,"tamano":50,"totalElementos":0,"totalPaginas":0,"elementos":[]}""")
        repositorio.obtenerSesiones().first()
        repositorio.resumenDelServidor().first()
        tutores.obtenerTutor("t-1").first()
        tutores.obtenerResenas("t-1").first()
        tutores.obtenerTutores().first()
        rutas.peticiones.clear()
        val calificada = sesion(extra = ""","calificacion":5,"comentario":"Excelente"""")
        rutas.responder("POST", "/api/sesiones/se-1/calificacion", calificada, 201)
        rutas.responder("GET", "/api/sesiones", "[$calificada]")
        rutas.responder("GET", "/api/usuarios/yo/resumen", resumen(2))

        repositorio.calificar("se-1", 5, "Excelente")

        assertEquals(
            """{"estrellas":5,"comentario":"Excelente"}""",
            rutas.pedidas("POST", "/api/sesiones/se-1/calificacion").single().cuerpoTexto()
        )
        assertEquals(1, rutas.pedidas("GET", "/api/sesiones").size)
        assertEquals(1, rutas.pedidas("GET", "/api/usuarios/yo/resumen").size)
        assertEquals(1, rutas.pedidas("GET", tutor).size)
        assertEquals(1, rutas.pedidas("GET", "$tutor/resenas").size)
        assertEquals(1, rutas.pedidas("GET", "/api/tutores").size)
        assertEquals(5, repositorio.obtenerSesiones().first().single().calificacion)
        assertEquals(2, repositorio.resumenDelServidor().first().sesiones)
    }

    @Test
    fun `calificar a un estudiante no toca los perfiles de tutores`() = enPrueba {
        val calificada = sesion(comoTutor = true, extra = ""","calificacion":3""")
        rutas.responder("POST", "/api/sesiones/se-1/calificacion", calificada, 201)

        repositorio.calificar("se-1", 3, " ")

        assertEquals("""{"estrellas":3}""", rutas.pedidas("POST", "/api/sesiones/se-1/calificacion").single().cuerpoTexto())
        assertTrue(rutas.peticiones.none { it.path!!.startsWith("/api/tutores") })
    }

    @Test
    fun `un rechazo con codigo llega a la pantalla y la lista se pone al dia`() = enPrueba {
        repositorio.obtenerSesiones().first()
        rutas.fallar("POST", "/api/sesiones/se-1/calificacion", 409, CodigoError.SESION_YA_CALIFICADA)
        rutas.responder("GET", "/api/sesiones", "[${sesion(extra = ""","calificacion":2""")}]")
        try {
            repositorio.calificar("se-1", 5, null)
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.SESION_YA_CALIFICADA, e.codigo)
        }
        assertEquals(2, repositorio.obtenerSesiones().first().single().calificacion)
    }

    private object CuentaFija : AuthRepository {
        override val autenticado = MutableStateFlow(true)
        override val usuarioActual = MutableStateFlow(Usuario("u-1", "Ana", "@ana", "a@b.co", Rol.ESTUDIANTE))
        override suspend fun iniciarSesion(correo: String, contrasena: String) = throw NotImplementedError()
        override suspend fun registrarse(nombre: String, correo: String, contrasena: String) = throw NotImplementedError()
        override suspend fun verificarCorreo(correo: String, codigo: String) = Unit
        override suspend fun reenviarCodigo(correo: String) = Unit
        override suspend fun recuperarContrasena(correo: String) = Unit
        override suspend fun restablecerContrasena(correo: String, codigo: String, nueva: String) = Unit
        override suspend fun iniciarSesionGoogle() = true
        override suspend fun cambiarModo(rol: Rol) = Unit
        override suspend fun definirMaterias(materias: List<String>) = Unit
        override fun cerrarSesion() = Unit
        override suspend fun eliminarCuenta(contrasena: String) = Unit
    }

    private companion object {
        const val TUTOR = """{"id":"t-1","nombre":"Ana Torres","handle":"@ana","bio":"","universidad":"","materias":[],
            "calificacionPromedio":4.8,"totalResenas":1,"sesionesTotales":1,"estudiantes":1,"precioHora":0,"enLinea":false,"horario":[]}"""
    }
}
