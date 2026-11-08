package com.grupo3.nexo.tutorias;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** RF8: conversaciones, cursores, no leídos y solo lectura. */
class ChatApiTest extends PruebaTutoriasBase {

    private Cuenta ana;
    private Cuenta vale;
    private LocalDate dia;
    private String chat;
    private String solicitud;

    @BeforeEach
    void preparar() throws Exception {
        dia = manana();
        ana = tutor("Ana Torres", dia, "10:00");
        vale = cuenta("Valeria Ríos");
        String aceptada = tutoriaAceptada(vale, ana, dia, "10:00");
        chat = campo(aceptada, "$.chat.id");
        solicitud = campo(aceptada, "$.solicitud.id");
    }

    private String ruta() {
        return "/api/chats/" + chat + "/mensajes";
    }

    private String mensaje(Cuenta quien, String texto) throws Exception {
        String r = enviarComo(quien.token(), ruta(), "{\"texto\":\"%s\"}".formatted(texto))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return campo(r, "$.id");
    }

    @Test
    void elChatNaceAlAceptarYCadaLadoLoEncuentra() throws Exception {
        pedir("/api/chats?modo=ESTUDIANTE", vale.token()).andExpect(jsonPath("$[*].id", contains(chat)))
            .andExpect(jsonPath("$[0].nombre").value("Ana Torres"))
            .andExpect(jsonPath("$[0].ultimoMensaje").value(""))
            .andExpect(jsonPath("$[0].ultimoMensajeEn").doesNotExist())
            .andExpect(jsonPath("$[0].comoTutor").value(false));
        pedir("/api/chats?modo=TUTOR", ana.token()).andExpect(jsonPath("$[0].nombre").value("Valeria Ríos"))
            .andExpect(jsonPath("$[0].comoTutor").value(true));
        pedir("/api/chats?modo=TUTOR", vale.token()).andExpect(jsonPath("$", hasSize(0)));
        pedir("/api/chats/con/" + ana.id() + "?modo=ESTUDIANTE", vale.token()).andExpect(jsonPath("$.id").value(chat));
        pedir("/api/chats/con/" + vale.id() + "?modo=TUTOR", ana.token()).andExpect(jsonPath("$.id").value(chat));
    }

    @Test
    void sinSolicitudAceptadaNoHayChat() throws Exception {
        Cuenta otra = cuenta("Otra");
        pedir("/api/chats/con/" + ana.id() + "?modo=ESTUDIANTE", otra.token()).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("CHAT_NO_HABILITADO"));
        pedir("/api/chats/con/no-es-un-id?modo=ESTUDIANTE", otra.token()).andExpect(status().isNotFound());
    }

    @Test
    void paraUnaCuentaAjenaElChatNoExiste() throws Exception {
        Cuenta otra = cuenta("Otra");
        pedir("/api/chats/" + chat, otra.token()).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("CHAT_NO_ENCONTRADO"));
        pedir(ruta(), otra.token()).andExpect(status().isNotFound());
        enviarComo(otra.token(), ruta(), "{\"texto\":\"hola\"}").andExpect(status().isNotFound());
        enviarComo(otra.token(), "/api/chats/" + chat + "/leido", "").andExpect(status().isNotFound());
    }

    @Test
    void losMensajesLlevanQuienLosEnvioDesdeCadaLado() throws Exception {
        mensaje(vale, "Hola");
        mensaje(ana, "¡Hola!");
        pedir(ruta(), vale.token()).andExpect(jsonPath("$.hayMas").value(false))
            .andExpect(jsonPath("$.mensajes[*].texto", contains("Hola", "¡Hola!")))
            .andExpect(jsonPath("$.mensajes[*].mio", contains(true, false)));
        pedir(ruta(), ana.token()).andExpect(jsonPath("$.mensajes[*].mio", contains(false, true)));
        pedir("/api/chats/" + chat, vale.token()).andExpect(jsonPath("$.ultimoMensaje").value("¡Hola!"));
    }

    @Test
    void validaElTexto() throws Exception {
        enviarComo(vale.token(), ruta(), "{\"texto\":\"   \"}").andExpect(status().isBadRequest());
        enviarComo(vale.token(), ruta(), "{\"texto\":\"%s\"}".formatted("x".repeat(2001))).andExpect(status().isBadRequest());
        enviarComo(vale.token(), ruta(), "{}").andExpect(status().isBadRequest());
    }

    @Test
    void reintentarConElMismoIdClienteNoDuplica() throws Exception {
        String cuerpo = "{\"texto\":\"Hola\",\"idCliente\":\"%s\"}".formatted(UUID.randomUUID());
        String primero = enviarComo(vale.token(), ruta(), cuerpo).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String segundo = enviarComo(vale.token(), ruta(), cuerpo).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertEquals((String) campo(primero, "$.id"), campo(segundo, "$.id"));
        pedir(ruta(), vale.token()).andExpect(jsonPath("$.mensajes", hasSize(1)));
    }

    @Test
    void elMismoIdClienteDeLaOtraPersonaEsOtroMensaje() throws Exception {
        String cuerpo = "{\"texto\":\"Hola\",\"idCliente\":\"%s\"}".formatted(UUID.randomUUID());
        enviarComo(vale.token(), ruta(), cuerpo).andExpect(status().isCreated());
        enviarComo(ana.token(), ruta(), cuerpo).andExpect(status().isCreated());
        pedir(ruta(), vale.token()).andExpect(jsonPath("$.mensajes", hasSize(2)));
    }

    @Test
    void losCursoresPaginanEnAmbasDirecciones() throws Exception {
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ids.add(mensaje(vale, "m" + i));
        }
        pedir(ruta() + "?limite=2", vale.token())
            .andExpect(jsonPath("$.mensajes[*].texto", contains("m4", "m5")))
            .andExpect(jsonPath("$.hayMas").value(true));
        pedir(ruta() + "?limite=2&antes=" + ids.get(3), vale.token())
            .andExpect(jsonPath("$.mensajes[*].texto", contains("m2", "m3")))
            .andExpect(jsonPath("$.hayMas").value(true));
        pedir(ruta() + "?limite=2&antes=" + ids.get(1), vale.token())
            .andExpect(jsonPath("$.mensajes[*].texto", contains("m1")))
            .andExpect(jsonPath("$.hayMas").value(false));
        pedir(ruta() + "?limite=2&desde=" + ids.get(0), vale.token())
            .andExpect(jsonPath("$.mensajes[*].texto", contains("m2", "m3")))
            .andExpect(jsonPath("$.hayMas").value(true));
        pedir(ruta() + "?desde=" + ids.get(4), vale.token())
            .andExpect(jsonPath("$.mensajes", hasSize(0)))
            .andExpect(jsonPath("$.hayMas").value(false));
    }

    @Test
    void rechazaCursoresInvalidos() throws Exception {
        String id = mensaje(vale, "m");
        pedir(ruta() + "?antes=" + id + "&desde=" + id, vale.token()).andExpect(status().isBadRequest());
        pedir(ruta() + "?desde=" + UUID.randomUUID(), vale.token()).andExpect(status().isBadRequest());
        pedir(ruta() + "?desde=no-es-un-id", vale.token()).andExpect(status().isBadRequest());
        pedir(ruta() + "?limite=0", vale.token()).andExpect(status().isBadRequest());
        pedir(ruta() + "?limite=101", vale.token()).andExpect(status().isBadRequest());
    }

    @Test
    void losNoLeidosSoloCuentanLoDeLaOtraPersona() throws Exception {
        mensaje(vale, "uno");
        mensaje(vale, "dos");
        pedir("/api/chats?modo=TUTOR", ana.token()).andExpect(jsonPath("$[0].noLeidos").value(2));
        pedir("/api/chats?modo=ESTUDIANTE", vale.token()).andExpect(jsonPath("$[0].noLeidos").value(0));
        enviarComo(ana.token(), "/api/chats/" + chat + "/leido", "").andExpect(status().isNoContent());
        pedir("/api/chats?modo=TUTOR", ana.token()).andExpect(jsonPath("$[0].noLeidos").value(0));
    }

    @Test
    void alCancelarLaUnicaSesionQuedaEnSoloLecturaYConservaElHistorial() throws Exception {
        String idCliente = UUID.randomUUID().toString();
        enviarComo(vale.token(), ruta(), "{\"texto\":\"Hola\",\"idCliente\":\"%s\"}".formatted(idCliente)).andExpect(status().isCreated());
        enviarComo(vale.token(), "/api/solicitudes/" + solicitud + "/cancelar", "").andExpect(status().isOk());

        pedir("/api/chats/" + chat, ana.token()).andExpect(jsonPath("$.soloLectura").value(true));
        enviarComo(ana.token(), ruta(), "{\"texto\":\"¿Sigues ahí?\"}")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CHAT_SOLO_LECTURA"));
        pedir(ruta(), ana.token()).andExpect(jsonPath("$.mensajes", hasSize(1)));
        enviarComo(ana.token(), "/api/chats/" + chat + "/leido", "").andExpect(status().isNoContent());
        // Un reintento de lo que sí llegó a guardarse sigue devolviendo el mensaje
        enviarComo(vale.token(), ruta(), "{\"texto\":\"Hola\",\"idCliente\":\"%s\"}".formatted(idCliente)).andExpect(status().isCreated());
    }

    @Test
    void aceptarOtraSolicitudLoReabre() throws Exception {
        mensaje(vale, "Hola");
        enviarComo(vale.token(), "/api/solicitudes/" + solicitud + "/cancelar", "").andExpect(status().isOk());
        pedir("/api/chats/" + chat, vale.token()).andExpect(jsonPath("$.soloLectura").value(true));

        String otra = tutoriaAceptada(vale, ana, dia, "10:00");
        assertEquals(chat, (String) campo(otra, "$.chat.id"));
        pedir("/api/chats/" + chat, vale.token()).andExpect(jsonPath("$.soloLectura").value(false));
        mensaje(vale, "Otra vez");
    }

    @Test
    void cuandoTerminaLaHoraNoAdmiteMensajesAunqueNadieLoHayaActualizado() throws Exception {
        Cuenta jorge = cuenta("Jorge Luna");
        sesionTerminada(jorge, ana);
        String id = jdbc.queryForObject("SELECT id::text FROM chats WHERE estudiante_id = ?", String.class, jorge.uuid());
        pedir("/api/chats/" + id, jorge.token()).andExpect(jsonPath("$.soloLectura").value(true));
        enviarComo(jorge.token(), "/api/chats/" + id + "/mensajes", "{\"texto\":\"Hola\"}")
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CHAT_SOLO_LECTURA"));
    }

    @Test
    void laBandejaOcultaLosChatsVaciosYaCerradosYOrdenaPorElUltimoMensaje() throws Exception {
        // Vacío y cerrado: su sesión terminó sin que nadie escribiera
        sesionTerminada(vale, tutor("Carlos Mendoza", dia, "12:00"));
        pedir("/api/chats?modo=ESTUDIANTE", vale.token()).andExpect(jsonPath("$[*].id", contains(chat)));

        LocalDate otroDia = dia.plusDays(1);
        Cuenta lucia = tutor("Lucía Fernández", otroDia, "12:00");
        String deLucia = campo(tutoriaAceptada(vale, lucia, otroDia, "12:00"), "$.chat.id");
        Cuenta pablo = tutor("Pablo Sánchez", otroDia, "14:00");
        String nuevo = campo(tutoriaAceptada(vale, pablo, otroDia, "14:00"), "$.chat.id");
        mensaje(vale, "Primero");
        enviarComo(vale.token(), "/api/chats/" + deLucia + "/mensajes", "{\"texto\":\"Después\"}").andExpect(status().isCreated());
        pedir("/api/chats?modo=ESTUDIANTE", vale.token()).andExpect(jsonPath("$[*].id", contains(nuevo, deLucia, chat)));
    }

    @Test
    void limitaLosEnviosPorMinuto() throws Exception {
        for (int i = 0; i < 30; i++) {
            mensaje(vale, "m" + i);
        }
        enviarComo(vale.token(), ruta(), "{\"texto\":\"uno más\"}")
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.codigo").value("DEMASIADAS_SOLICITUDES"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Retry-After", "60"));
        // La otra persona sigue pudiendo escribir
        mensaje(ana, "Tranquila");
    }
}
