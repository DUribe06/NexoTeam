package com.grupo3.nexo.admin;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea la cuenta de administrador la primera vez, con los datos de NEXO_ADMIN_CORREO y NEXO_ADMIN_CONTRASENA. No hay otra
 * forma de crearla: el registro público nunca da ese rol. Si la cuenta ya existe no la toca, así que cambiar la variable
 * no cambia una contraseña ya creada.
 */
@Component
class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final JdbcClient jdbc;
    private final PasswordEncoder encriptador;
    private final String correo;
    private final String contrasena;

    AdministradorInicial(JdbcClient jdbc, PasswordEncoder encriptador,
                         @Value("${nexo.admin.correo:}") String correo,
                         @Value("${nexo.admin.contrasena:}") String contrasena) {
        this.jdbc = jdbc;
        this.encriptador = encriptador;
        this.correo = correo.trim().toLowerCase();
        this.contrasena = contrasena;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (correo.isEmpty() || contrasena.isEmpty()) {
            return;
        }
        int creadas = jdbc.sql("""
            INSERT INTO usuarios (id, nombre, correo, contrasena_hash, rol, estado_cuenta, handle)
            VALUES (?, 'Administración', ?, ?, 'ADMIN', 'ACTIVA', ?)
            ON CONFLICT DO NOTHING
            """).params(UUID.randomUUID(), correo, encriptador.encode(contrasena), "@admin-" + UUID.randomUUID().toString().substring(0, 8))
            .update();
        if (creadas == 1) {
            log.info("Cuenta de administrador creada");
        }
    }
}
