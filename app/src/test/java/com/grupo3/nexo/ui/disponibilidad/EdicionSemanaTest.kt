package com.grupo3.nexo.ui.disponibilidad

import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.calendarioVacio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EdicionSemanaTest {

    private val virtual = setOf(Modalidad.VIRTUAL)
    private val presencial = setOf(Modalidad.PRESENCIAL)
    private val vacia = calendarioVacio()

    private fun Int.marcadasEn(dias: List<com.grupo3.nexo.domain.model.DiaDisponibilidad>) =
        assertEquals(this, dias.sumOf { it.horasActivas })

    @Test
    fun `el pincel marca una hora libre, cambia una distinta y borra una igual`() {
        assertEquals(virtual, destinoDe(emptySet(), virtual))
        assertEquals(virtual, destinoDe(presencial, virtual))
        assertEquals(emptySet<Modalidad>(), destinoDe(virtual, virtual))
    }

    @Test
    fun `un toque cambia solo esa hora`() {
        val c = Celda(2, 3)
        val semana = vacia.conTrazo(Trazo(c, c, virtual))
        1.marcadasEn(semana)
        assertEquals(virtual, semana[2].horas[3].modalidades)
    }

    @Test
    fun `arrastrar marca el rectangulo en cualquier direccion`() {
        // De jueves 14:00 hacia atrás, hasta lunes 10:00: 4 días por 3 horas
        val semana = vacia.conTrazo(Trazo(Celda(3, 3), Celda(0, 1), virtual))
        12.marcadasEn(semana)
        assertEquals(virtual, semana[0].horas[1].modalidades)
        assertEquals(emptySet<Modalidad>(), semana[4].horas[1].modalidades)
    }

    @Test
    fun `un trazo de borrado solo quita lo que cubre`() {
        val llena = vacia.conTrazo(Trazo(Celda(0, 0), Celda(1, 1), virtual))
        val semana = llena.conTrazo(Trazo(Celda(0, 0), Celda(0, 1), emptySet()))
        2.marcadasEn(semana)
        assertTrue(semana[0].horas.none { it.seleccionada })
    }

    @Test
    fun `tocar un dia lo marca completo y otra vez lo borra`() {
        val marcado = vacia.alternarDia(1, virtual)
        7.marcadasEn(marcado)
        0.marcadasEn(marcado.alternarDia(1, virtual))
    }

    @Test
    fun `tocar un dia a medias lo completa con el pincel, sin borrar`() {
        val aMedias = vacia.conTrazo(Trazo(Celda(1, 0), Celda(1, 0), presencial))
        val semana = aMedias.alternarDia(1, virtual)
        7.marcadasEn(semana)
        assertTrue(semana[1].horas.all { it.modalidades == virtual })
    }

    @Test
    fun `tocar una hora la marca en toda la semana y otra vez la borra`() {
        val marcada = vacia.alternarHora(2, virtual)
        7.marcadasEn(marcada)
        assertTrue(marcada.all { it.horas[2].modalidades == virtual })
        0.marcadasEn(marcada.alternarHora(2, virtual))
    }

    @Test
    fun `limpiar deja la semana sin horas pero con su estructura`() {
        val limpia = vacia.alternarDia(0, virtual).sinHoras()
        0.marcadasEn(limpia)
        assertEquals(vacia.size, limpia.size)
        assertEquals(vacia[0].horas.size, limpia[0].horas.size)
    }
}
