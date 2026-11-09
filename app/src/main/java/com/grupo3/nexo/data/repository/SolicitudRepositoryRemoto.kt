package com.grupo3.nexo.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import com.grupo3.nexo.data.remote.ErroresApi
import com.grupo3.nexo.data.remote.NexoApi
import com.grupo3.nexo.data.remote.SolicitudDto
import com.grupo3.nexo.data.remote.SolicitudNueva
import com.grupo3.nexo.data.remote.SolicitudRechazar
import com.grupo3.nexo.data.remote.aDominio
import com.grupo3.nexo.data.remote.aDto
import com.grupo3.nexo.data.remote.aTextoApi
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.EstadoSolicitud
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.repository.SolicitudRepository
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Aceptar, rechazar o cancelar lo hace la otra persona en otro teléfono: así se ve sin salir de la pantalla. */
private const val INTERVALO_SONDEO_MS = 15_000L

/**
 * Solicitudes contra /api/solicitudes, solo las del modo actual.
 * Aceptar, rechazar y cancelar también crean o cancelan la sesión y habilitan o cierran el chat en el servidor, en una
 * sola transacción: por eso cada una refresca solicitudes, sesiones y chats.
 */
@Singleton
class SolicitudRepositoryRemoto @Inject constructor(
    private val api: NexoApi,
    private val errores: ErroresApi,
    private val sesiones: SesionRepositoryRemoto,
    private val chats: ChatRepositoryRemoto,
    contexto: ContextoRemoto
) : SolicitudRepository {

    private val sondeos = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Mientras alguien las observa se consultan cada [INTERVALO_SONDEO_MS]
    private val solicitudes = EstadoRemoto<List<Solicitud>>(contexto) { clave, _ ->
        errores.llamar { api.solicitudes(clave.modo.aDto()) }.mapNotNull { it.aDominio() }
    }.also { it.sondear(sondeos, INTERVALO_SONDEO_MS) }

    override fun obtenerSolicitudes(): Flow<List<Solicitud>> = solicitudes.flujo()

    override suspend fun actualizarEstado(id: String, estado: EstadoSolicitud, motivo: String?) {
        val respuesta: SolicitudDto = try {
            when (estado) {
                EstadoSolicitud.ACEPTADA -> errores.llamar { api.aceptarSolicitud(id) }.solicitud
                EstadoSolicitud.RECHAZADA -> errores.llamar {
                    api.rechazarSolicitud(id, SolicitudRechazar(motivo?.takeIf { it.isNotBlank() }))
                }
                EstadoSolicitud.CANCELADA -> errores.llamar { api.cancelarSolicitud(id) }
                else -> throw IllegalArgumentException("La app no puede pasar una solicitud a $estado")
            }
        } catch (e: ErrorApi) {
            // Lo que se ve está viejo (otra parte la movió o venció): la lista debe mostrar lo que hay ahora
            refrescarTodo()
            throw e
        }
        respuesta.aDominio()?.let { poner(it) }
        refrescarTodo()
    }

    /** El nombre del tutor, el momento de envío y el plazo los pone el servidor. */
    override suspend fun enviar(
        tutorId: String,
        materia: String,
        tema: String,
        fecha: LocalDate,
        hora: LocalTime,
        modalidad: Modalidad,
        mensaje: String,
        precioPropuesto: Double?
    ) {
        val enviada = errores.llamar {
            api.enviarSolicitud(
                SolicitudNueva(
                    tutorId = tutorId,
                    materia = materia,
                    tema = tema,
                    fecha = fecha.toString(),
                    hora = hora.aTextoApi(),
                    modalidad = modalidad.aDto(),
                    mensaje = mensaje.takeIf { it.isNotBlank() },
                    precioPropuesto = precioPropuesto
                )
            )
        }
        enviada.aDominio()?.let { poner(it) }
        solicitudes.refrescar()
    }

    private suspend fun poner(solicitud: Solicitud) = solicitudes.actualizar { lista ->
        if (lista.any { it.id == solicitud.id }) lista.map { if (it.id == solicitud.id) solicitud else it }
        else lista + solicitud
    }

    private suspend fun refrescarTodo() = coroutineScope {
        launch { solicitudes.refrescar() }
        launch { sesiones.refrescar() }
        launch { chats.refrescarChats() }
    }
}
