package com.grupo3.nexo.usuarios;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grupo3.nexo.sesiones.ResumenService;

import jakarta.validation.Valid;

/** Endpoints de /api/usuarios/yo según el contrato (openapi/nexo-api.yaml). */
@RestController
@RequestMapping("/api/usuarios/yo")
public class UsuarioController {

    private final UsuarioService usuarios;
    private final ResumenService resumenes;
    private final EliminacionCuenta eliminacion;

    public UsuarioController(UsuarioService usuarios, ResumenService resumenes, EliminacionCuenta eliminacion) {
        this.usuarios = usuarios;
        this.resumenes = resumenes;
        this.eliminacion = eliminacion;
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

    /** POST y no DELETE: lleva la contraseña en el cuerpo, y muchos intermediarios descartan el cuerpo de un DELETE. */
    @PostMapping("/eliminar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SolicitudesUsuario.EliminarCuenta s) {
        eliminacion.eliminar(id(jwt), s.contrasena(), s.tokenGoogle());
    }

    @GetMapping("/resumen")
    public ResumenActividadDto resumen(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Modo modo) {
        return resumenes.de(id(jwt), modo);
    }

    private static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
