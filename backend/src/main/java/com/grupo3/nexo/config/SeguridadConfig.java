package com.grupo3.nexo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API sin estado, autenticada con JWT. Son públicos: /api/salud, el contrato en /openapi y los endpoints de
 * acceso de /api/auth (registro, verificar el correo, reenviar el código, login y refrescar). Todo lo demás exige
 * un token válido, y /api/admin/** además el rol ADMIN (D3).
 */
@Configuration
public class SeguridadConfig {

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
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
                .requestMatchers("/api/salud", "/openapi/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/verificar-correo",
                    "/api/auth/reenviar-codigo", "/api/auth/login", "/api/auth/refrescar").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(noAutenticado))
            .oauth2ResourceServer(o -> o
                .authenticationEntryPoint(noAutenticado)
                .jwt(j -> j.jwtAuthenticationConverter(convertidorDeRoles())));
        return http.build();
    }

    /** El claim "rol" del token se convierte en la autoridad ROLE_<rol>. */
    private JwtAuthenticationConverter convertidorDeRoles() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("rol");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }
}
