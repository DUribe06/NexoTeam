package com.grupo3.nexo.usuarios;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/** `PerfilPropio` del contrato: los campos opcionales sin valor se omiten. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PerfilPropioDto(UUID id, String nombre, String correo, Rol rol, EstadoCuenta estadoCuenta,
                              String handle, Rol modo, boolean perfilTutorActivo, String universidad, String carrera,
                              String bio, List<String> materias, Double precioHora) {

    public static PerfilPropioDto de(Usuario u) {
        return new PerfilPropioDto(u.getId(), u.getNombre(), u.getCorreo(), u.getRol(), u.getEstadoCuenta(),
            u.getHandle(), u.getRol(), u.isPerfilTutorActivo(), u.getUniversidad(), u.getCarrera(), u.getBio(),
            u.getMaterias(), u.getPrecioHora() == null ? null : u.getPrecioHora().doubleValue());
    }
}
