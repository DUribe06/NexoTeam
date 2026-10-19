package com.grupo3.nexo.auth;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.grupo3.nexo.error.ErrorNegocio;

/** Reglas de contraseña (RF1): mínimo 8, con letra y número, y que no sea una de las más comunes. */
@Component
public class PoliticaContrasena {

    private static final int MINIMO = 8;
    /** BCrypt solo usa los primeros 72 bytes; más allá se rechaza para no truncar en silencio. */
    private static final int MAXIMO_BYTES = 72;
    private static final Set<String> COMUNES = Set.of(
        "12345678", "123456789", "1234567890", "password1", "password123", "contrasena1",
        "contrasena123", "qwerty123", "abc12345", "iloveyou1", "admin1234", "colombia123");

    public void validar(String contrasena, String correo) {
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            throw ErrorNegocio.datosInvalidos("contrasena", "es demasiado larga");
        }
        boolean tieneLetra = contrasena.chars().anyMatch(Character::isLetter);
        boolean tieneNumero = contrasena.chars().anyMatch(Character::isDigit);
        String usuarioDelCorreo = correo.substring(0, correo.indexOf('@'));
        boolean contieneElCorreo = usuarioDelCorreo.length() >= 4
            && contrasena.toLowerCase().contains(usuarioDelCorreo);
        if (contrasena.length() < MINIMO || !tieneLetra || !tieneNumero
            || COMUNES.contains(contrasena.toLowerCase()) || contieneElCorreo) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "CONTRASENA_DEBIL",
                "La contraseña es débil: usa al menos 8 caracteres con letras y números, y evita las comunes");
        }
    }
}
