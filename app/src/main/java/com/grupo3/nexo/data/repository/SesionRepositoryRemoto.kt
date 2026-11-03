package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.remote.ErroresApi
import com.grupo3.nexo.data.remote.NexoApi
import com.grupo3.nexo.data.remote.SesionTutoriaDto
import com.grupo3.nexo.data.remote.SolicitudAcuerdo
import com.grupo3.nexo.data.remote.SolicitudRevision
import com.grupo3.nexo.data.remote.SolicitudCalificar
import com.grupo3.nexo.data.remote.SolicitudIniciarSesion
import com.grupo3.nexo.data.remote.aDominio
import com.grupo3.nexo.data.remote.aDto
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.ResumenActividad
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.model.TipoAcuerdo
import com.grupo3.nexo.domain.repository.SesionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** El estudiante no recibe un aviso cuando el tutor envía el PIN: lo ve en la siguiente consulta. */
private const val INTERVALO_SONDEO_MS = 10_000L

/**
 * Sesiones contra /api/sesiones, solo las del modo actual. Se usa cuando `BuildConfig.USAR_API` es true.
 * Mientras alguien las observa se consultan cada [INTERVALO_SONDEO_MS]: el PIN, un retardo o una cancelación los hace la
 * otra persona, en otro teléfono.
 */
@Singleton
class SesionRepositoryRemoto @Inject constructor(
    private val api: NexoApi,
    private val errores: ErroresApi,
    private val tutores: TutorRepositoryRemoto,
    contexto: ContextoRemoto
) : SesionRepository {

    private val sondeos = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val sesiones = EstadoRemoto<List<Sesion>>(contexto) { clave, _ ->
        errores.llamar { api.sesiones(clave.modo.aDto()) }.mapNotNull { it.aDominio() }
    }.also { it.sondear(sondeos, INTERVALO_SONDEO_MS) }

    private val resumen = EstadoRemoto<ResumenActividad>(contexto) { clave, _ ->
        errores.llamar { api.resumenActividad(clave.modo.aDto()) }.aDominio()
    }

    override fun obtenerSesiones(): Flow<List<Sesion>> = sesiones.flujo()

    override fun resumenDelServidor(): Flow<ResumenActividad> = resumen.flujo()

    /** No-op: el servidor agenda la sesión al aceptar la solicitud (POST /api/solicitudes/{id}/aceptar). */
    override suspend fun programar(solicitud: Solicitud, comoTutor: Boolean) = Unit

    /** No-op: el servidor cancela la sesión al rechazar o cancelar la solicitud. */
    override suspend fun cancelarDe(solicitudId: String) = Unit

    /** El servidor publica además la reseña si califica el estudiante: el perfil del tutor también cambia. */
    override suspend fun calificar(sesionId: String, estrellas: Int, comentario: String?) {
        val calificada = try {
            errores.llamar {
                api.calificarSesion(sesionId, SolicitudCalificar(estrellas, comentario?.takeIf { it.isNotBlank() }))
            }
        } catch (e: ErrorApi) {
            // Ya estaba calificada o aún no ha terminado: lo que se ve está viejo
            refrescar()
            throw e
        }
        calificada.aDominio()?.let { nueva ->
            sesiones.actualizar { lista -> lista.map { if (it.id == nueva.id) nueva else it } }
        }
        coroutineScope {
            launch { refrescar() }
            if (!calificada.comoTutor) launch { tutores.refrescarTutor(calificada.contraparteId) }
        }
    }

    override suspend fun enviarPin(sesionId: String) = cambiar { api.enviarPin(sesionId) }

    override suspend fun iniciar(sesionId: String, pin: String) = cambiar { api.iniciarSesionTutoria(sesionId, SolicitudIniciarSesion(pin)) }

    override suspend fun proponerAcuerdo(sesionId: String, tipo: TipoAcuerdo, minutos: Int?) =
        cambiar { api.proponerAcuerdo(sesionId, SolicitudAcuerdo(tipo.name, minutos)) }

    override suspend fun aceptarAcuerdo(sesionId: String) = cambiar { api.aceptarAcuerdo(sesionId) }

    override suspend fun rechazarAcuerdo(sesionId: String) = cambiar { api.rechazarAcuerdo(sesionId) }

    override suspend fun pedirRevision(sesionId: String, motivo: String) =
        cambiar { api.pedirRevision(sesionId, SolicitudRevision(motivo)) }

    /** Se ve lo que respondió el servidor; si lo rechaza, lo que se veía estaba viejo y se vuelve a leer. */
    private suspend fun cambiar(llamada: suspend () -> SesionTutoriaDto) {
        val nueva = try {
            errores.llamar { llamada() }
        } catch (e: ErrorApi) {
            refrescar()
            throw e
        }
        nueva.aDominio()?.let { actual -> sesiones.actualizar { lista -> lista.map { if (it.id == actual.id) actual else it } } }
    }

    internal suspend fun refrescar() = coroutineScope {
        launch { sesiones.refrescar() }
        launch { resumen.refrescar() }
    }
}
