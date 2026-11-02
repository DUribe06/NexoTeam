package com.grupo3.nexo.domain.model

/**
 * Agrupa las materias en áreas reconocibles para filtrar con un toque
 * ("Matemáticas" encuentra Cálculo I, Álgebra y Matemática Básica).
 */
enum class AreaEstudio(val etiqueta: String, val claves: List<String>) {
    MATEMATICAS("Matemáticas", listOf("Cálculo", "Álgebra", "Matemática")),
    PROGRAMACION("Programación", listOf("Programación", "Python", "Java")),
    CIENCIAS("Ciencias", listOf("Física", "Química", "Biología", "Estadística")),
    IDIOMAS("Idiomas", listOf("Inglés", "Redacción")),
    DATOS("Bases de datos", listOf("Bases de Datos", "SQL")),
    DISENO("Diseño", listOf("Diseño", "Figma")),
    NEGOCIOS("Negocios", listOf("Contabilidad", "Economía"));

    companion object {
        fun desde(nombre: String?): AreaEstudio? = entries.firstOrNull { it.name == nombre }

        /** Área a la que pertenece una materia (para sugerir tutores alternativos, RF6). */
        fun deMateria(materia: String): AreaEstudio? =
            entries.firstOrNull { area -> area.claves.any { materia.contains(it, ignoreCase = true) } }
    }
}
