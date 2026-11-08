package com.grupo3.nexo.config;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** El contrato habla en hora de Colombia, y el reloj del servidor va en UTC. */
public final class HoraColombia {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private HoraColombia() {
    }

    public static LocalDate hoy(Clock reloj) {
        return LocalDate.ofInstant(reloj.instant(), ZONA);
    }

    /** Un momento como lo pide el contrato: `2026-11-02T14:00:00-05:00`. */
    public static OffsetDateTime momento(Instant instante) {
        return instante.atZone(ZONA).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS);
    }
}
