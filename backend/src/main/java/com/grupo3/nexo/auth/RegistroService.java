package com.grupo3.nexo.auth;

import java.time.Clock;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.usuarios.GeneradorHandle;
import com.grupo3.nexo.usuarios.Usuario;
import com.grupo3.nexo.usuarios.UsuarioRepository;

/** RF1: crea la cuenta sin verificar y envía el código de 6 dígitos al correo (D2). */
@Service
public class RegistroService {

    private final UsuarioRepository usuarios;
    private final ValidadorCorreo validadorCorreo;
    private final PoliticaContrasena politicaContrasena;
    private final PasswordEncoder encriptador;
    private final CodigoVerificacionService codigos;
    private final GeneradorHandle handles;
    private final Clock reloj;

    public RegistroService(UsuarioRepository usuarios, ValidadorCorreo validadorCorreo,
                           PoliticaContrasena politicaContrasena, PasswordEncoder encriptador,
                           CodigoVerificacionService codigos, GeneradorHandle handles, Clock reloj) {
        this.usuarios = usuarios;
        this.validadorCorreo = validadorCorreo;
        this.politicaContrasena = politicaContrasena;
        this.encriptador = encriptador;
        this.codigos = codigos;
        this.handles = handles;
        this.reloj = reloj;
    }

    @Transactional
    public RespuestaRegistro registrar(SolicitudRegistro s) {
        politicaContrasena.validar(s.contrasena(), s.correo());
        validadorCorreo.validarDominioNoDesechable(s.correo());
        if (usuarios.existsByCorreo(s.correo())) {
            throw correoDuplicado();
        }
        validadorCorreo.validarQueElDominioRecibeCorreo(s.correo());

        String nombre = s.nombre().trim();
        Usuario usuario = Usuario.nuevo(nombre, s.correo(), encriptador.encode(s.contrasena()),
            handles.paraNombre(nombre), textoOpcional(s.documentoIdentidad()), s.versionTerminos(), reloj.instant());
        usuarios.saveAndFlush(usuario);
        codigos.emitirYEnviar(usuario, PropositoCodigo.VERIFICAR_CORREO);

        return new RespuestaRegistro(usuario.getId(), usuario.getCorreo(), usuario.getEstadoCuenta(),
            (int) CodigoVerificacionService.VIGENCIA.toSeconds());
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static ErrorNegocio correoDuplicado() {
        return new ErrorNegocio(HttpStatus.CONFLICT, "CORREO_DUPLICADO", "Ya existe una cuenta con ese correo");
    }
}
