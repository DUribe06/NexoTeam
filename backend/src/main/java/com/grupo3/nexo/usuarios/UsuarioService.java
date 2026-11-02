package com.grupo3.nexo.usuarios;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.error.ErrorNegocio;

/** Perfil propio y modo de uso. Toda cuenta puede ser Estudiante y Tutor; el tutor no se verifica. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;

    public UsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
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
        Usuario usuario = cuentaEnSesion(usuarioId);
        if (!usuario.isPerfilTutorActivo()) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "NO_ES_TUTOR",
                "Aún no tienes perfil de tutor; se crea al cambiar al modo Tutor");
        }
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
    private Usuario cuentaEnSesion(UUID usuarioId) {
        return usuarios.findById(usuarioId)
            .filter(u -> u.getEstadoCuenta() == EstadoCuenta.ACTIVA)
            .orElseThrow(() -> new ErrorNegocio(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", "Falta el token o no es válido"));
    }

    private static String textoOpcional(String texto) {
        return texto.isBlank() ? null : texto.trim();
    }
}
