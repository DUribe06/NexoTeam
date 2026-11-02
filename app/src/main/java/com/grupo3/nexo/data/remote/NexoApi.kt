package com.grupo3.nexo.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Contrato HTTP con el backend. Crece con cada requerimiento (Fase 1: /auth).
 * Las rutas que responden 204 devuelven `Response<Unit>`: `ErroresApi.sinCuerpo` revisa el resultado.
 */
interface NexoApi {
    @GET("api/salud")
    suspend fun salud(): RespuestaSalud

    @POST("api/auth/registro")
    suspend fun registrar(@Body solicitud: SolicitudRegistro): RespuestaRegistro

    @POST("api/auth/verificar-correo")
    suspend fun verificarCorreo(@Body solicitud: SolicitudVerificarCorreo): SesionDto

    @POST("api/auth/reenviar-codigo")
    suspend fun reenviarCodigo(@Body solicitud: SolicitudCorreo): Response<Unit>

    @POST("api/auth/login")
    suspend fun login(@Body solicitud: SolicitudLogin): SesionDto

    /** Sin `suspend`: el `Authenticator` de OkHttp corre en un hilo de red y espera la respuesta ahí mismo. */
    @POST("api/auth/refrescar")
    fun refrescar(@Body solicitud: SolicitudRefrescar): Call<SesionDto>

    /** Recibe la cabecera a mano porque la sesión local ya se borró cuando esta llamada sale. */
    @POST("api/auth/cerrar-sesion")
    suspend fun cerrarSesion(
        @Header(CABECERA_AUTORIZACION) autorizacion: String,
        @Body solicitud: SolicitudRefrescar
    ): Response<Unit>

    @POST("api/auth/recuperar")
    suspend fun recuperar(@Body solicitud: SolicitudCorreo): Response<Unit>

    @POST("api/auth/restablecer")
    suspend fun restablecer(@Body solicitud: SolicitudRestablecer): Response<Unit>
}

@Serializable
data class RespuestaSalud(val estado: String, val servicio: String)
