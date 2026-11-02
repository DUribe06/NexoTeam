package com.grupo3.nexo.tutores;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grupo3.nexo.tutores.TutorDtos.ReporteDto;
import com.grupo3.nexo.tutores.TutorDtos.ResenaDto;
import com.grupo3.nexo.tutores.TutorDtos.SolicitudReporte;
import com.grupo3.nexo.tutores.TutorDtos.Tutor;
import com.grupo3.nexo.usuarios.Modalidad;

import jakarta.validation.Valid;

/** Endpoints de /api/tutores según el contrato (openapi/nexo-api.yaml). */
@RestController
@RequestMapping("/api/tutores")
public class TutorController {

    private final TutorService tutores;

    public TutorController(TutorService tutores) {
        this.tutores = tutores;
    }

    /** `orden` solo tiene un valor posible y ya es el que se aplica; se declara para rechazar cualquier otro con un 400. */
    @GetMapping
    public PaginaDto<Tutor> buscar(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(required = false) List<String> materia,
                                   @RequestParam(required = false) AreaEstudio area,
                                   @RequestParam(required = false) Modalidad modalidad,
                                   @RequestParam(required = false) Franja franja,
                                   @RequestParam(defaultValue = "false") boolean enLinea,
                                   @RequestParam(required = false) OrdenTutores orden,
                                   @RequestParam(defaultValue = "0") int pagina,
                                   @RequestParam(defaultValue = "20") int tamano) {
        return tutores.buscar(id(jwt), new FiltrosTutores(q, materia, area, modalidad, franja, enLinea), pagina, tamano);
    }

    @GetMapping("/{tutorId}")
    public Tutor detalle(@AuthenticationPrincipal Jwt jwt, @PathVariable String tutorId) {
        return tutores.detalle(id(jwt), tutorId);
    }

    @GetMapping("/{tutorId}/resenas")
    public PaginaDto<ResenaDto> resenas(@AuthenticationPrincipal Jwt jwt, @PathVariable String tutorId,
                                        @RequestParam(defaultValue = "0") int pagina,
                                        @RequestParam(defaultValue = "20") int tamano) {
        return tutores.resenas(id(jwt), tutorId, pagina, tamano);
    }

    @PostMapping("/{tutorId}/reportes")
    @ResponseStatus(HttpStatus.CREATED)
    public ReporteDto reportar(@AuthenticationPrincipal Jwt jwt, @PathVariable String tutorId,
                               @Valid @RequestBody SolicitudReporte s) {
        return tutores.reportar(id(jwt), tutorId, s.motivo());
    }

    private static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
