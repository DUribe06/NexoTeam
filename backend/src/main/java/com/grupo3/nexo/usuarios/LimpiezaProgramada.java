package com.grupo3.nexo.usuarios;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Cada cuenta limpia lo suyo al entrar; esto se lleva lo de las cuentas que no vuelven. Corre de madrugada. */
@Component
class LimpiezaProgramada {

    private final JdbcClient jdbc;

    LimpiezaProgramada(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "America/Bogota")
    @Transactional
    void limpiar() {
        jdbc.sql("DELETE FROM tokens_refresco WHERE vence_en < now()").update();
        jdbc.sql("DELETE FROM codigos_verificacion WHERE creado_en < now() - INTERVAL '1 day'").update();
    }
}
