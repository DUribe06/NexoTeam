package com.grupo3.nexo.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET

/** Contrato HTTP con el backend. Crece con cada requerimiento (Fase 1: /auth). */
interface NexoApi {
    @GET("api/salud")
    suspend fun salud(): RespuestaSalud
}

@Serializable
data class RespuestaSalud(val estado: String, val servicio: String)
