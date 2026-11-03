package com.grupo3.nexo.sesiones;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica lo que depende del paso del tiempo, sin un proceso programado: la siguiente petición lo deja al día antes de
 * leer o de decidir. Casi siempre no hay nada que cambiar, y los índices parciales lo hacen barato.
 *
 * Si otra petición está cerrando las mismas filas, esta la espera en vez de saltárselas: al seguir ya las ve cerradas.
 * Saltarlas (`SKIP LOCKED`) dejaría calificar una sesión terminada con un SESION_NO_COMPLETADA falso. Bloquear siempre en
 * orden de id evita que dos puestas al día se interbloqueen. La hora es la de la base, la misma que usan los triggers
 * que validan estas transiciones.
 */
@Component
public class TutoriasAlDia {

    private final JdbcClient jdbc;

    public TutoriasAlDia(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void poner() {
        // Primero las presenciales que nadie inició: si no, la de abajo las daría por completadas
        jdbc.sql("""
            WITH vencidas AS (
                SELECT s.id, s.solicitud_id, s.tutor_id, s.estudiante_id, s.pin_enviado_en, s.hora_pactada
                FROM sesiones s JOIN solicitudes so ON so.id = s.solicitud_id
                WHERE s.estado = 'PROGRAMADA' AND s.iniciada_en IS NULL AND so.modalidad = 'PRESENCIAL' AND s.inicio <= now()
                  AND fn_sesion_limite(s.inicio, s.fin, s.hora_pactada, s.pin_enviado_en) <= now()
                ORDER BY s.id
                FOR UPDATE OF s
            ), canceladas AS (
                UPDATE sesiones s
                SET estado = 'CANCELADA',
                    cierre = CASE WHEN v.pin_enviado_en IS NULL THEN 'TUTOR_NO_LLEGO' ELSE 'ESTUDIANTE_NO_LLEGO' END
                FROM vencidas v WHERE s.id = v.id
                RETURNING s.id
            ), penalizadas AS (
                INSERT INTO penalizaciones (id, usuario_id, sesion_id, nivel)
                SELECT gen_random_uuid(), CASE WHEN v.pin_enviado_en IS NULL THEN v.tutor_id ELSE v.estudiante_id END, v.id,
                       CASE WHEN v.hora_pactada IS NULL THEN 'NORMAL' ELSE 'GRAVE' END
                FROM vencidas v JOIN canceladas c ON c.id = v.id
                RETURNING sesion_id
            )
            UPDATE solicitudes so SET estado = 'CANCELADA'
            FROM vencidas v JOIN penalizadas p ON p.sesion_id = v.id
            WHERE so.id = v.solicitud_id AND so.estado = 'ACEPTADA'
            """).update();
        jdbc.sql("""
            WITH terminadas AS (
                SELECT id, solicitud_id FROM sesiones
                WHERE estado = 'PROGRAMADA' AND fin <= now()
                ORDER BY id
                FOR UPDATE
            ), completadas AS (
                UPDATE sesiones s SET estado = 'COMPLETADA' FROM terminadas t WHERE s.id = t.id
                RETURNING t.solicitud_id
            )
            UPDATE solicitudes so SET estado = 'FINALIZADA'
            FROM completadas c WHERE so.id = c.solicitud_id AND so.estado = 'ACEPTADA'
            """).update();
        jdbc.sql("""
            UPDATE solicitudes SET estado = 'EXPIRADA'
            WHERE id IN (SELECT id FROM solicitudes WHERE estado = 'PENDIENTE' AND vence_en <= now()
                         ORDER BY id FOR UPDATE)
            """).update();
    }
}
