package com.grupo3.nexo.usuarios;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.sesiones.TutoriasAlDia;

/**
 * Eliminar una cuenta la anonimiza (Ley 1581): se van sus datos de identificación, pero las filas de sesiones, reseñas y
 * mensajes se quedan porque también son de otras personas. Con tutorías agendadas no se puede: dejaría plantado a alguien.
 */
@Service
public class EliminacionCuenta {

    private final JdbcClient jdbc;
    private final UsuarioService usuarios;
    private final TutoriasAlDia alDia;
    private final PasswordEncoder encriptador;

    public EliminacionCuenta(JdbcClient jdbc, UsuarioService usuarios, TutoriasAlDia alDia, PasswordEncoder encriptador) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.alDia = alDia;
        this.encriptador = encriptador;
    }

    @Transactional
    public void eliminar(UUID yo, String contrasena) {
        Usuario usuario = usuarios.cuentaEnSesion(yo);
        if (usuario.getRol() == Rol.ADMIN) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "ADMIN_SIN_MODOS", "Una cuenta de administrador no se elimina desde aquí");
        }
        if (!encriptador.matches(contrasena, usuario.getContrasenaHash())) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "CONTRASENA_INCORRECTA", "La contraseña no es correcta");
        }
        alDia.poner();
        boolean agendadas = jdbc.sql("""
            SELECT EXISTS (SELECT 1 FROM sesiones WHERE (tutor_id = ? OR estudiante_id = ?) AND estado = 'PROGRAMADA')
            """).params(yo, yo).query(Boolean.class).single();
        if (agendadas) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "CUENTA_CON_SESIONES", "Tienes tutorías agendadas: termínalas o cancélalas antes de eliminar tu cuenta");
        }
        jdbc.sql("UPDATE solicitudes SET estado = 'CANCELADA' WHERE estudiante_id = ? AND estado = 'PENDIENTE'").param(yo).update();
        jdbc.sql("UPDATE solicitudes SET estado = 'RECHAZADA' WHERE tutor_id = ? AND estado = 'PENDIENTE'").param(yo).update();
        for (String tabla : new String[] {"tokens_refresco", "codigos_verificacion", "tutor_materias", "tutor_disponibilidad"}) {
            jdbc.sql("DELETE FROM " + tabla + " WHERE usuario_id = ?").param(yo).update();
        }
        jdbc.sql("""
            UPDATE usuarios SET nombre = 'Cuenta eliminada', correo = ?, handle = ?, contrasena_hash = '!', rol = 'ESTUDIANTE',
                   estado_cuenta = 'ELIMINADA', documento_identidad = NULL, universidad = NULL, carrera = NULL, bio = NULL,
                   precio_hora = NULL, perfil_tutor_activo = FALSE, calendario_creado_en = NULL, ultima_actividad_en = NULL
            WHERE id = ?
            """).params("eliminada-" + yo + "@nexo.invalid", "@x" + yo.toString().replace("-", ""), yo).update();
    }
}
