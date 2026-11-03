package com.grupo3.nexo.data.remote

import kotlinx.serialization.Serializable

/* Esquemas de /api/auth tal como están en backend/.../openapi/nexo-api.yaml. */

/** ADMIN no está en el contrato pero el servidor sí lo envía: esa cuenta no puede usar la app (RF12). */
@Serializable(with = RolDtoSerializador::class)
enum class RolDto { ESTUDIANTE, TUTOR, ADMIN, DESCONOCIDO }

object RolDtoSerializador : EnumTolerante<RolDto>("RolDto", RolDto.entries, RolDto.DESCONOCIDO)

@Serializable(with = EstadoCuentaDtoSerializador::class)
enum class EstadoCuentaDto { PENDIENTE_VERIFICACION_CORREO, ACTIVA, SUSPENDIDA, DESCONOCIDO }

object EstadoCuentaDtoSerializador :
    EnumTolerante<EstadoCuentaDto>("EstadoCuentaDto", EstadoCuentaDto.entries, EstadoCuentaDto.DESCONOCIDO)

@Serializable
data class SolicitudRegistro(
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val aceptaTerminos: Boolean,
    val versionTerminos: String
)

@Serializable
data class RespuestaRegistro(
    val usuarioId: String,
    val correo: String,
    val estadoCuenta: EstadoCuentaDto,
    val codigoVenceEnSegundos: Int? = null
)

@Serializable
data class SolicitudVerificarCorreo(val correo: String, val codigo: String)

@Serializable
data class SolicitudCorreo(val correo: String)

@Serializable
data class SolicitudLogin(val correo: String, val contrasena: String)

@Serializable
data class SolicitudRefrescar(val tokenRefresco: String)

@Serializable
data class SolicitudRestablecer(val correo: String, val codigo: String, val nuevaContrasena: String)

@Serializable
data class UsuarioDto(
    val id: String,
    val nombre: String,
    val correo: String,
    val rol: RolDto,
    val estadoCuenta: EstadoCuentaDto
)

@Serializable
data class SesionDto(
    val tokenAcceso: String,
    val tokenRefresco: String,
    val expiraEnSegundos: Int,
    val usuario: UsuarioDto
)

@Serializable
data class DetalleErrorDto(val campo: String, val problema: String)

/** Esquema `Error`: el cuerpo de toda respuesta fallida del backend. */
@Serializable
data class ErrorDto(
    val codigo: String,
    val mensaje: String,
    val detalles: List<DetalleErrorDto> = emptyList()
)
