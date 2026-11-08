package com.grupo3.nexo.tutorias;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.grupo3.nexo.auth.PruebaAuthBase;
import com.grupo3.nexo.config.HoraColombia;
import com.grupo3.nexo.disponibilidad.DiaSemana;

/** Cuentas, solicitudes y sesiones de prueba. Lo que la API no permite (fechas pasadas) se inserta directo en la base. */
public abstract class PruebaTutoriasBase extends PruebaAuthBase {

    protected record Cuenta(String id, String token) {
        protected UUID uuid() {
            return UUID.fromString(id);
        }
    }

    @Autowired private TransactionTemplate enTransaccion;

    private int diasAtras = 2;

    protected ResultActions pedir(String ruta, String token) throws Exception {
        return mvc.perform(get(ruta).header("Authorization", "Bearer " + token));
    }

    protected ResultActions enviarComo(String token, String ruta, String cuerpo) throws Exception {
        return mvc.perform(post(ruta).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    protected Cuenta cuenta(String nombre) throws Exception {
        String sesion = cuentaActiva(UUID.randomUUID() + "@ejemplo.com");
        Cuenta c = new Cuenta(campo(sesion, "$.usuario.id"), campo(sesion, "$.tokenAcceso"));
        confirmarPendientes();
        jdbc.update("UPDATE usuarios SET nombre = ? WHERE id = ?", nombre, c.uuid());
        em.clear();
        return c;
    }

    /** Un tutor que enseña Cálculo I y publica esa hora, en Virtual, el día de la semana de `fecha`. */
    protected Cuenta tutor(String nombre, LocalDate fecha, String hora) throws Exception {
        Cuenta c = cuenta(nombre);
        mvc.perform(put("/api/usuarios/yo/modo").header("Authorization", "Bearer " + c.token())
            .contentType(MediaType.APPLICATION_JSON).content("{\"modo\":\"TUTOR\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/usuarios/yo/perfil-tutor").header("Authorization", "Bearer " + c.token())
            .contentType(MediaType.APPLICATION_JSON).content("{\"materias\":[\"Cálculo I\"]}")).andExpect(status().isOk());
        enviarComo(c.token(), "/api/disponibilidad/yo", "").andExpect(status().is2xxSuccessful());
        publicar(c, fecha, hora);
        return c;
    }

    protected void publicar(Cuenta tutor, LocalDate fecha, String hora) throws Exception {
        mvc.perform(put("/api/disponibilidad/yo/horas").header("Authorization", "Bearer " + tutor.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"dia\":\"%s\",\"hora\":\"%s\",\"modalidades\":[\"VIRTUAL\"]}".formatted(DiaSemana.de(fecha), hora)))
            .andExpect(status().isOk());
    }

    protected LocalDate manana() {
        return LocalDate.now(HoraColombia.ZONA).plusDays(1);
    }

    protected static String solicitud(Cuenta tutor, LocalDate fecha, String hora) {
        return """
            {"tutorId":"%s","materia":"Cálculo I","tema":"Integrales","fecha":"%s","hora":"%s","modalidad":"VIRTUAL"}
            """.formatted(tutor.id(), fecha, hora);
    }

    protected String enviarSolicitud(Cuenta estudiante, Cuenta tutor, LocalDate fecha, String hora) throws Exception {
        String respuesta = enviarComo(estudiante.token(), "/api/solicitudes", solicitud(tutor, fecha, hora))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return campo(respuesta, "$.id");
    }

    /** Envía y acepta; devuelve la respuesta de aceptar. */
    protected String tutoriaAceptada(Cuenta estudiante, Cuenta tutor, LocalDate fecha, String hora) throws Exception {
        String id = enviarSolicitud(estudiante, tutor, fecha, hora);
        return enviarComo(tutor.token(), "/api/solicitudes/" + id + "/aceptar", "")
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    /**
     * Una tutoría cuya hora ya terminó y nadie ha puesto al día: solicitud ACEPTADA y sesión PROGRAMADA en el pasado,
     * como quedarían al pasar el tiempo. Devuelve el id de la sesión.
     */
    protected UUID sesionTerminada(Cuenta estudiante, Cuenta tutor) {
        confirmarPendientes();
        return sesionEn(estudiante.uuid(), tutor.uuid(), inicioPasado(), "ACEPTADA", "PROGRAMADA");
    }

    /** Una presencial programada de dos horas, tal como quedaría en este momento de su historia. Devuelve el id de la sesión. */
    protected UUID sesionPresencial(Cuenta estudiante, Cuenta tutor, Instant inicio, Instant pinEnviado, Instant horaPactada,
                                    Instant iniciada) {
        confirmarPendientes();
        UUID sesion = UUID.randomUUID();
        enTransaccion.executeWithoutResult(estado -> {
            UUID solicitud = UUID.randomUUID();
            Instant enviada = inicio.minus(3, ChronoUnit.DAYS);
            jdbc.update("""
                INSERT INTO chats (id, estudiante_id, tutor_id) VALUES (?, ?, ?) ON CONFLICT (estudiante_id, tutor_id) DO NOTHING
                """, UUID.randomUUID(), estudiante.uuid(), tutor.uuid());
            jdbc.update("""
                INSERT INTO solicitudes (id, estudiante_id, tutor_id, materia, tema, inicio, modalidad, estado, enviada_en, vence_en)
                VALUES (?, ?, ?, 'Cálculo I', 'Repaso', ?, 'PRESENCIAL', 'ACEPTADA', ?, ?)
                """, solicitud, estudiante.uuid(), tutor.uuid(), Timestamp.from(inicio), Timestamp.from(enviada),
                Timestamp.from(enviada.plus(24, ChronoUnit.HOURS)));
            jdbc.update("""
                INSERT INTO sesiones (id, solicitud_id, estudiante_id, tutor_id, inicio, fin, pin, pin_enviado_en, hora_pactada, retardo_de, iniciada_en)
                VALUES (?, ?, ?, ?, ?, ?, '4821', ?, ?, ?, ?)
                """, sesion, solicitud, estudiante.uuid(), tutor.uuid(), Timestamp.from(inicio),
                Timestamp.from(inicio.plus(2, ChronoUnit.HOURS)), momento(pinEnviado), momento(horaPactada),
                horaPactada == null ? null : tutor.uuid(), momento(iniciada));
        });
        return sesion;
    }

    private static Timestamp momento(Instant instante) {
        return instante == null ? null : Timestamp.from(instante);
    }

    /** Una sesión completada, con su calificación del estudiante publicada como reseña. */
    protected void resena(UUID tutor, UUID autor, int estrellas, String texto, Instant publicada) {
        confirmarPendientes();
        UUID sesion = sesionEn(autor, tutor, inicioPasado(), "FINALIZADA", "COMPLETADA");
        jdbc.update("""
            INSERT INTO calificaciones (id, sesion_id, estudiante_id, tutor_id, sentido, estrellas, comentario, creado_en)
            VALUES (?, ?, ?, ?, 'ESTUDIANTE_A_TUTOR', ?, ?, ?)
            """, UUID.randomUUID(), sesion, autor, tutor, estrellas, texto.isEmpty() ? null : texto, Timestamp.from(publicada));
        em.clear();
    }

    /** Cada una en un día distinto hacia atrás, a una hora del calendario, para que no se solapen. */
    private Instant inicioPasado() {
        return LocalDate.now(HoraColombia.ZONA).minusDays(diasAtras++).atTime(10, 0).atZone(HoraColombia.ZONA).toInstant();
    }

    /** JPA aún no ha escrito lo de la petición anterior si la prueba corre dentro de una transacción. */
    private void confirmarPendientes() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            em.flush();
        }
    }

    /** En una sola transacción: solicitud y sesión solo son coherentes juntas (trigger diferido). */
    private UUID sesionEn(UUID estudiante, UUID tutor, Instant inicio, String estadoSolicitud, String estadoSesion) {
        return enTransaccion.execute(estado -> insertarSesion(estudiante, tutor, inicio, estadoSolicitud, estadoSesion));
    }

    private UUID insertarSesion(UUID estudiante, UUID tutor, Instant inicio, String estadoSolicitud, String estadoSesion) {
        UUID solicitud = UUID.randomUUID();
        Instant enviada = inicio.minus(3, ChronoUnit.DAYS);
        jdbc.update("""
            INSERT INTO chats (id, estudiante_id, tutor_id) VALUES (?, ?, ?) ON CONFLICT (estudiante_id, tutor_id) DO NOTHING
            """, UUID.randomUUID(), estudiante, tutor);
        jdbc.update("""
            INSERT INTO solicitudes (id, estudiante_id, tutor_id, materia, tema, inicio, modalidad, estado, enviada_en, vence_en)
            VALUES (?, ?, ?, 'Cálculo I', 'Repaso', ?, 'VIRTUAL', ?, ?, ?)
            """, solicitud, estudiante, tutor, Timestamp.from(inicio), estadoSolicitud, Timestamp.from(enviada),
            Timestamp.from(enviada.plus(24, ChronoUnit.HOURS)));
        UUID sesion = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO sesiones (id, solicitud_id, estudiante_id, tutor_id, inicio, fin, estado) VALUES (?, ?, ?, ?, ?, ?, ?)
            """, sesion, solicitud, estudiante, tutor, Timestamp.from(inicio), Timestamp.from(inicio.plus(1, ChronoUnit.HOURS)), estadoSesion);
        return sesion;
    }
}
