package com.grupo3.nexo.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.grupo3.nexo.correo.EnviadorCorreo;

/** Pruebas: en vez de enviar, recuerda el último código que se mandó a cada correo. */
class EnviadorCorreoEnMemoria implements EnviadorCorreo {

    final Map<String, String> ultimoCodigo = new ConcurrentHashMap<>();

    @Override
    public void enviarCodigo(String destinatario, String asunto, String codigo, int minutosDeVigencia) {
        ultimoCodigo.put(destinatario, codigo);
    }
}
