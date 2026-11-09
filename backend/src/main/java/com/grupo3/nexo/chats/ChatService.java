package com.grupo3.nexo.chats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.chats.ChatDtos.ChatDto;
import com.grupo3.nexo.chats.ChatDtos.MensajeDto;
import com.grupo3.nexo.chats.ChatDtos.PaginaMensajes;
import com.grupo3.nexo.chats.ChatDtos.SolicitudMensaje;
import com.grupo3.nexo.config.HoraColombia;
import com.grupo3.nexo.config.Ids;
import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.usuarios.Modo;
import com.grupo3.nexo.usuarios.Usuario;
import com.grupo3.nexo.usuarios.UsuarioService;

/**
 * RF8: conversaciones entre estudiante y tutor. Quién puede escribir y cuándo lo decide la base: la secuencia y el
 * "solo lectura" los resuelve un trigger bajo el bloqueo del chat, para que no dependan del orden de las peticiones.
 */
@Service
public class ChatService {

    private static final int MENSAJES_POR_MINUTO = 30;
    private static final int LIMITE_MAXIMO = 100;

    /** Los no leídos solo cuentan lo que escribió la otra persona. */
    private static final String CONSULTA = """
        SELECT c.id, c.tutor_id, c.activo_hasta, c.ultimo_mensaje_en, o.id AS contraparte_id, o.nombre,
               o.ultima_actividad_en, o.foto_google, o.foto_url,
               COALESCE(NULLIF(m.texto, ''), CASE WHEN m.imagen_url IS NOT NULL THEN 'Foto' END, '') AS ultimo_texto,
               (SELECT COUNT(*) FROM mensajes n
                WHERE n.chat_id = c.id
                  AND n.secuencia > CASE WHEN c.tutor_id = :yo THEN c.leido_hasta_tutor ELSE c.leido_hasta_estudiante END
                  AND n.emisor = CASE WHEN c.tutor_id = :yo THEN 'ESTUDIANTE' ELSE 'TUTOR' END) AS no_leidos
        FROM chats c
        JOIN usuarios o ON o.id = CASE WHEN c.tutor_id = :yo THEN c.estudiante_id ELSE c.tutor_id END
        LEFT JOIN mensajes m ON m.chat_id = c.id AND m.secuencia = c.ultima_secuencia
        """;

    private final JdbcClient jdbc;
    private final UsuarioService usuarios;
    private final Clock reloj;

    public ChatService(JdbcClient jdbc, UsuarioService usuarios, Clock reloj) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    /**
     * Primero los nuevos sin mensajes, después el último mensaje más reciente. Un chat sin mensajes y en solo lectura no
     * se muestra: su sesión se canceló o terminó sin que nadie escribiera, y ya no admite que alguien lo haga.
     */
    @Transactional(readOnly = true)
    public List<ChatDto> bandeja(UUID yo, Modo modo) {
        Modo efectivo = usuarios.modoDe(yo, modo);
        String columna = efectivo == Modo.TUTOR ? "c.tutor_id" : "c.estudiante_id";
        return jdbc.sql(CONSULTA + " WHERE " + columna + " = :yo AND (c.ultima_secuencia > 0 OR c.activo_hasta > now())"
                + " ORDER BY c.ultimo_mensaje_en DESC NULLS FIRST, c.id")
            .param("yo", yo).query((rs, n) -> leer(rs, yo, reloj.instant())).list();
    }

    @Transactional(readOnly = true)
    public ChatDto con(UUID yo, String personaId, Modo modo) {
        Modo efectivo = usuarios.modoDe(yo, modo);
        String condicion = efectivo == Modo.TUTOR
            ? "c.tutor_id = :yo AND c.estudiante_id = :persona"
            : "c.estudiante_id = :yo AND c.tutor_id = :persona";
        return Ids.leer(personaId).flatMap(persona -> jdbc.sql(CONSULTA + " WHERE " + condicion)
                .param("yo", yo).param("persona", persona)
                .query((rs, n) -> leer(rs, yo, reloj.instant())).optional())
            .orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND, "CHAT_NO_HABILITADO",
                "El chat se habilita cuando el tutor acepta una solicitud"));
    }

    @Transactional(readOnly = true)
    public ChatDto uno(UUID yo, String chatId) {
        usuarios.cuentaEnSesion(yo);
        return Ids.leer(chatId).flatMap(id -> buscar(yo, id)).orElseThrow(ChatService::noEncontrado);
    }

    /** Del más antiguo al más reciente en los tres casos; `limite + 1` filas dicen si `hayMas`. */
    @Transactional(readOnly = true)
    public PaginaMensajes mensajes(UUID yo, String chatId, UUID antes, UUID desde, int limite) {
        usuarios.cuentaEnSesion(yo);
        Lado lado = lado(yo, chatId);
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw ErrorNegocio.datosInvalidos("limite", "debe estar entre 1 y 100");
        }
        if (antes != null && desde != null) {
            throw ErrorNegocio.datosInvalidos("desde", "no se puede usar junto con antes");
        }
        if (desde != null) {
            List<MensajeDto> filas = jdbc.sql(SELECCION_MENSAJE + " WHERE chat_id = :chat AND secuencia > :cursor ORDER BY secuencia LIMIT :filas")
                .param("chat", lado.chat()).param("cursor", secuenciaDe(lado.chat(), desde, "desde")).param("filas", limite + 1)
                .query((rs, n) -> leerMensaje(rs, lado.emisor())).list();
            boolean hayMas = filas.size() > limite;
            return new PaginaMensajes(hayMas ? filas.subList(0, limite) : filas, hayMas);
        }
        String condicion = antes == null ? "" : " AND secuencia < :cursor";
        JdbcClient.StatementSpec consulta = jdbc.sql(SELECCION_MENSAJE + " WHERE chat_id = :chat" + condicion + " ORDER BY secuencia DESC LIMIT :filas")
            .param("chat", lado.chat()).param("filas", limite + 1);
        if (antes != null) {
            consulta = consulta.param("cursor", secuenciaDe(lado.chat(), antes, "antes"));
        }
        List<MensajeDto> filas = consulta.query((rs, n) -> leerMensaje(rs, lado.emisor())).list();
        boolean hayMas = filas.size() > limite;
        List<MensajeDto> pagina = new ArrayList<>(hayMas ? filas.subList(0, limite) : filas);
        Collections.reverse(pagina);
        return new PaginaMensajes(pagina, hayMas);
    }

    private static final String SELECCION_MENSAJE = "SELECT id, chat_id, texto, imagen_url, enviado_en, emisor FROM mensajes";

    /**
     * Reintentar con el mismo `idCliente` devuelve el mensaje ya guardado, incluso si entretanto el chat pasó a solo
     * lectura: por eso se busca antes de insertar. `ON CONFLICT` cubre dos reintentos simultáneos.
     */
    @Transactional
    public MensajeDto enviar(UUID yo, String chatId, SolicitudMensaje s) {
        usuarios.cuentaEnSesion(yo);
        Lado lado = lado(yo, chatId);
        if (s.idCliente() != null) {
            Optional<MensajeDto> guardado = porIdCliente(lado, s.idCliente());
            if (guardado.isPresent()) {
                return guardado.get();
            }
        }
        // Con este tope por lado, un minuto cabe en los últimos 2 × MENSAJES_POR_MINUTO: no hace falta recorrer todo el chat
        int recientes = jdbc.sql("""
            SELECT COUNT(*) FROM mensajes
            WHERE chat_id = ? AND emisor = ? AND enviado_en > now() - INTERVAL '1 minute'
              AND secuencia > (SELECT ultima_secuencia - ? FROM chats WHERE id = ?)
            """).params(lado.chat(), lado.emisor(), 2 * MENSAJES_POR_MINUTO, lado.chat()).query(Integer.class).single();
        if (recientes >= MENSAJES_POR_MINUTO) {
            throw new ErrorNegocio(HttpStatus.TOO_MANY_REQUESTS, "DEMASIADAS_SOLICITUDES",
                "Estás enviando mensajes muy rápido; espera un momento", List.of(), 60);
        }
        return jdbc.sql("""
                INSERT INTO mensajes (id, chat_id, emisor, id_cliente, texto, imagen_url) VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (chat_id, emisor, id_cliente) DO NOTHING
                RETURNING id, chat_id, texto, imagen_url, enviado_en, emisor
                """).params(UUID.randomUUID(), lado.chat(), lado.emisor(), s.idCliente(), s.textoLimpio(), s.imagenUrl())
            .query((rs, n) -> leerMensaje(rs, lado.emisor())).optional()
            .orElseGet(() -> porIdCliente(lado, s.idCliente()).orElseThrow());
    }

    /** No es secreto que se leyó: funciona también en solo lectura, para que nada quede pendiente en la bandeja. */
    @Transactional
    public void marcarLeido(UUID yo, String chatId) {
        usuarios.cuentaEnSesion(yo);
        Lado lado = lado(yo, chatId);
        String columna = lado.emisor().equals("TUTOR") ? "leido_hasta_tutor" : "leido_hasta_estudiante";
        jdbc.sql("UPDATE chats SET " + columna + " = ultima_secuencia WHERE id = ?").param(lado.chat()).update();
    }

    /** Dentro de la transacción que acepta una solicitud; el trigger de sesiones lo activa al programar la sesión. */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID habilitar(UUID estudiante, UUID tutor) {
        jdbc.sql("""
            INSERT INTO chats (id, estudiante_id, tutor_id) VALUES (?, ?, ?)
            ON CONFLICT (estudiante_id, tutor_id) DO NOTHING
            """).params(UUID.randomUUID(), estudiante, tutor).update();
        return jdbc.sql("SELECT id FROM chats WHERE estudiante_id = ? AND tutor_id = ?")
            .params(estudiante, tutor).query(UUID.class).single();
    }

    /**
     * Deja escrito en el chat lo que `autor` hizo sobre una sesión, para que el acuerdo quede en la conversación. Va dentro
     * de la misma transacción y sin el tope de mensajes: no lo escribe la persona.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void anotar(UUID estudiante, UUID tutor, UUID autor, String texto) {
        jdbc.sql("""
            INSERT INTO mensajes (id, chat_id, emisor, texto)
            SELECT ?, id, ?, ? FROM chats WHERE estudiante_id = ? AND tutor_id = ?
            """).params(UUID.randomUUID(), autor.equals(tutor) ? "TUTOR" : "ESTUDIANTE", texto, estudiante, tutor).update();
    }

    /** Un chat en el que ya se sabe que participa `yo`. */
    public ChatDto vista(UUID yo, UUID chatId) {
        return buscar(yo, chatId).orElseThrow(ChatService::noEncontrado);
    }

    private Optional<ChatDto> buscar(UUID yo, UUID chatId) {
        return jdbc.sql(CONSULTA + " WHERE c.id = :id AND (c.tutor_id = :yo OR c.estudiante_id = :yo)")
            .param("id", chatId).param("yo", yo)
            .query((rs, n) -> leer(rs, yo, reloj.instant())).optional();
    }

    /** El chat y el lado desde el que `yo` escribe; si no participa, para esa cuenta no existe. */
    private record Lado(UUID chat, String emisor) {
    }

    private Lado lado(UUID yo, String chatId) {
        return Ids.leer(chatId).flatMap(id -> jdbc.sql("""
                SELECT id, CASE WHEN tutor_id = :yo THEN 'TUTOR' ELSE 'ESTUDIANTE' END AS emisor FROM chats
                WHERE id = :id AND (tutor_id = :yo OR estudiante_id = :yo)
                """).param("id", id).param("yo", yo)
                .query((rs, n) -> new Lado(rs.getObject("id", UUID.class), rs.getString("emisor"))).optional())
            .orElseThrow(ChatService::noEncontrado);
    }

    private long secuenciaDe(UUID chat, UUID mensaje, String campo) {
        return jdbc.sql("SELECT secuencia FROM mensajes WHERE id = ? AND chat_id = ?").params(mensaje, chat)
            .query(Long.class).optional()
            .orElseThrow(() -> ErrorNegocio.datosInvalidos(campo, "no es un mensaje de este chat"));
    }

    private Optional<MensajeDto> porIdCliente(Lado lado, UUID idCliente) {
        return jdbc.sql(SELECCION_MENSAJE + " WHERE chat_id = ? AND emisor = ? AND id_cliente = ?")
            .params(lado.chat(), lado.emisor(), idCliente)
            .query((rs, n) -> leerMensaje(rs, lado.emisor())).optional();
    }

    private static ErrorNegocio noEncontrado() {
        return new ErrorNegocio(HttpStatus.NOT_FOUND, "CHAT_NO_ENCONTRADO", "No existe esa conversación");
    }

    private static ChatDto leer(ResultSet rs, UUID yo, Instant ahora) throws SQLException {
        OffsetDateTime ultimo = rs.getObject("ultimo_mensaje_en", OffsetDateTime.class);
        OffsetDateTime activoHasta = rs.getObject("activo_hasta", OffsetDateTime.class);
        OffsetDateTime actividad = rs.getObject("ultima_actividad_en", OffsetDateTime.class);
        return new ChatDto(rs.getObject("id", UUID.class), rs.getObject("contraparte_id", UUID.class), rs.getString("nombre"),
            rs.getString("ultimo_texto"), ultimo == null ? null : HoraColombia.momento(ultimo.toInstant()),
            rs.getInt("no_leidos"),
            actividad != null && actividad.toInstant().isAfter(ahora.minus(Usuario.VENTANA_EN_LINEA)),
            activoHasta == null || !activoHasta.toInstant().isAfter(ahora),
            rs.getObject("tutor_id", UUID.class).equals(yo),
            Usuario.fotoUrl(rs.getString("foto_url"), rs.getString("foto_google")));
    }

    private static MensajeDto leerMensaje(ResultSet rs, String miLado) throws SQLException {
        return new MensajeDto(rs.getObject("id", UUID.class), rs.getObject("chat_id", UUID.class), rs.getString("texto"),
            rs.getString("imagen_url"),
            HoraColombia.momento(rs.getObject("enviado_en", OffsetDateTime.class).toInstant()),
            rs.getString("emisor").equals(miLado));
    }
}
