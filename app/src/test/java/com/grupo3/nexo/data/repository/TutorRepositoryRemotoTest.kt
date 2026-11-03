package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.remote.AlmacenEnMemoria
import com.grupo3.nexo.data.remote.ErroresApi
import com.grupo3.nexo.data.remote.RutasFalsas
import com.grupo3.nexo.data.remote.apiDePrueba
import com.grupo3.nexo.data.remote.cuerpoTexto
import com.grupo3.nexo.data.remote.enPrueba
import com.grupo3.nexo.data.remote.jsonDePrueba
import com.grupo3.nexo.data.remote.sesionDePrueba
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.DiaSemana
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.FiltroTutores
import com.grupo3.nexo.domain.model.Franja
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.usecase.BuscarTutoresUseCase
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class TutorRepositoryRemotoTest {
    private val servidor = MockWebServer()
    private val rutas = RutasFalsas()
    private val almacen = AlmacenEnMemoria(sesionDePrueba())
    private lateinit var repositorio: TutorRepositoryRemoto

    @Before
    fun preparar() {
        servidor.dispatcher = rutas
        servidor.start()
        repositorio = TutorRepositoryRemoto(apiDePrueba(servidor.url("/")), ErroresApi(jsonDePrueba), ContextoRemoto(almacen))
    }

    @After
    fun cerrar() = servidor.shutdown()

    private fun tutor(id: String = "t-1", rating: Double = 4.5, horario: String = "") =
        """{"id":"$id","nombre":"Ana Torres","handle":"@anatorres","bio":"","universidad":"UIS","materias":["Cálculo I"],
            "calificacionPromedio":$rating,"totalResenas":23,"sesionesTotales":58,"estudiantes":31,"precioHora":35000,
            "enLinea":true,"horario":[$horario]}"""

    private fun pagina(elementos: String, pagina: Int = 0, totalPaginas: Int = 1) =
        """{"pagina":$pagina,"tamano":50,"totalElementos":2,"totalPaginas":$totalPaginas,"elementos":[$elementos]}"""

    @Test
    fun `lee el catalogo recorriendo todas las paginas`() = enPrueba {
        rutas.responderSegun("GET", "/api/tutores") { peticion ->
            val pagina = peticion.requestUrl!!.queryParameter("pagina")!!.toInt()
            MockResponse().setBody(pagina(tutor("t-${pagina + 1}"), pagina, totalPaginas = 2))
        }

        val tutores = repositorio.obtenerTutores().first()

        assertEquals(listOf("t-1", "t-2"), tutores.map { it.id })
        assertEquals(
            listOf("/api/tutores?pagina=0&tamano=50", "/api/tutores?pagina=1&tamano=50"),
            rutas.peticiones.map { it.path }
        )
    }

    @Test
    fun `convierte el perfil y su horario, sin tumbarse por un dia o modalidad desconocidos`() = enPrueba {
        val horario = """{"dia":"MIERCOLES","hora":"14:00","modalidades":["VIRTUAL","POR_TELEPATIA"]},
            {"dia":"FERIADO","hora":"08:00","modalidades":["VIRTUAL"]},
            {"dia":"LUNES","hora":"08:00","modalidades":["POR_TELEPATIA"]}"""
        rutas.responder("GET", "/api/tutores/t-1", tutor(horario = horario))

        val t = repositorio.obtenerTutor("t-1").first()!!

        assertEquals(4.5, t.rating, 0.0)
        assertEquals(35000.0, t.precioHora, 0.0)
        val franja = t.horario.single()
        assertEquals(DiaSemana.MIERCOLES, franja.dia)
        assertEquals(LocalTime.of(14, 0), franja.hora)
        assertEquals(setOf(Modalidad.VIRTUAL), franja.modalidades)
    }

    @Test
    fun `un tutor que no existe es null, no un error`() = enPrueba {
        rutas.fallar("GET", "/api/tutores/t-9", 404, CodigoError.TUTOR_NO_ENCONTRADO)
        assertNull(repositorio.obtenerTutor("t-9").first())
    }

    @Test
    fun `otro fallo al leer un tutor se propaga y reintentar vuelve a pedir`() = enPrueba {
        rutas.cortar("GET", "/api/tutores/t-1")
        try {
            repositorio.obtenerTutor("t-1").first()
            fail("Debía fallar")
        } catch (e: IOException) {
            // Lo que la pantalla muestra con "Reintentar"
        }
        rutas.responder("GET", "/api/tutores/t-1", tutor())
        assertEquals("t-1", repositorio.obtenerTutor("t-1").first()?.id)
    }

    @Test
    fun `convierte las resenas y omite las que no tienen comentario`() = enPrueba {
        val con = """{"id":"r-1","tutorId":"t-1","autor":"Diego","handleAutor":"@diego","texto":"Muy clara","estrellas":5,"fecha":"2026-11-02"}"""
        val sin = """{"id":"r-2","tutorId":"t-1","autor":"Luis","handleAutor":"@luis","texto":"","estrellas":4,"fecha":"2026-11-01"}"""
        rutas.responder("GET", "/api/tutores/t-1/resenas", pagina("$con,$sin"))

        assertEquals(
            listOf(Resena("r-1", "t-1", "Diego", "@diego", "Muy clara", 5, LocalDate.of(2026, 11, 2))),
            repositorio.obtenerResenas("t-1").first()
        )
    }

    @Test
    fun `quien nunca ha sido tutor tiene cero resenas, no un error`() = enPrueba {
        rutas.fallar("GET", "/api/tutores/u-1/resenas", 404, CodigoError.TUTOR_NO_ENCONTRADO)
        assertEquals(emptyList<Resena>(), repositorio.obtenerResenas("u-1").first())
    }

    @Test
    fun `sin usuario no se pide ninguna resena`() = enPrueba {
        assertEquals(emptyList<Resena>(), repositorio.obtenerResenas("").first())
        assertTrue(rutas.peticiones.isEmpty())
    }

    @Test
    fun `la busqueda con filtros la hace el servidor y el caso de uso ordena por calificacion`() = enPrueba {
        rutas.responder("GET", "/api/tutores", pagina("${tutor("t-1", 3.0)},${tutor("t-2", 5.0)}"))
        val filtro = FiltroTutores(
            consulta = "  calculo ",
            materias = listOf("Cálculo", "Álgebra"),
            modalidad = Modalidad.VIRTUAL,
            franja = Franja.NOCHE,
            soloEnLinea = true
        )

        val tutores = BuscarTutoresUseCase(repositorio)(filtro).first()

        assertEquals(listOf("t-2", "t-1"), tutores.map { it.id })
        val url = rutas.peticiones.single().requestUrl!!
        assertEquals("calculo", url.queryParameter("q"))
        assertEquals(listOf("Cálculo", "Álgebra"), url.queryParameterValues("materia"))
        assertEquals("VIRTUAL", url.queryParameter("modalidad"))
        assertEquals("NOCHE", url.queryParameter("franja"))
        assertEquals("true", url.queryParameter("enLinea"))
    }

    @Test
    fun `sin filtros la busqueda es el catalogo en memoria`() = enPrueba {
        rutas.responder("GET", "/api/tutores", pagina(tutor()))
        repositorio.obtenerTutores().first()
        repositorio.buscarTutores(FiltroTutores()).first()
        assertEquals(1, rutas.peticiones.size)
    }

    @Test
    fun `refrescar tras calificar actualiza el catalogo, el perfil y las busquedas abiertas`() = enPrueba {
        rutas.responder("GET", "/api/tutores", pagina(tutor(rating = 3.0)))
        rutas.responder("GET", "/api/tutores/t-1", tutor(rating = 3.0))
        repositorio.obtenerTutores().first()
        repositorio.obtenerTutor("t-1").first()
        val nuevaBusqueda = CompletableDeferred<Double>()
        val abierta = CoroutineScope(Dispatchers.Default).launch {
            repositorio.buscarTutores(FiltroTutores(consulta = "ana")).collect { lista ->
                if (lista.single().rating == 5.0) nuevaBusqueda.complete(5.0)
            }
        }
        // Antes del refresco la búsqueda ya respondió con el promedio viejo
        while (rutas.peticiones.none { it.requestUrl!!.queryParameter("q") == "ana" }) kotlinx.coroutines.delay(10)

        rutas.responder("GET", "/api/tutores", pagina(tutor(rating = 5.0)))
        rutas.responder("GET", "/api/tutores/t-1", tutor(rating = 5.0))
        repositorio.refrescarTutor("t-1")

        assertEquals(5.0, repositorio.obtenerTutores().first().single().rating, 0.0)
        assertEquals(5.0, repositorio.obtenerTutor("t-1").first()!!.rating, 0.0)
        assertEquals(5.0, nuevaBusqueda.await(), 0.0)
        abierta.cancelAndJoin()
    }

    @Test
    fun `reportar envia el motivo y dice si ya habia un reporte abierto`() = enPrueba {
        rutas.responder("POST", "/api/tutores/t-1/reportes", """{"id":"rep-1","creadoEn":"2026-11-02T14:00:00-05:00"}""", 201)
        repositorio.reportar("t-1", "Información falsa")
        assertEquals("""{"motivo":"Información falsa"}""", rutas.peticiones.single().cuerpoTexto())

        rutas.fallar("POST", "/api/tutores/t-1/reportes", 409, CodigoError.REPORTE_DUPLICADO)
        try {
            repositorio.reportar("t-1", "Información falsa")
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.REPORTE_DUPLICADO, e.codigo)
        }
    }

    @Test
    fun `agregar resena no llama al servidor`() = enPrueba {
        repositorio.agregarResena(Resena("r", "t-1", "a", "@a", "x", 5, LocalDate.of(2026, 11, 2)))
        assertTrue(rutas.peticiones.isEmpty())
    }
}
