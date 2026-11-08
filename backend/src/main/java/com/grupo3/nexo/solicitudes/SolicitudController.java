package com.grupo3.nexo.solicitudes;

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

import com.grupo3.nexo.solicitudes.SolicitudDtos.ResultadoAceptar;
import com.grupo3.nexo.solicitudes.SolicitudDtos.SolicitudDto;
import com.grupo3.nexo.solicitudes.SolicitudDtos.SolicitudNueva;
import com.grupo3.nexo.solicitudes.SolicitudDtos.SolicitudRechazar;
import com.grupo3.nexo.usuarios.Modo;

import jakarta.validation.Valid;

/** Endpoints de /api/solicitudes según el contrato (openapi/nexo-api.yaml). */
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudes;

    public SolicitudController(SolicitudService solicitudes) {
        this.solicitudes = solicitudes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudDto enviar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SolicitudNueva s) {
        return solicitudes.enviar(id(jwt), s);
    }

    @GetMapping
    public List<SolicitudDto> listar(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Modo modo,
                                     @RequestParam(required = false) List<EstadoSolicitud> estado) {
        return solicitudes.listar(id(jwt), modo, estado);
    }

    @GetMapping("/{solicitudId}")
    public SolicitudDto una(@AuthenticationPrincipal Jwt jwt, @PathVariable String solicitudId) {
        return solicitudes.una(id(jwt), solicitudId);
    }

    @PostMapping("/{solicitudId}/aceptar")
    public ResultadoAceptar aceptar(@AuthenticationPrincipal Jwt jwt, @PathVariable String solicitudId) {
        return solicitudes.aceptar(id(jwt), solicitudId);
    }

    @PostMapping("/{solicitudId}/rechazar")
    public SolicitudDto rechazar(@AuthenticationPrincipal Jwt jwt, @PathVariable String solicitudId,
                                 @Valid @RequestBody(required = false) SolicitudRechazar s) {
        return solicitudes.rechazar(id(jwt), solicitudId, s == null ? null : s.motivo());
    }

    @PostMapping("/{solicitudId}/cancelar")
    public SolicitudDto cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable String solicitudId) {
        return solicitudes.cancelar(id(jwt), solicitudId);
    }

    private static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
