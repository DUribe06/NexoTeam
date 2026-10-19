package com.grupo3.nexo.auth;

import java.time.Clock;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.usuarios.Rol;
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
    private final Clock reloj;

    public RegistroService(UsuarioRepository usuarios, ValidadorCorreo validadorCorreo,
                           PoliticaContrasena politicaContrasena, PasswordEncoder encriptador,
                           CodigoVerificacionService codigos, Clock reloj) {
        this.usuarios = usuarios;
        this.validadorCorreo = validadorCorreo;
        this.politicaContrasena = politicaContrasena;
        this.encriptador = encriptador;
        this.codigos = codigos;
        this.reloj = reloj;
    }

    @Transactional
    public RespuestaRegistro registrar(SolicitudRegistro s) {
        validarRolYDatosDeTutor(s);
        politicaContrasena.validar(s.contrasena(), s.correo());
        validadorCorreo.validarDominioNoDesechable(s.correo());
        if (usuarios.existsByCorreo(s.correo())) {
            throw correoDuplicado();
        }
        validadorCorreo.validarQueElDominioRecibeCorreo(s.correo());

        Usuario usuario = Usuario.nuevo(s.nombre().trim(), s.correo(), encriptador.encode(s.contrasena()), s.rol(),
            s.documentoIdentidad().trim(), s.versionTerminos(), reloj.instant());
        if (s.rol() == Rol.TUTOR) {
            s.datosTutor().materias().forEach(m -> usuario.getMaterias().add(m.trim()));
            usuario.getModalidades().addAll(s.datosTutor().modalidades());
        }
        usuarios.saveAndFlush(usuario);
        codigos.emitirYEnviar(usuario, PropositoCodigo.VERIFICAR_CORREO);

        return new RespuestaRegistro(usuario.getId(), usuario.getCorreo(), usuario.getEstadoCuenta(),
            (int) CodigoVerificacionService.VIGENCIA.toSeconds());
    }

    private void validarRolYDatosDeTutor(SolicitudRegistro s) {
        if (s.rol() == Rol.ADMIN) {
            throw ErrorNegocio.datosInvalidos("rol", "solo se puede registrar un estudiante o un tutor");
        }
        if (s.rol() == Rol.TUTOR && s.datosTutor() == null) {
            throw ErrorNegocio.datosInvalidos("datosTutor", "es obligatorio para un tutor");
        }
        if (s.rol() == Rol.ESTUDIANTE && s.datosTutor() != null) {
            throw ErrorNegocio.datosInvalidos("datosTutor", "solo aplica a tutores");
        }
    }

    private static ErrorNegocio correoDuplicado() {
        return new ErrorNegocio(HttpStatus.CONFLICT, "CORREO_DUPLICADO", "Ya existe una cuenta con ese correo");
    }
}
