package com.grupo3.nexo.correo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.grupo3.nexo.error.ErrorNegocio;

class EnviadorCorreoSmtpTest {

    private final JavaMailSender correo = mock(JavaMailSender.class);
    private final EnviadorCorreoSmtp enviador = new EnviadorCorreoSmtp(correo, "Nexo <no-responder@nexo.test>");

    @Test
    void elCorreoLlevaElCodigoYSuVigencia() {
        enviador.enviarCodigo("ana@ejemplo.com", "Confirma tu correo", "123456", 15);
        ArgumentCaptor<SimpleMailMessage> mensaje = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(correo).send(mensaje.capture());
        assertThat(mensaje.getValue().getTo()).containsExactly("ana@ejemplo.com");
        assertThat(mensaje.getValue().getFrom()).isEqualTo("Nexo <no-responder@nexo.test>");
        assertThat(mensaje.getValue().getText()).contains("123456").contains("15 minutos");
    }

    @Test
    void siElProveedorFallaResponde503YNoFingeQueSalio() {
        doThrow(new MailSendException("sin conexión")).when(correo).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> enviador.enviarCodigo("ana@ejemplo.com", "Asunto", "123456", 15))
            .isInstanceOfSatisfying(ErrorNegocio.class, e -> {
                assertThat(e.getEstado()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(e.getCodigo()).isEqualTo("CORREO_NO_DISPONIBLE");
            });
    }
}
