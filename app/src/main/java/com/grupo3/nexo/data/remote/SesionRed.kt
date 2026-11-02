package com.grupo3.nexo.data.remote

import com.grupo3.nexo.data.local.AlmacenSesion
import java.io.IOException
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

const val CABECERA_AUTORIZACION = "Authorization"

fun cabeceraBearer(token: String) = "Bearer $token"

/**
 * Rutas sin autenticación según el contrato. No llevan token porque Spring rechaza con 401 un token
 * vencido aunque la ruta sea pública, y entonces no se podría volver a entrar.
 */
private val RUTAS_PUBLICAS = setOf(
    "/api/salud",
    "/api/auth/registro",
    "/api/auth/verificar-correo",
    "/api/auth/reenviar-codigo",
    "/api/auth/login",
    "/api/auth/refrescar",
    "/api/auth/recuperar",
    "/api/auth/restablecer"
)

/** Añade `Authorization: Bearer` a toda petición privada mientras haya sesión. */
class InterceptorSesion(private val almacen: AlmacenSesion) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion = chain.request()
        val token = almacen.sesion.value?.tokenAcceso
        val agregar = token != null &&
            peticion.header(CABECERA_AUTORIZACION) == null &&
            peticion.url.encodedPath !in RUTAS_PUBLICAS
        return chain.proceed(
            if (agregar) peticion.conToken(token!!) else peticion
        )
    }
}

/**
 * Ante un 401 pide tokens nuevos y repite la petición una vez. Si el servidor rechaza el token de refresco, borra
 * la sesión y la app vuelve al acceso. Un fallo de red o un 5xx no la borran: quedarse sin internet no debe sacar a
 * nadie de su cuenta; la petición falla y se reintenta luego.
 *
 * @param apiSinSesion cliente sin este autenticador, para que el refresco no se llame a sí mismo.
 */
class AutenticadorSesion(
    private val almacen: AlmacenSesion,
    private val apiSinSesion: NexoApi
) : Authenticator {

    // Sincronizado: si varias peticiones reciben 401 a la vez, solo la primera gasta el token de refresco
    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        // Sin token no es sesión vencida sino, por ejemplo, una clave errada en el login
        val enviada = response.request.header(CABECERA_AUTORIZACION) ?: return null
        val vigente = almacen.sesion.value ?: return null

        // Otra petición ya renovó los tokens mientras esta esperaba
        if (enviada != cabeceraBearer(vigente.tokenAcceso)) return response.request.conToken(vigente.tokenAcceso)

        // El token recién renovado también fue rechazado
        if (response.priorResponse != null) {
            almacen.borrar()
            return null
        }

        val refresco = try {
            apiSinSesion.refrescar(SolicitudRefrescar(vigente.tokenRefresco)).execute()
        } catch (e: IOException) {
            return null
        }
        val nueva = refresco.body()
        if (refresco.isSuccessful && nueva != null) {
            almacen.guardar(nueva)
            return response.request.conToken(nueva.tokenAcceso)
        }
        if (refresco.code() in 400..499) almacen.borrar()
        return null
    }
}

private fun Request.conToken(token: String): Request =
    newBuilder().header(CABECERA_AUTORIZACION, cabeceraBearer(token)).build()
