package com.grupo3.nexo.ui.inicio

import com.grupo3.nexo.domain.model.CierreSesion
import com.grupo3.nexo.domain.model.EstadoSesion
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.NivelPenalizacion
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.inicio
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaseDeSesionTest {

    private val inicio = LocalDateTime.of(2026, 11, 3, 18, 0)

    private fun sesion(
        cuando: LocalDateTime = inicio,
        modalidad: Modalidad = Modalidad.VIRTUAL,
        estado: EstadoSesion = EstadoSesion.PROGRAMADA,
        calificacion: Int? = null
    ) = Sesion(
        id = "s", contraparteId = "c", conQuien = "Diego", materia = "Python", fecha = cuando.toLocalDate(),
        hora = cuando.toLocalTime(), duracionHoras = 1.0, modalidad = modalidad, estado = estado, calificacion = calificacion
    )

    private fun fase(ahora: LocalDateTime, vararg proximas: Sesion, historial: List<Sesion> = emptyList(), pendientes: Int = 0) =
        faseDe(proximas.toList(), historial, pendientes, ahora)

    @Test
    fun `la sesion avanza sola con la hora, en virtual`() {
        val s = sesion()
        assertEquals(FaseSesion.AGENDADA, fase(inicio.minusDays(1), s))
        assertEquals(FaseSesion.HOY, fase(inicio.minusMinutes(31), s))
        assertEquals(FaseSesion.YA_CASI_VIRTUAL, fase(inicio.minusMinutes(30), s))
        assertEquals(FaseSesion.YA_CASI_VIRTUAL, fase(inicio.minusSeconds(1), s))
        assertEquals(FaseSesion.EN_SESION_VIRTUAL, fase(inicio, s))
        assertEquals(FaseSesion.EN_SESION_VIRTUAL, fase(inicio.plusMinutes(59), s))
        assertEquals(FaseSesion.CALIFICAR, fase(inicio.plusMinutes(60), s))
    }

    @Test
    fun `en presencial la hora no la inicia, solo el PIN`() {
        val s = sesion(modalidad = Modalidad.PRESENCIAL)
        assertEquals(FaseSesion.HOY, fase(inicio.minusMinutes(31), s))
        assertEquals(FaseSesion.YA_CASI, fase(inicio.minusMinutes(20), s))
        assertEquals(FaseSesion.YA_CASI, fase(inicio.plusMinutes(5), s))
        val enviado = s.copy(pinEnviado = true)
        assertEquals(FaseSesion.LLEGO, fase(inicio.minusMinutes(5), enviado))
        assertEquals(FaseSesion.EN_SESION, fase(inicio.plusMinutes(5), enviado.copy(iniciada = true)))
    }

    private fun presencial(comoTutor: Boolean = false) = sesion(modalidad = Modalidad.PRESENCIAL)
        .copy(comoTutor = comoTutor, limite = inicio.plusMinutes(10))

    @Test
    fun `el tutor toca la barra para enviar el PIN y luego para escribirlo`() {
        val s = presencial(comoTutor = true)
        assertNull(avisoDePin(s, inicio.minusMinutes(31)))
        assertEquals(AccionPin.ENVIAR, avisoDePin(s, inicio.minusMinutes(30))?.accion)
        assertEquals(AccionPin.ESCRIBIR, avisoDePin(s.copy(pinEnviado = true), inicio.minusMinutes(5))?.accion)
        assertNull(avisoDePin(s.copy(pinEnviado = true, iniciada = true), inicio))
    }

    @Test
    fun `el estudiante solo ve el PIN cuando el tutor lo envia, y sin accion`() {
        val s = presencial()
        assertNull(avisoDePin(s, inicio.minusMinutes(5)))
        val esperando = avisoDePin(s, inicio.plusMinutes(1))
        assertEquals("Tu tutor aún no llega", esperando?.titulo)
        assertNull(esperando?.pin)
        val conPin = avisoDePin(s.copy(pinEnviado = true, pin = "4821"), inicio.minusMinutes(5))
        assertEquals("4821", conPin?.pin)
        assertEquals("Tu PIN es 4821", conPin?.titulo)
        assertNull(conPin?.accion)
    }

    @Test
    fun `con un retardo pactado el estudiante ve la nueva hora`() {
        val s = presencial().copy(horaPactada = inicio.toLocalTime().plusMinutes(30), limite = inicio.plusMinutes(40))
        val aviso = avisoDePin(s, inicio.minusMinutes(5))
        assertEquals("Nueva hora: 18:30", aviso?.titulo)
        assertTrue(aviso!!.detalle.contains("18:40"))
    }

    @Test
    fun `una virtual nunca pide PIN`() {
        assertNull(avisoDePin(sesion().copy(comoTutor = true, limite = inicio), inicio))
    }

    @Test
    fun `una sesion cancelada por no llegar se cuenta en la barra por unas horas`() {
        val s = sesion(modalidad = Modalidad.PRESENCIAL, estado = EstadoSesion.CANCELADA)
            .copy(cierre = CierreSesion.TUTOR_NO_LLEGO, comoTutor = true, penalizacion = NivelPenalizacion.NORMAL)
        val aviso = avisoDeCierre(listOf(s), inicio.plusMinutes(11))
        assertEquals("Sesión cancelada", aviso?.titulo)
        assertEquals("No enviaste el PIN a tiempo. Recibiste una penalización.", aviso?.detalle)
        assertNull(avisoDeCierre(listOf(s), inicio.plusHours(3)))
        assertNull(avisoDeCierre(listOf(s.copy(cierre = null)), inicio.plusMinutes(11)))
    }

    @Test
    fun `la noche anterior todavia no cuenta como hoy`() {
        val s = sesion(inicio.toLocalDate().atTime(8, 0))
        assertEquals(FaseSesion.AGENDADA, fase(s.inicio.minusHours(9), s))
        assertEquals(FaseSesion.HOY, fase(s.inicio.minusHours(2), s))
    }

    @Test
    fun `al terminar pide calificar y, ya calificada, sigue con lo proximo`() {
        val hecha = sesion(inicio.minusDays(1), estado = EstadoSesion.COMPLETADA)
        val calificada = hecha.copy(calificacion = 5)
        val siguiente = sesion(inicio.plusDays(2))
        assertEquals(FaseSesion.CALIFICAR, fase(inicio, siguiente, historial = listOf(hecha)))
        assertEquals(FaseSesion.AGENDADA, fase(inicio, siguiente, historial = listOf(calificada)))
    }

    @Test
    fun `una sesion vieja sin calificar no tapa lo demas`() {
        val vieja = sesion(inicio.minusDays(10), estado = EstadoSesion.COMPLETADA)
        assertEquals(FaseSesion.SOLA, fase(inicio, historial = listOf(vieja)))
    }

    @Test
    fun `una sesion cancelada no se califica`() {
        val cancelada = sesion(inicio.minusDays(1), estado = EstadoSesion.CANCELADA)
        assertEquals(FaseSesion.SOLA, fase(inicio, historial = listOf(cancelada)))
    }

    @Test
    fun `lo que pasa hoy va antes que calificar y lo inmediato antes que todo`() {
        val hecha = sesion(inicio.minusDays(1), estado = EstadoSesion.COMPLETADA)
        val hoy = sesion()
        assertEquals(FaseSesion.HOY, fase(inicio.minusHours(2), hoy, historial = listOf(hecha)))
        assertEquals(FaseSesion.EN_SESION_VIRTUAL, fase(inicio, hoy, historial = listOf(hecha)))
    }

    @Test
    fun `sin sesiones muestra la solicitud pendiente o la escena sola`() {
        assertEquals(FaseSesion.ENVIADA, fase(inicio, pendientes = 1))
        assertEquals(FaseSesion.SOLA, fase(inicio))
    }

    @Test
    fun `una sesion agendada va antes que una solicitud pendiente`() {
        assertEquals(FaseSesion.AGENDADA, fase(inicio.minusDays(1), sesion(), pendientes = 2))
    }

    @Test
    fun `los minutos que faltan cuentan como un reloj`() {
        val s = sesion()
        assertEquals(30L, s.minutosPara(inicio.minusMinutes(30)))
        assertEquals(12L, s.minutosPara(inicio.minusMinutes(11).minusSeconds(30)))
        assertEquals(1L, s.minutosPara(inicio.minusSeconds(5)))
    }
}
