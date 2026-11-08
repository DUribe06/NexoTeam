package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.remote.ErroresApi
import com.grupo3.nexo.data.remote.NexoApi
import com.grupo3.nexo.data.remote.SolicitudReporte
import com.grupo3.nexo.data.remote.aDominio
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.FiltroTutores
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.Tutor
import com.grupo3.nexo.domain.repository.TutorRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Límite de `q` en el contrato: un texto más largo se rechazaría con DATOS_INVALIDOS. */
private const val MAXIMO_CONSULTA = 100

/** Tutores contra /api/tutores. */
@Singleton
class TutorRepositoryRemoto @Inject constructor(
    private val api: NexoApi,
    private val errores: ErroresApi,
    private val contexto: ContextoRemoto
) : TutorRepository {

    private val catalogo = EstadoRemoto<List<Tutor>>(contexto) { _, _ -> traerTutores(FiltroTutores()) }

    private val detalles = EstadosPorId<Tutor?>(contexto) { id, _, _ ->
        try {
            errores.llamar { api.tutor(id) }.aDominio()
        } catch (e: ErrorApi) {
            if (e.codigo == CodigoError.TUTOR_NO_ENCONTRADO) null else throw e
        }
    }

    private val resenas = EstadosPorId<List<Resena>>(contexto) { id, _, _ ->
        try {
            todasLasPaginas { pagina -> errores.llamar { api.resenas(id, pagina, TAMANO_PAGINA) } }
                .map { it.aDominio() }
                // Calificar sin comentario suma al promedio, pero no hay nada que mostrar
                .filter { it.texto.isNotBlank() }
        } catch (e: ErrorApi) {
            // Las reseñas propias se piden siempre, y quien nunca ha sido tutor no tiene perfil: no es un fallo
            if (e.codigo == CodigoError.TUTOR_NO_ENCONTRADO) emptyList() else throw e
        }
    }

    /** Hace repetir las búsquedas abiertas cuando cambian los datos de un tutor. */
    private val revisionBusquedas = MutableStateFlow(0)

    override fun obtenerTutores(): Flow<List<Tutor>> = catalogo.flujo()

    override fun obtenerTutor(id: String): Flow<Tutor?> = detalles.de(id).flujo()

    override fun obtenerResenas(tutorId: String): Flow<List<Resena>> =
        if (tutorId.isBlank()) flowOf(emptyList()) else resenas.de(tutorId).flujo()

    /** Sin filtros es el catálogo en memoria; con filtros los aplica el servidor, sin distinguir tildes (RF3). */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun buscarTutores(filtro: FiltroTutores): Flow<List<Tutor>> =
        if (!filtro.activo) catalogo.flujo()
        else combine(contexto.clave, revisionBusquedas) { clave, _ -> clave }.flatMapLatest { clave ->
            if (clave == null) emptyFlow() else flow { emit(traerTutores(filtro)) }
        }

    override suspend fun reportar(tutorId: String, motivo: String) {
        errores.llamar { api.reportarTutor(tutorId, SolicitudReporte(motivo)) }
    }

    /** No-op: la reseña la publica el servidor al calificar la sesión (POST /api/sesiones/{id}/calificacion). */
    override suspend fun agregarResena(resena: Resena) = Unit

    /** Calificar cambia el promedio y las reseñas del tutor. */
    internal suspend fun refrescarTutor(tutorId: String) = coroutineScope {
        revisionBusquedas.update { it + 1 }
        launch { catalogo.refrescar() }
        launch { detalles.yaPedido(tutorId)?.refrescar() }
        launch { resenas.yaPedido(tutorId)?.refrescar() }
    }

    private suspend fun traerTutores(filtro: FiltroTutores): List<Tutor> = todasLasPaginas { pagina ->
        errores.llamar {
            api.buscarTutores(
                consulta = filtro.consulta.trim().take(MAXIMO_CONSULTA).ifEmpty { null },
                materias = filtro.materias,
                modalidad = filtro.modalidad?.name,
                franja = filtro.franja?.name,
                enLinea = true.takeIf { filtro.soloEnLinea },
                pagina = pagina,
                tamano = TAMANO_PAGINA
            )
        }
    }.map { it.aDominio() }
}
