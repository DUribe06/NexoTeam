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
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.DiaSemana
import com.grupo3.nexo.domain.model.DiaSolicitable
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.FranjaTutor
import com.grupo3.nexo.domain.model.HoraDisponible
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.usecase.ObtenerDiasSolicitablesUseCase
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class DisponibilidadRepositoryRemotoTest {
    private val servidor = MockWebServer()
    private val rutas = RutasFalsas()
    private val almacen = AlmacenEnMemoria(sesionDePrueba(rol = RolDto.TUTOR))
    private lateinit var repositorio: DisponibilidadRepositoryRemoto

    @Before
    fun preparar() {
        servidor.dispatcher = rutas
        servidor.start()
        repositorio = DisponibilidadRepositoryRemoto(apiDePrueba(servidor.url("/")), ErroresApi(jsonDePrueba), ContextoRemoto(almacen))
    }

    @After
    fun cerrar() = servidor.shutdown()

    private fun semana(modalidades: String = "") =
        """{"creado":true,"dias":[{"dia":"MIERCOLES","horas":[{"hora":"14:00","modalidades":[$modalidades]}]},
            {"dia":"FERIADO","horas":[]}]}"""

    @Test
    fun `lee la semana y usa el nombre de dia que espera el dominio`() = enPrueba {
        rutas.responder("GET", "/api/disponibilidad/yo", semana(""""VIRTUAL""""))
        assertEquals(
            listOf(DiaDisponibilidad("Miércoles", listOf(HoraDisponible(LocalTime.of(14, 0), setOf(Modalidad.VIRTUAL))))),
            repositorio.obtener().first()
        )
    }

    @Test
    fun `sin calendario la semana esta vacia`() = enPrueba {
        rutas.responder("GET", "/api/disponibilidad/yo", """{"creado":false,"dias":[]}""")
        assertEquals(emptyList<DiaDisponibilidad>(), repositorio.obtener().first())
    }

    @Test
    fun `en modo estudiante no se pide nada aunque el Inicio la lea`() = enPrueba {
        almacen.guardar(sesionDePrueba())
        assertEquals(emptyList<DiaDisponibilidad>(), repositorio.obtener().first())
        assertTrue(rutas.peticiones.isEmpty())
    }

    @Test
    fun `sin perfil de tutor la semana esta vacia, no es un error`() = enPrueba {
        rutas.fallar("GET", "/api/disponibilidad/yo", 403, CodigoError.NO_ES_TUTOR)
        assertEquals(emptyList<DiaDisponibilidad>(), repositorio.obtener().first())
    }

    @Test
    fun `un fallo de lectura se propaga`() = enPrueba {
        rutas.cortar("GET", "/api/disponibilidad/yo")
        try {
            repositorio.obtener().first()
            fail("Debía fallar")
        } catch (e: IOException) {
            // La pantalla muestra el error con "Reintentar"
        }
    }

    @Test
    fun `crear el calendario deja la semana que respondio el servidor`() = enPrueba {
        rutas.responder("GET", "/api/disponibilidad/yo", """{"creado":false,"dias":[]}""")
        repositorio.obtener().first()
        rutas.responder("POST", "/api/disponibilidad/yo", semana(), 201)

        repositorio.crearCalendario()

        assertEquals("Miércoles", repositorio.obtener().first().single().dia)
    }

    @Test
    fun `guardar la semana envia solo lo publicado y todas las pantallas ven el cambio`() = enPrueba {
        rutas.responder("GET", "/api/disponibilidad/yo", semana())
        val actual = repositorio.obtener().first()
        rutas.responder("PUT", "/api/disponibilidad/yo/semana", semana(""""PRESENCIAL""""))
        val marcada = actual.map { d -> d.copy(horas = d.horas.map { it.copy(modalidades = setOf(Modalidad.PRESENCIAL)) }) }

        repositorio.definirSemana(marcada)

        assertEquals(
            """{"horas":[{"dia":"MIERCOLES","hora":"14:00","modalidades":["PRESENCIAL"]}]}""",
            rutas.pedidas("PUT", "/api/disponibilidad/yo/semana").single().cuerpoTexto()
        )
        assertEquals(setOf(Modalidad.PRESENCIAL), repositorio.obtener().first().single().horas.single().modalidades)
    }

    @Test
    fun `una semana rechazada llega con su codigo y lo guardado no cambia`() = enPrueba {
        rutas.responder("GET", "/api/disponibilidad/yo", semana())
        val actual = repositorio.obtener().first()
        rutas.fallar("PUT", "/api/disponibilidad/yo/semana", 422, CodigoError.HORA_NO_VALIDA)
        try {
            repositorio.definirSemana(actual)
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.HORA_NO_VALIDA, e.codigo)
        }
        assertEquals(emptySet<Modalidad>(), repositorio.obtener().first().single().horas.single().modalidades)
    }

    @Test
    fun `los dias solicitables vienen del servidor y omiten los que quedan sin modalidades conocidas`() = enPrueba {
        rutas.responder(
            "GET", "/api/tutores/t-1/disponibilidad",
            """[{"fecha":"2026-11-03","franjas":[{"hora":"14:00","modalidades":["VIRTUAL"]}]},
                {"fecha":"2026-11-04","franjas":[{"hora":"14:00","modalidades":["POR_TELEPATIA"]}]}]"""
        )

        val dias = repositorio.diasSolicitablesDelServidor("t-1").first()

        assertEquals(
            listOf(
                DiaSolicitable(
                    LocalDate.of(2026, 11, 3),
                    listOf(FranjaTutor(DiaSemana.MARTES, LocalTime.of(14, 0), setOf(Modalidad.VIRTUAL)))
                )
            ),
            dias
        )
    }

    @Test
    fun `si fallan los dias del servidor la hoja usa los del horario del tutor`() = enPrueba {
        rutas.cortar("GET", "/api/tutores/t-1/disponibilidad")
        val horario = DiaSemana.entries.map { FranjaTutor(it, LocalTime.of(8, 0), setOf(Modalidad.VIRTUAL)) }
        val tutor = Tutor("t-1", "Ana", "@ana", "", "", emptyList(), 0.0, 0, 0, 0, 0.0, false, horario)

        assertEquals(14, ObtenerDiasSolicitablesUseCase(repositorio)(tutor).first().size)
    }
}
