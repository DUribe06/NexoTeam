package com.grupo3.nexo.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AplicacionConfig {

    /** Reloj inyectable: las pruebas pueden fijarlo para comprobar vencimientos. */
    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    /** Contraseñas y códigos de verificación se guardan siempre con BCrypt. */
    @Bean
    PasswordEncoder encriptador() {
        return new BCryptPasswordEncoder(10);
    }
}
