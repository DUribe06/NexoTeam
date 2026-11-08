package com.grupo3.nexo.ui.chat

import com.grupo3.nexo.domain.model.EstadoSesion
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Sesion
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AcuerdosTest {

    private val inicio = LocalDateTime.of(2026, 11, 3, 18, 0)

    private fun sesion(modalidad: Modalidad = Modalidad.PRESENCIAL, duracion: Double = 2.0) = Sesion(
        id = "s", contraparteId = "c", conQuien = "Carlos Mendoza", materia = "Python", fecha = inicio.toLocalDate(),
        hora = inicio.toLocalTime(), duracionHoras = duracion, modalidad = modalidad, estado = EstadoSesion.PROGRAMADA
    )

    @Test
    fun `solo una presencial de hoy sin empezar admite acuerdos`() {
        val hoy = inicio.toLocalDate()
        assertTrue(sesion().admiteAcuerdo(hoy))
        assertFalse(sesion().admiteAcuerdo(hoy.plusDays(1)))
        assertFalse(sesion(Modalidad.VIRTUAL).admiteAcuerdo(hoy))
        assertFalse(sesion().copy(iniciada = true).admiteAcuerdo(hoy))
        assertFalse(sesion().copy(estado = EstadoSesion.CANCELADA).admiteAcuerdo(hoy))
    }

    @Test
    fun `solo ofrece horas que no han pasado y que caben en la sesion`() {
        assertEquals(listOf(15, 30, 45, 60), sesion().minutosDeRetardo(inicio.minusMinutes(5)))
        assertEquals(listOf(30, 45, 60), sesion().minutosDeRetardo(inicio.plusMinutes(15)))
        assertEquals(listOf(15, 30, 45), sesion(duracion = 1.0).minutosDeRetardo(inicio))
    }
}
