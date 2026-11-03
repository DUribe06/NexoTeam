package com.grupo3.nexo.correo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** SOLO DESARROLLO: no envía nada, escribe el código en el log. Se desactiva con nexo.correo.modo=smtp. */
@Component
@ConditionalOnProperty(name = "nexo.correo.modo", havingValue = "consola", matchIfMissing = true)
public class EnviadorCorreoConsola implements EnviadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(EnviadorCorreoConsola.class);

    @Override
    public void enviarCodigo(String destinatario, String asunto, String codigo, int minutosDeVigencia) {
        log.info("[CORREO-DEV] Para: {} | {} | código: {} (vence en {} min)", destinatario, asunto, codigo, minutosDeVigencia);
    }
}
