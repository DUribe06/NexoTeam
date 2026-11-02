package com.grupo3.nexo.usuarios;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Endpoints de /api/usuarios/yo según el contrato (openapi/nexo-api.yaml). */
@RestController
@RequestMapping("/api/usuarios/yo")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public PerfilPropioDto perfil(@AuthenticationPrincipal Jwt jwt) {
        return usuarios.perfil(id(jwt));
    }

    @PutMapping("/modo")
    public PerfilPropioDto cambiarModo(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody SolicitudesUsuario.CambiarModo s) {
        return usuarios.cambiarModo(id(jwt), s.modo());
    }

    @PatchMapping("/perfil-tutor")
    public PerfilPropioDto editarPerfilTutor(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody SolicitudesUsuario.EditarPerfilTutor s) {
        return usuarios.editarPerfilTutor(id(jwt), s);
    }

    @GetMapping("/resumen")
    public ResumenActividadDto resumen(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Modo modo) {
        return usuarios.resumen(id(jwt), modo);
    }

    private static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
