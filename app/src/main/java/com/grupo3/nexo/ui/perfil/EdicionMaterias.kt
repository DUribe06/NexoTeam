package com.grupo3.nexo.ui.perfil

import com.grupo3.nexo.domain.model.AreaEstudio
import java.text.Normalizer

/** Los límites que pone el servidor: una materia de hasta 80 letras y hasta 20 materias. */
internal const val MAXIMO_LETRAS_MATERIA = 80
internal const val MAXIMO_MATERIAS = 20

/** Lo que pasa al intentar agregar una materia: `lista` queda igual si no se pudo y `aviso` dice por qué. */
internal data class ResultadoMateria(val lista: List<String>, val aviso: String? = null)

/** "  cálculo   I " y "CALCULO I" son la misma materia: así compara el servidor y así se evita repetirla. */
private fun String.clave(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().replace(Regex("\\s+"), " ")

private fun String.limpia(): String = trim().replace(Regex("\\s+"), " ")

internal fun List<String>.agregarMateria(texto: String): ResultadoMateria {
    val materia = texto.limpia()
    return when {
        materia.isEmpty() -> ResultadoMateria(this)
        any { it.clave() == materia.clave() } -> ResultadoMateria(this, "Ya tienes $materia en tu lista.")
        materia.length > MAXIMO_LETRAS_MATERIA ->
            ResultadoMateria(this, "Usa un nombre de hasta $MAXIMO_LETRAS_MATERIA letras.")
        size >= MAXIMO_MATERIAS -> ResultadoMateria(this, "Puedes tener hasta $MAXIMO_MATERIAS materias.")
        else -> ResultadoMateria(this + materia)
    }
}

/** Materias conocidas que aún no están en la lista; lo escrito las va filtrando. */
internal fun sugerenciasDeMaterias(actuales: List<String>, escrito: String): List<String> {
    val buscado = escrito.clave()
    return AreaEstudio.entries.flatMap { it.claves }.distinctBy { it.clave() }
        .filter { sugerida -> actuales.none { it.clave() == sugerida.clave() } }
        .filter { buscado.isEmpty() || it.clave().contains(buscado) }
}
