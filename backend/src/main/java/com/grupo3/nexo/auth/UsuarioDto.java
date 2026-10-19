package com.grupo3.nexo.auth;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.grupo3.nexo.usuarios.EstadoCuenta;
import com.grupo3.nexo.usuarios.EstadoVerificacionTutor;
import com.grupo3.nexo.usuarios.Rol;
import com.grupo3.nexo.usuarios.Usuario;

/** `Usuario` del contrato. Nunca incluye la contraseña ni su hash. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsuarioDto(UUID id, String nombre, String correo, Rol rol, EstadoCuenta estadoCuenta,
                         EstadoVerificacionTutor estadoVerificacionTutor) {

    public static UsuarioDto de(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getCorreo(), u.getRol(), u.getEstadoCuenta(),
            u.getEstadoVerificacionTutor());
    }
}
