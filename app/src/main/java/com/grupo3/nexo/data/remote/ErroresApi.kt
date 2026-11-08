package com.grupo3.nexo.data.remote

import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.DetalleError
import com.grupo3.nexo.domain.model.ErrorApi
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response

/** Campo de `detalles` donde CUENTA_BLOQUEADA dice hasta cuándo dura el bloqueo. */
private const val CAMPO_BLOQUEADO_HASTA = "bloqueadoHasta"

/**
 * Convierte las respuestas fallidas del servidor en [ErrorApi]. Los fallos de red no pasan por aquí:
 * Retrofit y OkHttp ya los lanzan como `IOException`.
 */
class ErroresApi @Inject constructor(private val json: Json) {

    /** Para las rutas que devuelven un cuerpo. */
    suspend fun <T> llamar(peticion: suspend () -> T): T =
        try {
            peticion()
        } catch (e: HttpException) {
            throw convertir(e.response() ?: throw e)
        }

    /** Para las rutas que responden 204. */
    suspend fun sinCuerpo(peticion: suspend () -> Response<Unit>) {
        val respuesta = peticion()
        if (!respuesta.isSuccessful) throw convertir(respuesta)
    }

    fun convertir(respuesta: Response<*>): ErrorApi {
        val espera = respuesta.headers()["Retry-After"]?.toIntOrNull()
        val cuerpo = try {
            respuesta.errorBody()?.string()?.let { json.decodeFromString<ErrorDto>(it) }
        } catch (e: SerializationException) {
            null
        }
        return if (cuerpo != null) {
            ErrorApi(cuerpo.codigo, cuerpo.mensaje, cuerpo.detalles.map { DetalleError(it.campo, it.problema) }, espera)
        } else {
            // Un proxy o el contenedor pueden responder sin el esquema del contrato
            ErrorApi(CodigoError.ERROR_SERVIDOR, "El servidor respondió ${respuesta.code()}", reintentarEnSegundos = espera)
        }
    }
}

/** Minutos que faltan para el desbloqueo, redondeados hacia arriba; null si `detalles` no lo indica. */
fun minutosDeBloqueo(detalles: List<DetalleError>, ahora: Instant = Instant.now()): Int? {
    val hasta = detalles.firstOrNull { it.campo == CAMPO_BLOQUEADO_HASTA }?.problema ?: return null
    val instante = try {
        Instant.parse(hasta)
    } catch (e: DateTimeParseException) {
        return null
    }
    return ceil(Duration.between(ahora, instante).seconds / 60.0).toInt().coerceAtLeast(1)
}
