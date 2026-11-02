package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.mock.MockData
import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.HoraDisponible
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.calendarioVacio
import com.grupo3.nexo.domain.repository.DisponibilidadRepository
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class DisponibilidadRepositoryImpl @Inject constructor() : DisponibilidadRepository {

    private val dias = MutableStateFlow(MockData.disponibilidad)

    override fun obtener(): Flow<List<DiaDisponibilidad>> = dias.asStateFlow()

    override suspend fun crearCalendario() {
        dias.update { actual -> actual.ifEmpty { calendarioVacio() } }
    }

    override suspend fun definirHora(dia: String, hora: LocalTime, modalidades: Set<Modalidad>) {
        dias.update { actual ->
            actual.map { d ->
                if (d.dia == dia) d.copy(
                    horas = d.horas.map { h -> if (h.hora == hora) HoraDisponible(h.hora, modalidades) else h }
                ) else d
            }
        }
    }
}
