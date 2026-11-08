package com.grupo3.nexo.correo;

/** Envío de correos de la aplicación. En producción sale por SMTP con Brevo (EnviadorCorreoSmtp); las pruebas usan uno en memoria. */
public interface EnviadorCorreo {

    void enviarCodigo(String destinatario, String asunto, String codigo, int minutosDeVigencia);
}
