package com.grupo3.nexo.correo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.grupo3.nexo.error.ErrorNegocio;

/** Envío real, con cualquier proveedor SMTP. Si falla, la petición falla y su transacción no deja un código sin entregar. */
@Component
@ConditionalOnProperty(name = "nexo.correo.modo", havingValue = "smtp")
public class EnviadorCorreoSmtp implements EnviadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(EnviadorCorreoSmtp.class);

    private final JavaMailSender correo;
    private final String remitente;

    public EnviadorCorreoSmtp(JavaMailSender correo, @Value("${nexo.correo.remitente}") String remitente) {
        this.correo = correo;
        this.remitente = remitente;
    }

    @Override
    public void enviarCodigo(String destinatario, String asunto, String codigo, int minutosDeVigencia) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText("""
            Tu código de Nexo es: %s

            Vence en %d minutos. Si no lo pediste, ignora este correo: nadie puede entrar a tu cuenta sin él.
            """.formatted(codigo, minutosDeVigencia));
        try {
            correo.send(mensaje);
        } catch (MailException e) {
            // Sin el destinatario ni el código en el registro; el detalle del proveedor sí sirve para diagnosticar
            log.error("No se pudo enviar un correo: {}", e.getMessage());
            throw new ErrorNegocio(HttpStatus.SERVICE_UNAVAILABLE, "CORREO_NO_DISPONIBLE", "No pudimos enviar el correo. Inténtalo de nuevo en un momento");
        }
    }
}
