package com.grupo3.nexo.usuarios;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Poner y quitar la foto de perfil. Solo se aceptan direcciones de Cloudinary, donde la sube la app. */
@RestController
public class FotoPerfilController {

    public record CambiarFoto(@NotNull @Size(max = 1000) @Pattern(regexp = "https://res\\.cloudinary\\.com/.+") String url) {
    }

    private final FotoPerfilService fotos;

    public FotoPerfilController(FotoPerfilService fotos) {
        this.fotos = fotos;
    }

    @PutMapping("/api/usuarios/yo/foto")
    public PerfilPropioDto cambiar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CambiarFoto s) {
        return fotos.cambiar(UUID.fromString(jwt.getSubject()), s.url());
    }

    @DeleteMapping("/api/usuarios/yo/foto")
    public PerfilPropioDto quitar(@AuthenticationPrincipal Jwt jwt) {
        return fotos.quitar(UUID.fromString(jwt.getSubject()));
    }
}
