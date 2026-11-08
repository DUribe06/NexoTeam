package com.grupo3.nexo.tutores;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.grupo3.nexo.config.HoraColombia;
import com.grupo3.nexo.tutores.TutorDtos.ActividadTutor;
import com.grupo3.nexo.tutores.TutorDtos.ResenaDto;
import com.grupo3.nexo.tutores.TutorDtos.ResumenResenas;

/** Una reseña es la calificación que un estudiante le dio a su tutor (RF4, RF9); no hay otra copia del dato. */
@Component
class Resenas {

    private final JdbcClient jdbc;

    Resenas(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<ResenaDto> deTutor(UUID tutor, int pagina, int tamano) {
        return jdbc.sql("""
                SELECT c.id, c.tutor_id, a.nombre, a.handle, COALESCE(c.comentario, '') AS texto, c.estrellas, c.creado_en
                FROM calificaciones c JOIN usuarios a ON a.id = c.estudiante_id
                WHERE c.tutor_id = :tutor AND c.sentido = 'ESTUDIANTE_A_TUTOR'
                ORDER BY c.creado_en DESC, c.id
                LIMIT :tamano OFFSET :desde
                """).param("tutor", tutor).param("tamano", tamano).param("desde", (long) pagina * tamano)
            .query((rs, n) -> new ResenaDto(rs.getObject("id", UUID.class), rs.getObject("tutor_id", UUID.class),
                rs.getString("nombre"), rs.getString("handle"), rs.getString("texto"), rs.getInt("estrellas"),
                HoraColombia.momento(rs.getObject("creado_en", java.time.OffsetDateTime.class).toInstant()).toLocalDate()))
            .list();
    }

    long contarDe(UUID tutor) {
        return jdbc.sql("SELECT total_resenas FROM usuarios WHERE id = ?").param(tutor).query(Long.class).single();
    }

    Map<UUID, ResumenResenas> resumenes(Collection<UUID> tutores) {
        return jdbc.sql("SELECT id, calificacion_promedio, total_resenas FROM usuarios WHERE id IN (:tutores)")
            .param("tutores", tutores)
            .query((rs, n) -> new ResumenResenas(rs.getObject("id", UUID.class), rs.getDouble("calificacion_promedio"),
                rs.getLong("total_resenas")))
            .list().stream().collect(Collectors.toMap(ResumenResenas::tutorId, Function.identity()));
    }

    /** Una sesión cuya hora ya pasó cuenta aunque nadie la haya puesto al día todavía: la búsqueda solo lee. */
    Map<UUID, ActividadTutor> actividad(Collection<UUID> tutores) {
        return jdbc.sql("""
                SELECT tutor_id, COUNT(*) AS sesiones, COUNT(DISTINCT estudiante_id) AS estudiantes
                FROM sesiones
                WHERE tutor_id IN (:tutores) AND (estado = 'COMPLETADA' OR (estado = 'PROGRAMADA' AND fin <= now()))
                GROUP BY tutor_id
                """).param("tutores", tutores)
            .query((rs, n) -> Map.entry(rs.getObject("tutor_id", UUID.class),
                new ActividadTutor(rs.getInt("sesiones"), rs.getInt("estudiantes"))))
            .list().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
