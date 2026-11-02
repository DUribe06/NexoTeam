package com.grupo3.nexo.ui.auth

import androidx.lifecycle.SavedStateHandle
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.ResultadoAcceso
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.repository.AuthRepository
import com.grupo3.nexo.domain.usecase.IniciarSesionGoogleUseCase
import com.grupo3.nexo.domain.usecase.IniciarSesionUseCase
import com.grupo3.nexo.domain.usecase.RecuperarContrasenaUseCase
import com.grupo3.nexo.domain.usecase.ReenviarCodigoUseCase
import com.grupo3.nexo.domain.usecase.RegistrarseUseCase
import com.grupo3.nexo.domain.usecase.RestablecerContrasenaUseCase
import com.grupo3.nexo.domain.usecase.ValidarAccesoUseCase
import com.grupo3.nexo.domain.usecase.ValidarRestablecerUseCase
import com.grupo3.nexo.domain.usecase.VerificarCorreoUseCase
import com.grupo3.nexo.ui.navigation.Rutas
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Repositorio configurable: cada acción hace lo que la prueba le indique y deja registro de lo que recibió. */
private class CuentasDePrueba : AuthRepository {
    override val autenticado: StateFlow<Boolean> = MutableStateFlow(false)
    override val usuarioActual: StateFlow<Usuario> = MutableStateFlow(Usuario("", "", "", "", Rol.ESTUDIANTE))
    var alEntrar: () -> ResultadoAcceso = { ResultadoAcceso.Entro }
    var alRegistrar: () -> ResultadoAcceso = { ResultadoAcceso.FaltaVerificarCorreo("ana@correo.co") }
    var alVerificar: () -> Unit = {}
    var alReenviar: () -> Unit = {}
    var alRecuperar: () -> Unit = {}
    var alRestablecer: () -> Unit = {}
    var alGoogle: () -> Boolean = { true }
    val codigosRecibidos = mutableListOf<String>()
    var reenvios = 0

    override suspend fun iniciarSesion(correo: String, contrasena: String) = alEntrar()
    override suspend fun registrarse(nombre: String, correo: String, contrasena: String) = alRegistrar()
    override suspend fun verificarCorreo(correo: String, codigo: String) {
        codigosRecibidos += codigo
        alVerificar()
    }
    override suspend fun reenviarCodigo(correo: String) {
        reenvios++
        alReenviar()
    }
    override suspend fun recuperarContrasena(correo: String) {
        alRecuperar()
    }
    override suspend fun restablecerContrasena(correo: String, codigo: String, nueva: String) = alRestablecer()
    override suspend fun iniciarSesionGoogle() = alGoogle()
    override fun cambiarModo(rol: Rol) = Unit
    override fun cerrarSesion() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class FlujosCuentaTest {
    private val cuentas = CuentasDePrueba()
    private val despachador = StandardTestDispatcher()

    @Before
    fun preparar() = Dispatchers.setMain(despachador)

    @After
    fun cerrar() = Dispatchers.resetMain()

    private fun login() = LoginViewModel(
        IniciarSesionUseCase(cuentas),
        RegistrarseUseCase(cuentas),
        IniciarSesionGoogleUseCase(cuentas),
        ValidarAccesoUseCase()
    ).apply {
        cambiarCorreo("ana@correo.co")
        cambiarContrasena("clave1234")
    }

    private fun confirmar(reenviar: Boolean = false) = ConfirmarCorreoViewModel(
        SavedStateHandle(
            mapOf(Rutas.ConfirmarCorreo.ARG_CORREO to "ana@correo.co", Rutas.ConfirmarCorreo.ARG_REENVIAR to reenviar)
        ),
        VerificarCorreoUseCase(cuentas),
        ReenviarCodigoUseCase(cuentas)
    )

    private fun recuperar() = RecuperarContrasenaViewModel(
        SavedStateHandle(mapOf(Rutas.RecuperarContrasena.ARG_CORREO to "ana@correo.co")),
        RecuperarContrasenaUseCase(cuentas),
        RestablecerContrasenaUseCase(cuentas),
        ValidarRestablecerUseCase()
    )

    @Test
    fun `entrar con correo sin confirmar lleva al codigo y pide reenviarlo`() = runTest(despachador) {
        cuentas.alEntrar = { ResultadoAcceso.FaltaVerificarCorreo("ana@correo.co") }
        val vm = login()

        vm.entrar()
        runCurrent()

        assertEquals("ana@correo.co", vm.estado.value.correoPorVerificar)
        assertTrue(vm.estado.value.reenviarAlVerificar)
        assertFalse(vm.estado.value.autenticado)
        assertNull(vm.estado.value.error)
    }

    @Test
    fun `registrarse lleva al codigo sin pedir otro`() = runTest(despachador) {
        val vm = login()
        vm.alternarModo()
        vm.cambiarNombre("Ana")
        vm.cambiarCondiciones(true)

        vm.entrar()
        runCurrent()

        assertEquals("ana@correo.co", vm.estado.value.correoPorVerificar)
        assertFalse(vm.estado.value.reenviarAlVerificar)
    }

    @Test
    fun `un correo repetido al registrarse se explica`() = runTest(despachador) {
        cuentas.alRegistrar = { throw ErrorApi(CodigoError.CORREO_DUPLICADO, "x") }
        val vm = login()
        vm.alternarModo()
        vm.cambiarNombre("Ana")
        vm.cambiarCondiciones(true)

        vm.entrar()
        runCurrent()

        assertEquals("Ya hay una cuenta con ese correo. Inicia sesión o usa otro.", vm.estado.value.error)
    }

    @Test
    fun `google no disponible se avisa y el boton sigue habilitado`() = runTest(despachador) {
        cuentas.alGoogle = { throw ErrorApi(CodigoError.GOOGLE_NO_DISPONIBLE, "x") }
        val vm = login()

        vm.entrarGoogle()
        runCurrent()

        assertEquals("Inicio con Google disponible pronto.", vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)
        assertFalse(vm.estado.value.autenticado)
    }

    @Test
    fun `pegar el codigo con espacios lo limpia y lo envia solo`() = runTest(despachador) {
        val vm = confirmar()

        vm.cambiarCodigo("482 913")
        runCurrent()

        assertEquals(listOf("482913"), cuentas.codigosRecibidos)
        assertTrue(vm.estado.value.entro)
    }

    @Test
    fun `no acepta mas de seis digitos ni letras`() = runTest(despachador) {
        val vm = confirmar()

        vm.cambiarCodigo("12a")
        assertEquals("12", vm.estado.value.codigo)
        vm.cambiarCodigo("1234")
        assertEquals("1234", vm.estado.value.codigo)
        runCurrent()
        assertTrue(cuentas.codigosRecibidos.isEmpty())
    }

    @Test
    fun `un codigo rechazado marca las casillas y un fallo de red no`() = runTest(despachador) {
        val vm = confirmar()
        cuentas.alVerificar = { throw ErrorApi(CodigoError.CODIGO_VENCIDO, "x") }
        vm.cambiarCodigo("482913")
        runCurrent()
        assertEquals("El código venció. Pide uno nuevo.", vm.estado.value.error)
        assertTrue(vm.estado.value.codigoRechazado)

        cuentas.alVerificar = { throw IOException() }
        vm.confirmar()
        runCurrent()
        assertTrue(vm.estado.value.error!!.startsWith("No pudimos conectarnos."))
        assertFalse(vm.estado.value.codigoRechazado)
    }

    @Test
    fun `tras registrarse hay que esperar para reenviar`() = runTest(despachador) {
        val vm = confirmar()
        runCurrent()
        assertEquals(SEGUNDOS_ENTRE_ENVIOS, vm.estado.value.espera)

        vm.reenviar()
        runCurrent()
        assertEquals(0, cuentas.reenvios)

        advanceTimeBy(SEGUNDOS_ENTRE_ENVIOS * 1_000L + 1)
        assertEquals(0, vm.estado.value.espera)
        vm.reenviar()
        runCurrent()
        assertEquals(1, cuentas.reenvios)
        assertEquals("Te enviamos un código nuevo.", vm.estado.value.aviso)
        assertEquals(SEGUNDOS_ENTRE_ENVIOS, vm.estado.value.espera)
    }

    @Test
    fun `al llegar desde el login pide un codigo`() = runTest(despachador) {
        val vm = confirmar(reenviar = true)
        runCurrent()

        assertEquals(1, cuentas.reenvios)
        assertEquals(SEGUNDOS_ENTRE_ENVIOS, vm.estado.value.espera)
    }

    @Test
    fun `al llegar desde el login un codigo reciente no es un error y respeta la espera del servidor`() = runTest(despachador) {
        cuentas.alReenviar = { throw ErrorApi(CodigoError.DEMASIADAS_SOLICITUDES, "x", reintentarEnSegundos = 30) }
        val vm = confirmar(reenviar = true)
        runCurrent()

        assertNull(vm.estado.value.error)
        assertEquals(30, vm.estado.value.espera)
    }

    @Test
    fun `reenviar a mano si muestra la espera del servidor`() = runTest(despachador) {
        val vm = confirmar()
        advanceTimeBy(SEGUNDOS_ENTRE_ENVIOS * 1_000L + 1)
        cuentas.alReenviar = { throw ErrorApi(CodigoError.DEMASIADAS_SOLICITUDES, "x", reintentarEnSegundos = 30) }

        vm.reenviar()
        runCurrent()

        assertTrue(vm.estado.value.error!!.contains("30 segundos"))
        assertEquals(30, vm.estado.value.espera)
    }

    @Test
    fun `recuperar avanza al segundo paso solo si el pedido sale bien`() = runTest(despachador) {
        val vm = recuperar()
        cuentas.alRecuperar = { throw IOException() }
        vm.pedirCodigo()
        runCurrent()
        assertEquals(RecuperarContrasenaViewModel.Paso.PEDIR_CODIGO, vm.estado.value.paso)
        assertTrue(vm.estado.value.error!!.startsWith("No pudimos conectarnos."))

        cuentas.alRecuperar = {}
        vm.pedirCodigo()
        runCurrent()
        assertEquals(RecuperarContrasenaViewModel.Paso.CAMBIAR_CONTRASENA, vm.estado.value.paso)
        assertNull(vm.estado.value.error)
    }

    @Test
    fun `restablecer valida antes de llamar y termina al salir bien`() = runTest(despachador) {
        val vm = recuperar()
        vm.pedirCodigo()
        runCurrent()

        vm.cambiarCodigo("48")
        vm.cambiarNueva("nueva1234")
        vm.cambiarContrasena()
        runCurrent()
        assertEquals("Escribe los 6 dígitos del código", vm.estado.value.error)

        vm.cambiarCodigo("482913")
        vm.cambiarNueva("corta")
        vm.cambiarContrasena()
        runCurrent()
        assertEquals("La contraseña debe tener al menos 8 caracteres", vm.estado.value.error)

        vm.cambiarNueva("nueva1234")
        vm.cambiarContrasena()
        runCurrent()
        assertTrue(vm.estado.value.listo)
    }

    @Test
    fun `volver al acceso con la contrasena cambiada deja el correo y el aviso`() = runTest(despachador) {
        val vm = login()

        vm.contrasenaRestablecida("otra@correo.co")

        assertEquals("otra@correo.co", vm.estado.value.correo)
        assertEquals("", vm.estado.value.contrasena)
        assertEquals("Listo. Ya puedes iniciar sesión con tu contraseña nueva.", vm.estado.value.aviso)
    }
}
