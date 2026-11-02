package com.grupo3.nexo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

/** POST /api/auth/login (RF2): credenciales, correo sin verificar y bloqueo tras 5 intentos. */
class LoginApiTest extends PruebaAuthBase {

    private static final String CORREO = "login@ejemplo.com";

    @Test
    void conCredencialesCorrectasInicia() throws Exception {
        cuentaActiva(CORREO);
        login(CORREO, CLAVE)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenAcceso").isNotEmpty())
            .andExpect(jsonPath("$.tokenRefresco").isNotEmpty())
            .andExpect(jsonPath("$.usuario.correo").value(CORREO))
            .andExpect(jsonPath("$.usuario.contrasenaHash").doesNotExist())
            .andExpect(jsonPath("$.usuario.contrasena").doesNotExist());
    }

    @Test
    void elCorreoNoDistingueMayusculasNiEspacios() throws Exception {
        cuentaActiva(CORREO);
        login("  LOGIN@Ejemplo.com ", CLAVE).andExpect(status().isOk());
    }

    @Test
    void laContrasenaIncorrectaYElCorreoInexistenteDanLaMismaRespuesta() throws Exception {
        cuentaActiva(CORREO);
        String conCorreoInexistente = login("nadie@ejemplo.com", CLAVE)
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
            .andReturn().getResponse().getContentAsString();
        String conContrasenaMala = login(CORREO, "OtraClave2026")
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
            .andReturn().getResponse().getContentAsString();
        assertEquals(conCorreoInexistente, conContrasenaMala);
    }

    @Test
    void sinVerificarElCorreoNoInicia() throws Exception {
        registrar(CORREO);
        login(CORREO, CLAVE)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("CORREO_NO_VERIFICADO"));
        // con contraseña incorrecta no se revela que la cuenta existe sin verificar
        login(CORREO, "OtraClave2026").andExpect(status().isUnauthorized());
    }

    @Test
    void unaCuentaSuspendidaNoInicia() throws Exception {
        cuentaActiva(CORREO);
        sql("UPDATE usuarios SET estado_cuenta = 'SUSPENDIDA' WHERE correo = ?", CORREO);
        login(CORREO, CLAVE)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("CUENTA_SUSPENDIDA"));
    }

    @Test
    void tras5IntentosFallidosBloqueaLaCuentaAunqueLuegoSeaLaContrasenaCorrecta() throws Exception {
        cuentaActiva(CORREO);
        for (int i = 1; i <= 4; i++) {
            login(CORREO, "Incorrecta" + i).andExpect(status().isUnauthorized());
        }
        login(CORREO, "Incorrecta5")
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"))
            .andExpect(jsonPath("$.detalles[0].campo").value("bloqueadoHasta"));
        login(CORREO, CLAVE)
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"));
    }

    @Test
    void elBloqueoDuraQuinceMinutosYAlTerminarSeReiniciaElContador() throws Exception {
        cuentaActiva(CORREO);
        for (int i = 1; i <= 5; i++) {
            login(CORREO, "Incorrecta" + i);
        }
        em.flush();
        Instant hasta = jdbc.queryForObject("SELECT bloqueado_hasta FROM usuarios WHERE correo = ?",
            java.sql.Timestamp.class, CORREO).toInstant();
        long minutos = Duration.between(Instant.now(), hasta).toMinutes();
        assertEquals(true, minutos >= 14 && minutos <= 15, "bloqueada " + minutos + " min");

        sql("UPDATE usuarios SET bloqueado_hasta = ? WHERE correo = ?",
            java.sql.Timestamp.from(Instant.now().minusSeconds(5)), CORREO);
        login(CORREO, CLAVE).andExpect(status().isOk());
        em.flush();
        assertEquals(0, jdbc.queryForObject("SELECT intentos_fallidos FROM usuarios WHERE correo = ?", Integer.class, CORREO));
    }

    @Test
    void unInicioCorrectoReiniciaLosIntentosFallidos() throws Exception {
        cuentaActiva(CORREO);
        for (int i = 1; i <= 4; i++) {
            login(CORREO, "Incorrecta" + i).andExpect(status().isUnauthorized());
        }
        login(CORREO, CLAVE).andExpect(status().isOk());
        for (int i = 1; i <= 4; i++) {          // vuelve a tener 4 intentos antes de bloquearse
            login(CORREO, "Incorrecta" + i).andExpect(status().isUnauthorized());
        }
        login(CORREO, CLAVE).andExpect(status().isOk());
    }

    @Test
    void rechazaSolicitudesMalFormadas() throws Exception {
        enviar("/api/auth/login", "{\"correo\":\"no-es-correo\",\"contrasena\":\"\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
            .andExpect(jsonPath("$.detalles[?(@.campo=='correo')]").exists())
            .andExpect(jsonPath("$.detalles[?(@.campo=='contrasena')]").exists());
    }
}
