package com.grupo3.nexo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import com.grupo3.nexo.error.ErrorNegocio;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/** Con tokens firmados aquí, para comprobar cada regla sin depender de la red. */
class VerificadorTokenGoogleTest {

    private static final String CLIENTE = "cliente-web.apps.googleusercontent.com";
    private static KeyPair llaves;
    private static VerificadorTokenGoogleNimbus verificador;

    @BeforeAll
    static void preparar() throws Exception {
        llaves = par();
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withPublicKey((RSAPublicKey) llaves.getPublic()).build();
        decodificador.setJwtValidator(VerificadorTokenGoogleNimbus.validador(CLIENTE));
        verificador = new VerificadorTokenGoogleNimbus(decodificador);
    }

    private static KeyPair par() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        return generador.generateKeyPair();
    }

    private static String token(KeyPair firma, Consumer<JwtClaimsSet.Builder> ajuste) {
        JwtClaimsSet.Builder datos = JwtClaimsSet.builder()
            .issuer("https://accounts.google.com")
            .audience(List.of(CLIENTE))
            .subject("sub-1")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claim("email", "Valeria@Gmail.com")
            .claim("email_verified", true)
            .claim("name", "Valeria Ríos");
        ajuste.accept(datos);
        var codificador = NimbusJwtEncoder.withKeyPair((RSAPublicKey) firma.getPublic(), (RSAPrivateKey) firma.getPrivate()).build();
        return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), datos.build())).getTokenValue();
    }

    private static void rechaza(String token) {
        assertEquals("TOKEN_GOOGLE_INVALIDO", assertThrows(ErrorNegocio.class, () -> verificador.verificar(token)).getCodigo());
    }

    @Test
    void aceptaUnTokenBuenoYNormalizaElCorreo() {
        IdentidadGoogle identidad = verificador.verificar(token(llaves, d -> { }));
        assertEquals("sub-1", identidad.sub());
        assertEquals("valeria@gmail.com", identidad.correo());
        assertEquals("Valeria Ríos", identidad.nombre());
    }

    /** El constructor de Spring no deja crear este emisor, por eso se firma directamente. */
    @Test
    void aceptaElEmisorSinHttps() throws Exception {
        SignedJWT firmado = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder()
            .issuer("accounts.google.com")
            .audience(CLIENTE)
            .subject("sub-1")
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
            .claim("email", "valeria@gmail.com")
            .claim("email_verified", true)
            .build());
        firmado.sign(new RSASSASigner(llaves.getPrivate()));
        assertEquals("sub-1", verificador.verificar(firmado.serialize()).sub());
    }

    @Test
    void rechazaOtraFirma() throws Exception {
        rechaza(token(par(), d -> { }));
    }

    @Test
    void rechazaOtraAudiencia() {
        rechaza(token(llaves, d -> d.audience(List.of("otra-app.apps.googleusercontent.com"))));
    }

    @Test
    void rechazaOtroEmisor() {
        rechaza(token(llaves, d -> d.issuer("https://accounts.falso.com")));
    }

    @Test
    void rechazaUnTokenVencido() {
        rechaza(token(llaves, d -> d.issuedAt(Instant.now().minusSeconds(7200)).expiresAt(Instant.now().minusSeconds(3600))));
    }

    @Test
    void rechazaUnCorreoSinConfirmar() {
        rechaza(token(llaves, d -> d.claim("email_verified", false)));
    }

    @Test
    void rechazaUnTokenSinCorreo() {
        rechaza(token(llaves, d -> d.claims(c -> c.remove("email"))));
    }

    @Test
    void rechazaBasura() {
        rechaza("no-es-un-jwt");
    }

    @Test
    void sinClienteConfiguradoAvisaQueNoEstaDisponible() {
        var sinCliente = new VerificadorTokenGoogleNimbus("");
        assertEquals("GOOGLE_NO_DISPONIBLE", assertThrows(ErrorNegocio.class, () -> sinCliente.verificar("x")).getCodigo());
    }

    @Test
    void siNoSePuedenLeerLasLlavesNoCulpaAlToken() {
        var sinRed = new VerificadorTokenGoogleNimbus(t -> { throw new JwtException("sin red"); });
        assertEquals("GOOGLE_NO_DISPONIBLE", assertThrows(ErrorNegocio.class, () -> sinRed.verificar("x")).getCodigo());
    }
}
