package com.grupo3.nexo.tutorias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import com.jayway.jsonpath.JsonPath;

/**
 * Peticiones simultáneas de verdad, cada una en su hilo y su transacción. Sin la transacción envolvente de las demás
 * pruebas, así que lo confirmado se limpia al terminar cada una.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CarrerasTutoriasTest extends PruebaTutoriasBase {

    @Autowired private DataSource fuente;

    /** Lo confirmado aquí no debe verlo ninguna otra prueba. La guarda impide vaciar algo que no sea el esquema de pruebas. */
    @AfterEach
    void limpiar() {
        assertEquals("pruebas", jdbc.queryForObject("SELECT current_schema()", String.class));
        jdbc.execute("TRUNCATE usuarios CASCADE");
    }

    /** Lanza todas a la vez y devuelve estado HTTP y código de error de cada una. */
    private List<String> aLaVez(List<Callable<MvcResult>> peticiones) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(peticiones.size());
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<MvcResult>> enCurso = new ArrayList<>();
            for (Callable<MvcResult> p : peticiones) {
                enCurso.add(hilos.submit(() -> {
                    salida.await();
                    return p.call();
                }));
            }
            salida.countDown();
            List<String> resultados = new ArrayList<>();
            for (Future<MvcResult> f : enCurso) {
                MvcResult r = f.get();
                int estado = r.getResponse().getStatus();
                resultados.add(estado >= 400
                    ? estado + " " + JsonPath.read(r.getResponse().getContentAsString(), "$.codigo")
                    : String.valueOf(estado));
            }
            return resultados;
        } finally {
            hilos.shutdownNow();
        }
    }

    private static Map<String, Long> contar(List<String> resultados) {
        return resultados.stream().collect(Collectors.groupingBy(r -> r, Collectors.counting()));
    }

    @Test
    void dosAceptacionesALaMismaHoraDejanUnaSolaSesion() throws Exception {
        LocalDate dia = manana();
        Cuenta ana = tutor("Ana Torres", dia, "10:00");
        List<Callable<MvcResult>> aceptaciones = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String id = enviarSolicitud(cuenta("E" + i), ana, dia, "10:00");
            aceptaciones.add(() -> enviarComo(ana.token(), "/api/solicitudes/" + id + "/aceptar", "").andReturn());
        }

        Map<String, Long> resultado = contar(aLaVez(aceptaciones));
        assertEquals(1L, resultado.get("200"), resultado.toString());
        assertEquals(7L, resultado.get("409 HORA_NO_DISPONIBLE"), resultado.toString());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM sesiones WHERE tutor_id = ? AND estado = 'PROGRAMADA'", Integer.class, ana.uuid()));
        // Las que perdieron no quedaron a medias: la transacción completa se deshizo
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM solicitudes WHERE tutor_id = ? AND estado = 'ACEPTADA'", Integer.class, ana.uuid()));
        assertEquals(7, jdbc.queryForObject("SELECT COUNT(*) FROM solicitudes WHERE tutor_id = ? AND estado = 'PENDIENTE'", Integer.class, ana.uuid()));
    }

    @Test
    void aceptarYRechazarLaMismaSolicitudTieneUnSoloGanador() throws Exception {
        LocalDate dia = manana();
        Cuenta ana = tutor("Ana Torres", dia, "10:00");
        Cuenta vale = cuenta("Valeria Ríos");
        String id = enviarSolicitud(vale, ana, dia, "10:00");

        List<String> resultado = aLaVez(List.of(
            () -> enviarComo(ana.token(), "/api/solicitudes/" + id + "/aceptar", "").andReturn(),
            () -> enviarComo(ana.token(), "/api/solicitudes/" + id + "/rechazar", "").andReturn()));
        assertEquals(1, resultado.stream().filter("200"::equals).count(), resultado.toString());
        assertEquals(1, resultado.stream().filter("409 SOLICITUD_NO_PENDIENTE"::equals).count(), resultado.toString());
    }

    @Test
    void cancelarUnaAceptadaConfirmaSolicitudSesionYChatJuntos() throws Exception {
        LocalDate dia = manana();
        Cuenta ana = tutor("Ana Torres", dia, "10:00");
        Cuenta vale = cuenta("Valeria Ríos");
        String aceptada = tutoriaAceptada(vale, ana, dia, "10:00");
        String id = campo(aceptada, "$.solicitud.id");

        enviarComo(vale.token(), "/api/solicitudes/" + id + "/cancelar", "").andExpect(status().isOk());

        // Sin transacción envolvente: aquí sí corren los triggers diferidos, al confirmar de verdad
        assertEquals("CANCELADA", jdbc.queryForObject("SELECT estado FROM sesiones WHERE solicitud_id = ?::uuid", String.class, id));
        assertEquals("CANCELADA", jdbc.queryForObject("SELECT estado FROM solicitudes WHERE id = ?::uuid", String.class, id));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM chats WHERE tutor_id = ? AND activo_hasta IS NOT NULL", Integer.class, ana.uuid()));
    }

    @Test
    void unaPersonaNoQuedaEnDosSesionesCruzandoPapeles() throws Exception {
        LocalDate dia = manana();
        Cuenta ana = tutor("Ana Torres", dia, "10:00");
        Cuenta bruno = tutor("Bruno Silva", dia, "10:00");
        // Ana da una tutoría a las 10:00 y a la vez pide otra a esa misma hora
        String comoTutora = enviarSolicitud(cuenta("Cami Rojas"), ana, dia, "10:00");
        String comoEstudiante = enviarSolicitud(ana, bruno, dia, "10:00");

        Map<String, Long> resultado = contar(aLaVez(List.of(
            () -> enviarComo(ana.token(), "/api/solicitudes/" + comoTutora + "/aceptar", "").andReturn(),
            () -> enviarComo(bruno.token(), "/api/solicitudes/" + comoEstudiante + "/aceptar", "").andReturn())));
        assertEquals(1L, resultado.get("200"), resultado.toString());
        assertEquals(1L, resultado.get("409 HORA_NO_DISPONIBLE"), resultado.toString());
    }

    @Test
    void unEstudianteNoQuedaEnDosSesionesALaMismaHora() throws Exception {
        LocalDate dia = manana();
        Cuenta ana = tutor("Ana Torres", dia, "10:00");
        Cuenta bruno = tutor("Bruno Silva", dia, "10:00");
        Cuenta vale = cuenta("Valeria Ríos");
        String conAna = enviarSolicitud(vale, ana, dia, "10:00");
        String conBruno = enviarSolicitud(vale, bruno, dia, "10:00");

        Map<String, Long> resultado = contar(aLaVez(List.of(
            () -> enviarComo(ana.token(), "/api/solicitudes/" + conAna + "/aceptar", "").andReturn(),
            () -> enviarComo(bruno.token(), "/api/solicitudes/" + conBruno + "/aceptar", "").andReturn())));
        assertEquals(1L, resultado.get("200"), resultado.toString());
        assertEquals(1L, resultado.get("409 HORA_NO_DISPONIBLE"), resultado.toString());
    }

    @Test
    void dosReportesIgualesDejanUnoSolo() throws Exception {
        Cuenta ana = tutor("Ana Torres", manana(), "10:00");
        Cuenta vale = cuenta("Valeria Ríos");
        String ruta = "/api/tutores/" + ana.id() + "/reportes";
        Map<String, Long> resultado = contar(aLaVez(List.of(
            () -> enviarComo(vale.token(), ruta, "{\"motivo\":\"Motivo de prueba\"}").andReturn(),
            () -> enviarComo(vale.token(), ruta, "{\"motivo\":\"Motivo de prueba\"}").andReturn())));
        assertEquals(1L, resultado.get("201"), resultado.toString());
        assertEquals(1L, resultado.get("409 REPORTE_DUPLICADO"), resultado.toString());
    }

    @Test
    void dosRegistrosConElMismoCorreoDejanUnaCuenta() throws Exception {
        String correo = UUID.randomUUID() + "@ejemplo.com";
        String cuerpo = """
            {"nombre":"Valeria Ríos","correo":"%s","contrasena":"%s","aceptaTerminos":true,"versionTerminos":"2026-10-18"}
            """.formatted(correo, CLAVE);
        Callable<MvcResult> registro = () -> mvc.perform(post("/api/auth/registro")
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
        Map<String, Long> resultado = contar(aLaVez(List.of(registro, registro)));
        assertEquals(1L, resultado.get("201"), resultado.toString());
        assertEquals(1L, resultado.get("409 CORREO_DUPLICADO"), resultado.toString());
    }

    @Test
    void unTokenDeRefrescoSoloSirveUnaVezAunqueLlegueDosVeces() throws Exception {
        String sesion = cuentaActiva(UUID.randomUUID() + "@ejemplo.com");
        String cuerpo = "{\"tokenRefresco\":\"%s\"}".formatted(campo(sesion, "$.tokenRefresco"));
        Callable<MvcResult> refresco = () -> mvc.perform(post("/api/auth/refrescar")
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
        List<String> resultado = aLaVez(List.of(refresco, refresco));
        assertEquals(1, resultado.stream().filter("200"::equals).count(), resultado.toString());
    }

    @Test
    void losFallosDeLoginSimultaneosCuentanTodos() throws Exception {
        String correo = UUID.randomUUID() + "@ejemplo.com";
        cuentaActiva(correo);
        List<Callable<MvcResult>> intentos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            intentos.add(() -> login(correo, "ClaveErrada2026").andReturn());
        }
        aLaVez(intentos);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE correo = ? AND bloqueado_hasta IS NOT NULL", Integer.class, correo));
    }

    @Test
    void losIntentosSimultaneosDeUnCodigoCuentanTodos() throws Exception {
        String correo = UUID.randomUUID() + "@ejemplo.com";
        String codigo = registrar(correo);
        String errado = "000000".equals(codigo) ? "111111" : "000000";
        String cuerpo = "{\"correo\":\"%s\",\"codigo\":\"%s\"}".formatted(correo, errado);
        List<Callable<MvcResult>> intentos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            intentos.add(() -> mvc.perform(post("/api/auth/verificar-correo")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn());
        }
        aLaVez(intentos);
        assertEquals(5, jdbc.queryForObject("""
            SELECT c.intentos FROM codigos_verificacion c JOIN usuarios u ON u.id = c.usuario_id WHERE u.correo = ?
            """, Integer.class, correo));
    }

    @Test
    void elMismoCodigoCorrectoEnviadoDosVecesSirveUnaSolaVez() throws Exception {
        String correo = UUID.randomUUID() + "@ejemplo.com";
        String codigo = registrar(correo);
        String cuerpo = "{\"correo\":\"%s\",\"codigo\":\"%s\"}".formatted(correo, codigo);
        Callable<MvcResult> intento = () -> mvc.perform(post("/api/auth/verificar-correo")
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
        List<String> resultado = aLaVez(List.of(intento, intento, intento));
        assertEquals(1, resultado.stream().filter("200"::equals).count(), resultado.toString());
        assertEquals(1, jdbc.queryForObject("""
            SELECT COUNT(*) FROM tokens_refresco t JOIN usuarios u ON u.id = t.usuario_id WHERE u.correo = ?
            """, Integer.class, correo));
    }

    @Test
    void unaEsperaDeBloqueoEnElLoginSeVuelveConflicto() throws Exception {
        String correo = UUID.randomUUID() + "@ejemplo.com";
        cuentaActiva(correo);
        try (Connection otra = fuente.getConnection(); Statement sentencia = otra.createStatement()) {
            otra.setAutoCommit(false);
            sentencia.execute("SELECT 1 FROM usuarios WHERE correo = '" + correo + "' FOR UPDATE");
            login(correo, "ClaveErrada2026")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"));
            otra.rollback();
        }
    }

    @Test
    void entrarLimpiaLosTokensVencidosDeEsaCuenta() throws Exception {
        String correo = UUID.randomUUID() + "@ejemplo.com";
        cuentaActiva(correo);
        jdbc.update("""
            UPDATE tokens_refresco SET vence_en = now() - INTERVAL '1 day'
            WHERE usuario_id = (SELECT id FROM usuarios WHERE correo = ?)
            """, correo);
        login(correo, CLAVE).andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("""
            SELECT COUNT(*) FROM tokens_refresco t JOIN usuarios u ON u.id = t.usuario_id WHERE u.correo = ?
            """, Integer.class, correo));
    }

    @Test
    void unaEsperaDeBloqueoQueVenceSeVuelveConflicto() throws Exception {
        LocalDate dia = manana();
        Cuenta ana = tutor("Ana Torres", dia, "10:00");
        String id = enviarSolicitud(cuenta("Valeria Ríos"), ana, dia, "10:00");
        try (Connection otra = fuente.getConnection(); Statement sentencia = otra.createStatement()) {
            otra.setAutoCommit(false);
            sentencia.execute("SELECT 1 FROM solicitudes WHERE id = '" + id + "' FOR UPDATE");
            enviarComo(ana.token(), "/api/solicitudes/" + id + "/aceptar", "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"));
            otra.rollback();
        }
    }

    @Test
    void elMismoEnvioDosVecesCreaUnaSolaSolicitud() throws Exception {
        LocalDate dia = manana();
        Cuenta ana = tutor("Ana Torres", dia, "10:00");
        Cuenta vale = cuenta("Valeria Ríos");
        List<Callable<MvcResult>> envios = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            envios.add(() -> enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "10:00")).andReturn());
        }
        Map<String, Long> resultado = contar(aLaVez(envios));
        assertEquals(1L, resultado.get("201"), resultado.toString());
        assertEquals(5L, resultado.get("409 SOLICITUD_DUPLICADA"), resultado.toString());
    }

    @Test
    void losMensajesSimultaneosNoDejanHuecosYElReintentoNoDuplica() throws Exception {
        LocalDate dia = manana();
        Cuenta ana = tutor("Ana Torres", dia, "10:00");
        Cuenta vale = cuenta("Valeria Ríos");
        String chat = campo(tutoriaAceptada(vale, ana, dia, "10:00"), "$.chat.id");
        String ruta = "/api/chats/" + chat + "/mensajes";
        String repetido = "{\"texto\":\"Repetido\",\"idCliente\":\"%s\"}".formatted(UUID.randomUUID());

        List<Callable<MvcResult>> envios = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String texto = "{\"texto\":\"m%d\"}".formatted(i);
            Cuenta quien = i % 2 == 0 ? vale : ana;
            envios.add(() -> enviarComo(quien.token(), ruta, texto).andReturn());
            envios.add(() -> enviarComo(vale.token(), ruta, repetido).andReturn());
        }
        List<String> resultado = aLaVez(envios);
        assertTrue(resultado.stream().allMatch("201"::equals), resultado.toString());

        UUID id = UUID.fromString(chat);
        assertEquals(11, jdbc.queryForObject("SELECT COUNT(*) FROM mensajes WHERE chat_id = ?", Integer.class, id));
        assertEquals(List.of(1L, 11L, 11L), jdbc.queryForObject("""
            SELECT MIN(secuencia) AS minimo, MAX(secuencia) AS maximo, COUNT(DISTINCT secuencia) AS distintas
            FROM mensajes WHERE chat_id = ?
            """, (rs, n) -> List.of(rs.getLong("minimo"), rs.getLong("maximo"), rs.getLong("distintas")), id));
        assertEquals(11L, jdbc.queryForObject("SELECT ultima_secuencia FROM chats WHERE id = ?", Long.class, id));
    }

    @Test
    void calificacionesSimultaneasNoPierdenEstrellasNiSeRepiten() throws Exception {
        LocalDate dia = manana();
        Cuenta ana = tutor("Ana Torres", dia, "10:00");
        List<Callable<MvcResult>> envios = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Cuenta e = cuenta("E" + i);
            UUID sesion = sesionTerminada(e, ana);
            envios.add(() -> enviarComo(e.token(), "/api/sesiones/" + sesion + "/calificacion", "{\"estrellas\":4}").andReturn());
        }
        Cuenta vale = cuenta("Valeria Ríos");
        UUID doble = sesionTerminada(vale, ana);
        envios.add(() -> enviarComo(vale.token(), "/api/sesiones/" + doble + "/calificacion", "{\"estrellas\":4}").andReturn());
        envios.add(() -> enviarComo(vale.token(), "/api/sesiones/" + doble + "/calificacion", "{\"estrellas\":4}").andReturn());

        Map<String, Long> resultado = contar(aLaVez(envios));
        assertEquals(7L, resultado.get("201"), resultado.toString());
        assertEquals(1L, resultado.get("409 SESION_YA_CALIFICADA"), resultado.toString());
        assertEquals(7, jdbc.queryForObject("SELECT total_resenas FROM usuarios WHERE id = ?", Integer.class, ana.uuid()));
        assertEquals(28, jdbc.queryForObject("SELECT suma_estrellas FROM usuarios WHERE id = ?", Integer.class, ana.uuid()));
    }

    @Test
    void lasPeticionesQuePonenAlDiaALaVezNoChocan() throws Exception {
        LocalDate dia = manana();
        Cuenta ana = tutor("Ana Torres", dia, "10:00");
        for (int i = 0; i < 5; i++) {
            sesionTerminada(cuenta("E" + i), ana);
        }
        List<Callable<MvcResult>> lecturas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            lecturas.add(() -> pedir("/api/sesiones?modo=TUTOR", ana.token()).andExpect(status().isOk()).andReturn());
        }
        List<String> resultado = aLaVez(lecturas);
        assertTrue(resultado.stream().allMatch("200"::equals), resultado.toString());
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM solicitudes WHERE tutor_id = ? AND estado = 'FINALIZADA'", Integer.class, ana.uuid()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sesiones WHERE tutor_id = ? AND estado = 'PROGRAMADA' AND fin <= now()", Integer.class, ana.uuid()));
    }
}
