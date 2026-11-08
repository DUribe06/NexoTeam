package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.remote.ErroresApi
import com.grupo3.nexo.data.remote.NexoApi
import com.grupo3.nexo.data.remote.SolicitudDefinirHora
import com.grupo3.nexo.data.remote.SolicitudDefinirSemana
import com.grupo3.nexo.data.remote.aDominio
import com.grupo3.nexo.data.remote.aDto
import com.grupo3.nexo.data.remote.aTextoApi
import com.grupo3.nexo.domain.model.CodigoError
import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.DiaSemana
import com.grupo3.nexo.domain.model.DiaSolicitable
import com.grupo3.nexo.domain.model.ErrorApi
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.repository.DisponibilidadRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Disponibilidad contra /api/disponibilidad. */
@Singleton
class DisponibilidadRepositoryRemoto @Inject constructor(
    private val api: NexoApi,
    private val errores: ErroresApi,
    contexto: ContextoRemoto
) : DisponibilidadRepository {

    private val semana = EstadoRemoto<List<DiaDisponibilidad>>(contexto) { clave, _ ->
        // El Inicio la lee en los dos modos, y una cuenta que nunca ha sido tutor recibiría NO_ES_TUTOR
        if (clave.modo != Rol.TUTOR) emptyList()
        else try {
            errores.llamar { api.miDisponibilidad() }.aDominio()
        } catch (e: ErrorApi) {
            if (e.codigo == CodigoError.NO_ES_TUTOR) emptyList() else throw e
        }
    }

    override fun obtener(): Flow<List<DiaDisponibilidad>> = semana.flujo()

    override suspend fun crearCalendario() {
        val creada = errores.llamar { api.crearCalendario() }.aDominio()
        semana.actualizar { creada }
    }

    /** Se envían solo las horas publicadas; el servidor responde la semana como quedó. */
    override suspend fun definirSemana(dias: List<DiaDisponibilidad>) {
        val horas = dias.flatMap { dia ->
            val diaSemana = DiaSemana.entries.first { it.nombre == dia.dia }
            dia.horas.filter { it.seleccionada }.map { hora ->
                SolicitudDefinirHora(diaSemana.aDto(), hora.hora.aTextoApi(), hora.modalidades.map { it.aDto() })
            }
        }
        val actualizada = errores.llamar { api.definirSemana(SolicitudDefinirSemana(horas)) }.aDominio()
        semana.actualizar { actualizada }
    }

    /** Sin caché: al abrir la hoja Solicitar siempre debe verse lo que sigue libre. */
    override fun diasSolicitablesDelServidor(tutorId: String): Flow<List<DiaSolicitable>> = flow {
        emit(errores.llamar { api.disponibilidadDeTutor(tutorId) }.mapNotNull { it.aDominio() })
    }
}
