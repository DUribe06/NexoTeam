package com.grupo3.nexo.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `el registro envia solo los campos acordados`() {
        val cuerpo = json.encodeToString(
            SolicitudRegistro("Ana", "ana@correo.co", "clave1234", aceptaTerminos = true, versionTerminos = "2026-10-18")
        )
        assertEquals(
            setOf("nombre", "correo", "contrasena", "aceptaTerminos", "versionTerminos"),
            json.parseToJsonElement(cuerpo).jsonObject.keys
        )
    }

    @Test
    fun `lee la sesion del contrato`() {
        val dto = json.decodeFromString<SesionDto>(
            """{"tokenAcceso":"a","tokenRefresco":"r","expiraEnSegundos":900,
                "usuario":{"id":"7f6a","nombre":"Ana","correo":"ana@correo.co","rol":"TUTOR","estadoCuenta":"ACTIVA"}}"""
        )
        assertEquals("a", dto.tokenAcceso)
        assertEquals(900, dto.expiraEnSegundos)
        assertEquals(RolDto.TUTOR, dto.usuario.rol)
    }

    @Test
    fun `un rol o estado de cuenta que la app no conoce no tumba la lectura`() {
        val admin = json.decodeFromString<UsuarioDto>(
            """{"id":"1","nombre":"Ana","correo":"a@b.co","rol":"ADMIN","estadoCuenta":"ACTIVA"}"""
        )
        val nuevo = json.decodeFromString<UsuarioDto>(
            """{"id":"1","nombre":"Ana","correo":"a@b.co","rol":"MONITOR","estadoCuenta":"EN_REVISION"}"""
        )
        assertEquals(RolDto.ADMIN, admin.rol)
        assertEquals(RolDto.DESCONOCIDO, nuevo.rol)
        assertEquals(EstadoCuentaDto.DESCONOCIDO, nuevo.estadoCuenta)
    }

    @Test
    fun `lee la respuesta del registro con y sin vencimiento`() {
        val con = json.decodeFromString<RespuestaRegistro>(
            """{"usuarioId":"1","correo":"a@b.co","estadoCuenta":"PENDIENTE_VERIFICACION_CORREO","codigoVenceEnSegundos":900}"""
        )
        val sin = json.decodeFromString<RespuestaRegistro>(
            """{"usuarioId":"1","correo":"a@b.co","estadoCuenta":"PENDIENTE_VERIFICACION_CORREO"}"""
        )
        assertEquals(EstadoCuentaDto.PENDIENTE_VERIFICACION_CORREO, con.estadoCuenta)
        assertEquals(900, con.codigoVenceEnSegundos)
        assertNull(sin.codigoVenceEnSegundos)
    }

    @Test
    fun `los detalles del error son opcionales`() {
        val sin = json.decodeFromString<ErrorDto>("""{"codigo":"X","mensaje":"m"}""")
        val con = json.decodeFromString<ErrorDto>(
            """{"codigo":"X","mensaje":"m","detalles":[{"campo":"correo","problema":"no es válido"}]}"""
        )
        assertEquals(emptyList<DetalleErrorDto>(), sin.detalles)
        assertEquals(DetalleErrorDto("correo", "no es válido"), con.detalles.single())
    }

    @Test
    fun `la sesion guardada sobrevive a ida y vuelta`() {
        val sesion = SesionDto(
            "a", "r", 900,
            UsuarioDto("1", "Ana", "a@b.co", RolDto.ESTUDIANTE, EstadoCuentaDto.ACTIVA)
        )
        assertEquals(sesion, json.decodeFromString<SesionDto>(json.encodeToString(sesion)))
    }
}
