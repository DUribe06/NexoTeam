package com.grupo3.nexo.auth;

import java.util.Locale;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/auth/registro (RF1). Coincide con `SolicitudRegistro` del contrato.
 * No hay campo de rol: toda cuenta nace en modo Estudiante, y lo que un cliente envíe de más se ignora.
 */
public record SolicitudRegistro(
    @NotBlank @Size(min = 2, max = 100) String nombre,
    @NotBlank @Email @Size(max = 254) String correo,
    @NotBlank @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String contrasena,
    @Size(max = 20) String documentoIdentidad,
    @AssertTrue(message = "debes aceptar los términos y la política de datos") boolean aceptaTerminos,
    @NotBlank @Size(max = 30) String versionTerminos) {

    /** El correo se normaliza (sin espacios, en minúsculas) antes de validarlo y de guardarlo. */
    public SolicitudRegistro {
        if (correo != null) {
            correo = correo.trim().toLowerCase(Locale.ROOT);
        }
    }
}
