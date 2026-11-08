package com.grupo3.nexo.sesiones;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.grupo3.nexo.config.HoraColombia;
import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.usuarios.Modo;

/**
 * Qué efecto tiene una penalización. Una NORMAL suma al contador de los últimos 90 días, y la tercera en ese plazo pausa
 * la cuenta. Una GRAVE siempre la pausa. Durante la pausa no puede enviar ni aceptar solicitudes y sale del catálogo. Lo
 * ya agendado sigue en pie, porque cancelarlo castigaría a la otra persona. Las reglas viven en la base (V13 y V14), para
 * que el catálogo y la API no puedan diferir.
 */
@Component
public class Penalizaciones {

    /** Condición SQL del catálogo. `columna` debe ser fija en el código: nunca algo que escribió el usuario. */
    public static String sinPausa(String columna) {
        return "fn_pausa_hasta(" + columna + ") IS NULL";
    }

    private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("d 'de' MMMM, HH:mm", Locale.forLanguageTag("es-CO"));

    private final JdbcClient jdbc;

    public Penalizaciones(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Instant> pausaHasta(UUID usuario) {
        return jdbc.sql("SELECT fn_pausa_hasta(?)").param(usuario).query(OffsetDateTime.class).optional()
            .map(OffsetDateTime::toInstant);
    }

    public void exigirSinPausa(UUID usuario) {
        pausaHasta(usuario).ifPresent(hasta -> {
            // La hora evita que alguien lo intente ese mismo día, más temprano, y crea que falló
            String momento = MOMENTO.format(hasta.atZone(HoraColombia.ZONA));
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "CUENTA_EN_PAUSA",
                "Por tus penalizaciones, no puedes enviar ni aceptar solicitudes hasta el " + momento + ".");
        });
    }

    public int recientes(UUID usuario, Modo modo) {
        return jdbc.sql("SELECT fn_penalizaciones_recientes(?, ?)").params(usuario, modo == Modo.TUTOR)
            .query(Integer.class).single();
    }

    /** Para mostrarlas en el perfil público de cada tutor. */
    public Map<UUID, Integer> recientesComoTutor(Collection<UUID> tutores) {
        Map<UUID, Integer> cuentas = new HashMap<>();
        jdbc.sql("SELECT id, fn_penalizaciones_recientes(id, TRUE) AS total FROM usuarios WHERE id IN (:tutores)")
            .param("tutores", tutores)
            .query(rs -> {
                cuentas.put(rs.getObject("id", UUID.class), rs.getInt("total"));
            });
        return cuentas;
    }
}
