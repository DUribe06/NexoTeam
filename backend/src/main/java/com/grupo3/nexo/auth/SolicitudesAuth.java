package com.grupo3.nexo.auth;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpos de las solicitudes de /api/auth. Los correos se normalizan igual que en el registro. */
public final class SolicitudesAuth {

    private SolicitudesAuth() {
    }

    static String normalizar(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }

    public record VerificarCorreo(
        @NotBlank @Email @Size(max = 254) String correo,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "debe tener exactamente 6 dígitos") String codigo) {
        public VerificarCorreo {
            correo = normalizar(correo);
        }
    }

    public record SoloCorreo(@NotBlank @Email @Size(max = 254) String correo) {
        public SoloCorreo {
            correo = normalizar(correo);
        }
    }

    public record Login(@NotBlank @Email @Size(max = 254) String correo, @NotBlank @Size(max = 72) String contrasena) {
        public Login {
            correo = normalizar(correo);
        }
    }

    public record Refrescar(@NotBlank @Size(max = 200) String tokenRefresco) {
    }
}
