package com.grupo3.nexo.usuarios;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** `ResumenActividad` del contrato; `promedioRecibido` y `suspendidaHasta` se omiten si no hay valor. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResumenActividadDto(int sesiones, double horas, int porCalificar, Double promedioRecibido,
                                  int totalResenas, int personas, List<String> materias, int penalizaciones,
                                  OffsetDateTime suspendidaHasta) {
}
