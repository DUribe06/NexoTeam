package com.grupo3.nexo.usuarios;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpos de las peticiones de /api/usuarios/yo según el contrato. */
public final class SolicitudesUsuario {

    private SolicitudesUsuario() {
    }

    public record CambiarModo(@NotNull Modo modo) {
    }

    public record EliminarCuenta(@NotBlank String contrasena) {
    }

    /** Un campo sin enviar no cambia; un texto vacío borra ese dato. */
    public record EditarPerfilTutor(
        @Size(max = 500) String bio,
        @Size(min = 1, max = 20) List<@NotBlank @Size(max = 80) String> materias,
        @DecimalMin("0") @DecimalMax("10000000") @Digits(integer = 8, fraction = 2) BigDecimal precioHora,
        @Size(max = 120) String universidad,
        @Size(max = 120) String carrera) {

        boolean estaVacia() {
            return bio == null && materias == null && precioHora == null && universidad == null && carrera == null;
        }
    }
}
