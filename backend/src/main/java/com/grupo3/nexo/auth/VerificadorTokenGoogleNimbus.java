package com.grupo3.nexo.auth;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.grupo3.nexo.error.ErrorNegocio;

/**
 * Verifica la firma con las llaves públicas de Google, y que el token sea para esta app, de Google y vigente.
 * No se expone como bean `JwtDecoder`: ese tipo lo usa la seguridad para los tokens propios de Nexo.
 */
@Component
class VerificadorTokenGoogleNimbus implements VerificadorTokenGoogle {

    private static final String LLAVES_GOOGLE = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> EMISORES = Set.of("https://accounts.google.com", "accounts.google.com");

    /** Null si este servidor no tiene el cliente de Google configurado. */
    private final JwtDecoder decodificador;

    @Autowired
    VerificadorTokenGoogleNimbus(@Value("${nexo.google.client-id:}") String clientId) {
        this(clientId.isBlank() ? null : decodificadorDeGoogle(clientId));
    }

    VerificadorTokenGoogleNimbus(JwtDecoder decodificador) {
        this.decodificador = decodificador;
    }

    private static JwtDecoder decodificadorDeGoogle(String clientId) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withJwkSetUri(LLAVES_GOOGLE).build();
        decodificador.setJwtValidator(validador(clientId));
        return decodificador;
    }

    static OAuth2TokenValidator<Jwt> validador(String clientId) {
        return new DelegatingOAuth2TokenValidator<>(List.of(
            new JwtTimestampValidator(),
            // Como texto: getIssuer() exige una URL y Google también emite "accounts.google.com", sin https
            exige("emisor", jwt -> EMISORES.contains(String.valueOf(jwt.getClaimAsString("iss")))),
            exige("audiencia", jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId))));
    }

    private static OAuth2TokenValidator<Jwt> exige(String que, Predicate<Jwt> condicion) {
        return jwt -> condicion.test(jwt)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", que + " no válido", null));
    }

    @Override
    public IdentidadGoogle verificar(String tokenId) {
        if (decodificador == null) {
            throw noDisponible();
        }
        Jwt jwt;
        try {
            jwt = decodificador.decode(tokenId);
        } catch (BadJwtException e) {
            throw tokenInvalido();
        } catch (JwtException e) {
            // No se pudieron leer las llaves de Google: el token puede ser bueno, así que no se le culpa a la persona
            throw noDisponible();
        }
        String correo = jwt.getClaimAsString("email");
        // Sin correo confirmado por Google no se puede saber de quién es la cuenta con ese correo
        if (jwt.getSubject() == null || correo == null || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw tokenInvalido();
        }
        return new IdentidadGoogle(jwt.getSubject(), correo.trim().toLowerCase(Locale.ROOT), jwt.getClaimAsString("name"),
            jwt.getClaimAsString("picture"));
    }

    private static ErrorNegocio tokenInvalido() {
        return new ErrorNegocio(HttpStatus.UNAUTHORIZED, "TOKEN_GOOGLE_INVALIDO", "No pudimos validar tu cuenta de Google");
    }

    private static ErrorNegocio noDisponible() {
        return new ErrorNegocio(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_NO_DISPONIBLE", "El inicio con Google no está disponible en este momento");
    }
}
