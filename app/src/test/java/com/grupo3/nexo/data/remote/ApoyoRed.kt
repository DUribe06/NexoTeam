package com.grupo3.nexo.data.remote

import com.grupo3.nexo.data.local.AlmacenSesion
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val jsonDePrueba = Json { ignoreUnknownKeys = true }

/**
 * Sin reintentos: un corte simulado debe llegar como fallo, no gastar otra respuesta de la cola.
 * Solo la dirección en que escucha el servidor falso: tras un corte OkHttp descarta esa ruta, probaría la otra de
 * "localhost" (IPv6) y la conexión fallaría sin motivo.
 */
fun clienteDePrueba(): OkHttpClient = OkHttpClient.Builder()
    .retryOnConnectionFailure(false)
    .dns(object : Dns {
        override fun lookup(hostname: String) = listOf(InetAddress.getByName(hostname))
    })
    .build()

fun apiDePrueba(url: HttpUrl, cliente: OkHttpClient = clienteDePrueba()): NexoApi = Retrofit.Builder()
    .baseUrl(url)
    .client(cliente)
    .addConverterFactory(jsonDePrueba.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(NexoApi::class.java)

class AlmacenEnMemoria(inicial: SesionDto? = null) : AlmacenSesion {
    private val _sesion = MutableStateFlow(inicial)
    override val sesion: StateFlow<SesionDto?> = _sesion
    override fun guardar(nueva: SesionDto) { _sesion.value = nueva }
    override fun borrar() { _sesion.value = null }
}

fun sesionDePrueba(acceso: String = "acceso", refresco: String = "refresco", rol: RolDto = RolDto.ESTUDIANTE, id: String = "u-1") = SesionDto(
    acceso, refresco, 900,
    UsuarioDto(id, "Ana María", "ana@correo.co", rol, EstadoCuentaDto.ACTIVA)
)

fun cuerpoSesion(acceso: String = "acceso", refresco: String = "refresco", rol: String = "ESTUDIANTE") =
    """{"tokenAcceso":"$acceso","tokenRefresco":"$refresco","expiraEnSegundos":900,
        "usuario":{"id":"u-1","nombre":"Ana María","correo":"ana@correo.co","rol":"$rol","estadoCuenta":"ACTIVA"}}"""

fun cuerpoPerfil(modo: String = "ESTUDIANTE", id: String = "u-1") =
    """{"id":"$id","nombre":"Ana María","correo":"ana@correo.co","rol":"$modo","estadoCuenta":"ACTIVA",
        "handle":"@anamaria","modo":"$modo","perfilTutorActivo":false,"universidad":"UIS","materias":[]}"""

fun cuerpoError(codigo: String, detalles: String = "[]") =
    """{"codigo":"$codigo","mensaje":"Texto del servidor","detalles":$detalles}"""

/** Lecturas con flujos que no terminan: si algo se queda esperando, la prueba falla en vez de colgarse. */
fun enPrueba(bloque: suspend () -> Unit) = runBlocking { withTimeout(15_000) { bloque() } }

/**
 * Cola normal de MockWebServer, salvo GET /api/usuarios/yo: el repositorio de cuentas lo pide solo en segundo plano y
 * no debe gastar las respuestas que la prueba encoló para otra cosa.
 */
class ColaConPerfil(@Volatile var perfil: MockResponse = MockResponse().setBody(cuerpoPerfil())) : QueueDispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse =
        if (request.method == "GET" && request.path == RUTA_PERFIL) perfil else super.dispatch(request)

    companion object {
        const val RUTA_PERFIL = "/api/usuarios/yo"
    }
}

/** Servidor falso por rutas: cada una responde siempre lo último que se le indicó, y queda registro de lo pedido. */
class RutasFalsas : Dispatcher() {
    private val rutas = ConcurrentHashMap<String, (RecordedRequest) -> MockResponse>()
    val peticiones = CopyOnWriteArrayList<RecordedRequest>()

    fun responder(metodo: String, ruta: String, cuerpo: String, estado: Int = 200) {
        rutas["$metodo $ruta"] = { MockResponse().setResponseCode(estado).setBody(cuerpo) }
    }

    fun responderSegun(metodo: String, ruta: String, respuesta: (RecordedRequest) -> MockResponse) {
        rutas["$metodo $ruta"] = respuesta
    }

    fun fallar(metodo: String, ruta: String, estado: Int, codigo: String) = responder(metodo, ruta, cuerpoError(codigo), estado)

    /** DISCONNECT_AT_START no sirve aquí: el servidor solo lo mira al aceptar la conexión, no en una respuesta despachada. */
    fun cortar(metodo: String, ruta: String) {
        rutas["$metodo $ruta"] = { MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST) }
    }

    fun pedidas(metodo: String, ruta: String): List<RecordedRequest> =
        peticiones.filter { it.method == metodo && it.requestUrl!!.encodedPath == ruta }

    override fun dispatch(request: RecordedRequest): MockResponse {
        peticiones += request
        val respuesta = rutas["${request.method} ${request.requestUrl!!.encodedPath}"]
        return respuesta?.invoke(request) ?: MockResponse().setResponseCode(500).setBody(cuerpoError("RUTA_SIN_PREPARAR"))
    }
}

fun RecordedRequest.cuerpoTexto(): String = body.clone().readUtf8()
