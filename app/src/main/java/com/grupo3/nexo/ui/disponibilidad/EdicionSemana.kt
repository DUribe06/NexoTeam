package com.grupo3.nexo.ui.disponibilidad

import com.grupo3.nexo.domain.model.DiaDisponibilidad
import com.grupo3.nexo.domain.model.HoraDisponible
import com.grupo3.nexo.domain.model.Modalidad

/** Una hora de la cuadrícula: columna del día y fila de la hora. */
internal data class Celda(val dia: Int, val hora: Int)

/** Un gesto sobre la cuadrícula: el rectángulo entre dos celdas recibe `valor`. Un toque es un rectángulo de una celda. */
internal data class Trazo(val inicio: Celda, val fin: Celda, val valor: Set<Modalidad>) {
    fun contiene(c: Celda): Boolean =
        c.dia in minOf(inicio.dia, fin.dia)..maxOf(inicio.dia, fin.dia) &&
            c.hora in minOf(inicio.hora, fin.hora)..maxOf(inicio.hora, fin.hora)
}

/** Lo que hace el pincel sobre una hora: la marca, o la borra si ya estaba marcada igual. */
internal fun destinoDe(actual: Set<Modalidad>, pincel: Set<Modalidad>): Set<Modalidad> =
    if (actual == pincel) emptySet() else pincel

internal fun List<DiaDisponibilidad>.conTrazo(trazo: Trazo): List<DiaDisponibilidad> =
    cambiar { celda, actual -> if (trazo.contiene(celda)) trazo.valor else actual }

/** Marca todas las horas de un día; si ya estaban todas marcadas con el pincel, las borra. */
internal fun List<DiaDisponibilidad>.alternarDia(dia: Int, pincel: Set<Modalidad>): List<DiaDisponibilidad> {
    val horas = getOrNull(dia)?.horas.orEmpty()
    val valor = if (horas.all { it.modalidades == pincel }) emptySet() else pincel
    return cambiar { celda, actual -> if (celda.dia == dia) valor else actual }
}

/** Marca una misma hora en toda la semana; si ya estaba marcada con el pincel en todos los días, la borra. */
internal fun List<DiaDisponibilidad>.alternarHora(hora: Int, pincel: Set<Modalidad>): List<DiaDisponibilidad> {
    val valor = if (all { it.horas.getOrNull(hora)?.modalidades == pincel }) emptySet() else pincel
    return cambiar { celda, actual -> if (celda.hora == hora) valor else actual }
}

internal fun List<DiaDisponibilidad>.sinHoras(): List<DiaDisponibilidad> = cambiar { _, _ -> emptySet() }

private fun List<DiaDisponibilidad>.cambiar(
    valor: (Celda, Set<Modalidad>) -> Set<Modalidad>
): List<DiaDisponibilidad> = mapIndexed { d, dia ->
    dia.copy(horas = dia.horas.mapIndexed { h, hora -> HoraDisponible(hora.hora, valor(Celda(d, h), hora.modalidades)) })
}
