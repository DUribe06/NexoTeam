package com.grupo3.nexo.solicitudes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.chats.ChatService;
import com.grupo3.nexo.config.HoraColombia;
import com.grupo3.nexo.config.Ids;
import com.grupo3.nexo.disponibilidad.DiaSemana;
import com.grupo3.nexo.disponibilidad.HoraPublicada;
import com.grupo3.nexo.disponibilidad.HoraPublicadaRepository;
import com.grupo3.nexo.disponibilidad.HorasCalendario;
import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.sesiones.Penalizaciones;
import com.grupo3.nexo.sesiones.SesionTutoriaService;
import com.grupo3.nexo.sesiones.TutoriasAlDia;
import com.grupo3.nexo.solicitudes.SolicitudDtos.ResultadoAceptar;
import com.grupo3.nexo.solicitudes.SolicitudDtos.SolicitudDto;
import com.grupo3.nexo.solicitudes.SolicitudDtos.SolicitudNueva;
import com.grupo3.nexo.usuarios.Modalidad;
import com.grupo3.nexo.usuarios.Modo;
import com.grupo3.nexo.usuarios.Usuario;
import com.grupo3.nexo.usuarios.UsuarioService;

/**
 * RF5 y RF6: enviar una solicitud y responderla. Aceptar, rechazar y cancelar toman la fila con `FOR UPDATE`, y lo que
 * no se puede comprobar de antemano sin carreras (dos aceptaciones a la misma hora) lo resuelve la base; ver
 * `ManejadorErrores`.
 */
@Service
public class SolicitudService {

    private static final int DIAS_SOLICITABLES = 14;
    private static final Duration PLAZO = Duration.ofHours(24);
    private static final Duration DURACION_SESION = Duration.ofHours(1);

    private static final String CONSULTA = """
        SELECT so.id, so.tutor_id, t.nombre AS nombre_tutor, so.estudiante_id, e.nombre AS nombre_estudiante,
               so.materia, so.tema, so.inicio, so.modalidad, so.estado, so.mensaje, so.motivo_rechazo,
               so.enviada_en, so.vence_en, so.precio_propuesto, fn_penalizaciones_recientes(so.estudiante_id, FALSE) AS penalizaciones_estudiante
        FROM solicitudes so
        JOIN usuarios t ON t.id = so.tutor_id
        JOIN usuarios e ON e.id = so.estudiante_id
        """;

    private final JdbcClient jdbc;
    private final UsuarioService usuarios;
    private final HoraPublicadaRepository horas;
    private final TutoriasAlDia alDia;
    private final SesionTutoriaService sesiones;
    private final ChatService chats;
    private final Penalizaciones penalizaciones;
    private final Clock reloj;

    public SolicitudService(JdbcClient jdbc, UsuarioService usuarios, HoraPublicadaRepository horas, TutoriasAlDia alDia,
                            SesionTutoriaService sesiones, ChatService chats, Penalizaciones penalizaciones, Clock reloj) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.horas = horas;
        this.alDia = alDia;
        this.sesiones = sesiones;
        this.chats = chats;
        this.penalizaciones = penalizaciones;
        this.reloj = reloj;
    }

    @Transactional
    public SolicitudDto enviar(UUID yo, SolicitudNueva s) {
        usuarios.cuentaEnSesion(yo);
        alDia.poner();
        penalizaciones.exigirSinPausa(yo);
        Usuario tutor = usuarios.tutorPublico(s.tutorId());
        if (tutor.getId().equals(yo)) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "SOLICITUD_A_SI_MISMO", "No puedes solicitarte una tutoría a ti mismo");
        }
        // Del catálogo ya salió, pero se puede llegar a su perfil desde el chat o el historial
        if (penalizaciones.pausaHasta(tutor.getId()).isPresent()) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "TUTOR_EN_PAUSA", "Este tutor no está recibiendo solicitudes por ahora");
        }
        LocalDate hoy = HoraColombia.hoy(reloj);
        if (s.fecha().isBefore(hoy.plusDays(1)) || s.fecha().isAfter(hoy.plusDays(DIAS_SOLICITABLES))) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "FECHA_NO_VALIDA", "Elige una fecha desde mañana hasta dentro de 14 días");
        }
        String materia = materiaOfrecida(tutor, s.materia());
        LocalTime hora = LocalTime.parse(s.hora());
        validarHora(tutor.getId(), s.fecha(), hora, s.modalidad());
        Instant inicio = s.fecha().atTime(hora).atZone(HoraColombia.ZONA).toInstant();
        validarHoraLibre(tutor.getId(), inicio);

        // El índice uq_solicitudes_activa cubre las peticiones simultáneas; esto da el mismo error sin provocar el choque
        boolean repetida = jdbc.sql("""
            SELECT EXISTS (SELECT 1 FROM solicitudes WHERE estudiante_id = ? AND tutor_id = ? AND inicio = ?
                           AND estado IN ('PENDIENTE', 'ACEPTADA'))
            """).params(yo, tutor.getId(), momento(inicio)).query(Boolean.class).single();
        if (repetida) {
            throw solicitudDuplicada();
        }

        UUID id = UUID.randomUUID();
        // La base guarda microsegundos: truncar aquí mantiene exacta la igualdad de ck_solicitudes_plazo
        Instant enviada = reloj.instant().truncatedTo(ChronoUnit.MICROS);
        Instant vence = enviada.plus(PLAZO).isBefore(inicio) ? enviada.plus(PLAZO) : inicio;
        jdbc.sql("""
            INSERT INTO solicitudes (id, estudiante_id, tutor_id, materia, tema, inicio, modalidad, mensaje, enviada_en, vence_en,
                                     precio_propuesto)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """).params(id, yo, tutor.getId(), materia, s.tema().trim(), momento(inicio), s.modalidad().name(),
                s.mensaje() == null ? "" : s.mensaje().trim(), momento(enviada), momento(vence), s.precioPropuesto()).update();
        return buscar(id).orElseThrow();
    }

    /** Se guarda como la escribió el tutor, no como la envió la app. */
    private static String materiaOfrecida(Usuario tutor, String pedida) {
        String limpia = pedida.trim();
        return tutor.getMaterias().stream().filter(limpia::equalsIgnoreCase).findFirst()
            .orElseThrow(() -> new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "MATERIA_NO_OFRECIDA", "Este tutor no enseña esa materia"));
    }

    private void validarHora(UUID tutor, LocalDate fecha, LocalTime hora, Modalidad modalidad) {
        List<HoraPublicada> publicadas = HorasCalendario.HORAS.contains(hora)
            ? horas.findByUsuarioIdAndDiaAndHora(tutor, DiaSemana.de(fecha), hora)
            : List.of();
        if (publicadas.isEmpty()) {
            throw horaNoDisponible();
        }
        if (publicadas.stream().noneMatch(h -> h.getModalidad() == modalidad)) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "MODALIDAD_NO_DISPONIBLE", "El tutor no ofrece esa modalidad a esa hora");
        }
    }

    private void validarHoraLibre(UUID tutor, Instant inicio) {
        boolean ocupada = jdbc.sql("""
            SELECT EXISTS (SELECT 1 FROM sesiones
                           WHERE (tutor_id = ? OR estudiante_id = ?) AND estado = 'PROGRAMADA' AND inicio < ? AND fin > ?)
            """).params(tutor, tutor, momento(inicio.plus(DURACION_SESION)), momento(inicio)).query(Boolean.class).single();
        if (ocupada) {
            throw horaNoDisponible();
        }
    }

    private static ErrorNegocio horaNoDisponible() {
        return new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "HORA_NO_DISPONIBLE", "El tutor no tiene esa hora disponible");
    }

    private static ErrorNegocio solicitudDuplicada() {
        return new ErrorNegocio(HttpStatus.CONFLICT, "SOLICITUD_DUPLICADA", "Ya tienes una solicitud con este tutor para ese día y hora");
    }

    /** Pendientes primero, la que vence antes arriba; después el resto, la más reciente primero. */
    @Transactional
    public List<SolicitudDto> listar(UUID yo, Modo modo, List<EstadoSolicitud> estados) {
        Modo efectivo = usuarios.modoDe(yo, modo);
        alDia.poner();
        String columna = efectivo == Modo.TUTOR ? "so.tutor_id" : "so.estudiante_id";
        String filtro = estados == null || estados.isEmpty() ? "" : " AND so.estado IN (:estados)";
        JdbcClient.StatementSpec consulta = jdbc.sql(CONSULTA + " WHERE " + columna + " = :yo" + filtro + """

            ORDER BY CASE WHEN so.estado = 'PENDIENTE' THEN 0 ELSE 1 END,
                     CASE WHEN so.estado = 'PENDIENTE' THEN so.vence_en END,
                     so.enviada_en DESC, so.id
            """).param("yo", yo);
        if (!filtro.isEmpty()) {
            consulta = consulta.param("estados", estados.stream().map(Enum::name).toList());
        }
        return consulta.query(SolicitudService::leer).list();
    }

    @Transactional
    public SolicitudDto una(UUID yo, String solicitudId) {
        usuarios.cuentaEnSesion(yo);
        alDia.poner();
        return Ids.leer(solicitudId).flatMap(this::buscar)
            .filter(s -> s.tutorId().equals(yo) || s.estudianteId().equals(yo))
            .orElseThrow(SolicitudService::noEncontrada);
    }

    @Transactional
    public ResultadoAceptar aceptar(UUID yo, String solicitudId) {
        usuarios.cuentaEnSesion(yo);
        alDia.poner();
        Bloqueada s = bloquear(yo, solicitudId);
        s.exigirTutor(yo, "aceptarla");
        s.exigirPendiente();
        penalizaciones.exigirSinPausa(yo);
        Instant fin = s.inicio().plus(DURACION_SESION);
        // Primero el chat: la sesión lo referencia. Sus choques de horario llegan como HORA_NO_DISPONIBLE (ManejadorErrores)
        UUID chat = chats.habilitar(s.estudianteId(), yo);
        cambiarEstado(s.id(), EstadoSolicitud.ACEPTADA, null);
        UUID sesion = sesiones.programar(s.id(), s.estudianteId(), yo, s.inicio(), fin);
        return new ResultadoAceptar(buscar(s.id()).orElseThrow(), sesiones.vista(yo, sesion), chats.vista(yo, chat));
    }

    @Transactional
    public SolicitudDto rechazar(UUID yo, String solicitudId, String motivo) {
        usuarios.cuentaEnSesion(yo);
        alDia.poner();
        Bloqueada s = bloquear(yo, solicitudId);
        s.exigirTutor(yo, "rechazarla");
        s.exigirPendiente();
        String texto = motivo == null || motivo.isBlank() ? null : motivo.trim();
        cambiarEstado(s.id(), EstadoSolicitud.RECHAZADA, texto);
        return buscar(s.id()).orElseThrow();
    }

    /** Una aceptada se puede cancelar hasta que empiece su sesión; el trigger de sesiones cierra el chat si ya no quedan. */
    @Transactional
    public SolicitudDto cancelar(UUID yo, String solicitudId) {
        usuarios.cuentaEnSesion(yo);
        alDia.poner();
        Bloqueada s = bloquear(yo, solicitudId);
        if (!s.estudianteId().equals(yo)) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "NO_ES_ESTUDIANTE", "Solo el estudiante que la envió puede cancelarla");
        }
        boolean aceptadaSinEmpezar = s.estado() == EstadoSolicitud.ACEPTADA && s.inicio().isAfter(reloj.instant());
        if (s.estado() != EstadoSolicitud.PENDIENTE && !aceptadaSinEmpezar) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "SOLICITUD_NO_CANCELABLE", "Esta solicitud ya no se puede cancelar");
        }
        if (aceptadaSinEmpezar) {
            sesiones.cancelarDe(s.id());
        }
        cambiarEstado(s.id(), EstadoSolicitud.CANCELADA, null);
        return buscar(s.id()).orElseThrow();
    }

    /** Lo necesario para decidir una respuesta, con la fila ya bloqueada hasta el final de la transacción. */
    private record Bloqueada(UUID id, UUID tutorId, UUID estudianteId, EstadoSolicitud estado, Instant inicio) {

        void exigirTutor(UUID yo, String accion) {
            if (!tutorId.equals(yo)) {
                throw new ErrorNegocio(HttpStatus.FORBIDDEN, "NO_ES_TUTOR", "Solo el tutor de la solicitud puede " + accion);
            }
        }

        void exigirPendiente() {
            if (estado == EstadoSolicitud.EXPIRADA) {
                throw new ErrorNegocio(HttpStatus.CONFLICT, "SOLICITUD_EXPIRADA", "Venció el plazo para responder esta solicitud");
            }
            if (estado != EstadoSolicitud.PENDIENTE) {
                throw new ErrorNegocio(HttpStatus.CONFLICT, "SOLICITUD_NO_PENDIENTE", "Esta solicitud ya no espera respuesta");
            }
        }
    }

    private Bloqueada bloquear(UUID yo, String solicitudId) {
        return Ids.leer(solicitudId).flatMap(id -> jdbc.sql("""
                SELECT id, tutor_id, estudiante_id, estado, inicio FROM solicitudes
                WHERE id = ? AND (tutor_id = ? OR estudiante_id = ?)
                FOR UPDATE
                """).params(id, yo, yo)
                .query((rs, n) -> new Bloqueada(rs.getObject("id", UUID.class), rs.getObject("tutor_id", UUID.class),
                    rs.getObject("estudiante_id", UUID.class), EstadoSolicitud.valueOf(rs.getString("estado")),
                    rs.getObject("inicio", OffsetDateTime.class).toInstant()))
                .optional())
            .orElseThrow(SolicitudService::noEncontrada);
    }

    private void cambiarEstado(UUID id, EstadoSolicitud estado, String motivo) {
        jdbc.sql("UPDATE solicitudes SET estado = ?, motivo_rechazo = ? WHERE id = ?")
            .params(estado.name(), motivo, id).update();
    }

    private Optional<SolicitudDto> buscar(UUID id) {
        return jdbc.sql(CONSULTA + " WHERE so.id = ?").param(id).query(SolicitudService::leer).optional();
    }

    private static ErrorNegocio noEncontrada() {
        return new ErrorNegocio(HttpStatus.NOT_FOUND, "SOLICITUD_NO_ENCONTRADA", "No existe esa solicitud");
    }

    private static OffsetDateTime momento(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }

    private static SolicitudDto leer(ResultSet rs, int fila) throws SQLException {
        var inicio = rs.getObject("inicio", OffsetDateTime.class).toInstant().atZone(HoraColombia.ZONA);
        return new SolicitudDto(rs.getObject("id", UUID.class), rs.getObject("tutor_id", UUID.class), rs.getString("nombre_tutor"),
            rs.getObject("estudiante_id", UUID.class), rs.getString("nombre_estudiante"), rs.getString("materia"),
            rs.getString("tema"), inicio.toLocalDate(), HorasCalendario.texto(inicio.toLocalTime()),
            Modalidad.valueOf(rs.getString("modalidad")), EstadoSolicitud.valueOf(rs.getString("estado")),
            rs.getString("mensaje"), rs.getString("motivo_rechazo"),
            HoraColombia.momento(rs.getObject("enviada_en", OffsetDateTime.class).toInstant()),
            HoraColombia.momento(rs.getObject("vence_en", OffsetDateTime.class).toInstant()),
            rs.getInt("penalizaciones_estudiante"), rs.getBigDecimal("precio_propuesto"));
    }
}
