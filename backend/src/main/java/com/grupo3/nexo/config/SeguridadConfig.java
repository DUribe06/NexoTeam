package com.grupo3.nexo.config;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import com.grupo3.nexo.usuarios.Rol;
import com.grupo3.nexo.usuarios.UsuarioRepository;

/**
 * API sin estado, autenticada con JWT. Son públicos: /api/salud, el contrato en /openapi, los archivos del panel web
 * (/admin; sus datos sí exigen token) y la política de privacidad, y los endpoints de
 * acceso de /api/auth (registro, verificar el correo, reenviar el código, login, refrescar, recuperar y restablecer
 * la contraseña). Todo lo demás exige
 * un token válido, y /api/admin/** además el rol ADMIN (D3), que se lee de la base: el token no lleva el rol.
 */
@Configuration
public class SeguridadConfig {

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, UsuarioRepository usuarios) throws Exception {
        AuthenticationEntryPoint noAutenticado = (peticion, respuesta, e) -> {
            respuesta.setStatus(401);
            respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
            respuesta.setCharacterEncoding("UTF-8");
            respuesta.getWriter().write(
                "{\"codigo\":\"NO_AUTENTICADO\",\"mensaje\":\"Falta el token o no es válido\",\"detalles\":[]}");
        };

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                // /error: cuando una petición falla, el contenedor la reenvía ahí; si no es pública, todo error se vería como 401
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/salud", "/openapi/**", "/admin", "/admin/", "/admin/**", "/privacidad.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/verificar-correo",
                    "/api/auth/reenviar-codigo", "/api/auth/login", "/api/auth/google", "/api/auth/refrescar",
                    "/api/auth/recuperar", "/api/auth/restablecer").permitAll()
                .requestMatchers("/api/admin/**").access(soloAdministradores(usuarios))
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(noAutenticado))
            .oauth2ResourceServer(o -> o
                .authenticationEntryPoint(noAutenticado)
                .jwt(j -> { }));
        return http.build();
    }

    private static AuthorizationManager<RequestAuthorizationContext> soloAdministradores(UsuarioRepository usuarios) {
        return (autenticacion, contexto) -> new AuthorizationDecision(esAdministrador(autenticacion.get(), usuarios));
    }

    private static boolean esAdministrador(Authentication autenticacion, UsuarioRepository usuarios) {
        return autenticacion instanceof JwtAuthenticationToken token
            && usuarios.existsByIdAndRol(UUID.fromString(token.getToken().getSubject()), Rol.ADMIN);
    }
}
