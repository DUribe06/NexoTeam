package com.grupo3.nexo.usuarios;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** `ResumenActividad` del contrato; `promedioRecibido` se omite si aún no hay calificaciones. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResumenActividadDto(int sesiones, double horas, int porCalificar, Double promedioRecibido,
                                  int totalResenas, int personas, List<String> materias) {

    static ResumenActividadDto sinActividad() {
        return new ResumenActividadDto(0, 0, 0, null, 0, 0, List.of());
    }
}
