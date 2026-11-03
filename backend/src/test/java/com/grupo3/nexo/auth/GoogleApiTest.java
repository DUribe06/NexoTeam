package com.grupo3.nexo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class GoogleApiTest extends PruebaAuthBase {

    private static final String RUTA = "/api/auth/google";
    private static final String CORREO = "valeria@gmail.test";
    private static final String TOKEN = "sub-1|" + CORREO + "|Valeria Ríos";

    private ResultActions conTerminos(String token) throws Exception {
        return enviar(RUTA, "{\"tokenId\":\"%s\",\"aceptaTerminos\":true,\"versionTerminos\":\"2026-10-18\"}".formatted(token));
    }

    private ResultActions sinTerminos(String token) throws Exception {
        return enviar(RUTA, "{\"tokenId\":\"%s\"}".formatted(token));
    }

    private String columna(String nombre) {
        em.flush();
        return jdbc.queryForObject("SELECT " + nombre + " FROM usuarios WHERE correo = ?", String.class, CORREO);
    }

    @Test
    void laPrimeraVezPideAceptarLasCondicionesYNoCreaLaCuenta() throws Exception {
        sinTerminos(TOKEN).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("TERMINOS_REQUERIDOS"));
        em.flush();
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE correo = ?", Integer.class, CORREO));
    }

    @Test
    void conLasCondicionesCreaUnaCuentaActivaSinContrasena() throws Exception {
        conTerminos(TOKEN).andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenAcceso").isNotEmpty())
            .andExpect(jsonPath("$.usuario.nombre").value("Valeria Ríos"));
        assertEquals("ACTIVA", columna("estado_cuenta"));
        assertEquals("sub-1", columna("google_sub"));
        assertEquals("2026-10-18", columna("terminos_version"));
        assertNull(columna("contrasena_hash"));
    }

    @Test
    void laSegundaVezEntraSinPedirCondiciones() throws Exception {
        conTerminos(TOKEN).andExpect(status().isOk());
        sinTerminos(TOKEN).andExpect(status().isOk()).andExpect(jsonPath("$.tokenAcceso").isNotEmpty());
    }

    @Test
    void uneLaCuentaConfirmadaConEseCorreoYConservaSuContrasena() throws Exception {
        verificar(CORREO, registrar(CORREO)).andExpect(status().isOk());
        String hash = columna("contrasena_hash");

        sinTerminos(TOKEN).andExpect(status().isOk());

        assertEquals("sub-1", columna("google_sub"));
        assertEquals(hash, columna("contrasena_hash"));
        login(CORREO, CLAVE).andExpect(status().isOk());
    }

    /** Si alguien registró un correo ajeno sin confirmarlo, su contraseña no debe servir cuando entre el verdadero dueño. */
    @Test
    void alTomarUnaCuentaSinConfirmarBorraLaContrasenaAnterior() throws Exception {
        registrar(CORREO);

        sinTerminos(TOKEN).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("TERMINOS_REQUERIDOS"));
        conTerminos(TOKEN).andExpect(status().isOk());

        assertEquals("ACTIVA", columna("estado_cuenta"));
        assertNull(columna("contrasena_hash"));
        login(CORREO, CLAVE).andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaOtraCuentaDeGoogleConElMismoCorreo() throws Exception {
        conTerminos(TOKEN).andExpect(status().isOk());
        sinTerminos("sub-2|" + CORREO + "|Otra").andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("GOOGLE_CUENTA_DISTINTA"));
    }

    @Test
    void unaCuentaSuspendidaNoEntra() throws Exception {
        conTerminos(TOKEN).andExpect(status().isOk());
        em.flush();
        jdbc.update("UPDATE usuarios SET estado_cuenta = 'SUSPENDIDA' WHERE correo = ?", CORREO);
        em.clear();
        sinTerminos(TOKEN).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("CUENTA_SUSPENDIDA"));
    }

    @Test
    void rechazaUnTokenInvalido() throws Exception {
        sinTerminos("no-es-un-token").andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("TOKEN_GOOGLE_INVALIDO"));
    }

    @Test
    void unaCuentaSoloDeGoogleNoEntraConContrasena() throws Exception {
        conTerminos(TOKEN).andExpect(status().isOk());
        login(CORREO, CLAVE).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void puedeCrearseUnaContrasenaConLaRecuperacion() throws Exception {
        conTerminos(TOKEN).andExpect(status().isOk());
        enviar("/api/auth/recuperar", "{\"correo\":\"%s\"}".formatted(CORREO)).andExpect(status().isNoContent());
        enviar("/api/auth/restablecer", "{\"correo\":\"%s\",\"codigo\":\"%s\",\"nuevaContrasena\":\"%s\"}"
            .formatted(CORREO, correos.ultimoCodigo.get(CORREO), CLAVE)).andExpect(status().isNoContent());
        login(CORREO, CLAVE).andExpect(status().isOk());
    }

    @Test
    void eliminarUnaCuentaDeGoogleExigeLaMismaCuentaYLiberaElCorreo() throws Exception {
        String acceso = JsonDe.campo(conTerminos(TOKEN).andExpect(status().isOk()), "$.tokenAcceso");
        String ruta = "/api/usuarios/yo/eliminar";

        enviarConToken(ruta, "{\"tokenGoogle\":\"sub-2|otro@gmail.test|Otra\"}", acceso).andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.codigo").value("GOOGLE_NO_COINCIDE"));
        enviarConToken(ruta, "{}", acceso).andExpect(status().isUnprocessableContent());
        enviarConToken(ruta, "{\"tokenGoogle\":\"%s\"}".formatted(TOKEN), acceso).andExpect(status().isNoContent());

        em.flush();
        em.clear();
        // Sin esto, volver a entrar devolvería la cuenta anonimizada
        conTerminos(TOKEN).andExpect(status().isOk()).andExpect(jsonPath("$.usuario.correo").value(CORREO));
    }
}
