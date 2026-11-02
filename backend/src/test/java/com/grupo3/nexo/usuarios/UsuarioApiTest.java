package com.grupo3.nexo.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.grupo3.nexo.auth.PruebaAuthBase;

/** /api/usuarios/yo: perfil propio, cambio de modo sin verificación y perfil de tutor. */
class UsuarioApiTest extends PruebaAuthBase {

    private static final String CORREO = "valeria@ejemplo.com";

    private String token() throws Exception {
        return campo(cuentaActiva(CORREO), "$.tokenAcceso");
    }

    private ResultActions yo(String token) throws Exception {
        return mvc.perform(get("/api/usuarios/yo").header("Authorization", "Bearer " + token));
    }

    private ResultActions cambiarModo(String token, String modo) throws Exception {
        return mvc.perform(put("/api/usuarios/yo/modo").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"modo\":\"%s\"}".formatted(modo)));
    }

    private ResultActions editar(String token, String cuerpo) throws Exception {
        return mvc.perform(patch("/api/usuarios/yo/perfil-tutor").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions resumen(String token, String consulta) throws Exception {
        return mvc.perform(get("/api/usuarios/yo/resumen" + consulta).header("Authorization", "Bearer " + token));
    }

    private String tutorActivo() throws Exception {
        String token = token();
        cambiarModo(token, "TUTOR").andExpect(status().isOk());
        return token;
    }

    // ---------- GET /yo ----------

    @Test
    void todoExigeToken() throws Exception {
        mvc.perform(get("/api/usuarios/yo")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(get("/api/usuarios/yo/resumen")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/usuarios/yo/modo").contentType(MediaType.APPLICATION_JSON).content("{\"modo\":\"TUTOR\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/usuarios/yo/perfil-tutor").contentType(MediaType.APPLICATION_JSON).content("{\"bio\":\"x\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unaCuentaNuevaEsEstudianteSinPerfilDeTutor() throws Exception {
        yo(token())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.nombre").value("Valeria Ríos"))
            .andExpect(jsonPath("$.correo").value(CORREO))
            .andExpect(jsonPath("$.rol").value("ESTUDIANTE"))
            .andExpect(jsonPath("$.estadoCuenta").value("ACTIVA"))
            .andExpect(jsonPath("$.handle").value("@valeriarios"))
            .andExpect(jsonPath("$.modo").value("ESTUDIANTE"))
            .andExpect(jsonPath("$.perfilTutorActivo").value(false))
            .andExpect(jsonPath("$.materias").isEmpty())
            // Un campo opcional sin valor se omite, no se envía null
            .andExpect(jsonPath("$.bio").doesNotExist())
            .andExpect(jsonPath("$.universidad").doesNotExist())
            .andExpect(jsonPath("$.carrera").doesNotExist())
            .andExpect(jsonPath("$.precioHora").doesNotExist())
            .andExpect(jsonPath("$.contrasenaHash").doesNotExist())
            .andExpect(jsonPath("$.documentoIdentidad").doesNotExist());
    }

    @Test
    void unTokenDeUnaCuentaSuspendidaOBorradaDejaDeServir() throws Exception {
        String token = token();
        sql("UPDATE usuarios SET estado_cuenta = 'SUSPENDIDA' WHERE correo = ?", CORREO);
        yo(token).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));

        sql("DELETE FROM usuarios WHERE correo = ?", CORREO);
        yo(token).andExpect(status().isUnauthorized());
    }

    // ---------- PUT /yo/modo ----------

    @Test
    void cualquieraPasaATutorSinVerificacionYSeCreaSuPerfilVacio() throws Exception {
        cambiarModo(token(), "TUTOR")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.modo").value("TUTOR"))
            .andExpect(jsonPath("$.rol").value("TUTOR"))
            .andExpect(jsonPath("$.perfilTutorActivo").value(true))
            .andExpect(jsonPath("$.materias").isEmpty())
            .andExpect(jsonPath("$.bio").doesNotExist());
    }

    @Test
    void alVolverAEstudianteConservaElPerfilYElMismoTokenSigueSirviendo() throws Exception {
        String token = tutorActivo();
        editar(token, "{\"materias\":[\"SQL\"]}").andExpect(status().isOk());

        cambiarModo(token, "ESTUDIANTE")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.modo").value("ESTUDIANTE"))
            .andExpect(jsonPath("$.perfilTutorActivo").value(true))
            .andExpect(jsonPath("$.materias[0]").value("SQL"));

        cambiarModo(token, "TUTOR").andExpect(jsonPath("$.materias[0]").value("SQL"));
        yo(token).andExpect(jsonPath("$.modo").value("TUTOR"));
    }

    @Test
    void elLoginDevuelveElModoVigente() throws Exception {
        tutorActivo();
        login(CORREO, CLAVE).andExpect(status().isOk()).andExpect(jsonPath("$.usuario.rol").value("TUTOR"));
    }

    @Test
    void nadieSeHaceAdministradorCambiandoDeModo() throws Exception {
        String token = token();
        cambiarModo(token, "ADMIN")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        yo(token).andExpect(jsonPath("$.rol").value("ESTUDIANTE"));
        assertEquals("ESTUDIANTE", jdbc.queryForObject("SELECT rol FROM usuarios WHERE correo = ?", String.class, CORREO));
    }

    @Test
    void rechazaUnModoInexistenteOAusente() throws Exception {
        String token = token();
        cambiarModo(token, "OTRO").andExpect(status().isBadRequest());
        mvc.perform(put("/api/usuarios/yo/modo").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalles[?(@.campo=='modo')]").exists());
    }

    @Test
    void unAdministradorNoPierdeSuRolCambiandoDeModo() throws Exception {
        String token = token();
        sql("UPDATE usuarios SET rol = 'ADMIN' WHERE correo = ?", CORREO);
        cambiarModo(token, "ESTUDIANTE")
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ADMIN_SIN_MODOS"));
        assertEquals("ADMIN", jdbc.queryForObject("SELECT rol FROM usuarios WHERE correo = ?", String.class, CORREO));
    }

    // ---------- PATCH /yo/perfil-tutor ----------

    @Test
    void sinPerfilDeTutorNoSePuedeEditar() throws Exception {
        editar(token(), "{\"bio\":\"Hola\"}")
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("NO_ES_TUTOR"));
    }

    @Test
    void guardaTodoElPerfilDeTutor() throws Exception {
        String token = tutorActivo();
        editar(token, """
            {"bio":"  Explico cálculo.  ","materias":["Cálculo I","Programación"],"precioHora":25000,
             "universidad":"Universidad Industrial de Santander","carrera":"Ingeniería de Sistemas"}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bio").value("Explico cálculo."))
            .andExpect(jsonPath("$.materias", Matchers.contains("Cálculo I", "Programación")))
            .andExpect(jsonPath("$.precioHora").value(25000.0))
            .andExpect(jsonPath("$.universidad").value("Universidad Industrial de Santander"))
            .andExpect(jsonPath("$.carrera").value("Ingeniería de Sistemas"));

        em.flush();
        em.clear();
        yo(token)
            .andExpect(jsonPath("$.precioHora").value(25000.0))
            .andExpect(jsonPath("$.materias", Matchers.contains("Cálculo I", "Programación")));
    }

    @Test
    void soloCambiaLoEnviadoYUnTextoVacioBorraElDato() throws Exception {
        String token = tutorActivo();
        editar(token, "{\"bio\":\"Hola\",\"carrera\":\"Física\",\"precioHora\":1000}").andExpect(status().isOk());

        editar(token, "{\"carrera\":\"  \"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bio").value("Hola"))
            .andExpect(jsonPath("$.precioHora").value(1000.0))
            .andExpect(jsonPath("$.carrera").doesNotExist());
    }

    @Test
    void lasMateriasSeReemplazanConservandoElOrdenYSinRepetidas() throws Exception {
        String token = tutorActivo();
        editar(token, "{\"materias\":[\" Álgebra \",\"Cálculo I\",\"álgebra\",\"CÁLCULO I\",\"Zoología\"]}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.materias", Matchers.contains("Álgebra", "Cálculo I", "Zoología")));
        em.flush();
        em.clear();

        // Mismas materias en otro lugar: en la base cambian de fila sin chocar con la clave (usuario, materia)
        editar(token, "{\"materias\":[\"Zoología\",\"Álgebra\"]}").andExpect(status().isOk());
        em.flush();
        em.clear();
        yo(token).andExpect(jsonPath("$.materias", Matchers.contains("Zoología", "Álgebra")));
    }

    @Test
    void sePuedeEditarDesdeElModoEstudianteSiYaTienePerfil() throws Exception {
        String token = tutorActivo();
        cambiarModo(token, "ESTUDIANTE").andExpect(status().isOk());
        editar(token, "{\"bio\":\"Hola\"}").andExpect(status().isOk()).andExpect(jsonPath("$.modo").value("ESTUDIANTE"));
    }

    @Test
    void rechazaLoQueElContratoNoAdmite() throws Exception {
        String token = tutorActivo();
        for (String cuerpo : java.util.List.of(
            "{}",
            "{\"bio\":null}",
            "{\"bio\":\"%s\"}".formatted("x".repeat(501)),
            "{\"materias\":[]}",
            "{\"materias\":[\"  \"]}",
            "{\"materias\":[\"%s\"]}".formatted("x".repeat(81)),
            "{\"precioHora\":-1}",
            "{\"precioHora\":10000000.01}",
            "{\"precioHora\":100.555}",
            "{\"precioHora\":\"mucho\"}",
            "{\"universidad\":\"%s\"}".formatted("x".repeat(121)),
            "{\"carrera\":\"%s\"}".formatted("x".repeat(121)))) {
            editar(token, cuerpo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        }
        editar(token, "{\"materias\":[" + "\"m\",".repeat(20) + "\"m\"]}").andExpect(status().isBadRequest());
    }

    @Test
    void laTarifaPuedeSerCero() throws Exception {
        editar(tutorActivo(), "{\"precioHora\":0}").andExpect(status().isOk()).andExpect(jsonPath("$.precioHora").value(0.0));
    }

    // ---------- GET /yo/resumen ----------

    @Test
    void elResumenDevuelveCerosHastaQueExistanLasSesiones() throws Exception {
        String token = token();
        for (String consulta : java.util.List.of("", "?modo=ESTUDIANTE", "?modo=TUTOR")) {
            resumen(token, consulta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesiones").value(0))
                .andExpect(jsonPath("$.horas").value(0.0))
                .andExpect(jsonPath("$.porCalificar").value(0))
                .andExpect(jsonPath("$.totalResenas").value(0))
                .andExpect(jsonPath("$.personas").value(0))
                .andExpect(jsonPath("$.materias").isEmpty())
                .andExpect(jsonPath("$.promedioRecibido").doesNotExist());
        }
    }

    @Test
    void elResumenRechazaUnModoQueNoExiste() throws Exception {
        String token = token();
        for (String modo : java.util.List.of("ADMIN", "OTRO")) {
            resumen(token, "?modo=" + modo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.detalles[?(@.campo=='modo')]").exists());
        }
    }
}
