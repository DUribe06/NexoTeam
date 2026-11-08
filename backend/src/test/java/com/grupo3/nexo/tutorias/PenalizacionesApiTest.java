package com.grupo3.nexo.tutorias;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.grupo3.nexo.config.HoraColombia;

/** Las normales suman al contador y la tercera pausa; una grave pausa cada vez más tiempo; y se puede pedir revisión. */
class PenalizacionesApiTest extends PruebaTutoriasBase {

    private static final String RESUMEN = "/api/usuarios/yo/resumen";
    private static final String CATALOGO = "/api/tutores?tamano=50";

    private LocalDate dia;
    private Cuenta ana;
    private Cuenta vale;
    private int diasAtras = 3;

    @BeforeEach
    void preparar() throws Exception {
        dia = manana();
        ana = tutor("Ana Torres", dia, "10:00");
        vale = cuenta("Valeria Ríos");
    }

    /** Cada una en un día pasado distinto, para que no se solapen. */
    private Instant pasado() {
        return LocalDate.now(HoraColombia.ZONA).minusDays(diasAtras++).atTime(10, 0).atZone(HoraColombia.ZONA).toInstant();
    }

    private UUID tutorFalta(boolean conRetardo) {
        Instant hora = pasado();
        return sesionPresencial(vale, ana, hora, null, conRetardo ? hora.plus(30, ChronoUnit.MINUTES) : null, null);
    }

    private UUID estudianteFalta(boolean conRetardo) {
        Instant hora = pasado();
        return sesionPresencial(vale, ana, hora, hora, conRetardo ? hora.plus(30, ChronoUnit.MINUTES) : null, null);
    }

    @Test
    void unaNormalSoloSumaAlContadorDeSuModo() throws Exception {
        tutorFalta(false);
        pedir(RESUMEN + "?modo=TUTOR", ana.token()).andExpect(status().isOk())
            .andExpect(jsonPath("$.penalizaciones").value(1)).andExpect(jsonPath("$.suspendidaHasta").doesNotExist());
        pedir(RESUMEN + "?modo=ESTUDIANTE", ana.token()).andExpect(jsonPath("$.penalizaciones").value(0));
        pedir(RESUMEN + "?modo=TUTOR", vale.token()).andExpect(jsonPath("$.penalizaciones").value(0));

        pedir(CATALOGO, vale.token()).andExpect(jsonPath("$.elementos[*].id", hasItem(ana.id())));
        pedir("/api/tutores/" + ana.id(), vale.token()).andExpect(jsonPath("$.penalizaciones").value(1));
        enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "10:00")).andExpect(status().isCreated());
    }

    @Test
    void elPerfilPublicoSoloCuentaLasRecibidasComoTutor() throws Exception {
        Cuenta otra = cuenta("Otra Persona");
        Instant hora = pasado();
        sesionPresencial(ana, otra, hora, hora, null, null);
        pedir("/api/tutores/" + ana.id(), vale.token()).andExpect(jsonPath("$.penalizaciones").value(0));
        pedir(RESUMEN + "?modo=ESTUDIANTE", ana.token()).andExpect(jsonPath("$.penalizaciones").value(1));
    }

    @Test
    void unTutorEnPausaSaleDelCatalogoYNoRecibeNiAceptaSolicitudes() throws Exception {
        String pendiente = enviarSolicitud(vale, ana, dia, "10:00");
        tutorFalta(true);

        pedir(CATALOGO, vale.token()).andExpect(jsonPath("$.elementos[*].id", not(hasItem(ana.id()))));
        pedir("/api/tutores/" + ana.id(), vale.token()).andExpect(status().isOk())
            .andExpect(jsonPath("$.penalizaciones").value(1));

        Cuenta otro = cuenta("Otro Estudiante");
        enviarComo(otro.token(), "/api/solicitudes", solicitud(ana, dia, "10:00")).andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("TUTOR_EN_PAUSA"));
        enviarComo(ana.token(), "/api/solicitudes/" + pendiente + "/aceptar", "").andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("CUENTA_EN_PAUSA"));
        enviarComo(ana.token(), "/api/solicitudes/" + pendiente + "/rechazar", "{}").andExpect(status().isOk());
    }

    @Test
    void unEstudianteEnPausaNoEnviaSolicitudesYElMensajeDiceHastaCuando() throws Exception {
        estudianteFalta(true);
        String fecha = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-CO"))
            .format(LocalDate.now(HoraColombia.ZONA).plusDays(7));

        pedir(RESUMEN + "?modo=ESTUDIANTE", vale.token())
            .andExpect(jsonPath("$.penalizaciones").value(1)).andExpect(jsonPath("$.suspendidaHasta").exists());
        // La pausa es de la cuenta: se ve también desde el otro modo, sin sumar a su contador
        pedir(RESUMEN + "?modo=TUTOR", vale.token())
            .andExpect(jsonPath("$.penalizaciones").value(0)).andExpect(jsonPath("$.suspendidaHasta").exists());
        enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "10:00")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("CUENTA_EN_PAUSA"))
            .andExpect(jsonPath("$.mensaje", containsString(fecha)));
    }

    @Test
    void loYaAgendadoSigueDuranteLaPausa() throws Exception {
        String aceptada = tutoriaAceptada(vale, ana, dia, "10:00");
        tutorFalta(true);
        pedir(RESUMEN + "?modo=TUTOR", ana.token()).andExpect(jsonPath("$.suspendidaHasta").exists());
        pedir("/api/sesiones/" + campo(aceptada, "$.sesion.id"), ana.token()).andExpect(jsonPath("$.estado").value("PROGRAMADA"));
    }

    /** Una grave ya vieja: así la fila nace con su fecha, sin tocar el trigger que la hace inmutable. */
    private void graveHace(int dias) {
        UUID sesion = tutorFalta(true);
        jdbc.update("""
            INSERT INTO penalizaciones (id, usuario_id, sesion_id, nivel, creado_en) VALUES (?, ?, ?, 'GRAVE', ?)
            """, UUID.randomUUID(), ana.uuid(), sesion, Timestamp.from(Instant.now().minus(dias, ChronoUnit.DAYS).minusSeconds(60)));
        jdbc.update("UPDATE sesiones SET estado = 'CANCELADA', cierre = 'TUTOR_NO_LLEGO' WHERE id = ?", sesion);
    }

    private static String enDias(int dias) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now(HoraColombia.ZONA).plusDays(dias));
    }

    @Test
    void laPausaTerminaSolaALosSieteDias() throws Exception {
        graveHace(7);
        pedir(RESUMEN + "?modo=TUTOR", ana.token())
            .andExpect(jsonPath("$.penalizaciones").value(1)).andExpect(jsonPath("$.suspendidaHasta").doesNotExist());
        pedir(CATALOGO, vale.token()).andExpect(jsonPath("$.elementos[*].id", hasItem(ana.id())));
        enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "10:00")).andExpect(status().isCreated());
    }

    @Test
    void laSegundaGraveEnNoventaDiasPausaCatorceYLaTerceraTreinta() throws Exception {
        graveHace(40);
        tutorFalta(true);
        pedir(RESUMEN + "?modo=TUTOR", ana.token()).andExpect(jsonPath("$.penalizaciones").value(2))
            .andExpect(jsonPath("$.suspendidaHasta", containsString(enDias(14))));

        graveHace(20);
        tutorFalta(true);
        pedir(RESUMEN + "?modo=TUTOR", ana.token()).andExpect(jsonPath("$.penalizaciones").value(4))
            .andExpect(jsonPath("$.suspendidaHasta", containsString(enDias(30))));
    }

    @Test
    void laTerceraNormalEnNoventaDiasPausaSieteDias() throws Exception {
        tutorFalta(false);
        tutorFalta(false);
        pedir(RESUMEN + "?modo=TUTOR", ana.token()).andExpect(jsonPath("$.penalizaciones").value(2))
            .andExpect(jsonPath("$.suspendidaHasta").doesNotExist());

        tutorFalta(false);
        pedir(RESUMEN + "?modo=TUTOR", ana.token()).andExpect(jsonPath("$.penalizaciones").value(3))
            .andExpect(jsonPath("$.suspendidaHasta", containsString(enDias(7))));
    }

    @Test
    void loQuePasoHaceMasDeNoventaDiasNoSumaNiAgranda() throws Exception {
        graveHace(100);
        tutorFalta(true);
        pedir(RESUMEN + "?modo=TUTOR", ana.token()).andExpect(jsonPath("$.penalizaciones").value(1))
            .andExpect(jsonPath("$.suspendidaHasta", containsString(enDias(7))));
    }

    @Test
    void elTutorVeLasPenalizacionesRecientesDelEstudianteEnLaSolicitud() throws Exception {
        estudianteFalta(false);
        String id = enviarSolicitud(vale, ana, dia, "10:00");
        pedir("/api/solicitudes/" + id, ana.token()).andExpect(jsonPath("$.penalizacionesEstudiante").value(1));

        Cuenta limpia = cuenta("Sin Faltas");
        String otra = enviarSolicitud(limpia, ana, dia.plusDays(7), "10:00");
        pedir("/api/solicitudes/" + otra, ana.token()).andExpect(jsonPath("$.penalizacionesEstudiante").value(0));
    }

    private String revision(UUID sesion) {
        return "/api/sesiones/" + sesion + "/revision";
    }

    private static final String MOTIVO = "{\"motivo\":\"Sí llegué, pero la app no me dejaba enviar el PIN\"}";

    @Test
    void quienRecibioLaPenalizacionPideRevisionUnaVezYLaPausaSigue() throws Exception {
        UUID s = tutorFalta(true);
        pedir("/api/sesiones/" + s, ana.token()).andExpect(jsonPath("$.puedePedirRevision").value(true));

        enviarComo(ana.token(), revision(s), MOTIVO).andExpect(status().isCreated())
            .andExpect(jsonPath("$.revision").value("ABIERTA")).andExpect(jsonPath("$.puedePedirRevision").value(false))
            .andExpect(jsonPath("$.penalizacion").value("GRAVE"));
        pedir(RESUMEN + "?modo=TUTOR", ana.token()).andExpect(jsonPath("$.suspendidaHasta").exists());

        enviarComo(ana.token(), revision(s), MOTIVO).andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("REVISION_DUPLICADA"));
    }

    @Test
    void laOtraParteNoPuedePedirRevisionYQuienNoParticipoNoVeLaSesion() throws Exception {
        UUID s = tutorFalta(false);
        pedir("/api/sesiones/" + s, vale.token()).andExpect(status().isOk()).andExpect(jsonPath("$.puedePedirRevision").value(false));
        enviarComo(vale.token(), revision(s), MOTIVO).andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("SIN_PENALIZACION"));
        enviarComo(cuenta("Otra").token(), revision(s), MOTIVO).andExpect(status().isNotFound());
    }

    @Test
    void noSePideRevisionConUnMotivoCortoNiPasadosSieteDias() throws Exception {
        UUID s = tutorFalta(false);
        enviarComo(ana.token(), revision(s), "{\"motivo\":\"corto\"}").andExpect(status().isBadRequest());

        UUID vieja = tutorFalta(true);
        jdbc.update("""
            INSERT INTO penalizaciones (id, usuario_id, sesion_id, nivel, creado_en) VALUES (?, ?, ?, 'GRAVE', ?)
            """, UUID.randomUUID(), ana.uuid(), vieja, Timestamp.from(Instant.now().minus(8, ChronoUnit.DAYS)));
        jdbc.update("UPDATE sesiones SET estado = 'CANCELADA', cierre = 'TUTOR_NO_LLEGO' WHERE id = ?", vieja);
        pedir("/api/sesiones/" + vieja, ana.token()).andExpect(jsonPath("$.puedePedirRevision").value(false));
        enviarComo(ana.token(), revision(vieja), MOTIVO).andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("REVISION_FUERA_DE_PLAZO"));
    }

    @Test
    void siLaRevisionLaRetiraDejaDeContarYTerminaLaPausa() throws Exception {
        UUID s = tutorFalta(true);
        enviarComo(ana.token(), revision(s), MOTIVO).andExpect(status().isCreated());
        jdbc.update("UPDATE revisiones_penalizacion SET estado = 'ACEPTADA', resuelto_en = now()");

        pedir(RESUMEN + "?modo=TUTOR", ana.token()).andExpect(jsonPath("$.penalizaciones").value(0))
            .andExpect(jsonPath("$.suspendidaHasta").doesNotExist());
        pedir(CATALOGO, vale.token()).andExpect(jsonPath("$.elementos[*].id", hasItem(ana.id())));
        pedir("/api/sesiones/" + s, ana.token()).andExpect(jsonPath("$.penalizacion").doesNotExist())
            .andExpect(jsonPath("$.revision").value("ACEPTADA"));
    }
}
