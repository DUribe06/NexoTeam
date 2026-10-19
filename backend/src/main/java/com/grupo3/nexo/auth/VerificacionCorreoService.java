package com.grupo3.nexo.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.usuarios.EstadoCuenta;
import com.grupo3.nexo.usuarios.Usuario;
import com.grupo3.nexo.usuarios.UsuarioRepository;

/** D2: el dueño del correo lo confirma con el código de 6 dígitos y la sesión se abre de inmediato. */
@Service
public class VerificacionCorreoService {

    private final UsuarioRepository usuarios;
    private final CodigoVerificacionService codigos;
    private final SesionService sesiones;

    public VerificacionCorreoService(UsuarioRepository usuarios, CodigoVerificacionService codigos, SesionService sesiones) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.sesiones = sesiones;
    }

    /** Los intentos fallidos se guardan aunque se devuelva el error (por eso noRollbackFor). */
    @Transactional(noRollbackFor = ErrorNegocio.class)
    public SesionDto verificar(String correo, String codigo) {
        Usuario usuario = usuarios.findByCorreo(correo)
            .filter(u -> u.getEstadoCuenta() == EstadoCuenta.PENDIENTE_VERIFICACION_CORREO)
            .orElseThrow(() -> new ErrorNegocio(HttpStatus.NOT_FOUND, "CUENTA_NO_ENCONTRADA",
                "No hay una cuenta pendiente de verificar con ese correo"));
        codigos.verificar(usuario, PropositoCodigo.VERIFICAR_CORREO, codigo);
        usuario.marcarCorreoVerificado();
        return sesiones.iniciar(usuario);
    }

    /** Responde igual exista o no la cuenta, para no revelar qué correos están registrados. */
    @Transactional
    public void reenviar(String correo) {
        usuarios.findByCorreo(correo)
            .filter(u -> u.getEstadoCuenta() == EstadoCuenta.PENDIENTE_VERIFICACION_CORREO)
            .ifPresent(usuario -> {
                codigos.exigirQuePuedaReenviar(usuario, PropositoCodigo.VERIFICAR_CORREO);
                codigos.emitirYEnviar(usuario, PropositoCodigo.VERIFICAR_CORREO);
            });
    }
}
