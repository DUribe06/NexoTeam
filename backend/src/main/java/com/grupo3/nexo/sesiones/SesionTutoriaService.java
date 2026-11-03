package com.grupo3.nexo.sesiones;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.config.HoraColombia;
import com.grupo3.nexo.config.Ids;
import com.grupo3.nexo.disponibilidad.HorasCalendario;
import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.sesiones.SesionDtos.AcuerdoPendiente;
import com.grupo3.nexo.sesiones.SesionDtos.CierreSesion;
import com.grupo3.nexo.sesiones.SesionDtos.EstadoRevision;
import com.grupo3.nexo.sesiones.SesionDtos.NivelPenalizacion;
import com.grupo3.nexo.sesiones.SesionDtos.SesionTutoria;
import com.grupo3.nexo.sesiones.SesionDtos.TipoAcuerdo;
import com.grupo3.nexo.sesiones.SesionDtos.SolicitudCalificar;
import com.grupo3.nexo.usuarios.Modalidad;
import com.grupo3.nexo.usuarios.Modo;
import com.grupo3.nexo.usuarios.UsuarioService;

/** RF9 y RF10: sesiones de tutoría, siempre vistas desde la cuenta que consulta, y su calificación mutua. */
@Service
public class SesionTutoriaService {

    /** La fila de la calificación "dada" y la "recibida" dependen de quién pregunta: `:yo` decide ambas. */
    private static final String CONSULTA = """
        SELECT s.id, s.solicitud_id, s.tutor_id, s.inicio, s.fin, s.estado, s.pin, s.pin_enviado_en, s.iniciada_en,
               s.hora_pactada, s.retardo_de, s.cierre, so.materia, so.modalidad,
               fn_sesion_limite(s.inicio, s.fin, s.hora_pactada, s.pin_enviado_en) AS limite,
               CASE WHEN r.estado = 'ACEPTADA' THEN NULL ELSE p.nivel END AS penalizacion, r.estado AS revision,
               (p.id IS NOT NULL AND r.id IS NULL AND p.creado_en > now() - INTERVAL '7 days') AS revisable,
               a.tipo AS acuerdo_tipo, a.nueva_hora AS acuerdo_hora, a.propuesto_por AS acuerdo_de,
               o.id AS contraparte_id, o.nombre AS con_quien,
               dada.estrellas AS dada, dada.comentario AS comentario, recibida.estrellas AS recibida
        FROM sesiones s
        JOIN solicitudes so ON so.id = s.solicitud_id
        JOIN usuarios o ON o.id = CASE WHEN s.tutor_id = :yo THEN s.estudiante_id ELSE s.tutor_id END
        LEFT JOIN calificaciones dada ON dada.sesion_id = s.id
             AND dada.sentido = CASE WHEN s.tutor_id = :yo THEN 'TUTOR_A_ESTUDIANTE' ELSE 'ESTUDIANTE_A_TUTOR' END
        LEFT JOIN calificaciones recibida ON recibida.sesion_id = s.id
             AND recibida.sentido = CASE WHEN s.tutor_id = :yo THEN 'ESTUDIANTE_A_TUTOR' ELSE 'TUTOR_A_ESTUDIANTE' END
        LEFT JOIN penalizaciones p ON p.sesion_id = s.id AND p.usuario_id = :yo
        LEFT JOIN revisiones_penalizacion r ON r.penalizacion_id = p.id
        LEFT JOIN acuerdos_sesion a ON a.sesion_id = s.id AND a.estado = 'PROPUESTO' AND s.estado = 'PROGRAMADA'
        """;

    private final JdbcClient jdbc;
    private final UsuarioService usuarios;
    private final TutoriasAlDia alDia;

    public SesionTutoriaService(JdbcClient jdbc, UsuarioService usuarios, TutoriasAlDia alDia) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.alDia = alDia;
    }

    /** La más reciente primero. */
    @Transactional
    public List<SesionTutoria> listar(UUID yo, Modo modo, List<EstadoSesion> estados) {
        Modo efectivo = usuarios.modoDe(yo, modo);
        alDia.poner();
        String filtro = estados == null || estados.isEmpty() ? "" : " AND s.estado IN (:estados)";
        JdbcClient.StatementSpec consulta = jdbc.sql(CONSULTA + " WHERE " + columna(efectivo) + " = :yo" + filtro
            + " ORDER BY s.inicio DESC, s.id").param("yo", yo);
        if (!filtro.isEmpty()) {
            consulta = consulta.param("estados", estados.stream().map(Enum::name).toList());
        }
        return consulta.query((rs, n) -> leer(rs, yo)).list();
    }

    @Transactional
    public List<SesionTutoria> proximas(UUID yo, Modo modo) {
        Modo efectivo = usuarios.modoDe(yo, modo);
        alDia.poner();
        return jdbc.sql(CONSULTA + " WHERE " + columna(efectivo) + " = :yo AND s.estado = 'PROGRAMADA' ORDER BY s.inicio, s.id")
            .param("yo", yo).query((rs, n) -> leer(rs, yo)).list();
    }

    @Transactional
    public List<SesionTutoria> historial(UUID yo, Modo modo, String materia) {
        Modo efectivo = usuarios.modoDe(yo, modo);
        alDia.poner();
        String filtro = materia == null || materia.isBlank() ? "" : " AND LOWER(so.materia) = LOWER(:materia)";
        JdbcClient.StatementSpec consulta = jdbc.sql(CONSULTA + " WHERE " + columna(efectivo) + " = :yo AND s.estado <> 'PROGRAMADA'"
            + filtro + " ORDER BY s.inicio DESC, s.id").param("yo", yo);
        if (!filtro.isEmpty()) {
            consulta = consulta.param("materia", materia.trim());
        }
        return consulta.query((rs, n) -> leer(rs, yo)).list();
    }

    @Transactional
    public SesionTutoria una(UUID yo, String sesionId) {
        usuarios.cuentaEnSesion(yo);
        alDia.poner();
        return Ids.leer(sesionId).map(id -> vista(yo, id)).orElseThrow(SesionTutoriaService::noEncontrada);
    }

    /**
     * Mutua y sin edición (RF9). La calificación del estudiante es la reseña del tutor, y un trigger recalcula su promedio.
     * Un doble envío simultáneo choca con uq_calificaciones_sesion_sentido y sale como SESION_YA_CALIFICADA.
     */
    @Transactional
    public SesionTutoria calificar(UUID yo, String sesionId, SolicitudCalificar s) {
        usuarios.cuentaEnSesion(yo);
        alDia.poner();
        SesionTutoria sesion = Ids.leer(sesionId).map(id -> vista(yo, id)).orElseThrow(SesionTutoriaService::noEncontrada);
        if (sesion.estado() != EstadoSesion.COMPLETADA) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "SESION_NO_COMPLETADA", "Solo se puede calificar una sesión completada");
        }
        if (sesion.calificacion() != null) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "SESION_YA_CALIFICADA", "Ya calificaste esta sesión");
        }
        UUID estudiante = sesion.comoTutor() ? sesion.contraparteId() : yo;
        UUID tutor = sesion.comoTutor() ? yo : sesion.contraparteId();
        String comentario = s.comentario() == null || s.comentario().isBlank() ? null : s.comentario().trim();
        jdbc.sql("""
            INSERT INTO calificaciones (id, sesion_id, estudiante_id, tutor_id, sentido, estrellas, comentario)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """).params(UUID.randomUUID(), sesion.id(), estudiante, tutor,
                sesion.comoTutor() ? "TUTOR_A_ESTUDIANTE" : "ESTUDIANTE_A_TUTOR", s.estrellas(), comentario).update();
        return vista(yo, sesion.id());
    }

    /** Dentro de la transacción que acepta una solicitud. */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID programar(UUID solicitudId, UUID estudiante, UUID tutor, Instant inicio, Instant fin) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO sesiones (id, solicitud_id, estudiante_id, tutor_id, inicio, fin) VALUES (?, ?, ?, ?, ?, ?)")
            .params(id, solicitudId, estudiante, tutor, inicio.atOffset(ZoneOffset.UTC), fin.atOffset(ZoneOffset.UTC)).update();
        return id;
    }

    /** Dentro de la transacción que cancela una solicitud. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelarDe(UUID solicitudId) {
        // Una presencial se puede iniciar antes de su hora: ya en curso, no se cancela
        boolean yaIniciada = jdbc.sql("SELECT EXISTS (SELECT 1 FROM sesiones WHERE solicitud_id = ? AND iniciada_en IS NOT NULL)")
            .param(solicitudId).query(Boolean.class).single();
        if (yaIniciada) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "SESION_YA_INICIADA", "La sesión ya empezó: no se puede cancelar");
        }
        jdbc.sql("UPDATE sesiones SET estado = 'CANCELADA' WHERE solicitud_id = ? AND estado = 'PROGRAMADA'")
            .param(solicitudId).update();
    }

    /** La sesión vista desde `yo`; si no participa en ella, para esa cuenta no existe. */
    public SesionTutoria vista(UUID yo, UUID sesionId) {
        return jdbc.sql(CONSULTA + " WHERE s.id = :id AND (s.tutor_id = :yo OR s.estudiante_id = :yo)")
            .param("id", sesionId).param("yo", yo)
            .query((rs, n) -> leer(rs, yo)).optional()
            .orElseThrow(SesionTutoriaService::noEncontrada);
    }

    private static String columna(Modo modo) {
        return modo == Modo.TUTOR ? "s.tutor_id" : "s.estudiante_id";
    }

    private static ErrorNegocio noEncontrada() {
        return new ErrorNegocio(HttpStatus.NOT_FOUND, "SESION_NO_ENCONTRADA", "No existe esa sesión");
    }

    private static SesionTutoria leer(ResultSet rs, UUID yo) throws SQLException {
        Instant inicio = rs.getObject("inicio", OffsetDateTime.class).toInstant();
        Instant fin = rs.getObject("fin", OffsetDateTime.class).toInstant();
        var local = inicio.atZone(HoraColombia.ZONA);
        boolean comoTutor = rs.getObject("tutor_id", UUID.class).equals(yo);
        Modalidad modalidad = Modalidad.valueOf(rs.getString("modalidad"));
        EstadoSesion estado = EstadoSesion.valueOf(rs.getString("estado"));
        boolean sinIniciar = estado == EstadoSesion.PROGRAMADA && modalidad == Modalidad.PRESENCIAL && rs.getObject("iniciada_en") == null;
        boolean pinEnviado = rs.getObject("pin_enviado_en") != null;
        // El tutor nunca lo recibe: si pudiera verlo, escribirlo no probaría que se encontraron
        String pin = sinIniciar && pinEnviado && !comoTutor ? rs.getString("pin") : null;
        UUID retardoDe = rs.getObject("retardo_de", UUID.class);
        return new SesionTutoria(rs.getObject("id", UUID.class), rs.getObject("solicitud_id", UUID.class),
            rs.getObject("contraparte_id", UUID.class), rs.getString("con_quien"), rs.getString("materia"),
            local.toLocalDate(), HorasCalendario.texto(local.toLocalTime()),
            Duration.between(inicio, fin).toMinutes() / 60.0,
            modalidad, estado, comoTutor, rs.getObject("dada", Integer.class),
            rs.getString("comentario"), rs.getObject("recibida", Integer.class), instanteONulo(rs, "iniciada_en"),
            pinEnviado, pin, sinIniciar ? instanteONulo(rs, "limite") : null, horaLocal(rs, "hora_pactada"),
            retardoDe == null ? null : retardoDe.equals(yo),
            enumONulo(CierreSesion.class, rs.getString("cierre")), enumONulo(NivelPenalizacion.class, rs.getString("penalizacion")),
            acuerdo(rs, yo), enumONulo(EstadoRevision.class, rs.getString("revision")), rs.getBoolean("revisable"));
    }

    private static AcuerdoPendiente acuerdo(ResultSet rs, UUID yo) throws SQLException {
        String tipo = rs.getString("acuerdo_tipo");
        return tipo == null ? null : new AcuerdoPendiente(TipoAcuerdo.valueOf(tipo), horaLocal(rs, "acuerdo_hora"),
            yo.equals(rs.getObject("acuerdo_de", UUID.class)));
    }

    private static String horaLocal(ResultSet rs, String columna) throws SQLException {
        Instant valor = instanteONulo(rs, columna);
        return valor == null ? null : HorasCalendario.texto(valor.atZone(HoraColombia.ZONA).toLocalTime());
    }

    private static Instant instanteONulo(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : valor.toInstant();
    }

    private static <E extends Enum<E>> E enumONulo(Class<E> tipo, String valor) {
        return valor == null ? null : Enum.valueOf(tipo, valor);
    }
}
