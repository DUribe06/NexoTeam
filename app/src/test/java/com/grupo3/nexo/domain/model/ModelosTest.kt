package com.grupo3.nexo.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelosTest {

    private val ahora = LocalDateTime.of(2026, 11, 1, 12, 0)

    private fun solicitud(estado: EstadoSolicitud, enviadaHaceHoras: Long) = Solicitud(
        id = "s1",
        tutorId = "t1",
        nombreTutor = "Ana Torres",
        estudianteId = "u1",
        nombreEstudiante = "Diego",
        materia = "Cálculo I",
        tema = "Límites",
        fecha = LocalDate.of(2026, 11, 3),
        hora = LocalTime.of(10, 0),
        modalidad = Modalidad.VIRTUAL,
        estado = estado,
        mensaje = "",
        enviada = ahora.minusHours(enviadaHaceHoras)
    )

    @Test
    fun `sin venceEn del servidor el plazo es 24 h tras el envio, contando minutos completos`() {
        val s = solicitud(EstadoSolicitud.PENDIENTE, 0)
        assertEquals(s.enviada.plusHours(24), s.venceEn)
        // A los 10 min 30 s de enviada solo cuentan 10 minutos: 23 h 50 min, como antes de existir venceEn
        assertEquals(1430 / 60.0, s.horasRestantes(s.enviada.plusMinutes(10).plusSeconds(30)), 1e-9)
        assertEquals(0.0, s.horasRestantes(s.enviada.plusHours(24).plusSeconds(30)), 1e-9)
    }

    @Test
    fun `horasRestantes usa venceEn aunque llegue antes de las 24 h`() {
        val s = solicitud(EstadoSolicitud.PENDIENTE, 2).copy(venceEn = ahora.plusHours(3))
        assertEquals(3.0, s.horasRestantes(ahora), 1e-9)
        assertEquals(EstadoSolicitud.EXPIRADA, s.conPlazoAplicado(ahora.plusHours(3)).estado)
    }

    @Test
    fun `una solicitud puede expirar`() {
        assertTrue(EstadoSolicitud.entries.contains(EstadoSolicitud.EXPIRADA))
        assertEquals("Expirada", EstadoSolicitud.EXPIRADA.etiqueta)
    }

    @Test
    fun `una pendiente expira al vencer el plazo de 24 h`() {
        assertEquals(EstadoSolicitud.PENDIENTE, solicitud(EstadoSolicitud.PENDIENTE, 23).conPlazoAplicado(ahora).estado)
        assertEquals(EstadoSolicitud.EXPIRADA, solicitud(EstadoSolicitud.PENDIENTE, 24).conPlazoAplicado(ahora).estado)
    }

    @Test
    fun `el plazo no cambia las solicitudes ya respondidas`() {
        assertEquals(EstadoSolicitud.ACEPTADA, solicitud(EstadoSolicitud.ACEPTADA, 48).conPlazoAplicado(ahora).estado)
    }

    @Test
    fun `las horas restantes se cuentan desde el envio`() {
        assertEquals(18.0, solicitud(EstadoSolicitud.PENDIENTE, 6).horasRestantes(ahora), 0.01)
    }

    @Test
    fun `el estudiante puede buscar otros tutores si su solicitud expira`() {
        assertEquals(AccionesSolicitud.VER_OTROS, solicitud(EstadoSolicitud.EXPIRADA, 30).acciones(esTutor = false))
        assertEquals(AccionesSolicitud.RESPONDER, solicitud(EstadoSolicitud.PENDIENTE, 1).acciones(esTutor = true))
    }
}
