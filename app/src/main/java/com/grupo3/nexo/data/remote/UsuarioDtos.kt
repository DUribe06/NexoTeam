package com.grupo3.nexo.data.remote

import kotlinx.serialization.Serializable

/* Esquemas de /api/usuarios. */

@Serializable
data class PerfilPropioDto(
    val id: String,
    val nombre: String,
    val correo: String,
    val rol: RolDto,
    val estadoCuenta: EstadoCuentaDto,
    val handle: String,
    val modo: RolDto,
    val perfilTutorActivo: Boolean,
    val universidad: String? = null,
    val carrera: String? = null,
    val bio: String? = null,
    val materias: List<String> = emptyList(),
    val precioHora: Double? = null,
    val tieneContrasena: Boolean = true,
    val fotoUrl: String? = null
)

@Serializable
data class SolicitudCambiarModo(val modo: RolDto)

/** Solo las materias: el servidor no toca los demás datos del perfil que no se envían. */
@Serializable
data class SolicitudEditarMaterias(val materias: List<String>)

/** Los datos públicos del tutor; un texto vacío borra ese dato y un precio null no lo cambia. */
@Serializable
data class SolicitudEditarDatosTutor(
    val universidad: String,
    val carrera: String,
    val bio: String,
    val precioHora: Double? = null
)

@Serializable
data class SolicitudEliminarCuenta(val contrasena: String? = null, val tokenGoogle: String? = null)

@Serializable
data class ResumenActividadDto(
    val sesiones: Int,
    val horas: Double,
    val porCalificar: Int,
    val promedioRecibido: Double? = null,
    val totalResenas: Int,
    val personas: Int,
    val materias: List<String> = emptyList(),
    val penalizaciones: Int = 0,
    val suspendidaHasta: String? = null
)

/** La foto ya está en Cloudinary: el servidor solo guarda su dirección. */
@Serializable
data class SolicitudFoto(val url: String)
