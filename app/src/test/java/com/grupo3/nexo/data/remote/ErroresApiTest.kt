package com.grupo3.nexo.data.remote

import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.DetalleError
import com.grupo3.nexo.domain.model.ErrorApi
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ErroresApiTest {
    private lateinit var servidor: MockWebServer
    private lateinit var api: NexoApi
    private val errores = ErroresApi(jsonDePrueba)

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        api = apiDePrueba(servidor.url("/"))
    }

    @After
    fun cerrar() = servidor.shutdown()

    private suspend fun errorDe(peticion: suspend () -> Unit): ErrorApi =
        try {
            peticion()
            fail("Debía fallar")
            throw AssertionError()
        } catch (e: ErrorApi) {
            e
        }

    @Test
    fun `el cuerpo Error se convierte en ErrorApi con codigo, mensaje y detalles`() = runTest {
        servidor.enqueue(
            MockResponse().setResponseCode(400)
                .setBody(cuerpoError("DATOS_INVALIDOS", """[{"campo":"correo","problema":"no es válido"}]"""))
        )

        val error = errorDe { errores.llamar { api.login(SolicitudLogin("a", "b")) } }

        assertEquals(CodigoError.DATOS_INVALIDOS, error.codigo)
        assertEquals("Texto del servidor", error.message)
        assertEquals(listOf(DetalleError("correo", "no es válido")), error.detalles)
        assertNull(error.reintentarEnSegundos)
    }

    @Test
    fun `una ruta 204 con error tambien se convierte y lee Retry-After`() = runTest {
        servidor.enqueue(
            MockResponse().setResponseCode(429).setHeader("Retry-After", "42")
                .setBody(cuerpoError("DEMASIADAS_SOLICITUDES"))
        )

        val error = errorDe { errores.sinCuerpo { api.reenviarCodigo(SolicitudCorreo("a@b.co")) } }

        assertEquals(CodigoError.DEMASIADAS_SOLICITUDES, error.codigo)
        assertEquals(42, error.reintentarEnSegundos)
    }

    @Test
    fun `una ruta 204 exitosa no lanza nada`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(204))

        errores.sinCuerpo { api.recuperar(SolicitudCorreo("a@b.co")) }

        assertEquals("/api/auth/recuperar", servidor.takeRequest().path)
    }

    @Test
    fun `un error sin el esquema del contrato es ERROR_SERVIDOR`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(502).setBody("<html>Bad gateway</html>"))

        val error = errorDe { errores.llamar { api.login(SolicitudLogin("a", "b")) } }

        assertEquals(CodigoError.ERROR_SERVIDOR, error.codigo)
    }

    @Test
    fun `un error sin cuerpo es ERROR_SERVIDOR`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))

        val error = errorDe { errores.sinCuerpo { api.recuperar(SolicitudCorreo("a@b.co")) } }

        assertEquals(CodigoError.ERROR_SERVIDOR, error.codigo)
    }

    @Test
    fun `un corte de red sigue siendo IOException`() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        try {
            errores.llamar { api.login(SolicitudLogin("a", "b")) }
            fail("Debía fallar")
        } catch (e: IOException) {
            // Lo esperado: la UI lo muestra como falta de conexión
        }
    }

    @Test
    fun `calcula los minutos de bloqueo redondeando hacia arriba`() {
        val ahora = Instant.parse("2026-11-01T15:00:00Z")
        val detalles = listOf(DetalleError("bloqueadoHasta", "2026-11-01T15:14:01.500Z"))
        assertEquals(15, minutosDeBloqueo(detalles, ahora))
    }

    @Test
    fun `un bloqueo a punto de acabar o ya vencido cuenta como un minuto`() {
        val ahora = Instant.parse("2026-11-01T15:00:00Z")
        assertEquals(1, minutosDeBloqueo(listOf(DetalleError("bloqueadoHasta", "2026-11-01T14:59:00Z")), ahora))
    }

    @Test
    fun `sin fecha de bloqueo legible no hay minutos`() {
        assertNull(minutosDeBloqueo(emptyList()))
        assertNull(minutosDeBloqueo(listOf(DetalleError("bloqueadoHasta", "pronto"))))
    }
}
