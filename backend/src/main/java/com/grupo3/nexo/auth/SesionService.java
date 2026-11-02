package com.grupo3.nexo.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.usuarios.EstadoCuenta;
import com.grupo3.nexo.usuarios.Usuario;
import com.grupo3.nexo.usuarios.UsuarioRepository;

/** Emite, renueva y revoca sesiones: JWT de acceso (15 min) + token de refresco opaco rotativo (30 días). */
@Service
public class SesionService {

    static final Duration VIGENCIA_ACCESO = Duration.ofMinutes(15);
    static final Duration VIGENCIA_REFRESCO = Duration.ofDays(30);

    private final SecureRandom azar = new SecureRandom();
    private final JwtEncoder jwt;
    private final TokenRefrescoRepository tokens;
    private final UsuarioRepository usuarios;
    private final Clock reloj;

    public SesionService(JwtEncoder jwt, TokenRefrescoRepository tokens, UsuarioRepository usuarios, Clock reloj) {
        this.jwt = jwt;
        this.tokens = tokens;
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    /** Abre una sesión nueva para el usuario. */
    public SesionDto iniciar(Usuario usuario) {
        Instant ahora = reloj.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(usuario.getId().toString())
            .issuedAt(ahora)
            .expiresAt(ahora.plus(VIGENCIA_ACCESO))
            .claim("rol", usuario.getRol().name())
            .claim("correo", usuario.getCorreo())
            .build();
        String acceso = jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

        byte[] aleatorio = new byte[32];
        azar.nextBytes(aleatorio);
        String refresco = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
        tokens.save(new TokenRefresco(usuario.getId(), hash(refresco), ahora.plus(VIGENCIA_REFRESCO), ahora));

        return new SesionDto(acceso, refresco, (int) VIGENCIA_ACCESO.toSeconds(), UsuarioDto.de(usuario));
    }

    /**
     * Cambia un token de refresco por una sesión nueva. Cada token sirve una sola vez: si alguien presenta uno ya
     * usado, se asume que fue robado y se cierran todas las sesiones de esa cuenta.
     */
    @Transactional(noRollbackFor = ErrorNegocio.class)
    public SesionDto refrescar(String tokenRefresco) {
        Instant ahora = reloj.instant();
        TokenRefresco guardado = tokens.findByTokenHash(hash(tokenRefresco)).orElseThrow(SesionService::tokenInvalido);
        if (guardado.estaRevocado()) {
            revocarTodos(guardado.getUsuarioId(), ahora);
            throw tokenInvalido();
        }
        if (guardado.estaVencido(ahora)) {
            guardado.revocar(ahora);
            throw tokenInvalido();
        }
        Usuario usuario = usuarios.findById(guardado.getUsuarioId()).orElseThrow(SesionService::tokenInvalido);
        if (usuario.getEstadoCuenta() != EstadoCuenta.ACTIVA) {
            guardado.revocar(ahora);
            throw tokenInvalido();
        }
        guardado.revocar(ahora);
        return iniciar(usuario);
    }

    /** Cierra la sesión del token de refresco indicado, solo si pertenece a ese usuario. Es idempotente. */
    @Transactional
    public void cerrar(UUID usuarioId, String tokenRefresco) {
        tokens.findByTokenHash(hash(tokenRefresco))
            .filter(t -> t.getUsuarioId().equals(usuarioId))
            .ifPresent(t -> t.revocar(reloj.instant()));
    }

    private void revocarTodos(UUID usuarioId, Instant ahora) {
        tokens.findByUsuarioIdAndRevocadoEnIsNull(usuarioId).forEach(t -> t.revocar(ahora));
    }

    static String hash(String token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ErrorNegocio tokenInvalido() {
        return new ErrorNegocio(HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO", "La sesión no es válida; inicia sesión de nuevo");
    }
}
