package com.grupo3.nexo.auth;

/** `Sesion` del contrato. */
public record SesionDto(String tokenAcceso, String tokenRefresco, int expiraEnSegundos, UsuarioDto usuario) {
}
