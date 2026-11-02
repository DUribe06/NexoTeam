package com.grupo3.nexo.correo;

/** Comprueba que un dominio puede recibir correo. */
public interface VerificadorDominioCorreo {

    boolean dominioRecibeCorreo(String dominio);
}
