package com.grupo3.nexo.disponibilidad;

import java.time.LocalDate;
import java.util.List;

import com.grupo3.nexo.usuarios.Modalidad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Esquemas de disponibilidad del contrato. */
public final class DisponibilidadDtos {

    private DisponibilidadDtos() {
    }

    public record HoraDisponible(String hora, List<Modalidad> modalidades) {
    }

    public record DiaDisponibilidad(DiaSemana dia, List<HoraDisponible> horas) {
    }

    public record SemanaDisponibilidad(boolean creado, List<DiaDisponibilidad> dias) {
    }

    public record DiaSolicitable(LocalDate fecha, List<HoraDisponible> franjas) {
    }

    public record FranjaTutor(DiaSemana dia, String hora, List<Modalidad> modalidades) {
    }

    public record SolicitudDefinirHora(
        @NotNull DiaSemana dia,
        @NotNull @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "debe tener el formato HH:mm") String hora,
        @NotNull List<@NotNull Modalidad> modalidades) {
    }

    /** La semana completa de una vez: lo que no venga deja de estar publicado. Son 7 días de 7 horas. */
    public record SolicitudDefinirSemana(@NotNull @Size(max = 49) List<@NotNull @Valid SolicitudDefinirHora> horas) {
    }

    /** `nuevo` decide entre el 201 y el 200 de POST /api/disponibilidad/yo. */
    public record CalendarioCreado(boolean nuevo, SemanaDisponibilidad semana) {
    }
}
