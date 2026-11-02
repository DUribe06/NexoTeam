package com.grupo3.nexo.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SaludDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `lee la respuesta de salud del backend`() {
        val dto = json.decodeFromString<RespuestaSalud>("""{"estado":"ok","servicio":"nexo-backend"}""")
        assertEquals("ok", dto.estado)
        assertEquals("nexo-backend", dto.servicio)
    }

    @Test
    fun `ignora campos nuevos que agregue el backend`() {
        val dto = json.decodeFromString<RespuestaSalud>("""{"estado":"ok","servicio":"x","version":"2"}""")
        assertEquals("x", dto.servicio)
    }
}
