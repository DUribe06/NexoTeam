package com.grupo3.nexo.auth;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.grupo3.nexo.correo.VerificadorDominioCorreo;

@TestConfiguration
class ConfiguracionPruebas {

    @Bean
    @Primary
    EnviadorCorreoEnMemoria enviadorEnMemoria() {
        return new EnviadorCorreoEnMemoria();
    }

    /** Todos los dominios reciben correo salvo sin-mx.test; así las pruebas no dependen de la red. */
    @Bean
    @Primary
    VerificadorDominioCorreo verificadorDeDominio() {
        return dominio -> !dominio.equals("sin-mx.test");
    }
}
