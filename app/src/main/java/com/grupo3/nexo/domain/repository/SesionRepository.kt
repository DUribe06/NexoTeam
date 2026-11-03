package com.grupo3.nexo.domain.repository

import com.grupo3.nexo.domain.model.ResumenActividad
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.Solicitud
import com.grupo3.nexo.domain.model.TipoAcuerdo
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de datos de las sesiones y su calificación.
 *
 * Las lecturas (`Flow`) y las escrituras (`suspend`) de una implementación remota deben lanzar una excepción
 * ante fallos de red o del servidor: la UI la captura, muestra el error con "Reintentar" o un aviso breve.
 */
interface SesionRepository {
    /** Sesiones del usuario, de la más reciente a la más antigua. Con el servidor llegan solo las del modo actual. */
    fun obtenerSesiones(): Flow<List<Sesion>>
    /** Cifras del modo actual si quien guarda los datos las entrega; con null, el caso de uso las calcula. */
    fun resumenDelServidor(): Flow<ResumenActividad>? = null
    /** Agenda la sesión de una solicitud recién aceptada (RF6 → RF10). Con el servidor ya ocurrió al aceptar. */
    suspend fun programar(solicitud: Solicitud, comoTutor: Boolean)
    /** Cancela la sesión que nació de una solicitud, si existe. Con el servidor ya ocurrió al rechazar o cancelar. */
    suspend fun cancelarDe(solicitudId: String)
    suspend fun calificar(sesionId: String, estrellas: Int, comentario: String?)
    /** El tutor llegó: desde ahora el estudiante ve el PIN. */
    suspend fun enviarPin(sesionId: String)
    /** El tutor escribe el PIN que le dijo el estudiante y la sesión empieza. */
    suspend fun iniciar(sesionId: String, pin: String)
    /** Propone un retardo de [minutos] sobre la hora original, o cancelar si es otro tipo. */
    suspend fun proponerAcuerdo(sesionId: String, tipo: TipoAcuerdo, minutos: Int?)
    suspend fun aceptarAcuerdo(sesionId: String)
    /** La otra persona la rechaza; quien la propuso la retira. */
    suspend fun rechazarAcuerdo(sesionId: String)
    /** Quien recibió la penalización cree que fue un error. */
    suspend fun pedirRevision(sesionId: String, motivo: String)
}
