package com.grupo3.nexo.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.error.ErrorApi;
import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.usuarios.EstadoCuenta;
import com.grupo3.nexo.usuarios.Usuario;
import com.grupo3.nexo.usuarios.UsuarioRepository;

/** RF2: inicio de sesión con bloqueo temporal tras 5 intentos fallidos consecutivos. */
@Service
public class LoginService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encriptador;
    private final SesionService sesiones;
    private final Clock reloj;
    private final int maximoIntentos;
    private final Duration bloqueo;
    /** Se compara contra él cuando el correo no existe, para que tarde lo mismo y no delate qué correos hay. */
    private final String hashFalso;

    public LoginService(UsuarioRepository usuarios, PasswordEncoder encriptador, SesionService sesiones, Clock reloj,
                        @Value("${nexo.login.maximo-intentos:5}") int maximoIntentos,
                        @Value("${nexo.login.minutos-de-bloqueo:15}") int minutosDeBloqueo) {
        this.usuarios = usuarios;
        this.encriptador = encriptador;
        this.sesiones = sesiones;
        this.reloj = reloj;
        this.maximoIntentos = maximoIntentos;
        this.bloqueo = Duration.ofMinutes(minutosDeBloqueo);
        this.hashFalso = encriptador.encode("contrasena-que-no-existe");
    }

    /** Los intentos fallidos y el bloqueo se guardan aunque se devuelva el error (por eso noRollbackFor). */
    @Transactional(noRollbackFor = ErrorNegocio.class)
    public SesionDto iniciarSesion(String correo, String contrasena) {
        Instant ahora = reloj.instant();
        Optional<Usuario> encontrado = usuarios.findByCorreo(correo);
        if (encontrado.isEmpty()) {
            encriptador.matches(contrasena, hashFalso);
            throw credencialesInvalidas();
        }
        Usuario usuario = encontrado.get();

        if (usuario.estaBloqueado(ahora)) {
            throw cuentaBloqueada(usuario);
        }
        if (!encriptador.matches(contrasena, usuario.getContrasenaHash())) {
            if (usuario.registrarLoginFallido(ahora, maximoIntentos, bloqueo)) {
                throw cuentaBloqueada(usuario);
            }
            throw credencialesInvalidas();
        }
        if (usuario.getEstadoCuenta() == EstadoCuenta.PENDIENTE_VERIFICACION_CORREO) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "CORREO_NO_VERIFICADO",
                "Confirma tu correo con el código que te enviamos antes de iniciar sesión");
        }
        if (usuario.getEstadoCuenta() == EstadoCuenta.SUSPENDIDA) {
            throw new ErrorNegocio(HttpStatus.FORBIDDEN, "CUENTA_SUSPENDIDA", "Tu cuenta está suspendida");
        }
        usuario.registrarLoginExitoso();
        return sesiones.iniciar(usuario);
    }

    private static ErrorNegocio credencialesInvalidas() {
        return new ErrorNegocio(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos");
    }

    private static ErrorNegocio cuentaBloqueada(Usuario usuario) {
        return new ErrorNegocio(HttpStatus.LOCKED, "CUENTA_BLOQUEADA",
            "Demasiados intentos fallidos; tu cuenta está bloqueada temporalmente",
            List.of(new ErrorApi.Detalle("bloqueadoHasta", usuario.getBloqueadoHasta().toString())));
    }
}
