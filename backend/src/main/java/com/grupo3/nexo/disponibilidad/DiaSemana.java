package com.grupo3.nexo.disponibilidad;

import java.time.LocalDate;

/** En orden de calendario, de lunes a domingo: el orden del enum es el que usan las respuestas. */
public enum DiaSemana {
    LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO;

    public static DiaSemana de(LocalDate fecha) {
        return values()[fecha.getDayOfWeek().getValue() - 1];
    }
}
