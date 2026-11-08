package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.local.AlmacenSesion
import com.grupo3.nexo.data.remote.ErroresApi
import com.grupo3.nexo.data.remote.NexoApi
import com.grupo3.nexo.data.remote.PerfilPropioDto
import com.grupo3.nexo.data.remote.SesionDto
import com.grupo3.nexo.data.remote.SolicitudCambiarModo
import com.grupo3.nexo.data.remote.SolicitudEditarMaterias
import com.grupo3.nexo.data.remote.SolicitudCorreo
import com.grupo3.nexo.data.remote.SolicitudGoogle
import com.grupo3.nexo.data.remote.SolicitudLogin
import com.grupo3.nexo.data.remote.SolicitudEliminarCuenta
import com.grupo3.nexo.data.remote.SolicitudRefrescar
import com.grupo3.nexo.data.remote.SolicitudRegistro
import com.grupo3.nexo.data.remote.SolicitudRestablecer
import com.grupo3.nexo.data.remote.SolicitudVerificarCorreo
import com.grupo3.nexo.data.remote.aDominio
import com.grupo3.nexo.data.remote.aDto
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
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.awaitResponse

/** Versión de las condiciones que el usuario acepta al registrarse (`versionTerminos` del contrato). */
private const val VERSION_TERMINOS = "2026-11-03"

/** Usuario que se expone mientras no hay sesión: `usuarioActual` nunca es null. */
private val SIN_USUARIO = Usuario(id = "", nombre = "", handle = "", correo = "", rol = Rol.ESTUDIANTE)

/** Espera antes de volver a pedir el perfil si falló, y hasta dónde puede crecer. */
private const val ESPERA_PERFIL_MS = 5_000L
private const val ESPERA_PERFIL_MAXIMA_MS = 60_000L

/**
 * Cuentas contra /api/auth y /api/usuarios.
 * El modo en uso vive en la sesión guardada: así sobrevive a reiniciar la app y `ContextoRemoto` lo lee de ahí. El resto
 * del perfil (handle, universidad...) llega de GET /api/usuarios/yo poco después de abrir la sesión.
 */
@Singleton
class AuthRepositoryRemoto @Inject constructor(
    private val api: NexoApi,
    private val almacen: AlmacenSesion,
    private val errores: ErroresApi
) : AuthRepository {

    // Unconfined: autenticado y usuario se actualizan en el mismo paso en que cambia el almacén
    private val estado = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val red = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val perfil = MutableStateFlow<PerfilPropioDto?>(null)

    /** Una respuesta vieja de GET /yo no debe deshacer un cambio de modo que terminó mientras venía. */
    private val cerrojoPerfil = Mutex()
    private var cargaPerfil: Job? = null

    override val autenticado: StateFlow<Boolean> = almacen.sesion
        .map { it != null }
        .stateIn(estado, SharingStarted.Eagerly, almacen.sesion.value != null)

    override val usuarioActual: StateFlow<Usuario> = combine(almacen.sesion, perfil) { sesion, datos ->
        sesion?.aDominio(datos) ?: SIN_USUARIO
    }.stateIn(estado, SharingStarted.Eagerly, almacen.sesion.value?.aDominio(null) ?: SIN_USUARIO)

    init {
        // También al arrancar con una sesión guardada, que el almacén entrega cuando termina de leer el disco
        estado.launch {
            almacen.sesion.map { it?.usuario?.id }.distinctUntilChanged().collect { id ->
                cargaPerfil?.cancel()
                perfil.value = null
                if (id != null) cargaPerfil = red.launch { pedirPerfil() }
            }
        }
    }

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

    override suspend fun iniciarSesionGoogle(tokenId: String, aceptaCondiciones: Boolean): ResultadoAcceso =
        try {
            val solicitud = SolicitudGoogle(
                tokenId = tokenId,
                aceptaTerminos = aceptaCondiciones,
                versionTerminos = VERSION_TERMINOS.takeIf { aceptaCondiciones }
            )
            abrirSesion(errores.llamar { api.iniciarSesionGoogle(solicitud) })
            ResultadoAcceso.Entro
        } catch (e: ErrorApi) {
            when (e.codigo) {
                CodigoError.TERMINOS_REQUERIDOS -> ResultadoAcceso.FaltaAceptarCondiciones
                CodigoError.CUENTA_SUSPENDIDA -> ResultadoAcceso.CuentaSuspendida
                else -> throw e
            }
        }

    /** La primera vez que pasa a Tutor el servidor crea el perfil de tutor. Si falla, el modo sigue igual. */
    override suspend fun cambiarModo(rol: Rol) {
        if (rol == usuarioActual.value.rol) return
        cerrojoPerfil.withLock { aplicarPerfil(errores.llamar { api.cambiarModo(SolicitudCambiarModo(rol.aDto())) }) }
    }

    override suspend fun definirMaterias(materias: List<String>) {
        cerrojoPerfil.withLock { aplicarPerfil(errores.llamar { api.editarPerfilTutor(SolicitudEditarMaterias(materias)) }) }
    }

    /** La sesión local se borra primero: salir no espera a la red y funciona también sin conexión. */
    override fun cerrarSesion() {
        val sesion = almacen.sesion.value
        almacen.borrar()
        if (sesion != null) red.launch { invalidarEnServidor(sesion) }
    }

    override suspend fun eliminarCuenta(contrasena: String) {
        errores.sinCuerpo { api.eliminarCuenta(SolicitudEliminarCuenta(contrasena = contrasena)) }
        almacen.borrar()
    }

    override suspend fun eliminarCuentaConGoogle(tokenGoogle: String) {
        errores.sinCuerpo { api.eliminarCuenta(SolicitudEliminarCuenta(tokenGoogle = tokenGoogle)) }
        almacen.borrar()
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

    /** Una cuenta de administrador sí tiene credenciales válidas, pero no usa la app: no se le abre la sesión. */
    private fun abrirSesion(sesion: SesionDto) {
        if (sesion.usuario.rol.aDominio() == null) {
            red.launch { invalidarEnServidor(sesion) }
            throw ErrorApi(CodigoError.CUENTA_NO_USA_APP, "Esta cuenta no usa la app")
        }
        almacen.guardar(sesion)
    }

    /** Sin el perfil la app funciona, solo le falta el handle: se sigue intentando mientras dure la sesión. */
    private suspend fun pedirPerfil() {
        var espera = ESPERA_PERFIL_MS
        while (true) {
            try {
                cerrojoPerfil.withLock { aplicarPerfil(errores.llamar { api.perfilPropio() }) }
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                delay(espera)
                espera = min(espera * 2, ESPERA_PERFIL_MAXIMA_MS)
            }
        }
    }

    /** El modo del servidor manda: se guarda con la sesión para que siga ahí al reiniciar la app. */
    private fun aplicarPerfil(nuevo: PerfilPropioDto) {
        val sesion = almacen.sesion.value?.takeIf { it.usuario.id == nuevo.id } ?: return
        if (nuevo.modo.aDominio() == null) {
            cerrarSesion()
            return
        }
        perfil.value = nuevo
        if (sesion.usuario.rol != nuevo.modo) almacen.guardar(sesion.copy(usuario = sesion.usuario.copy(rol = nuevo.modo)))
    }
}

private fun SesionDto.aDominio(datos: PerfilPropioDto?): Usuario {
    val propio = datos?.takeIf { it.id == usuario.id }
    return Usuario(
        id = usuario.id,
        nombre = propio?.nombre ?: usuario.nombre,
        handle = propio?.handle.orEmpty(),
        correo = propio?.correo ?: usuario.correo,
        // Una cuenta que no usa modos nunca llega a tener sesión abierta
        rol = usuario.rol.aDominio() ?: Rol.ESTUDIANTE,
        universidad = propio?.universidad.orEmpty(),
        carrera = propio?.carrera.orEmpty(),
        bio = propio?.bio.orEmpty(),
        materias = propio?.materias.orEmpty(),
        tieneContrasena = propio?.tieneContrasena ?: true
    )
}
