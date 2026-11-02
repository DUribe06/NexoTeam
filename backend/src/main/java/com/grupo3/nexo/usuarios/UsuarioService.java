package com.grupo3.nexo.usuarios;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * TODO(fase de sesiones): calcular las cifras con las sesiones del usuario en `modo` (si es null, su modo actual).
     * Hasta que exista ese módulo no hay actividad que contar, así que siempre responde ceros.
     */
    @Transactional(readOnly = true)
    public ResumenActividadDto resumen(UUID usuarioId, Modo modo) {
        cuentaEnSesion(usuarioId);
        return ResumenActividadDto.sinActividad();
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
        return idDe(tutorId).flatMap(usuarios::findById)
            .filter(Usuario::esTutorPublico)
            .orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND, "TUTOR_NO_ENCONTRADO", "No existe ese tutor"));
    }

    private static Optional<UUID> idDe(String texto) {
        try {
            return Optional.of(UUID.fromString(texto));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String textoOpcional(String texto) {
        return texto.isBlank() ? null : texto.trim();
    }
}
