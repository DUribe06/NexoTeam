package com.grupo3.nexo.usuarios;

/**
 * Lo que una cuenta puede elegir en el Perfil. Es un tipo aparte de {@link Rol} a propósito: ADMIN no existe aquí,
 * así que `{"modo":"ADMIN"}` ni siquiera se puede leer y nadie se convierte en administrador por esta vía.
 */
public enum Modo {
    ESTUDIANTE, TUTOR;

    Rol comoRol() {
        return this == TUTOR ? Rol.TUTOR : Rol.ESTUDIANTE;
    }
}
