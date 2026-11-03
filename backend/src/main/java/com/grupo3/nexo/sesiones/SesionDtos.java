package com.grupo3.nexo.sesiones;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.grupo3.nexo.usuarios.Modalidad;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Esquemas de sesiones del contrato. */
public final class SesionDtos {

    private SesionDtos() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SesionTutoria(UUID id, UUID solicitudId, UUID contraparteId, String conQuien, String materia,
                                LocalDate fecha, String hora, double duracionHoras, Modalidad modalidad,
                                EstadoSesion estado, boolean comoTutor, Integer calificacion, String comentario,
                                Integer calificacionRecibida, Instant iniciadaEn, boolean pinEnviado, String pin,
                                Instant limite, String horaPactada, Boolean retardoMio, CierreSesion cierre,
                                NivelPenalizacion penalizacion, AcuerdoPendiente acuerdo, EstadoRevision revision,
                                boolean puedePedirRevision) {
    }

    /** Lo que uno propuso por el chat y falta que el otro responda. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AcuerdoPendiente(TipoAcuerdo tipo, String nuevaHora, boolean propuestoPorMi) {
    }

    public enum TipoAcuerdo { RETARDO, CANCELACION }

    public enum CierreSesion { TUTOR_NO_LLEGO, ESTUDIANTE_NO_LLEGO, ACORDADA }

    public enum NivelPenalizacion { NORMAL, GRAVE }

    public enum EstadoRevision { ABIERTA, ACEPTADA, RECHAZADA }

    /** `minutos`, solo en un retardo: cuánto después de la hora original empezarían. */
    public record SolicitudAcuerdo(@NotNull TipoAcuerdo tipo, @Min(5) @Max(60) Integer minutos) {
    }

    public record SolicitudIniciarSesion(@NotNull @Pattern(regexp = "[0-9]{4}") String pin) {
    }

    public record SolicitudRevision(@NotBlank @Size(min = 10, max = 500) String motivo) {
        public SolicitudRevision {
            motivo = motivo == null ? null : motivo.trim();
        }
    }

    public record SolicitudCalificar(@NotNull @Min(1) @Max(5) Integer estrellas, @Size(max = 500) String comentario) {
    }
}
