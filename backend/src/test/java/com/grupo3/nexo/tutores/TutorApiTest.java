package com.grupo3.nexo.tutores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.grupo3.nexo.auth.PruebaAuthBase;
import com.grupo3.nexo.config.HoraColombia;

/** /api/tutores: búsqueda (RF3), perfil y reseñas (RF4) y reportes (RF11). */
class TutorApiTest extends PruebaAuthBase {

    private record Cuenta(String id, String token) {
    }

    private Cuenta yo;
    private int resenasCreadas;

    @BeforeEach
    void iniciarSesion() throws Exception {
        yo = cuenta("yo@ejemplo.com");
    }

    private ResultActions pedir(MockHttpServletRequestBuilder p, String token) throws Exception {
        return mvc.perform(p.header("Authorization", "Bearer " + token));
    }

    private ResultActions enviarJson(MockHttpServletRequestBuilder p, String token, String cuerpo) throws Exception {
        return pedir(p.contentType(MediaType.APPLICATION_JSON).content(cuerpo), token);
    }

    private Cuenta cuenta(String correo) throws Exception {
        String sesion = cuentaActiva(correo);
        return new Cuenta(campo(sesion, "$.usuario.id"), campo(sesion, "$.tokenAcceso"));
    }

    /** Una cuenta con perfil de tutor, sin materias ni calendario. */
    private Cuenta tutorSinNada(String correo, String nombre) throws Exception {
        Cuenta c = cuenta(correo);
        enviarJson(put("/api/usuarios/yo/modo"), c.token(), "{\"modo\":\"TUTOR\"}").andExpect(status().isOk());
        sql("UPDATE usuarios SET nombre = ? WHERE correo = ?", nombre, correo);
        return c;
    }

    private void materias(Cuenta c, String... materias) throws Exception {
        String lista = String.join(",", java.util.Arrays.stream(materias).map(m -> "\"" + m + "\"").toList());
        enviarJson(patch("/api/usuarios/yo/perfil-tutor"), c.token(), "{\"materias\":[%s]}".formatted(lista)).andExpect(status().isOk());
    }

    private void hora(Cuenta c, String dia, String hora, String... modalidades) throws Exception {
        pedir(post("/api/disponibilidad/yo"), c.token()).andExpect(status().is2xxSuccessful());
        String lista = String.join(",", java.util.Arrays.stream(modalidades).map(m -> "\"" + m + "\"").toList());
        enviarJson(put("/api/disponibilidad/yo/horas"), c.token(),
            "{\"dia\":\"%s\",\"hora\":\"%s\",\"modalidades\":[%s]}".formatted(dia, hora, lista)).andExpect(status().isOk());
    }

    /** Un tutor que ya sale en la búsqueda. */
    private Cuenta tutor(String correo, String nombre, String materia) throws Exception {
        Cuenta c = tutorSinNada(correo, nombre);
        materias(c, materia);
        hora(c, "LUNES", "10:00", "VIRTUAL");
        return c;
    }

    /** Toda reseña sale de una sesión completada; a mediodía para que la fecha sea esa en hora de Colombia. */
    private void resena(Cuenta tutor, Cuenta autor, int estrellas, String texto, LocalDate fecha) {
        em.flush();
        Instant inicio = fecha.minusDays(1 + resenasCreadas++).atTime(10, 0).atZone(HoraColombia.ZONA).toInstant();
        UUID estudiante = UUID.fromString(autor.id());
        UUID profesor = UUID.fromString(tutor.id());
        UUID solicitud = UUID.randomUUID();
        UUID sesion = UUID.randomUUID();
        Instant enviada = inicio.minus(3, ChronoUnit.DAYS);
        jdbc.update("INSERT INTO chats (id, estudiante_id, tutor_id) VALUES (?, ?, ?) ON CONFLICT (estudiante_id, tutor_id) DO NOTHING",
            UUID.randomUUID(), estudiante, profesor);
        jdbc.update("""
            INSERT INTO solicitudes (id, estudiante_id, tutor_id, materia, tema, inicio, modalidad, estado, enviada_en, vence_en)
            VALUES (?, ?, ?, 'M', 'T', ?, 'VIRTUAL', 'FINALIZADA', ?, ?)
            """, solicitud, estudiante, profesor, Timestamp.from(inicio), Timestamp.from(enviada), Timestamp.from(enviada.plus(1, ChronoUnit.DAYS)));
        jdbc.update("""
            INSERT INTO sesiones (id, solicitud_id, estudiante_id, tutor_id, inicio, fin, estado)
            VALUES (?, ?, ?, ?, ?, ?, 'COMPLETADA')
            """, sesion, solicitud, estudiante, profesor, Timestamp.from(inicio), Timestamp.from(inicio.plus(1, ChronoUnit.HOURS)));
        jdbc.update("""
            INSERT INTO calificaciones (id, sesion_id, estudiante_id, tutor_id, sentido, estrellas, comentario, creado_en)
            VALUES (?, ?, ?, ?, 'ESTUDIANTE_A_TUTOR', ?, ?, ?)
            """, UUID.randomUUID(), sesion, estudiante, profesor, estrellas, texto.isEmpty() ? null : texto,
            Timestamp.from(fecha.atTime(12, 0).atZone(HoraColombia.ZONA).toInstant()));
        em.clear();
    }

    private ResultActions buscar(String consulta) throws Exception {
        return pedir(get("/api/tutores" + consulta), yo.token());
    }

    private void debenSalir(String consulta, String... nombres) throws Exception {
        buscar(consulta).andExpect(status().isOk())
            .andExpect(jsonPath("$.elementos[*].nombre", Matchers.containsInAnyOrder(nombres)));
    }

    private void noDebeSalirNadie(String consulta) throws Exception {
        buscar(consulta).andExpect(status().isOk())
            .andExpect(jsonPath("$.elementos").isEmpty())
            .andExpect(jsonPath("$.totalElementos").value(0))
            .andExpect(jsonPath("$.totalPaginas").value(0));
    }

    private ResultActions reportar(String token, String tutorId, String cuerpo) throws Exception {
        return enviarJson(post("/api/tutores/" + tutorId + "/reportes"), token, cuerpo);
    }

    // ---------- Autenticación ----------

    @Test
    void todoExigeToken() throws Exception {
        String id = UUID.randomUUID().toString();
        for (var peticion : List.of(get("/api/tutores"), get("/api/tutores/" + id), get("/api/tutores/" + id + "/resenas"))) {
            mvc.perform(peticion).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        }
        mvc.perform(post("/api/tutores/" + id + "/reportes").contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"abc\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenDeUnaCuentaSuspendidaDejaDeServir() throws Exception {
        sql("UPDATE usuarios SET estado_cuenta = 'SUSPENDIDA' WHERE correo = ?", "yo@ejemplo.com");
        buscar("").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    // ---------- Quién sale en la búsqueda ----------

    @Test
    void unTutorConMateriaYHoraPublicadaSaleConTodosSusDatos() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        enviarJson(patch("/api/usuarios/yo/perfil-tutor"), ana.token(),
            "{\"bio\":\"Explico\",\"universidad\":\"UIS\",\"precioHora\":25000}").andExpect(status().isOk());

        buscar("")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pagina").value(0))
            .andExpect(jsonPath("$.tamano").value(20))
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.totalPaginas").value(1))
            .andExpect(jsonPath("$.elementos[0].id").value(ana.id()))
            .andExpect(jsonPath("$.elementos[0].nombre").value("Ana Torres"))
            .andExpect(jsonPath("$.elementos[0].handle").isNotEmpty())
            .andExpect(jsonPath("$.elementos[0].bio").value("Explico"))
            .andExpect(jsonPath("$.elementos[0].universidad").value("UIS"))
            .andExpect(jsonPath("$.elementos[0].materias[0]").value("Cálculo I"))
            .andExpect(jsonPath("$.elementos[0].calificacionPromedio").value(0.0))
            .andExpect(jsonPath("$.elementos[0].totalResenas").value(0))
            .andExpect(jsonPath("$.elementos[0].sesionesTotales").value(0))
            .andExpect(jsonPath("$.elementos[0].estudiantes").value(0))
            .andExpect(jsonPath("$.elementos[0].precioHora").value(25000.0))
            .andExpect(jsonPath("$.elementos[0].enLinea").isBoolean())
            .andExpect(jsonPath("$.elementos[0].horario[0].dia").value("LUNES"))
            .andExpect(jsonPath("$.elementos[0].horario[0].hora").value("10:00"))
            .andExpect(jsonPath("$.elementos[0].horario[0].modalidades[0]").value("VIRTUAL"))
            .andExpect(jsonPath("$.elementos[0].correo").doesNotExist())
            .andExpect(jsonPath("$.elementos[0].contrasenaHash").doesNotExist());
    }

    @Test
    void unTutorSinHoraPublicadaNoSale() throws Exception {
        Cuenta ana = tutorSinNada("ana@ejemplo.com", "Ana Torres");
        materias(ana, "Cálculo I");
        noDebeSalirNadie("");

        // Con el calendario creado pero sin publicar nada, tampoco
        pedir(post("/api/disponibilidad/yo"), ana.token()).andExpect(status().isCreated());
        noDebeSalirNadie("");

        hora(ana, "LUNES", "10:00", "VIRTUAL");
        debenSalir("", "Ana Torres");

        // Al despublicar su única hora deja de salir
        hora(ana, "LUNES", "10:00");
        noDebeSalirNadie("");
    }

    @Test
    void unTutorSinMateriasNoSale() throws Exception {
        hora(tutorSinNada("ana@ejemplo.com", "Ana Torres"), "LUNES", "10:00", "VIRTUAL");
        noDebeSalirNadie("");
    }

    @Test
    void unEstudianteSinPerfilDeTutorNoSale() throws Exception {
        cuenta("estudiante@ejemplo.com");
        noDebeSalirNadie("");
    }

    @Test
    void nuncaSaleLaPropiaCuenta() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        pedir(get("/api/tutores"), ana.token()).andExpect(status().isOk()).andExpect(jsonPath("$.elementos").isEmpty());
        debenSalir("", "Ana Torres");
    }

    @Test
    void unaCuentaSuspendidaNoSale() throws Exception {
        tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        sql("UPDATE usuarios SET estado_cuenta = 'SUSPENDIDA' WHERE correo = ?", "ana@ejemplo.com");
        noDebeSalirNadie("");
    }

    @Test
    void quienVolvioAModoEstudianteConservaSuPerfilYSigueSaliendo() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        enviarJson(put("/api/usuarios/yo/modo"), ana.token(), "{\"modo\":\"ESTUDIANTE\"}").andExpect(status().isOk());
        debenSalir("", "Ana Torres");
    }

    // ---------- Filtros ----------

    @Test
    void filtraPorMateriaSinDistinguirTildesNiMayusculasYBastaConUna() throws Exception {
        tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        tutor("luz@ejemplo.com", "Luz Díaz", "Física I");
        tutor("eva@ejemplo.com", "Eva Mora", "Inglés");

        debenSalir("?materia=calculo", "Ana Torres");
        debenSalir("?materia=CÁLCULO", "Ana Torres");
        debenSalir("?materia=Calculo&materia=Fisica", "Ana Torres", "Luz Díaz");
        noDebeSalirNadie("?materia=Química");
    }

    @Test
    void elAreaSeTraduceASusPalabrasClaveYSeSumaAMateria() throws Exception {
        tutor("ana@ejemplo.com", "Ana Torres", "Álgebra Lineal");
        tutor("luz@ejemplo.com", "Luz Díaz", "Matemática Básica");
        tutor("pab@ejemplo.com", "Pablo Rey", "Diseño UI/UX");
        tutor("eva@ejemplo.com", "Eva Mora", "Inglés");

        debenSalir("?area=MATEMATICAS", "Ana Torres", "Luz Díaz");
        debenSalir("?area=DISENO", "Pablo Rey");
        debenSalir("?area=DISENO&materia=Inglés", "Pablo Rey", "Eva Mora");
        noDebeSalirNadie("?area=NEGOCIOS");
    }

    @Test
    void modalidadYFranjaSeEvaluanSobreLaMismaHora() throws Exception {
        // Virtual en la mañana y presencial en la noche: no tiene ninguna hora virtual nocturna
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        hora(ana, "MARTES", "20:00", "PRESENCIAL");
        // Esta sí
        Cuenta luz = tutor("luz@ejemplo.com", "Luz Díaz", "Física I");
        hora(luz, "LUNES", "10:00");
        hora(luz, "JUEVES", "18:00", "VIRTUAL");

        debenSalir("?modalidad=VIRTUAL&franja=NOCHE", "Luz Díaz");
        debenSalir("?modalidad=VIRTUAL", "Ana Torres", "Luz Díaz");
        debenSalir("?modalidad=PRESENCIAL", "Ana Torres");
        debenSalir("?franja=NOCHE", "Ana Torres", "Luz Díaz");
        debenSalir("?modalidad=VIRTUAL&franja=MANANA", "Ana Torres");
        noDebeSalirNadie("?modalidad=PRESENCIAL&franja=MANANA");
    }

    @Test
    void lasFranjasCubrenSusLimites() throws Exception {
        hora(tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I"), "LUNES", "10:00");
        Cuenta luz = tutorSinNada("luz@ejemplo.com", "Luz Díaz");
        materias(luz, "Física I");
        hora(luz, "LUNES", "12:00", "VIRTUAL");
        Cuenta eva = tutorSinNada("eva@ejemplo.com", "Eva Mora");
        materias(eva, "Inglés");
        hora(eva, "LUNES", "18:00", "VIRTUAL");

        noDebeSalirNadie("?franja=MANANA");
        debenSalir("?franja=TARDE", "Luz Díaz");
        debenSalir("?franja=NOCHE", "Eva Mora");
    }

    @Test
    void elTextoBuscaEnNombreMateriasYUniversidad() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Núñez", "Cálculo I");
        enviarJson(patch("/api/usuarios/yo/perfil-tutor"), ana.token(), "{\"universidad\":\"Universidad de Santander\"}").andExpect(status().isOk());
        tutor("luz@ejemplo.com", "Luz Díaz", "Física I");

        debenSalir("?q=ana", "Ana Núñez");
        debenSalir("?q=NUNEZ", "Ana Núñez");
        debenSalir("?q=Núñez", "Ana Núñez");
        debenSalir("?q=calculo", "Ana Núñez");
        debenSalir("?q=santander", "Ana Núñez");
        debenSalir("?q=  fisica  ", "Luz Díaz");
        noDebeSalirNadie("?q=zoologia");
    }

    @Test
    void losComodinesDelTextoSeTomanLiteralmente() throws Exception {
        tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        noDebeSalirNadie("?q=%");
        noDebeSalirNadie("?q=_");
        noDebeSalirNadie("?q=!");
        noDebeSalirNadie("?materia=%");
        noDebeSalirNadie("?q=' OR '1'='1");
    }

    @Test
    void unTextoOMateriaEnBlancoNoFiltra() throws Exception {
        tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        debenSalir("?q=", "Ana Torres");
        debenSalir("?q=  ", "Ana Torres");
        debenSalir("?materia=", "Ana Torres");
    }

    @Test
    void losFiltrosSeCombinanConY() throws Exception {
        tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        tutor("luz@ejemplo.com", "Luz Torres", "Física I");
        debenSalir("?q=torres&materia=fisica&modalidad=VIRTUAL", "Luz Torres");
        noDebeSalirNadie("?q=torres&materia=fisica&modalidad=PRESENCIAL");
    }

    @Test
    void rechazaFiltrosInvalidos() throws Exception {
        for (String consulta : List.of("?modalidad=HOLOGRAMA", "?franja=MADRUGADA", "?area=COCINA", "?orden=PRECIO", "?enLinea=quizas",
            "?pagina=-1", "?pagina=abc", "?tamano=0", "?tamano=51", "?pagina=2147483647&tamano=50",
            "?q=" + "x".repeat(101), "?materia=" + "x".repeat(81))) {
            buscar(consulta).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        }
        buscar("?q=" + "x".repeat(100)).andExpect(status().isOk());
        buscar("?orden=CALIFICACION&tamano=50").andExpect(status().isOk());
    }

    // ---------- Calificación y orden ----------

    @Test
    void elPromedioYElTotalSeCalculanDesdeLasResenasYOrdenanLaLista() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        Cuenta luz = tutor("luz@ejemplo.com", "Luz Díaz", "Física I");
        tutor("eva@ejemplo.com", "Eva Mora", "Inglés");
        Cuenta paz = tutor("paz@ejemplo.com", "Paz Ruiz", "Química");
        LocalDate hoy = LocalDate.now();
        resena(ana, yo, 5, "a", hoy);
        resena(ana, luz, 4, "b", hoy);
        resena(ana, paz, 5, "c", hoy);
        resena(luz, yo, 5, "d", hoy);
        resena(paz, yo, 5, "e", hoy);
        resena(paz, ana, 5, "f", hoy);

        buscar("?orden=CALIFICACION")
            .andExpect(status().isOk())
            // Paz y Luz empatan en 5.0: va primero quien tiene más reseñas. Sin reseñas, al final.
            .andExpect(jsonPath("$.elementos[*].nombre", Matchers.contains("Paz Ruiz", "Luz Díaz", "Ana Torres", "Eva Mora")))
            .andExpect(jsonPath("$.elementos[0].calificacionPromedio").value(5.0))
            .andExpect(jsonPath("$.elementos[0].totalResenas").value(2))
            .andExpect(jsonPath("$.elementos[2].calificacionPromedio").value(4.7))
            .andExpect(jsonPath("$.elementos[2].totalResenas").value(3))
            .andExpect(jsonPath("$.elementos[3].calificacionPromedio").value(0.0))
            .andExpect(jsonPath("$.elementos[3].totalResenas").value(0));

        // Sale del cálculo: una reseña nueva lo cambia sin tocar nada más
        resena(luz, ana, 1, "g", hoy);
        pedir(get("/api/tutores/" + luz.id()), yo.token())
            .andExpect(jsonPath("$.calificacionPromedio").value(3.0))
            .andExpect(jsonPath("$.totalResenas").value(2));
    }

    @Test
    void sinDatosLosTextosVanVaciosYElPrecioEnCero() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        pedir(get("/api/tutores/" + ana.id()), yo.token())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bio").value(""))
            .andExpect(jsonPath("$.universidad").value(""))
            .andExpect(jsonPath("$.precioHora").value(0.0));
    }

    @Test
    void pagina() throws Exception {
        tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        tutor("luz@ejemplo.com", "Luz Díaz", "Física I");
        tutor("eva@ejemplo.com", "Eva Mora", "Inglés");

        buscar("?tamano=2")
            .andExpect(jsonPath("$.tamano").value(2))
            .andExpect(jsonPath("$.totalElementos").value(3))
            .andExpect(jsonPath("$.totalPaginas").value(2))
            .andExpect(jsonPath("$.elementos[*].nombre", Matchers.contains("Ana Torres", "Eva Mora")));
        buscar("?tamano=2&pagina=1")
            .andExpect(jsonPath("$.pagina").value(1))
            .andExpect(jsonPath("$.elementos[*].nombre", Matchers.contains("Luz Díaz")));
        buscar("?tamano=2&pagina=9").andExpect(status().isOk()).andExpect(jsonPath("$.elementos").isEmpty())
            .andExpect(jsonPath("$.totalElementos").value(3));
    }

    // ---------- enLinea ----------

    @Test
    void usarLaApiRegistraLaActividadYSoloEscribeUnaVezPorMinuto() throws Exception {
        sql("UPDATE usuarios SET ultima_actividad_en = NULL WHERE id = ?", UUID.fromString(yo.id()));
        buscar("").andExpect(status().isOk());
        em.flush();
        Timestamp primera = jdbc.queryForObject("SELECT ultima_actividad_en FROM usuarios WHERE id = ?", Timestamp.class, UUID.fromString(yo.id()));
        assertNotNull(primera);

        buscar("").andExpect(status().isOk());
        assertEquals(primera, jdbc.queryForObject("SELECT ultima_actividad_en FROM usuarios WHERE id = ?", Timestamp.class, UUID.fromString(yo.id())));

        Timestamp hace = Timestamp.from(Instant.now().minus(Duration.ofMinutes(2)));
        sql("UPDATE usuarios SET ultima_actividad_en = ? WHERE id = ?", hace, UUID.fromString(yo.id()));
        buscar("").andExpect(status().isOk());
        assertTrue(jdbc.queryForObject("SELECT ultima_actividad_en FROM usuarios WHERE id = ?", Timestamp.class, UUID.fromString(yo.id())).after(hace));
    }

    @Test
    void unaPeticionSinTokenNoRegistraActividad() throws Exception {
        tutorSinNada("ana@ejemplo.com", "Ana Torres");
        sql("UPDATE usuarios SET ultima_actividad_en = NULL WHERE correo = ?", "ana@ejemplo.com");
        login("ana@ejemplo.com", CLAVE).andExpect(status().isOk());
        em.flush();
        assertNull(jdbc.queryForObject("SELECT ultima_actividad_en FROM usuarios WHERE correo = ?", Timestamp.class, "ana@ejemplo.com"));
    }

    @Test
    void estaEnLineaSiUsoLaApiEnLosUltimosCincoMinutos() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        Cuenta luz = tutor("luz@ejemplo.com", "Luz Díaz", "Física I");
        Cuenta eva = tutor("eva@ejemplo.com", "Eva Mora", "Inglés");
        sql("UPDATE usuarios SET ultima_actividad_en = ? WHERE id = ?", Timestamp.from(Instant.now().minus(Duration.ofMinutes(4))), UUID.fromString(ana.id()));
        sql("UPDATE usuarios SET ultima_actividad_en = ? WHERE id = ?", Timestamp.from(Instant.now().minus(Duration.ofMinutes(6))), UUID.fromString(luz.id()));
        sql("UPDATE usuarios SET ultima_actividad_en = NULL WHERE id = ?", UUID.fromString(eva.id()));

        pedir(get("/api/tutores/" + ana.id()), yo.token()).andExpect(jsonPath("$.enLinea").value(true));
        pedir(get("/api/tutores/" + luz.id()), yo.token()).andExpect(jsonPath("$.enLinea").value(false));
        pedir(get("/api/tutores/" + eva.id()), yo.token()).andExpect(jsonPath("$.enLinea").value(false));

        debenSalir("?enLinea=true", "Ana Torres");
        debenSalir("?enLinea=false", "Ana Torres", "Luz Díaz", "Eva Mora");
        debenSalir("", "Ana Torres", "Luz Díaz", "Eva Mora");
    }

    // ---------- GET /{tutorId} ----------

    @Test
    void elDetalleTraeElHorarioEnOrdenDeCalendario() throws Exception {
        Cuenta ana = tutorSinNada("ana@ejemplo.com", "Ana Torres");
        materias(ana, "Cálculo I", "Álgebra");
        hora(ana, "SABADO", "08:00", "PRESENCIAL");
        hora(ana, "LUNES", "16:00", "VIRTUAL", "PRESENCIAL");
        hora(ana, "LUNES", "08:00", "VIRTUAL");

        pedir(get("/api/tutores/" + ana.id()), yo.token())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.materias", Matchers.contains("Cálculo I", "Álgebra")))
            .andExpect(jsonPath("$.horario[*].dia", Matchers.contains("LUNES", "LUNES", "SABADO")))
            .andExpect(jsonPath("$.horario[*].hora", Matchers.contains("08:00", "16:00", "08:00")))
            .andExpect(jsonPath("$.horario[1].modalidades", Matchers.contains("PRESENCIAL", "VIRTUAL")));
    }

    @Test
    void elDetalleMuestraTambienAUnTutorQueAunNoSaleEnLaBusqueda() throws Exception {
        Cuenta ana = tutorSinNada("ana@ejemplo.com", "Ana Torres");
        pedir(get("/api/tutores/" + ana.id()), ana.token())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.materias").isEmpty())
            .andExpect(jsonPath("$.horario").isEmpty());
    }

    @Test
    void elDetalleDaNoEncontradoSiNoEsUnTutorVisible() throws Exception {
        Cuenta suspendido = tutor("sus@ejemplo.com", "Sus Pendido", "Cálculo I");
        sql("UPDATE usuarios SET estado_cuenta = 'SUSPENDIDA' WHERE correo = ?", "sus@ejemplo.com");
        for (String id : List.of(UUID.randomUUID().toString(), "no-es-un-id", yo.id(), suspendido.id())) {
            pedir(get("/api/tutores/" + id), yo.token())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("TUTOR_NO_ENCONTRADO"));
        }
    }

    // ---------- GET /{tutorId}/resenas ----------

    @Test
    void lasResenasVanDeLaMasRecienteALaMasAntiguaConSuAutor() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        LocalDate hoy = LocalDate.of(2026, 11, 2);
        resena(ana, yo, 4, "Antigua", hoy.minusDays(30));
        resena(ana, yo, 5, "Reciente", hoy);
        resena(ana, yo, 3, "", hoy.minusDays(7));

        pedir(get("/api/tutores/" + ana.id() + "/resenas"), yo.token())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(3))
            .andExpect(jsonPath("$.totalPaginas").value(1))
            .andExpect(jsonPath("$.elementos[*].texto", Matchers.contains("Reciente", "", "Antigua")))
            .andExpect(jsonPath("$.elementos[0].id").isNotEmpty())
            .andExpect(jsonPath("$.elementos[0].tutorId").value(ana.id()))
            .andExpect(jsonPath("$.elementos[0].autor").value("Valeria Ríos"))
            .andExpect(jsonPath("$.elementos[0].handleAutor").isNotEmpty())
            .andExpect(jsonPath("$.elementos[0].estrellas").value(5))
            .andExpect(jsonPath("$.elementos[0].fecha").value("2026-11-02"));
    }

    @Test
    void lasResenasSePaginan() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        LocalDate hoy = LocalDate.of(2026, 11, 2);
        resena(ana, yo, 5, "uno", hoy);
        resena(ana, yo, 5, "dos", hoy.minusDays(1));
        resena(ana, yo, 5, "tres", hoy.minusDays(2));

        pedir(get("/api/tutores/" + ana.id() + "/resenas?tamano=2&pagina=1"), yo.token())
            .andExpect(jsonPath("$.pagina").value(1))
            .andExpect(jsonPath("$.tamano").value(2))
            .andExpect(jsonPath("$.totalElementos").value(3))
            .andExpect(jsonPath("$.totalPaginas").value(2))
            .andExpect(jsonPath("$.elementos[*].texto", Matchers.contains("tres")));
        for (String consulta : List.of("?pagina=-1", "?tamano=0", "?tamano=51", "?pagina=x")) {
            pedir(get("/api/tutores/" + ana.id() + "/resenas" + consulta), yo.token())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        }
    }

    @Test
    void lasResenasRecibidasSeVenConElIdPropio() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        resena(ana, yo, 5, "Muy bien", LocalDate.now());
        pedir(get("/api/tutores/" + ana.id() + "/resenas"), ana.token())
            .andExpect(status().isOk()).andExpect(jsonPath("$.elementos[0].texto").value("Muy bien"));
    }

    @Test
    void unTutorSinResenasDevuelveUnaPaginaVacia() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        pedir(get("/api/tutores/" + ana.id() + "/resenas"), yo.token())
            .andExpect(status().isOk()).andExpect(jsonPath("$.elementos").isEmpty()).andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    void lasResenasDeAlguienQueNoEsTutorDanNoEncontrado() throws Exception {
        for (String id : List.of(UUID.randomUUID().toString(), "no-es-un-id", yo.id())) {
            pedir(get("/api/tutores/" + id + "/resenas"), yo.token())
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("TUTOR_NO_ENCONTRADO"));
        }
    }

    // ---------- POST /{tutorId}/reportes ----------

    @Test
    void reportaYElReporteQuedaAbierto() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");

        String respuesta = reportar(yo.token(), ana.id(), "{\"motivo\":\"  Información falsa  \"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.creadoEn", Matchers.endsWith("-05:00")))
            .andReturn().getResponse().getContentAsString();

        em.flush();
        var fila = jdbc.queryForMap("SELECT motivo, reportante_id, tutor_id, cerrado_en FROM reportes WHERE id = ?",
            UUID.fromString(campo(respuesta, "$.id")));
        assertEquals("Información falsa", fila.get("motivo"));
        assertNull(fila.get("cerrado_en"));
    }

    @Test
    void soloAdmiteUnReporteAbiertoPorPersonaYTutor() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        Cuenta otro = cuenta("otro@ejemplo.com");
        reportar(yo.token(), ana.id(), "{\"motivo\":\"Primero\"}").andExpect(status().isCreated());

        reportar(yo.token(), ana.id(), "{\"motivo\":\"Segundo\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("REPORTE_DUPLICADO"));
        // Otra persona sí puede, y sobre otro tutor también
        reportar(otro.token(), ana.id(), "{\"motivo\":\"Otra persona\"}").andExpect(status().isCreated());
        Cuenta luz = tutor("luz@ejemplo.com", "Luz Díaz", "Física I");
        reportar(yo.token(), luz.id(), "{\"motivo\":\"Otro tutor\"}").andExpect(status().isCreated());

        // Cerrado el reporte, se puede volver a reportar
        sql("UPDATE reportes SET cerrado_en = CURRENT_TIMESTAMP WHERE reportante_id = ? AND tutor_id = ?",
            UUID.fromString(yo.id()), UUID.fromString(ana.id()));
        reportar(yo.token(), ana.id(), "{\"motivo\":\"De nuevo\"}").andExpect(status().isCreated());
    }

    @Test
    void laBaseImpideDosReportesAbiertosAunqueLleguenALaVez() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        reportar(yo.token(), ana.id(), "{\"motivo\":\"Primero\"}").andExpect(status().isCreated());
        em.flush();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
            "INSERT INTO reportes (id, tutor_id, reportante_id, motivo) VALUES (?, ?, ?, 'Segundo')",
            UUID.randomUUID(), UUID.fromString(ana.id()), UUID.fromString(yo.id())));
    }

    @Test
    void noSePuedeReportarASiMismo() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        reportar(ana.token(), ana.id(), "{\"motivo\":\"Me reporto\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.codigo").value("REPORTE_A_SI_MISMO"));
    }

    @Test
    void reportarAQuienNoEsTutorDaNoEncontrado() throws Exception {
        Cuenta otro = cuenta("otro@ejemplo.com");
        for (String id : List.of(UUID.randomUUID().toString(), "no-es-un-id", otro.id())) {
            reportar(yo.token(), id, "{\"motivo\":\"Un motivo\"}")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("TUTOR_NO_ENCONTRADO"));
        }
    }

    @Test
    void elMotivoEsObligatorioYTieneLimites() throws Exception {
        Cuenta ana = tutor("ana@ejemplo.com", "Ana Torres", "Cálculo I");
        for (String cuerpo : List.of("{}", "{\"motivo\":null}", "{\"motivo\":\"   \"}", "{\"motivo\":\"ab\"}", "{\"motivo\":\"  ab  \"}",
            "{\"motivo\":\"%s\"}".formatted("x".repeat(501)))) {
            reportar(yo.token(), ana.id(), cuerpo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        }
        reportar(yo.token(), ana.id(), "{\"motivo\":\"%s\"}".formatted("x".repeat(500))).andExpect(status().isCreated());
    }
}
