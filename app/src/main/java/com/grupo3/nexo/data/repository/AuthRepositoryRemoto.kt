package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.local.AlmacenSesion
import com.grupo3.nexo.data.remote.ErroresApi
import com.grupo3.nexo.data.remote.NexoApi
import com.grupo3.nexo.data.remote.RolDto
import com.grupo3.nexo.data.remote.SesionDto
import com.grupo3.nexo.data.remote.SolicitudCorreo
import com.grupo3.nexo.data.remote.SolicitudLogin
import com.grupo3.nexo.data.remote.SolicitudRefrescar
import com.grupo3.nexo.data.remote.SolicitudRegistro
import com.grupo3.nexo.data.remote.SolicitudRestablecer
import com.grupo3.nexo.data.remote.SolicitudVerificarCorreo
import com.grupo3.nexo.data.remote.cabeceraBearer
import com.grupo3.nexo.data.remote.minutosDeBloqueo
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.ResultadoAcceso
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

/** Versión de las condiciones que el usuario acepta al registrarse (`versionTerminos` del contrato). */
private const val VERSION_TERMINOS = "2026-10-18"

/** Usuario que se expone mientras no hay sesión: `usuarioActual` nunca es null. */
private val SIN_USUARIO = Usuario(id = "", nombre = "", handle = "", correo = "", rol = Rol.ESTUDIANTE)

/** Cuentas contra /api/auth. Se usa cuando `BuildConfig.USAR_API` es true. */
@Singleton
class AuthRepositoryRemoto @Inject constructor(
    private val api: NexoApi,
    private val almacen: AlmacenSesion,
    private val errores: ErroresApi
) : AuthRepository {

    // Unconfined: autenticado y usuario se actualizan en el mismo paso en que cambia el almacén
    private val estado = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val red = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Modo elegido en el Perfil. Es local hasta que exista el módulo de usuario del backend. */
    private val modoLocal = MutableStateFlow<Rol?>(null)

    override val autenticado: StateFlow<Boolean> = almacen.sesion
        .map { it != null }
        .stateIn(estado, SharingStarted.Eagerly, almacen.sesion.value != null)

    override val usuarioActual: StateFlow<Usuario> = combine(almacen.sesion, modoLocal) { sesion, modo ->
        sesion?.aDominio(modo) ?: SIN_USUARIO
    }.stateIn(estado, SharingStarted.Eagerly, almacen.sesion.value?.aDominio(null) ?: SIN_USUARIO)

    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoAcceso =
        try {
            abrirSesion(errores.llamar { api.login(SolicitudLogin(correo, contrasena)) })
            ResultadoAcceso.Entro
        } catch (e: ErrorApi) {
            when (e.codigo) {
                CodigoError.CREDENCIALES_INVALIDAS -> ResultadoAcceso.CredencialesIncorrectas
                CodigoError.CORREO_NO_VERIFICADO -> ResultadoAcceso.FaltaVerificarCorreo(correo)
                CodigoError.CUENTA_BLOQUEADA -> ResultadoAcceso.CuentaBloqueada(minutosDeBloqueo(e.detalles))
                CodigoError.CUENTA_SUSPENDIDA -> ResultadoAcceso.CuentaSuspendida
                else -> throw e
            }
        }

    /** Aceptar las condiciones es requisito previo en la pantalla de registro. */
    override suspend fun registrarse(nombre: String, correo: String, contrasena: String): ResultadoAcceso {
        val respuesta = errores.llamar {
            api.registrar(
                SolicitudRegistro(
                    nombre = nombre,
                    correo = correo,
                    contrasena = contrasena,
                    aceptaTerminos = true,
                    versionTerminos = VERSION_TERMINOS
                )
            )
        }
        return ResultadoAcceso.FaltaVerificarCorreo(respuesta.correo)
    }

    override suspend fun verificarCorreo(correo: String, codigo: String) {
        abrirSesion(errores.llamar { api.verificarCorreo(SolicitudVerificarCorreo(correo, codigo)) })
    }

    override suspend fun reenviarCodigo(correo: String) =
        errores.sinCuerpo { api.reenviarCodigo(SolicitudCorreo(correo)) }

    override suspend fun recuperarContrasena(correo: String) =
        errores.sinCuerpo { api.recuperar(SolicitudCorreo(correo)) }

    override suspend fun restablecerContrasena(correo: String, codigo: String, nueva: String) =
        errores.sinCuerpo { api.restablecer(SolicitudRestablecer(correo, codigo, nueva)) }

    /** El backend aún no tiene inicio con Google; la UI lo muestra como "disponible pronto". */
    override suspend fun iniciarSesionGoogle(): Boolean =
        throw ErrorApi(CodigoError.GOOGLE_NO_DISPONIBLE, "Inicio con Google disponible pronto.")

    /** Solo local: se pierde al reiniciar la app. Pasará al servidor con el módulo de usuario. */
    override fun cambiarModo(rol: Rol) {
        modoLocal.value = rol
    }

    /** La sesión local se borra primero: salir no espera a la red y funciona también sin conexión. */
    override fun cerrarSesion() {
        val sesion = almacen.sesion.value
        modoLocal.value = null
        almacen.borrar()
        if (sesion != null) red.launch { invalidarEnServidor(sesion) }
    }

    /** Lo normal es salir con el token de acceso ya vencido: entonces se renueva para poder invalidar el de refresco. */
    private suspend fun invalidarEnServidor(sesion: SesionDto) {
        try {
            val cierre = api.cerrarSesion(cabeceraBearer(sesion.tokenAcceso), SolicitudRefrescar(sesion.tokenRefresco))
            if (cierre.code() != 401) return
            val renovada = api.refrescar(SolicitudRefrescar(sesion.tokenRefresco)).awaitResponse().body() ?: return
            api.cerrarSesion(cabeceraBearer(renovada.tokenAcceso), SolicitudRefrescar(renovada.tokenRefresco))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin red el token de refresco sigue vivo en el servidor hasta vencer, pero aquí ya nadie lo tiene
        }
    }

    private fun abrirSesion(sesion: SesionDto) {
        modoLocal.value = null
        almacen.guardar(sesion)
    }
}

private fun SesionDto.aDominio(modo: Rol?): Usuario = Usuario(
    id = usuario.id,
    nombre = usuario.nombre,
    handle = "@" + usuario.nombre.lowercase().replace(" ", "."),
    correo = usuario.correo,
    rol = modo ?: if (usuario.rol == RolDto.TUTOR) Rol.TUTOR else Rol.ESTUDIANTE
)
