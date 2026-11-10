package com.grupo3.nexo.sesiones;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.chats.ChatService;
import com.grupo3.nexo.config.HoraColombia;
import com.grupo3.nexo.config.Ids;
import com.grupo3.nexo.disponibilidad.HorasCalendario;
import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.sesiones.SesionDtos.SesionTutoria;
import com.grupo3.nexo.sesiones.SesionDtos.SolicitudAcuerdo;
import com.grupo3.nexo.sesiones.SesionDtos.SolicitudIniciarSesion;
import com.grupo3.nexo.sesiones.SesionDtos.TipoAcuerdo;
import com.grupo3.nexo.usuarios.Modalidad;
import com.grupo3.nexo.usuarios.UsuarioService;

/**
 * Que los dos lleguen a una sesión presencial: el tutor envía el PIN al llegar, el estudiante se lo dice y el tutor lo
 * escribe. Lo que pasa si nadie lo hace a tiempo está en {@link TutoriasAlDia}. Quien va tarde puede pactar por chat una
 * nueva hora. Cancelar de común acuerdo, sin penalización, vale para cualquier sesión programada, también las virtuales:
 * es la única forma que tiene el tutor de cancelar.
 *
 * Todo toma la fila de la sesión con `FOR UPDATE`: así, un acuerdo no se cruza con la puesta al día ni con el PIN.
 */
@Service
public class AsistenciaService {

    /** Desde cuándo se puede enviar el PIN: el tutor puede llegar un poco antes de la hora. */
    private static final Duration ANTICIPO_PIN = Duration.ofMinutes(30);
    /** Con 5 intentos cada 5 minutos no se alcanza a adivinar un PIN de 4 cifras en lo que dura una sesión. */
    private static final int INTENTOS_PIN = 5;
    private static final Duration BLOQUEO_PIN = Duration.ofMinutes(5);
    /** Tras la nueva hora deben quedar estos minutos para llegar antes de que termine la sesión. */
    private static final Duration TOLERANCIA = Duration.ofMinutes(10);

    private final JdbcClient jdbc;
    private final UsuarioService usuarios;
    private final TutoriasAlDia alDia;
    private final SesionTutoriaService sesiones;
    private final ChatService chats;
    private final Clock reloj;

    public AsistenciaService(JdbcClient jdbc, UsuarioService usuarios, TutoriasAlDia alDia, SesionTutoriaService sesiones,
                             ChatService chats, Clock reloj) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.alDia = alDia;
        this.sesiones = sesiones;
        this.chats = chats;
        this.reloj = reloj;
    }

    /**
     * Para quien cree que su penalización fue un error: una por penalización y dentro de los 7 días siguientes, mientras
     * los dos recuerdan lo que pasó. No detiene la pausa. La columna `revisable` de la consulta aplica las mismas reglas.
     */
    @Transactional
    public SesionTutoria pedirRevision(UUID yo, String sesionId, String motivo) {
        usuarios.cuentaEnSesion(yo);
        alDia.poner();
        UUID sesion = Ids.leer(sesionId).map(id -> sesiones.vista(yo, id).id()).orElseThrow(() ->
            new ErrorNegocio(HttpStatus.NOT_FOUND, "SESION_NO_ENCONTRADA", "No existe esa sesión"));
        var penalizacion = jdbc.sql("""
                SELECT id, creado_en > now() - INTERVAL '7 days' AS a_tiempo FROM penalizaciones
                WHERE sesion_id = ? AND usuario_id = ?
                """).params(sesion, yo)
            .query((rs, n) -> Map.entry(rs.getObject("id", UUID.class), rs.getBoolean("a_tiempo"))).optional()
            .orElseThrow(() -> new ErrorNegocio(HttpStatus.CONFLICT, "SIN_PENALIZACION", "Esta sesión no te dejó ninguna penalización"));
        if (!penalizacion.getValue()) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "REVISION_FUERA_DE_PLAZO", "Pasaron los 7 días para pedir la revisión");
        }
        int creadas = jdbc.sql("""
            INSERT INTO revisiones_penalizacion (id, penalizacion_id, motivo) VALUES (?, ?, ?)
            ON CONFLICT (penalizacion_id) DO NOTHING
            """).params(UUID.randomUUID(), penalizacion.getKey(), motivo).update();
        if (creadas == 0) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "REVISION_DUPLICADA", "Ya pediste la revisión de esta penalización");
        }
        return sesiones.vista(yo, sesion);
    }

    private record Fila(UUID id, UUID solicitud, UUID tutor, UUID estudiante, Instant inicio, Instant fin, String estado,
                        String modalidad, String pin, boolean pinEnviado, int intentos, Instant bloqueadaHasta,
                        boolean iniciada, boolean conRetardo) {

        boolean esTutor(UUID yo) {
            return tutor.equals(yo);
        }
    }

    /** Repetirlo no es un error: la respuesta anterior pudo perderse en la red. */
    @Transactional
    public SesionTutoria enviarPin(UUID yo, String sesionId) {
        Fila s = presencialProgramada(yo, sesionId);
        if (!s.esTutor(yo)) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "NO_ES_TUTOR", "Solo el tutor de la sesión envía el PIN");
        }
        if (!s.pinEnviado() && !s.iniciada()) {
            if (reloj.instant().isBefore(s.inicio().minus(ANTICIPO_PIN))) {
                throw new ErrorNegocio(HttpStatus.CONFLICT, "SESION_AUN_NO_EMPIEZA",
                    "Podrás enviarlo desde " + ANTICIPO_PIN.toMinutes() + " minutos antes de la hora");
            }
            jdbc.sql("UPDATE sesiones SET pin_enviado_en = now() WHERE id = ?").param(s.id()).update();
        }
        return sesiones.vista(yo, s.id());
    }

    /**
     * Repetir el envío de una sesión ya iniciada no es un error: el primero pudo perderse en la red. Un intento fallido
     * debe quedar guardado aunque la petición termine en error.
     */
    @Transactional(noRollbackFor = ErrorNegocio.class)
    public SesionTutoria iniciar(UUID yo, String sesionId, SolicitudIniciarSesion p) {
        Fila s = presencialProgramada(yo, sesionId);
        if (!s.esTutor(yo)) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "NO_ES_TUTOR", "Solo el tutor de la sesión puede iniciarla");
        }
        if (!s.iniciada()) {
            if (!s.pinEnviado()) {
                throw new ErrorNegocio(HttpStatus.CONFLICT, "PIN_SIN_ENVIAR", "Primero envía el PIN al estudiante");
            }
            comprobarPin(s, p.pin());
        }
        return sesiones.vista(yo, s.id());
    }

    private void comprobarPin(Fila s, String pin) {
        Instant ahora = reloj.instant();
        if (s.bloqueadaHasta() != null && ahora.isBefore(s.bloqueadaHasta())) {
            throw pinBloqueado(Duration.between(ahora, s.bloqueadaHasta()));
        }
        // Pasado el bloqueo, los intentos vuelven a empezar
        int gastados = s.bloqueadaHasta() != null ? 0 : s.intentos();
        if (MessageDigest.isEqual(s.pin().getBytes(StandardCharsets.UTF_8), pin.getBytes(StandardCharsets.UTF_8))) {
            jdbc.sql("UPDATE sesiones SET iniciada_en = now(), pin_intentos = 0, pin_bloqueado_hasta = NULL WHERE id = ?")
                .param(s.id()).update();
            return;
        }
        int intentos = gastados + 1;
        if (intentos >= INTENTOS_PIN) {
            jdbc.sql("UPDATE sesiones SET pin_intentos = 0, pin_bloqueado_hasta = ? WHERE id = ?")
                .params(ahora.plus(BLOQUEO_PIN).atOffset(ZoneOffset.UTC), s.id()).update();
            throw pinBloqueado(BLOQUEO_PIN);
        }
        jdbc.sql("UPDATE sesiones SET pin_intentos = ?, pin_bloqueado_hasta = NULL WHERE id = ?").params(intentos, s.id()).update();
        int quedan = INTENTOS_PIN - intentos;
        throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "PIN_INCORRECTO",
            "El PIN no coincide. " + (quedan == 1 ? "Te queda 1 intento." : "Te quedan " + quedan + " intentos."));
    }

    private static ErrorNegocio pinBloqueado(Duration espera) {
        return new ErrorNegocio(HttpStatus.TOO_MANY_REQUESTS, "PIN_BLOQUEADO",
            "Demasiados intentos con un PIN incorrecto", List.of(), (int) Math.max(1, espera.toSeconds()));
    }

    /**
     * Quien propone un retardo es quien va tarde. Proponer no detiene el reloj: si el otro no responde a tiempo, la
     * sesión se vence igual.
     */
    @Transactional
    public SesionTutoria proponer(UUID yo, String sesionId, SolicitudAcuerdo p) {
        Fila s = sinIniciar(yo, sesionId);
        if (p.tipo() == TipoAcuerdo.RETARDO) {
            exigirPresencial(s);
        }
        if (hayPropuesta(s.id())) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "ACUERDO_PENDIENTE", "Ya hay una propuesta sin responder");
        }
        Instant nuevaHora = null;
        if (p.tipo() == TipoAcuerdo.RETARDO) {
            nuevaHora = horaDelRetardo(s, p.minutos());
        }
        jdbc.sql("INSERT INTO acuerdos_sesion (id, sesion_id, tipo, propuesto_por, nueva_hora) VALUES (?, ?, ?, ?, ?)")
            .params(UUID.randomUUID(), s.id(), p.tipo().name(), yo, nuevaHora == null ? null : nuevaHora.atOffset(ZoneOffset.UTC))
            .update();
        chats.anotar(s.estudiante(), s.tutor(), yo, p.tipo() == TipoAcuerdo.RETARDO
            ? "Voy a llegar tarde. Propongo que empecemos a las " + legible(nuevaHora) + "."
            : "Propongo que cancelemos esta sesión.");
        return sesiones.vista(yo, s.id());
    }

    private Instant horaDelRetardo(Fila s, Integer minutos) {
        if (minutos == null) {
            throw ErrorNegocio.datosInvalidos("minutos", "es obligatorio en un retardo");
        }
        if (s.conRetardo()) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "RETARDO_YA_ACORDADO", "Esta sesión ya tuvo un retardo pactado");
        }
        Instant nueva = s.inicio().plus(Duration.ofMinutes(minutos));
        if (!nueva.isAfter(reloj.instant()) || nueva.plus(TOLERANCIA).isAfter(s.fin())) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "RETARDO_NO_VALIDO", "Esa hora ya pasó o no alcanza para la sesión");
        }
        return nueva;
    }

    /** Solo la otra parte puede aceptar. Aceptar una cancelación la cierra sin penalización para nadie. */
    @Transactional
    public SesionTutoria aceptar(UUID yo, String sesionId) {
        Fila s = sinIniciar(yo, sesionId);
        Propuesta p = propuesta(s.id());
        if (p.de().equals(yo)) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "ACUERDO_PROPIO", "Debe aceptarlo la otra persona");
        }
        if (p.tipo() == TipoAcuerdo.RETARDO) {
            if (!p.nuevaHora().isAfter(reloj.instant())) {
                throw new ErrorNegocio(HttpStatus.CONFLICT, "RETARDO_NO_VALIDO", "Esa hora ya pasó");
            }
            jdbc.sql("UPDATE sesiones SET hora_pactada = ?, retardo_de = ? WHERE id = ?")
                .params(p.nuevaHora().atOffset(ZoneOffset.UTC), p.de(), s.id()).update();
            chats.anotar(s.estudiante(), s.tutor(), yo, "De acuerdo, empezamos a las " + legible(p.nuevaHora()) + ".");
        } else {
            // Antes de cancelar: al cancelar, el chat pasa a solo lectura
            chats.anotar(s.estudiante(), s.tutor(), yo, "De acuerdo, cancelamos la sesión.");
            jdbc.sql("UPDATE sesiones SET estado = 'CANCELADA', cierre = 'ACORDADA' WHERE id = ?").param(s.id()).update();
            jdbc.sql("UPDATE solicitudes SET estado = 'CANCELADA' WHERE id = ? AND estado = 'ACEPTADA'").param(s.solicitud()).update();
        }
        resolver(s.id(), "ACEPTADO");
        return sesiones.vista(yo, s.id());
    }

    /** El otro la rechaza; quien la propuso la retira. Después se puede proponer otra. */
    @Transactional
    public SesionTutoria rechazar(UUID yo, String sesionId) {
        Fila s = sinIniciar(yo, sesionId);
        Propuesta p = propuesta(s.id());
        resolver(s.id(), "RECHAZADO");
        chats.anotar(s.estudiante(), s.tutor(), yo, p.de().equals(yo) ? "Retiré mi propuesta." : "No acepto la propuesta.");
        return sesiones.vista(yo, s.id());
    }

    private record Propuesta(TipoAcuerdo tipo, UUID de, Instant nuevaHora) {
    }

    private boolean hayPropuesta(UUID sesion) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM acuerdos_sesion WHERE sesion_id = ? AND estado = 'PROPUESTO')")
            .param(sesion).query(Boolean.class).single();
    }

    private Propuesta propuesta(UUID sesion) {
        return jdbc.sql("SELECT tipo, propuesto_por, nueva_hora FROM acuerdos_sesion WHERE sesion_id = ? AND estado = 'PROPUESTO'")
            .param(sesion)
            .query((rs, n) -> new Propuesta(TipoAcuerdo.valueOf(rs.getString("tipo")), rs.getObject("propuesto_por", UUID.class),
                instanteONulo(rs, "nueva_hora")))
            .optional()
            .orElseThrow(() -> new ErrorNegocio(HttpStatus.CONFLICT, "ACUERDO_NO_PENDIENTE", "No hay ninguna propuesta por responder"));
    }

    private void resolver(UUID sesion, String estado) {
        jdbc.sql("UPDATE acuerdos_sesion SET estado = ?, resuelto_en = now() WHERE sesion_id = ? AND estado = 'PROPUESTO'")
            .params(estado, sesion).update();
    }

    /** Una virtual nunca se "inicia": admite acuerdos mientras siga programada. */
    private Fila sinIniciar(UUID yo, String sesionId) {
        Fila s = programada(yo, sesionId);
        if (s.iniciada()) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "SESION_YA_INICIADA", "La sesión ya empezó");
        }
        return s;
    }

    private Fila presencialProgramada(UUID yo, String sesionId) {
        Fila s = programada(yo, sesionId);
        exigirPresencial(s);
        return s;
    }

    private static void exigirPresencial(Fila s) {
        if (!Modalidad.PRESENCIAL.name().equals(s.modalidad())) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "SESION_SIN_PIN", "Solo las sesiones presenciales usan PIN");
        }
    }

    /** Después de la puesta al día: si se venció justo ahora, responde que ya no está programada. */
    private Fila programada(UUID yo, String sesionId) {
        usuarios.cuentaEnSesion(yo);
        alDia.poner();
        UUID id = Ids.leer(sesionId).orElseThrow(AsistenciaService::noEncontrada);
        // Primero la solicitud, en el mismo orden que al cancelarla: si no, las dos operaciones podrían interbloquearse
        jdbc.sql("""
            SELECT so.id FROM solicitudes so JOIN sesiones s ON s.solicitud_id = so.id
            WHERE s.id = :id AND (s.tutor_id = :yo OR s.estudiante_id = :yo)
            FOR UPDATE OF so
            """).param("id", id).param("yo", yo).query(UUID.class).optional();
        Fila s = jdbc.sql("""
            SELECT s.id, s.solicitud_id, s.tutor_id, s.estudiante_id, s.inicio, s.fin, s.estado, so.modalidad, s.pin,
                   s.pin_enviado_en, s.pin_intentos, s.pin_bloqueado_hasta, s.iniciada_en, s.hora_pactada
            FROM sesiones s JOIN solicitudes so ON so.id = s.solicitud_id
            WHERE s.id = :id AND (s.tutor_id = :yo OR s.estudiante_id = :yo)
            FOR UPDATE OF s
            """).param("id", id).param("yo", yo)
            .query((rs, n) -> new Fila(rs.getObject("id", UUID.class), rs.getObject("solicitud_id", UUID.class),
                rs.getObject("tutor_id", UUID.class), rs.getObject("estudiante_id", UUID.class),
                instanteONulo(rs, "inicio"), instanteONulo(rs, "fin"), rs.getString("estado"), rs.getString("modalidad"),
                rs.getString("pin"), rs.getObject("pin_enviado_en") != null, rs.getInt("pin_intentos"),
                instanteONulo(rs, "pin_bloqueado_hasta"), rs.getObject("iniciada_en") != null, rs.getObject("hora_pactada") != null))
            .optional().orElseThrow(AsistenciaService::noEncontrada);
        if (!EstadoSesion.PROGRAMADA.name().equals(s.estado())) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "SESION_NO_PROGRAMADA", "Esta sesión ya no está programada");
        }
        return s;
    }

    private static Instant instanteONulo(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : valor.toInstant();
    }

    /** Como la muestra la app. */
    private static String legible(Instant instante) {
        return HorasCalendario.texto(instante.atZone(HoraColombia.ZONA).toLocalTime());
    }

    private static ErrorNegocio noEncontrada() {
        return new ErrorNegocio(HttpStatus.NOT_FOUND, "SESION_NO_ENCONTRADA", "No existe esa sesión");
    }
}
