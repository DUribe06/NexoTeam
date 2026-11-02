package com.grupo3.nexo.tutores;

import java.text.Normalizer;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.grupo3.nexo.usuarios.EstadoCuenta;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * La consulta del catálogo (RF3). Es SQL y no JPQL porque ignorar las tildes y ordenar por un promedio que no se guarda
 * necesita funciones de la base. Todo lo que usa existe igual en PostgreSQL y en H2.
 */
@Component
class BusquedaTutores {

    /**
     * Quita tildes con TRANSLATE y solo después pasa a minúsculas: así no depende del idioma configurado en la base,
     * que con el idioma C dejaría intactas las letras con tilde.
     */
    private static final String CON_TILDE = "ÁÀÄÂÃÉÈËÊÍÌÏÎÓÒÖÔÕÚÙÜÛÑÇáàäâãéèëêíìïîóòöôõúùüûñç";
    private static final String SIN_TILDE = "AAAAAEEEEIIIIOOOOOUUUUNCaaaaaeeeeiiiiooooouuuunc";

    private static final String MATERIA_DEL_TUTOR = "SELECT 1 FROM tutor_materias m WHERE m.usuario_id = u.id";

    private final EntityManager em;

    BusquedaTutores(EntityManager em) {
        this.em = em;
    }

    long contar(UUID yo, FiltrosTutores filtros, Instant conectadosDesde) {
        Consulta consulta = new Consulta(yo, filtros, conectadosDesde);
        Query query = em.createNativeQuery("SELECT COUNT(*) FROM usuarios u WHERE " + consulta.condiciones);
        consulta.parametros.forEach(query::setParameter);
        return ((Number) query.getSingleResult()).longValue();
    }

    /** Los ids de una página, de mayor a menor promedio; quien tiene más reseñas va primero si empatan. */
    @SuppressWarnings("unchecked")
    List<UUID> pagina(UUID yo, FiltrosTutores filtros, Instant conectadosDesde, int pagina, int tamano) {
        Consulta consulta = new Consulta(yo, filtros, conectadosDesde);
        Query query = em.createNativeQuery("""
            SELECT u.id FROM usuarios u
            LEFT JOIN (SELECT tutor_id, AVG(CAST(estrellas AS DOUBLE PRECISION)) AS promedio, COUNT(*) AS total
                       FROM resenas GROUP BY tutor_id) r ON r.tutor_id = u.id
            WHERE %s
            ORDER BY COALESCE(r.promedio, 0) DESC, COALESCE(r.total, 0) DESC, u.nombre, u.id
            """.formatted(consulta.condiciones), UUID.class);
        consulta.parametros.forEach(query::setParameter);
        return query.setFirstResult(pagina * tamano).setMaxResults(tamano).getResultList();
    }

    /** Las condiciones de la consulta y sus parámetros; lo que escribe el usuario siempre va como parámetro. */
    private static final class Consulta {

        final Map<String, Object> parametros = new HashMap<>();
        final String condiciones;

        Consulta(UUID yo, FiltrosTutores f, Instant conectadosDesde) {
            parametros.put("yo", yo);
            parametros.put("activa", EstadoCuenta.ACTIVA.name());
            StringBuilder sql = new StringBuilder("""
                u.estado_cuenta = :activa AND u.perfil_tutor_activo = TRUE AND u.id <> :yo
                AND EXISTS (%s)
                """.formatted(MATERIA_DEL_TUTOR));
            sql.append(" AND EXISTS (").append(horaPublicada(f)).append(")");

            List<String> claves = f.clavesDeMateria();
            if (!claves.isEmpty()) {
                sql.append(" AND EXISTS (").append(MATERIA_DEL_TUTOR).append(" AND (");
                for (int i = 0; i < claves.size(); i++) {
                    sql.append(i == 0 ? "" : " OR ").append(contiene("m.materia", "clave" + i));
                    parametros.put("clave" + i, patron(claves.get(i)));
                }
                sql.append("))");
            }
            if (!f.q().isEmpty()) {
                parametros.put("q", patron(f.q()));
                sql.append(" AND (").append(contiene("u.nombre", "q"))
                    .append(" OR ").append(contiene("COALESCE(u.universidad, '')", "q"))
                    .append(" OR EXISTS (").append(MATERIA_DEL_TUTOR).append(" AND ").append(contiene("m.materia", "q"))
                    .append("))");
            }
            if (f.enLinea()) {
                parametros.put("conectadosDesde", conectadosDesde);
                sql.append(" AND u.ultima_actividad_en >= :conectadosDesde");
            }
            condiciones = sql.toString();
        }

        /** Modalidad y franja se evalúan sobre la misma fila: "virtual en la noche" exige una hora nocturna virtual. */
        private String horaPublicada(FiltrosTutores f) {
            StringBuilder sql = new StringBuilder("SELECT 1 FROM tutor_disponibilidad d WHERE d.usuario_id = u.id");
            if (f.modalidad() != null) {
                parametros.put("modalidad", f.modalidad().name());
                sql.append(" AND d.modalidad = :modalidad");
            }
            if (f.franja() != null) {
                parametros.put("desde", f.franja().desde());
                parametros.put("hasta", f.franja().hasta());
                sql.append(" AND d.hora >= :desde AND d.hora < :hasta");
            }
            return sql.toString();
        }

        private static String contiene(String columna, String parametro) {
            return "LOWER(TRANSLATE(%s, '%s', '%s')) LIKE :%s ESCAPE '!'".formatted(columna, CON_TILDE, SIN_TILDE, parametro);
        }

        /** El texto buscado se compara tal cual: un % o un _ que escriba el usuario no son comodines. */
        private static String patron(String texto) {
            String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
            return "%" + normalizado.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        }
    }
}
