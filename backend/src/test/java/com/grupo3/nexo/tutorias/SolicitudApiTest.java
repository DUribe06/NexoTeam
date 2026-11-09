package com.grupo3.nexo.tutorias;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** RF5 y RF6 de punta a punta. */
class SolicitudApiTest extends PruebaTutoriasBase {

    private Cuenta ana;
    private Cuenta vale;
    private LocalDate dia;

    @BeforeEach
    void preparar() throws Exception {
        dia = manana();
        ana = tutor("Ana Torres", dia, "10:00");
        vale = cuenta("Valeria Ríos");
    }

    private void codigo(org.springframework.test.web.servlet.ResultActions r, int estado, String codigo) throws Exception {
        r.andExpect(status().is(estado)).andExpect(jsonPath("$.codigo").value(codigo));
    }

    @Test
    void enviarLaDejaPendienteConElNombreYElPlazoQuePoneElServidor() throws Exception {
        enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "10:00"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.nombreTutor").value("Ana Torres"))
            .andExpect(jsonPath("$.nombreEstudiante").value("Valeria Ríos"))
            .andExpect(jsonPath("$.fecha").value(dia.toString()))
            .andExpect(jsonPath("$.hora").value("10:00"))
            .andExpect(jsonPath("$.mensaje").value(""))
            .andExpect(jsonPath("$.motivoRechazo").doesNotExist());
    }

    @Test
    void elPlazoEsDe24HorasOElInicioSiLlegaAntes() throws Exception {
        String id = enviarSolicitud(vale, ana, dia, "10:00");
        String respuesta = pedir("/api/solicitudes/" + id, vale.token()).andReturn().getResponse().getContentAsString();
        Instant enviada = OffsetDateTime.parse(campo(respuesta, "$.enviada")).toInstant();
        Instant vence = OffsetDateTime.parse(campo(respuesta, "$.venceEn")).toInstant();
        Instant inicio = dia.atTime(10, 0).atZone(com.grupo3.nexo.config.HoraColombia.ZONA).toInstant();
        Instant esperado = enviada.plus(Duration.ofHours(24)).isBefore(inicio) ? enviada.plus(Duration.ofHours(24)) : inicio;
        assertEquals(0, Duration.between(esperado, vence).getSeconds());
    }

    @Test
    void validaCadaRegla() throws Exception {
        codigo(enviarComo(ana.token(), "/api/solicitudes", solicitud(ana, dia, "10:00")), 422, "SOLICITUD_A_SI_MISMO");
        codigo(enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia.minusDays(1), "10:00")), 422, "FECHA_NO_VALIDA");
        codigo(enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia.plusDays(14), "10:00")), 422, "FECHA_NO_VALIDA");
        codigo(enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "12:00")), 422, "HORA_NO_DISPONIBLE");
        codigo(enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "09:00")), 422, "HORA_NO_DISPONIBLE");
        codigo(enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "10:00").replace("VIRTUAL", "PRESENCIAL")), 422, "MODALIDAD_NO_DISPONIBLE");
        codigo(enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "10:00").replace("Cálculo I", "Física")), 422, "MATERIA_NO_OFRECIDA");
        codigo(enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "10:00").replace(ana.id(), UUID.randomUUID().toString())), 404, "TUTOR_NO_ENCONTRADO");
        codigo(enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "25:00")), 400, "DATOS_INVALIDOS");
        codigo(enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "10:00").replace("Integrales", " ")), 400, "DATOS_INVALIDOS");
    }

    @Test
    void elEstudiantePuedeProponerOtroPrecio() throws Exception {
        String conPrecio = solicitud(ana, dia, "10:00").replaceFirst("\\}$", ",\"precioPropuesto\":20000}");
        String id = campo(enviarComo(vale.token(), "/api/solicitudes", conPrecio)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.precioPropuesto").value(20000))
            .andReturn().getResponse().getContentAsString(), "$.id");
        pedir("/api/solicitudes/" + id, ana.token()).andExpect(jsonPath("$.precioPropuesto").value(20000));
    }

    @Test
    void sinPrecioPropuestoNoSeEnviaElCampo() throws Exception {
        enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "10:00"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.precioPropuesto").doesNotExist());
    }

    @Test
    void elPrecioPropuestoDebeSerValido() throws Exception {
        for (String precio : new String[] {"0", "-5000", "10000001"}) {
            String cuerpo = solicitud(ana, dia, "10:00").replaceFirst("\\}$", ",\"precioPropuesto\":" + precio + "}");
            codigo(enviarComo(vale.token(), "/api/solicitudes", cuerpo), 400, "DATOS_INVALIDOS");
        }
    }

    @Test
    void laMateriaSeGuardaComoLaEscribioElTutor() throws Exception {
        enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "10:00").replace("Cálculo I", "cálculo i"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.materia").value("Cálculo I"));
    }

    @Test
    void noSePuedeRepetirMientrasSigaVivaPeroSiDespuesDeCancelar() throws Exception {
        String id = enviarSolicitud(vale, ana, dia, "10:00");
        codigo(enviarComo(vale.token(), "/api/solicitudes", solicitud(ana, dia, "10:00")), 409, "SOLICITUD_DUPLICADA");
        enviarComo(vale.token(), "/api/solicitudes/" + id + "/cancelar", "").andExpect(status().isOk());
        enviarSolicitud(vale, ana, dia, "10:00");
    }

    @Test
    void dosEstudiantesPuedenPedirLaMismaHoraYGanaAquelQueElTutorAcepte() throws Exception {
        Cuenta jorge = cuenta("Jorge Luna");
        String primera = enviarSolicitud(vale, ana, dia, "10:00");
        String segunda = enviarSolicitud(jorge, ana, dia, "10:00");
        enviarComo(ana.token(), "/api/solicitudes/" + primera + "/aceptar", "").andExpect(status().isOk());
        codigo(enviarComo(ana.token(), "/api/solicitudes/" + segunda + "/aceptar", ""), 409, "HORA_NO_DISPONIBLE");
        // Y ya no se puede pedir: la hora dejó de estar libre
        codigo(enviarComo(cuenta("Otra").token(), "/api/solicitudes", solicitud(ana, dia, "10:00")), 422, "HORA_NO_DISPONIBLE");
    }

    @Test
    void unEstudianteNoQuedaEnDosTutoriasALaMismaHora() throws Exception {
        Cuenta carlos = tutor("Carlos Mendoza", dia, "10:00");
        String conAna = enviarSolicitud(vale, ana, dia, "10:00");
        String conCarlos = enviarSolicitud(vale, carlos, dia, "10:00");
        enviarComo(ana.token(), "/api/solicitudes/" + conAna + "/aceptar", "").andExpect(status().isOk());
        codigo(enviarComo(carlos.token(), "/api/solicitudes/" + conCarlos + "/aceptar", ""), 409, "HORA_NO_DISPONIBLE");
    }

    @Test
    void aceptarCreaSesionYChatEnUnaSolaRespuesta() throws Exception {
        String id = enviarSolicitud(vale, ana, dia, "10:00");
        enviarComo(ana.token(), "/api/solicitudes/" + id + "/aceptar", "")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.solicitud.estado").value("ACEPTADA"))
            .andExpect(jsonPath("$.sesion.estado").value("PROGRAMADA"))
            .andExpect(jsonPath("$.sesion.solicitudId").value(id))
            .andExpect(jsonPath("$.sesion.comoTutor").value(true))
            .andExpect(jsonPath("$.sesion.conQuien").value("Valeria Ríos"))
            .andExpect(jsonPath("$.sesion.duracionHoras").value(1.0))
            .andExpect(jsonPath("$.chat.contraparteId").value(vale.id()))
            .andExpect(jsonPath("$.chat.soloLectura").value(false))
            .andExpect(jsonPath("$.chat.comoTutor").value(true));
    }

    @Test
    void soloElTutorRespondeYSoloElEstudianteCancela() throws Exception {
        String id = enviarSolicitud(vale, ana, dia, "10:00");
        codigo(enviarComo(vale.token(), "/api/solicitudes/" + id + "/aceptar", ""), 403, "NO_ES_TUTOR");
        codigo(enviarComo(vale.token(), "/api/solicitudes/" + id + "/rechazar", ""), 403, "NO_ES_TUTOR");
        codigo(enviarComo(ana.token(), "/api/solicitudes/" + id + "/cancelar", ""), 403, "NO_ES_ESTUDIANTE");
    }

    @Test
    void paraUnaCuentaAjenaLaSolicitudNoExiste() throws Exception {
        String id = enviarSolicitud(vale, ana, dia, "10:00");
        Cuenta otra = cuenta("Otra");
        codigo(pedir("/api/solicitudes/" + id, otra.token()), 404, "SOLICITUD_NO_ENCONTRADA");
        codigo(enviarComo(otra.token(), "/api/solicitudes/" + id + "/aceptar", ""), 404, "SOLICITUD_NO_ENCONTRADA");
        codigo(enviarComo(otra.token(), "/api/solicitudes/" + id + "/cancelar", ""), 404, "SOLICITUD_NO_ENCONTRADA");
        codigo(pedir("/api/solicitudes/no-es-un-id", otra.token()), 404, "SOLICITUD_NO_ENCONTRADA");
    }

    @Test
    void rechazarGuardaElMotivoYQueSoloPasaUnaVez() throws Exception {
        String id = enviarSolicitud(vale, ana, dia, "10:00");
        enviarComo(ana.token(), "/api/solicitudes/" + id + "/rechazar", "{\"motivo\":\" Estoy de viaje \"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RECHAZADA"))
            .andExpect(jsonPath("$.motivoRechazo").value("Estoy de viaje"));
        codigo(enviarComo(ana.token(), "/api/solicitudes/" + id + "/aceptar", ""), 409, "SOLICITUD_NO_PENDIENTE");
        codigo(enviarComo(ana.token(), "/api/solicitudes/" + id + "/rechazar", ""), 409, "SOLICITUD_NO_PENDIENTE");
    }

    @Test
    void cancelarUnaAceptadaCancelaSuSesionYCierraElChat() throws Exception {
        String aceptada = tutoriaAceptada(vale, ana, dia, "10:00");
        String id = campo(aceptada, "$.solicitud.id");
        String chat = campo(aceptada, "$.chat.id");
        enviarComo(vale.token(), "/api/solicitudes/" + id + "/cancelar", "")
            .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
        pedir("/api/sesiones/" + campo(aceptada, "$.sesion.id"), vale.token()).andExpect(jsonPath("$.estado").value("CANCELADA"));
        pedir("/api/chats/" + chat, vale.token()).andExpect(jsonPath("$.soloLectura").value(true));
        // La hora vuelve a estar libre
        enviarSolicitud(cuenta("Otra"), ana, dia, "10:00");
    }

    @Test
    void elChatSigueActivoSiQuedaOtraSesionConEsaPersona() throws Exception {
        publicar(ana, dia, "12:00");
        String primera = tutoriaAceptada(vale, ana, dia, "10:00");
        tutoriaAceptada(vale, ana, dia, "12:00");
        enviarComo(vale.token(), "/api/solicitudes/" + campo(primera, "$.solicitud.id") + "/cancelar", "").andExpect(status().isOk());
        pedir("/api/chats/" + campo(primera, "$.chat.id"), vale.token()).andExpect(jsonPath("$.soloLectura").value(false));
    }

    @Test
    void unaRechazadaOFinalizadaNoSePuedeCancelar() throws Exception {
        String id = enviarSolicitud(vale, ana, dia, "10:00");
        enviarComo(ana.token(), "/api/solicitudes/" + id + "/rechazar", "").andExpect(status().isOk());
        codigo(enviarComo(vale.token(), "/api/solicitudes/" + id + "/cancelar", ""), 409, "SOLICITUD_NO_CANCELABLE");

        UUID sesion = sesionTerminada(vale, ana);
        String deLaSesion = jdbc.queryForObject("SELECT solicitud_id::text FROM sesiones WHERE id = ?", String.class, sesion);
        codigo(enviarComo(vale.token(), "/api/solicitudes/" + deLaSesion + "/cancelar", ""), 409, "SOLICITUD_NO_CANCELABLE");
    }

    @Test
    void unaPendienteVencidaLlegaComoExpiradaYNoSePuedeResponder() throws Exception {
        String id = enviarSolicitud(vale, ana, dia, "10:00");
        Instant enviada = Instant.now().minus(Duration.ofHours(25));
        // Los campos de la solicitud no cambian (lo exige un trigger): se reemplaza la fila por una enviada hace más de 24 h
        jdbc.update("DELETE FROM solicitudes WHERE id = ?::uuid", id);
        jdbc.update("""
            INSERT INTO solicitudes (id, estudiante_id, tutor_id, materia, tema, inicio, modalidad, enviada_en, vence_en)
            VALUES (?::uuid, ?, ?, 'Cálculo I', 'Repaso', ?, 'VIRTUAL', ?, ?)
            """, id, vale.uuid(), ana.uuid(), Timestamp.from(dia.atTime(10, 0).atZone(com.grupo3.nexo.config.HoraColombia.ZONA).toInstant()),
            Timestamp.from(enviada), Timestamp.from(enviada.plus(Duration.ofHours(24))));

        codigo(enviarComo(ana.token(), "/api/solicitudes/" + id + "/aceptar", ""), 409, "SOLICITUD_EXPIRADA");
        codigo(enviarComo(ana.token(), "/api/solicitudes/" + id + "/rechazar", ""), 409, "SOLICITUD_EXPIRADA");
        pedir("/api/solicitudes/" + id, vale.token()).andExpect(jsonPath("$.estado").value("EXPIRADA"));
        pedir("/api/solicitudes?estado=EXPIRADA", vale.token()).andExpect(jsonPath("$", hasSize(1)));
        pedir("/api/solicitudes?estado=PENDIENTE", vale.token()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void cadaModoVeLasSuyasConLasPendientesPrimero() throws Exception {
        publicar(ana, dia, "12:00");
        String aceptada = campo(tutoriaAceptada(vale, ana, dia, "10:00"), "$.solicitud.id");
        String pendiente = enviarSolicitud(vale, ana, dia, "12:00");

        pedir("/api/solicitudes?modo=ESTUDIANTE", vale.token()).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id", contains(pendiente, aceptada)));
        pedir("/api/solicitudes?modo=TUTOR", ana.token()).andExpect(jsonPath("$[*].id", contains(pendiente, aceptada)));
        pedir("/api/solicitudes?modo=TUTOR", vale.token()).andExpect(jsonPath("$", hasSize(0)));
        pedir("/api/solicitudes?modo=ESTUDIANTE", ana.token()).andExpect(jsonPath("$", hasSize(0)));
        // Sin modo, el actual de cada cuenta
        pedir("/api/solicitudes", ana.token()).andExpect(jsonPath("$", hasSize(2)));
        pedir("/api/solicitudes?estado=ACEPTADA&estado=RECHAZADA", vale.token()).andExpect(jsonPath("$[*].id", contains(aceptada)));
        pedir("/api/solicitudes?estado=OTRO", vale.token()).andExpect(status().isBadRequest());
        pedir("/api/solicitudes?modo=ADMIN", vale.token()).andExpect(status().isBadRequest());
    }

    @Test
    void laDisponibilidadPublicaYaNoOfreceUnaHoraOcupada() throws Exception {
        publicar(ana, dia, "12:00");
        tutoriaAceptada(vale, ana, dia, "10:00");
        pedir("/api/tutores/" + ana.id() + "/disponibilidad", vale.token()).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].fecha").value(dia.toString()))
            .andExpect(jsonPath("$[0].franjas[*].hora", contains("12:00")));
    }

    @Test
    void todasLasRutasExigenToken() throws Exception {
        pedirSinToken("/api/solicitudes");
        pedirSinToken("/api/sesiones");
        pedirSinToken("/api/sesiones/proximas");
        pedirSinToken("/api/chats");
    }

    private void pedirSinToken(String ruta) throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(ruta))
            .andExpect(status().isUnauthorized());
    }
}
