package com.grupo3.nexo.disponibilidad;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.grupo3.nexo.disponibilidad.DisponibilidadDtos.CalendarioCreado;
import com.grupo3.nexo.disponibilidad.DisponibilidadDtos.DiaSolicitable;
import com.grupo3.nexo.disponibilidad.DisponibilidadDtos.SemanaDisponibilidad;
import com.grupo3.nexo.disponibilidad.DisponibilidadDtos.SolicitudDefinirHora;

import jakarta.validation.Valid;

/** /api/disponibilidad/yo y /api/tutores/{tutorId}/disponibilidad según el contrato (openapi/nexo-api.yaml). */
@RestController
public class DisponibilidadController {

    private final DisponibilidadService disponibilidad;

    public DisponibilidadController(DisponibilidadService disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    @GetMapping("/api/disponibilidad/yo")
    public SemanaDisponibilidad propia(@AuthenticationPrincipal Jwt jwt) {
        return disponibilidad.propia(id(jwt));
    }

    /** 201 si se creó; 200 si ya existía. */
    @PostMapping("/api/disponibilidad/yo")
    public ResponseEntity<SemanaDisponibilidad> crear(@AuthenticationPrincipal Jwt jwt) {
        CalendarioCreado resultado = disponibilidad.crear(id(jwt));
        return ResponseEntity.status(resultado.nuevo() ? HttpStatus.CREATED : HttpStatus.OK).body(resultado.semana());
    }

    @PutMapping("/api/disponibilidad/yo/horas")
    public SemanaDisponibilidad definirHora(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SolicitudDefinirHora s) {
        return disponibilidad.definirHora(id(jwt), s);
    }

    @GetMapping("/api/tutores/{tutorId}/disponibilidad")
    public List<DiaSolicitable> solicitables(@AuthenticationPrincipal Jwt jwt, @PathVariable String tutorId) {
        return disponibilidad.solicitables(id(jwt), tutorId);
    }

    private static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
