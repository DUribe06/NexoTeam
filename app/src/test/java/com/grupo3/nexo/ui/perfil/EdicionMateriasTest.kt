package com.grupo3.nexo.ui.perfil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EdicionMateriasTest {

    @Test
    fun `agrega al final, sin espacios de sobra`() {
        val r = listOf("Java").agregarMateria("  Cálculo   I ")
        assertEquals(listOf("Java", "Cálculo I"), r.lista)
        assertNull(r.aviso)
    }

    @Test
    fun `no repite aunque cambien mayusculas o tildes`() {
        val r = listOf("Cálculo I").agregarMateria("CALCULO i")
        assertEquals(listOf("Cálculo I"), r.lista)
        assertNotNull(r.aviso)
    }

    @Test
    fun `un texto vacio no hace nada ni avisa`() {
        val r = listOf("Java").agregarMateria("   ")
        assertEquals(listOf("Java"), r.lista)
        assertNull(r.aviso)
    }

    @Test
    fun `respeta los limites del servidor`() {
        assertNotNull(emptyList<String>().agregarMateria("x".repeat(81)).aviso)
        assertEquals(1, emptyList<String>().agregarMateria("x".repeat(80)).lista.size)
        val llena = List(20) { "Materia $it" }
        assertEquals(llena, llena.agregarMateria("Otra").lista)
    }

    @Test
    fun `las sugerencias omiten lo que ya tiene y filtran por lo escrito`() {
        val todas = sugerenciasDeMaterias(emptyList(), "")
        assertTrue("Python" in todas)
        assertFalse("Python" in sugerenciasDeMaterias(listOf("python"), ""))
        assertEquals(listOf("Cálculo"), sugerenciasDeMaterias(emptyList(), "calcu"))
    }
}
