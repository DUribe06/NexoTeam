package com.grupo3.nexo.ui.auth

import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.DetalleError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.ResultadoAcceso
import com.grupo3.nexo.ui.components.avisoFallo
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            CodigoError.GOOGLE_NO_DISPONIBLE,
            CodigoError.TOKEN_GOOGLE_INVALIDO,
            CodigoError.GOOGLE_CUENTA_DISTINTA,
            CodigoError.GOOGLE_NO_COINCIDE,
            CodigoError.CUENTA_NO_USA_APP,
            CodigoError.ADMIN_SIN_MODOS,
            CodigoError.NO_ES_TUTOR,
            CodigoError.NO_ES_ESTUDIANTE,
            CodigoError.TUTOR_NO_ENCONTRADO,
            CodigoError.REPORTE_DUPLICADO,
            CodigoError.REPORTE_A_SI_MISMO,
            CodigoError.CALENDARIO_NO_CREADO,
            CodigoError.HORA_NO_VALIDA,
            CodigoError.SOLICITUD_DUPLICADA,
            CodigoError.FECHA_NO_VALIDA,
            CodigoError.HORA_NO_DISPONIBLE,
            CodigoError.MODALIDAD_NO_DISPONIBLE,
            CodigoError.MATERIA_NO_OFRECIDA,
            CodigoError.SOLICITUD_A_SI_MISMO,
            CodigoError.SOLICITUD_NO_ENCONTRADA,
            CodigoError.SOLICITUD_NO_PENDIENTE,
            CodigoError.SOLICITUD_EXPIRADA,
            CodigoError.SOLICITUD_NO_CANCELABLE,
            CodigoError.SESION_NO_ENCONTRADA,
            CodigoError.SESION_NO_COMPLETADA,
            CodigoError.SESION_YA_CALIFICADA,
            CodigoError.CHAT_NO_HABILITADO,
            CodigoError.CHAT_NO_ENCONTRADO,
            CodigoError.CHAT_SOLO_LECTURA
        ).forEach { codigo ->
            assertTrue(codigo, mensajeDeError(ErrorApi(codigo, "x")) != generico)
        }
    }

    @Test
    fun `el aviso de una accion dice por que la rechazo el servidor, y solo sin conexion culpa a la red`() {
        val rechazo = avisoFallo("aceptar la solicitud", ErrorApi(CodigoError.SOLICITUD_NO_PENDIENTE, "x"))
        assertEquals("Esta solicitud ya no está pendiente: pudo cancelarse o vencer.", rechazo)
        assertFalse(rechazo.contains("conexión"))
        assertFalse(avisoFallo("aceptar la solicitud", ErrorApi(CodigoError.ERROR_SERVIDOR, "x")).contains("conexión"))
        assertEquals(
            "No se pudo aceptar la solicitud. Revisa tu conexión e inténtalo de nuevo.",
            avisoFallo("aceptar la solicitud", IOException("sin red"))
        )
    }

    @Test
    fun `una cuenta de administrador no se confunde con un fallo de conexion`() {
        val mensaje = mensajeDeError(ErrorApi(CodigoError.CUENTA_NO_USA_APP, "x"))
        assertTrue(mensaje.contains("administración"))
        assertFalse(mensaje.contains("conexión"))
    }

    @Test
    fun `google no disponible invita a entrar con el correo`() {
        assertEquals(
            "El inicio con Google no está disponible en este momento. Entra con tu correo.",
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
