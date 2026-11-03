package com.grupo3.nexo.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.grupo3.nexo.usuarios.EstadoCuenta;

/** Esquemas del panel de administración (RF12). */
public final class AdminDtos {

    private AdminDtos() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReporteAdmin(UUID id, UUID tutorId, String tutor, String correoTutor, UUID reportanteId, String reportante,
                               String motivo, OffsetDateTime creadoEn, OffsetDateTime cerradoEn) {
    }

    public record RevisionAdmin(UUID id, UUID usuarioId, String persona, String correo, String nivel, String cierre,
                                String materia, OffsetDateTime sesion, OffsetDateTime penalizadaEn, String motivo,
                                OffsetDateTime pedidaEn) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UsuarioAdmin(UUID id, String nombre, String correo, String handle, EstadoCuenta estadoCuenta,
                               boolean esTutor, int penalizacionesComoTutor, int penalizacionesComoEstudiante,
                               OffsetDateTime pausaHasta, long reportesAbiertos) {
    }
}
