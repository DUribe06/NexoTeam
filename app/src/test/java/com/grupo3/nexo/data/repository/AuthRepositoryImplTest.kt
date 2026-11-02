package com.grupo3.nexo.data.repository

import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.ResultadoAcceso
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Con los datos simulados el acceso debe seguir funcionando como antes de conectar la API. */
class AuthRepositoryImplTest {
    private val repositorio = AuthRepositoryImpl()

    @Test
    fun `entrar con datos validos abre la sesion`() = runTest {
        assertEquals(ResultadoAcceso.Entro, repositorio.iniciarSesion("ana@correo.co", "1234"))
        assertTrue(repositorio.autenticado.value)
    }

    @Test
    fun `datos invalidos no abren la sesion`() = runTest {
        assertEquals(ResultadoAcceso.CredencialesIncorrectas, repositorio.iniciarSesion("sinarroba", "1234"))
        assertFalse(repositorio.autenticado.value)
    }

    @Test
    fun `registrarse entra directo, sin pasar por el codigo`() = runTest {
        assertEquals(ResultadoAcceso.Entro, repositorio.registrarse("Ana", "ana@correo.co", "1234"))
        assertTrue(repositorio.autenticado.value)
        assertEquals("Ana", repositorio.usuarioActual.value.nombre)
    }

    @Test
    fun `los correos de revision muestran los demas casos`() = runTest {
        assertEquals(
            ResultadoAcceso.FaltaVerificarCorreo("sinverificar@correo.co"),
            repositorio.iniciarSesion("sinverificar@correo.co", "1234")
        )
        assertEquals(ResultadoAcceso.CuentaBloqueada(15), repositorio.iniciarSesion("bloqueada@correo.co", "1234"))
        assertFalse(repositorio.autenticado.value)
    }

    @Test
    fun `un codigo valido entra y 000000 se rechaza`() = runTest {
        try {
            repositorio.verificarCorreo("ana@correo.co", "000000")
            fail("Debía fallar")
        } catch (e: ErrorApi) {
            assertEquals(CodigoError.CODIGO_INVALIDO, e.codigo)
        }
        assertFalse(repositorio.autenticado.value)

        repositorio.verificarCorreo("ana@correo.co", "482913")
        assertTrue(repositorio.autenticado.value)
    }
}
