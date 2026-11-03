package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.remote.AlmacenEnMemoria
import com.grupo3.nexo.data.remote.RolDto
import com.grupo3.nexo.data.remote.enPrueba
import com.grupo3.nexo.data.remote.sesionDePrueba
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EstadoRemotoTest {
    private val almacen = AlmacenEnMemoria(sesionDePrueba())
    private val contexto = ContextoRemoto(almacen)
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lecturas = AtomicInteger()

    @After
    fun cerrar() = alcance.cancel()

    @Test
    fun `dos suscriptores seguidos comparten una sola lectura`() = enPrueba {
        val estado = EstadoRemoto<Int>(contexto) { _, _ -> lecturas.incrementAndGet() }
        assertEquals(1, estado.flujo().first())
        assertEquals(1, estado.flujo().first())
        assertEquals(1, lecturas.get())
    }

    @Test
    fun `una lectura fallida se propaga y reintentar vuelve a pedir`() = enPrueba {
        var falla = true
        val estado = EstadoRemoto<Int>(contexto) { _, _ ->
            lecturas.incrementAndGet()
            if (falla) throw IOException("sin red") else 7
        }
        try {
            estado.flujo().first()
            fail("Debía fallar")
        } catch (e: IOException) {
            assertEquals(1, lecturas.get())
        }
        falla = false
        assertEquals(7, estado.flujo().first())
        assertEquals(2, lecturas.get())
    }

    @Test
    fun `refrescar actualiza a quien ya esta suscrito`() = enPrueba {
        val estado = EstadoRemoto<Int>(contexto) { _, _ -> lecturas.incrementAndGet() }
        val vistos = mutableListOf<Int>()
        val suscrito = alcance.launch { estado.flujo().collect { vistos += it } }
        delay(100)
        estado.refrescar()
        delay(100)
        suscrito.cancelAndJoin()
        assertEquals(listOf(1, 2), vistos)
    }

    @Test
    fun `un refresco fallido conserva lo que hay y la proxima suscripcion lo vuelve a pedir`() = enPrueba {
        var falla = false
        val estado = EstadoRemoto<Int>(contexto) { _, _ ->
            if (falla) throw IOException("sin red") else lecturas.incrementAndGet()
        }
        assertEquals(1, estado.flujo().first())
        falla = true
        estado.refrescar()
        assertEquals(1, estado.flujo().first())

        falla = false
        val visto = CompletableDeferred<Int>()
        val suscrito = alcance.launch { estado.flujo().collect { if (it == 2) visto.complete(it) } }
        assertEquals(2, visto.await())
        suscrito.cancelAndJoin()
    }

    @Test
    fun `refrescar sin nada en memoria no pide nada`() = enPrueba {
        val estado = EstadoRemoto<Int>(contexto) { _, _ -> lecturas.incrementAndGet() }
        estado.refrescar()
        assertEquals(0, lecturas.get())
    }

    @Test
    fun `al cambiar de modo o de usuario se limpia y se pide con la clave nueva`() = enPrueba {
        val claves = mutableListOf<ClaveDatos>()
        val estado = EstadoRemoto<Int>(contexto) { clave, _ ->
            claves += clave
            lecturas.incrementAndGet()
        }
        estado.flujo().first()
        almacen.guardar(sesionDePrueba(rol = RolDto.TUTOR))
        assertEquals(2, estado.flujo().first())
        almacen.guardar(sesionDePrueba(rol = RolDto.TUTOR, id = "u-2"))
        assertEquals(3, estado.flujo().first())
        assertEquals(
            listOf("u-1" to "ESTUDIANTE", "u-1" to "TUTOR", "u-2" to "TUTOR"),
            claves.map { it.usuarioId to it.modo.name }
        )
    }

    @Test
    fun `al cerrar sesion no queda nada y al volver se pide de nuevo`() = enPrueba {
        val estado = EstadoRemoto<Int>(contexto) { _, _ -> lecturas.incrementAndGet() }
        estado.flujo().first()

        almacen.borrar()
        assertNull(contexto.clave.value)
        val sinSesion = alcance.launch { estado.flujo().collect { fail("Sin sesión no debe emitir") } }
        delay(100)
        sinSesion.cancelAndJoin()
        assertEquals(1, lecturas.get())

        almacen.guardar(sesionDePrueba())
        assertEquals(2, estado.flujo().first())
    }

    @Test
    fun `una cuenta de administrador no tiene datos que pedir`() {
        almacen.guardar(sesionDePrueba(rol = RolDto.ADMIN))
        assertNull(contexto.clave.value)
    }

    @Test
    fun `una respuesta que llega despues de cambiar de modo se descarta`() = enPrueba {
        val enCamino = CompletableDeferred<Unit>()
        val respuesta = CompletableDeferred<Int>()
        val estado = EstadoRemoto<Int>(contexto) { clave, _ ->
            if (clave.modo.name == "ESTUDIANTE") {
                enCamino.complete(Unit)
                respuesta.await()
            } else {
                99
            }
        }
        val viejo = alcance.launch { estado.flujo().first() }
        enCamino.await()
        almacen.guardar(sesionDePrueba(rol = RolDto.TUTOR))
        respuesta.complete(1)
        viejo.join()
        assertEquals(99, estado.flujo().first())
    }

    @Test
    fun `sondea solo mientras hay suscriptores`() = enPrueba {
        val estado = EstadoRemoto<Int>(contexto) { _, _ -> lecturas.incrementAndGet() }
        val sondeo = estado.sondear(alcance, 20)
        delay(100)
        assertEquals(0, lecturas.get())

        val suscrito = alcance.launch { estado.flujo().collect() }
        delay(200)
        val conSuscriptor = lecturas.get()
        assertTrue("Debía sondear: $conSuscriptor", conSuscriptor > 2)

        suscrito.cancelAndJoin()
        delay(50)
        val alSalir = lecturas.get()
        delay(150)
        assertEquals(alSalir, lecturas.get())
        sondeo.cancelAndJoin()
    }
}
