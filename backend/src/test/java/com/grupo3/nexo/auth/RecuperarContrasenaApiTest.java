package com.grupo3.nexo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** POST /api/auth/recuperar y /api/auth/restablecer (RF16). */
class RecuperarContrasenaApiTest extends PruebaAuthBase {

    private static final String CORREO = "recupera@ejemplo.com";
    private static final String NUEVA = "OtraClave2026";

    private ResultActions recuperar(String correo) throws Exception {
        return enviar("/api/auth/recuperar", "{\"correo\":\"%s\"}".formatted(correo));
    }

    private ResultActions restablecer(String correo, String codigo, String nueva) throws Exception {
        return enviar("/api/auth/restablecer",
            "{\"correo\":\"%s\",\"codigo\":\"%s\",\"nuevaContrasena\":\"%s\"}".formatted(correo, codigo, nueva));
    }

    /** Pide un código para una cuenta activa y lo devuelve. */
    private String codigoDeRecuperacion() throws Exception {
        recuperar(CORREO).andExpect(status().isNoContent());
        return correos.ultimoCodigo.get(CORREO);
    }

    private int codigosEmitidos(String correo) {
        em.flush();
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM codigos_verificacion c JOIN usuarios u ON u.id = c.usuario_id
            WHERE u.correo = ? AND c.proposito = 'RESTABLECER_CONTRASENA'
            """, Integer.class, correo);
    }

    // ---------- POST /recuperar ----------

    @Test
    void recuperarNoRevelaSiElCorreoExiste() throws Exception {
        cuentaActiva(CORREO);

        var existente = recuperar(CORREO).andExpect(status().isNoContent()).andExpect(content().string("")).andReturn();
        var inexistente = recuperar("nadie@ejemplo.com").andExpect(status().isNoContent()).andExpect(content().string("")).andReturn();

        assertEquals(existente.getResponse().getStatus(), inexistente.getResponse().getStatus());
        assertEquals(existente.getResponse().getContentAsString(), inexistente.getResponse().getContentAsString());
        assertEquals(existente.getResponse().getHeaderNames(), inexistente.getResponse().getHeaderNames());
        assertEquals(1, codigosEmitidos(CORREO));
        assertNull(correos.ultimoCodigo.get("nadie@ejemplo.com"));
    }

    @Test
    void alSuperarLosLimitesTambienResponde204YNoEnviaNada() throws Exception {
        String primero = codigoAlPrimerEnvio();

        // Un 429 aquí delataría que la cuenta existe, porque un correo sin cuenta nunca lo recibiría
        recuperar(CORREO).andExpect(status().isNoContent());
        assertEquals(1, codigosEmitidos(CORREO));
        assertEquals(primero, correos.ultimoCodigo.get(CORREO));

        // Pasada la espera de 60 s sí sale otro
        sql("UPDATE codigos_verificacion SET creado_en = ? WHERE proposito = 'RESTABLECER_CONTRASENA'",
            java.sql.Timestamp.from(Instant.now().minus(Duration.ofSeconds(61))));
        recuperar(CORREO).andExpect(status().isNoContent());
        assertEquals(2, codigosEmitidos(CORREO));
    }

    private String codigoAlPrimerEnvio() throws Exception {
        cuentaActiva(CORREO);
        return codigoDeRecuperacion();
    }

    @Test
    void recuperarAplicaElTopeDeEnviosPorHora() throws Exception {
        cuentaActiva(CORREO);
        for (int i = 0; i < 5; i++) {
            recuperar(CORREO).andExpect(status().isNoContent());
            sql("UPDATE codigos_verificacion SET creado_en = ? WHERE proposito = 'RESTABLECER_CONTRASENA' AND cerrado_en IS NULL",
                java.sql.Timestamp.from(Instant.now().minus(Duration.ofMinutes(2))));
        }
        assertEquals(5, codigosEmitidos(CORREO));
        recuperar(CORREO).andExpect(status().isNoContent());
        assertEquals(5, codigosEmitidos(CORREO));
    }

    @Test
    void recuperarExigeUnCorreoBienEscritoYNoPideToken() throws Exception {
        recuperar("no-es-correo").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        enviar("/api/auth/recuperar", "{}").andExpect(status().isBadRequest());
    }

    @Test
    void elCorreoSeNormaliza() throws Exception {
        cuentaActiva(CORREO);
        recuperar("  RECUPERA@Ejemplo.com ").andExpect(status().isNoContent());
        assertEquals(1, codigosEmitidos(CORREO));
    }

    // ---------- POST /restablecer ----------

    @Test
    void restableceLaContrasenaYLaAnteriorDejaDeServir() throws Exception {
        cuentaActiva(CORREO);
        restablecer(CORREO, codigoDeRecuperacion(), NUEVA).andExpect(status().isNoContent()).andExpect(content().string(""));

        login(CORREO, CLAVE).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        login(CORREO, NUEVA).andExpect(status().isOk());
    }

    @Test
    void restablecerInvalidaLasSesiones() throws Exception {
        String sesion = cuentaActiva(CORREO);
        String otra = login(CORREO, CLAVE).andReturn().getResponse().getContentAsString();

        restablecer(CORREO, codigoDeRecuperacion(), NUEVA).andExpect(status().isNoContent());
        em.flush();

        for (String s : java.util.List.of(sesion, otra)) {
            enviar("/api/auth/refrescar", "{\"tokenRefresco\":\"%s\"}".formatted(campo(s, "$.tokenRefresco")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
        }
        assertEquals(0, jdbc.queryForObject("""
            SELECT COUNT(*) FROM tokens_refresco t JOIN usuarios u ON u.id = t.usuario_id
            WHERE u.correo = ? AND t.revocado_en IS NULL
            """, Integer.class, CORREO));
    }

    @Test
    void elCodigoSoloSirveUnaVez() throws Exception {
        cuentaActiva(CORREO);
        String codigo = codigoDeRecuperacion();
        restablecer(CORREO, codigo, NUEVA).andExpect(status().isNoContent());
        restablecer(CORREO, codigo, "TerceraClave2026")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
        login(CORREO, NUEVA).andExpect(status().isOk());
    }

    @Test
    void uncorreoInexistenteRespondeIgualQueUnCodigoIncorrecto() throws Exception {
        cuentaActiva(CORREO);
        String codigo = codigoDeRecuperacion();
        String otro = codigo.equals("000000") ? "000001" : "000000";

        String conCuenta = restablecer(CORREO, otro, NUEVA)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"))
            .andReturn().getResponse().getContentAsString();
        String sinCuenta = restablecer("nadie@ejemplo.com", otro, NUEVA)
            .andExpect(status().isBadRequest())
            .andReturn().getResponse().getContentAsString();
        assertEquals(conCuenta, sinCuenta);
    }

    @Test
    void unaCuentaSinCodigoActivoRespondeIgualQueUnaInexistente() throws Exception {
        cuentaActiva(CORREO);
        String conCuenta = restablecer(CORREO, "123456", NUEVA).andExpect(status().isBadRequest())
            .andReturn().getResponse().getContentAsString();
        String sinCuenta = restablecer("nadie@ejemplo.com", "123456", NUEVA).andExpect(status().isBadRequest())
            .andReturn().getResponse().getContentAsString();
        assertEquals(conCuenta, sinCuenta);
    }

    @Test
    void elCodigoVencidoNoSirve() throws Exception {
        cuentaActiva(CORREO);
        String codigo = codigoDeRecuperacion();
        sql("UPDATE codigos_verificacion SET vence_en = ? WHERE proposito = 'RESTABLECER_CONTRASENA'",
            java.sql.Timestamp.from(Instant.now().minusSeconds(1)));
        restablecer(CORREO, codigo, NUEVA)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("CODIGO_VENCIDO"));
    }

    @Test
    void alQuintoIntentoFallidoSeAgotaElCodigo() throws Exception {
        cuentaActiva(CORREO);
        String codigo = codigoDeRecuperacion();
        String otro = codigo.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 4; i++) {
            restablecer(CORREO, otro, NUEVA).andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
        }
        restablecer(CORREO, otro, NUEVA)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("CODIGO_INTENTOS_AGOTADOS"));
        restablecer(CORREO, codigo, NUEVA)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
    }

    @Test
    void elCodigoDeVerificarElCorreoNoSirveParaRestablecer() throws Exception {
        String codigoDeVerificacion = registrar(CORREO);
        restablecer(CORREO, codigoDeVerificacion, NUEVA)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
    }

    @Test
    void unaContrasenaDebilNoSeAceptaNiGastaElCodigo() throws Exception {
        cuentaActiva(CORREO);
        String codigo = codigoDeRecuperacion();

        for (String debil : java.util.List.of("soloLetrasAqui", "12345678", "recupera2026")) {
            restablecer(CORREO, codigo, debil)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CONTRASENA_DEBIL"));
        }
        login(CORREO, CLAVE).andExpect(status().isOk());
        restablecer(CORREO, codigo, NUEVA).andExpect(status().isNoContent());
    }

    @Test
    void rechazaCamposConFormatoIncorrecto() throws Exception {
        for (String cuerpo : java.util.List.of(
            "{}",
            "{\"correo\":\"no-es-correo\",\"codigo\":\"123456\",\"nuevaContrasena\":\"%s\"}".formatted(NUEVA),
            "{\"correo\":\"%s\",\"codigo\":\"12345\",\"nuevaContrasena\":\"%s\"}".formatted(CORREO, NUEVA),
            "{\"correo\":\"%s\",\"codigo\":\"abcdef\",\"nuevaContrasena\":\"%s\"}".formatted(CORREO, NUEVA),
            "{\"correo\":\"%s\",\"codigo\":\"123456\",\"nuevaContrasena\":\"corta1\"}".formatted(CORREO),
            "{\"correo\":\"%s\",\"codigo\":\"123456\",\"nuevaContrasena\":\"%s\"}".formatted(CORREO, "a1".repeat(40)))) {
            enviar("/api/auth/restablecer", cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        }
    }

    @Test
    void restablecerLevantaElBloqueoPorIntentosFallidos() throws Exception {
        cuentaActiva(CORREO);
        for (int i = 0; i < 5; i++) {
            login(CORREO, "Equivocada2026");
        }
        login(CORREO, CLAVE).andExpect(status().isLocked());

        restablecer(CORREO, codigoDeRecuperacion(), NUEVA).andExpect(status().isNoContent());
        login(CORREO, NUEVA).andExpect(status().isOk());
    }

    @Test
    void laContrasenaSeGuardaCifrada() throws Exception {
        cuentaActiva(CORREO);
        restablecer(CORREO, codigoDeRecuperacion(), NUEVA).andExpect(status().isNoContent());
        em.flush();
        String hash = jdbc.queryForObject("SELECT contrasena_hash FROM usuarios WHERE correo = ?", String.class, CORREO);
        assertNotNull(hash);
        assertFalse(hash.contains(NUEVA));
    }
}
