package com.grupo3.nexo.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato HTTP con el backend (`backend/.../openapi/nexo-api.yaml`): solo las rutas que la app usa.
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

    @POST("api/auth/google")
    suspend fun iniciarSesionGoogle(@Body solicitud: SolicitudGoogle): SesionDto

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

    /* ==================== Usuario ==================== */

    @GET("api/usuarios/yo")
    suspend fun perfilPropio(): PerfilPropioDto

    @PUT("api/usuarios/yo/modo")
    suspend fun cambiarModo(@Body solicitud: SolicitudCambiarModo): PerfilPropioDto

    @PATCH("api/usuarios/yo/perfil-tutor")
    suspend fun editarPerfilTutor(@Body solicitud: SolicitudEditarMaterias): PerfilPropioDto

    @PATCH("api/usuarios/yo/perfil-tutor")
    suspend fun editarDatosTutor(@Body solicitud: SolicitudEditarDatosTutor): PerfilPropioDto

    @PUT("api/usuarios/yo/foto")
    suspend fun cambiarFoto(@Body solicitud: SolicitudFoto): PerfilPropioDto

    @DELETE("api/usuarios/yo/foto")
    suspend fun quitarFoto(): PerfilPropioDto

    @POST("api/usuarios/yo/eliminar")
    suspend fun eliminarCuenta(@Body solicitud: SolicitudEliminarCuenta): Response<Unit>

    @GET("api/usuarios/yo/resumen")
    suspend fun resumenActividad(@Query("modo") modo: RolDto): ResumenActividadDto

    /* ==================== Tutores ==================== */

    @GET("api/tutores")
    suspend fun buscarTutores(
        @Query("q") consulta: String? = null,
        @Query("materia") materias: List<String> = emptyList(),
        @Query("modalidad") modalidad: String? = null,
        @Query("franja") franja: String? = null,
        @Query("enLinea") enLinea: Boolean? = null,
        @Query("pagina") pagina: Int,
        @Query("tamano") tamano: Int
    ): PaginaDto<TutorDto>

    @GET("api/tutores/{tutorId}")
    suspend fun tutor(@Path("tutorId") tutorId: String): TutorDto

    @GET("api/tutores/{tutorId}/resenas")
    suspend fun resenas(
        @Path("tutorId") tutorId: String,
        @Query("pagina") pagina: Int,
        @Query("tamano") tamano: Int
    ): PaginaDto<ResenaDto>

    @POST("api/tutores/{tutorId}/reportes")
    suspend fun reportarTutor(@Path("tutorId") tutorId: String, @Body solicitud: SolicitudReporte): ReporteDto

    /* ==================== Disponibilidad ==================== */

    @GET("api/disponibilidad/yo")
    suspend fun miDisponibilidad(): SemanaDisponibilidadDto

    @POST("api/disponibilidad/yo")
    suspend fun crearCalendario(): SemanaDisponibilidadDto

    @PUT("api/disponibilidad/yo/semana")
    suspend fun definirSemana(@Body solicitud: SolicitudDefinirSemana): SemanaDisponibilidadDto

    @GET("api/tutores/{tutorId}/disponibilidad")
    suspend fun disponibilidadDeTutor(@Path("tutorId") tutorId: String): List<DiaSolicitableDto>

    /* ==================== Solicitudes ==================== */

    @GET("api/solicitudes")
    suspend fun solicitudes(@Query("modo") modo: RolDto): List<SolicitudDto>

    @POST("api/solicitudes")
    suspend fun enviarSolicitud(@Body solicitud: SolicitudNueva): SolicitudDto

    @POST("api/solicitudes/{solicitudId}/aceptar")
    suspend fun aceptarSolicitud(@Path("solicitudId") solicitudId: String): ResultadoAceptarSolicitudDto

    @POST("api/solicitudes/{solicitudId}/rechazar")
    suspend fun rechazarSolicitud(
        @Path("solicitudId") solicitudId: String,
        @Body solicitud: SolicitudRechazar
    ): SolicitudDto

    @POST("api/solicitudes/{solicitudId}/cancelar")
    suspend fun cancelarSolicitud(@Path("solicitudId") solicitudId: String): SolicitudDto

    /* ==================== Sesiones ==================== */

    @GET("api/sesiones")
    suspend fun sesiones(@Query("modo") modo: RolDto): List<SesionTutoriaDto>

    @POST("api/sesiones/{sesionId}/pin")
    suspend fun enviarPin(@Path("sesionId") sesionId: String): SesionTutoriaDto

    @POST("api/sesiones/{sesionId}/iniciar")
    suspend fun iniciarSesionTutoria(
        @Path("sesionId") sesionId: String,
        @Body solicitud: SolicitudIniciarSesion
    ): SesionTutoriaDto

    @POST("api/sesiones/{sesionId}/acuerdo")
    suspend fun proponerAcuerdo(
        @Path("sesionId") sesionId: String,
        @Body solicitud: SolicitudAcuerdo
    ): SesionTutoriaDto

    @POST("api/sesiones/{sesionId}/acuerdo/aceptar")
    suspend fun aceptarAcuerdo(@Path("sesionId") sesionId: String): SesionTutoriaDto

    @POST("api/sesiones/{sesionId}/acuerdo/rechazar")
    suspend fun rechazarAcuerdo(@Path("sesionId") sesionId: String): SesionTutoriaDto

    @POST("api/sesiones/{sesionId}/revision")
    suspend fun pedirRevision(
        @Path("sesionId") sesionId: String,
        @Body solicitud: SolicitudRevision
    ): SesionTutoriaDto

    @POST("api/sesiones/{sesionId}/calificacion")
    suspend fun calificarSesion(
        @Path("sesionId") sesionId: String,
        @Body solicitud: SolicitudCalificar
    ): SesionTutoriaDto

    /* ==================== Chats ==================== */

    @GET("api/chats")
    suspend fun chats(@Query("modo") modo: RolDto): List<ChatDto>

    @GET("api/chats/con/{personaId}")
    suspend fun chatCon(@Path("personaId") personaId: String, @Query("modo") modo: RolDto): ChatDto

    /** `antes` y `desde` no se pueden usar a la vez. */
    @GET("api/chats/{chatId}/mensajes")
    suspend fun mensajes(
        @Path("chatId") chatId: String,
        @Query("antes") antes: String? = null,
        @Query("desde") desde: String? = null,
        @Query("limite") limite: Int
    ): PaginaMensajesDto

    @POST("api/chats/{chatId}/mensajes")
    suspend fun enviarMensaje(@Path("chatId") chatId: String, @Body solicitud: SolicitudMensaje): MensajeDto

    @POST("api/chats/{chatId}/leido")
    suspend fun marcarChatLeido(@Path("chatId") chatId: String): Response<Unit>
}

@Serializable
data class RespuestaSalud(val estado: String, val servicio: String)
