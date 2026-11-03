package com.grupo3.nexo.auth;

/** Comprueba un token de Google. Si no es válido, o no se puede comprobar, lanza un `ErrorNegocio`. */
public interface VerificadorTokenGoogle {
    IdentidadGoogle verificar(String tokenId);
}
