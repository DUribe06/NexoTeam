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
                        double precioHora, boolean enLinea, List<FranjaTutor> horario) {
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

    /** Promedio y total de reseñas de un tutor, calculados en la base. */
    public record ResumenResenas(UUID tutorId, double promedio, long total) {
    }
}
