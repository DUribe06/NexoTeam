package com.grupo3.nexo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** POST /api/auth/verificar-correo y /api/auth/reenviar-codigo (D2). */
class VerificarCorreoApiTest extends PruebaAuthBase {

    private static final String CORREO = "verifica@ejemplo.com";

    private UUID idDe(String correo) {
        return jdbc.queryForObject("SELECT id FROM usuarios WHERE correo = ?", UUID.class, correo);
    }

    private int intentosDelCodigo(String correo) {
        em.flush();
        return jdbc.queryForObject("SELECT intentos FROM codigos_verificacion WHERE usuario_id = ? ORDER BY creado_en DESC LIMIT 1",
            Integer.class, idDe(correo));
    }

    @Test
    void conElCodigoCorrectoActivaLaCuentaYAbreLaSesion() throws Exception {
        String codigo = registrar(CORREO);

        verificar(CORREO, codigo)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenAcceso").isNotEmpty())
            .andExpect(jsonPath("$.tokenRefresco").isNotEmpty())
            .andExpect(jsonPath("$.expiraEnSegundos").value(900))
            .andExpect(jsonPath("$.usuario.correo").value(CORREO))
            .andExpect(jsonPath("$.usuario.estadoCuenta").value("ACTIVA"))
            .andExpect(jsonPath("$.usuario.rol").value("ESTUDIANTE"))
            .andExpect(jsonPath("$.usuario.contrasenaHash").doesNotExist())
            .andExpect(jsonPath("$.usuario.estadoVerificacionTutor").doesNotExist());

        em.flush();
        assertEquals("ACTIVA", jdbc.queryForObject("SELECT estado_cuenta FROM usuarios WHERE correo = ?", String.class, CORREO));
        assertTrue(jdbc.queryForObject("SELECT cerrado_en IS NOT NULL FROM codigos_verificacion WHERE usuario_id = ?",
            Boolean.class, idDe(CORREO)));
    }

    @Test
    void elCodigoSoloSirveUnaVez() throws Exception {
        String codigo = registrar(CORREO);
        verificar(CORREO, codigo).andExpect(status().isOk());
        verificar(CORREO, codigo)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("CUENTA_NO_ENCONTRADA"));
    }

    @Test
    void unCodigoIncorrectoSumaUnIntentoYSeGuardaAunqueHayaError() throws Exception {
        String codigo = registrar(CORREO);
        String equivocado = codigo.equals("000000") ? "111111" : "000000";

        verificar(CORREO, equivocado).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
        assertEquals(1, intentosDelCodigo(CORREO));
        verificar(CORREO, codigo).andExpect(status().isOk());
    }

    @Test
    void tras5IntentosFallidosElCodigoSeAgotaAunqueLuegoSeaElCorrecto() throws Exception {
        String codigo = registrar(CORREO);
        String equivocado = codigo.equals("000000") ? "111111" : "000000";

        for (int i = 1; i <= 4; i++) {
            verificar(CORREO, equivocado).andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
        }
        verificar(CORREO, equivocado)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("CODIGO_INTENTOS_AGOTADOS"));
        verificar(CORREO, codigo)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
    }

    @Test
    void unCodigoVencidoSeRechaza() throws Exception {
        String codigo = registrar(CORREO);
        sql("UPDATE codigos_verificacion SET vence_en = ? WHERE usuario_id = ?",
            java.sql.Timestamp.from(Instant.now().minus(Duration.ofMinutes(1))), idDe(CORREO));

        verificar(CORREO, codigo)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("CODIGO_VENCIDO"));
    }

    @Test
    void rechazaCorreosSinCuentaPendienteYCodigosMalFormados() throws Exception {
        verificar("nadie@ejemplo.com", "123456").andExpect(status().isNotFound());
        registrar(CORREO);
        verificar(CORREO, "12345").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        verificar(CORREO, "abcdef").andExpect(status().isBadRequest());
    }

    @Test
    void elCorreoSeNormalizaAlVerificar() throws Exception {
        String codigo = registrar(CORREO);
        verificar("  VERIFICA@Ejemplo.COM ", codigo).andExpect(status().isOk());
    }

    // ---------- reenvío ----------

    @Test
    void reenviarInvalidaElCodigoAnteriorYMandaUnoNuevo() throws Exception {
        String viejo = registrar(CORREO);
        sql("UPDATE codigos_verificacion SET creado_en = ? WHERE usuario_id = ?",
            java.sql.Timestamp.from(Instant.now().minus(Duration.ofMinutes(2))), idDe(CORREO));

        reenviar(CORREO).andExpect(status().isNoContent());
        String nuevo = correos.ultimoCodigo.get(CORREO);

        if (nuevo.equals(viejo)) {
            // 1 en un millón: si coincide, no se puede distinguir; basta comprobar que sigue habiendo un solo código abierto
            em.flush();
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM codigos_verificacion WHERE usuario_id = ? AND cerrado_en IS NULL",
                Integer.class, idDe(CORREO)));
            return;
        }
        verificar(CORREO, viejo).andExpect(status().isBadRequest());
        verificar(CORREO, nuevo).andExpect(status().isOk());
    }

    @Test
    void reenviarAntesDe60SegundosDa429ConRetryAfter() throws Exception {
        registrar(CORREO);
        reenviar(CORREO)
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.codigo").value("DEMASIADAS_SOLICITUDES"))
            .andExpect(header().exists("Retry-After"));
    }

    @Test
    void elTopeEsDe5EnviosPorHora() throws Exception {
        registrar(CORREO);                      // envío 1
        for (int i = 2; i <= 5; i++) {          // envíos 2 a 5
            sql("UPDATE codigos_verificacion SET creado_en = creado_en - INTERVAL '2' MINUTE WHERE usuario_id = ?", idDe(CORREO));
            reenviar(CORREO).andExpect(status().isNoContent());
        }
        sql("UPDATE codigos_verificacion SET creado_en = creado_en - INTERVAL '2' MINUTE WHERE usuario_id = ?", idDe(CORREO));
        reenviar(CORREO)                        // el sexto en menos de una hora
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"));
    }

    @Test
    void reenviarNoRevelaSiLaCuentaExiste() throws Exception {
        reenviar("fantasma@ejemplo.com").andExpect(status().isNoContent());
        assertNull(correos.ultimoCodigo.get("fantasma@ejemplo.com"));
        String codigo = registrar(CORREO);
        verificar(CORREO, codigo).andExpect(status().isOk());
        reenviar(CORREO).andExpect(status().isNoContent());   // ya verificada: tampoco se envía nada
        assertEquals(codigo, correos.ultimoCodigo.get(CORREO), "no debió enviarse un código nuevo");
    }
}
