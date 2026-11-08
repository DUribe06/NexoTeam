package com.grupo3.nexo.admin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.admin.AdminDtos.ReporteAdmin;
import com.grupo3.nexo.admin.AdminDtos.RevisionAdmin;
import com.grupo3.nexo.admin.AdminDtos.UsuarioAdmin;
import com.grupo3.nexo.config.HoraColombia;
import com.grupo3.nexo.config.Ids;
import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.sesiones.TutoriasAlDia;
import com.grupo3.nexo.usuarios.EstadoCuenta;

/** RF12: reportes, revisiones de penalizaciones y suspensión de cuentas. El rol lo exige `SeguridadConfig`. */
@Service
public class AdminService {

    private static final int MAXIMO_USUARIOS = 50;

    private final JdbcClient jdbc;
    private final TutoriasAlDia alDia;

    public AdminService(JdbcClient jdbc, TutoriasAlDia alDia) {
        this.jdbc = jdbc;
        this.alDia = alDia;
    }

    /** Abiertos primero, el más antiguo arriba: es el que lleva más tiempo esperando. */
    @Transactional(readOnly = true)
    public List<ReporteAdmin> reportes() {
        return jdbc.sql("""
            SELECT r.id, r.tutor_id, t.nombre AS tutor, t.correo AS correo_tutor, r.reportante_id, q.nombre AS reportante,
                   r.motivo, r.creado_en, r.cerrado_en
            FROM reportes r JOIN usuarios t ON t.id = r.tutor_id JOIN usuarios q ON q.id = r.reportante_id
            ORDER BY r.cerrado_en IS NOT NULL, CASE WHEN r.cerrado_en IS NULL THEN r.creado_en END, r.cerrado_en DESC
            LIMIT 200
            """).query((rs, n) -> new ReporteAdmin(rs.getObject("id", UUID.class), rs.getObject("tutor_id", UUID.class),
                rs.getString("tutor"), rs.getString("correo_tutor"), rs.getObject("reportante_id", UUID.class),
                rs.getString("reportante"), rs.getString("motivo"), momento(rs, "creado_en"), momento(rs, "cerrado_en")))
            .list();
    }

    @Transactional
    public void cerrarReporte(String id) {
        int cerrados = Ids.leer(id).map(uuid -> jdbc.sql("UPDATE reportes SET cerrado_en = now() WHERE id = ? AND cerrado_en IS NULL")
            .param(uuid).update()).orElse(0);
        if (cerrados == 0) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "REPORTE_NO_ABIERTO", "Ese reporte no existe o ya estaba cerrado");
        }
    }

    @Transactional(readOnly = true)
    public List<RevisionAdmin> revisionesAbiertas() {
        return jdbc.sql("""
            SELECT r.id, u.id AS usuario_id, u.nombre, u.correo, p.nivel, s.cierre, so.materia, s.inicio,
                   p.creado_en AS penalizada_en, r.motivo, r.creado_en
            FROM revisiones_penalizacion r
            JOIN penalizaciones p ON p.id = r.penalizacion_id
            JOIN usuarios u ON u.id = p.usuario_id
            JOIN sesiones s ON s.id = p.sesion_id
            JOIN solicitudes so ON so.id = s.solicitud_id
            WHERE r.estado = 'ABIERTA'
            ORDER BY r.creado_en
            """).query((rs, n) -> new RevisionAdmin(rs.getObject("id", UUID.class), rs.getObject("usuario_id", UUID.class),
                rs.getString("nombre"), rs.getString("correo"), rs.getString("nivel"), rs.getString("cierre"),
                rs.getString("materia"), momento(rs, "inicio"), momento(rs, "penalizada_en"), rs.getString("motivo"),
                momento(rs, "creado_en")))
            .list();
    }

    /** Aceptarla retira la penalización y, con ella, la pausa que causó: `penalizaciones_firmes` ya no la cuenta. */
    @Transactional
    public void resolverRevision(String id, boolean aceptada) {
        int resueltas = Ids.leer(id).map(uuid -> jdbc.sql("""
                UPDATE revisiones_penalizacion SET estado = ?, resuelto_en = now() WHERE id = ? AND estado = 'ABIERTA'
                """).params(aceptada ? "ACEPTADA" : "RECHAZADA", uuid).update()).orElse(0);
        if (resueltas == 0) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "REVISION_NO_ABIERTA", "Esa revisión no existe o ya estaba resuelta");
        }
    }

    /** Sin texto, las primeras cuentas por nombre. Lo escrito va como parámetro y sus comodines no cuentan. */
    @Transactional
    public List<UsuarioAdmin> usuarios(String texto) {
        alDia.poner();
        String buscado = texto == null ? "" : texto.trim().toLowerCase().replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return jdbc.sql("""
            SELECT u.id, u.nombre, u.correo, u.handle, u.estado_cuenta, u.perfil_tutor_activo,
                   fn_penalizaciones_recientes(u.id, TRUE) AS como_tutor,
                   fn_penalizaciones_recientes(u.id, FALSE) AS como_estudiante,
                   fn_pausa_hasta(u.id) AS pausa_hasta,
                   (SELECT COUNT(*) FROM reportes r WHERE r.tutor_id = u.id AND r.cerrado_en IS NULL) AS reportes_abiertos
            FROM usuarios u
            WHERE u.rol <> 'ADMIN' AND u.estado_cuenta <> 'ELIMINADA'
              AND (LOWER(u.nombre) LIKE :patron ESCAPE '!' OR LOWER(u.correo) LIKE :patron ESCAPE '!')
            ORDER BY u.nombre, u.id
            LIMIT :maximo
            """).param("patron", "%" + buscado + "%").param("maximo", MAXIMO_USUARIOS)
            .query((rs, n) -> new UsuarioAdmin(rs.getObject("id", UUID.class), rs.getString("nombre"), rs.getString("correo"),
                rs.getString("handle"), EstadoCuenta.valueOf(rs.getString("estado_cuenta")), rs.getBoolean("perfil_tutor_activo"),
                rs.getInt("como_tutor"), rs.getInt("como_estudiante"), momento(rs, "pausa_hasta"), rs.getLong("reportes_abiertos")))
            .list();
    }

    /** Suspender cierra todas sus sesiones. Sus tutorías agendadas no se tocan: decidirlo es parte de revisar el caso. */
    @Transactional
    public void suspender(String id) {
        UUID usuario = cambiarEstado(id, "ACTIVA", "SUSPENDIDA");
        jdbc.sql("DELETE FROM tokens_refresco WHERE usuario_id = ?").param(usuario).update();
    }

    @Transactional
    public void reactivar(String id) {
        cambiarEstado(id, "SUSPENDIDA", "ACTIVA");
    }

    private UUID cambiarEstado(String id, String desde, String hacia) {
        UUID usuario = Ids.leer(id).orElse(null);
        int cambiados = usuario == null ? 0 : jdbc.sql("""
            UPDATE usuarios SET estado_cuenta = ? WHERE id = ? AND estado_cuenta = ? AND rol <> 'ADMIN'
            """).params(hacia, usuario, desde).update();
        if (cambiados == 0) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "ESTADO_NO_CAMBIABLE", "Esa cuenta no existe o ya no está en ese estado");
        }
        return usuario;
    }

    private static OffsetDateTime momento(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : HoraColombia.momento(valor.toInstant());
    }
}
