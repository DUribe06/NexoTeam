package com.grupo3.nexo.sesiones;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.config.HoraColombia;
import com.grupo3.nexo.usuarios.Modo;
import com.grupo3.nexo.usuarios.ResumenActividadDto;
import com.grupo3.nexo.usuarios.UsuarioService;

/** Las cifras del Perfil y del Historial. Solo cuentan las sesiones COMPLETADA del modo pedido. */
@Service
public class ResumenService {

    private final JdbcClient jdbc;
    private final UsuarioService usuarios;
    private final TutoriasAlDia alDia;
    private final Penalizaciones penalizaciones;

    public ResumenService(JdbcClient jdbc, UsuarioService usuarios, TutoriasAlDia alDia, Penalizaciones penalizaciones) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.alDia = alDia;
        this.penalizaciones = penalizaciones;
    }

    @Transactional
    public ResumenActividadDto de(UUID yo, Modo modo) {
        Modo efectivo = usuarios.modoDe(yo, modo);
        alDia.poner();
        boolean comoTutor = efectivo == Modo.TUTOR;
        String mia = comoTutor ? "s.tutor_id" : "s.estudiante_id";
        String otra = comoTutor ? "s.estudiante_id" : "s.tutor_id";
        String sentidoDado = comoTutor ? "TUTOR_A_ESTUDIANTE" : "ESTUDIANTE_A_TUTOR";
        String sentidoRecibido = comoTutor ? "ESTUDIANTE_A_TUTOR" : "TUTOR_A_ESTUDIANTE";

        Cifras cifras = jdbc.sql("""
            SELECT COUNT(*) AS sesiones,
                   COALESCE(SUM(EXTRACT(EPOCH FROM (s.fin - s.inicio))), 0) / 3600 AS horas,
                   COUNT(*) FILTER (WHERE dada.id IS NULL) AS por_calificar,
                   AVG(recibida.estrellas) AS promedio,
                   COUNT(recibida.id) AS total_resenas,
                   COUNT(DISTINCT %s) AS personas
            FROM sesiones s
            LEFT JOIN calificaciones dada ON dada.sesion_id = s.id AND dada.sentido = :dado
            LEFT JOIN calificaciones recibida ON recibida.sesion_id = s.id AND recibida.sentido = :recibido
            WHERE %s = :yo AND s.estado = 'COMPLETADA'
            """.formatted(otra, mia))
            .param("dado", sentidoDado).param("recibido", sentidoRecibido).param("yo", yo)
            .query((rs, n) -> new Cifras(rs.getInt("sesiones"), rs.getDouble("horas"), rs.getInt("por_calificar"),
                rs.getObject("promedio", java.math.BigDecimal.class), rs.getInt("total_resenas"), rs.getInt("personas")))
            .single();

        List<String> materias = jdbc.sql("""
            SELECT so.materia FROM sesiones s JOIN solicitudes so ON so.id = s.solicitud_id
            WHERE %s = :yo AND s.estado = 'COMPLETADA'
            GROUP BY so.materia ORDER BY COUNT(*) DESC, so.materia
            """.formatted(mia)).param("yo", yo).query(String.class).list();

        return new ResumenActividadDto(cifras.sesiones(), cifras.horas(), cifras.porCalificar(),
            cifras.promedio() == null ? null : Math.round(cifras.promedio().doubleValue() * 10) / 10.0,
            cifras.totalResenas(), cifras.personas(), materias, penalizaciones.recientes(yo, efectivo),
            penalizaciones.pausaHasta(yo).map(HoraColombia::momento).orElse(null));
    }

    private record Cifras(int sesiones, double horas, int porCalificar, java.math.BigDecimal promedio, int totalResenas,
                          int personas) {
    }
}
