package com.grupo3.nexo.data.remote

import com.grupo3.nexo.data.local.AlmacenSesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val jsonDePrueba = Json { ignoreUnknownKeys = true }

/** Sin reintentos: un corte simulado debe llegar como fallo, no gastar otra respuesta de la cola. */
fun clienteDePrueba(): OkHttpClient = OkHttpClient.Builder().retryOnConnectionFailure(false).build()

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

fun sesionDePrueba(acceso: String = "acceso", refresco: String = "refresco") = SesionDto(
    acceso, refresco, 900,
    UsuarioDto("u-1", "Ana María", "ana@correo.co", RolDto.ESTUDIANTE, EstadoCuentaDto.ACTIVA)
)

fun cuerpoSesion(acceso: String = "acceso", refresco: String = "refresco") =
    """{"tokenAcceso":"$acceso","tokenRefresco":"$refresco","expiraEnSegundos":900,
        "usuario":{"id":"u-1","nombre":"Ana María","correo":"ana@correo.co","rol":"ESTUDIANTE","estadoCuenta":"ACTIVA"}}"""

fun cuerpoError(codigo: String, detalles: String = "[]") =
    """{"codigo":"$codigo","mensaje":"Texto del servidor","detalles":$detalles}"""
