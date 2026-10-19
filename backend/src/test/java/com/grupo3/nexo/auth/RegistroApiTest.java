package com.grupo3.nexo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.usuarios.EstadoCuenta;
import com.grupo3.nexo.usuarios.EstadoVerificacionTutor;
import com.grupo3.nexo.usuarios.Modalidad;
import com.grupo3.nexo.usuarios.Usuario;
import com.grupo3.nexo.usuarios.UsuarioRepository;

/** POST /api/auth/registro (RF1) con las reglas de D2, contra la base real de pruebas. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ConfiguracionPruebas.class)
@Transactional
class RegistroApiTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired CodigoVerificacionRepository codigos;
    @Autowired PasswordEncoder encriptador;
    @Autowired EnviadorCorreoEnMemoria correos;

    private static String estudiante(String correo, String contrasena) {
        return """
            {"nombre":"Valeria Ríos","correo":"%s","contrasena":"%s","rol":"ESTUDIANTE",
             "documentoIdentidad":"1098765432","aceptaTerminos":true,"versionTerminos":"2026-10-18"}
            """.formatted(correo, contrasena);
    }

    private static String estudiante(String correo) {
        return estudiante(correo, "Clave2026seg");
    }

    private static String tutor(String correo, String datosTutor) {
        return """
            {"nombre":"Diego Ramírez","correo":"%s","contrasena":"Clave2026seg","rol":"TUTOR",
             "documentoIdentidad":"1098000111","aceptaTerminos":true,"versionTerminos":"2026-10-18"%s}
            """.formatted(correo, datosTutor == null ? "" : ",\"datosTutor\":" + datosTutor);
    }

    private ResultActions registrar(String cuerpo) throws Exception {
        return mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    // ---------- registro correcto ----------

    @Test
    void registraUnEstudianteYDevuelveLoQueDiceElContrato() throws Exception {
        registrar(estudiante("valeria@ejemplo.com"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.usuarioId").isNotEmpty())
            .andExpect(jsonPath("$.correo").value("valeria@ejemplo.com"))
            .andExpect(jsonPath("$.estadoCuenta").value("PENDIENTE_VERIFICACION_CORREO"))
            .andExpect(jsonPath("$.codigoVenceEnSegundos").value(900))
            .andExpect(jsonPath("$.contrasena").doesNotExist())
            .andExpect(jsonPath("$.contrasenaHash").doesNotExist());

        Usuario u = usuarios.findByCorreo("valeria@ejemplo.com").orElseThrow();
        assertEquals(EstadoCuenta.PENDIENTE_VERIFICACION_CORREO, u.getEstadoCuenta());
        assertEquals(null, u.getEstadoVerificacionTutor());
        assertEquals("2026-10-18", u.getTerminosVersion());
        assertTrue(u.getTerminosAceptadosEn() != null);
    }

    @Test
    void laContrasenaSeGuardaConBcryptYNuncaEnClaro() throws Exception {
        registrar(estudiante("hash@ejemplo.com", "Clave2026seg")).andExpect(status().isCreated());

        Usuario u = usuarios.findByCorreo("hash@ejemplo.com").orElseThrow();
        assertTrue(u.getContrasenaHash().startsWith("$2"), "debe ser un hash BCrypt");
        assertFalse(u.getContrasenaHash().contains("Clave2026seg"));
        assertTrue(encriptador.matches("Clave2026seg", u.getContrasenaHash()));
    }

    @Test
    void envíaUnCodigoDeSeisDigitosQueSeGuardaCifradoYVenceEnQuinceMinutos() throws Exception {
        registrar(estudiante("codigo@ejemplo.com")).andExpect(status().isCreated());

        String enviado = correos.ultimoCodigo.get("codigo@ejemplo.com");
        assertTrue(enviado != null && enviado.matches("^[0-9]{6}$"), "código enviado: " + enviado);

        Usuario u = usuarios.findByCorreo("codigo@ejemplo.com").orElseThrow();
        List<CodigoVerificacion> guardados = codigos.findByUsuarioIdAndProposito(u.getId(), PropositoCodigo.VERIFICAR_CORREO);
        assertEquals(1, guardados.size());
        CodigoVerificacion c = guardados.get(0);
        assertFalse(c.getCodigoHash().contains(enviado), "no debe guardarse el código en claro");
        assertTrue(encriptador.matches(enviado, c.getCodigoHash()));
        assertEquals(0, c.getIntentos());
        assertEquals(null, c.getCerradoEn());
        long minutos = Duration.between(Instant.now(), c.getVenceEn()).toMinutes();
        assertTrue(minutos >= 14 && minutos <= 15, "vence en " + minutos + " min");
    }

    @Test
    void registraUnTutorPendienteDeVerificacionConSusMateriasYModalidades() throws Exception {
        registrar(tutor("tutor@ejemplo.com",
            "{\"materias\":[\"Cálculo I\",\" Programación \"],\"modalidades\":[\"VIRTUAL\",\"PRESENCIAL\"]}"))
            .andExpect(status().isCreated());

        Usuario u = usuarios.findByCorreo("tutor@ejemplo.com").orElseThrow();
        assertEquals(EstadoVerificacionTutor.PENDIENTE, u.getEstadoVerificacionTutor());
        assertEquals(java.util.Set.of("Cálculo I", "Programación"), u.getMaterias());
        assertEquals(java.util.Set.of(Modalidad.VIRTUAL, Modalidad.PRESENCIAL), u.getModalidades());
    }

    @Test
    void normalizaElCorreoAMinusculasYSinEspacios() throws Exception {
        registrar(estudiante("  ANA.Prueba@Ejemplo.COM "))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.correo").value("ana.prueba@ejemplo.com"));
        assertTrue(usuarios.existsByCorreo("ana.prueba@ejemplo.com"));
    }

    // ---------- reglas de D2 ----------

    @Test
    void rechazaUnCorreoYaRegistradoSinDistinguirMayusculas() throws Exception {
        registrar(estudiante("dup@ejemplo.com")).andExpect(status().isCreated());
        registrar(estudiante("DUP@ejemplo.com"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("CORREO_DUPLICADO"));
    }

    @Test
    void rechazaUnDominioQueNoRecibeCorreo() throws Exception {
        registrar(estudiante("alguien@sin-mx.test"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.codigo").value("CORREO_SIN_MX"));
        assertFalse(usuarios.existsByCorreo("alguien@sin-mx.test"));
    }

    @Test
    void rechazaCorreosTemporalesTambienEnSubdominios() throws Exception {
        for (String correo : List.of("a@mailinator.com", "b@sub.mailinator.com", "c@YOPMAIL.com")) {
            registrar(estudiante(correo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CORREO_DESECHABLE"));
        }
    }

    @Test
    void rechazaContrasenasDebilesConElCodigoDelContrato() throws Exception {
        for (String debil : List.of("abcdefgh", "12345678", "Password123", "mariaperez2026")) {
            String correo = debil.equals("mariaperez2026") ? "mariaperez@ejemplo.com" : "debil@ejemplo.com";
            registrar(estudiante(correo, debil))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CONTRASENA_DEBIL"));
        }
        assertFalse(usuarios.existsByCorreo("debil@ejemplo.com"));
    }

    // ---------- datos inválidos (400) ----------

    @Test
    void exigeAceptarLosTerminos() throws Exception {
        registrar(estudiante("sin@ejemplo.com").replace("\"aceptaTerminos\":true", "\"aceptaTerminos\":false"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
            .andExpect(jsonPath("$.detalles[?(@.campo=='aceptaTerminos')]").exists());
        assertFalse(usuarios.existsByCorreo("sin@ejemplo.com"));
    }

    @Test
    void indicaCadaCampoIncorrecto() throws Exception {
        registrar("""
            {"nombre":"","correo":"no-es-un-correo","contrasena":"corta","rol":"ESTUDIANTE",
             "documentoIdentidad":"","aceptaTerminos":true,"versionTerminos":"2026-10-18"}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
            .andExpect(jsonPath("$.detalles[?(@.campo=='nombre')]").exists())
            .andExpect(jsonPath("$.detalles[?(@.campo=='correo')]").exists())
            .andExpect(jsonPath("$.detalles[?(@.campo=='contrasena')]").exists())
            .andExpect(jsonPath("$.detalles[?(@.campo=='documentoIdentidad')]").exists());
    }

    @Test
    void unTutorDebeEnviarSusDatos() throws Exception {
        registrar(tutor("t1@ejemplo.com", null))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalles[?(@.campo=='datosTutor')]").exists());
        registrar(tutor("t2@ejemplo.com", "{\"materias\":[],\"modalidades\":[\"VIRTUAL\"]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void unEstudianteNoPuedeEnviarDatosDeTutor() throws Exception {
        registrar(estudiante("e@ejemplo.com").replace("}\n", ",\"datosTutor\":{\"materias\":[\"X\"],\"modalidades\":[\"VIRTUAL\"]}}\n"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalles[?(@.campo=='datosTutor')]").exists());
    }

    @Test
    void nadiePuedeRegistrarseComoAdministrador() throws Exception {
        registrar(estudiante("adm@ejemplo.com").replace("\"ESTUDIANTE\"", "\"ADMIN\""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalles[?(@.campo=='rol')]").exists());
        assertFalse(usuarios.existsByCorreo("adm@ejemplo.com"));
    }

    @Test
    void rechazaUnRolInexistenteYUnJsonRoto() throws Exception {
        registrar(estudiante("r@ejemplo.com").replace("\"ESTUDIANTE\"", "\"SUPERUSUARIO\""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        registrar("{esto no es json").andExpect(status().isBadRequest());
    }

    @Test
    void elRegistroEsPublicoPeroNoLoQueExigeSesion() throws Exception {
        registrar(estudiante("publico@ejemplo.com")).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/cerrar-sesion").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }
}
