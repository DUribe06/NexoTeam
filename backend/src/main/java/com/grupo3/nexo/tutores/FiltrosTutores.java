package com.grupo3.nexo.tutores;

import java.util.List;
import java.util.stream.Stream;

import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.usuarios.Modalidad;

/** Filtros de GET /api/tutores. Todos son opcionales y se combinan con Y. */
public record FiltrosTutores(String q, List<String> materias, AreaEstudio area, Modalidad modalidad, Franja franja,
                             boolean enLinea) {

    private static final int MAXIMO_Q = 100;
    private static final int MAXIMO_MATERIA = 80;

    public FiltrosTutores {
        q = q == null ? "" : q.trim();
        materias = materias == null ? List.of() : materias.stream().map(String::trim).filter(m -> !m.isEmpty()).toList();
        if (q.length() > MAXIMO_Q) {
            throw ErrorNegocio.datosInvalidos("q", "admite hasta " + MAXIMO_Q + " caracteres");
        }
        if (materias.stream().anyMatch(m -> m.length() > MAXIMO_MATERIA)) {
            throw ErrorNegocio.datosInvalidos("materia", "admite hasta " + MAXIMO_MATERIA + " caracteres");
        }
    }

    /** Las materias pedidas más las palabras clave del área: basta con que una materia del tutor contenga alguna. */
    List<String> clavesDeMateria() {
        return area == null ? materias : Stream.concat(materias.stream(), area.claves().stream()).toList();
    }
}
