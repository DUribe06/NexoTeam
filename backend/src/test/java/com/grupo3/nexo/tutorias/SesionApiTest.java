package com.grupo3.nexo.tutorias;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** RF9 y RF10: la sesión desde cada lado, su calificación mutua y las cifras del resumen. */
class SesionApiTest extends PruebaTutoriasBase {

    private Cuenta ana;
    private Cuenta vale;
    private LocalDate dia;

    @BeforeEach
    void preparar() throws Exception {
        dia = manana();
        ana = tutor("Ana Torres", dia, "10:00");
        vale = cuenta("Valeria Ríos");
    }

    private static String estrellas(int n) {
        return "{\"estrellas\":%d}".formatted(n);
    }

    @Test
    void cadaLadoVeLaSesionDesdeSuPosicion() throws Exception {
        String id = campo(tutoriaAceptada(vale, ana, dia, "10:00"), "$.sesion.id");
        pedir("/api/sesiones/" + id, vale.token()).andExpect(status().isOk())
            .andExpect(jsonPath("$.comoTutor").value(false))
            .andExpect(jsonPath("$.conQuien").value("Ana Torres"))
            .andExpect(jsonPath("$.contraparteId").value(ana.id()))
            .andExpect(jsonPath("$.materia").value("Cálculo I"))
            .andExpect(jsonPath("$.modalidad").value("VIRTUAL"))
            .andExpect(jsonPath("$.calificacion").doesNotExist());
        pedir("/api/sesiones/" + id, ana.token())
            .andExpect(jsonPath("$.comoTutor").value(true))
            .andExpect(jsonPath("$.conQuien").value("Valeria Ríos"));
    }

    @Test
    void paraUnaCuentaAjenaLaSesionNoExiste() throws Exception {
        String id = campo(tutoriaAceptada(vale, ana, dia, "10:00"), "$.sesion.id");
        Cuenta otra = cuenta("Otra");
        pedir("/api/sesiones/" + id, otra.token()).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("SESION_NO_ENCONTRADA"));
        enviarComo(otra.token(), "/api/sesiones/" + id + "/calificacion", estrellas(5)).andExpect(status().isNotFound());
        pedir("/api/sesiones/no-es-un-id", otra.token()).andExpect(status().isNotFound());
    }

    @Test
    void alTerminarSuHoraPasaACompletadaYSuSolicitudAFinalizada() throws Exception {
        UUID sesion = sesionTerminada(vale, ana);
        pedir("/api/sesiones/" + sesion, vale.token()).andExpect(jsonPath("$.estado").value("COMPLETADA"));
        String solicitud = jdbc.queryForObject("SELECT solicitud_id::text FROM sesiones WHERE id = ?", String.class, sesion);
        pedir("/api/solicitudes/" + solicitud, vale.token()).andExpect(jsonPath("$.estado").value("FINALIZADA"));
    }

    @Test
    void proximasEHistorialSeparanLasSesiones() throws Exception {
        publicar(ana, dia, "12:00");
        String tarde = campo(tutoriaAceptada(vale, ana, dia, "12:00"), "$.sesion.id");
        String temprano = campo(tutoriaAceptada(vale, ana, dia, "10:00"), "$.sesion.id");
        UUID pasada = sesionTerminada(vale, ana);

        pedir("/api/sesiones/proximas?modo=ESTUDIANTE", vale.token())
            .andExpect(jsonPath("$[*].id", contains(temprano, tarde)));
        pedir("/api/sesiones/historial?modo=ESTUDIANTE", vale.token())
            .andExpect(jsonPath("$[*].id", contains(pasada.toString())));
        pedir("/api/sesiones/historial?modo=ESTUDIANTE&materia=cálculo i", vale.token()).andExpect(jsonPath("$", hasSize(1)));
        pedir("/api/sesiones/historial?modo=ESTUDIANTE&materia=Física", vale.token()).andExpect(jsonPath("$", hasSize(0)));
        // La más reciente primero
        pedir("/api/sesiones?modo=TUTOR", ana.token())
            .andExpect(jsonPath("$[*].id", contains(tarde, temprano, pasada.toString())));
        pedir("/api/sesiones?modo=TUTOR&estado=COMPLETADA", ana.token()).andExpect(jsonPath("$", hasSize(1)));
        pedir("/api/sesiones?modo=ESTUDIANTE", ana.token()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void laCalificacionEsMutuaUnaVezYPublicaLaResena() throws Exception {
        UUID sesion = sesionTerminada(vale, ana);
        enviarComo(vale.token(), "/api/sesiones/" + sesion + "/calificacion", "{\"estrellas\":5,\"comentario\":\" Muy clara \"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.calificacion").value(5))
            .andExpect(jsonPath("$.comentario").value("Muy clara"))
            .andExpect(jsonPath("$.calificacionRecibida").doesNotExist());
        enviarComo(vale.token(), "/api/sesiones/" + sesion + "/calificacion", estrellas(1))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("SESION_YA_CALIFICADA"));

        // El tutor califica por su lado, y cada uno ve lo que dio y lo que recibió
        enviarComo(ana.token(), "/api/sesiones/" + sesion + "/calificacion", estrellas(4))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.calificacion").value(4))
            .andExpect(jsonPath("$.calificacionRecibida").value(5));
        pedir("/api/sesiones/" + sesion, vale.token())
            .andExpect(jsonPath("$.calificacion").value(5))
            .andExpect(jsonPath("$.calificacionRecibida").value(4));

        // Solo la del estudiante es reseña
        pedir("/api/tutores/" + ana.id(), vale.token())
            .andExpect(jsonPath("$.calificacionPromedio").value(5.0))
            .andExpect(jsonPath("$.totalResenas").value(1))
            .andExpect(jsonPath("$.sesionesTotales").value(1))
            .andExpect(jsonPath("$.estudiantes").value(1));
        pedir("/api/tutores/" + ana.id() + "/resenas", vale.token())
            .andExpect(jsonPath("$.elementos[0].autor").value("Valeria Ríos"))
            .andExpect(jsonPath("$.elementos[0].texto").value("Muy clara"))
            .andExpect(jsonPath("$.elementos[0].estrellas").value(5));
    }

    @Test
    void noSeCalificaLoQueNoEstaCompletado() throws Exception {
        String id = campo(tutoriaAceptada(vale, ana, dia, "10:00"), "$.sesion.id");
        enviarComo(vale.token(), "/api/sesiones/" + id + "/calificacion", estrellas(5))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("SESION_NO_COMPLETADA"));
    }

    @Test
    void validaLasEstrellasYElComentario() throws Exception {
        UUID sesion = sesionTerminada(vale, ana);
        String ruta = "/api/sesiones/" + sesion + "/calificacion";
        enviarComo(vale.token(), ruta, estrellas(0)).andExpect(status().isBadRequest());
        enviarComo(vale.token(), ruta, estrellas(6)).andExpect(status().isBadRequest());
        enviarComo(vale.token(), ruta, "{}").andExpect(status().isBadRequest());
        enviarComo(vale.token(), ruta, "{\"estrellas\":5,\"comentario\":\"%s\"}".formatted("x".repeat(501))).andExpect(status().isBadRequest());
        // Un comentario en blanco es no dejar comentario
        enviarComo(vale.token(), ruta, "{\"estrellas\":5,\"comentario\":\"  \"}").andExpect(status().isCreated())
            .andExpect(jsonPath("$.comentario").doesNotExist());
    }

    @Test
    void elPromedioDelTutorSeMantieneConCadaCalificacion() throws Exception {
        Cuenta jorge = cuenta("Jorge Luna");
        UUID uno = sesionTerminada(vale, ana);
        UUID dos = sesionTerminada(jorge, ana);
        enviarComo(vale.token(), "/api/sesiones/" + uno + "/calificacion", estrellas(5)).andExpect(status().isCreated());
        enviarComo(jorge.token(), "/api/sesiones/" + dos + "/calificacion", estrellas(2)).andExpect(status().isCreated());
        pedir("/api/tutores/" + ana.id(), vale.token())
            .andExpect(jsonPath("$.calificacionPromedio").value(3.5))
            .andExpect(jsonPath("$.totalResenas").value(2))
            .andExpect(jsonPath("$.sesionesTotales").value(2))
            .andExpect(jsonPath("$.estudiantes").value(2));
    }

    @Test
    void elResumenCuentaSoloLasCompletadasDeCadaModo() throws Exception {
        tutoriaAceptada(vale, ana, dia, "10:00");
        UUID calificada = sesionTerminada(vale, ana);
        sesionTerminada(vale, ana);
        enviarComo(vale.token(), "/api/sesiones/" + calificada + "/calificacion", estrellas(5)).andExpect(status().isCreated());
        enviarComo(ana.token(), "/api/sesiones/" + calificada + "/calificacion", estrellas(4)).andExpect(status().isCreated());

        pedir("/api/usuarios/yo/resumen?modo=ESTUDIANTE", vale.token())
            .andExpect(jsonPath("$.sesiones").value(2))
            .andExpect(jsonPath("$.horas").value(2.0))
            .andExpect(jsonPath("$.porCalificar").value(1))
            .andExpect(jsonPath("$.promedioRecibido").value(4.0))
            .andExpect(jsonPath("$.totalResenas").value(1))
            .andExpect(jsonPath("$.personas").value(1))
            .andExpect(jsonPath("$.materias[0]").value("Cálculo I"));
        pedir("/api/usuarios/yo/resumen?modo=TUTOR", ana.token())
            .andExpect(jsonPath("$.sesiones").value(2))
            .andExpect(jsonPath("$.promedioRecibido").value(5.0));
        pedir("/api/usuarios/yo/resumen?modo=TUTOR", vale.token())
            .andExpect(jsonPath("$.sesiones").value(0))
            .andExpect(jsonPath("$.promedioRecibido").doesNotExist());
    }

    @Test
    void laResenaUsaLaFechaEnHoraDeColombia() throws Exception {
        // 02:00 UTC del día 10 sigue siendo el día 9 en Colombia
        Instant publicada = Instant.parse("2026-10-10T02:00:00Z");
        resena(ana.uuid(), vale.uuid(), 5, "", publicada);
        pedir("/api/tutores/" + ana.id() + "/resenas", vale.token())
            .andExpect(jsonPath("$.elementos[0].fecha").value("2026-10-09"))
            .andExpect(jsonPath("$.elementos[0].texto").value(""));
    }
}
