package com.grupo3.nexo.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.grupo3.nexo.correo.EnviadorCorreo;
import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.usuarios.Usuario;

/**
 * Códigos de 6 dígitos (D2): se guarda solo el hash, vencen a los 15 minutos, sirven una vez, admiten
 * 5 intentos y el reenvío tiene espera de 60 s y tope de 5 envíos por hora.
 */
@Service
public class CodigoVerificacionService {

    public static final Duration VIGENCIA = Duration.ofMinutes(15);
    static final int MAXIMO_INTENTOS = 5;
    static final Duration ESPERA_ENTRE_ENVIOS = Duration.ofSeconds(60);
    static final int MAXIMO_ENVIOS_POR_HORA = 5;

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

    /** Crea un código nuevo (invalidando los anteriores), lo guarda cifrado y lo envía al correo del usuario. */
    public void emitirYEnviar(Usuario usuario, PropositoCodigo proposito) {
        Instant ahora = reloj.instant();
        codigos.findByUsuarioIdAndPropositoAndCerradoEnIsNull(usuario.getId(), proposito)
            .forEach(anterior -> anterior.cerrar(ahora));

        String codigo = String.format("%06d", azar.nextInt(1_000_000));
        codigos.save(new CodigoVerificacion(usuario.getId(), proposito, encriptador.encode(codigo),
            ahora.plus(VIGENCIA), ahora));
        String asunto = proposito == PropositoCodigo.VERIFICAR_CORREO
            ? "Tu código para verificar tu correo en Nexo"
            : "Tu código para restablecer tu contraseña en Nexo";
        enviador.enviarCodigo(usuario.getCorreo(), asunto, codigo, (int) VIGENCIA.toMinutes());
    }

    /**
     * Comprueba el código ingresado y, si es correcto, lo cierra. Los intentos fallidos deben guardarse aunque
     * se lance el error, así que quien llame debe usar {@code noRollbackFor = ErrorNegocio.class}.
     */
    public void verificar(Usuario usuario, PropositoCodigo proposito, String ingresado) {
        Instant ahora = reloj.instant();
        CodigoVerificacion codigo = codigos
            .findFirstByUsuarioIdAndPropositoAndCerradoEnIsNullOrderByCreadoEnDesc(usuario.getId(), proposito)
            .orElseThrow(CodigoVerificacionService::codigoInvalido);

        if (!ahora.isBefore(codigo.getVenceEn())) {
            codigo.cerrar(ahora);
            throw error("CODIGO_VENCIDO", "El código venció; pide uno nuevo");
        }
        if (codigo.getIntentos() >= MAXIMO_INTENTOS) {
            codigo.cerrar(ahora);
            throw error("CODIGO_INTENTOS_AGOTADOS", "Superaste los intentos; pide un código nuevo");
        }
        if (!encriptador.matches(ingresado, codigo.getCodigoHash())) {
            codigo.registrarIntentoFallido();
            if (codigo.getIntentos() >= MAXIMO_INTENTOS) {
                codigo.cerrar(ahora);
                throw error("CODIGO_INTENTOS_AGOTADOS", "Superaste los intentos; pide un código nuevo");
            }
            throw codigoInvalido();
        }
        codigo.cerrar(ahora);
    }

    /** Aplica la espera de 60 s y el tope por hora antes de mandar otro código. */
    public void exigirQuePuedaReenviar(Usuario usuario, PropositoCodigo proposito) {
        esperaParaReenviar(usuario, proposito).ifPresent(espera -> {
            throw demasiadasSolicitudes(espera);
        });
    }

    /** Cuánto falta para poder mandar otro código; vacío si ya se puede. */
    public Optional<Duration> esperaParaReenviar(Usuario usuario, PropositoCodigo proposito) {
        Instant ahora = reloj.instant();
        Optional<Duration> porEspera = codigos.findFirstByUsuarioIdAndPropositoOrderByCreadoEnDesc(usuario.getId(), proposito)
            .map(ultimo -> Duration.between(ahora, ultimo.getCreadoEn().plus(ESPERA_ENTRE_ENVIOS)))
            .filter(espera -> !espera.isNegative() && !espera.isZero());
        if (porEspera.isPresent()) {
            return porEspera;
        }
        long ultimaHora = codigos.countByUsuarioIdAndPropositoAndCreadoEnAfter(usuario.getId(), proposito, ahora.minus(Duration.ofHours(1)));
        return ultimaHora >= MAXIMO_ENVIOS_POR_HORA ? Optional.of(Duration.ofMinutes(10)) : Optional.empty();
    }

    /** Dice lo mismo si el código está mal, si no hay ninguno activo o si no hay cuenta: así no delata qué correos existen. */
    static ErrorNegocio codigoInvalido() {
        return error("CODIGO_INVALIDO", "El código es incorrecto o ya no está activo");
    }

    private static ErrorNegocio demasiadasSolicitudes(Duration espera) {
        return new ErrorNegocio(HttpStatus.TOO_MANY_REQUESTS, "DEMASIADAS_SOLICITUDES",
            "Espera un momento antes de pedir otro código", java.util.List.of(), (int) Math.max(1, espera.toSeconds()));
    }

    private static ErrorNegocio error(String codigo, String mensaje) {
        return new ErrorNegocio(HttpStatus.BAD_REQUEST, codigo, mensaje);
    }
}
