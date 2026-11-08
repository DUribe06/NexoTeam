package com.grupo3.nexo.disponibilidad;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Las únicas horas en que se puede publicar disponibilidad (RF7). La base exige las mismas. */
public final class HorasCalendario {

    public static final List<LocalTime> HORAS = List.of(LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(12, 0),
        LocalTime.of(14, 0), LocalTime.of(16, 0), LocalTime.of(18, 0), LocalTime.of(20, 0));

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm");

    private HorasCalendario() {
    }

    public static String texto(LocalTime hora) {
        return hora.format(FORMATO);
    }
}
