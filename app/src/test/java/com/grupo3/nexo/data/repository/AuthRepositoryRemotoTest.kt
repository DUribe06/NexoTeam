package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.remote.AlmacenEnMemoria
import com.grupo3.nexo.data.remote.ColaConPerfil
import com.grupo3.nexo.data.remote.ErroresApi
import com.grupo3.nexo.data.remote.RolDto
import com.grupo3.nexo.data.remote.apiDePrueba
import com.grupo3.nexo.data.remote.cuerpoError
import com.grupo3.nexo.data.remote.cuerpoPerfil
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
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
    private val cola = ColaConPerfil()

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.dispatcher = cola
        servidor.start()
        almacen = AlmacenEnMemoria()
        repositorio = AuthRepositoryRemoto(apiDePrueba(servidor.url("/")), almacen, ErroresApi(jsonDePrueba))
    }

    @After
    fun cerrar() = servidor.shutdown()

    private fun responderError(estado: Int, codigo: String, detalles: String = "[]") =
        servidor.enqueue(MockResponse().setResponseCode(estado).setBody(cuerpoError(codigo, detalles)))

    /** La siguiente petición que no sea el GET /yo que el repositorio hace por su cuenta. */
    private fun siguientePeticion(): RecordedRequest {
        while (true) {
            val peticion = servidor.takeRequest(5, TimeUnit.SECONDS)!!
            if (peticion.path != ColaConPerfil.RUTA_PERFIL) return peticion
        }
    }

    /** Espera a que el GET /yo en segundo plano termine de aplicarse. */
    private suspend fun esperarPerfil() = withTimeout(5_000) {
        repositorio.usuarioActual.first { it.handle.isNotEmpty() }
    }

    @Test
    fun `entrar guarda la sesion y expone al usuario`() = runTest {
        servidor.enqueue(MockResponse().setBody(cuerpoSesion()))

        val resultado = repositorio.iniciarSesion("ana@correo.co", "clave1234")

        assertEquals(ResultadoAcceso.Entro, resultado)
        assertEquals("acceso", almacen.sesion.value?.tokenAcceso)
        assertTrue(repositorio.autenticado.value)
        assertEquals("Ana María", repositorio.usuarioActual.value.nombre)
        assertEquals(Rol.ESTUDIANTE, repositorio.usuarioActual.value.rol)
    }

    @Test
    fun `eliminar la cuenta borra la sesion y si el servidor se niega la conserva`() = runTest {
        servidor.enqueue(MockResponse().setBody(cuerpoSesion()))
        repositorio.iniciarSesion("ana@correo.co", "clave1234")
        siguientePeticion()

        responderError(422, "CONTRASENA_INCORRECTA")
        val fallo = runCatching { repositorio.eliminarCuenta("mala") }.exceptionOrNull()
        assertEquals("CONTRASENA_INCORRECTA", (fallo as ErrorApi).codigo)
        assertTrue(repositorio.autenticado.value)
        siguientePeticion()

        servidor.enqueue(MockResponse().setResponseCode(204))
        repositorio.eliminarCuenta("clave1234")
        val peticion = siguientePeticion()
        assertEquals("/api/usuarios/yo/eliminar", peticion.path)
        assertEquals("""{"contrasena":"clave1234"}""", peticion.body.readUtf8())
        assertFalse(repositorio.autenticado.value)
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
        val cuerpo = siguientePeticion().body.readUtf8()
        assertFalse(cuerpo.contains("rol"))
        assertFalse(cuerpo.contains("documentoIdentidad"))
        assertTrue(cuerpo.contains(""""aceptaTerminos":true"""))
    }

    @Test
    fun `confirmar el codigo abre la sesion`() = runTest {
        servidor.enqueue(MockResponse().setBody(cuerpoSesion()))

        repositorio.verificarCorreo("a@b.co", "482913")

        assertTrue(repositorio.autenticado.value)
        assertEquals("""{"correo":"a@b.co","codigo":"482913"}""", siguientePeticion().body.readUtf8())
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

        val peticion = siguientePeticion()
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
    fun `el handle y el resto del perfil vienen de GET yo`() = runBlocking {
        almacen.guardar(sesionDePrueba())

        esperarPerfil()

        assertEquals("@anamaria", repositorio.usuarioActual.value.handle)
        assertEquals("UIS", repositorio.usuarioActual.value.universidad)
    }

    @Test
    fun `sin el perfil se entra igual`() = runBlocking {
        cola.perfil = MockResponse().setResponseCode(500).setBody(cuerpoError("ERROR"))
        almacen.guardar(sesionDePrueba())
        assertEquals("", repositorio.usuarioActual.value.handle)
        assertTrue(repositorio.autenticado.value)
    }

    @Test
    fun `guardar las materias envia solo eso y si falla lo guardado no cambia`() = runBlocking {
        almacen.guardar(sesionDePrueba())
        esperarPerfil()
        servidor.enqueue(MockResponse().setBody(cuerpoPerfil("TUTOR")))

        repositorio.definirMaterias(listOf("Java", "Python"))

        val peticion = siguientePeticion()
        assertEquals("PATCH", peticion.method)
        assertEquals("/api/usuarios/yo/perfil-tutor", peticion.path)
        assertEquals("""{"materias":["Java","Python"]}""", peticion.body.readUtf8())

        val antes = repositorio.usuarioActual.value.materias
        servidor.enqueue(MockResponse().setResponseCode(500))
        val fallo = runCatching { repositorio.definirMaterias(listOf("Otra")) }
        assertTrue(fallo.isFailure)
        assertEquals(antes, repositorio.usuarioActual.value.materias)
    }

    @Test
    fun `cambiar de modo va al servidor y queda en la sesion guardada`() = runBlocking {
        almacen.guardar(sesionDePrueba())
        esperarPerfil()
        servidor.enqueue(MockResponse().setBody(cuerpoPerfil("TUTOR")))

        repositorio.cambiarModo(Rol.TUTOR)

        val peticion = siguientePeticion()
        assertEquals("PUT", peticion.method)
        assertEquals("/api/usuarios/yo/modo", peticion.path)
        assertEquals("""{"modo":"TUTOR"}""", peticion.body.readUtf8())
        assertEquals(Rol.TUTOR, repositorio.usuarioActual.value.rol)
        assertEquals(RolDto.TUTOR, almacen.sesion.value?.usuario?.rol)
    }

    @Test
    fun `si cambiar de modo falla el modo no cambia y el error llega a la pantalla`() = runBlocking {
        almacen.guardar(sesionDePrueba())
        esperarPerfil()
        responderError(403, "ADMIN_SIN_MODOS")
        try {
            repositorio.cambiarModo(Rol.TUTOR)
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.ADMIN_SIN_MODOS, e.codigo)
        }
        assertEquals(Rol.ESTUDIANTE, repositorio.usuarioActual.value.rol)

        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        try {
            repositorio.cambiarModo(Rol.TUTOR)
            fail("Debía fallar")
        } catch (e: IOException) {
            assertEquals(Rol.ESTUDIANTE, repositorio.usuarioActual.value.rol)
        }
    }

    @Test
    fun `elegir el modo en que ya esta no llama al servidor`() = runBlocking {
        almacen.guardar(sesionDePrueba())
        esperarPerfil()
        repositorio.cambiarModo(Rol.ESTUDIANTE)
        assertEquals(RolDto.ESTUDIANTE, almacen.sesion.value?.usuario?.rol)
        val pedidas = generateSequence { servidor.takeRequest(200, TimeUnit.MILLISECONDS) }.map { it.path }.toList()
        assertFalse(pedidas.toString(), pedidas.any { it != ColaConPerfil.RUTA_PERFIL })
    }

    @Test
    fun `si otro dispositivo cambio el modo manda el del servidor`() = runBlocking {
        cola.perfil = MockResponse().setBody(cuerpoPerfil("TUTOR"))
        almacen.guardar(sesionDePrueba())

        withTimeout(5_000) { repositorio.usuarioActual.first { it.rol == Rol.TUTOR } }

        assertEquals(RolDto.TUTOR, almacen.sesion.value?.usuario?.rol)
    }

    @Test
    fun `una cuenta de administrador no abre sesion y recibe un error propio`() = runTest {
        servidor.enqueue(MockResponse().setBody(cuerpoSesion(rol = "ADMIN")))
        try {
            repositorio.iniciarSesion("admin@nexo.co", "clave1234")
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.CUENTA_NO_USA_APP, e.codigo)
        }
        assertFalse(repositorio.autenticado.value)
        assertNull(almacen.sesion.value)
        // Los tokens que acaba de emitir el servidor no se quedan vivos sin que nadie los tenga
        assertEquals("/api/auth/login", siguientePeticion().path)
        assertEquals("/api/auth/cerrar-sesion", siguientePeticion().path)
    }

    @Test
    fun `un rol que la app no conoce tampoco tumba la lectura`() = runTest {
        servidor.enqueue(MockResponse().setBody(cuerpoSesion(rol = "MONITOR")))
        try {
            repositorio.verificarCorreo("a@b.co", "482913")
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.CUENTA_NO_USA_APP, e.codigo)
        }
        assertFalse(repositorio.autenticado.value)
    }

    @Test
    fun `si la cuenta resulta ser de administrador al pedir el perfil se cierra la sesion`() = runBlocking {
        cola.perfil = MockResponse().setBody(cuerpoPerfil("ADMIN"))
        almacen.guardar(sesionDePrueba())

        withTimeout(5_000) { repositorio.autenticado.first { !it } }

        assertNull(almacen.sesion.value)
    }

    @Test
    fun `cerrar sesion avisa al servidor con los tokens que tenia`() {
        almacen.guardar(sesionDePrueba())
        servidor.enqueue(MockResponse().setResponseCode(204))

        repositorio.cerrarSesion()

        assertNull(almacen.sesion.value)
        assertFalse(repositorio.autenticado.value)
        val peticion = siguientePeticion()
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

        siguientePeticion()
        assertEquals("/api/auth/refrescar", siguientePeticion().path)
        val ultimo = siguientePeticion()
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
