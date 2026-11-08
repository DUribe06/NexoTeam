package com.grupo3.nexo.tutorias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.config.HoraColombia;

/**
 * Cada regla que la base garantiza, probada sin pasar por el código de la aplicación: si un camino de escritura se
 * saltara los servicios, esto es lo que seguiría en pie.
 */
@SpringBootTest
@Transactional
class EsquemaTutoriasTest {

    @Autowired JdbcTemplate jdbc;

    private UUID estudiante;
    private UUID tutor;
    private Instant inicio;
    private Instant enviada;

    @BeforeEach
    void preparar() {
        estudiante = usuario();
        tutor = usuario();
        inicio = LocalDate.now(HoraColombia.ZONA).plusDays(3).atTime(10, 0).atZone(HoraColombia.ZONA).toInstant();
        enviada = inicio.minus(Duration.ofDays(2)).truncatedTo(ChronoUnit.MICROS);
        chat(estudiante, tutor);
    }

    private UUID usuario() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO usuarios (id, nombre, correo, contrasena_hash, handle, rol, estado_cuenta, terminos_version, terminos_aceptados_en)
            VALUES (?, 'P', ?, 'h', ?, 'ESTUDIANTE', 'ACTIVA', 't', CURRENT_TIMESTAMP)
            """, id, id + "@e.com", "@" + id);
        return id;
    }

    private UUID chat(UUID est, UUID tut) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO chats (id, estudiante_id, tutor_id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING", id, est, tut);
        return jdbc.queryForObject("SELECT id FROM chats WHERE estudiante_id = ? AND tutor_id = ?", UUID.class, est, tut);
    }

    private static Timestamp ts(Instant i) {
        return Timestamp.from(i);
    }

    private UUID solicitud(UUID est, UUID tut, Instant ini, Instant env, String estado) {
        UUID id = UUID.randomUUID();
        Instant plazo = env.plus(Duration.ofHours(24));
        jdbc.update("""
            INSERT INTO solicitudes (id, estudiante_id, tutor_id, materia, tema, inicio, modalidad, estado, enviada_en, vence_en)
            VALUES (?, ?, ?, 'M', 'T', ?, 'VIRTUAL', ?, ?, ?)
            """, id, est, tut, ts(ini), estado, ts(env), ts(plazo.isBefore(ini) ? plazo : ini));
        return id;
    }

    private UUID solicitud(String estado) {
        return solicitud(estudiante, tutor, inicio, enviada, estado);
    }

    private UUID sesion(UUID solicitud, UUID est, UUID tut, Instant ini, Instant fin, String estado) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO sesiones (id, solicitud_id, estudiante_id, tutor_id, inicio, fin, estado) VALUES (?, ?, ?, ?, ?, ?, ?)",
            id, solicitud, est, tut, ts(ini), ts(fin), estado);
        return id;
    }

    private UUID sesionCompletada() {
        Instant ini = LocalDate.now(HoraColombia.ZONA).minusDays(5).atTime(10, 0).atZone(HoraColombia.ZONA).toInstant();
        UUID s = solicitud(estudiante, tutor, ini, ini.minus(Duration.ofDays(2)), "FINALIZADA");
        return sesion(s, estudiante, tutor, ini, ini.plus(Duration.ofHours(1)), "COMPLETADA");
    }

    private void calificar(UUID sesion, String sentido, int estrellas) {
        jdbc.update("INSERT INTO calificaciones (id, sesion_id, estudiante_id, tutor_id, sentido, estrellas) VALUES (?, ?, ?, ?, ?, ?)",
            UUID.randomUUID(), sesion, estudiante, tutor, sentido, estrellas);
    }

    private void rechazaPor(String restriccion, Runnable accion) {
        DataAccessException e = assertThrows(DataAccessException.class, accion::run);
        String mensaje = String.valueOf(e.getMostSpecificCause().getMessage());
        assertTrue(mensaje.contains(restriccion), "esperaba " + restriccion + ", llegó: " + mensaje);
    }

    @Test
    void unaPersonaNoSeSolicitaASiMisma() {
        rechazaPor("ck_solicitudes_personas_distintas", () -> solicitud(tutor, tutor, inicio, enviada, "PENDIENTE"));
    }

    @Test
    void elInicioDebeEstarEnElCalendario() {
        Instant fuera = inicio.plus(Duration.ofHours(1));
        rechazaPor("ck_solicitudes_inicio_en_calendario", () -> solicitud(estudiante, tutor, fuera, enviada, "PENDIENTE"));
    }

    @Test
    void elPlazoNoLoDecideQuienEscribe() {
        rechazaPor("ck_solicitudes_plazo", () -> jdbc.update("""
            INSERT INTO solicitudes (id, estudiante_id, tutor_id, materia, tema, inicio, modalidad, enviada_en, vence_en)
            VALUES (?, ?, ?, 'M', 'T', ?, 'VIRTUAL', ?, ?)
            """, UUID.randomUUID(), estudiante, tutor, ts(inicio), ts(enviada), ts(enviada.plus(Duration.ofDays(9)))));
    }

    @Test
    void unEstudianteNoRepiteUnaSolicitudViva() {
        solicitud("PENDIENTE");
        rechazaPor("uq_solicitudes_activa", () -> solicitud("PENDIENTE"));
        // Otro estudiante sí puede pedir esa hora
        UUID otro = usuario();
        solicitud(otro, tutor, inicio, enviada, "PENDIENTE");
    }

    @Test
    void lasTransicionesDeUnaSolicitudSonLasDelFlujo() {
        UUID id = solicitud("PENDIENTE");
        jdbc.update("UPDATE solicitudes SET estado = 'RECHAZADA' WHERE id = ?", id);
        rechazaPor("tg_solicitudes_transicion", () -> jdbc.update("UPDATE solicitudes SET estado = 'ACEPTADA' WHERE id = ?", id));
    }

    @Test
    void loPedidoEnUnaSolicitudNoCambia() {
        UUID id = solicitud("PENDIENTE");
        rechazaPor("tg_solicitudes_transicion", () -> jdbc.update("UPDATE solicitudes SET materia = 'Otra' WHERE id = ?", id));
    }

    @Test
    void elMotivoSoloExisteEnUnaRechazada() {
        UUID id = solicitud("PENDIENTE");
        rechazaPor("ck_solicitudes_motivo", () -> jdbc.update("UPDATE solicitudes SET motivo_rechazo = 'x' WHERE id = ?", id));
    }

    @Test
    void unaSolicitudTieneUnaSolaSesion() {
        UUID s = solicitud("ACEPTADA");
        sesion(s, estudiante, tutor, inicio, inicio.plus(Duration.ofHours(1)), "PROGRAMADA");
        rechazaPor("uq_sesiones_solicitud", () -> sesion(s, estudiante, tutor, inicio, inicio.plus(Duration.ofHours(1)), "CANCELADA"));
    }

    @Test
    void laSesionTieneLasPersonasYLaHoraDeSuSolicitud() {
        UUID s = solicitud("ACEPTADA");
        Instant otraHora = inicio.plus(Duration.ofHours(2));
        rechazaPor("fk_sesiones_solicitud", () -> sesion(s, estudiante, tutor, otraHora, otraHora.plus(Duration.ofHours(1)), "PROGRAMADA"));
    }

    @Test
    void todaSesionTieneChat() {
        UUID sinChat = usuario();
        UUID s = solicitud(sinChat, tutor, inicio, enviada, "ACEPTADA");
        rechazaPor("fk_sesiones_chat", () -> sesion(s, sinChat, tutor, inicio, inicio.plus(Duration.ofHours(1)), "PROGRAMADA"));
    }

    @Test
    void unTutorNoTieneDosSesionesSolapadas() {
        UUID otro = usuario();
        chat(otro, tutor);
        Instant fin = inicio.plus(Duration.ofHours(1));
        sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, fin, "PROGRAMADA");
        rechazaPor("tg_sesiones_agenda", () ->
            sesion(solicitud(otro, tutor, inicio, enviada, "ACEPTADA"), otro, tutor, inicio, fin, "PROGRAMADA"));
    }

    @Test
    void lasExclusionesProtegenAunqueFalteElTriggerDeAgenda() {
        jdbc.execute("ALTER TABLE sesiones DISABLE TRIGGER tg_sesiones_agenda");
        UUID otro = usuario();
        chat(otro, tutor);
        chat(estudiante, otro);
        Instant fin = inicio.plus(Duration.ofHours(1));
        sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, fin, "PROGRAMADA");
        rechazaPor("ex_sesiones_tutor_sin_solape", () ->
            sesion(solicitud(otro, tutor, inicio, enviada, "ACEPTADA"), otro, tutor, inicio, fin, "PROGRAMADA"));
        rechazaPor("ex_sesiones_estudiante_sin_solape", () ->
            sesion(solicitud(estudiante, otro, inicio, enviada, "ACEPTADA"), estudiante, otro, inicio, fin, "PROGRAMADA"));
    }

    @Test
    void unaPersonaNoDaYTomaSesionesALaVez() {
        UUID otro = usuario();
        chat(tutor, otro);
        Instant fin = inicio.plus(Duration.ofHours(1));
        sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, fin, "PROGRAMADA");
        rechazaPor("tg_sesiones_agenda", () ->
            sesion(solicitud(tutor, otro, inicio, enviada, "ACEPTADA"), tutor, otro, inicio, fin, "PROGRAMADA"));
    }

    @Test
    void deUnaSesionSoloCambiaElEstado() {
        UUID s = sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, inicio.plus(Duration.ofHours(1)), "PROGRAMADA");
        rechazaPor("tg_sesiones_transicion", () -> jdbc.update("UPDATE sesiones SET fin = fin + INTERVAL '1 hour' WHERE id = ?", s));
        rechazaPor("tg_sesiones_transicion", () -> jdbc.update("UPDATE sesiones SET estado = 'COMPLETADA', fin = inicio + INTERVAL '1 minute' WHERE id = ?", s));
    }

    @Test
    void unaSesionQueAunNoAcabaNoNaceTerminada() {
        UUID s = solicitud(estudiante, tutor, inicio, enviada, "FINALIZADA");
        rechazaPor("tg_sesiones_terminada_en_el_pasado", () ->
            sesion(s, estudiante, tutor, inicio, inicio.plus(Duration.ofHours(1)), "COMPLETADA"));
    }

    @Test
    void unaSesionNoSeBorra() {
        UUID s = sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, inicio.plus(Duration.ofHours(1)), "PROGRAMADA");
        rechazaPor("tg_sesiones_sin_borrado", () -> jdbc.update("DELETE FROM sesiones WHERE id = ?", s));
    }

    @Test
    void unMensajeNoSeEditaNiSeBorra() {
        UUID c = chat(estudiante, tutor);
        sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, inicio.plus(Duration.ofHours(1)), "PROGRAMADA");
        mensaje(c, "ESTUDIANTE", null, "Uno");
        rechazaPor("tg_mensajes_inmutables", () -> jdbc.update("UPDATE mensajes SET texto = 'Otro' WHERE chat_id = ?", c));
        rechazaPor("tg_mensajes_inmutables", () -> jdbc.update("DELETE FROM mensajes WHERE chat_id = ?", c));
    }

    @Test
    void unaInasistenciaCierraTambienLaSolicitud() {
        Instant ini = LocalDate.now(HoraColombia.ZONA).minusDays(5).atTime(10, 0).atZone(HoraColombia.ZONA).toInstant();
        UUID sol = solicitud(estudiante, tutor, ini, ini.minus(Duration.ofDays(2)), "ACEPTADA");
        UUID s = sesion(sol, estudiante, tutor, ini, ini.plus(Duration.ofHours(1)), "PROGRAMADA");
        jdbc.update("UPDATE sesiones SET estado = 'NO_ASISTIO' WHERE id = ?", s);
        rechazaPor("ctg_solicitud_y_sesion_coherentes", () -> jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE"));
    }

    @Test
    void unEstudianteNoTieneDosSesionesSolapadas() {
        UUID otro = usuario();
        chat(estudiante, otro);
        Instant fin = inicio.plus(Duration.ofHours(1));
        sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, fin, "PROGRAMADA");
        rechazaPor("tg_sesiones_agenda", () ->
            sesion(solicitud(estudiante, otro, inicio, enviada, "ACEPTADA"), estudiante, otro, inicio, fin, "PROGRAMADA"));
    }

    @Test
    void unaSesionQueAcabaCuandoEmpiezaLaOtraNoChoca() {
        Instant fin = inicio.plus(Duration.ofHours(2));
        UUID otra = solicitud(estudiante, tutor, fin, enviada, "ACEPTADA");
        sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, fin, "PROGRAMADA");
        sesion(otra, estudiante, tutor, fin, fin.plus(Duration.ofHours(1)), "PROGRAMADA");
    }

    @Test
    void unaSesionCanceladaYaNoOcupaLaHora() {
        Instant fin = inicio.plus(Duration.ofHours(1));
        UUID s = sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, fin, "PROGRAMADA");
        jdbc.update("UPDATE sesiones SET estado = 'CANCELADA' WHERE id = ?", s);
        UUID otro = usuario();
        chat(otro, tutor);
        sesion(solicitud(otro, tutor, inicio, enviada, "ACEPTADA"), otro, tutor, inicio, fin, "PROGRAMADA");
    }

    @Test
    void unaSesionFuturaNoSeCompleta() {
        UUID s = sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, inicio.plus(Duration.ofHours(1)), "PROGRAMADA");
        rechazaPor("tg_sesiones_transicion", () -> jdbc.update("UPDATE sesiones SET estado = 'COMPLETADA' WHERE id = ?", s));
    }

    @Test
    void laSesionNoDuraMasDe4Horas() {
        rechazaPor("ck_sesiones_duracion", () -> sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, inicio.plus(Duration.ofHours(5)), "PROGRAMADA"));
    }

    @Test
    void solicitudYSesionNoQuedanEnEstadosIncoherentesAlConfirmar() {
        // El trigger es diferido: se fuerza a comprobar ahora, porque esta prueba nunca confirma
        solicitud("ACEPTADA");
        rechazaPor("ctg_solicitud_y_sesion_coherentes", () -> jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE"));
    }

    @Test
    void soloSeCalificaUnaSesionCompletada() {
        UUID s = sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, inicio.plus(Duration.ofHours(1)), "PROGRAMADA");
        rechazaPor("fk_calificaciones_sesion_completada", () -> calificar(s, "ESTUDIANTE_A_TUTOR", 5));
    }

    @Test
    void unaCalificacionPorPersonaYSesion() {
        UUID s = sesionCompletada();
        calificar(s, "ESTUDIANTE_A_TUTOR", 5);
        calificar(s, "TUTOR_A_ESTUDIANTE", 4);
        rechazaPor("uq_calificaciones_sesion_sentido", () -> calificar(s, "ESTUDIANTE_A_TUTOR", 1));
    }

    @Test
    void lasEstrellasVanDe1A5() {
        UUID s = sesionCompletada();
        rechazaPor("ck_calificaciones_estrellas", () -> calificar(s, "ESTUDIANTE_A_TUTOR", 0));
        rechazaPor("ck_calificaciones_estrellas", () -> calificar(s, "TUTOR_A_ESTUDIANTE", 6));
    }

    @Test
    void unaCalificacionNoSeEditaPeroSuComentarioSePuedeRetirar() {
        UUID s = sesionCompletada();
        jdbc.update("INSERT INTO calificaciones (id, sesion_id, estudiante_id, tutor_id, sentido, estrellas, comentario) VALUES (?, ?, ?, ?, 'ESTUDIANTE_A_TUTOR', 5, 'ofensivo')",
            UUID.randomUUID(), s, estudiante, tutor);
        jdbc.update("UPDATE calificaciones SET comentario = NULL WHERE sesion_id = ?", s);
        rechazaPor("tg_calificaciones_inmutables", () -> jdbc.update("UPDATE calificaciones SET estrellas = 1 WHERE sesion_id = ?", s));
    }

    @Test
    void unaSesionCalificadaNoCambiaDeEstado() {
        UUID s = sesionCompletada();
        calificar(s, "ESTUDIANTE_A_TUTOR", 5);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("UPDATE sesiones SET estado = 'NO_ASISTIO' WHERE id = ?", s));
    }

    @Test
    void elPromedioSeMantieneConCadaCalificacionYSoloCuentaLaDelEstudiante() {
        UUID uno = sesionCompletada();
        calificar(uno, "ESTUDIANTE_A_TUTOR", 5);
        calificar(uno, "TUTOR_A_ESTUDIANTE", 1);
        Instant ini = LocalDate.now(HoraColombia.ZONA).minusDays(6).atTime(10, 0).atZone(HoraColombia.ZONA).toInstant();
        UUID dos = sesion(solicitud(estudiante, tutor, ini, ini.minus(Duration.ofDays(2)), "FINALIZADA"), estudiante, tutor, ini, ini.plus(Duration.ofHours(1)), "COMPLETADA");
        calificar(dos, "ESTUDIANTE_A_TUTOR", 2);

        assertEquals(3.5, jdbc.queryForObject("SELECT calificacion_promedio FROM usuarios WHERE id = ?", Double.class, tutor));
        assertEquals(2, jdbc.queryForObject("SELECT total_resenas FROM usuarios WHERE id = ?", Integer.class, tutor));
        assertEquals(0, jdbc.queryForObject("SELECT total_resenas FROM usuarios WHERE id = ?", Integer.class, estudiante));

        jdbc.update("DELETE FROM calificaciones WHERE sesion_id = ? AND sentido = 'ESTUDIANTE_A_TUTOR'", dos);
        assertEquals(5.0, jdbc.queryForObject("SELECT calificacion_promedio FROM usuarios WHERE id = ?", Double.class, tutor));
    }

    @Test
    void laReputacionNoAceptaCifrasImposibles() {
        rechazaPor("ck_usuarios_reputacion", () -> jdbc.update("UPDATE usuarios SET suma_estrellas = 6, total_resenas = 1 WHERE id = ?", tutor));
    }

    @Test
    void noSeEscribeEnUnChatEnSoloLectura() {
        UUID c = chat(estudiante, tutor);
        rechazaPor("tg_mensajes_chat_activo", () -> mensaje(c, "ESTUDIANTE", null, "Hola"));
    }

    @Test
    void laSecuenciaAvanzaSinHuecosYActualizaElChat() {
        UUID c = chat(estudiante, tutor);
        sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, inicio.plus(Duration.ofHours(1)), "PROGRAMADA");
        mensaje(c, "ESTUDIANTE", null, "Uno");
        mensaje(c, "TUTOR", null, "Dos");
        assertEquals(List.of(1L, 2L), jdbc.queryForList("SELECT secuencia FROM mensajes WHERE chat_id = ? ORDER BY secuencia", Long.class, c));
        assertEquals(2L, jdbc.queryForObject("SELECT ultima_secuencia FROM chats WHERE id = ?", Long.class, c));
    }

    @Test
    void unIdClienteRepetidoNoGastaSecuencia() {
        UUID c = chat(estudiante, tutor);
        sesion(solicitud("ACEPTADA"), estudiante, tutor, inicio, inicio.plus(Duration.ofHours(1)), "PROGRAMADA");
        UUID cliente = UUID.randomUUID();
        mensaje(c, "ESTUDIANTE", cliente, "Uno");
        rechazaPor("uq_mensajes_id_cliente", () -> mensaje(c, "ESTUDIANTE", cliente, "Otra vez"));
        // Así envía el servicio: el reintento no inserta nada y el siguiente mensaje sigue sin hueco
        int insertadas = jdbc.update("""
            INSERT INTO mensajes (id, chat_id, emisor, id_cliente, texto) VALUES (?, ?, 'ESTUDIANTE', ?, 'Otra vez')
            ON CONFLICT (chat_id, emisor, id_cliente) DO NOTHING
            """, UUID.randomUUID(), c, cliente);
        assertEquals(0, insertadas);
        assertEquals(1L, jdbc.queryForObject("SELECT ultima_secuencia FROM chats WHERE id = ?", Long.class, c));
        mensaje(c, "ESTUDIANTE", null, "Dos");
        assertEquals(List.of(1L, 2L), jdbc.queryForList("SELECT secuencia FROM mensajes WHERE chat_id = ? ORDER BY secuencia", Long.class, c));
    }

    @Test
    void noSeLeeMasAllaDelUltimoMensaje() {
        UUID c = chat(estudiante, tutor);
        rechazaPor("ck_chats_lectura", () -> jdbc.update("UPDATE chats SET leido_hasta_tutor = 5 WHERE id = ?", c));
    }

    @Test
    void unaCuentaConHistorialNoSeBorraEnCascada() {
        solicitud("PENDIENTE");
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("DELETE FROM usuarios WHERE id = ?", estudiante));
    }

    private void mensaje(UUID chat, String emisor, UUID cliente, String texto) {
        jdbc.update("INSERT INTO mensajes (id, chat_id, emisor, id_cliente, texto) VALUES (?, ?, ?, ?, ?)",
            UUID.randomUUID(), chat, emisor, cliente, texto);
    }
}
