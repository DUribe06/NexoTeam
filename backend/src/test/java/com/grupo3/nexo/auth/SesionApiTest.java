package com.grupo3.nexo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/** Tokens de acceso (JWT) y de refresco: refrescar, cerrar sesión y proteger rutas. */
class SesionApiTest extends PruebaAuthBase {

    private static String refresco(String sesion) {
        return campo(sesion, "$.tokenRefresco");
    }

    private static String acceso(String sesion) {
        return campo(sesion, "$.tokenAcceso");
    }

    private String cuerpoRefresco(String token) {
        return "{\"tokenRefresco\":\"%s\"}".formatted(token);
    }

    @Test
    void elTokenDeAccesoEsUnJwtFirmadoConElRolDentro() throws Exception {
        String sesion = cuentaActiva("jwt@ejemplo.com");
        String[] partes = acceso(sesion).split("\\.");
        assertEquals(3, partes.length);
        String carga = new String(java.util.Base64.getUrlDecoder().decode(partes[1]), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(carga.contains("\"rol\":\"ESTUDIANTE\""), carga);
        assertTrue(carga.contains("\"correo\":\"jwt@ejemplo.com\""), carga);
    }

    @Test
    void elTokenDeRefrescoSeGuardaSoloComoHash() throws Exception {
        String sesion = cuentaActiva("hash@ejemplo.com");
        em.flush();
        String guardado = jdbc.queryForObject("SELECT token_hash FROM tokens_refresco", String.class);
        assertEquals(64, guardado.length());
        assertNotEquals(refresco(sesion), guardado);
        assertTrue(!guardado.contains(refresco(sesion)));
    }

    @Test
    void refrescarEntregaUnaSesionNuevaYElTokenViejoDejaDeServir() throws Exception {
        String primera = cuentaActiva("rota@ejemplo.com");
        String segunda = enviar("/api/auth/refrescar", cuerpoRefresco(refresco(primera)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuario.correo").value("rota@ejemplo.com"))
            .andReturn().getResponse().getContentAsString();

        assertNotEquals(refresco(primera), refresco(segunda));
        enviar("/api/auth/refrescar", cuerpoRefresco(refresco(primera)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    @Test
    void reutilizarUnTokenYaUsadoCierraTodasLasSesionesDeLaCuenta() throws Exception {
        String primera = cuentaActiva("robo@ejemplo.com");
        String segunda = enviar("/api/auth/refrescar", cuerpoRefresco(refresco(primera)))
            .andReturn().getResponse().getContentAsString();

        enviar("/api/auth/refrescar", cuerpoRefresco(refresco(primera))).andExpect(status().isUnauthorized());   // reuso
        enviar("/api/auth/refrescar", cuerpoRefresco(refresco(segunda))).andExpect(status().isUnauthorized());   // ya revocado
    }

    @Test
    void unTokenInventadoOVencidoOUnaCuentaSuspendidaNoRefrescan() throws Exception {
        enviar("/api/auth/refrescar", cuerpoRefresco("inventado")).andExpect(status().isUnauthorized());

        String vencida = cuentaActiva("vence@ejemplo.com");
        sql("UPDATE tokens_refresco SET vence_en = ?", java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(5)));
        enviar("/api/auth/refrescar", cuerpoRefresco(refresco(vencida))).andExpect(status().isUnauthorized());

        String suspendida = cuentaActiva("susp@ejemplo.com");
        sql("UPDATE usuarios SET estado_cuenta = 'SUSPENDIDA' WHERE correo = 'susp@ejemplo.com'");
        enviar("/api/auth/refrescar", cuerpoRefresco(refresco(suspendida))).andExpect(status().isUnauthorized());
    }

    @Test
    void cerrarSesionRevocaElTokenDeRefresco() throws Exception {
        String sesion = cuentaActiva("sale@ejemplo.com");
        enviarConToken("/api/auth/cerrar-sesion", cuerpoRefresco(refresco(sesion)), acceso(sesion))
            .andExpect(status().isNoContent());
        enviar("/api/auth/refrescar", cuerpoRefresco(refresco(sesion))).andExpect(status().isUnauthorized());
    }

    @Test
    void cerrarSesionNoPuedeRevocarElTokenDeOtraPersona() throws Exception {
        String ana = cuentaActiva("ana@ejemplo.com");
        String beto = cuentaActiva("beto@ejemplo.com");
        enviarConToken("/api/auth/cerrar-sesion", cuerpoRefresco(refresco(ana)), acceso(beto))
            .andExpect(status().isNoContent());                       // idempotente, pero no hizo nada
        enviar("/api/auth/refrescar", cuerpoRefresco(refresco(ana))).andExpect(status().isOk());
    }

    @Test
    void cerrarSesionExigeUnTokenValido() throws Exception {
        enviar("/api/auth/cerrar-sesion", cuerpoRefresco("x"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        enviarConToken("/api/auth/cerrar-sesion", cuerpoRefresco("x"), "no.es.un-jwt")
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));

        String sesion = cuentaActiva("firma@ejemplo.com");
        String[] p = acceso(sesion).split("\\.");
        String manipulado = p[0] + "." + p[1] + "." + "A".repeat(p[2].length());      // firma alterada
        enviarConToken("/api/auth/cerrar-sesion", cuerpoRefresco("x"), manipulado).andExpect(status().isUnauthorized());
    }

    @Test
    void lasRutasDeAdministradorExigenElRolAdmin() throws Exception {
        String estudiante = cuentaActiva("noadmin@ejemplo.com");
        mvc.perform(get("/api/admin/tutores")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/tutores").header("Authorization", "Bearer " + acceso(estudiante)))
            .andExpect(status().isForbidden());
    }

    @Test
    void elSecretoDelJwtDebeTenerAlMenos32Caracteres() {
        assertThrows(IllegalStateException.class, () -> new JwtConfig("demasiado-corto"));
    }
}
