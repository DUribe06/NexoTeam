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
import com.grupo3.nexo.domain.model.ErrorApi
import java.io.IOException
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ChatRepositoryRemotoTest {
    private val servidor = MockWebServer()
    private val rutas = RutasFalsas()
    private val almacen = AlmacenEnMemoria(sesionDePrueba())
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repositorio: ChatRepositoryRemoto

    private val rutaMensajes = "/api/chats/c-1/mensajes"

    @Before
    fun preparar() {
        servidor.dispatcher = rutas
        servidor.start()
        repositorio = crear(intervaloMs = 60_000)
        rutas.responder("GET", "/api/chats", "[${chat()}]")
    }

    @After
    fun cerrar() {
        alcance.cancel()
        servidor.shutdown()
    }

    private fun crear(intervaloMs: Long) =
        ChatRepositoryRemoto(apiDePrueba(servidor.url("/")), ErroresApi(jsonDePrueba), ContextoRemoto(almacen), intervaloMs)

    private fun chat(
        id: String = "c-1",
        ultimo: String = "Hola",
        en: String = ""","ultimoMensajeEn":"2026-11-02T15:24:00Z"""",
        noLeidos: Int = 2,
        soloLectura: Boolean = false
    ) = """{"id":"$id","contraparteId":"t-1","nombre":"Ana Torres","ultimoMensaje":"$ultimo"$en,"noLeidos":$noLeidos,
            "enLinea":true,"soloLectura":$soloLectura,"comoTutor":false}"""

    private fun mensaje(id: String, mio: Boolean = false, texto: String = "Texto $id") =
        """{"id":"$id","chatId":"c-1","texto":"$texto","enviadoEn":"2026-11-02T10:24:00-05:00","mio":$mio}"""

    private fun pagina(vararg mensajes: String, hayMas: Boolean = false) =
        """{"mensajes":[${mensajes.joinToString(",")}],"hayMas":$hayMas}"""

    /** Responde el historial una vez y después solo lo que se le indique para `desde`. */
    private fun historial(vararg mensajes: String, nuevos: () -> String = { pagina() }) {
        rutas.responderSegun("GET", rutaMensajes) { peticion ->
            val cuerpo = if (peticion.requestUrl!!.queryParameter("desde") == null) pagina(*mensajes) else nuevos()
            MockResponse().setBody(cuerpo)
        }
    }

    private fun desdes(): List<String?> = rutas.pedidas("GET", rutaMensajes).map { it.requestUrl!!.queryParameter("desde") }

    @Test
    fun `lee los chats del modo y convierte la fecha a hora de Colombia`() = enPrueba {
        val c = repositorio.obtenerChats().first().single()

        assertEquals("/api/chats?modo=ESTUDIANTE", rutas.peticiones.single().path)
        assertEquals("t-1", c.contraparteId)
        // 15:24 en UTC son las 10:24 en Colombia
        assertEquals(LocalDateTime.of(2026, 11, 2, 10, 24), c.hora)
        assertEquals(2, c.noLeidos)
    }

    @Test
    fun `un chat sin mensajes llega con el texto de la bandeja y sin hora`() = enPrueba {
        rutas.responder("GET", "/api/chats", "[${chat(ultimo = "", en = "")}]")
        val c = repositorio.obtenerChats().first().single()
        assertEquals("Inicia la conversación", c.ultimoMensaje)
        assertNull(c.hora)
    }

    @Test
    fun `lee los mensajes con su hora y mio`() = enPrueba {
        historial(mensaje("m-1", mio = true))
        val m = repositorio.obtenerMensajes("c-1").first().single()
        assertEquals(LocalDateTime.of(2026, 11, 2, 10, 24), m.hora)
        assertTrue(m.mio)
    }

    @Test
    fun `el historial recorre hacia atras lo que no cabe en una pagina`() = enPrueba {
        rutas.responderSegun("GET", rutaMensajes) { peticion ->
            val cuerpo = if (peticion.requestUrl!!.queryParameter("antes") == "m-3") pagina(mensaje("m-1"), mensaje("m-2"))
            else pagina(mensaje("m-3"), mensaje("m-4"), hayMas = true)
            MockResponse().setBody(cuerpo)
        }
        assertEquals(listOf("m-1", "m-2", "m-3", "m-4"), repositorio.obtenerMensajes("c-1").first().map { it.id })
    }

    @Test
    fun `un fallo de lectura se propaga y reintentar vuelve a pedir`() = enPrueba {
        rutas.cortar("GET", rutaMensajes)
        try {
            repositorio.obtenerMensajes("c-1").first()
            fail("Debía fallar")
        } catch (e: IOException) {
            // La pantalla muestra el error con "Reintentar"
        }
        historial(mensaje("m-1"))
        assertEquals(1, repositorio.obtenerMensajes("c-1").first().size)
    }

    @Test
    fun `sondea los chats mientras hay suscriptores y deja de hacerlo al salir`() = enPrueba {
        val sondeado = crear(intervaloMs = 30)
        val suscrito = alcance.launch { sondeado.obtenerChats().collect() }
        delay(300)
        assertTrue(rutas.pedidas("GET", "/api/chats").size > 2)

        suscrito.cancelAndJoin()
        delay(60)
        val alSalir = rutas.pedidas("GET", "/api/chats").size
        delay(150)
        assertEquals(alSalir, rutas.pedidas("GET", "/api/chats").size)
    }

    @Test
    fun `varios suscriptores comparten un solo sondeo`() = enPrueba {
        val sondeado = crear(intervaloMs = 100)
        val uno = alcance.launch { sondeado.obtenerChats().collect() }
        val dos = alcance.launch { sondeado.obtenerChats().collect() }
        delay(550)
        uno.cancelAndJoin()
        dos.cancelAndJoin()
        // Unas 6 lecturas en ese tiempo; con un sondeo por suscriptor serían el doble
        assertTrue(rutas.pedidas("GET", "/api/chats").size in 2..8)
    }

    @Test
    fun `un fallo del sondeo no borra lo que se ve y se recupera en la siguiente consulta`() = enPrueba {
        val sondeado = crear(intervaloMs = 30)
        val vistos = java.util.concurrent.CopyOnWriteArrayList<Int>()
        val suscrito = alcance.launch { sondeado.obtenerChats().collect { vistos += it.single().noLeidos } }
        while (vistos.isEmpty()) delay(5)

        rutas.cortar("GET", "/api/chats")
        delay(100)
        assertTrue(suscrito.isActive)
        rutas.responder("GET", "/api/chats", "[${chat(noLeidos = 5)}]")
        while (5 !in vistos) delay(5)

        suscrito.cancelAndJoin()
        assertEquals(listOf(2, 5), vistos.toList())
    }

    @Test
    fun `el sondeo de mensajes usa desde, pide solo lo nuevo y no duplica`() = enPrueba {
        val sondeado = crear(intervaloMs = 30)
        var respondidos = 0
        historial(mensaje("m-1")) {
            if (respondidos++ == 0) pagina(mensaje("m-2")) else pagina()
        }
        val vistas = java.util.concurrent.CopyOnWriteArrayList<List<String>>()
        val suscrito = alcance.launch { sondeado.obtenerMensajes("c-1").collect { l -> vistas += l.map { it.id } } }
        while (vistas.none { it.size == 2 }) delay(5)
        delay(100)
        suscrito.cancelAndJoin()

        assertEquals(listOf(listOf("m-1"), listOf("m-1", "m-2")), vistas.toList())
        val cursores = desdes()
        assertEquals(listOf(null, "m-1"), cursores.take(2))
        assertEquals(setOf("m-2"), cursores.drop(2).toSet())
    }

    @Test
    fun `un envio propio no adelanta el cursor y lo que escribio la otra persona antes sigue llegando`() = enPrueba {
        val sondeado = crear(intervaloMs = 40)
        var deLaOtraPersona = false
        historial(mensaje("m-1")) {
            if (deLaOtraPersona) pagina(mensaje("m-2"), mensaje("m-3", mio = true)) else pagina()
        }
        rutas.responder("POST", rutaMensajes, mensaje("m-3", mio = true), 201)
        rutas.responder("POST", "/api/chats/c-1/leido", "", 204)
        val vistas = java.util.concurrent.CopyOnWriteArrayList<List<String>>()
        val suscrito = alcance.launch { sondeado.obtenerMensajes("c-1").collect { l -> vistas += l.map { it.id } } }
        while (vistas.isEmpty()) delay(5)

        sondeado.enviarMensaje("c-1", "Texto m-3")
        deLaOtraPersona = true
        while (vistas.none { "m-2" in it }) delay(5)
        suscrito.cancelAndJoin()

        assertEquals(setOf("m-1", "m-2", "m-3"), vistas.last().toSet())
        assertEquals(3, vistas.last().size)
        assertEquals("m-1", desdes().filterNotNull().first())
    }

    @Test
    fun `lo que llega con la conversacion abierta queda leido`() = enPrueba {
        val sondeado = crear(intervaloMs = 30)
        var respondidos = 0
        historial(mensaje("m-1")) { if (respondidos++ == 0) pagina(mensaje("m-2")) else pagina() }
        rutas.responder("POST", "/api/chats/c-1/leido", "", 204)
        val suscrito = alcance.launch { sondeado.obtenerMensajes("c-1").collect() }
        while (rutas.pedidas("POST", "/api/chats/c-1/leido").isEmpty()) delay(5)
        suscrito.cancelAndJoin()
    }

    @Test
    fun `enviar lleva idCliente, aparece en los mensajes y en la bandeja`() = enPrueba {
        repositorio.obtenerChats().first()
        historial(mensaje("m-1"))
        repositorio.obtenerMensajes("c-1").first()
        rutas.responder("POST", rutaMensajes, mensaje("m-2", mio = true, texto = "Nuevo"), 201)
        rutas.responder("GET", "/api/chats", "[${chat(ultimo = "Nuevo")}]")

        repositorio.enviarMensaje("c-1", "Nuevo")

        val cuerpo = rutas.pedidas("POST", rutaMensajes).single().cuerpoTexto()
        assertTrue(cuerpo, cuerpo.contains(""""texto":"Nuevo""""))
        assertTrue(cuerpo, Regex(""""idCliente":"[0-9a-f-]{36}"""").containsMatchIn(cuerpo))
        assertEquals(listOf("m-1", "m-2"), repositorio.obtenerMensajes("c-1").first().map { it.id })
        assertEquals("Nuevo", repositorio.obtenerChats().first().single().ultimoMensaje)
    }

    @Test
    fun `reintentar tras un corte reutiliza el idCliente y un mensaje nuevo lleva otro`() = enPrueba {
        rutas.cortar("POST", rutaMensajes)
        try {
            repositorio.enviarMensaje("c-1", "Hola")
            fail("Debía fallar")
        } catch (e: IOException) {
            // La pantalla devuelve el texto a la caja para reintentar
        }
        rutas.responder("POST", rutaMensajes, mensaje("m-1", mio = true), 201)
        repositorio.enviarMensaje("c-1", "Hola")
        repositorio.enviarMensaje("c-1", "Hola")

        val ids = rutas.pedidas("POST", rutaMensajes).map {
            Regex(""""idCliente":"([^"]+)"""").find(it.cuerpoTexto())!!.groupValues[1]
        }
        assertEquals(3, ids.size)
        assertEquals(ids[0], ids[1])
        assertNotEquals(ids[1], ids[2])
    }

    @Test
    fun `un chat en solo lectura rechaza el mensaje y la pantalla se entera`() = enPrueba {
        repositorio.obtenerChats().first()
        rutas.fallar("POST", rutaMensajes, 409, CodigoError.CHAT_SOLO_LECTURA)
        rutas.responder("GET", "/api/chats", "[${chat(soloLectura = true)}]")
        try {
            repositorio.enviarMensaje("c-1", "Hola")
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.CHAT_SOLO_LECTURA, e.codigo)
        }
        assertTrue(repositorio.obtenerChats().first().single().soloLectura)
    }

    @Test
    fun `marcar leido deja la bandeja en cero`() = enPrueba {
        repositorio.obtenerChats().first()
        rutas.responder("POST", "/api/chats/c-1/leido", "", 204)

        repositorio.marcarLeido("c-1")

        assertEquals(0, repositorio.obtenerChats().first().single().noLeidos)
    }

    @Test
    fun `asegurar el chat lo busca con el modo de la conversacion y devuelve su id`() = enPrueba {
        rutas.responder("GET", "/api/chats", "[]")
        repositorio.obtenerChats().first()
        rutas.responder("GET", "/api/chats/con/e-1", chat(id = "c-7"))

        val id = repositorio.asegurarChat("e-1", "Luis", enLinea = false, comoTutor = true)

        assertEquals("c-7", id)
        assertEquals("/api/chats/con/e-1?modo=TUTOR", rutas.pedidas("GET", "/api/chats/con/e-1").single().path)
        assertEquals("c-7", repositorio.obtenerChats().first().single().id)
    }

    @Test
    fun `sin solicitud aceptada asegurar el chat llega como CHAT_NO_HABILITADO`() = enPrueba {
        rutas.fallar("GET", "/api/chats/con/t-1", 404, CodigoError.CHAT_NO_HABILITADO)
        try {
            repositorio.asegurarChat("t-1", "Ana", enLinea = false, comoTutor = false)
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.CHAT_NO_HABILITADO, e.codigo)
        }
    }

    @Test
    fun `habilitar y cerrar el chat no llaman al servidor`() = enPrueba {
        repositorio.habilitarChat("t-1", "Ana", enLinea = false, comoTutor = false)
        repositorio.cerrarChat("t-1")
        assertTrue(rutas.peticiones.isEmpty())
    }

    @Test
    fun `al cambiar de modo o cerrar sesion se olvidan los chats`() = enPrueba {
        repositorio.obtenerChats().first()
        almacen.guardar(sesionDePrueba(rol = RolDto.TUTOR))
        repositorio.obtenerChats().first()
        almacen.borrar()
        almacen.guardar(sesionDePrueba(rol = RolDto.TUTOR))
        repositorio.obtenerChats().first()
        assertEquals(
            listOf("/api/chats?modo=ESTUDIANTE", "/api/chats?modo=TUTOR", "/api/chats?modo=TUTOR"),
            rutas.pedidas("GET", "/api/chats").map { it.path }
        )
    }
}
