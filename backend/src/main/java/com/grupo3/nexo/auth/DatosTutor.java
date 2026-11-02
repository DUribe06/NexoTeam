package com.grupo3.nexo.auth;

import java.util.List;

import com.grupo3.nexo.usuarios.Modalidad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record DatosTutor(
    @NotEmpty(message = "indica al menos una materia") List<@NotBlank @Size(max = 80) String> materias,
    @NotEmpty(message = "indica al menos una modalidad") List<Modalidad> modalidades) {
}
