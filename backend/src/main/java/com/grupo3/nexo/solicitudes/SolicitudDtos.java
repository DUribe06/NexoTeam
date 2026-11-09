package com.grupo3.nexo.solicitudes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.grupo3.nexo.chats.ChatDtos.ChatDto;
import com.grupo3.nexo.sesiones.SesionDtos.SesionTutoria;
import com.grupo3.nexo.usuarios.Modalidad;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Esquemas de solicitudes del contrato. */
public final class SolicitudDtos {

    private SolicitudDtos() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SolicitudDto(UUID id, UUID tutorId, String nombreTutor, UUID estudianteId, String nombreEstudiante,
                               String materia, String tema, LocalDate fecha, String hora, Modalidad modalidad,
                               EstadoSolicitud estado, String mensaje, String motivoRechazo, OffsetDateTime enviada,
                               OffsetDateTime venceEn, int penalizacionesEstudiante, BigDecimal precioPropuesto) {
    }

    public record SolicitudNueva(@NotBlank String tutorId,
                                 @NotBlank @Size(max = 80) String materia,
                                 @NotBlank @Size(max = 200) String tema,
                                 @NotNull LocalDate fecha,
                                 @NotNull @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$") String hora,
                                 @NotNull Modalidad modalidad,
                                 @Size(max = 500) String mensaje,
                                 @DecimalMin("1") @DecimalMax("10000000") @Digits(integer = 8, fraction = 2) BigDecimal precioPropuesto) {
    }

    public record SolicitudRechazar(@Size(max = 300) String motivo) {
    }

    public record ResultadoAceptar(SolicitudDto solicitud, SesionTutoria sesion, ChatDto chat) {
    }
}
