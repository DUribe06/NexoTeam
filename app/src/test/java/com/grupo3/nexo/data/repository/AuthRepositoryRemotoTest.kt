package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.remote.AlmacenEnMemoria
import com.grupo3.nexo.data.remote.ErroresApi
import com.grupo3.nexo.data.remote.apiDePrueba
import com.grupo3.nexo.data.remote.cuerpoError
import com.grupo3.nexo.data.remote.cuerpoSesion
import com.grupo3.nexo.data.remote.jsonDePrueba
import com.grupo3.nexo.data.remote.sesionDePrueba
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.ResultadoAcceso
import com.grupo3.nexo.domain.model.Rol
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AuthRepositoryRemotoTest {
    private lateinit var servidor: MockWebServer
    private lateinit var almacen: AlmacenEnMemoria
    private lateinit var repositorio: AuthRepositoryRemoto

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        almacen = AlmacenEnMemoria()
        repositorio = AuthRepositoryRemoto(apiDePrueba(servidor.url("/")), almacen, ErroresApi(jsonDePrueba))
    }

    @After
    fun cerrar() = servidor.shutdown()

    private fun responderError(estado: Int, codigo: String, detalles: String = "[]") =
        servidor.enqueue(MockResponse().setResponseCode(estado).setBody(cuerpoError(codigo, detalles)))

    @Test
    fun `entrar guarda la sesion y expone al usuario`() = runTest {
        servidor.enqueue(MockResponse().setBody(cuerpoSesion()))

        val resultado = repositorio.iniciarSesion("ana@correo.co", "clave1234")

        assertEquals(ResultadoAcceso.Entro, resultado)
        assertEquals("acceso", almacen.sesion.value?.tokenAcceso)
        assertTrue(repositorio.autenticado.value)
        assertEquals("Ana María", repositorio.usuarioActual.value.nombre)
        assertEquals("@ana.maría", repositorio.usuarioActual.value.handle)
        assertEquals(Rol.ESTUDIANTE, repositorio.usuarioActual.value.rol)
    }

    @Test
    fun `distingue credenciales incorrectas`() = runTest {
        responderError(401, "CREDENCIALES_INVALIDAS")
        assertEquals(ResultadoAcceso.CredencialesIncorrectas, repositorio.iniciarSesion("a@b.co", "x"))
        assertFalse(repositorio.autenticado.value)
    }

    @Test
    fun `distingue correo sin verificar`() = runTest {
        responderError(403, "CORREO_NO_VERIFICADO")
        assertEquals(ResultadoAcceso.FaltaVerificarCorreo("a@b.co"), repositorio.iniciarSesion("a@b.co", "x"))
    }

    @Test
    fun `distingue cuenta suspendida`() = runTest {
        responderError(403, "CUENTA_SUSPENDIDA")
        assertEquals(ResultadoAcceso.CuentaSuspendida, repositorio.iniciarSesion("a@b.co", "x"))
    }

    @Test
    fun `distingue cuenta bloqueada con sus minutos`() = runTest {
        val hasta = Instant.now().plusSeconds(600)
        responderError(423, "CUENTA_BLOQUEADA", """[{"campo":"bloqueadoHasta","problema":"$hasta"}]""")
        assertEquals(ResultadoAcceso.CuentaBloqueada(10), repositorio.iniciarSesion("a@b.co", "x"))
    }

    @Test
    fun `los demas errores del login se lanzan`() = runTest {
        responderError(400, "DATOS_INVALIDOS")
        try {
            repositorio.iniciarSesion("a@b.co", "x")
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.DATOS_INVALIDOS, e.codigo)
        }
    }

    @Test
    fun `sin red el login lanza IOException`() = runTest {
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        try {
            repositorio.iniciarSesion("a@b.co", "x")
            fail("Debía fallar")
        } catch (e: IOException) {
            assertFalse(repositorio.autenticado.value)
        }
    }

    @Test
    fun `registrarse pide confirmar el correo, no abre sesion y no envia rol ni documento`() = runTest {
        servidor.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"usuarioId":"1","correo":"ana@correo.co","estadoCuenta":"PENDIENTE_VERIFICACION_CORREO","codigoVenceEnSegundos":900}"""
            )
        )

        val resultado = repositorio.registrarse("Ana", " ANA@correo.co", "clave1234")

        assertEquals(ResultadoAcceso.FaltaVerificarCorreo("ana@correo.co"), resultado)
        assertFalse(repositorio.autenticado.value)
        val cuerpo = servidor.takeRequest().body.readUtf8()
        assertFalse(cuerpo.contains("rol"))
        assertFalse(cuerpo.contains("documentoIdentidad"))
        assertTrue(cuerpo.contains(""""aceptaTerminos":true"""))
    }

    @Test
    fun `confirmar el codigo abre la sesion`() = runTest {
        servidor.enqueue(MockResponse().setBody(cuerpoSesion()))

        repositorio.verificarCorreo("a@b.co", "482913")

        assertTrue(repositorio.autenticado.value)
        assertEquals("""{"correo":"a@b.co","codigo":"482913"}""", servidor.takeRequest().body.readUtf8())
    }

    @Test
    fun `un codigo rechazado lanza su codigo de error`() = runTest {
        responderError(400, "CODIGO_VENCIDO")
        try {
            repositorio.verificarCorreo("a@b.co", "482913")
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.CODIGO_VENCIDO, e.codigo)
        }
    }

    @Test
    fun `restablecer envia la contrasena nueva con el nombre del contrato`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(204))

        repositorio.restablecerContrasena("a@b.co", "482913", "nueva1234")

        val peticion = servidor.takeRequest()
        assertEquals("/api/auth/restablecer", peticion.path)
        assertEquals(
            """{"correo":"a@b.co","codigo":"482913","nuevaContrasena":"nueva1234"}""",
            peticion.body.readUtf8()
        )
    }

    @Test
    fun `google lanza el error que la pantalla muestra como disponible pronto`() = runTest {
        try {
            repositorio.iniciarSesionGoogle()
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.GOOGLE_NO_DISPONIBLE, e.codigo)
        }
    }

    @Test
    fun `cambiar de modo es local y se olvida al cerrar sesion`() {
        almacen.guardar(sesionDePrueba())
        servidor.enqueue(MockResponse().setResponseCode(204))

        repositorio.cambiarModo(Rol.TUTOR)
        assertEquals(Rol.TUTOR, repositorio.usuarioActual.value.rol)
        assertEquals(0, servidor.requestCount)

        repositorio.cerrarSesion()
        almacen.guardar(sesionDePrueba())
        assertEquals(Rol.ESTUDIANTE, repositorio.usuarioActual.value.rol)
    }

    @Test
    fun `cerrar sesion avisa al servidor con los tokens que tenia`() {
        almacen.guardar(sesionDePrueba())
        servidor.enqueue(MockResponse().setResponseCode(204))

        repositorio.cerrarSesion()

        assertNull(almacen.sesion.value)
        assertFalse(repositorio.autenticado.value)
        val peticion = servidor.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("/api/auth/cerrar-sesion", peticion.path)
        assertEquals("Bearer acceso", peticion.getHeader("Authorization"))
        assertEquals("""{"tokenRefresco":"refresco"}""", peticion.body.readUtf8())
    }

    @Test
    fun `cerrar sesion con el token vencido lo renueva para invalidar el de refresco`() {
        almacen.guardar(sesionDePrueba())
        responderError(401, "NO_AUTENTICADO")
        servidor.enqueue(MockResponse().setBody(cuerpoSesion("acceso2", "refresco2")))
        servidor.enqueue(MockResponse().setResponseCode(204))

        repositorio.cerrarSesion()

        servidor.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("/api/auth/refrescar", servidor.takeRequest(5, TimeUnit.SECONDS)!!.path)
        val ultimo = servidor.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("Bearer acceso2", ultimo.getHeader("Authorization"))
        assertEquals("""{"tokenRefresco":"refresco2"}""", ultimo.body.readUtf8())
        assertNull(almacen.sesion.value)
    }

    @Test
    fun `cerrar sesion borra la sesion local aunque no haya red`() {
        almacen.guardar(sesionDePrueba())
        servidor.shutdown()

        repositorio.cerrarSesion()

        assertNull(almacen.sesion.value)
        assertFalse(repositorio.autenticado.value)
        assertEquals("", repositorio.usuarioActual.value.id)
    }
}
