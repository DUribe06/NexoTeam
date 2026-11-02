package com.grupo3.nexo.auth;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Endpoints de /api/auth según el contrato (openapi/nexo-api.yaml). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistroService registro;
    private final VerificacionCorreoService verificacion;
    private final LoginService login;
    private final SesionService sesiones;

    public AuthController(RegistroService registro, VerificacionCorreoService verificacion, LoginService login,
                          SesionService sesiones) {
        this.registro = registro;
        this.verificacion = verificacion;
        this.login = login;
        this.sesiones = sesiones;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaRegistro registrar(@Valid @RequestBody SolicitudRegistro solicitud) {
        return registro.registrar(solicitud);
    }

    @PostMapping("/verificar-correo")
    public SesionDto verificarCorreo(@Valid @RequestBody SolicitudesAuth.VerificarCorreo s) {
        return verificacion.verificar(s.correo(), s.codigo());
    }

    @PostMapping("/reenviar-codigo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviarCodigo(@Valid @RequestBody SolicitudesAuth.SoloCorreo s) {
        verificacion.reenviar(s.correo());
    }

    @PostMapping("/login")
    public SesionDto iniciarSesion(@Valid @RequestBody SolicitudesAuth.Login s) {
        return login.iniciarSesion(s.correo(), s.contrasena());
    }

    @PostMapping("/refrescar")
    public SesionDto refrescar(@Valid @RequestBody SolicitudesAuth.Refrescar s) {
        return sesiones.refrescar(s.tokenRefresco());
    }

    @PostMapping("/cerrar-sesion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarSesion(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SolicitudesAuth.Refrescar s) {
        sesiones.cerrar(UUID.fromString(jwt.getSubject()), s.tokenRefresco());
    }
}
