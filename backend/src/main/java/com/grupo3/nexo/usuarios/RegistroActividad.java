package com.grupo3.nexo.usuarios;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Toda petición con token cuenta como actividad de esa cuenta; de ahí sale `enLinea`. */
@Component
public class RegistroActividad implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RegistroActividad.class);

    private final UsuarioService usuarios;

    public RegistroActividad(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object manejador) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken token) {
            try {
                usuarios.registrarActividad(UUID.fromString(token.getToken().getSubject()));
            } catch (RuntimeException e) {
                // Es un dato de cortesía: si no se pudo guardar, la petición sigue
                log.warn("No se pudo registrar la actividad: {}", e.getMessage());
            }
        }
        return true;
    }
}
