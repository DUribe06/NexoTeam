package com.grupo3.nexo.correo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Con nexo.correo.verificar-mx=false no se consulta el DNS (entornos sin red). */
@Component
@ConditionalOnProperty(name = "nexo.correo.verificar-mx", havingValue = "false")
public class VerificadorDominioPermisivo implements VerificadorDominioCorreo {

    @Override
    public boolean dominioRecibeCorreo(String dominio) {
        return true;
    }
}
