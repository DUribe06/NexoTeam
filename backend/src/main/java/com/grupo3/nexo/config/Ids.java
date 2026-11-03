package com.grupo3.nexo.config;

import java.util.Optional;
import java.util.UUID;

/** Los ids de la ruta llegan en texto: uno mal formado tampoco es de ningún recurso, así que da el mismo 404. */
public final class Ids {

    private Ids() {
    }

    public static Optional<UUID> leer(String texto) {
        try {
            return Optional.of(UUID.fromString(texto));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
