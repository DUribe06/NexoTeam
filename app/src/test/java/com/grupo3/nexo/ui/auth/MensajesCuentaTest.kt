package com.grupo3.nexo.ui.auth

import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.DetalleError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.ResultadoAcceso
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MensajesCuentaTest {

    @Test
    fun `cada codigo del contrato tiene su mensaje propio`() {
        val generico = mensajeDeError(ErrorApi("CODIGO_QUE_NO_EXISTE", "x"))
        listOf(
            CodigoError.DATOS_INVALIDOS,
            CodigoError.CORREO_DUPLICADO,
            CodigoError.CORREO_SIN_MX,
            CodigoError.CORREO_DESECHABLE,
            CodigoError.CONTRASENA_DEBIL,
            CodigoError.DEMASIADAS_SOLICITUDES,
            CodigoError.CODIGO_INVALIDO,
            CodigoError.CODIGO_VENCIDO,
            CodigoError.CODIGO_INTENTOS_AGOTADOS,
            CodigoError.CUENTA_NO_ENCONTRADA,
            CodigoError.TOKEN_INVALIDO,
            CodigoError.NO_AUTENTICADO,
            CodigoError.GOOGLE_NO_DISPONIBLE
        ).forEach { codigo ->
            assertTrue(codigo, mensajeDeError(ErrorApi(codigo, "x")) != generico)
        }
    }

    @Test
    fun `google se anuncia como disponible pronto`() {
        assertEquals(
            "Inicio con Google disponible pronto.",
            mensajeDeError(ErrorApi(CodigoError.GOOGLE_NO_DISPONIBLE, "x"))
        )
    }

    @Test
    fun `una excepcion que no es del servidor es falta de conexion`() {
        assertTrue(mensajeDeError(IOException()).startsWith("No pudimos conectarnos."))
    }

    @Test
    fun `los datos invalidos se explican por campo`() {
        val error = ErrorApi(CodigoError.DATOS_INVALIDOS, "x", listOf(DetalleError("contrasena", "corta")))
        assertEquals("La contraseña debe tener entre 8 y 72 caracteres.", mensajeDeError(error))
    }

    @Test
    fun `la espera dice cuanto falta`() {
        val segundos = ErrorApi(CodigoError.DEMASIADAS_SOLICITUDES, "x", reintentarEnSegundos = 45)
        val minutos = ErrorApi(CodigoError.DEMASIADAS_SOLICITUDES, "x", reintentarEnSegundos = 600)
        assertTrue(mensajeDeError(segundos).contains("45 segundos"))
        assertTrue(mensajeDeError(minutos).contains("10 minutos"))
    }

    @Test
    fun `el bloqueo dice los minutos si se conocen`() {
        assertTrue(mensajeDeAcceso(ResultadoAcceso.CuentaBloqueada(1))!!.contains("en 1 minuto."))
        assertTrue(mensajeDeAcceso(ResultadoAcceso.CuentaBloqueada(15))!!.contains("en 15 minutos."))
        assertTrue(mensajeDeAcceso(ResultadoAcceso.CuentaBloqueada(null))!!.contains("en unos minutos."))
    }

    @Test
    fun `entrar o ir a confirmar el correo no muestran error`() {
        assertNull(mensajeDeAcceso(ResultadoAcceso.Entro))
        assertNull(mensajeDeAcceso(ResultadoAcceso.FaltaVerificarCorreo("a@b.co")))
    }
}
