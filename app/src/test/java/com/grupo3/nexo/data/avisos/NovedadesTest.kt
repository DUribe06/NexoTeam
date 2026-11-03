package com.grupo3.nexo.data.avisos

import com.grupo3.nexo.data.remote.ChatDto
import com.grupo3.nexo.data.remote.EstadoSesionDto
import com.grupo3.nexo.data.remote.EstadoSolicitudDto
import com.grupo3.nexo.data.remote.ModalidadDto
import com.grupo3.nexo.data.remote.SesionTutoriaDto
import com.grupo3.nexo.data.remote.SolicitudDto
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NovedadesTest {

    private val ahora = Instant.parse("2026-11-03T14:00:00Z") // 9:00 a. m. en Colombia

    private fun solicitud(id: String, estado: EstadoSolicitudDto) = SolicitudDto(
        id = id, tutorId = "t", nombreTutor = "Ana", estudianteId = "e", nombreEstudiante = "Vale", materia = "Cálculo I",
        tema = "Repaso", fecha = "2026-11-05", hora = "10:00", modalidad = ModalidadDto.VIRTUAL, estado = estado, mensaje = "",
        enviada = "2026-11-03T08:00:00-05:00", venceEn = "2026-11-04T08:00:00-05:00"
    )

    private fun chat(noLeidos: Int) = ChatDto(
        id = "c", contraparteId = "p", nombre = "Ana", ultimoMensaje = "Hola", noLeidos = noLeidos, enLinea = false,
        soloLectura = false, comoTutor = false
    )

    private fun sesion(hora: String, comoTutor: Boolean = false, estado: EstadoSesionDto = EstadoSesionDto.PROGRAMADA) = SesionTutoriaDto(
        id = "s", solicitudId = "x", contraparteId = "p", conQuien = "Ana", materia = "Cálculo I", fecha = "2026-11-03", hora = hora,
        duracionHoras = 2.0, modalidad = ModalidadDto.PRESENCIAL, estado = estado, comoTutor = comoTutor
    )

    private fun datos(
        recibidas: List<SolicitudDto> = emptyList(), enviadas: List<SolicitudDto> = emptyList(),
        chats: List<ChatDto> = emptyList(), sesiones: List<SesionTutoriaDto> = emptyList()
    ) = DatosNovedades(recibidas, enviadas, chats, sesiones)

    @Test
    fun `la primera revision no avisa lo que ya estaba`() {
        val (novedades, foto) = calcularNovedades(
            null,
            datos(recibidas = listOf(solicitud("a", EstadoSolicitudDto.PENDIENTE)), chats = listOf(chat(5)), sesiones = listOf(sesion("10:00"))),
            ahora
        )
        assertTrue(novedades.isEmpty())
        assertEquals(setOf("a"), foto.recibidas)
        assertEquals(setOf("s"), foto.recordadas)
    }

    @Test
    fun `una solicitud recibida se avisa una sola vez`() {
        val d = datos(recibidas = listOf(solicitud("a", EstadoSolicitudDto.PENDIENTE)))
        val (primera, foto) = calcularNovedades(FotoNovedades(), d, ahora)
        assertEquals(listOf("Nueva solicitud de Vale"), primera.map { it.titulo })
        assertEquals("Cálculo I · 5 de noviembre, 10:00", primera.single().texto)
        assertTrue(calcularNovedades(foto, d, ahora).first.isEmpty())
    }

    @Test
    fun `avisa cuando responden o vence lo que estaba pendiente y no lo demas`() {
        val antes = FotoNovedades(
            enviadas = mapOf(
                "a" to EstadoSolicitudDto.PENDIENTE, "b" to EstadoSolicitudDto.PENDIENTE, "c" to EstadoSolicitudDto.PENDIENTE,
                "d" to EstadoSolicitudDto.ACEPTADA, "e" to EstadoSolicitudDto.PENDIENTE
            )
        )
        val d = datos(
            enviadas = listOf(
                solicitud("a", EstadoSolicitudDto.ACEPTADA), solicitud("b", EstadoSolicitudDto.RECHAZADA), solicitud("c", EstadoSolicitudDto.EXPIRADA),
                solicitud("d", EstadoSolicitudDto.FINALIZADA), solicitud("e", EstadoSolicitudDto.CANCELADA)
            )
        )
        assertEquals(
            listOf("Ana aceptó tu solicitud", "Ana no aceptó tu solicitud", "Ana no respondió a tiempo"),
            calcularNovedades(antes, d, ahora).first.map { it.titulo }
        )
    }

    @Test
    fun `un mensaje avisa si sube el contador y no si baja o sigue igual`() {
        val antes = FotoNovedades(noLeidos = mapOf("c" to 2))
        assertEquals(1, calcularNovedades(antes, datos(chats = listOf(chat(3))), ahora).first.size)
        assertTrue(calcularNovedades(antes, datos(chats = listOf(chat(2))), ahora).first.isEmpty())
        assertTrue(calcularNovedades(antes, datos(chats = listOf(chat(0))), ahora).first.isEmpty())
    }

    @Test
    fun `recuerda la tutoria en la hora anterior una sola vez y al tutor presencial el PIN`() {
        val d = datos(sesiones = listOf(sesion("10:00", comoTutor = true)))
        val (novedades, foto) = calcularNovedades(FotoNovedades(), d, ahora)
        assertEquals("Tu tutoría de Cálculo I es a las 10:00", novedades.single().titulo)
        assertTrue(novedades.single().texto.contains("PIN"))
        assertTrue(calcularNovedades(foto, d, ahora).first.isEmpty())
    }

    @Test
    fun `no recuerda una tutoria lejana ya pasada o que no sigue programada`() {
        assertTrue(calcularNovedades(FotoNovedades(), datos(sesiones = listOf(sesion("12:00"))), ahora).first.isEmpty())
        assertTrue(calcularNovedades(FotoNovedades(), datos(sesiones = listOf(sesion("08:00"))), ahora).first.isEmpty())
        assertTrue(
            calcularNovedades(FotoNovedades(), datos(sesiones = listOf(sesion("10:00", estado = EstadoSesionDto.CANCELADA))), ahora).first.isEmpty()
        )
    }
}
