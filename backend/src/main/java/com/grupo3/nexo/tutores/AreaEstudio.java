package com.grupo3.nexo.tutores;

import java.util.List;

/** Agrupa materias en áreas reconocibles, con las mismas palabras clave que `AreaEstudio` de la app. */
public enum AreaEstudio {
    MATEMATICAS("Cálculo", "Álgebra", "Matemática"),
    PROGRAMACION("Programación", "Python", "Java"),
    CIENCIAS("Física", "Química", "Biología", "Estadística"),
    IDIOMAS("Inglés", "Redacción"),
    DATOS("Bases de Datos", "SQL"),
    DISENO("Diseño", "Figma"),
    NEGOCIOS("Contabilidad", "Economía");

    private final List<String> claves;

    AreaEstudio(String... claves) {
        this.claves = List.of(claves);
    }

    public List<String> claves() {
        return claves;
    }
}
