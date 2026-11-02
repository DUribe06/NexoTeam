package com.grupo3.nexo.disponibilidad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.grupo3.nexo.auth.PruebaAuthBase;
import com.grupo3.nexo.config.HoraColombia;

/** /api/disponibilidad/yo y /api/tutores/{tutorId}/disponibilidad (RF5, RF7). */
class DisponibilidadApiTest extends PruebaAuthBase {

    private static final String CORREO = "tutor@ejemplo.com";

    private ResultActions pedir(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder p, String token) throws Exception {
        return mvc.perform(p.header("Authorization", "Bearer " + token));
    }

    private ResultActions verPropia(String token) throws Exception {
        return pedir(get("/api/disponibilidad/yo"), token);
    }

    private ResultActions crear(String token) throws Exception {
        return pedir(post("/api/disponibilidad/yo"), token);
    }

    private ResultActions definirCuerpo(String token, String cuerpo) throws Exception {
        return pedir(put("/api/disponibilidad/yo/horas").contentType(MediaType.APPLICATION_JSON).content(cuerpo), token);
    }

    private ResultActions definir(String token, String dia, String hora, String... modalidades) throws Exception {
        String lista = String.join(",", java.util.Arrays.stream(modalidades).map(m -> "\"" + m + "\"").toList());
        return definirCuerpo(token, "{\"dia\":\"%s\",\"hora\":\"%s\",\"modalidades\":[%s]}".formatted(dia, hora, lista));
    }

    private ResultActions verPublica(String token, String tutorId) throws Exception {
        return pedir(get("/api/tutores/" + tutorId + "/disponibilidad"), token);
    }

    private String sesion(String correo) throws Exception {
        return cuentaActiva(correo);
    }

    private String tutor(String correo) throws Exception {
        String s = sesion(correo);
        pedir(put("/api/usuarios/yo/modo").contentType(MediaType.APPLICATION_JSON).content("{\"modo\":\"TUTOR\"}"),
            campo(s, "$.tokenAcceso")).andExpect(status().isOk());
        return s;
    }

    private String tokenTutor() throws Exception {
        return campo(tutor(CORREO), "$.tokenAcceso");
    }

    private String tokenConCalendario() throws Exception {
        String token = tokenTutor();
        crear(token).andExpect(status().isCreated());
        return token;
    }

    // ---------- Autenticación y permisos ----------

    @Test
    void todoExigeToken() throws Exception {
        mvc.perform(get("/api/disponibilidad/yo")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(post("/api/disponibilidad/yo")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/disponibilidad/yo/horas").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tutores/" + UUID.randomUUID() + "/disponibilidad")).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenDeUnaCuentaSuspendidaDejaDeServir() throws Exception {
        String token = tokenConCalendario();
        sql("UPDATE usuarios SET estado_cuenta = 'SUSPENDIDA' WHERE correo = ?", CORREO);
        verPropia(token).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void sinPerfilDeTutorNoHayCalendario() throws Exception {
        String token = campo(sesion("estudiante@ejemplo.com"), "$.tokenAcceso");
        for (ResultActions r : List.of(verPropia(token), crear(token), definir(token, "LUNES", "10:00", "VIRTUAL"))) {
            r.andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("NO_ES_TUTOR"));
        }
    }

    @Test
    void quienVolvioAModoEstudianteConservaSuCalendario() throws Exception {
        String token = tokenConCalendario();
        pedir(put("/api/usuarios/yo/modo").contentType(MediaType.APPLICATION_JSON).content("{\"modo\":\"ESTUDIANTE\"}"), token)
            .andExpect(status().isOk());
        definir(token, "LUNES", "10:00", "VIRTUAL").andExpect(status().isOk());
    }

    // ---------- GET y POST /yo ----------

    @Test
    void sinCalendarioRespondeNoCreadoYVacio() throws Exception {
        verPropia(tokenTutor())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.creado").value(false))
            .andExpect(jsonPath("$.dias").isEmpty());
    }

    @Test
    void creaLosSieteDiasConLasHorasDelCalendarioSinPublicar() throws Exception {
        crear(tokenTutor())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.creado").value(true))
            .andExpect(jsonPath("$.dias[*].dia",
                Matchers.contains("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO")))
            .andExpect(jsonPath("$.dias[6].horas[*].hora",
                Matchers.contains("08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00")))
            .andExpect(jsonPath("$.dias[*].horas[*].modalidades[*]").isEmpty());
    }

    @Test
    void crearEsIdempotenteYNoBorraLoPublicado() throws Exception {
        String token = tokenConCalendario();
        definir(token, "MARTES", "12:00", "PRESENCIAL").andExpect(status().isOk());

        crear(token)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.creado").value(true))
            .andExpect(jsonPath("$.dias[1].horas[2].modalidades[0]").value("PRESENCIAL"));
    }

    // ---------- PUT /yo/horas ----------

    @Test
    void publicaCambiaYDespublicaUnaHora() throws Exception {
        String token = tokenConCalendario();

        definir(token, "LUNES", "10:00", "VIRTUAL", "PRESENCIAL")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dias", Matchers.hasSize(7)))
            .andExpect(jsonPath("$.dias[0].horas[1].hora").value("10:00"))
            .andExpect(jsonPath("$.dias[0].horas[1].modalidades", Matchers.contains("PRESENCIAL", "VIRTUAL")))
            .andExpect(jsonPath("$.dias[0].horas[0].modalidades").isEmpty());
        em.flush();
        em.clear();

        // Cambia una modalidad conservando la otra, sin chocar con la fila que se queda
        definir(token, "LUNES", "10:00", "VIRTUAL")
            .andExpect(jsonPath("$.dias[0].horas[1].modalidades", Matchers.contains("VIRTUAL")));
        em.flush();
        em.clear();
        verPropia(token).andExpect(jsonPath("$.dias[0].horas[1].modalidades", Matchers.contains("VIRTUAL")));

        definir(token, "LUNES", "10:00").andExpect(status().isOk())
            .andExpect(jsonPath("$.dias[0].horas[1].modalidades").isEmpty());
        em.flush();
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM tutor_disponibilidad d JOIN usuarios u ON u.id = d.usuario_id WHERE u.correo = ?",
            Integer.class, CORREO));
    }

    @Test
    void repetirLaMismaHoraNoDuplicaFilas() throws Exception {
        String token = tokenConCalendario();
        definir(token, "LUNES", "10:00", "VIRTUAL").andExpect(status().isOk());
        definir(token, "LUNES", "10:00", "VIRTUAL").andExpect(status().isOk());
        em.flush();
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM tutor_disponibilidad d JOIN usuarios u ON u.id = d.usuario_id WHERE u.correo = ?",
            Integer.class, CORREO));
    }

    @Test
    void sinCalendarioNoSePuedeDefinirUnaHora() throws Exception {
        definir(tokenTutor(), "LUNES", "10:00", "VIRTUAL")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("CALENDARIO_NO_CREADO"));
    }

    @Test
    void unaHoraFueraDelCalendarioDaHoraNoValida() throws Exception {
        String token = tokenConCalendario();
        for (String hora : List.of("07:00", "09:00", "15:00", "19:00", "22:00", "10:30")) {
            definir(token, "LUNES", hora, "VIRTUAL")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("HORA_NO_VALIDA"));
        }
    }

    @Test
    void rechazaCamposConFormatoIncorrecto() throws Exception {
        String token = tokenConCalendario();
        for (String cuerpo : List.of(
            "{}",
            "{\"hora\":\"10:00\",\"modalidades\":[]}",
            "{\"dia\":\"LUNES\",\"modalidades\":[]}",
            "{\"dia\":\"LUNES\",\"hora\":\"10:00\"}",
            "{\"dia\":\"FERIADO\",\"hora\":\"10:00\",\"modalidades\":[]}",
            "{\"dia\":\"LUNES\",\"hora\":\"25:00\",\"modalidades\":[]}",
            "{\"dia\":\"LUNES\",\"hora\":\"8:00\",\"modalidades\":[]}",
            "{\"dia\":\"LUNES\",\"hora\":\"10:00\",\"modalidades\":[\"HOLOGRAMA\"]}",
            "{\"dia\":\"LUNES\",\"hora\":\"10:00\",\"modalidades\":[null]}",
            "{\"dia\":\"LUNES\",\"hora\":\"10:00\",\"modalidades\":[\"VIRTUAL\",\"VIRTUAL\"]}")) {
            definirCuerpo(token, cuerpo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        }
    }

    // ---------- GET /api/tutores/{id}/disponibilidad ----------

    @Test
    void laPublicaDevuelveLosProximosCatorceDiasDesdeManana() throws Exception {
        String s = tutor(CORREO);
        String token = campo(s, "$.tokenAcceso");
        crear(token).andExpect(status().isCreated());
        for (DiaSemana dia : DiaSemana.values()) {
            definir(token, dia.name(), "10:00", "VIRTUAL").andExpect(status().isOk());
        }
        LocalDate hoy = HoraColombia.hoy(java.time.Clock.systemUTC());

        verPublica(token, campo(s, "$.usuario.id"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.hasSize(14)))
            .andExpect(jsonPath("$[0].fecha").value(hoy.plusDays(1).toString()))
            .andExpect(jsonPath("$[13].fecha").value(hoy.plusDays(14).toString()))
            .andExpect(jsonPath("$[0].franjas[0].hora").value("10:00"))
            .andExpect(jsonPath("$[0].franjas[0].modalidades[0]").value("VIRTUAL"));
    }

    @Test
    void laPublicaSoloIncluyeLosDiasConHorasYEnOrden() throws Exception {
        String s = tutor(CORREO);
        String token = campo(s, "$.tokenAcceso");
        crear(token).andExpect(status().isCreated());
        definir(token, "LUNES", "16:00", "VIRTUAL", "PRESENCIAL").andExpect(status().isOk());
        definir(token, "LUNES", "08:00", "VIRTUAL").andExpect(status().isOk());

        String otro = campo(sesion("otro@ejemplo.com"), "$.tokenAcceso");
        verPublica(otro, campo(s, "$.usuario.id"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.hasSize(2)))
            .andExpect(jsonPath("$[0].franjas[*].hora", Matchers.contains("08:00", "16:00")))
            .andExpect(jsonPath("$[0].franjas[1].modalidades", Matchers.contains("PRESENCIAL", "VIRTUAL")));
    }

    @Test
    void sinHorasPublicadasLaPublicaEstaVacia() throws Exception {
        String s = tutor(CORREO);
        verPublica(campo(s, "$.tokenAcceso"), campo(s, "$.usuario.id")).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void laPublicaDaNoEncontradoSiNoEsUnTutor() throws Exception {
        String s = sesion("estudiante@ejemplo.com");
        String token = campo(s, "$.tokenAcceso");
        for (String id : List.of(UUID.randomUUID().toString(), "no-es-un-id", campo(s, "$.usuario.id"))) {
            verPublica(token, id).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("TUTOR_NO_ENCONTRADO"));
        }
    }

    @Test
    void laDisponibilidadEsLaUnicaFuenteDelHorarioPublico() throws Exception {
        String s = tutor(CORREO);
        String token = campo(s, "$.tokenAcceso");
        String id = campo(s, "$.usuario.id");
        crear(token).andExpect(status().isCreated());
        definir(token, "JUEVES", "18:00", "VIRTUAL").andExpect(status().isOk());
        pedir(get("/api/tutores/" + id), token)
            .andExpect(jsonPath("$.horario", Matchers.hasSize(1)))
            .andExpect(jsonPath("$.horario[0].dia").value("JUEVES"))
            .andExpect(jsonPath("$.horario[0].hora").value("18:00"));

        definir(token, "JUEVES", "18:00").andExpect(status().isOk());
        pedir(get("/api/tutores/" + id), token).andExpect(jsonPath("$.horario").isEmpty());
        verPublica(token, id).andExpect(jsonPath("$").isEmpty());

        assertEquals(0, jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = 'tutor_modalidades'", Integer.class));
    }

    // ---------- Esquema ----------

    @Test
    void laBaseExigeLasHorasDelCalendarioYNoRepiteFilas() throws Exception {
        tokenConCalendario();
        em.flush();
        UUID id = jdbc.queryForObject("SELECT id FROM usuarios WHERE correo = ?", UUID.class, CORREO);
        String insertar = "INSERT INTO tutor_disponibilidad (id, usuario_id, dia, hora, modalidad) VALUES (?, ?, ?, CAST(? AS TIME), ?)";

        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update(insertar, UUID.randomUUID(), id, "LUNES", "09:00", "VIRTUAL"));
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update(insertar, UUID.randomUUID(), id, "FERIADO", "10:00", "VIRTUAL"));
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update(insertar, UUID.randomUUID(), id, "LUNES", "10:00", "HOLOGRAMA"));
        jdbc.update(insertar, UUID.randomUUID(), id, "LUNES", "10:00", "VIRTUAL");
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update(insertar, UUID.randomUUID(), id, "LUNES", "10:00", "VIRTUAL"));
    }
}
