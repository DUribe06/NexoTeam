package com.grupo3.nexo.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.grupo3.nexo.correo.EnviadorCorreo;
import com.grupo3.nexo.usuarios.Usuario;

/** Genera los códigos de 6 dígitos (D2): se guarda solo el hash, vencen a los 15 minutos y sirven una vez. */
@Service
public class CodigoVerificacionService {

    public static final Duration VIGENCIA = Duration.ofMinutes(15);

    private final SecureRandom azar = new SecureRandom();
    private final CodigoVerificacionRepository codigos;
    private final PasswordEncoder encriptador;
    private final EnviadorCorreo enviador;
    private final Clock reloj;

    public CodigoVerificacionService(CodigoVerificacionRepository codigos, PasswordEncoder encriptador,
                                     EnviadorCorreo enviador, Clock reloj) {
        this.codigos = codigos;
        this.encriptador = encriptador;
        this.enviador = enviador;
        this.reloj = reloj;
    }

    /** Crea un código nuevo, lo guarda cifrado y lo envía al correo del usuario. */
    public void emitirYEnviar(Usuario usuario, PropositoCodigo proposito) {
        String codigo = String.format("%06d", azar.nextInt(1_000_000));
        codigos.save(new CodigoVerificacion(usuario.getId(), proposito, encriptador.encode(codigo),
            reloj.instant().plus(VIGENCIA)));
        String asunto = proposito == PropositoCodigo.VERIFICAR_CORREO
            ? "Tu código para verificar tu correo en Nexo"
            : "Tu código para restablecer tu contraseña en Nexo";
        enviador.enviarCodigo(usuario.getCorreo(), asunto, codigo, (int) VIGENCIA.toMinutes());
    }
}
