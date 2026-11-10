package com.grupo3.nexo.tutorias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.grupo3.nexo.config.HoraColombia;

/** El PIN de una presencial, el vencimiento con su penalización y el retardo pactado por chat. */
class AsistenciaApiTest extends PruebaTutoriasBase {

    private static final String PIN_BUENO = "{\"pin\":\"4821\"}";
    private static final String PIN_MALO = "{\"pin\":\"0000\"}";
    private static final String RETARDO_30 = "{\"tipo\":\"RETARDO\",\"minutos\":30}";
    private static final String CANCELAR = "{\"tipo\":\"CANCELACION\"}";

    @MockitoSpyBean private Clock reloj;

    private Cuenta ana;
    private Cuenta vale;
    /** Mañana a las 10:00: nada se vence por sí solo durante la prueba. */
    private Instant inicio;

    @BeforeEach
    void preparar() throws Exception {
        ana = cuenta("Ana Torres");
        vale = cuenta("Valeria Ríos");
        inicio = manana().atTime(10, 0).atZone(HoraColombia.ZONA).toInstant();
    }

    private void relojEn(Instant ahora) {
        doReturn(ahora).when(reloj).instant();
    }

    private UUID porEmpezar() {
        relojEn(inicio.minus(5, ChronoUnit.MINUTES));
        return sesionPresencial(vale, ana, inicio, null, null, null);
    }

    private static String ruta(UUID sesion) {
        return "/api/sesiones/" + sesion;
    }

    /** Un inicio ya pasado, en otro día para que no se solape con los demás. */
    private static Instant pasado() {
        return LocalDate.now(HoraColombia.ZONA).minusDays(3).atTime(10, 0).atZone(HoraColombia.ZONA).toInstant();
    }

    @Test
    void elEstudianteNoVeElPinHastaQueElTutorLoEnvia() throws Exception {
        UUID s = porEmpezar();
        pedir(ruta(s), vale.token()).andExpect(status().isOk())
            .andExpect(jsonPath("$.pinEnviado").value(false)).andExpect(jsonPath("$.pin").doesNotExist())
            .andExpect(jsonPath("$.limite").exists());

        enviarComo(ana.token(), ruta(s) + "/pin", "").andExpect(status().isOk())
            .andExpect(jsonPath("$.pinEnviado").value(true)).andExpect(jsonPath("$.pin").doesNotExist());

        pedir(ruta(s), vale.token()).andExpect(jsonPath("$.pin").value("4821"));
        pedir(ruta(s), ana.token()).andExpect(jsonPath("$.pin").doesNotExist());
        enviarComo(ana.token(), ruta(s) + "/pin", "").andExpect(status().isOk());
    }

    @Test
    void soloElTutorEnviaElPinYNoAntesDeTiempo() throws Exception {
        UUID s = porEmpezar();
        enviarComo(vale.token(), ruta(s) + "/pin", "").andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("NO_ES_TUTOR"));
        relojEn(inicio.minus(31, ChronoUnit.MINUTES));
        enviarComo(ana.token(), ruta(s) + "/pin", "").andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("SESION_AUN_NO_EMPIEZA"));
        relojEn(inicio.minus(30, ChronoUnit.MINUTES));
        enviarComo(ana.token(), ruta(s) + "/pin", "").andExpect(status().isOk());
    }

    @Test
    void unaVirtualNoUsaPin() throws Exception {
        LocalDate dia = manana();
        Cuenta tutora = tutor("Marta Gil", dia, "10:00");
        String sesion = campo(tutoriaAceptada(vale, tutora, dia, "10:00"), "$.sesion.id");
        enviarComo(tutora.token(), "/api/sesiones/" + sesion + "/pin", "").andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("SESION_SIN_PIN"));
        enviarComo(vale.token(), "/api/sesiones/" + sesion + "/acuerdo", RETARDO_30).andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("SESION_SIN_PIN"));
        pedir("/api/sesiones/" + sesion, vale.token()).andExpect(jsonPath("$.limite").doesNotExist());
    }

    @Test
    void unaVirtualTambienSeCancelaDeAcuerdoYLaPuedeProponerElTutor() throws Exception {
        LocalDate dia = manana();
        Cuenta tutora = tutor("Marta Gil", dia, "10:00");
        String sesion = campo(tutoriaAceptada(vale, tutora, dia, "10:00"), "$.sesion.id");
        enviarComo(tutora.token(), "/api/sesiones/" + sesion + "/acuerdo", CANCELAR).andExpect(status().isCreated());
        enviarComo(vale.token(), "/api/sesiones/" + sesion + "/acuerdo/aceptar", "").andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADA")).andExpect(jsonPath("$.cierre").value("ACORDADA"))
            .andExpect(jsonPath("$.penalizacion").doesNotExist());
        pedir("/api/solicitudes?modo=ESTUDIANTE", vale.token()).andExpect(jsonPath("$[0].estado").value("CANCELADA"));
    }

    @Test
    void hayQueEnviarElPinAntesDeEscribirlo() throws Exception {
        UUID s = porEmpezar();
        enviarComo(ana.token(), ruta(s) + "/iniciar", PIN_BUENO).andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("PIN_SIN_ENVIAR"));
    }

    @Test
    void conElPinCorrectoEmpiezaYRepetirNoFalla() throws Exception {
        UUID s = porEmpezar();
        enviarComo(ana.token(), ruta(s) + "/pin", "").andExpect(status().isOk());
        enviarComo(vale.token(), ruta(s) + "/iniciar", PIN_BUENO).andExpect(status().isForbidden());
        enviarComo(ana.token(), ruta(s) + "/iniciar", PIN_BUENO).andExpect(status().isOk())
            .andExpect(jsonPath("$.iniciadaEn").exists()).andExpect(jsonPath("$.limite").doesNotExist());
        pedir(ruta(s), vale.token()).andExpect(jsonPath("$.pin").doesNotExist());
        enviarComo(ana.token(), ruta(s) + "/iniciar", PIN_MALO).andExpect(status().isOk());
    }

    @Test
    void cincoPinesMalosBloqueanCincoMinutos() throws Exception {
        UUID s = porEmpezar();
        enviarComo(ana.token(), ruta(s) + "/pin", "").andExpect(status().isOk());
        enviarComo(ana.token(), ruta(s) + "/iniciar", PIN_MALO).andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.codigo").value("PIN_INCORRECTO"))
            .andExpect(jsonPath("$.mensaje").value(containsString("Te quedan 4 intentos")));
        for (int i = 0; i < 3; i++) {
            enviarComo(ana.token(), ruta(s) + "/iniciar", PIN_MALO).andExpect(status().isUnprocessableEntity());
        }
        enviarComo(ana.token(), ruta(s) + "/iniciar", PIN_MALO).andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.codigo").value("PIN_BLOQUEADO")).andExpect(header().string("Retry-After", "300"));
        // Bloqueada, ni el correcto entra
        enviarComo(ana.token(), ruta(s) + "/iniciar", PIN_BUENO).andExpect(status().isTooManyRequests());
        relojEn(inicio.minus(5, ChronoUnit.MINUTES).plus(5, ChronoUnit.MINUTES));
        enviarComo(ana.token(), ruta(s) + "/iniciar", PIN_BUENO).andExpect(status().isOk());
    }

    @Test
    void sinPinALosDiezMinutosSeCancelaYSePenalizaAlTutor() throws Exception {
        UUID s = sesionPresencial(vale, ana, pasado(), null, null, null);
        pedir(ruta(s), ana.token()).andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADA")).andExpect(jsonPath("$.cierre").value("TUTOR_NO_LLEGO"))
            .andExpect(jsonPath("$.penalizacion").value("NORMAL"));
        pedir(ruta(s), vale.token()).andExpect(jsonPath("$.cierre").value("TUTOR_NO_LLEGO"))
            .andExpect(jsonPath("$.penalizacion").doesNotExist());
        assertThat(jdbc.queryForObject("SELECT so.estado FROM solicitudes so JOIN sesiones s ON s.solicitud_id = so.id WHERE s.id = ?",
            String.class, s)).isEqualTo("CANCELADA");
    }

    @Test
    void conPinPeroSinEscribirloSeCancelaYSePenalizaAlEstudiante() throws Exception {
        Instant hora = pasado();
        UUID s = sesionPresencial(vale, ana, hora, hora, null, null);
        pedir(ruta(s), vale.token()).andExpect(jsonPath("$.estado").value("CANCELADA"))
            .andExpect(jsonPath("$.cierre").value("ESTUDIANTE_NO_LLEGO")).andExpect(jsonPath("$.penalizacion").value("NORMAL"));
        pedir(ruta(s), ana.token()).andExpect(jsonPath("$.penalizacion").doesNotExist());
    }

    @Test
    void incumplirLaHoraPactadaPenalizaMasGrave() throws Exception {
        Instant hora = pasado();
        UUID s = sesionPresencial(vale, ana, hora, null, hora.plus(30, ChronoUnit.MINUTES), null);
        pedir(ruta(s), ana.token()).andExpect(jsonPath("$.estado").value("CANCELADA"))
            .andExpect(jsonPath("$.cierre").value("TUTOR_NO_LLEGO")).andExpect(jsonPath("$.penalizacion").value("GRAVE"))
            .andExpect(jsonPath("$.horaPactada").value("10:30")).andExpect(jsonPath("$.retardoMio").value(true));
    }

    @Test
    void unaSesionIniciadaTerminaCompletadaYSinPenalizacion() throws Exception {
        Instant hora = pasado();
        UUID s = sesionPresencial(vale, ana, hora, hora, null, hora);
        pedir(ruta(s), ana.token()).andExpect(jsonPath("$.estado").value("COMPLETADA"))
            .andExpect(jsonPath("$.penalizacion").doesNotExist());
    }

    @Test
    void elLimiteEsDiezMinutosTrasLaHoraOTrasElEnvioYNuncaPasaDelFin() {
        Instant hora = pasado();
        Instant fin = hora.plus(2, ChronoUnit.HOURS);
        assertThat(limite(hora, fin, null, null)).isEqualTo(hora.plus(10, ChronoUnit.MINUTES));
        // Enviado antes de la hora: cuenta la hora
        assertThat(limite(hora, fin, null, hora.minus(20, ChronoUnit.MINUTES))).isEqualTo(hora.plus(10, ChronoUnit.MINUTES));
        // Enviado en el minuto 9: el estudiante también tiene sus 10 minutos
        assertThat(limite(hora, fin, null, hora.plus(9, ChronoUnit.MINUTES))).isEqualTo(hora.plus(19, ChronoUnit.MINUTES));
        assertThat(limite(hora, fin, hora.plus(30, ChronoUnit.MINUTES), null)).isEqualTo(hora.plus(40, ChronoUnit.MINUTES));
        assertThat(limite(hora, fin, null, fin.minus(1, ChronoUnit.MINUTES))).isEqualTo(fin);
    }

    private Instant limite(Instant hora, Instant fin, Instant pactada, Instant enviado) {
        return jdbc.queryForObject("SELECT fn_sesion_limite(?::timestamptz, ?::timestamptz, ?::timestamptz, ?::timestamptz)",
            Timestamp.class, Timestamp.from(hora), Timestamp.from(fin), pactada == null ? null : Timestamp.from(pactada),
            enviado == null ? null : Timestamp.from(enviado)).toInstant();
    }

    @Test
    void elRetardoSePactaEntreLosDosYQuedaEnElChat() throws Exception {
        UUID s = porEmpezar();
        enviarComo(ana.token(), ruta(s) + "/acuerdo", RETARDO_30).andExpect(status().isCreated())
            .andExpect(jsonPath("$.acuerdo.tipo").value("RETARDO")).andExpect(jsonPath("$.acuerdo.nuevaHora").value("10:30"))
            .andExpect(jsonPath("$.acuerdo.propuestoPorMi").value(true));
        pedir(ruta(s), vale.token()).andExpect(jsonPath("$.acuerdo.propuestoPorMi").value(false))
            .andExpect(jsonPath("$.horaPactada").doesNotExist());
        enviarComo(vale.token(), ruta(s) + "/acuerdo", CANCELAR).andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("ACUERDO_PENDIENTE"));
        enviarComo(ana.token(), ruta(s) + "/acuerdo/aceptar", "").andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACUERDO_PROPIO"));

        enviarComo(vale.token(), ruta(s) + "/acuerdo/aceptar", "").andExpect(status().isOk())
            .andExpect(jsonPath("$.horaPactada").value("10:30")).andExpect(jsonPath("$.retardoMio").value(false))
            .andExpect(jsonPath("$.acuerdo").doesNotExist());
        pedir(ruta(s), ana.token()).andExpect(jsonPath("$.retardoMio").value(true));

        enviarComo(vale.token(), ruta(s) + "/acuerdo", RETARDO_30).andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("RETARDO_YA_ACORDADO"));
        assertThat(jdbc.queryForList("""
            SELECT m.texto FROM mensajes m JOIN chats c ON c.id = m.chat_id
            WHERE c.estudiante_id = ? AND c.tutor_id = ? ORDER BY m.secuencia
            """, String.class, vale.uuid(), ana.uuid()))
            .containsExactly("Voy a llegar tarde. Propongo que empecemos a las 10:30.", "De acuerdo, empezamos a las 10:30.");
    }

    @Test
    void unaPropuestaSeRechazaOSeRetiraYSePuedeVolverAProponer() throws Exception {
        UUID s = porEmpezar();
        enviarComo(ana.token(), ruta(s) + "/acuerdo/rechazar", "").andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("ACUERDO_NO_PENDIENTE"));
        enviarComo(ana.token(), ruta(s) + "/acuerdo", RETARDO_30).andExpect(status().isCreated());
        enviarComo(vale.token(), ruta(s) + "/acuerdo/rechazar", "").andExpect(status().isOk())
            .andExpect(jsonPath("$.acuerdo").doesNotExist());
        enviarComo(vale.token(), ruta(s) + "/acuerdo", RETARDO_30).andExpect(status().isCreated());
        enviarComo(vale.token(), ruta(s) + "/acuerdo/rechazar", "").andExpect(status().isOk());
    }

    @Test
    void elRetardoDebeSerAUnaHoraPosibleYPedirMinutos() throws Exception {
        UUID s = porEmpezar();
        enviarComo(ana.token(), ruta(s) + "/acuerdo", "{\"tipo\":\"RETARDO\"}").andExpect(status().isBadRequest());
        enviarComo(ana.token(), ruta(s) + "/acuerdo", "{\"tipo\":\"RETARDO\",\"minutos\":90}").andExpect(status().isBadRequest());
        relojEn(inicio.plus(40, ChronoUnit.MINUTES));
        enviarComo(ana.token(), ruta(s) + "/acuerdo", RETARDO_30).andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("RETARDO_NO_VALIDO"));
    }

    @Test
    void cancelarDeAcuerdoCierraSinPenalizarYDejaElChatEnSoloLectura() throws Exception {
        UUID s = porEmpezar();
        enviarComo(vale.token(), ruta(s) + "/acuerdo", CANCELAR).andExpect(status().isCreated());
        enviarComo(ana.token(), ruta(s) + "/acuerdo/aceptar", "").andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADA")).andExpect(jsonPath("$.cierre").value("ACORDADA"))
            .andExpect(jsonPath("$.penalizacion").doesNotExist()).andExpect(jsonPath("$.acuerdo").doesNotExist());
        pedir("/api/sesiones/proximas", vale.token()).andExpect(jsonPath("$.length()").value(0));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM penalizaciones WHERE sesion_id = ?", Integer.class, s)).isZero();
        assertThat(jdbc.queryForObject("SELECT activo_hasta IS NULL FROM chats WHERE estudiante_id = ? AND tutor_id = ?",
            Boolean.class, vale.uuid(), ana.uuid())).isTrue();
    }

    @Test
    void unaSesionYaIniciadaNoAdmiteAcuerdos() throws Exception {
        UUID s = porEmpezar();
        enviarComo(ana.token(), ruta(s) + "/pin", "").andExpect(status().isOk());
        enviarComo(ana.token(), ruta(s) + "/iniciar", PIN_BUENO).andExpect(status().isOk());
        enviarComo(vale.token(), ruta(s) + "/acuerdo", CANCELAR).andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("SESION_YA_INICIADA"));
    }

    @Test
    void paraUnaCuentaAjenaLaSesionNoExiste() throws Exception {
        UUID s = porEmpezar();
        Cuenta otra = cuenta("Otra Persona");
        enviarComo(otra.token(), ruta(s) + "/pin", "").andExpect(status().isNotFound());
        enviarComo(otra.token(), ruta(s) + "/acuerdo", CANCELAR).andExpect(status().isNotFound());
    }
}
