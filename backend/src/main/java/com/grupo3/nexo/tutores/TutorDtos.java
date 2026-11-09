package com.grupo3.nexo.tutores;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.grupo3.nexo.disponibilidad.DisponibilidadDtos.FranjaTutor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Esquemas de tutores del contrato. */
public final class TutorDtos {

    private TutorDtos() {
    }

    public record Tutor(UUID id, String nombre, String handle, String bio, String universidad, List<String> materias,
                        double calificacionPromedio, long totalResenas, int sesionesTotales, int estudiantes,
                        double precioHora, boolean enLinea, List<FranjaTutor> horario, int penalizaciones,
                        String fotoUrl) {
    }

    public record ResenaDto(UUID id, UUID tutorId, String autor, String handleAutor, String texto, int estrellas,
                            LocalDate fecha) {
    }

    public record ReporteDto(UUID id, OffsetDateTime creadoEn) {
    }

    public record SolicitudReporte(@NotBlank @Size(min = 3, max = 500) String motivo) {
        public SolicitudReporte {
            motivo = motivo == null ? null : motivo.trim();
        }
    }

    /** Promedio y total de reseñas de un tutor, los que mantiene la base al calificar. */
    public record ResumenResenas(UUID tutorId, double promedio, long total) {
    }

    /** Sesiones completadas de un tutor y los estudiantes distintos que atendió. */
    public record ActividadTutor(int sesiones, int estudiantes) {
        static final ActividadTutor NINGUNA = new ActividadTutor(0, 0);
    }
}
