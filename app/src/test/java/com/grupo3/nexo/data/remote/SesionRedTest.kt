package com.grupo3.nexo.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SesionRedTest {
    private lateinit var servidor: MockWebServer
    private lateinit var almacen: AlmacenEnMemoria
    private lateinit var cliente: OkHttpClient

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        almacen = AlmacenEnMemoria(sesionDePrueba())
        cliente = clienteDePrueba().newBuilder()
            .addInterceptor(InterceptorSesion(almacen))
            .authenticator(AutenticadorSesion(almacen, apiDePrueba(servidor.url("/"))))
            .build()
    }

    @After
    fun cerrar() = servidor.shutdown()

    private fun pedir(ruta: String = "/api/privada"): Int {
        val peticion = Request.Builder().url(servidor.url(ruta))
        if (ruta.startsWith("/api/auth")) peticion.post("{}".toRequestBody("application/json".toMediaType()))
        return cliente.newCall(peticion.build()).execute().use { it.code }
    }

    private fun noAutenticado() = MockResponse().setResponseCode(401).setBody(cuerpoError("NO_AUTENTICADO"))

    @Test
    fun `agrega el token a las rutas privadas`() {
        servidor.enqueue(MockResponse())

        pedir()

        assertEquals("Bearer acceso", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `no envia el token a las rutas publicas`() {
        servidor.enqueue(MockResponse())

        pedir("/api/auth/login")

        assertNull(servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `sin sesion no agrega nada`() {
        almacen.borrar()
        servidor.enqueue(MockResponse())

        pedir()

        assertNull(servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `ante un 401 renueva los tokens y repite la peticion`() {
        servidor.enqueue(noAutenticado())
        servidor.enqueue(MockResponse().setBody(cuerpoSesion("acceso2", "refresco2")))
        servidor.enqueue(MockResponse())

        val codigo = pedir()

        assertEquals(200, codigo)
        servidor.takeRequest()
        val refresco = servidor.takeRequest()
        assertEquals("/api/auth/refrescar", refresco.path)
        assertEquals("""{"tokenRefresco":"refresco"}""", refresco.body.readUtf8())
        assertEquals("Bearer acceso2", servidor.takeRequest().getHeader("Authorization"))
        assertEquals("refresco2", almacen.sesion.value?.tokenRefresco)
    }

    @Test
    fun `si el servidor rechaza el refresco se borra la sesion`() {
        servidor.enqueue(noAutenticado())
        servidor.enqueue(MockResponse().setResponseCode(401).setBody(cuerpoError("TOKEN_INVALIDO")))

        val codigo = pedir()

        assertEquals(401, codigo)
        assertNull(almacen.sesion.value)
    }

    @Test
    fun `si rechaza tambien el token renovado se borra la sesion sin insistir`() {
        servidor.enqueue(noAutenticado())
        servidor.enqueue(MockResponse().setBody(cuerpoSesion("acceso2", "refresco2")))
        servidor.enqueue(noAutenticado())

        val codigo = pedir()

        assertEquals(401, codigo)
        assertEquals(3, servidor.requestCount)
        assertNull(almacen.sesion.value)
    }

    @Test
    fun `un fallo del servidor al renovar no saca a nadie de su cuenta`() {
        servidor.enqueue(noAutenticado())
        servidor.enqueue(MockResponse().setResponseCode(500))

        pedir()

        assertEquals("acceso", almacen.sesion.value?.tokenAcceso)
    }

    @Test
    fun `un corte de red al renovar no saca a nadie de su cuenta`() {
        servidor.enqueue(noAutenticado())
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertEquals(401, pedir())

        assertEquals("acceso", almacen.sesion.value?.tokenAcceso)
    }

    @Test
    fun `un 401 sin token, como una clave errada, no intenta renovar`() {
        almacen.borrar()
        servidor.enqueue(MockResponse().setResponseCode(401).setBody(cuerpoError("CREDENCIALES_INVALIDAS")))

        assertEquals(401, pedir("/api/auth/login"))

        assertEquals(1, servidor.requestCount)
    }
}
