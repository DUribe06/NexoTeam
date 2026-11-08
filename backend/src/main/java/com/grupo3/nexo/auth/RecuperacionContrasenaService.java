package com.grupo3.nexo.auth;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.usuarios.Usuario;
import com.grupo3.nexo.usuarios.UsuarioRepository;

/** RF16: pedir un código al correo y cambiar la contraseña con él, sin delatar qué correos están registrados. */
@Service
public class RecuperacionContrasenaService {

    private static final String TEXTO_DE_RELLENO = "contrasena-que-no-existe";

    private final UsuarioRepository usuarios;
    private final CodigoVerificacionService codigos;
    private final PoliticaContrasena politica;
    private final PasswordEncoder encriptador;
    private final SesionService sesiones;
    /** Se compara contra él cuando el correo no existe, para que tarde lo mismo que un código real. */
    private final String hashFalso;

    public RecuperacionContrasenaService(UsuarioRepository usuarios, CodigoVerificacionService codigos,
                                         PoliticaContrasena politica, PasswordEncoder encriptador,
                                         SesionService sesiones) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.politica = politica;
        this.encriptador = encriptador;
        this.sesiones = sesiones;
        this.hashFalso = encriptador.encode(TEXTO_DE_RELLENO);
    }

    /**
     * Termina igual en todos los casos. Al superar los límites de envío no responde 429: solo las cuentas reales los
     * alcanzan, así que el error delataría que el correo existe. Sin envío se cifra un texto de relleno para que tarde igual.
     */
    @Transactional
    public void recuperar(String correo) {
        Optional<Usuario> destinatario = usuarios.findByCorreo(correo)
            .filter(u -> codigos.esperaParaReenviar(u, PropositoCodigo.RESTABLECER_CONTRASENA).isEmpty());
        if (destinatario.isPresent()) {
            codigos.emitirYEnviar(destinatario.get(), PropositoCodigo.RESTABLECER_CONTRASENA);
        } else {
            encriptador.encode(TEXTO_DE_RELLENO);
        }
    }

    /**
     * La contraseña se revisa antes que el código para que una débil no lo gaste. Los intentos fallidos se guardan
     * aunque se devuelva el error (por eso noRollbackFor).
     */
    @Transactional(noRollbackFor = ErrorNegocio.class)
    public void restablecer(String correo, String codigo, String nuevaContrasena) {
        politica.validar(nuevaContrasena, correo);
        Optional<Usuario> encontrado = usuarios.findByCorreo(correo);
        if (encontrado.isEmpty()) {
            encriptador.matches(codigo, hashFalso);
            throw CodigoVerificacionService.codigoInvalido();
        }
        Usuario usuario = encontrado.get();
        codigos.verificar(usuario, PropositoCodigo.RESTABLECER_CONTRASENA, codigo);
        usuario.cambiarContrasena(encriptador.encode(nuevaContrasena));
        sesiones.cerrarTodas(usuario.getId());
    }
}
