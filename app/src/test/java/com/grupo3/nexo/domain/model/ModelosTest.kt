package com.grupo3.nexo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelosTest {

    @Test
    fun `claveFecha ordena cronologicamente dentro del semestre`() {
        assertTrue(claveFecha("24 oct") < claveFecha("1 nov"))
        assertTrue(claveFecha("24 oct", "09:00") < claveFecha("24 oct", "16:30"))
    }

    @Test
    fun `claveFecha cuenta enero a junio como del año siguiente`() {
        assertTrue(claveFecha("30 dic") < claveFecha("5 ene"))
    }

    @Test
    fun `claveFecha tolera fechas mal formadas`() {
        assertEquals(claveFecha("???"), claveFecha(""))
    }

    @Test
    fun `una solicitud puede expirar`() {
        assertTrue(EstadoSolicitud.entries.contains(EstadoSolicitud.EXPIRADA))
        assertEquals("Expirada", EstadoSolicitud.EXPIRADA.etiqueta)
    }
}
