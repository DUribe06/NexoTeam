package com.grupo3.nexo.correo;

/** Envío de correos de la aplicación. La implementación real (SMTP o proveedor) se elige más adelante. */
public interface EnviadorCorreo {

    void enviarCodigo(String destinatario, String asunto, String codigo, int minutosDeVigencia);
}
