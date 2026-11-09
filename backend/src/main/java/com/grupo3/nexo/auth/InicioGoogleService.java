package com.grupo3.nexo.auth;

import java.time.Clock;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.usuarios.EstadoCuenta;
import com.grupo3.nexo.usuarios.GeneradorHandle;
import com.grupo3.nexo.usuarios.Usuario;
import com.grupo3.nexo.usuarios.UsuarioRepository;

/**
 * Inicio con Google. Une la cuenta que ya tenga ese correo, porque Google confirmó que es de esta persona.
 * Quien entra por primera vez debe aceptar las condiciones, igual que en el registro normal.
 */
@Service
public class InicioGoogleService {

    private static final int LARGO_MAXIMO_NOMBRE = 100;

    private final VerificadorTokenGoogle verificador;
    private final UsuarioRepository usuarios;
    private final SesionService sesiones;
    private final GeneradorHandle handles;
    private final Clock reloj;

    public InicioGoogleService(VerificadorTokenGoogle verificador, UsuarioRepository usuarios, SesionService sesiones,
                               GeneradorHandle handles, Clock reloj) {
        this.verificador = verificador;
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.handles = handles;
        this.reloj = reloj;
    }

    @Transactional
    public SesionDto iniciarSesion(SolicitudesAuth.Google s) {
        IdentidadGoogle identidad = verificador.verificar(s.tokenId());
        Usuario usuario = usuarios.findByGoogleSub(identidad.sub()).orElseGet(() -> unirOCrear(identidad, s));
        if (usuario.getEstadoCuenta() == EstadoCuenta.SUSPENDIDA) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "CUENTA_SUSPENDIDA", "Tu cuenta está suspendida");
        }
        // La foto de Google siempre al día: si la cambia allá, aquí también (la que suba en Nexo sigue ganando)
        usuario.cambiarFotoGoogle(identidad.foto());
        usuario.registrarLoginExitoso();
        return sesiones.iniciar(usuario);
    }

    private Usuario unirOCrear(IdentidadGoogle identidad, SolicitudesAuth.Google s) {
        Optional<Usuario> conEseCorreo = usuarios.findByCorreo(identidad.correo());
        if (conEseCorreo.isEmpty()) {
            exigirTerminos(s);
            String nombre = nombreDe(identidad);
            return usuarios.saveAndFlush(Usuario.nuevoConGoogle(nombre, identidad.correo(), identidad.sub(),
                handles.paraNombre(nombre), s.versionTerminos(), reloj.instant()));
        }
        Usuario usuario = conEseCorreo.get();
        if (usuario.getGoogleSub() != null) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "GOOGLE_CUENTA_DISTINTA", "Este correo ya está unido a otra cuenta de Google");
        }
        if (usuario.getEstadoCuenta() == EstadoCuenta.PENDIENTE_VERIFICACION_CORREO) {
            exigirTerminos(s);
            usuario.tomarCuentaSinConfirmar(identidad.sub(), s.versionTerminos(), reloj.instant());
        } else {
            usuario.unirConGoogle(identidad.sub());
        }
        return usuario;
    }

    private static void exigirTerminos(SolicitudesAuth.Google s) {
        if (!Boolean.TRUE.equals(s.aceptaTerminos()) || s.versionTerminos() == null || s.versionTerminos().isBlank()) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "TERMINOS_REQUERIDOS", "Debes aceptar las condiciones para crear tu cuenta");
        }
    }

    private static String nombreDe(IdentidadGoogle identidad) {
        String nombre = identidad.nombre() == null ? "" : identidad.nombre().trim();
        if (nombre.length() < 2) {
            nombre = identidad.correo().substring(0, identidad.correo().indexOf('@'));
        }
        return nombre.length() > LARGO_MAXIMO_NOMBRE ? nombre.substring(0, LARGO_MAXIMO_NOMBRE) : nombre;
    }
}
