package com.grupo3.nexo.auth;

/** Lo que Google afirma de la persona en un token ya verificado; el correo siempre viene confirmado por Google. */
public record IdentidadGoogle(String sub, String correo, String nombre, String foto) {
}
