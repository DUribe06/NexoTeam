package com.grupo3.nexo.sesiones;

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

import com.grupo3.nexo.sesiones.SesionDtos.SesionTutoria;
import com.grupo3.nexo.sesiones.SesionDtos.SolicitudAcuerdo;
import com.grupo3.nexo.sesiones.SesionDtos.SolicitudCalificar;
import com.grupo3.nexo.sesiones.SesionDtos.SolicitudIniciarSesion;
import com.grupo3.nexo.sesiones.SesionDtos.SolicitudRevision;
import com.grupo3.nexo.usuarios.Modo;

import jakarta.validation.Valid;

/** Endpoints de /api/sesiones según el contrato (openapi/nexo-api.yaml). */
@RestController
@RequestMapping("/api/sesiones")
public class SesionController {

    private final SesionTutoriaService sesiones;
    private final AsistenciaService asistencia;

    public SesionController(SesionTutoriaService sesiones, AsistenciaService asistencia) {
        this.sesiones = sesiones;
        this.asistencia = asistencia;
    }

    @GetMapping
    public List<SesionTutoria> listar(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Modo modo,
                                      @RequestParam(required = false) List<EstadoSesion> estado) {
        return sesiones.listar(id(jwt), modo, estado);
    }

    @GetMapping("/proximas")
    public List<SesionTutoria> proximas(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Modo modo) {
        return sesiones.proximas(id(jwt), modo);
    }

    @GetMapping("/historial")
    public List<SesionTutoria> historial(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Modo modo,
                                         @RequestParam(required = false) String materia) {
        return sesiones.historial(id(jwt), modo, materia);
    }

    @GetMapping("/{sesionId}")
    public SesionTutoria una(@AuthenticationPrincipal Jwt jwt, @PathVariable String sesionId) {
        return sesiones.una(id(jwt), sesionId);
    }

    @PostMapping("/{sesionId}/pin")
    public SesionTutoria enviarPin(@AuthenticationPrincipal Jwt jwt, @PathVariable String sesionId) {
        return asistencia.enviarPin(id(jwt), sesionId);
    }

    @PostMapping("/{sesionId}/iniciar")
    public SesionTutoria iniciar(@AuthenticationPrincipal Jwt jwt, @PathVariable String sesionId,
                                 @Valid @RequestBody SolicitudIniciarSesion s) {
        return asistencia.iniciar(id(jwt), sesionId, s);
    }

    @PostMapping("/{sesionId}/acuerdo")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionTutoria proponerAcuerdo(@AuthenticationPrincipal Jwt jwt, @PathVariable String sesionId,
                                         @Valid @RequestBody SolicitudAcuerdo s) {
        return asistencia.proponer(id(jwt), sesionId, s);
    }

    @PostMapping("/{sesionId}/acuerdo/aceptar")
    public SesionTutoria aceptarAcuerdo(@AuthenticationPrincipal Jwt jwt, @PathVariable String sesionId) {
        return asistencia.aceptar(id(jwt), sesionId);
    }

    @PostMapping("/{sesionId}/acuerdo/rechazar")
    public SesionTutoria rechazarAcuerdo(@AuthenticationPrincipal Jwt jwt, @PathVariable String sesionId) {
        return asistencia.rechazar(id(jwt), sesionId);
    }

    @PostMapping("/{sesionId}/revision")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionTutoria pedirRevision(@AuthenticationPrincipal Jwt jwt, @PathVariable String sesionId,
                                       @Valid @RequestBody SolicitudRevision s) {
        return asistencia.pedirRevision(id(jwt), sesionId, s.motivo());
    }

    @PostMapping("/{sesionId}/calificacion")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionTutoria calificar(@AuthenticationPrincipal Jwt jwt, @PathVariable String sesionId,
                                   @Valid @RequestBody SolicitudCalificar s) {
        return sesiones.calificar(id(jwt), sesionId, s);
    }

    private static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
