package com.grupo3.nexo.auth;

import org.springframework.http.HttpStatus;

import com.grupo3.nexo.error.ErrorNegocio;

/** En las pruebas el token es "sub|correo|nombre"; cualquier otra cosa no es válida. Así no dependen de Google. */
class VerificadorGoogleFalso implements VerificadorTokenGoogle {

    @Override
    public IdentidadGoogle verificar(String tokenId) {
        String[] partes = tokenId.split("\\|");
        if (partes.length != 3) {
            throw new ErrorNegocio(HttpStatus.UNAUTHORIZED, "TOKEN_GOOGLE_INVALIDO", "No pudimos validar tu cuenta de Google");
        }
        return new IdentidadGoogle(partes[0], partes[1], partes[2]);
    }
}
