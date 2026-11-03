package com.grupo3.nexo.usuarios;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.config.Ids;
import com.grupo3.nexo.error.ErrorNegocio;

/** Perfil propio y modo de uso. Toda cuenta puede ser Estudiante y Tutor; el tutor no se verifica. */
@Service
public class UsuarioService {

    private static final Duration FRECUENCIA_ACTIVIDAD = Duration.ofMinutes(1);

    private final UsuarioRepository usuarios;
    private final Clock reloj;

    public UsuarioService(UsuarioRepository usuarios, Clock reloj) {
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    /** Marca a la cuenta como activa ahora; se escribe como mucho una vez por minuto. */
    @Transactional
    public void registrarActividad(UUID usuarioId) {
        Instant ahora = reloj.instant();
        usuarios.registrarActividad(usuarioId, ahora, ahora.minus(FRECUENCIA_ACTIVIDAD));
    }

    @Transactional(readOnly = true)
    public PerfilPropioDto perfil(UUID usuarioId) {
        return PerfilPropioDto.de(cuentaEnSesion(usuarioId));
    }

    @Transactional
    public PerfilPropioDto cambiarModo(UUID usuarioId, Modo modo) {
        Usuario usuario = cuentaEnSesion(usuarioId);
        if (usuario.getRol() == Rol.ADMIN) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "ADMIN_SIN_MODOS", "Una cuenta de administrador no usa modos");
        }
        usuario.cambiarModo(modo);
        return PerfilPropioDto.de(usuario);
    }

    @Transactional
    public PerfilPropioDto editarPerfilTutor(UUID usuarioId, SolicitudesUsuario.EditarPerfilTutor s) {
        if (s.estaVacia()) {
            throw ErrorNegocio.datosInvalidos("cuerpo", "envía al menos un campo");
        }
        Usuario usuario = tutorEnSesion(usuarioId);
        if (s.bio() != null) usuario.cambiarBio(textoOpcional(s.bio()));
        if (s.universidad() != null) usuario.cambiarUniversidad(textoOpcional(s.universidad()));
        if (s.carrera() != null) usuario.cambiarCarrera(textoOpcional(s.carrera()));
        if (s.precioHora() != null) usuario.cambiarPrecioHora(s.precioHora());
        if (s.materias() != null) usuario.reemplazarMaterias(s.materias());
        return PerfilPropioDto.de(usuario);
    }

    /** El modo pedido o, si no se pidió ninguno, el actual. Un administrador no usa modos: consulta como estudiante. */
    public Modo modoDe(UUID usuarioId, Modo pedido) {
        Usuario usuario = cuentaEnSesion(usuarioId);
        if (pedido != null) {
            return pedido;
        }
        return usuario.getRol() == Rol.TUTOR ? Modo.TUTOR : Modo.ESTUDIANTE;
    }

    /** El token vive 15 min: si entretanto la cuenta se borró o se suspendió, ya no sirve. */
    public Usuario cuentaEnSesion(UUID usuarioId) {
        return usuarios.findById(usuarioId)
            .filter(u -> u.getEstadoCuenta() == EstadoCuenta.ACTIVA)
            .orElseThrow(() -> new ErrorNegocio(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", "Falta el token o no es válido"));
    }

    /** La cuenta en sesión, que además debe tener perfil de tutor. No importa en qué modo esté ahora. */
    public Usuario tutorEnSesion(UUID usuarioId) {
        Usuario usuario = cuentaEnSesion(usuarioId);
        if (!usuario.isPerfilTutorActivo()) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "NO_ES_TUTOR",
                "Aún no tienes perfil de tutor; se crea al cambiar al modo Tutor");
        }
        return usuario;
    }

    /** El tutor con ese id. El id llega en texto: uno mal formado tampoco es de ningún tutor, así que da el mismo 404. */
    public Usuario tutorPublico(String tutorId) {
        return Ids.leer(tutorId).flatMap(usuarios::findById)
            .filter(Usuario::esTutorPublico)
            .orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND, "TUTOR_NO_ENCONTRADO", "No existe ese tutor"));
    }

    private static String textoOpcional(String texto) {
        return texto.isBlank() ? null : texto.trim();
    }
}
