package com.grupo3.nexo.tutores;

import java.time.LocalTime;

/**
 * Parte del día para filtrar por horario (HU-05). El inicio entra en la franja y el fin no. La noche llega hasta las
 * 24:00, que no existe como hora; basta con el último segundo porque las horas del calendario son enteras.
 */
public enum Franja {
    MANANA(LocalTime.of(6, 0), LocalTime.of(12, 0)),
    TARDE(LocalTime.of(12, 0), LocalTime.of(18, 0)),
    NOCHE(LocalTime.of(18, 0), LocalTime.of(23, 59, 59));

    private final LocalTime desde;
    private final LocalTime hasta;

    Franja(LocalTime desde, LocalTime hasta) {
        this.desde = desde;
        this.hasta = hasta;
    }

    public LocalTime desde() { return desde; }
    public LocalTime hasta() { return hasta; }
}
